package com.college.attendance.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.college.attendance.R;
import com.college.attendance.api.ApiClient;
import com.college.attendance.api.ApiService;
import com.college.attendance.dto.AttendanceResponse;
import com.college.attendance.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    // =============================================================
    // VIEWS
    // =============================================================

    private TextView tvUsername;
    private TextView tvTodayDate;
    private TextView tvAttendanceStatus;
    private TextView tvAttendanceMessage;
    private TextView tvCheckInTime;
    private TextView tvCheckOutTime;

    private Button btnCheckIn;
    private Button btnCheckOut;
    private Button btnCalendar;
    private Button btnLogout;


    // =============================================================
    // API / SESSION
    // =============================================================

    private SessionManager sessionManager;
    private ApiService apiService;


    // =============================================================
    // LIFECYCLE
    // =============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);


        // =========================================================
        // SYSTEM BAR HANDLING
        // =========================================================

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );


        // =========================================================
        // INITIALIZE SESSION + API
        // =========================================================

        sessionManager =
                new SessionManager(this);

        apiService =
                ApiClient.getApiService(this);


        // =========================================================
        // CHECK LOGIN SESSION
        // =========================================================

        if (!sessionManager.isLoggedIn()) {

            openLoginActivity();

            return;
        }


        // =========================================================
        // INITIALIZE VIEWS
        // =========================================================

        initializeViews();


        // =========================================================
        // LOAD USER INFORMATION
        // =========================================================

        loadUserInformation();


        // =========================================================
        // DISPLAY TODAY'S DATE
        // =========================================================

        showTodayDate();


        // =========================================================
        // BUTTON LISTENERS
        // =========================================================

        setupClickListeners();


        // =========================================================
        // LOAD TODAY'S ATTENDANCE
        // =========================================================

        loadTodayAttendance();
    }


    // =============================================================
    // INITIALIZE VIEWS
    // =============================================================

    private void initializeViews() {

        tvUsername =
                findViewById(R.id.tvUsername);

        tvTodayDate =
                findViewById(R.id.tvTodayDate);

        tvAttendanceStatus =
                findViewById(R.id.tvAttendanceStatus);

        tvAttendanceMessage =
                findViewById(R.id.tvAttendanceMessage);

        tvCheckInTime =
                findViewById(R.id.tvCheckInTime);

        tvCheckOutTime =
                findViewById(R.id.tvCheckOutTime);


        btnCheckIn =
                findViewById(R.id.btnCheckIn);

        btnCheckOut =
                findViewById(R.id.btnCheckOut);

        btnCalendar =
                findViewById(R.id.btnCalendar);

        btnLogout =
                findViewById(R.id.btnLogout);
    }


    // =============================================================
    // LOAD USER INFORMATION
    // =============================================================

    private void loadUserInformation() {

        String username =
                sessionManager.getUsername();

        String role =
                sessionManager.getRole();


        // =========================================================
        // USERNAME
        // =========================================================

        if (username != null
                && !username.trim().isEmpty()) {

            tvUsername.setText(username);

        } else {

            tvUsername.setText("User");
        }


        // =========================================================
        // ROLE
        // =========================================================

        if (role != null
                && !role.trim().isEmpty()) {

            role =
                    role.toUpperCase(Locale.getDefault());

            /*
             * Currently supported Android roles:
             *
             * STUDENT
             * TEACHER
             *
             * ADMIN is handled by the web application.
             */

            if ("STUDENT".equals(role)) {

                // Student specific UI can be added here.

            } else if ("TEACHER".equals(role)) {

                // Teacher specific UI can be added here.
            }
        }
    }


    // =============================================================
    // SHOW TODAY'S DATE
    // =============================================================

    private void showTodayDate() {

        String today =
                new SimpleDateFormat(
                        "dd MMMM yyyy",
                        Locale.getDefault()
                ).format(new Date());

        tvTodayDate.setText(today);
    }


    // =============================================================
    // LOAD TODAY'S ATTENDANCE
    // =============================================================

    private void loadTodayAttendance() {

        String today =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.getDefault()
                ).format(new Date());


        apiService
                .getMyAttendanceByDate(today)
                .enqueue(
                        new Callback<AttendanceResponse>() {

                            @Override
                            public void onResponse(
                                    Call<AttendanceResponse> call,
                                    Response<AttendanceResponse> response) {


                                // =================================================
                                // SUCCESS
                                // =================================================

                                if (response.isSuccessful()
                                        && response.body() != null) {

                                    AttendanceResponse attendance =
                                            response.body();

                                    updateAttendanceUI(
                                            attendance
                                    );

                                    return;
                                }


                                // =================================================
                                // NO ATTENDANCE FOR TODAY
                                // =================================================

                                if (response.code() == 404) {

                                    showNotMarked();

                                    return;
                                }


                                // =================================================
                                // UNAUTHORIZED
                                // =================================================

                                if (response.code() == 401) {

                                    handleSessionExpired();

                                    return;
                                }


                                // =================================================
                                // FORBIDDEN
                                // =================================================

                                if (response.code() == 403) {

                                    Toast.makeText(
                                            MainActivity.this,
                                            "You are not authorized for this action",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    return;
                                }


                                // =================================================
                                // OTHER ERROR
                                // =================================================

                                Toast.makeText(
                                        MainActivity.this,
                                        "Unable to load today's attendance",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }


                            @Override
                            public void onFailure(
                                    Call<AttendanceResponse> call,
                                    Throwable t) {

                                /*
                                 * Ignore callback if Activity is no
                                 * longer active.
                                 */

                                if (isFinishing()
                                        || isDestroyed()) {

                                    return;
                                }


                                Toast.makeText(
                                        MainActivity.this,
                                        "Unable to connect to server",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                );
    }


    // =============================================================
    // UPDATE ATTENDANCE UI
    // =============================================================

    private void updateAttendanceUI(
            AttendanceResponse attendance) {


        // =========================================================
        // STATUS
        // =========================================================

        String status =
                attendance.getStatus();


        if (status != null
                && !status.trim().isEmpty()) {

            tvAttendanceStatus.setText(
                    status.toUpperCase(
                            Locale.getDefault()
                    )
            );

        } else {

            tvAttendanceStatus.setText(
                    "PRESENT"
            );
        }


        // =========================================================
        // MESSAGE
        // =========================================================

        tvAttendanceMessage.setText(
                "Attendance marked successfully"
        );


        // =========================================================
        // CHECK-IN TIME
        // =========================================================

        String checkInTime =
                attendance.getCheckInTime();


        if (checkInTime != null
                && !checkInTime.trim().isEmpty()) {

            tvCheckInTime.setText(
                    formatTime(checkInTime)
            );

        } else {

            tvCheckInTime.setText(
                    "--:--"
            );
        }


        // =========================================================
        // CHECK-OUT TIME
        // =========================================================

        String checkOutTime =
                attendance.getCheckOutTime();


        if (checkOutTime != null
                && !checkOutTime.trim().isEmpty()) {

            tvCheckOutTime.setText(
                    formatTime(checkOutTime)
            );

        } else {

            tvCheckOutTime.setText(
                    "--:--"
            );
        }


        // =========================================================
        // CHECK-IN BUTTON
        // =========================================================

        if (checkInTime != null
                && !checkInTime.trim().isEmpty()) {

            // Already checked in
            btnCheckIn.setEnabled(false);

            // Can check out
            btnCheckOut.setEnabled(true);

        } else {

            // Not checked in yet
            btnCheckIn.setEnabled(true);

            // Cannot check out before check-in
            btnCheckOut.setEnabled(false);
        }


        // =========================================================
        // CHECK-OUT BUTTON
        // =========================================================

        if (checkOutTime != null
                && !checkOutTime.trim().isEmpty()) {

            // Already checked out
            btnCheckOut.setEnabled(false);
        }
    }


    // =============================================================
    // SHOW NOT MARKED
    // =============================================================

    private void showNotMarked() {

        tvAttendanceStatus.setText(
                "NOT MARKED"
        );

        tvAttendanceMessage.setText(
                "Mark your attendance for today"
        );

        tvCheckInTime.setText(
                "--:--"
        );

        tvCheckOutTime.setText(
                "--:--"
        );


        // User can check in
        btnCheckIn.setEnabled(true);

        // User cannot check out yet
        btnCheckOut.setEnabled(false);
    }


    // =============================================================
    // FORMAT TIME
    // =============================================================

    private String formatTime(String time) {

        if (time == null
                || time.trim().isEmpty()) {

            return "--:--";
        }


        try {

            /*
             * Backend may return:
             *
             * HH:mm:ss
             *
             * Example:
             * 09:35:42
             *
             * We display:
             * 09:35
             */

            if (time.length() >= 8) {

                return time.substring(0, 5);
            }

            return time;

        } catch (Exception e) {

            return time;
        }
    }


    // =============================================================
    // CLICK LISTENERS
    // =============================================================

    private void setupClickListeners() {


        // =========================================================
        // CHECK-IN
        // =========================================================

        btnCheckIn.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            CameraActivity.class
                    );


            intent.putExtra(
                    CameraActivity.EXTRA_ACTION,
                    "CHECK_IN"
            );


            startActivity(intent);
        });


        // =========================================================
        // CHECK-OUT
        // =========================================================

        btnCheckOut.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            CameraActivity.class
                    );


            intent.putExtra(
                    CameraActivity.EXTRA_ACTION,
                    "CHECK_OUT"
            );


            startActivity(intent);
        });


        // =========================================================
        // CALENDAR
        // =========================================================

        btnCalendar.setOnClickListener(v -> {

            Toast.makeText(
                    MainActivity.this,
                    "Calendar screen will be added next",
                    Toast.LENGTH_SHORT
            ).show();

            /*
             * Later:
             *
             * Intent intent =
             *     new Intent(
             *         MainActivity.this,
             *         CalendarActivity.class
             *     );
             *
             * startActivity(intent);
             */
        });


        // =========================================================
        // LOGOUT
        // =========================================================

        btnLogout.setOnClickListener(
                v -> logout()
        );
    }


    // =============================================================
    // LOGOUT
    // =============================================================

    private void logout() {

        sessionManager.logout();

        openLoginActivity();
    }


    // =============================================================
    // OPEN LOGIN ACTIVITY
    // =============================================================

    private void openLoginActivity() {

        Intent intent =
                new Intent(
                        MainActivity.this,
                        LoginActivity.class
                );


        /*
         * Clear complete back stack.
         *
         * User cannot return to MainActivity
         * after logout.
         */

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );


        startActivity(intent);

        finish();
    }


    // =============================================================
    // SESSION EXPIRED
    // =============================================================

    private void handleSessionExpired() {

        Toast.makeText(
                MainActivity.this,
                "Session expired. Please login again.",
                Toast.LENGTH_LONG
        ).show();


        sessionManager.logout();

        openLoginActivity();
    }


    // =============================================================
    // ACTIVITY RESUME
    // =============================================================

    @Override
    protected void onResume() {

        super.onResume();


        /*
         * CameraActivity se wapas aane ke baad
         * attendance refresh hogi.
         *
         * Example:
         *
         * MainActivity
         *      ↓
         * CameraActivity
         *      ↓
         * Face Verification
         *      ↓
         * Attendance Marked
         *      ↓
         * MainActivity
         *
         * onResume()
         *      ↓
         * Today's attendance reload
         */

        if (apiService != null
                && sessionManager != null
                && sessionManager.isLoggedIn()) {

            loadTodayAttendance();
        }
    }
}

