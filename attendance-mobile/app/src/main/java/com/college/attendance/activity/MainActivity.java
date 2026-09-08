package com.college.attendance.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.college.attendance.R;
import com.college.attendance.api.ApiClient;
import com.college.attendance.api.ApiService;
import com.college.attendance.dto.AttendancePolicyResponse;
import com.college.attendance.dto.AttendanceResponse;
import com.college.attendance.utils.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

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
public class MainActivity extends AppCompatActivity {

    // =========================================================
    // HEADER
    // =========================================================

    private ImageButton btnMenu;
    private ImageButton btnNotification;
    private ImageButton btnProfile;

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

    /**
     * Current device GPS coordinates.
     *
     * These are kept internally for attendance/punching.
     * They are NOT displayed in the UI.
     */
    private Double currentLatitude;
    private Double currentLongitude;

    private boolean locationLoaded = false;
    private boolean locationLoading = false;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    /**
     * Reverse geocoding runs in background thread
     * so UI thread is not blocked.
     */
    private final ExecutorService geocoderExecutor =
            Executors.newSingleThreadExecutor();

    // =========================================================
    // POLICY
    // =========================================================

    private TextView tvShiftTiming;
    private TextView tvWeeklyOff;

    // =========================================================
    // NAVIGATION
    // =========================================================

    private LinearLayout navHome;
    private LinearLayout navCalendar;
    private LinearLayout navLeave;

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

    private boolean attendanceLoaded = false;
    private boolean policyLoaded = false;

    // =========================================================
    // TIME HANDLER
    // =========================================================

    private final Handler workHoursHandler =
            new Handler(Looper.getMainLooper());

    private final Runnable workHoursRunnable =
            new Runnable() {

                @Override
                public void run() {

                    updateWorkHours();

                    if (isCheckedIn()) {

                        workHoursHandler.postDelayed(
                                this,
                                60000
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

        sessionManager =
                new SessionManager(this);

        // -----------------------------------------------------
        // SESSION CHECK
        // -----------------------------------------------------

        if (!sessionManager.isLoggedIn()) {

            openLogin();

            return;
        }

        // -----------------------------------------------------
        // API
        // -----------------------------------------------------

        apiService =
                ApiClient.getApiService(this);

        // -----------------------------------------------------
        // LOCATION CLIENT
        // -----------------------------------------------------

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(
                        this
                );

        // -----------------------------------------------------
        // USER
        // -----------------------------------------------------

        setupUser();

        // -----------------------------------------------------
        // HEADER
        // -----------------------------------------------------

        setupHeader();

        // -----------------------------------------------------
        // BUTTONS
        // -----------------------------------------------------

        setupPunchButtons();

        // -----------------------------------------------------
        // BOTTOM NAVIGATION
        // -----------------------------------------------------

        setupBottomNavigation();

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

        /*
         * IMPORTANT:
         *
         * API calls are NOT made here.
         *
         * They are made from onResume().
         *
         * This prevents duplicate API requests because
         * Android calls onResume() immediately after onCreate().
         */
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
         * Refresh all required Home data whenever
         * user returns to this screen.
         */

        loadTodayAttendance();

        loadAttendancePolicy();

        loadCurrentLocation();
    }

    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initializeViews() {

        // -----------------------------------------------------
        // HEADER
        // -----------------------------------------------------

        btnMenu =
                findViewById(R.id.btnMenu);

        btnNotification =
                findViewById(R.id.btnNotification);

        btnProfile =
                findViewById(R.id.btnProfile);

        // -----------------------------------------------------
        // USER
        // -----------------------------------------------------

        tvWelcome =
                findViewById(R.id.tvWelcome);

        tvUsername =
                findViewById(R.id.tvUsername);

        tvRole =
                findViewById(R.id.tvRole);

        tvTodayDate =
                findViewById(R.id.tvTodayDate);

        // -----------------------------------------------------
        // ATTENDANCE
        // -----------------------------------------------------

        tvAttendanceStatus =
                findViewById(R.id.tvAttendanceStatus);

        tvCheckInTime =
                findViewById(R.id.tvCheckInTime);

        tvCheckOutTime =
                findViewById(R.id.tvCheckOutTime);

        tvWorkHours =
                findViewById(R.id.tvWorkHours);

        // Existing buttons kept for compatibility.
        btnCheckIn = null;
        btnCheckOut = null;

        // -----------------------------------------------------
        // PUNCH
        // -----------------------------------------------------

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

        // -----------------------------------------------------
        // LOCATION
        // -----------------------------------------------------

        tvLocationName =
                findViewById(R.id.tvLocationName);

        tvLocationRadius =
                findViewById(R.id.tvLocationRadius);

        // -----------------------------------------------------
        // POLICY
        // -----------------------------------------------------

        tvShiftTiming =
                findViewById(R.id.tvShiftTiming);

        tvWeeklyOff =
                findViewById(R.id.tvWeeklyOff);

        // -----------------------------------------------------
        // NAVIGATION
        // -----------------------------------------------------

        navHome =
                findViewById(R.id.navHome);

        navCalendar =
                findViewById(R.id.navCalendar);

        navLeave =
                findViewById(R.id.navLeave);

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

        if (username == null
                || username.trim().isEmpty()) {

            username = "User";
        }

        if (role == null
                || role.trim().isEmpty()) {

            role = "USER";
        }

        tvUsername.setText(username);

        tvRole.setText(role);

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

        // -----------------------------------------------------
        // PROFILE CIRCLE
        // -----------------------------------------------------

        GradientDrawable profileBackground =
                new GradientDrawable();

        profileBackground.setShape(
                GradientDrawable.OVAL
        );

        profileBackground.setColor(
                Color.rgb(
                        238,
                        238,
                        238
                )
        );

        btnProfile.setBackground(
                profileBackground
        );

        // -----------------------------------------------------
        // MENU
        // -----------------------------------------------------

        btnMenu.setOnClickListener(
                v -> showMenu()
        );

        // -----------------------------------------------------
        // NOTIFICATION
        // -----------------------------------------------------

        btnNotification.setOnClickListener(
                v -> Toast.makeText(
                        MainActivity.this,
                        "No new notifications.",
                        Toast.LENGTH_SHORT
                ).show()
        );

        // -----------------------------------------------------
        // PROFILE
        // -----------------------------------------------------

        btnProfile.setOnClickListener(
                v -> openProfile()
        );
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

                    // -------------------------------------------------
                    // CALENDAR
                    // -------------------------------------------------

                    if ("Calendar".equals(title)) {

                        openCalendar();

                        return true;
                    }

                    // -------------------------------------------------
                    // LEAVE
                    // -------------------------------------------------

                    if ("Leave".equals(title)) {

                        openLeave();

                        return true;
                    }

                    // -------------------------------------------------
                    // PROFILE
                    // -------------------------------------------------

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
    // PUNCH BUTTONS
    // =========================================================

    private void setupPunchButtons() {

        btnMainPunch.setOnClickListener(
                v -> handleMainPunch()
        );

        // -----------------------------------------------------
        // VIEW CALENDAR
        // -----------------------------------------------------

        tvViewCalendar.setOnClickListener(
                v -> openCalendar()
        );
    }

    // =========================================================
    // MAIN PUNCH
    // =========================================================

    private void handleMainPunch() {

        // -----------------------------------------------------
        // ATTENDANCE NOT LOADED
        // -----------------------------------------------------

        if (!attendanceLoaded) {

            loadTodayAttendance();

            Toast.makeText(
                    this,
                    "Loading attendance...",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // -----------------------------------------------------
        // POLICY NOT LOADED
        // -----------------------------------------------------

        if (!policyLoaded) {

            loadAttendancePolicy();

            Toast.makeText(
                    this,
                    "Loading attendance policy...",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // -----------------------------------------------------
        // LOCATION NOT AVAILABLE
        // -----------------------------------------------------

        if (!locationLoaded
                || currentLatitude == null
                || currentLongitude == null) {

            loadCurrentLocation();

            Toast.makeText(
                    this,
                    "Current location is unavailable.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // -----------------------------------------------------
        // POLICY NOT AVAILABLE
        // -----------------------------------------------------

        if (attendancePolicy == null) {

            Toast.makeText(
                    this,
                    "Attendance policy is unavailable.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // -----------------------------------------------------
        // ATTENDANCE NOT MARKED
        // -----------------------------------------------------

        if (todayAttendance == null
                || isEmpty(
                todayAttendance.getCheckInTime()
        )) {

            openCamera(
                    CameraActivity.ACTION_CHECK_IN
            );

            return;
        }

        // -----------------------------------------------------
        // CHECKED IN BUT NOT CHECKED OUT
        // -----------------------------------------------------

        if (isEmpty(
                todayAttendance.getCheckOutTime()
        )) {

            openCamera(
                    CameraActivity.ACTION_CHECK_OUT
            );

            return;
        }

        // -----------------------------------------------------
        // COMPLETED
        // -----------------------------------------------------

        Toast.makeText(
                this,
                "Today's attendance is already completed.",
                Toast.LENGTH_SHORT
        ).show();
    }

    // =========================================================
    // OPEN CAMERA
    // =========================================================

    private void openCamera(String action) {

        Intent intent =
                new Intent(
                        MainActivity.this,
                        CameraActivity.class
                );

        intent.putExtra(
                CameraActivity.EXTRA_ACTION,
                action
        );

        // -----------------------------------------------------
        // INTERNAL DEVICE LOCATION
        // -----------------------------------------------------

        if (currentLatitude != null) {

            intent.putExtra(
                    "CURRENT_LATITUDE",
                    currentLatitude
            );
        }

        if (currentLongitude != null) {

            intent.putExtra(
                    "CURRENT_LONGITUDE",
                    currentLongitude
            );
        }

        startActivity(intent);
    }

    // =========================================================
    // CURRENT DEVICE LOCATION
    // =========================================================

    private void loadCurrentLocation() {

        if (fusedLocationClient == null) {

            return;
        }

        // -----------------------------------------------------
        // ALREADY LOADING
        // -----------------------------------------------------

        if (locationLoading) {

            return;
        }

        // -----------------------------------------------------
        // PERMISSION CHECK
        // -----------------------------------------------------

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            requestLocationPermission();

            return;
        }

        locationLoading = true;

        locationLoaded = false;

        showLocationLoading();

        // -----------------------------------------------------
        // GET CURRENT DEVICE GPS
        // -----------------------------------------------------

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

                                refreshPunchButton();

                                return;
                            }

                            // -------------------------------------------------
                            // SAVE DEVICE GPS INTERNALLY
                            // -------------------------------------------------

                            currentLatitude =
                                    location.getLatitude();

                            currentLongitude =
                                    location.getLongitude();

                            locationLoaded = true;

                            // -------------------------------------------------
                            // UPDATE READABLE LOCATION
                            // -------------------------------------------------

                            updateCurrentLocationUI();

                            refreshPunchButton();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            locationLoading = false;

                            locationLoaded = false;

                            currentLatitude = null;
                            currentLongitude = null;

                            showLocationUnavailable();

                            refreshPunchButton();
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

        if (requestCode
                != LOCATION_PERMISSION_REQUEST_CODE) {

            return;
        }

        boolean granted = false;

        for (int result : grantResults) {

            if (result
                    == PackageManager.PERMISSION_GRANTED) {

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

            refreshPunchButton();
        }
    }

    // =========================================================
    // LOCATION UI - LOADING
    // =========================================================

    private void showLocationLoading() {

        tvLocationName.setText(
                "Detecting current location..."
        );

        tvLocationRadius.setText(
                "Please wait"
        );
    }

    // =========================================================
    // LOCATION UI - SUCCESS
    // =========================================================

    private void updateCurrentLocationUI() {

        if (currentLatitude == null
                || currentLongitude == null) {

            showLocationUnavailable();

            return;
        }

        /*
         * Do NOT display:
         *
         * latitude
         * longitude
         * attendance radius
         *
         * Only show readable device location.
         */

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

            // -------------------------------------------------
            // CITY
            // -------------------------------------------------

            String locality =
                    address.getLocality();

            // -------------------------------------------------
            // DISTRICT
            // -------------------------------------------------

            String subAdminArea =
                    address.getSubAdminArea();

            // -------------------------------------------------
            // STATE
            // -------------------------------------------------

            String adminArea =
                    address.getAdminArea();

            StringBuilder location =
                    new StringBuilder();

            // -------------------------------------------------
            // LOCALITY
            // -------------------------------------------------

            if (!isEmpty(locality)) {

                location.append(
                        locality.trim()
                );
            }

            // -------------------------------------------------
            // DISTRICT
            // -------------------------------------------------

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

            // -------------------------------------------------
            // STATE
            // -------------------------------------------------

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

            // -------------------------------------------------
            // FALLBACK ADDRESS
            // -------------------------------------------------

            if (location.length() == 0) {

                String addressLine =
                        address.getAddressLine(0);

                if (!isEmpty(addressLine)) {

                    location.append(
                            addressLine.trim()
                    );
                }
            }

            // -------------------------------------------------
            // FINAL FALLBACK
            // -------------------------------------------------

            if (location.length() == 0) {

                return "Current location";
            }

            return location.toString();

        } catch (IOException e) {

            return "Current location";

        } catch (Exception e) {

            return "Current location";
        }
    }

    // =========================================================
    // CONTAINS IGNORE CASE
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
                        value
                                .trim()
                                .toLowerCase(
                                        Locale.getDefault()
                                )
                );
    }

    // =========================================================
    // LOCATION UI - UNAVAILABLE
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
    // LOCATION UI - PERMISSION
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

    private void loadTodayAttendance() {

        attendanceLoaded = false;

        refreshPunchButton();

        String today =
                apiDateFormat.format(
                        new Date()
                );

        apiService
                .getMyAttendanceByDate(today)
                .enqueue(
                        new Callback<AttendanceResponse>() {

                            @Override
                            public void onResponse(
                                    Call<AttendanceResponse> call,
                                    Response<AttendanceResponse> response) {

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
                                // NO ATTENDANCE FOR TODAY
                                // -------------------------------------------------

                                if (response.code() == 404) {

                                    todayAttendance = null;

                                    attendanceLoaded = true;

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

                                    attendanceLoaded = true;

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

                                attendanceLoaded = false;

                                tvAttendanceStatus.setText(
                                        "Unable to load"
                                );

                                refreshPunchButton();
                            }
                        }
                );
    }

    // =========================================================
    // UPDATE ATTENDANCE UI
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

        if (status == null
                || status.trim().isEmpty()) {

            status = "PRESENT";
        }

        tvAttendanceStatus.setText(
                status
        );

        // -----------------------------------------------------
        // PRESENT
        // -----------------------------------------------------

        if ("PRESENT".equalsIgnoreCase(status)) {

            tvAttendanceStatus.setTextColor(
                    Color.rgb(
                            30,
                            158,
                            90
                    )
            );

            tvAttendanceStatus.setBackgroundColor(
                    Color.rgb(
                            234,
                            248,
                            240
                    )
            );

        }

        // -----------------------------------------------------
        // ABSENT
        // -----------------------------------------------------

        else if ("ABSENT".equalsIgnoreCase(status)) {

            tvAttendanceStatus.setTextColor(
                    Color.rgb(
                            210,
                            60,
                            60
                    )
            );

        }

        // -----------------------------------------------------
        // HALF DAY
        // -----------------------------------------------------

        else if ("HD".equalsIgnoreCase(status)
                || "HALFDAY".equalsIgnoreCase(status)
                || "HALF_DAY".equalsIgnoreCase(status)) {

            tvAttendanceStatus.setTextColor(
                    Color.rgb(
                            220,
                            140,
                            40
                    )
            );
        }

        // -----------------------------------------------------
        // CHECK IN
        // -----------------------------------------------------

        if (isEmpty(checkIn)) {

            tvCheckInTime.setText(
                    "--:--"
            );

        } else {

            tvCheckInTime.setText(
                    formatDisplayTime(checkIn)
            );
        }

        // -----------------------------------------------------
        // CHECK OUT
        // -----------------------------------------------------

        if (isEmpty(checkOut)) {

            tvCheckOutTime.setText(
                    "--:--"
            );

        } else {

            tvCheckOutTime.setText(
                    formatDisplayTime(checkOut)
            );
        }

        // -----------------------------------------------------
        // WORK HOURS
        // -----------------------------------------------------

        updateWorkHours();

        // -----------------------------------------------------
        // PUNCH CARD
        // -----------------------------------------------------

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
                Color.rgb(
                        110,
                        110,
                        110
                )
        );

        tvAttendanceStatus.setBackgroundColor(
                Color.rgb(
                        240,
                        240,
                        240
                )
        );

        tvCheckInTime.setText(
                "--:--"
        );

        tvCheckOutTime.setText(
                "--:--"
        );

        tvWorkHours.setText(
                "--:--"
        );

        updatePunchCard();
    }

    // =========================================================
    // WORK HOURS
    // =========================================================

    private void updateWorkHours() {

        // -----------------------------------------------------
        // REMOVE OLD TIMER
        // -----------------------------------------------------

        workHoursHandler.removeCallbacks(
                workHoursRunnable
        );

        // -----------------------------------------------------
        // NO ATTENDANCE
        // -----------------------------------------------------

        if (todayAttendance == null) {

            tvWorkHours.setText(
                    "--:--"
            );

            return;
        }

        String checkIn =
                todayAttendance.getCheckInTime();

        String checkOut =
                todayAttendance.getCheckOutTime();

        // -----------------------------------------------------
        // NO CHECK-IN
        // -----------------------------------------------------

        if (isEmpty(checkIn)) {

            tvWorkHours.setText(
                    "--:--"
            );

            return;
        }

        // -----------------------------------------------------
        // PARSE CHECK-IN
        // -----------------------------------------------------

        Date checkInDate =
                parseAttendanceTime(checkIn);

        if (checkInDate == null) {

            tvWorkHours.setText(
                    "--:--"
            );

            return;
        }

        Date endDate;

        // -----------------------------------------------------
        // CHECKED OUT
        // -----------------------------------------------------

        if (!isEmpty(checkOut)) {

            endDate =
                    parseAttendanceTime(checkOut);

        }

        // -----------------------------------------------------
        // STILL CHECKED IN
        // -----------------------------------------------------

        else {

            /*
             * Important:
             *
             * checkInDate is attached to TODAY's date.
             *
             * Therefore current Date() is now comparable
             * with checkInDate.
             */

            endDate =
                    new Date();
        }

        if (endDate == null) {

            tvWorkHours.setText(
                    "--:--"
            );

            return;
        }

        // -----------------------------------------------------
        // DIFFERENCE
        // -----------------------------------------------------

        long difference =
                endDate.getTime()
                        - checkInDate.getTime();

        // -----------------------------------------------------
        // MIDNIGHT CROSSING
        // -----------------------------------------------------

        if (difference < 0) {

            difference +=
                    24L
                            * 60L
                            * 60L
                            * 1000L;
        }

        // -----------------------------------------------------
        // TOTAL MINUTES
        // -----------------------------------------------------

        long totalMinutes =
                difference
                        / (60L * 1000L);

        // -----------------------------------------------------
        // HOURS
        // -----------------------------------------------------

        long hours =
                totalMinutes / 60L;

        // -----------------------------------------------------
        // MINUTES
        // -----------------------------------------------------

        long minutes =
                totalMinutes % 60L;

        // -----------------------------------------------------
        // DISPLAY
        // -----------------------------------------------------
        //
        // Example:
        //
        // Check-in : 02:55 PM
        // Current  : 05:23 PM
        //
        // Work    : 02:28
        //
        // -----------------------------------------------------

        String workHours =
                String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        hours,
                        minutes
                );

        tvWorkHours.setText(
                workHours
        );

        // -----------------------------------------------------
        // UPDATE EVERY MINUTE
        // -----------------------------------------------------

        if (isEmpty(checkOut)) {

            workHoursHandler.postDelayed(
                    workHoursRunnable,
                    60000
            );
        }
    }

    // =========================================================
    // PARSE ATTENDANCE TIME
    // =========================================================
    //
    // API examples:
    //
    // 14:55:00
    // 14:55
    // 14:55:00.123
    //
    // Also supports:
    //
    // 2026-09-07T14:55:00
    // 2026-09-07 14:55:00
    //
    // IMPORTANT:
    //
    // Parsed time is attached to TODAY's date.
    //
    // This prevents:
    //
    // 1970 -> 2026
    //
    // huge work-hour calculation.
    //
    // =========================================================

    private Date parseAttendanceTime(String value) {

        if (isEmpty(value)) {

            return null;
        }

        String time =
                value.trim();

        // -----------------------------------------------------
        // REMOVE ISO DATE
        // -----------------------------------------------------

        if (time.contains("T")) {

            time =
                    time.substring(
                            time.indexOf("T") + 1
                    );
        }

        // -----------------------------------------------------
        // REMOVE SPACE DATE
        // -----------------------------------------------------

        if (time.contains(" ")) {

            String[] parts =
                    time.split("\\s+");

            time =
                    parts[parts.length - 1];
        }

        // -----------------------------------------------------
        // REMOVE Z
        // -----------------------------------------------------

        if (time.endsWith("Z")) {

            time =
                    time.substring(
                            0,
                            time.length() - 1
                    );
        }

        // -----------------------------------------------------
        // REMOVE TIMEZONE
        // -----------------------------------------------------

        int plusIndex =
                time.indexOf("+");

        if (plusIndex > 0) {

            time =
                    time.substring(
                            0,
                            plusIndex
                    );
        }

        // -----------------------------------------------------
        // REMOVE MILLISECONDS
        // -----------------------------------------------------

        int dotIndex =
                time.indexOf(".");

        if (dotIndex > 0) {

            time =
                    time.substring(
                            0,
                            dotIndex
                    );
        }

        // -----------------------------------------------------
        // PARSE TIME
        // -----------------------------------------------------

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

        // -----------------------------------------------------
        // EXTRACT ONLY TIME
        // -----------------------------------------------------

        Calendar parsedCalendar =
                Calendar.getInstance();

        parsedCalendar.setTime(
                parsedTime
        );

        int hour =
                parsedCalendar.get(
                        Calendar.HOUR_OF_DAY
                );

        int minute =
                parsedCalendar.get(
                        Calendar.MINUTE
                );

        int second =
                parsedCalendar.get(
                        Calendar.SECOND
                );

        int millisecond =
                parsedCalendar.get(
                        Calendar.MILLISECOND
                );

        // -----------------------------------------------------
        // ATTACH TIME TO TODAY
        // -----------------------------------------------------

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

            btnMainPunch.setText(
                    "▶"
            );

            btnMainPunch.setEnabled(
                    attendanceLoaded
                            && policyLoaded
                            && locationLoaded
            );

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

            btnMainPunch.setText(
                    "▶"
            );

            btnMainPunch.setEnabled(
                    attendanceLoaded
                            && policyLoaded
                            && locationLoaded
            );

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

            btnMainPunch.setText(
                    "▶"
            );

            btnMainPunch.setEnabled(
                    attendanceLoaded
                            && policyLoaded
                            && locationLoaded
            );

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

        btnMainPunch.setText(
                "✓"
        );

        btnMainPunch.setEnabled(
                false
        );
    }

    // =========================================================
    // REFRESH PUNCH BUTTON
    // =========================================================

    private void refreshPunchButton() {

        if (!attendanceLoaded
                || !policyLoaded
                || !locationLoaded) {

            btnMainPunch.setEnabled(
                    false
            );

            return;
        }

        if (attendancePolicy == null) {

            btnMainPunch.setEnabled(
                    false
            );

            return;
        }

        /*
         * No getAttendanceRequired() check here.
         *
         * This avoids compilation problems if that
         * field/method does not exist in your DTO.
         */

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

        return formatter.format(
                new Date()
        );
    }

    // =========================================================
    // POLICY
    // =========================================================

    private void loadAttendancePolicy() {

        policyLoaded = false;

        refreshPunchButton();

        apiService
                .getCurrentPolicy()
                .enqueue(
                        new Callback<AttendancePolicyResponse>() {

                            @Override
                            public void onResponse(
                                    Call<AttendancePolicyResponse> call,
                                    Response<AttendancePolicyResponse> response) {

                                // -------------------------------------------------
                                // SUCCESS
                                // -------------------------------------------------

                                if (response.isSuccessful()
                                        && response.body() != null) {

                                    attendancePolicy =
                                            response.body();

                                    policyLoaded = true;

                                    updatePolicyUI();

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
                                // POLICY ERROR
                                // -------------------------------------------------

                                attendancePolicy = null;

                                policyLoaded = false;

                                showPolicyUnavailable();

                                refreshPunchButton();
                            }

                            @Override
                            public void onFailure(
                                    Call<AttendancePolicyResponse> call,
                                    Throwable t) {

                                attendancePolicy = null;

                                policyLoaded = false;

                                showPolicyUnavailable();

                                refreshPunchButton();
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

        /*
         * IMPORTANT:
         *
         * Policy latitude
         * Policy longitude
         * Attendance radius
         *
         * are NOT displayed.
         *
         * They are backend validation values only.
         */

        // -----------------------------------------------------
        // WORKING HOURS
        // -----------------------------------------------------

        if (attendancePolicy.getWorkingHours()
                != null) {

            /*
             * Avoid stripTrailingZeros().toPlainString()
             * because DTO type may not always be BigDecimal.
             */

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

        // -----------------------------------------------------
        // WEEKLY OFF
        // -----------------------------------------------------

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

        /*
         * IMPORTANT:
         *
         * Do NOT touch:
         *
         * tvLocationName
         * tvLocationRadius
         *
         * Policy API failure has nothing to do with
         * current device GPS.
         */
    }

    // =========================================================
    // INITIAL ATTENDANCE STATE
    // =========================================================

    private void showInitialAttendanceState() {

        tvAttendanceStatus.setText(
                "Loading"
        );

        tvCheckInTime.setText(
                "--:--"
        );

        tvCheckOutTime.setText(
                "--:--"
        );

        tvWorkHours.setText(
                "--:--"
        );

        tvPunchAction.setText(
                "PUNCH IN"
        );

        tvPunchTime.setText(
                getCurrentTime()
        );

        btnMainPunch.setEnabled(
                false
        );
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
                displayDateFormat.format(
                        new Date()
                )
        );
    }

    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    private void setupBottomNavigation() {

        // -----------------------------------------------------
        // HOME
        // -----------------------------------------------------

        navHome.setOnClickListener(
                v -> {
                    /*
                     * Already on Home.
                     */
                }
        );

        // -----------------------------------------------------
        // CALENDAR
        // -----------------------------------------------------

        navCalendar.setOnClickListener(
                v -> openCalendar()
        );

        // -----------------------------------------------------
        // LEAVE
        // -----------------------------------------------------

        navLeave.setOnClickListener(
                v -> openLeave()
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

        try {

            Class<?> activityClass =
                    Class.forName(
                            getPackageName()
                                    + ".activity.ProfileActivity"
                    );

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            activityClass
                    );

            startActivity(intent);

        } catch (ClassNotFoundException e) {

            Toast.makeText(
                    MainActivity.this,
                    "ProfileActivity is not created yet.",
                    Toast.LENGTH_SHORT
            ).show();
        }
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
    // LOGIN
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
    // EMPTY STRING
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
}