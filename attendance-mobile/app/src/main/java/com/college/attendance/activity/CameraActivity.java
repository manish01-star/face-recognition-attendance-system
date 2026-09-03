package com.college.attendance.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Size;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.college.attendance.R;
import com.college.attendance.api.ApiClient;
import com.college.attendance.api.ApiService;
import com.college.attendance.dto.AttendanceMarkResponse;
import com.college.attendance.utils.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/*
 * CameraX getImage() is an experimental API.
 */
@ExperimentalGetImage
public class CameraActivity extends AppCompatActivity {

    // =========================================================
    // PERMISSION CODES
    // =========================================================

    private static final int CAMERA_PERMISSION_REQUEST = 1001;

    private static final int LOCATION_PERMISSION_REQUEST = 1002;


    // =========================================================
    // ATTENDANCE ACTION
    // =========================================================

    public static final String EXTRA_ACTION = "attendance_action";

    public static final String ACTION_CHECK_IN = "CHECK_IN";

    public static final String ACTION_CHECK_OUT = "CHECK_OUT";


    // =========================================================
    // PERFORMANCE SETTINGS
    // =========================================================

    /*
     * Analyze at most approximately 8 frames per second.
     *
     * Camera preview remains smooth because analysis runs
     * on a background executor.
     */
    private static final long ANALYSIS_INTERVAL_MS = 120L;


    /*
     * Require 3 consecutive suitable frames before
     * automatically capturing the attendance photo.
     */
    private static final int REQUIRED_STABLE_FRAMES = 3;


    /*
     * Face must occupy at least 18% of analysis image height.
     */
    private static final float MIN_FACE_HEIGHT_RATIO = 0.18f;


    /*
     * Face should remain reasonably close to the center.
     */
    private static final float CENTER_TOLERANCE = 0.30f;


    // =========================================================
    // VIEWS
    // =========================================================

    private PreviewView previewView;

    private ImageButton btnBack;

    private TextView tvCameraTitle;


    // =========================================================
    // CAMERA
    // =========================================================

    private ImageCapture imageCapture;

    private ImageAnalysis imageAnalysis;

    private ProcessCameraProvider cameraProvider;


    // =========================================================
    // FACE DETECTION
    // =========================================================

    private FaceDetector faceDetector;

    private ExecutorService analysisExecutor;


    // =========================================================
    // LOCATION
    // =========================================================

    private FusedLocationProviderClient fusedLocationClient;


    // =========================================================
    // API / SESSION
    // =========================================================

    private ApiService apiService;

    private SessionManager sessionManager;


    // =========================================================
    // ATTENDANCE
    // =========================================================

    private String attendanceAction;

    private File pendingPhotoFile;


    /*
     * Prevent multiple captures / API calls.
     */
    private volatile boolean isProcessing = false;


    /*
     * Number of consecutive suitable face frames.
     */
    private int stableFaceFrames = 0;


    /*
     * Last frame analysis timestamp.
     */
    private long lastAnalysisTime = 0L;


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_camera);


        initializeViews();


        sessionManager =
                new SessionManager(this);


        /*
         * User must be logged in.
         */
        if (!sessionManager.isLoggedIn()) {

            openLoginScreen();

            return;
        }


        apiService =
                ApiClient.getApiService(this);


        fusedLocationClient =
                LocationServices
                        .getFusedLocationProviderClient(this);


        attendanceAction =
                getIntent()
                        .getStringExtra(EXTRA_ACTION);


        /*
         * Validate attendance action.
         */
        if (!ACTION_CHECK_IN.equals(attendanceAction)
                && !ACTION_CHECK_OUT.equals(attendanceAction)) {

            Toast.makeText(
                    this,
                    R.string.invalid_attendance_action,
                    Toast.LENGTH_LONG
            ).show();

            finish();

            return;
        }


        setupScreenText();


        // =====================================================
        // ML KIT FACE DETECTOR
        // =====================================================

        /*
         * FAST mode is enough because Android is only responsible
         * for detecting when a suitable face is available.
         *
         * Final face verification and anti-spoofing are performed
         * by the backend / Python face service.
         */
        FaceDetectorOptions detectorOptions =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(
                                FaceDetectorOptions
                                        .PERFORMANCE_MODE_FAST
                        )
                        .setMinFaceSize(0.15f)
                        .build();


        faceDetector =
                FaceDetection
                        .getClient(detectorOptions);


        /*
         * Dedicated background executor.
         *
         * Face detection never runs on the UI thread.
         */
        analysisExecutor =
                Executors.newSingleThreadExecutor();


        // =====================================================
        // START CAMERA
        // =====================================================

        if (hasCameraPermission()) {

            startCamera();

        } else {

            requestCameraPermission();
        }


        // =====================================================
        // BACK BUTTON
        // =====================================================

        btnBack.setOnClickListener(v -> {

            if (!isProcessing) {

                deletePendingPhoto();

                finish();
            }
        });
    }


    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initializeViews() {

        previewView =
                findViewById(R.id.previewView);

        btnBack =
                findViewById(R.id.btnBack);

        tvCameraTitle =
                findViewById(R.id.tvCameraTitle);
    }


    // =========================================================
    // SCREEN TEXT
    // =========================================================

    private void setupScreenText() {

        if (ACTION_CHECK_IN.equals(attendanceAction)) {

            tvCameraTitle.setText(
                    R.string.check_in
            );

        } else {

            tvCameraTitle.setText(
                    R.string.check_out
            );
        }
    }


    // =========================================================
    // CAMERA PERMISSION
    // =========================================================

    private boolean hasCameraPermission() {

        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }


    private void requestCameraPermission() {

        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.CAMERA
                },
                CAMERA_PERMISSION_REQUEST
        );
    }


    // =========================================================
    // LOCATION PERMISSION
    // =========================================================

    private boolean hasLocationPermission() {

        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                ||
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;
    }


    private void requestLocationPermission() {

        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST
        );
    }


    // =========================================================
    // START CAMERA
    // =========================================================

    private void startCamera() {

        ListenableFuture<ProcessCameraProvider>
                cameraProviderFuture =
                ProcessCameraProvider
                        .getInstance(this);


        cameraProviderFuture.addListener(
                () -> {

                    try {

                        cameraProvider =
                                cameraProviderFuture.get();

                        bindCamera(cameraProvider);

                    } catch (ExecutionException e) {

                        showCameraStartError();

                    } catch (InterruptedException e) {

                        Thread.currentThread().interrupt();

                        showCameraStartError();
                    }

                },
                ContextCompat.getMainExecutor(this)
        );
    }


    // =========================================================
    // BIND CAMERA
    // =========================================================

    private void bindCamera(
            ProcessCameraProvider provider) {

        // =====================================================
        // PREVIEW
        // =====================================================

        Preview preview =
                new Preview.Builder()
                        .build();


        preview.setSurfaceProvider(
                previewView.getSurfaceProvider()
        );


        // =====================================================
        // IMAGE CAPTURE
        // =====================================================

        /*
         * This camera use case captures the actual attendance
         * photograph which is uploaded to the backend.
         */
        imageCapture =
                new ImageCapture.Builder()
                        .setCaptureMode(
                                ImageCapture
                                        .CAPTURE_MODE_MINIMIZE_LATENCY
                        )
                        .build();


        // =====================================================
        // IMAGE ANALYSIS
        // =====================================================

        /*
         * Use a low analysis resolution.
         *
         * The analysis stream is only used to determine whether
         * a suitable face is present.
         */
        ResolutionSelector resolutionSelector =
                new ResolutionSelector.Builder()
                        .setResolutionStrategy(
                                new ResolutionStrategy(
                                        new Size(640, 480),
                                        ResolutionStrategy
                                                .FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                )
                        )
                        .build();


        imageAnalysis =
                new ImageAnalysis.Builder()
                        .setResolutionSelector(
                                resolutionSelector
                        )
                        .setBackpressureStrategy(
                                ImageAnalysis
                                        .STRATEGY_KEEP_ONLY_LATEST
                        )
                        .build();


        /*
         * Run ML Kit analysis on background executor.
         */
        imageAnalysis.setAnalyzer(
                analysisExecutor,
                this::analyzeFrame
        );


        // =====================================================
        // FRONT CAMERA
        // =====================================================

        CameraSelector cameraSelector =
                CameraSelector.DEFAULT_FRONT_CAMERA;


        try {

            provider.unbindAll();


            provider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    R.string.front_camera_failed,
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    // =========================================================
    // CAMERA START ERROR
    // =========================================================

    private void showCameraStartError() {

        Toast.makeText(
                this,
                R.string.camera_start_failed,
                Toast.LENGTH_LONG
        ).show();
    }


    // =========================================================
    // ANALYZE CAMERA FRAME
    // =========================================================

    private void analyzeFrame(
            @NonNull ImageProxy imageProxy) {

        /*
         * Attendance processing has already started.
         *
         * Do not analyze additional frames.
         */
        if (isProcessing) {

            imageProxy.close();

            return;
        }


        // =====================================================
        // THROTTLING
        // =====================================================

        long currentTime =
                System.currentTimeMillis();


        if (currentTime - lastAnalysisTime
                < ANALYSIS_INTERVAL_MS) {

            imageProxy.close();

            return;
        }


        lastAnalysisTime =
                currentTime;


        // =====================================================
        // GET ANDROID IMAGE
        // =====================================================

        if (imageProxy.getImage() == null) {

            imageProxy.close();

            return;
        }


        InputImage inputImage =
                InputImage.fromMediaImage(
                        imageProxy.getImage(),
                        imageProxy
                                .getImageInfo()
                                .getRotationDegrees()
                );


        /*
         * IMPORTANT:
         *
         * ImageProxy must remain open until ML Kit has finished
         * processing the underlying image.
         */
        faceDetector
                .process(inputImage)
                .addOnSuccessListener(
                        faces -> {

                            try {

                                if (!isProcessing) {

                                    handleDetectedFaces(
                                            faces,
                                            imageProxy
                                    );
                                }

                            } finally {

                                /*
                                 * Close only after ML Kit has
                                 * completed processing.
                                 */
                                imageProxy.close();
                            }
                        }
                )
                .addOnFailureListener(
                        e -> {

                            stableFaceFrames = 0;

                            imageProxy.close();
                        }
                );
    }


    // =========================================================
    // HANDLE DETECTED FACES
    // =========================================================

    private void handleDetectedFaces(
            List<Face> faces,
            ImageProxy imageProxy) {

        /*
         * Exactly one face is required.
         *
         * 0 faces:
         * reset stability.
         *
         * 2+ faces:
         * reset stability.
         */
        if (faces == null
                || faces.size() != 1) {

            stableFaceFrames = 0;

            return;
        }


        Face face =
                faces.get(0);


        // =====================================================
        // FACE QUALITY / POSITION
        // =====================================================

        if (!isFaceSuitable(
                face,
                imageProxy
        )) {

            stableFaceFrames = 0;

            return;
        }


        // =====================================================
        // STABLE FACE
        // =====================================================

        stableFaceFrames++;


        /*
         * Automatically capture after the face has remained
         * suitable for the required number of frames.
         */
        if (stableFaceFrames
                >= REQUIRED_STABLE_FRAMES
                && !isProcessing) {

            /*
             * Set this BEFORE switching to the UI thread.
             *
             * This prevents another analyzer callback from
             * starting a second capture.
             */
            isProcessing = true;

            stableFaceFrames = 0;


            runOnUiThread(
                    this::captureImage
            );
        }
    }


    // =========================================================
    // FACE QUALITY CHECK
    // =========================================================

    private boolean isFaceSuitable(
            Face face,
            ImageProxy imageProxy) {

        Rect bounds =
                face.getBoundingBox();


        int imageWidth =
                imageProxy.getWidth();

        int imageHeight =
                imageProxy.getHeight();


        if (imageWidth <= 0
                || imageHeight <= 0) {

            return false;
        }


        // =====================================================
        // FACE SIZE
        // =====================================================

        /*
         * Face should be sufficiently large in the frame.
         */
        float faceHeightRatio =
                (float) bounds.height()
                        / (float) imageHeight;


        if (faceHeightRatio
                < MIN_FACE_HEIGHT_RATIO) {

            return false;
        }


        // =====================================================
        // FACE POSITION
        // =====================================================

        float faceCenterX =
                bounds.centerX();

        float faceCenterY =
                bounds.centerY();


        float imageCenterX =
                imageWidth / 2f;

        float imageCenterY =
                imageHeight / 2f;


        float normalizedX =
                Math.abs(
                        faceCenterX
                                - imageCenterX
                ) / imageWidth;


        float normalizedY =
                Math.abs(
                        faceCenterY
                                - imageCenterY
                ) / imageHeight;


        return normalizedX <= CENTER_TOLERANCE
                && normalizedY <= CENTER_TOLERANCE;
    }


    // =========================================================
    // AUTOMATIC IMAGE CAPTURE
    // =========================================================

    private void captureImage() {

        /*
         * isProcessing has already been set to true by the
         * analyzer.
         */
        if (imageCapture == null) {

            isProcessing = false;

            Toast.makeText(
                    this,
                    R.string.camera_not_ready,
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        // =====================================================
        // CREATE TEMP IMAGE
        // =====================================================

        File photoFile =
                new File(
                        getCacheDir(),
                        "attendance_"
                                + System.currentTimeMillis()
                                + ".jpg"
                );


        ImageCapture.OutputFileOptions
                outputOptions =
                new ImageCapture
                        .OutputFileOptions
                        .Builder(photoFile)
                        .build();


        // =====================================================
        // CAPTURE ONE PHOTO
        // =====================================================

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {

                    @Override
                    public void onImageSaved(
                            @NonNull ImageCapture
                                    .OutputFileResults outputFileResults) {

                        pendingPhotoFile =
                                photoFile;


                        /*
                         * Continue to location and then upload.
                         */
                        getLocationAndSubmit();
                    }


                    @Override
                    public void onError(
                            @NonNull ImageCaptureException exception) {

                        isProcessing = false;


                        Toast.makeText(
                                CameraActivity.this,
                                R.string.image_capture_failed,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }


    // =========================================================
    // LOCATION + SUBMIT
    // =========================================================

    private void getLocationAndSubmit() {

        if (pendingPhotoFile == null
                || !pendingPhotoFile.exists()) {

            isProcessing = false;

            Toast.makeText(
                    this,
                    R.string.image_not_found,
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        // =====================================================
        // LOCATION PERMISSION
        // =====================================================

        if (!hasLocationPermission()) {

            /*
             * Keep the captured image.
             *
             * Once permission is granted, the upload can
             * continue from onRequestPermissionsResult().
             */
            requestLocationPermission();

            return;
        }


        // =====================================================
        // LOCATION / GPS CHECK
        // =====================================================

        if (!isLocationEnabled()) {

            /*
             * Keep processing state true because we still have
             * a captured photo waiting for location.
             */
            Toast.makeText(
                    this,
                    R.string.enable_location,
                    Toast.LENGTH_LONG
            ).show();


            Intent intent =
                    new Intent(
                            Settings
                                    .ACTION_LOCATION_SOURCE_SETTINGS
                    );


            startActivity(intent);

            return;
        }


        fetchCurrentLocation();
    }


    // =========================================================
    // CHECK GPS / NETWORK LOCATION
    // =========================================================

    private boolean isLocationEnabled() {

        LocationManager locationManager =
                (LocationManager)
                        getSystemService(
                                LOCATION_SERVICE
                        );


        if (locationManager == null) {

            return false;
        }


        boolean gpsEnabled = false;

        boolean networkEnabled = false;


        try {

            gpsEnabled =
                    locationManager
                            .isProviderEnabled(
                                    LocationManager
                                            .GPS_PROVIDER
                            );

        } catch (Exception ignored) {
        }


        try {

            networkEnabled =
                    locationManager
                            .isProviderEnabled(
                                    LocationManager
                                            .NETWORK_PROVIDER
                            );

        } catch (Exception ignored) {
        }


        return gpsEnabled
                || networkEnabled;
    }


    // =========================================================
    // GET CURRENT LOCATION
    // =========================================================

    private void fetchCurrentLocation() {

        if (!hasLocationPermission()) {

            requestLocationPermission();

            return;
        }


        CancellationTokenSource
                cancellationTokenSource =
                new CancellationTokenSource();


        try {

            fusedLocationClient
                    .getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            cancellationTokenSource
                                    .getToken()
                    )
                    .addOnSuccessListener(
                            location -> {

                                if (location == null) {

                                    isProcessing = false;

                                    Toast.makeText(
                                            CameraActivity.this,
                                            R.string.location_unavailable_message,
                                            Toast.LENGTH_LONG
                                    ).show();

                                    deletePendingPhoto();

                                    return;
                                }


                                double latitude =
                                        location.getLatitude();


                                double longitude =
                                        location.getLongitude();


                                uploadAttendance(
                                        pendingPhotoFile,
                                        latitude,
                                        longitude
                                );
                            }
                    )
                    .addOnFailureListener(
                            e -> {

                                isProcessing = false;

                                Toast.makeText(
                                        CameraActivity.this,
                                        R.string.location_failed,
                                        Toast.LENGTH_LONG
                                ).show();

                                deletePendingPhoto();
                            }
                    );

        } catch (SecurityException e) {

            requestLocationPermission();
        }
    }


    // =========================================================
    // UPLOAD ATTENDANCE
    // =========================================================

    private void uploadAttendance(
            File imageFile,
            double latitude,
            double longitude) {

        if (imageFile == null
                || !imageFile.exists()) {

            isProcessing = false;

            Toast.makeText(
                    this,
                    R.string.image_not_found,
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        // =====================================================
        // SESSION CHECK
        // =====================================================

        if (!sessionManager.isLoggedIn()) {

            handleSessionExpired();

            return;
        }


        // =====================================================
        // MULTIPART IMAGE
        // =====================================================

        MediaType imageMediaType =
                MediaType.parse(
                        "image/jpeg"
                );


        MediaType textMediaType =
                MediaType.parse(
                        "text/plain"
                );


        RequestBody imageRequestBody =
                RequestBody.create(
                        imageFile,
                        imageMediaType
                );


        MultipartBody.Part filePart =
                MultipartBody
                        .Part
                        .createFormData(
                                "file",
                                imageFile.getName(),
                                imageRequestBody
                        );


        // =====================================================
        // LATITUDE
        // =====================================================

        RequestBody latitudeBody =
                RequestBody.create(
                        String.valueOf(latitude),
                        textMediaType
                );


        // =====================================================
        // LONGITUDE
        // =====================================================

        RequestBody longitudeBody =
                RequestBody.create(
                        String.valueOf(longitude),
                        textMediaType
                );


        // =====================================================
        // SELECT API
        // =====================================================

        Call<AttendanceMarkResponse> call;


        if (ACTION_CHECK_IN.equals(
                attendanceAction)) {

            call =
                    apiService.checkIn(
                            filePart,
                            latitudeBody,
                            longitudeBody
                    );

        } else {

            call =
                    apiService.checkOut(
                            filePart,
                            latitudeBody,
                            longitudeBody
                    );
        }


        // =====================================================
        // API CALL
        // =====================================================

        call.enqueue(
                new Callback<>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<AttendanceMarkResponse> call,
                            @NonNull Response<AttendanceMarkResponse>
                                    response) {

                        isProcessing = false;


                        // =====================================
                        // SUCCESS
                        // =====================================

                        if (response.isSuccessful()
                                && response.body() != null) {

                            AttendanceMarkResponse result =
                                    response.body();


                            /*
                             * Backend should return exactly:
                             *
                             * Spoof Detected
                             * Unregistered
                             * USERNAME Successfully Registered
                             */
                            String message =
                                    result.getMessage();


                            /*
                             * Fallback if backend does not return
                             * a message.
                             */
                            if (message == null
                                    || message.trim().isEmpty()) {

                                String username =
                                        result.getUsername();


                                if (username == null
                                        || username.trim().isEmpty()) {

                                    username =
                                            sessionManager
                                                    .getUsername();
                                }


                                if (username == null
                                        || username.trim().isEmpty()) {

                                    username = "User";
                                }


                                message =
                                        username
                                                + " Successfully Registered";
                            }


                            Toast.makeText(
                                    CameraActivity.this,
                                    message,
                                    Toast.LENGTH_LONG
                            ).show();


                            deletePendingPhoto();

                            finish();

                            return;
                        }


                        // =====================================
                        // SESSION EXPIRED
                        // =====================================

                        if (response.code() == 401) {

                            handleSessionExpired();

                            return;
                        }


                        // =====================================
                        // FORBIDDEN
                        // =====================================

                        if (response.code() == 403) {

                            Toast.makeText(
                                    CameraActivity.this,
                                    R.string.attendance_not_allowed,
                                    Toast.LENGTH_LONG
                            ).show();

                            deletePendingPhoto();

                            return;
                        }


                        // =====================================
                        // OTHER ERROR
                        // =====================================

                        Toast.makeText(
                                CameraActivity.this,
                                getApiErrorMessage(response),
                                Toast.LENGTH_LONG
                        ).show();


                        deletePendingPhoto();
                    }


                    @Override
                    public void onFailure(
                            @NonNull Call<AttendanceMarkResponse> call,
                            @NonNull Throwable t) {

                        isProcessing = false;


                        Toast.makeText(
                                CameraActivity.this,
                                R.string.server_connection_failed,
                                Toast.LENGTH_LONG
                        ).show();


                        deletePendingPhoto();
                    }
                }
        );
    }


    // =========================================================
    // API ERROR MESSAGE
    // =========================================================

    private String getApiErrorMessage(
            Response<AttendanceMarkResponse> response) {

        if (response.code() == 401) {

            return getString(
                    R.string.session_expired
            );
        }


        if (response.code() == 403) {

            return getString(
                    R.string.attendance_not_allowed
            );
        }


        ResponseBody errorBody =
                response.errorBody();


        if (errorBody != null) {

            try (ResponseBody body = errorBody) {

                String errorMessage =
                        body.string();


                if (!errorMessage
                        .trim()
                        .isEmpty()) {

                    return extractBackendMessage(
                            errorMessage
                    );
                }

            } catch (Exception ignored) {
            }
        }


        return getString(
                R.string.attendance_failed_with_code,
                response.code()
        );
    }


    // =========================================================
    // EXTRACT BACKEND MESSAGE
    // =========================================================

    private String extractBackendMessage(
            String errorResponse) {

        String messageKey =
                "\"message\"";


        int messageIndex =
                errorResponse.indexOf(
                        messageKey
                );


        if (messageIndex >= 0) {

            int colonIndex =
                    errorResponse.indexOf(
                            ":",
                            messageIndex
                    );


            if (colonIndex >= 0) {

                int firstQuote =
                        errorResponse.indexOf(
                                "\"",
                                colonIndex + 1
                        );


                int secondQuote =
                        errorResponse.indexOf(
                                "\"",
                                firstQuote + 1
                        );


                if (firstQuote >= 0
                        && secondQuote > firstQuote) {

                    String message =
                            errorResponse.substring(
                                    firstQuote + 1,
                                    secondQuote
                            );


                    if (!message
                            .trim()
                            .isEmpty()) {

                        return message;
                    }
                }
            }
        }


        /*
         * Backend returned plain text.
         */
        if (!errorResponse
                .trim()
                .startsWith("{")) {

            return errorResponse.trim();
        }


        return getString(
                R.string.attendance_failed
        );
    }


    // =========================================================
    // SESSION EXPIRED
    // =========================================================

    private void handleSessionExpired() {

        isProcessing = false;


        deletePendingPhoto();


        sessionManager.logout();


        Toast.makeText(
                this,
                R.string.session_expired,
                Toast.LENGTH_LONG
        ).show();


        openLoginScreen();
    }


    private void openLoginScreen() {

        Intent intent =
                new Intent(
                        CameraActivity.this,
                        LoginActivity.class
                );


        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );


        startActivity(intent);

        finish();
    }


    // =========================================================
    // PERMISSION RESULT
    // =========================================================

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );


        // =====================================================
        // CAMERA
        // =====================================================

        if (requestCode ==
                CAMERA_PERMISSION_REQUEST) {

            if (hasCameraPermission()) {

                startCamera();

            } else {

                Toast.makeText(
                        this,
                        R.string.camera_permission_required,
                        Toast.LENGTH_LONG
                ).show();

                finish();
            }

            return;
        }


        // =====================================================
        // LOCATION
        // =====================================================

        if (requestCode ==
                LOCATION_PERMISSION_REQUEST) {

            if (hasLocationPermission()) {

                if (pendingPhotoFile != null
                        && pendingPhotoFile.exists()) {

                    isProcessing = true;

                    getLocationAndSubmit();
                }

            } else {

                isProcessing = false;

                deletePendingPhoto();


                Toast.makeText(
                        this,
                        R.string.location_permission_required,
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }


    // =========================================================
    // DELETE TEMP IMAGE
    // =========================================================

    private void deletePendingPhoto() {

        if (pendingPhotoFile != null
                && pendingPhotoFile.exists()) {

            pendingPhotoFile.delete();
        }


        pendingPhotoFile = null;
    }


    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        /*
         * Prevent any new capture or processing.
         */
        isProcessing = true;


        deletePendingPhoto();


        // =====================================================
        // STOP IMAGE ANALYZER
        // =====================================================

        if (imageAnalysis != null) {

            imageAnalysis.clearAnalyzer();
        }


        // =====================================================
        // STOP CAMERA
        // =====================================================

        if (cameraProvider != null) {

            cameraProvider.unbindAll();
        }


        // =====================================================
        // CLOSE ML KIT
        // =====================================================

        if (faceDetector != null) {

            faceDetector.close();
        }


        // =====================================================
        // STOP ANALYSIS THREAD
        // =====================================================

        if (analysisExecutor != null) {

            analysisExecutor.shutdown();
        }


        super.onDestroy();
    }
}

