package com.college.attendance.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "ATTENDANCE_DEBUG";

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
    // VIEWS
    // =========================================================

    private PreviewView previewView;
    private ImageView capturedImageView;
    private ImageButton btnBack;
    private TextView tvCameraTitle;
    private com.google.android.material.button.MaterialButton btnCapture;

    // =========================================================
    // CAMERA
    // =========================================================

    private ImageCapture imageCapture;
    private ProcessCameraProvider cameraProvider;

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
    private File pendingUploadFile;

    private boolean isProcessing = false;

    private android.view.View loadingOverlay;
    private TextView tvLoadingMessage;


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_camera);

        Log.d(TAG, "======================================");
        Log.d(TAG, "CameraActivity CREATED");
        Log.d(TAG, "======================================");

        // =====================================================
        // INITIALIZE VIEWS
        // =====================================================

        initializeViews();

        // =====================================================
        // SESSION
        // =====================================================

        sessionManager = new SessionManager(this);

        Log.d(
                TAG,
                "Logged In = " + sessionManager.isLoggedIn()
        );

        Log.d(
                TAG,
                "Username = " + sessionManager.getUsername()
        );

        Log.d(
                TAG,
                "Role = " + sessionManager.getRole()
        );

        Log.d(
                TAG,
                "Token Present = " + isTokenPresent()
        );

        if (!sessionManager.isLoggedIn()) {

            Log.e(TAG, "SESSION NOT FOUND");

            openLoginScreen();

            return;
        }

        // =====================================================
        // API
        // =====================================================

        apiService = ApiClient.getApiService(this);

        // =====================================================
        // LOCATION
        // =====================================================

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        // =====================================================
        // GET ATTENDANCE ACTION
        // =====================================================

        attendanceAction =
                getIntent().getStringExtra(EXTRA_ACTION);

        Log.d(
                TAG,
                "Attendance Action = " + attendanceAction
        );

        // =====================================================
        // VALIDATE ACTION
        // =====================================================

        if (!ACTION_CHECK_IN.equals(attendanceAction)
                && !ACTION_CHECK_OUT.equals(attendanceAction)) {

            Log.e(
                    TAG,
                    "INVALID ATTENDANCE ACTION = "
                            + attendanceAction
            );

            Toast.makeText(
                    this,
                    R.string.invalid_attendance_action,
                    Toast.LENGTH_LONG
            ).show();

            finish();

            return;
        }

        // =====================================================
        // SCREEN TEXT
        // =====================================================

        setupScreenText();

        // =====================================================
        // CAMERA PERMISSION
        // =====================================================

        if (hasCameraPermission()) {

            Log.d(
                    TAG,
                    "Camera permission = GRANTED"
            );

            startCamera();

        } else {

            Log.d(
                    TAG,
                    "Camera permission = NOT GRANTED"
            );

            requestCameraPermission();
        }

        // =====================================================
        // BACK BUTTON
        // =====================================================

        btnBack.setOnClickListener(v -> {

            Log.d(
                    TAG,
                    "Back button clicked. isProcessing = "
                            + isProcessing
            );

            if (!isProcessing) {

                deletePendingPhoto();

                finish();

            } else {

                Log.d(
                        TAG,
                        "Back ignored because processing is running"
                );
            }
        });

        // =====================================================
        // MANUAL CAPTURE BUTTON
        // =====================================================

        btnCapture.setOnClickListener(v -> {

            Log.d(
                    TAG,
                    "======================================"
            );

            Log.d(
                    TAG,
                    "CAPTURE BUTTON CLICKED"
            );

            Log.d(
                    TAG,
                    "isProcessing = " + isProcessing
            );

            Log.d(
                    TAG,
                    "imageCapture null = "
                            + (imageCapture == null)
            );

            Log.d(
                    TAG,
                    "======================================"
            );

            if (!isProcessing) {

                captureImage();

            } else {

                Log.d(
                        TAG,
                        "Capture ignored because processing is already running"
                );
            }
        });
    }


    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initializeViews() {

        previewView =
                findViewById(R.id.previewView);

        capturedImageView =
                findViewById(R.id.capturedImageView);

        btnBack =
                findViewById(R.id.btnBack);

        tvCameraTitle =
                findViewById(R.id.tvCameraTitle);

        btnCapture =
                findViewById(R.id.btnCapture);

        loadingOverlay =
                findViewById(R.id.loadingOverlay);

        tvLoadingMessage =
                findViewById(R.id.tvLoadingMessage);

        Log.d(TAG, "Views initialized");
    }


    // =========================================================
    // ATTENDANCE PROCESSING LOADER
    // =========================================================

    private void showLoading(String message) {

        if (tvLoadingMessage != null
                && message != null) {

            tvLoadingMessage.setText(message);
        }

        if (loadingOverlay != null) {

            loadingOverlay.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {

        if (loadingOverlay != null) {

            loadingOverlay.setVisibility(View.GONE);
        }
    }


    // =========================================================
    // SCREEN TEXT
    // =========================================================

    private void setupScreenText() {

        if (ACTION_CHECK_IN.equals(attendanceAction)) {

            tvCameraTitle.setText(
                    R.string.check_in
            );

            Log.d(
                    TAG,
                    "Screen = CHECK-IN"
            );

        } else {

            tvCameraTitle.setText(
                    R.string.check_out
            );

            Log.d(
                    TAG,
                    "Screen = CHECK-OUT"
            );
        }
    }


    // =========================================================
    // TOKEN CHECK
    // =========================================================

    private boolean isTokenPresent() {

        if (sessionManager == null) {

            return false;
        }

        String token =
                sessionManager.getAccessToken();

        return token != null
                && !token.trim().isEmpty();
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

        Log.d(
                TAG,
                "Requesting location permission"
        );

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

        Log.d(
                TAG,
                "Starting camera..."
        );

        ListenableFuture<ProcessCameraProvider>
                cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(
                () -> {

                    try {

                        cameraProvider =
                                cameraProviderFuture.get();

                        Log.d(
                                TAG,
                                "CameraProvider initialized"
                        );

                        bindCamera(cameraProvider);

                    } catch (ExecutionException e) {

                        Log.e(
                                TAG,
                                "CameraProvider ExecutionException",
                                e
                        );

                        showCameraStartError();

                    } catch (InterruptedException e) {

                        Thread.currentThread().interrupt();

                        Log.e(
                                TAG,
                                "CameraProvider InterruptedException",
                                e
                        );

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

        Log.d(
                TAG,
                "Binding camera..."
        );

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

        imageCapture =
                new ImageCapture.Builder()
                        .setCaptureMode(
                                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                        )
                        .build();

        Log.d(
                TAG,
                "ImageCapture initialized = "
                        + (imageCapture != null)
        );

        // =====================================================
        // CAMERA
        // =====================================================

        CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

        try {

            provider.unbindAll();

            provider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
            );

            Log.d(
                    TAG,
                    "BACK CAMERA BOUND SUCCESSFULLY"
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "CAMERA BIND FAILED",
                    e
            );

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
    // MANUAL CAPTURE
    // =========================================================

    private void captureImage() {

        Log.d(
                TAG,
                "--------------------------------------"
        );

        Log.d(
                TAG,
                "captureImage() STARTED"
        );

        // =====================================================
        // CHECK PROCESSING
        // =====================================================

        if (isProcessing) {

            Log.d(
                    TAG,
                    "Capture cancelled: already processing"
            );

            return;
        }

        // =====================================================
        // CHECK CAMERA
        // =====================================================

        if (imageCapture == null) {

            Log.e(
                    TAG,
                    "Capture failed: imageCapture is NULL"
            );

            Toast.makeText(
                    this,
                    R.string.camera_not_ready,
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // =====================================================
        // START PROCESSING
        // =====================================================

        isProcessing = true;

        showLoading("Capturing photo...");

        btnCapture.setEnabled(false);

        Log.d(
                TAG,
                "Capture processing started"
        );

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

        Log.d(
                TAG,
                "Photo path = "
                        + photoFile.getAbsolutePath()
        );

        ImageCapture.OutputFileOptions
                outputOptions =
                new ImageCapture.OutputFileOptions
                        .Builder(photoFile)
                        .build();

        // =====================================================
        // TAKE PHOTO
        // =====================================================

        Log.d(
                TAG,
                "Calling imageCapture.takePicture()"
        );

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {

                    @Override
                    public void onImageSaved(
                            @NonNull ImageCapture.OutputFileResults
                                    outputFileResults) {

                        Log.d(
                                TAG,
                                "======================================"
                        );

                        Log.d(
                                TAG,
                                "PHOTO CAPTURE SUCCESS"
                        );

                        Log.d(
                                TAG,
                                "Photo path = "
                                        + photoFile.getAbsolutePath()
                        );

                        Log.d(
                                TAG,
                                "Photo exists = "
                                        + photoFile.exists()
                        );

                        Log.d(
                                TAG,
                                "Photo size = "
                                        + photoFile.length()
                                        + " bytes"
                        );

                        Log.d(
                                TAG,
                                "======================================"
                        );

                        // =================================================
                        // SAVE PENDING PHOTO
                        // =================================================

                        pendingPhotoFile =
                                photoFile;

                        // =================================================
                        // FREEZE CAMERA
                        // =================================================

                        freezeCameraWithCapturedImage(
                                photoFile
                        );

                        // =================================================
                        // LOCATION + API
                        // =================================================

                        showLoading("Fetching location...");

                        getLocationAndSubmit();
                    }


                    @Override
                    public void onError(
                            @NonNull ImageCaptureException
                                    exception) {

                        Log.e(
                                TAG,
                                "======================================"
                        );

                        Log.e(
                                TAG,
                                "PHOTO CAPTURE FAILED"
                        );

                        Log.e(
                                TAG,
                                "Camera error code = "
                                        + exception
                                        .getImageCaptureError()
                        );

                        Log.e(
                                TAG,
                                "Camera error message = "
                                        + exception.getMessage(),
                                exception
                        );

                        Log.e(
                                TAG,
                                "======================================"
                        );

                        isProcessing = false;

                        hideLoading();

                        btnCapture.setEnabled(true);

                        Toast.makeText(
                                CameraActivity.this,
                                "Image capture failed: "
                                        + exception.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                        deletePendingPhoto();
                    }
                }
        );
    }


    // =========================================================
    // FREEZE CAMERA + SHOW CAPTURED IMAGE
    // =========================================================

    private void freezeCameraWithCapturedImage(
            File photoFile) {

        Log.d(
                TAG,
                "======================================"
        );

        Log.d(
                TAG,
                "FREEZING CAMERA"
        );

        Log.d(
                TAG,
                "======================================"
        );

        // =====================================================
        // STOP LIVE CAMERA
        // =====================================================

        if (cameraProvider != null) {

            try {

                cameraProvider.unbindAll();

                Log.d(
                        TAG,
                        "Camera unbound successfully"
                );

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Failed to unbind camera",
                        e
                );
            }
        }

        // =====================================================
        // DISABLE IMAGE CAPTURE
        // =====================================================

        imageCapture = null;

        // =====================================================
        // SHOW CAPTURED IMAGE
        // =====================================================

        if (capturedImageView != null
                && photoFile != null
                && photoFile.exists()) {

            Uri imageUri =
                    Uri.fromFile(photoFile);

            capturedImageView.setImageURI(
                    imageUri
            );

            capturedImageView.setVisibility(
                    View.VISIBLE
            );

            Log.d(
                    TAG,
                    "Captured image displayed"
            );
        }

        // =====================================================
        // HIDE LIVE CAMERA
        // =====================================================

        if (previewView != null) {

            previewView.setVisibility(
                    View.GONE
            );
        }

        // =====================================================
        // HIDE FACE GUIDE
        // =====================================================

        View faceGuide =
                findViewById(R.id.faceGuide);

        if (faceGuide != null) {

            faceGuide.setVisibility(
                    View.GONE
            );
        }

        // =====================================================
        // KEEP CAPTURE BUTTON DISABLED
        // =====================================================

        if (btnCapture != null) {

            btnCapture.setEnabled(false);
        }

        Log.d(
                TAG,
                "Camera is now FROZEN"
        );
    }


    // =========================================================
    // LOCATION + SUBMIT
    // =========================================================

    private void getLocationAndSubmit() {

        Log.d(
                TAG,
                "getLocationAndSubmit()"
        );

        if (pendingPhotoFile == null
                || !pendingPhotoFile.exists()) {

            Log.e(
                    TAG,
                    "Pending photo does not exist"
            );

            isProcessing = false;

            hideLoading();

            Toast.makeText(
                    this,
                    R.string.image_not_found,
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        Log.d(
                TAG,
                "Pending photo exists"
        );

        // =====================================================
        // LOCATION PERMISSION
        // =====================================================

        if (!hasLocationPermission()) {

            Log.d(
                    TAG,
                    "Location permission NOT granted"
            );

            requestLocationPermission();

            return;
        }

        Log.d(
                TAG,
                "Location permission GRANTED"
        );

        // =====================================================
        // LOCATION ENABLED
        // =====================================================

        if (!isLocationEnabled()) {

            Log.e(
                    TAG,
                    "Device location is DISABLED"
            );

            Toast.makeText(
                    this,
                    R.string.enable_location,
                    Toast.LENGTH_LONG
            ).show();

            Intent intent =
                    new Intent(
                            Settings.ACTION_LOCATION_SOURCE_SETTINGS
                    );

            startActivity(intent);

            return;
        }

        Log.d(
                TAG,
                "Device location is ENABLED"
        );

        // =====================================================
        // GET LOCATION
        // =====================================================

        fetchCurrentLocation();
    }


    // =========================================================
    // CHECK LOCATION ENABLED
    // =========================================================

    private boolean isLocationEnabled() {

        LocationManager locationManager =
                (LocationManager)
                        getSystemService(
                                LOCATION_SERVICE
                        );

        if (locationManager == null) {

            Log.e(
                    TAG,
                    "LocationManager is NULL"
            );

            return false;
        }

        boolean gpsEnabled = false;
        boolean networkEnabled = false;

        try {

            gpsEnabled =
                    locationManager.isProviderEnabled(
                            LocationManager.GPS_PROVIDER
                    );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "GPS provider check failed",
                    e
            );
        }

        try {

            networkEnabled =
                    locationManager.isProviderEnabled(
                            LocationManager.NETWORK_PROVIDER
                    );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Network provider check failed",
                    e
            );
        }

        Log.d(
                TAG,
                "GPS Enabled = " + gpsEnabled
        );

        Log.d(
                TAG,
                "Network Enabled = " + networkEnabled
        );

        return gpsEnabled || networkEnabled;
    }


    // =========================================================
    // GET CURRENT LOCATION
    // =========================================================

    private void fetchCurrentLocation() {

        Log.d(
                TAG,
                "fetchCurrentLocation() STARTED"
        );

        if (!hasLocationPermission()) {

            Log.e(
                    TAG,
                    "Location permission missing"
            );

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

                                    Log.e(
                                            TAG,
                                            "Location returned NULL"
                                    );

                                    isProcessing = false;

                                    hideLoading();

                                    Toast.makeText(
                                            CameraActivity.this,
                                            R.string.location_unavailable_message,
                                            Toast.LENGTH_LONG
                                    ).show();

                                    deletePendingPhoto();

                                    return;
                                }

                                // =============================================
                                // LOCATION VALUES
                                // =============================================

                                double latitude =
                                        location.getLatitude();

                                double longitude =
                                        location.getLongitude();

                                float accuracy =
                                        location.getAccuracy();

                                Log.d(
                                        TAG,
                                        "======================================"
                                );

                                Log.d(
                                        TAG,
                                        "LOCATION SUCCESS"
                                );

                                Log.d(
                                        TAG,
                                        "Latitude = "
                                                + latitude
                                );

                                Log.d(
                                        TAG,
                                        "Longitude = "
                                                + longitude
                                );

                                Log.d(
                                        TAG,
                                        "Accuracy = "
                                                + accuracy
                                                + " meters"
                                );

                                Log.d(
                                        TAG,
                                        "======================================"
                                );

                                // =============================================
                                // UPLOAD
                                // =============================================

                                uploadAttendance(
                                        pendingPhotoFile,
                                        latitude,
                                        longitude
                                );
                            }
                    )
                    .addOnFailureListener(
                            e -> {

                                Log.e(
                                        TAG,
                                        "LOCATION FAILED",
                                        e
                                );

                                isProcessing = false;

                                hideLoading();

                                Toast.makeText(
                                        CameraActivity.this,
                                        "Location failed: "
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();

                                deletePendingPhoto();
                            }
                    );

        } catch (SecurityException e) {

            Log.e(
                    TAG,
                    "Location SecurityException",
                    e
            );

            isProcessing = false;

            hideLoading();

            requestLocationPermission();
        }
    }


    // =========================================================
    // COMPRESS IMAGE BEFORE UPLOAD
    //
    // Resizes to a max dimension and re-encodes as JPEG so the
    // upload is fast even on slow/local WiFi. Falls back to the
    // original file if compression fails for any reason.
    // =========================================================

    private static final int UPLOAD_MAX_DIMENSION = 800;
    private static final int UPLOAD_JPEG_QUALITY = 80;

    private File compressForUpload(File originalFile) {

        try {

            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(
                    originalFile.getAbsolutePath(),
                    boundsOptions
            );

            int sourceWidth = boundsOptions.outWidth;
            int sourceHeight = boundsOptions.outHeight;

            if (sourceWidth <= 0 || sourceHeight <= 0) {
                return originalFile;
            }

            int longestSide = Math.max(sourceWidth, sourceHeight);

            int sampleSize = 1;
            while ((longestSide / sampleSize) > UPLOAD_MAX_DIMENSION * 2) {
                sampleSize *= 2;
            }

            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = sampleSize;

            Bitmap sampledBitmap = BitmapFactory.decodeFile(
                    originalFile.getAbsolutePath(),
                    decodeOptions
            );

            if (sampledBitmap == null) {
                return originalFile;
            }

            int width = sampledBitmap.getWidth();
            int height = sampledBitmap.getHeight();
            int longestSampledSide = Math.max(width, height);

            Bitmap finalBitmap = sampledBitmap;

            if (longestSampledSide > UPLOAD_MAX_DIMENSION) {

                float scale = UPLOAD_MAX_DIMENSION / (float) longestSampledSide;

                finalBitmap = Bitmap.createScaledBitmap(
                        sampledBitmap,
                        Math.max(1, Math.round(width * scale)),
                        Math.max(1, Math.round(height * scale)),
                        true
                );
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            finalBitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    UPLOAD_JPEG_QUALITY,
                    outputStream
            );

            File compressedFile = new File(
                    originalFile.getParentFile(),
                    "upload_" + originalFile.getName()
            );

            try (FileOutputStream fileOutputStream =
                         new FileOutputStream(compressedFile)) {
                fileOutputStream.write(outputStream.toByteArray());
            }

            return compressedFile;

        } catch (IOException | OutOfMemoryError e) {

            Log.e(
                    TAG,
                    "Image compression failed, uploading original file",
                    e
            );

            return originalFile;
        }
    }

    // =========================================================
    // UPLOAD ATTENDANCE
    // =========================================================

    private void uploadAttendance(
            File imageFile,
            double latitude,
            double longitude) {

        Log.d(
                TAG,
                "======================================"
        );

        Log.d(
                TAG,
                "uploadAttendance() STARTED"
        );

        // =====================================================
        // IMAGE VALIDATION
        // =====================================================

        if (imageFile == null) {

            Log.e(
                    TAG,
                    "Image file = NULL"
            );

            isProcessing = false;

            hideLoading();

            return;
        }

        if (!imageFile.exists()) {

            Log.e(
                    TAG,
                    "Image file does NOT exist"
            );

            Log.e(
                    TAG,
                    "Image path = "
                            + imageFile.getAbsolutePath()
            );

            isProcessing = false;

            hideLoading();

            Toast.makeText(
                    this,
                    R.string.image_not_found,
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // =====================================================
        // SESSION
        // =====================================================

        String username =
                sessionManager.getUsername();

        String role =
                sessionManager.getRole();

        boolean tokenPresent =
                isTokenPresent();

        // =====================================================
        // REQUEST DEBUG VALUES
        // =====================================================

        Log.d(
                TAG,
                "---------- REQUEST VALUES ----------"
        );

        Log.d(
                TAG,
                "Action = " + attendanceAction
        );

        Log.d(
                TAG,
                "Username = " + username
        );

        Log.d(
                TAG,
                "Role = " + role
        );

        Log.d(
                TAG,
                "Token Present = " + tokenPresent
        );

        Log.d(
                TAG,
                "Image Path = "
                        + imageFile.getAbsolutePath()
        );

        Log.d(
                TAG,
                "Image Name = "
                        + imageFile.getName()
        );

        Log.d(
                TAG,
                "Image Exists = "
                        + imageFile.exists()
        );

        Log.d(
                TAG,
                "Image Size = "
                        + imageFile.length()
                        + " bytes"
        );

        Log.d(
                TAG,
                "Latitude = " + latitude
        );

        Log.d(
                TAG,
                "Longitude = " + longitude
        );

        Log.d(
                TAG,
                "------------------------------------"
        );

        // =====================================================
        // TOKEN
        // =====================================================

        if (!tokenPresent) {

            Log.e(
                    TAG,
                    "TOKEN NOT PRESENT"
            );

            handleSessionExpired();

            return;
        }

        // =====================================================
        // IMAGE REQUEST BODY
        // =====================================================

        MediaType imageMediaType =
                MediaType.parse(
                        "image/jpeg"
                );

        MediaType textMediaType =
                MediaType.parse(
                        "text/plain"
                );

        // Full-resolution camera photos can be several MB; downscaling +
        // recompressing before upload cuts upload time and face-service
        // processing time without hurting recognition accuracy (the face
        // service downscales further anyway before detection).
        File uploadFile = compressForUpload(imageFile);
        pendingUploadFile = (uploadFile != imageFile) ? uploadFile : null;

        RequestBody imageRequestBody =
                RequestBody.create(
                        uploadFile,
                        imageMediaType
                );

        MultipartBody.Part filePart =
                MultipartBody.Part.createFormData(
                        "file",
                        uploadFile.getName(),
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

            Log.d(
                    TAG,
                    "API SELECTED = CHECK-IN"
            );

            call =
                    apiService.checkIn(
                            filePart,
                            latitudeBody,
                            longitudeBody
                    );

        } else {

            Log.d(
                    TAG,
                    "API SELECTED = CHECK-OUT"
            );

            call =
                    apiService.checkOut(
                            filePart,
                            latitudeBody,
                            longitudeBody
                    );
        }

        Log.d(
                TAG,
                "API CALL CREATED"
        );

        showLoading("Verifying face...");

        // =====================================================
        // API CALL
        // =====================================================

        call.enqueue(
                new Callback<AttendanceMarkResponse>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<AttendanceMarkResponse> call,
                            @NonNull Response<AttendanceMarkResponse>
                                    response) {

                        Log.d(
                                TAG,
                                "======================================"
                        );

                        Log.d(
                                TAG,
                                "API RESPONSE RECEIVED"
                        );

                        Log.d(
                                TAG,
                                "HTTP Code = "
                                        + response.code()
                        );

                        Log.d(
                                TAG,
                                "HTTP Message = "
                                        + response.message()
                        );

                        Log.d(
                                TAG,
                                "Successful = "
                                        + response.isSuccessful()
                        );

                        // =================================================
                        // SUCCESS
                        // =================================================

                        if (response.isSuccessful()
                                && response.body() != null) {

                            AttendanceMarkResponse result =
                                    response.body();

                            Log.d(
                                    TAG,
                                    "---------- SUCCESS RESPONSE ----------"
                            );

                            Log.d(
                                    TAG,
                                    "success = "
                                            + result.isSuccess()
                            );

                            Log.d(
                                    TAG,
                                    "action = "
                                            + result.getAction()
                            );

                            Log.d(
                                    TAG,
                                    "message = "
                                            + result.getMessage()
                            );

                            Log.d(
                                    TAG,
                                    "userId = "
                                            + result.getUserId()
                            );

                            Log.d(
                                    TAG,
                                    "username = "
                                            + result.getUsername()
                            );

                            Log.d(
                                    TAG,
                                    "attendanceDate = "
                                            + result.getAttendanceDate()
                            );

                            Log.d(
                                    TAG,
                                    "checkInTime = "
                                            + result.getCheckInTime()
                            );

                            Log.d(
                                    TAG,
                                    "checkOutTime = "
                                            + result.getCheckOutTime()
                            );

                            Log.d(
                                    TAG,
                                    "status = "
                                            + result.getStatus()
                            );

                            Log.d(
                                    TAG,
                                    "confidence = "
                                            + result.getConfidence()
                            );

                            Log.d(
                                    TAG,
                                    "distance = "
                                            + result.getDistance()
                            );

                            Log.d(
                                    TAG,
                                    "threshold = "
                                            + result.getThreshold()
                            );

                            Log.d(
                                    TAG,
                                    "latitude = "
                                            + result.getLatitude()
                            );

                            Log.d(
                                    TAG,
                                    "longitude = "
                                            + result.getLongitude()
                            );

                            Log.d(
                                    TAG,
                                    "source = "
                                            + result.getSource()
                            );

                            Log.d(
                                    TAG,
                                    "---------------------------------------"
                            );

                            isProcessing = false;

                            hideLoading();

                            String message =
                                    result.getMessage();

                            if (message == null
                                    || message.trim().isEmpty()) {

                                String resultUsername =
                                        result.getUsername();

                                if (resultUsername == null
                                        || resultUsername
                                        .trim()
                                        .isEmpty()) {

                                    resultUsername =
                                            sessionManager
                                                    .getUsername();
                                }

                                if (resultUsername == null
                                        || resultUsername
                                        .trim()
                                        .isEmpty()) {

                                    resultUsername = "User";
                                }

                                message =
                                        resultUsername
                                                + " Successfully Registered";
                            }

                            Toast.makeText(
                                    CameraActivity.this,
                                    message,
                                    Toast.LENGTH_LONG
                            ).show();

                            deletePendingPhoto();

                            Log.d(
                                    TAG,
                                    "Attendance SUCCESS"
                            );

                            Log.d(
                                    TAG,
                                    "Closing CameraActivity..."
                            );

                            new android.os.Handler(
                                    getMainLooper()
                            ).postDelayed(
                                    CameraActivity.this::finish,
                                    1200
                            );

                            return;
                        }

                        // =================================================
                        // ERROR BODY
                        // =================================================

                        String errorResponse = null;

                        ResponseBody errorBody =
                                response.errorBody();

                        if (errorBody != null) {

                            try {

                                errorResponse =
                                        errorBody.string();

                                Log.e(
                                        TAG,
                                        "---------- ERROR BODY ----------"
                                );

                                Log.e(
                                        TAG,
                                        errorResponse
                                );

                                Log.e(
                                        TAG,
                                        "--------------------------------"
                                );

                            } catch (Exception e) {

                                Log.e(
                                        TAG,
                                        "Unable to read error body",
                                        e
                                );
                            }
                        }

                        // =================================================
                        // RESET PROCESSING
                        // =================================================

                        isProcessing = false;

                        hideLoading();

                        // =================================================
                        // 401
                        // =================================================

                        if (response.code() == 401) {

                            Log.e(
                                    TAG,
                                    "HTTP 401 - SESSION EXPIRED"
                            );

                            handleSessionExpired();

                            return;
                        }

                        // =================================================
                        // 403
                        // =================================================

                        if (response.code() == 403) {

                            Log.e(
                                    TAG,
                                    "HTTP 403 - ACCESS DENIED"
                            );

                            String message =
                                    extractBackendMessage(
                                            errorResponse
                                    );

                            Toast.makeText(
                                    CameraActivity.this,
                                    message,
                                    Toast.LENGTH_LONG
                            ).show();

                            deletePendingPhoto();

                            return;
                        }

                        // =================================================
                        // OTHER ERROR
                        // =================================================

                        Log.e(
                                TAG,
                                "HTTP ERROR = "
                                        + response.code()
                        );

                        String errorMessage =
                                extractBackendMessage(
                                        errorResponse
                                );

                        Toast.makeText(
                                CameraActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();

                        deletePendingPhoto();

                        Log.d(
                                TAG,
                                "======================================"
                        );
                    }


                    @Override
                    public void onFailure(
                            @NonNull Call<AttendanceMarkResponse> call,
                            @NonNull Throwable t) {

                        isProcessing = false;

                        hideLoading();

                        Log.e(
                                TAG,
                                "======================================"
                        );

                        Log.e(
                                TAG,
                                "API CALL FAILED"
                        );

                        Log.e(
                                TAG,
                                "Error Type = "
                                        + t.getClass().getName()
                        );

                        Log.e(
                                TAG,
                                "Error Message = "
                                        + t.getMessage(),
                                t
                        );

                        Log.e(
                                TAG,
                                "======================================"
                        );

                        Toast.makeText(
                                CameraActivity.this,
                                "Server connection failed: "
                                        + t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                        deletePendingPhoto();
                    }
                }
        );
    }


    // =========================================================
    // EXTRACT BACKEND MESSAGE
    // =========================================================

    private String extractBackendMessage(
            String errorResponse) {

        if (errorResponse == null
                || errorResponse.trim().isEmpty()) {

            return "Attendance failed.";
        }

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

                    if (!message.trim().isEmpty()) {

                        return message;
                    }
                }
            }
        }

        // =====================================================
        // PLAIN TEXT RESPONSE
        // =====================================================

        String plainMessage =
                errorResponse.trim();

        if (!plainMessage.isEmpty()
                && !plainMessage.startsWith("{")) {

            return plainMessage;
        }

        return "Attendance failed.";
    }


    // =========================================================
    // SESSION EXPIRED
    // =========================================================

    private void handleSessionExpired() {

        Log.e(
                TAG,
                "Handling session expired"
        );

        isProcessing = false;

        hideLoading();

        deletePendingPhoto();

        sessionManager.logout();

        Toast.makeText(
                this,
                R.string.session_expired,
                Toast.LENGTH_LONG
        ).show();

        openLoginScreen();
    }


    // =========================================================
    // OPEN LOGIN
    // =========================================================

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

                Log.d(
                        TAG,
                        "Camera permission GRANTED"
                );

                startCamera();

            } else {

                Log.e(
                        TAG,
                        "Camera permission DENIED"
                );

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

            Log.d(
                    TAG,
                    "Location permission result"
            );

            if (hasLocationPermission()) {

                Log.d(
                        TAG,
                        "Location permission GRANTED"
                );

                if (pendingPhotoFile != null
                        && pendingPhotoFile.exists()) {

                    isProcessing = true;

                    showLoading("Fetching location...");

                    btnCapture.setEnabled(false);

                    getLocationAndSubmit();
                }

            } else {

                Log.e(
                        TAG,
                        "Location permission DENIED"
                );

                isProcessing = false;

                hideLoading();

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

        if (pendingPhotoFile != null) {

            Log.d(
                    TAG,
                    "Deleting pending photo = "
                            + pendingPhotoFile.getAbsolutePath()
            );

            if (pendingPhotoFile.exists()) {

                boolean deleted =
                        pendingPhotoFile.delete();

                Log.d(
                        TAG,
                        "Photo deleted = "
                                + deleted
                );
            }
        }

        pendingPhotoFile = null;

        if (pendingUploadFile != null
                && pendingUploadFile.exists()) {

            pendingUploadFile.delete();
        }

        pendingUploadFile = null;
    }


    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        Log.d(
                TAG,
                "CameraActivity DESTROYED"
        );

        deletePendingPhoto();

        if (cameraProvider != null) {

            Log.d(
                    TAG,
                    "Unbinding camera"
            );

            cameraProvider.unbindAll();
        }

        super.onDestroy();
    }
}
