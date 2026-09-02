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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

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

    private ExecutorService cameraExecutor;

    private FusedLocationProviderClient fusedLocationClient;

    private ApiService apiService;

    private String attendanceAction;

    private File pendingPhotoFile;

    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_camera);

        initializeViews();

        apiService = ApiClient.getApiService(this);

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        attendanceAction =
                getIntent().getStringExtra(EXTRA_ACTION);

        if (!ACTION_CHECK_IN.equals(attendanceAction)
                && !ACTION_CHECK_OUT.equals(attendanceAction)) {

            Toast.makeText(
                    this,
                    getString(R.string.invalid_attendance_action),
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        setupScreenText();

        cameraExecutor =
                Executors.newSingleThreadExecutor();

        if (hasCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }

        btnBack.setOnClickListener(v -> {

            if (!isProcessing) {
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

        previewView =
                findViewById(R.id.previewView);

        btnCapture =
                findViewById(R.id.btnCapture);

        btnBack =
                findViewById(R.id.btnBack);

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

            tvCameraTitle.setText(
                    R.string.check_in
            );

        } else {

            tvCameraTitle.setText(
                    R.string.check_out
            );

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

                    } catch (ExecutionException
                             | InterruptedException e) {

                        Toast.makeText(
                                CameraActivity.this,
                                getString(
                                        R.string.camera_start_failed
                                ),
                                Toast.LENGTH_LONG
                        ).show();
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
                    getString(
                            R.string.front_camera_failed
                    ),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // CAPTURE IMAGE
    // =========================================================

    private void captureImage() {

        if (imageCapture == null) {

            Toast.makeText(
                    this,
                    getString(
                            R.string.camera_not_ready
                    ),
                    Toast.LENGTH_SHORT
            ).show();

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
                                getString(
                                        R.string.image_capture_failed
                                ),
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
                    getString(
                            R.string.image_not_found
                    ),
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
                    getString(
                            R.string.enable_location
                    ),
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
                                    getString(
                                            R.string.location_unavailable_message
                                    ),
                                    Toast.LENGTH_LONG
                            ).show();

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
                                getString(
                                        R.string.location_failed
                                ),
                                Toast.LENGTH_LONG
                        ).show();
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
                    getString(
                            R.string.image_not_found
                    ),
                    Toast.LENGTH_LONG
            ).show();

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
                            Call<AttendanceMarkResponse> call,
                            Response<AttendanceMarkResponse>
                                    response) {

                        setProcessing(false);

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

                        } else {

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
                    }

                    @Override
                    public void onFailure(
                            Call<AttendanceMarkResponse> call,
                            Throwable t) {

                        setProcessing(false);

                        tvCameraSubtitle.setText(
                                R.string.server_connection_failed
                        );

                        Toast.makeText(
                                CameraActivity.this,
                                getString(
                                        R.string.server_connection_failed
                                ),
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

        if (response.errorBody() != null) {

            try (
                    okhttp3.ResponseBody errorBody =
                            response.errorBody()
            ) {

                String errorMessage =
                        errorBody.string();

                if (!errorMessage.trim().isEmpty()) {
                    return errorMessage;
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
    // PROCESSING STATE
    // =========================================================

    private void setProcessing(boolean processing) {

        isProcessing = processing;

        btnCapture.setEnabled(!processing);
        btnBack.setEnabled(!processing);

        btnCapture.setAlpha(
                processing ? 0.5f : 1.0f
        );

        btnBack.setAlpha(
                processing ? 0.5f : 1.0f
        );
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
                        getString(
                                R.string.camera_permission_required
                        ),
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
                        getString(
                                R.string.location_permission_required
                        ),
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

        super.onDestroy();

        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }

        deletePendingPhoto();
    }
}
