package com.college.attendance.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.college.attendance.R;
import com.college.attendance.api.ApiClient;
import com.college.attendance.api.ApiService;
import com.college.attendance.dto.AttendancePolicyResponse;
import com.college.attendance.dto.AttendanceResponse;
import com.college.attendance.dto.UserProfileResponse;
import com.college.attendance.utils.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("SetTextI18n")
public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";

    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private static final String PROFILE_IMAGE_BASE_URL =
            "http://192.168.137.1:8080";

    private static final String EXTRA_CURRENT_LATITUDE =
            "CURRENT_LATITUDE";

    private static final String EXTRA_CURRENT_LONGITUDE =
            "CURRENT_LONGITUDE";

    // =========================================================
    // CURRENT TAB
    // =========================================================

    @Override
    protected Tab getCurrentTab() {
        return Tab.HOME;
    }

    // =========================================================
    // HEADER
    // =========================================================

    private ImageButton btnMenu;
    private ImageButton btnNotification;
    private ShapeableImageView btnProfile;

    // =========================================================
    // USER
    // =========================================================

    private TextView tvWelcome;
    private TextView tvUsername;
    private TextView tvRole;
    private TextView tvTodayDate;

    // =========================================================
    // ATTENDANCE
    // =========================================================

    private TextView tvAttendanceStatus;
    private TextView tvCheckInTime;
    private TextView tvCheckOutTime;
    private TextView tvWorkHours;

    // Kept for compatibility
    private MaterialButton btnCheckIn;
    private MaterialButton btnCheckOut;

    // =========================================================
    // PUNCH
    // =========================================================

    private MaterialCardView cardPunch;
    private LinearLayout punchContainer;

    private MaterialButton btnMainPunch;
    private TextView tvPunchAction;
    private TextView tvPunchTime;

    // =========================================================
    // LOCATION
    // =========================================================

    private TextView tvLocationName;
    private TextView tvLocationRadius;

    private FusedLocationProviderClient fusedLocationClient;

    private Double currentLatitude;
    private Double currentLongitude;

    private boolean locationLoaded = false;
    private boolean locationLoading = false;

    // =========================================================
    // POLICY
    // =========================================================

    private TextView tvShiftTiming;
    private TextView tvWeeklyOff;

    // =========================================================
    // CALENDAR
    // =========================================================

    private TextView tvViewCalendar;

    // =========================================================
    // API / SESSION
    // =========================================================

    private SessionManager sessionManager;
    private ApiService apiService;

    // =========================================================
    // DATA
    // =========================================================

    private AttendanceResponse todayAttendance;
    private AttendancePolicyResponse attendancePolicy;

    /**
     * true  = today's attendance state successfully resolved.
     * false = still loading or failed.
     */
    private boolean attendanceLoaded = false;

    /**
     * Prevents duplicate attendance API requests.
     */
    private boolean attendanceLoading = false;

    /**
     * Policy is independent of punch button.
     */
    private boolean policyLoaded = false;

    /**
     * Prevents multiple punch clicks while attendance
     * state is being resolved.
     */
    private boolean punchActionInProgress = false;

    // =========================================================
    // GEOCODER
    // =========================================================

    private final ExecutorService geocoderExecutor =
            Executors.newSingleThreadExecutor();

    // =========================================================
    // WORK HOURS HANDLER
    // =========================================================

    private final Handler workHoursHandler =
            new Handler(Looper.getMainLooper());

    private final Runnable workHoursRunnable =
            new Runnable() {

                @Override
                public void run() {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    updateWorkHours();

                    if (isCheckedIn()) {

                        workHoursHandler.postDelayed(
                                this,
                                60_000
                        );
                    }
                }
            };

    // =========================================================
    // DATE FORMAT
    // =========================================================

    private final SimpleDateFormat displayDateFormat =
            new SimpleDateFormat(
                    "EEEE, dd MMMM yyyy",
                    Locale.getDefault()
            );

    private final SimpleDateFormat apiDateFormat =
            new SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
            );

    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initializeViews();

        sessionManager = new SessionManager(this);

        // -----------------------------------------------------
        // SESSION
        // -----------------------------------------------------

        if (!sessionManager.isLoggedIn()) {

            openLogin();

            return;
        }

        // -----------------------------------------------------
        // API
        // -----------------------------------------------------

        apiService = ApiClient.getApiService(this);

        // -----------------------------------------------------
        // LOCATION
        // -----------------------------------------------------

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        // -----------------------------------------------------
        // USER
        // -----------------------------------------------------

        setupUser();

        // -----------------------------------------------------
        // HEADER
        // -----------------------------------------------------

        setupHeader();

        // -----------------------------------------------------
        // PROFILE
        // -----------------------------------------------------

        loadProfileImage();

        // -----------------------------------------------------
        // BUTTONS
        // -----------------------------------------------------

        setupPunchButtons();

        // -----------------------------------------------------
        // DATE
        // -----------------------------------------------------

        updateTodayDate();

        // -----------------------------------------------------
        // INITIAL UI
        // -----------------------------------------------------

        showInitialAttendanceState();
        showInitialPolicyState();
        showInitialLocationState();
    }

    // =========================================================
    // ON RESUME
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();

        if (sessionManager == null
                || !sessionManager.isLoggedIn()) {

            return;
        }

        updateTodayDate();

        /*
         * Attendance determines only:
         *
         * PUNCH IN
         * PUNCH OUT
         * COMPLETED
         */
        loadTodayAttendance();

        /*
         * Policy is independent from camera opening.
         */
        loadAttendancePolicy();

        /*
         * Location is completely independent.
         */
        loadCurrentLocation();
    }

    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initializeViews() {

        btnMenu = findViewById(R.id.btnMenu);

        btnNotification =
                findViewById(R.id.btnNotification);

        btnProfile =
                findViewById(R.id.btnProfile);

        tvWelcome =
                findViewById(R.id.tvWelcome);

        tvUsername =
                findViewById(R.id.tvUsername);

        tvRole =
                findViewById(R.id.tvRole);

        tvTodayDate =
                findViewById(R.id.tvTodayDate);

        tvAttendanceStatus =
                findViewById(R.id.tvAttendanceStatus);

        tvCheckInTime =
                findViewById(R.id.tvCheckInTime);

        tvCheckOutTime =
                findViewById(R.id.tvCheckOutTime);

        tvWorkHours =
                findViewById(R.id.tvWorkHours);

        btnCheckIn = null;
        btnCheckOut = null;

        cardPunch =
                findViewById(R.id.cardPunch);

        punchContainer =
                findViewById(R.id.punchContainer);

        btnMainPunch =
                findViewById(R.id.btnMainPunch);

        tvPunchAction =
                findViewById(R.id.tvPunchAction);

        tvPunchTime =
                findViewById(R.id.tvPunchTime);

        tvLocationName =
                findViewById(R.id.tvLocationName);

        tvLocationRadius =
                findViewById(R.id.tvLocationRadius);

        tvShiftTiming =
                findViewById(R.id.tvShiftTiming);

        tvWeeklyOff =
                findViewById(R.id.tvWeeklyOff);

        tvViewCalendar =
                findViewById(R.id.tvViewCalendar);
    }

    // =========================================================
    // USER
    // =========================================================

    private void setupUser() {

        String username =
                sessionManager.getUsername();

        String role =
                sessionManager.getRole();

        if (isEmpty(username)) {
            username = "User";
        }

        if (isEmpty(role)) {
            role = "USER";
        }

        tvUsername.setText(
                username.trim()
        );

        tvRole.setText(
                role.trim().toUpperCase(
                        Locale.getDefault()
                )
        );

        Calendar calendar =
                Calendar.getInstance();

        int hour =
                calendar.get(Calendar.HOUR_OF_DAY);

        String greeting;

        if (hour < 12) {

            greeting = "Good Morning";

        } else if (hour < 17) {

            greeting = "Good Afternoon";

        } else {

            greeting = "Good Evening";
        }

        tvWelcome.setText(greeting);
    }

    // =========================================================
    // HEADER
    // =========================================================

    private void setupHeader() {

        btnMenu.setOnClickListener(
                v -> showMenu()
        );

        btnNotification.setOnClickListener(
                v -> Toast.makeText(
                        MainActivity.this,
                        "No new notifications.",
                        Toast.LENGTH_SHORT
                ).show()
        );

        btnProfile.setOnClickListener(
                v -> openProfile()
        );
    }

    // =========================================================
    // PROFILE
    // =========================================================

    private void loadProfileImage() {

        if (apiService == null) {

            setDefaultProfileImage();

            return;
        }

        apiService
                .getMyProfile()
                .enqueue(
                        new Callback<UserProfileResponse>() {

                            @Override
                            public void onResponse(
                                    Call<UserProfileResponse> call,
                                    Response<UserProfileResponse> response) {

                                if (!response.isSuccessful()
                                        || response.body() == null) {

                                    setDefaultProfileImage();

                                    return;
                                }

                                UserProfileResponse profile =
                                        response.body();

                                updateHeaderUserInfo(profile);

                                loadProfileImageFromUrl(
                                        profile.getProfileImageUrl()
                                );
                            }

                            @Override
                            public void onFailure(
                                    Call<UserProfileResponse> call,
                                    Throwable t) {

                                Log.e(
                                        TAG,
                                        "Profile loading failed",
                                        t
                                );

                                setDefaultProfileImage();
                            }
                        }
                );
    }

    // =========================================================
    // UPDATE HEADER USER INFO
    // =========================================================

    private void updateHeaderUserInfo(
            UserProfileResponse profile) {

        if (profile == null) {
            return;
        }

        String username =
                profile.getUsername();

        String role =
                profile.getRole();

        if (isEmpty(username)) {

            username =
                    sessionManager.getUsername();
        }

        if (isEmpty(username)) {

            username = "User";
        }

        tvUsername.setText(
                username.trim()
        );

        if (isEmpty(role)) {

            role =
                    sessionManager.getRole();
        }

        if (isEmpty(role)) {

            role = "USER";
        }

        tvRole.setText(
                role.trim().toUpperCase(
                        Locale.getDefault()
                )
        );
    }

    // =========================================================
    // LOAD PROFILE IMAGE
    // =========================================================

    private void loadProfileImageFromUrl(
            String profileImageUrl) {

        if (isEmpty(profileImageUrl)) {

            setDefaultProfileImage();

            return;
        }

        String imageUrl =
                profileImageUrl.trim();

        if (!imageUrl.startsWith("http://")
                && !imageUrl.startsWith("https://")) {

            if (!imageUrl.startsWith("/")) {
                imageUrl = "/" + imageUrl;
            }

            imageUrl =
                    PROFILE_IMAGE_BASE_URL
                            + imageUrl;
        }

        Log.d(
                TAG,
                "Profile image URL: " + imageUrl
        );

        Glide.with(MainActivity.this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .into(btnProfile);
    }

    // =========================================================
    // DEFAULT PROFILE IMAGE
    // =========================================================

    private void setDefaultProfileImage() {

        Glide.with(MainActivity.this)
                .load(R.drawable.ic_person)
                .circleCrop()
                .into(btnProfile);
    }

    // =========================================================
    // MENU
    // =========================================================

    private void showMenu() {

        android.widget.PopupMenu popupMenu =
                new android.widget.PopupMenu(
                        this,
                        btnMenu
                );

        popupMenu.getMenu().add("Calendar");
        popupMenu.getMenu().add("Leave");
        popupMenu.getMenu().add("Profile");

        popupMenu.setOnMenuItemClickListener(
                item -> {

                    String title =
                            item.getTitle().toString();

                    if ("Calendar".equals(title)) {

                        openCalendar();

                        return true;
                    }

                    if ("Leave".equals(title)) {

                        openLeave();

                        return true;
                    }

                    if ("Profile".equals(title)) {

                        openProfile();

                        return true;
                    }

                    return false;
                }
        );

        popupMenu.show();
    }

    // =========================================================
    // PUNCH BUTTON SETUP
    // =========================================================

    private void setupPunchButtons() {

        /*
         * Camera must not be blocked by:
         *
         * - location
         * - GPS
         * - policy
         *
         * Attendance state is the only thing that determines
         * Punch In / Punch Out / Completed.
         */

        btnMainPunch.setEnabled(true);
        btnMainPunch.setClickable(true);

        btnMainPunch.setOnClickListener(
                v -> handleMainPunch()
        );

        tvViewCalendar.setOnClickListener(
                v -> openCalendar()
        );
    }

    // =========================================================
    // MAIN PUNCH
    // =========================================================

    private void handleMainPunch() {

        if (punchActionInProgress) {

            Log.d(
                    TAG,
                    "Punch already in progress."
            );

            return;
        }

        /*
         * If today's attendance is still being resolved,
         * wait for the API result.
         */
        if (!attendanceLoaded) {

            if (attendanceLoading) {

                Toast.makeText(
                        this,
                        "Checking today's attendance...",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            punchActionInProgress = true;

            btnMainPunch.setEnabled(false);

            Toast.makeText(
                    this,
                    "Checking today's attendance...",
                    Toast.LENGTH_SHORT
            ).show();

            loadTodayAttendanceForPunch();

            return;
        }

        /*
         * Attendance is already known.
         * Open camera immediately.
         */
        openPunchCameraFromAttendance();
    }

    // =========================================================
    // LOAD TODAY ATTENDANCE FOR PUNCH
    // =========================================================

    private void loadTodayAttendanceForPunch() {

        if (apiService == null) {

            punchActionInProgress = false;

            btnMainPunch.setEnabled(true);

            Toast.makeText(
                    this,
                    "API service is unavailable.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (attendanceLoading) {
            return;
        }

        attendanceLoading = true;

        String today =
                apiDateFormat.format(new Date());

        Log.d(
                TAG,
                "Checking attendance for date: " + today
        );

        apiService
                .getMyAttendanceByDate(today)
                .enqueue(
                        new Callback<AttendanceResponse>() {

                            @Override
                            public void onResponse(
                                    Call<AttendanceResponse> call,
                                    Response<AttendanceResponse> response) {

                                attendanceLoading = false;
                                punchActionInProgress = false;

                                Log.d(
                                        TAG,
                                        "Attendance response code: "
                                                + response.code()
                                );

                                // -------------------------------------------------
                                // ATTENDANCE EXISTS
                                // -------------------------------------------------

                                if (response.isSuccessful()
                                        && response.body() != null) {

                                    todayAttendance =
                                            response.body();

                                    attendanceLoaded = true;

                                    Log.d(
                                            TAG,
                                            "Today's attendance loaded successfully."
                                    );

                                    updateAttendanceUI();

                                    /*
                                     * IMPORTANT:
                                     *
                                     * Do not check:
                                     * - location
                                     * - policy
                                     *
                                     * Camera opens immediately.
                                     */
                                    openPunchCameraFromAttendance();

                                    return;
                                }

                                // -------------------------------------------------
                                // NO ATTENDANCE FOR TODAY
                                // -------------------------------------------------

                                if (response.code() == 404) {

                                    todayAttendance = null;

                                    attendanceLoaded = true;

                                    Log.d(
                                            TAG,
                                            "No attendance found for today. "
                                                    + "Punch In required."
                                    );

                                    showNotMarked();

                                    /*
                                     * No attendance = PUNCH IN.
                                     *
                                     * Camera opens immediately.
                                     */
                                    openPunchCameraFromAttendance();

                                    return;
                                }

                                // -------------------------------------------------
                                // SESSION EXPIRED
                                // -------------------------------------------------

                                if (response.code() == 401) {

                                    handleSessionExpired();

                                    return;
                                }

                                // -------------------------------------------------
                                // ACCESS DENIED
                                // -------------------------------------------------

                                if (response.code() == 403) {

                                    attendanceLoaded = false;

                                    btnMainPunch.setEnabled(true);

                                    tvAttendanceStatus.setText(
                                            "Access denied"
                                    );

                                    Toast.makeText(
                                            MainActivity.this,
                                            "You are not allowed to access attendance.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }

                                // -------------------------------------------------
                                // OTHER HTTP ERROR
                                // -------------------------------------------------

                                attendanceLoaded = false;

                                btnMainPunch.setEnabled(true);

                                String errorMessage =
                                        "HTTP " + response.code();

                                try {

                                    if (response.errorBody() != null) {

                                        String errorBody =
                                                response.errorBody().string();

                                        if (!isEmpty(errorBody)) {

                                            errorMessage +=
                                                    " - "
                                                            + errorBody;
                                        }
                                    }

                                } catch (Exception e) {

                                    Log.e(
                                            TAG,
                                            "Unable to read error body",
                                            e
                                    );
                                }

                                Log.e(
                                        TAG,
                                        "Attendance API error: "
                                                + errorMessage
                                );

                                Toast.makeText(
                                        MainActivity.this,
                                        "Unable to check today's attendance.",
                                        Toast.LENGTH_LONG
                                ).show();
                            }

                            @Override
                            public void onFailure(
                                    Call<AttendanceResponse> call,
                                    Throwable t) {

                                attendanceLoading = false;
                                punchActionInProgress = false;
                                attendanceLoaded = false;

                                btnMainPunch.setEnabled(true);

                                /*
                                 * onFailure() can mean:
                                 *
                                 * 1. Network/connection error
                                 * 2. Timeout
                                 * 3. Server unreachable
                                 * 4. Cleartext HTTP problem
                                 * 5. JSON/Gson parsing error
                                 * 6. Response conversion error
                                 *
                                 * Therefore always log the complete
                                 * Throwable.
                                 */

                                Log.e(
                                        TAG,
                                        "Attendance API onFailure",
                                        t
                                );

                                Log.e(
                                        TAG,
                                        "Attendance API error class: "
                                                + t.getClass().getName()
                                );

                                Log.e(
                                        TAG,
                                        "Attendance API error message: "
                                                + t.getMessage()
                                );

                                String error =
                                        t.getMessage();

                                if (isEmpty(error)) {

                                    error =
                                            "Unable to connect to server";
                                }

                                Toast.makeText(
                                        MainActivity.this,
                                        "Connection error: " + error,
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }

    // =========================================================
    // LOAD TODAY ATTENDANCE
    // =========================================================

    private void loadTodayAttendance() {

        if (apiService == null) {
            return;
        }

        if (attendanceLoading) {
            return;
        }

        attendanceLoading = true;
        attendanceLoaded = false;

        refreshPunchButton();

        String today =
                apiDateFormat.format(new Date());

        Log.d(
                TAG,
                "Loading today's attendance: " + today
        );

        apiService
                .getMyAttendanceByDate(today)
                .enqueue(
                        new Callback<AttendanceResponse>() {

                            @Override
                            public void onResponse(
                                    Call<AttendanceResponse> call,
                                    Response<AttendanceResponse> response) {

                                attendanceLoading = false;

                                Log.d(
                                        TAG,
                                        "Attendance load response: "
                                                + response.code()
                                );

                                // -------------------------------------------------
                                // ATTENDANCE EXISTS
                                // -------------------------------------------------

                                if (response.isSuccessful()
                                        && response.body() != null) {

                                    todayAttendance =
                                            response.body();

                                    attendanceLoaded = true;

                                    updateAttendanceUI();

                                    refreshPunchButton();

                                    return;
                                }

                                // -------------------------------------------------
                                // NO ATTENDANCE
                                // -------------------------------------------------

                                if (response.code() == 404) {

                                    todayAttendance = null;

                                    attendanceLoaded = true;

                                    Log.d(
                                            TAG,
                                            "No attendance found for today."
                                    );

                                    showNotMarked();

                                    refreshPunchButton();

                                    return;
                                }

                                // -------------------------------------------------
                                // SESSION EXPIRED
                                // -------------------------------------------------

                                if (response.code() == 401) {

                                    handleSessionExpired();

                                    return;
                                }

                                // -------------------------------------------------
                                // ACCESS DENIED
                                // -------------------------------------------------

                                if (response.code() == 403) {

                                    attendanceLoaded = false;

                                    tvAttendanceStatus.setText(
                                            "Access denied"
                                    );

                                    refreshPunchButton();

                                    return;
                                }

                                // -------------------------------------------------
                                // OTHER ERROR
                                // -------------------------------------------------

                                attendanceLoaded = false;

                                tvAttendanceStatus.setText(
                                        "Unable to load"
                                );

                                refreshPunchButton();
                            }

                            @Override
                            public void onFailure(
                                    Call<AttendanceResponse> call,
                                    Throwable t) {

                                attendanceLoading = false;
                                attendanceLoaded = false;

                                Log.e(
                                        TAG,
                                        "Attendance loading failed",
                                        t
                                );

                                Log.e(
                                        TAG,
                                        "Attendance error class: "
                                                + t.getClass().getName()
                                );

                                Log.e(
                                        TAG,
                                        "Attendance error message: "
                                                + t.getMessage()
                                );

                                tvAttendanceStatus.setText(
                                        "Unable to load"
                                );

                                refreshPunchButton();
                            }
                        }
                );
    }

    // =========================================================
    // OPEN CAMERA FROM ATTENDANCE
    // =========================================================

    private void openPunchCameraFromAttendance() {

        if (!attendanceLoaded) {

            btnMainPunch.setEnabled(true);

            return;
        }

        String action;

        // -----------------------------------------------------
        // PUNCH IN
        // -----------------------------------------------------

        if (todayAttendance == null
                || isEmpty(
                todayAttendance.getCheckInTime()
        )) {

            action =
                    CameraActivity.ACTION_CHECK_IN;
        }

        // -----------------------------------------------------
        // PUNCH OUT
        // -----------------------------------------------------

        else if (isEmpty(
                todayAttendance.getCheckOutTime()
        )) {

            action =
                    CameraActivity.ACTION_CHECK_OUT;
        }

        // -----------------------------------------------------
        // COMPLETED
        // -----------------------------------------------------

        else {

            Toast.makeText(
                    this,
                    "Today's attendance is already completed.",
                    Toast.LENGTH_SHORT
            ).show();

            updatePunchCard();

            return;
        }

        Log.d(
                TAG,
                "Opening camera for action: " + action
        );

        openCamera(action);
    }

    // =========================================================
    // OPEN CAMERA
    // =========================================================

    private void openCamera(String action) {

        if (isEmpty(action)) {

            Log.e(
                    TAG,
                    "Cannot open camera: action is empty"
            );

            Toast.makeText(
                    this,
                    "Unable to start attendance.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent intent =
                new Intent(
                        MainActivity.this,
                        CameraActivity.class
                );

        intent.putExtra(
                CameraActivity.EXTRA_ACTION,
                action
        );

        /*
         * Location is OPTIONAL.
         *
         * CameraActivity can open even if:
         *
         * - GPS is unavailable
         * - location permission is denied
         * - current location has not loaded yet
         */

        if (currentLatitude != null) {

            intent.putExtra(
                    EXTRA_CURRENT_LATITUDE,
                    currentLatitude
            );
        }

        if (currentLongitude != null) {

            intent.putExtra(
                    EXTRA_CURRENT_LONGITUDE,
                    currentLongitude
            );
        }

        Log.d(
                TAG,
                "Starting CameraActivity"
        );

        startActivity(intent);
    }

    // =========================================================
    // CURRENT LOCATION
    // =========================================================

    private void loadCurrentLocation() {

        if (fusedLocationClient == null) {
            return;
        }

        if (locationLoading) {
            return;
        }

        boolean fineGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        if (!fineGranted && !coarseGranted) {

            requestLocationPermission();

            return;
        }

        locationLoading = true;
        locationLoaded = false;

        showLocationLoading();

        fusedLocationClient
                .getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        null
                )
                .addOnSuccessListener(
                        location -> {

                            locationLoading = false;

                            if (location == null) {

                                locationLoaded = false;

                                currentLatitude = null;
                                currentLongitude = null;

                                showLocationUnavailable();

                                return;
                            }

                            currentLatitude =
                                    location.getLatitude();

                            currentLongitude =
                                    location.getLongitude();

                            locationLoaded = true;

                            Log.d(
                                    TAG,
                                    "Location: lat="
                                            + currentLatitude
                                            + ", lng="
                                            + currentLongitude
                            );

                            updateCurrentLocationUI();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            locationLoading = false;
                            locationLoaded = false;

                            currentLatitude = null;
                            currentLongitude = null;

                            Log.e(
                                    TAG,
                                    "Location fetch failed",
                                    e
                            );

                            showLocationUnavailable();
                        }
                );
    }

    // =========================================================
    // REQUEST LOCATION PERMISSION
    // =========================================================

    private void requestLocationPermission() {

        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE
        );
    }

    // =========================================================
    // LOCATION PERMISSION RESULT
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

        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        boolean granted = false;

        for (int result : grantResults) {

            if (result == PackageManager.PERMISSION_GRANTED) {

                granted = true;

                break;
            }
        }

        if (granted) {

            loadCurrentLocation();

        } else {

            locationLoaded = false;

            currentLatitude = null;
            currentLongitude = null;

            showLocationPermissionRequired();
        }
    }

    // =========================================================
    // LOCATION UI
    // =========================================================

    private void showLocationLoading() {

        tvLocationName.setText(
                "Detecting current location..."
        );

        tvLocationRadius.setText(
                "Please wait"
        );
    }

    private void updateCurrentLocationUI() {

        if (currentLatitude == null
                || currentLongitude == null) {

            showLocationUnavailable();

            return;
        }

        tvLocationName.setText(
                "Finding location name..."
        );

        tvLocationRadius.setText(
                "GPS ready"
        );

        final double latitude =
                currentLatitude;

        final double longitude =
                currentLongitude;

        geocoderExecutor.execute(
                () -> {

                    String locationName =
                            getReadableLocationName(
                                    latitude,
                                    longitude
                            );

                    runOnUiThread(
                            () -> {

                                if (isFinishing()
                                        || isDestroyed()) {

                                    return;
                                }

                                tvLocationName.setText(
                                        locationName
                                );

                                tvLocationRadius.setText(
                                        "GPS ready"
                                );
                            }
                    );
                }
        );
    }

    // =========================================================
    // REVERSE GEOCODING
    // =========================================================

    private String getReadableLocationName(
            double latitude,
            double longitude) {

        try {

            Geocoder geocoder =
                    new Geocoder(
                            this,
                            Locale.getDefault()
                    );

            if (!Geocoder.isPresent()) {
                return "Current location";
            }

            List<Address> addresses =
                    geocoder.getFromLocation(
                            latitude,
                            longitude,
                            1
                    );

            if (addresses == null
                    || addresses.isEmpty()) {

                return "Current location";
            }

            Address address =
                    addresses.get(0);

            String locality =
                    address.getLocality();

            String subAdminArea =
                    address.getSubAdminArea();

            String adminArea =
                    address.getAdminArea();

            StringBuilder location =
                    new StringBuilder();

            if (!isEmpty(locality)) {

                location.append(
                        locality.trim()
                );
            }

            if (!isEmpty(subAdminArea)
                    && !containsIgnoreCase(
                    location.toString(),
                    subAdminArea
            )) {

                if (location.length() > 0) {
                    location.append(", ");
                }

                location.append(
                        subAdminArea.trim()
                );
            }

            if (!isEmpty(adminArea)
                    && !containsIgnoreCase(
                    location.toString(),
                    adminArea
            )) {

                if (location.length() > 0) {
                    location.append(", ");
                }

                location.append(
                        adminArea.trim()
                );
            }

            if (location.length() == 0) {

                String addressLine =
                        address.getAddressLine(0);

                if (!isEmpty(addressLine)) {

                    location.append(
                            addressLine.trim()
                    );
                }
            }

            if (location.length() == 0) {
                return "Current location";
            }

            return location.toString();

        } catch (IOException e) {

            Log.w(
                    TAG,
                    "Reverse geocoding failed",
                    e
            );

            return "Current location";

        } catch (Exception e) {

            Log.w(
                    TAG,
                    "Unexpected geocoding error",
                    e
            );

            return "Current location";
        }
    }

    // =========================================================
    // STRING CONTAINS
    // =========================================================

    private boolean containsIgnoreCase(
            String source,
            String value) {

        if (source == null
                || value == null) {

            return false;
        }

        return source
                .toLowerCase(Locale.getDefault())
                .contains(
                        value.trim()
                                .toLowerCase(Locale.getDefault())
                );
    }

    // =========================================================
    // LOCATION UNAVAILABLE
    // =========================================================

    private void showLocationUnavailable() {

        tvLocationName.setText(
                "Unable to detect location"
        );

        tvLocationRadius.setText(
                "Location unavailable"
        );
    }

    // =========================================================
    // LOCATION PERMISSION REQUIRED
    // =========================================================

    private void showLocationPermissionRequired() {

        tvLocationName.setText(
                "Location permission required"
        );

        tvLocationRadius.setText(
                "Enable location permission"
        );
    }

    // =========================================================
    // ATTENDANCE
    // =========================================================

    private void updateAttendanceUI() {

        if (todayAttendance == null) {

            showNotMarked();

            return;
        }

        String checkIn =
                todayAttendance.getCheckInTime();

        String checkOut =
                todayAttendance.getCheckOutTime();

        String status =
                todayAttendance.getStatus();

        if (isEmpty(status)) {
            status = "PRESENT";
        }

        tvAttendanceStatus.setText(status);

        if ("PRESENT".equalsIgnoreCase(status)) {

            tvAttendanceStatus.setTextColor(
                    Color.rgb(30, 158, 90)
            );

            tvAttendanceStatus.setBackgroundColor(
                    Color.rgb(234, 248, 240)
            );

        } else if ("ABSENT".equalsIgnoreCase(status)) {

            tvAttendanceStatus.setTextColor(
                    Color.rgb(210, 60, 60)
            );

        } else if ("HD".equalsIgnoreCase(status)
                || "HALFDAY".equalsIgnoreCase(status)
                || "HALF_DAY".equalsIgnoreCase(status)) {

            tvAttendanceStatus.setTextColor(
                    Color.rgb(220, 140, 40)
            );
        }

        if (isEmpty(checkIn)) {

            tvCheckInTime.setText("--:--");

        } else {

            tvCheckInTime.setText(
                    formatDisplayTime(checkIn)
            );
        }

        if (isEmpty(checkOut)) {

            tvCheckOutTime.setText("--:--");

        } else {

            tvCheckOutTime.setText(
                    formatDisplayTime(checkOut)
            );
        }

        updateWorkHours();
        updatePunchCard();
    }

    // =========================================================
    // NOT MARKED
    // =========================================================

    private void showNotMarked() {

        tvAttendanceStatus.setText(
                "Not marked"
        );

        tvAttendanceStatus.setTextColor(
                Color.rgb(110, 110, 110)
        );

        tvAttendanceStatus.setBackgroundColor(
                Color.rgb(240, 240, 240)
        );

        tvCheckInTime.setText("--:--");
        tvCheckOutTime.setText("--:--");
        tvWorkHours.setText("--:--");

        updatePunchCard();
    }

    // =========================================================
    // WORK HOURS
    // =========================================================

    private void updateWorkHours() {

        workHoursHandler.removeCallbacks(
                workHoursRunnable
        );

        if (todayAttendance == null) {

            tvWorkHours.setText("--:--");

            return;
        }

        String checkIn =
                todayAttendance.getCheckInTime();

        String checkOut =
                todayAttendance.getCheckOutTime();

        if (isEmpty(checkIn)) {

            tvWorkHours.setText("--:--");

            return;
        }

        Date checkInDate =
                parseAttendanceTime(checkIn);

        if (checkInDate == null) {

            tvWorkHours.setText("--:--");

            return;
        }

        Date endDate;

        if (!isEmpty(checkOut)) {

            endDate =
                    parseAttendanceTime(checkOut);

        } else {

            endDate = new Date();
        }

        if (endDate == null) {

            tvWorkHours.setText("--:--");

            return;
        }

        long difference =
                endDate.getTime()
                        - checkInDate.getTime();

        if (difference < 0) {

            difference +=
                    24L
                            * 60L
                            * 60L
                            * 1000L;
        }

        long totalMinutes =
                difference / (60L * 1000L);

        long hours =
                totalMinutes / 60L;

        long minutes =
                totalMinutes % 60L;

        String workHours =
                String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        hours,
                        minutes
                );

        tvWorkHours.setText(workHours);

        if (isEmpty(checkOut)) {

            workHoursHandler.postDelayed(
                    workHoursRunnable,
                    60_000
            );
        }
    }

    // =========================================================
    // PARSE ATTENDANCE TIME
    // =========================================================

    private Date parseAttendanceTime(String value) {

        if (isEmpty(value)) {
            return null;
        }

        String time =
                value.trim();

        if (time.contains("T")) {

            time =
                    time.substring(
                            time.indexOf("T") + 1
                    );
        }

        if (time.contains(" ")) {

            String[] parts =
                    time.split("\\s+");

            time =
                    parts[parts.length - 1];
        }

        if (time.endsWith("Z")) {

            time =
                    time.substring(
                            0,
                            time.length() - 1
                    );
        }

        int plusIndex =
                time.indexOf("+");

        if (plusIndex > 0) {

            time =
                    time.substring(
                            0,
                            plusIndex
                    );
        }

        int dotIndex =
                time.indexOf(".");

        if (dotIndex > 0) {

            time =
                    time.substring(
                            0,
                            dotIndex
                    );
        }

        Date parsedTime = null;

        String[] formats = {
                "HH:mm:ss",
                "HH:mm"
        };

        for (String format : formats) {

            try {

                SimpleDateFormat sdf =
                        new SimpleDateFormat(
                                format,
                                Locale.getDefault()
                        );

                sdf.setLenient(false);

                parsedTime =
                        sdf.parse(time);

                if (parsedTime != null) {
                    break;
                }

            } catch (Exception ignored) {
            }
        }

        if (parsedTime == null) {
            return null;
        }

        Calendar parsedCalendar =
                Calendar.getInstance();

        parsedCalendar.setTime(parsedTime);

        int hour =
                parsedCalendar.get(Calendar.HOUR_OF_DAY);

        int minute =
                parsedCalendar.get(Calendar.MINUTE);

        int second =
                parsedCalendar.get(Calendar.SECOND);

        int millisecond =
                parsedCalendar.get(Calendar.MILLISECOND);

        Calendar today =
                Calendar.getInstance();

        today.set(
                Calendar.HOUR_OF_DAY,
                hour
        );

        today.set(
                Calendar.MINUTE,
                minute
        );

        today.set(
                Calendar.SECOND,
                second
        );

        today.set(
                Calendar.MILLISECOND,
                millisecond
        );

        return today.getTime();
    }

    // =========================================================
    // DISPLAY TIME
    // =========================================================

    private String formatDisplayTime(String value) {

        Date date =
                parseAttendanceTime(value);

        if (date == null) {
            return value;
        }

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "hh:mm a",
                        Locale.getDefault()
                );

        return formatter.format(date);
    }

    // =========================================================
    // PUNCH CARD UI
    // =========================================================

    private void updatePunchCard() {

        /*
         * Punch state depends ONLY on attendance.
         *
         * Location and policy never disable the button.
         */

        if (!attendanceLoaded) {

            tvPunchAction.setText(
                    "PUNCH"
            );

            tvPunchTime.setText(
                    getCurrentTime()
            );

            btnMainPunch.setText("▶");

            btnMainPunch.setEnabled(true);

            return;
        }

        // -----------------------------------------------------
        // NO ATTENDANCE
        // -----------------------------------------------------

        if (todayAttendance == null) {

            tvPunchAction.setText(
                    "PUNCH IN"
            );

            tvPunchTime.setText(
                    getCurrentTime()
            );

            btnMainPunch.setText("▶");

            btnMainPunch.setEnabled(true);

            return;
        }

        String checkIn =
                todayAttendance.getCheckInTime();

        String checkOut =
                todayAttendance.getCheckOutTime();

        // -----------------------------------------------------
        // PUNCH IN
        // -----------------------------------------------------

        if (isEmpty(checkIn)) {

            tvPunchAction.setText(
                    "PUNCH IN"
            );

            tvPunchTime.setText(
                    getCurrentTime()
            );

            btnMainPunch.setText("▶");

            btnMainPunch.setEnabled(true);

            return;
        }

        // -----------------------------------------------------
        // PUNCH OUT
        // -----------------------------------------------------

        if (isEmpty(checkOut)) {

            tvPunchAction.setText(
                    "PUNCH OUT"
            );

            tvPunchTime.setText(
                    getCurrentTime()
            );

            btnMainPunch.setText("▶");

            btnMainPunch.setEnabled(true);

            return;
        }

        // -----------------------------------------------------
        // COMPLETED
        // -----------------------------------------------------

        tvPunchAction.setText(
                "ATTENDANCE COMPLETED"
        );

        tvPunchTime.setText(
                formatDisplayTime(checkOut)
        );

        btnMainPunch.setText("✓");

        btnMainPunch.setEnabled(false);
    }

    // =========================================================
    // REFRESH PUNCH BUTTON
    // =========================================================

    private void refreshPunchButton() {

        updatePunchCard();
    }

    // =========================================================
    // CURRENT TIME
    // =========================================================

    private String getCurrentTime() {

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "hh:mm a",
                        Locale.getDefault()
                );

        return formatter.format(new Date());
    }

    // =========================================================
    // POLICY
    // =========================================================

    private void loadAttendancePolicy() {

        if (apiService == null) {
            return;
        }

        policyLoaded = false;

        apiService
                .getCurrentPolicy()
                .enqueue(
                        new Callback<AttendancePolicyResponse>() {

                            @Override
                            public void onResponse(
                                    Call<AttendancePolicyResponse> call,
                                    Response<AttendancePolicyResponse> response) {

                                if (response.isSuccessful()
                                        && response.body() != null) {

                                    attendancePolicy =
                                            response.body();

                                    policyLoaded = true;

                                    updatePolicyUI();

                                    return;
                                }

                                if (response.code() == 401) {

                                    handleSessionExpired();

                                    return;
                                }

                                attendancePolicy = null;

                                policyLoaded = false;

                                showPolicyUnavailable();
                            }

                            @Override
                            public void onFailure(
                                    Call<AttendancePolicyResponse> call,
                                    Throwable t) {

                                attendancePolicy = null;

                                policyLoaded = false;

                                Log.e(
                                        TAG,
                                        "Policy loading failed",
                                        t
                                );

                                showPolicyUnavailable();
                            }
                        }
                );
    }

    // =========================================================
    // POLICY UI
    // =========================================================

    private void updatePolicyUI() {

        if (attendancePolicy == null) {

            showPolicyUnavailable();

            return;
        }

        if (attendancePolicy.getWorkingHours() != null) {

            String hours =
                    String.valueOf(
                            attendancePolicy.getWorkingHours()
                    );

            tvShiftTiming.setText(
                    "Required working time: "
                            + hours
                            + " hours"
            );

        } else {

            tvShiftTiming.setText(
                    "Required working time: --"
            );
        }

        boolean saturdayOff =
                Boolean.TRUE.equals(
                        attendancePolicy.getSaturdayOff()
                );

        boolean sundayOff =
                Boolean.TRUE.equals(
                        attendancePolicy.getSundayOff()
                );

        String weeklyOff;

        if (saturdayOff && sundayOff) {

            weeklyOff =
                    "Saturday & Sunday";

        } else if (saturdayOff) {

            weeklyOff =
                    "Saturday";

        } else if (sundayOff) {

            weeklyOff =
                    "Sunday";

        } else {

            weeklyOff =
                    "No weekly off";
        }

        tvWeeklyOff.setText(
                "Weekly off: "
                        + weeklyOff
        );
    }

    // =========================================================
    // POLICY UNAVAILABLE
    // =========================================================

    private void showPolicyUnavailable() {

        tvShiftTiming.setText(
                "Required working time: --"
        );

        tvWeeklyOff.setText(
                "Weekly off: --"
        );
    }

    // =========================================================
    // INITIAL ATTENDANCE STATE
    // =========================================================

    private void showInitialAttendanceState() {

        tvAttendanceStatus.setText(
                "Loading"
        );

        tvCheckInTime.setText("--:--");
        tvCheckOutTime.setText("--:--");
        tvWorkHours.setText("--:--");

        tvPunchAction.setText(
                "PUNCH"
        );

        tvPunchTime.setText(
                getCurrentTime()
        );

        /*
         * Button remains enabled while API is loading.
         */
        btnMainPunch.setEnabled(true);
        btnMainPunch.setClickable(true);
    }

    // =========================================================
    // INITIAL POLICY STATE
    // =========================================================

    private void showInitialPolicyState() {

        tvShiftTiming.setText(
                "Required working time: --"
        );

        tvWeeklyOff.setText(
                "Weekly off: --"
        );
    }

    // =========================================================
    // INITIAL LOCATION STATE
    // =========================================================

    private void showInitialLocationState() {

        tvLocationName.setText(
                "Detecting current location..."
        );

        tvLocationRadius.setText(
                "Please wait"
        );
    }

    // =========================================================
    // TODAY DATE
    // =========================================================

    private void updateTodayDate() {

        tvTodayDate.setText(
                displayDateFormat.format(new Date())
        );
    }

    // =========================================================
    // OPEN CALENDAR
    // =========================================================

    private void openCalendar() {

        Intent intent =
                new Intent(
                        MainActivity.this,
                        CalendarActivity.class
                );

        startActivity(intent);

        finish();
    }

    // =========================================================
    // OPEN LEAVE
    // =========================================================

    private void openLeave() {

        Intent intent =
                new Intent(
                        MainActivity.this,
                        LeaveActivity.class
                );

        startActivity(intent);

        finish();
    }

    // =========================================================
    // OPEN PROFILE
    // =========================================================

    private void openProfile() {

        Intent intent =
                new Intent(
                        MainActivity.this,
                        ProfileActivity.class
                );

        startActivity(intent);
    }

    // =========================================================
    // SESSION EXPIRED
    // =========================================================

    private void handleSessionExpired() {

        if (sessionManager != null) {

            sessionManager.logout();
        }

        Toast.makeText(
                MainActivity.this,
                "Session expired. Please login again.",
                Toast.LENGTH_SHORT
        ).show();

        openLogin();
    }

    // =========================================================
    // CHECKED IN
    // =========================================================

    private boolean isCheckedIn() {

        return todayAttendance != null
                && !isEmpty(
                todayAttendance.getCheckInTime()
        )
                && isEmpty(
                todayAttendance.getCheckOutTime()
        );
    }

    // =========================================================
    // EMPTY
    // =========================================================

    private boolean isEmpty(String value) {

        return value == null
                || value.trim().isEmpty();
    }

    // =========================================================
    // ON DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        workHoursHandler.removeCallbacks(
                workHoursRunnable
        );

        geocoderExecutor.shutdownNow();

        super.onDestroy();
    }

    // =========================================================
// OPEN LOGIN
// =========================================================

    private void openLogin() {

        Intent intent =
                new Intent(
                        MainActivity.this,
                        LoginActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }
}