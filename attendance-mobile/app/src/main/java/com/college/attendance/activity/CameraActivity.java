package com.college.attendance.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ImageButton;
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

import java.io.File;
import java.util.concurrent.ExecutionException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CameraActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 1001;
    private static final int LOCATION_PERMISSION_REQUEST = 1002;

    public static final String EXTRA_ACTION = "attendance_action";

    public static final String ACTION_CHECK_IN = "CHECK_IN";
    public static final String ACTION_CHECK_OUT = "CHECK_OUT";

    private PreviewView previewView;
    private ImageButton btnCapture;
    private ImageButton btnBack;

    private TextView tvCameraTitle;
    private TextView tvCameraSubtitle;

    private ImageCapture imageCapture;

    private FusedLocationProviderClient fusedLocationClient;

    private ApiService apiService;
    private SessionManager sessionManager;

    private String attendanceAction;

    private File pendingPhotoFile;

    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_camera);

        initializeViews();

        sessionManager = new SessionManager(this);

        // User must be logged in
        if (!sessionManager.isLoggedIn()) {
            openLoginScreen();
            return;
        }

        apiService = ApiClient.getApiService(this);

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        attendanceAction =
                getIntent().getStringExtra(EXTRA_ACTION);

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

        if (hasCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }

        btnBack.setOnClickListener(v -> {

            if (!isProcessing) {
                deletePendingPhoto();
                finish();
            }
        });

        btnCapture.setOnClickListener(v -> {

            if (!isProcessing) {
                captureImage();
            }
        });
    }

    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initializeViews() {

        previewView = findViewById(R.id.previewView);

        btnCapture = findViewById(R.id.btnCapture);

        btnBack = findViewById(R.id.btnBack);

        tvCameraTitle =
                findViewById(R.id.tvCameraTitle);

        tvCameraSubtitle =
                findViewById(R.id.tvCameraSubtitle);
    }

    // =========================================================
    // SCREEN TEXT
    // =========================================================

    private void setupScreenText() {

        if (ACTION_CHECK_IN.equals(attendanceAction)) {

            tvCameraTitle.setText(R.string.check_in);

        } else {

            tvCameraTitle.setText(R.string.check_out);
        }

        tvCameraSubtitle.setText(
                R.string.camera_instruction
        );
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
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(
                () -> {

                    try {

                        ProcessCameraProvider cameraProvider =
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
            ProcessCameraProvider cameraProvider) {

        Preview preview =
                new Preview.Builder()
                        .build();

        preview.setSurfaceProvider(
                previewView.getSurfaceProvider()
        );

        imageCapture =
                new ImageCapture.Builder()
                        .setCaptureMode(
                                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                        )
                        .build();

        CameraSelector cameraSelector =
                CameraSelector.DEFAULT_FRONT_CAMERA;

        try {

            cameraProvider.unbindAll();

            cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    R.string.front_camera_failed,
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void showCameraStartError() {

        Toast.makeText(
                this,
                R.string.camera_start_failed,
                Toast.LENGTH_LONG
        ).show();
    }

    // =========================================================
    // CAPTURE IMAGE
    // =========================================================

    private void captureImage() {

        if (imageCapture == null) {

            Toast.makeText(
                    this,
                    R.string.camera_not_ready,
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (isProcessing) {
            return;
        }

        setProcessing(true);

        tvCameraSubtitle.setText(
                R.string.capturing_image
        );

        File photoFile =
                new File(
                        getCacheDir(),
                        "attendance_"
                                + System.currentTimeMillis()
                                + ".jpg"
                );

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions
                        .Builder(photoFile)
                        .build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {

                    @Override
                    public void onImageSaved(
                            @NonNull ImageCapture.OutputFileResults
                                    outputFileResults) {

                        pendingPhotoFile = photoFile;

                        tvCameraSubtitle.setText(
                                R.string.getting_location
                        );

                        getLocationAndSubmit();
                    }

                    @Override
                    public void onError(
                            @NonNull ImageCaptureException exception) {

                        setProcessing(false);

                        tvCameraSubtitle.setText(
                                R.string.camera_instruction
                        );

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

            setProcessing(false);

            Toast.makeText(
                    this,
                    R.string.image_not_found,
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (!hasLocationPermission()) {

            setProcessing(false);

            requestLocationPermission();

            return;
        }

        if (!isLocationEnabled()) {

            setProcessing(false);

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

        fetchCurrentLocation();
    }

    // =========================================================
    // CHECK GPS
    // =========================================================

    private boolean isLocationEnabled() {

        LocationManager locationManager =
                (LocationManager)
                        getSystemService(LOCATION_SERVICE);

        if (locationManager == null) {
            return false;
        }

        boolean gpsEnabled = false;
        boolean networkEnabled = false;

        try {

            gpsEnabled =
                    locationManager.isProviderEnabled(
                            LocationManager.GPS_PROVIDER
                    );

        } catch (Exception ignored) {
        }

        try {

            networkEnabled =
                    locationManager.isProviderEnabled(
                            LocationManager.NETWORK_PROVIDER
                    );

        } catch (Exception ignored) {
        }

        return gpsEnabled || networkEnabled;
    }

    // =========================================================
    // GET CURRENT LOCATION
    // =========================================================

    private void fetchCurrentLocation() {

        if (!hasLocationPermission()) {

            setProcessing(false);

            requestLocationPermission();

            return;
        }

        tvCameraSubtitle.setText(
                R.string.getting_current_location
        );

        CancellationTokenSource
                cancellationTokenSource =
                new CancellationTokenSource();

        try {

            fusedLocationClient
                    .getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            cancellationTokenSource.getToken()
                    )
                    .addOnSuccessListener(location -> {

                        if (location == null) {

                            setProcessing(false);

                            tvCameraSubtitle.setText(
                                    R.string.location_unavailable
                            );

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
                    })
                    .addOnFailureListener(e -> {

                        setProcessing(false);

                        tvCameraSubtitle.setText(
                                R.string.location_unavailable
                        );

                        Toast.makeText(
                                CameraActivity.this,
                                R.string.location_failed,
                                Toast.LENGTH_LONG
                        ).show();

                        deletePendingPhoto();
                    });

        } catch (SecurityException e) {

            setProcessing(false);

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

            setProcessing(false);

            Toast.makeText(
                    this,
                    R.string.image_not_found,
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // Make sure session still exists
        if (!sessionManager.isLoggedIn()) {

            handleSessionExpired();

            return;
        }

        tvCameraSubtitle.setText(
                R.string.verifying_attendance
        );

        MediaType imageMediaType =
                MediaType.parse("image/jpeg");

        MediaType textMediaType =
                MediaType.parse("text/plain");

        RequestBody imageRequestBody =
                RequestBody.create(
                        imageFile,
                        imageMediaType
                );

        MultipartBody.Part filePart =
                MultipartBody.Part.createFormData(
                        "file",
                        imageFile.getName(),
                        imageRequestBody
                );

        RequestBody latitudeBody =
                RequestBody.create(
                        String.valueOf(latitude),
                        textMediaType
                );

        RequestBody longitudeBody =
                RequestBody.create(
                        String.valueOf(longitude),
                        textMediaType
                );

        Call<AttendanceMarkResponse> call;

        if (ACTION_CHECK_IN.equals(attendanceAction)) {

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

        call.enqueue(
                new Callback<AttendanceMarkResponse>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<AttendanceMarkResponse> call,
                            @NonNull Response<AttendanceMarkResponse>
                                    response) {

                        setProcessing(false);

                        // =========================================
                        // SUCCESS
                        // =========================================

                        if (response.isSuccessful()
                                && response.body() != null) {

                            AttendanceMarkResponse
                                    attendanceResponse =
                                    response.body();

                            String message =
                                    attendanceResponse.getMessage();

                            if (message == null
                                    || message.trim().isEmpty()) {

                                if (ACTION_CHECK_IN.equals(
                                        attendanceAction)) {

                                    message =
                                            getString(
                                                    R.string.check_in_success
                                            );

                                } else {

                                    message =
                                            getString(
                                                    R.string.check_out_success
                                            );
                                }
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

                        // =========================================
                        // SESSION EXPIRED
                        // =========================================

                        if (response.code() == 401) {

                            handleSessionExpired();

                            return;
                        }

                        // =========================================
                        // FORBIDDEN
                        // =========================================

                        if (response.code() == 403) {

                            tvCameraSubtitle.setText(
                                    R.string.verification_failed
                            );

                            Toast.makeText(
                                    CameraActivity.this,
                                    R.string.attendance_not_allowed,
                                    Toast.LENGTH_LONG
                            ).show();

                            deletePendingPhoto();

                            return;
                        }

                        // =========================================
                        // OTHER API ERROR
                        // =========================================

                        tvCameraSubtitle.setText(
                                R.string.verification_failed
                        );

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

                        setProcessing(false);

                        tvCameraSubtitle.setText(
                                R.string.server_connection_failed
                        );

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
    // API ERROR
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

            try {

                String errorMessage =
                        errorBody.string();

                if (!errorMessage.trim().isEmpty()) {

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
    // EXTRACT BACKEND ERROR MESSAGE
    // =========================================================

    private String extractBackendMessage(
            String errorResponse) {

        String messageKey =
                "\"message\"";

        int messageIndex =
                errorResponse.indexOf(messageKey);

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

        // If backend returned plain text
        if (!errorResponse.trim().startsWith("{")) {
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

        setProcessing(false);

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
    // PROCESSING STATE
    // =========================================================

    private void setProcessing(
            boolean processing) {

        isProcessing = processing;

        if (btnCapture != null) {

            btnCapture.setEnabled(!processing);

            btnCapture.setAlpha(
                    processing ? 0.5f : 1.0f
            );
        }

        if (btnBack != null) {

            btnBack.setEnabled(!processing);

            btnBack.setAlpha(
                    processing ? 0.5f : 1.0f
            );
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

        if (requestCode == CAMERA_PERMISSION_REQUEST) {

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

        if (requestCode == LOCATION_PERMISSION_REQUEST) {

            if (hasLocationPermission()) {

                if (pendingPhotoFile != null
                        && pendingPhotoFile.exists()) {

                    setProcessing(true);

                    getLocationAndSubmit();
                }

            } else {

                setProcessing(false);

                deletePendingPhoto();

                tvCameraSubtitle.setText(
                        R.string.location_permission_required
                );

                Toast.makeText(
                        this,
                        R.string.location_permission_required,
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        deletePendingPhoto();

        super.onDestroy();
    }
}