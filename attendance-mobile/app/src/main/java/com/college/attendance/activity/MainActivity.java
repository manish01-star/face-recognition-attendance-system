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

    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        // Handle system bars
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

        // Initialize
        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService(this);

        // Initialize views
        initializeViews();

        // Display user information
        loadUserInformation();

        // Display today's date
        showTodayDate();

        // Load today's attendance
        loadTodayAttendance();

        // Button listeners
        setupClickListeners();
    }

    private void initializeViews() {

        tvUsername = findViewById(R.id.tvUsername);
        tvTodayDate = findViewById(R.id.tvTodayDate);
        tvAttendanceStatus = findViewById(R.id.tvAttendanceStatus);
        tvAttendanceMessage = findViewById(R.id.tvAttendanceMessage);
        tvCheckInTime = findViewById(R.id.tvCheckInTime);
        tvCheckOutTime = findViewById(R.id.tvCheckOutTime);

        btnCheckIn = findViewById(R.id.btnCheckIn);
        btnCheckOut = findViewById(R.id.btnCheckOut);
        btnCalendar = findViewById(R.id.btnCalendar);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void loadUserInformation() {

        String username =
                sessionManager.getUsername();

        if (username != null && !username.trim().isEmpty()) {

            tvUsername.setText(username);

        } else {

            tvUsername.setText("User");
        }
    }

    private void showTodayDate() {

        String today =
                new SimpleDateFormat(
                        "dd MMMM yyyy",
                        Locale.getDefault()
                ).format(new Date());

        tvTodayDate.setText(today);
    }

    private void loadTodayAttendance() {

        String today =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.getDefault()
                ).format(new Date());

        apiService.getMyAttendanceByDate(today)
                .enqueue(new Callback<AttendanceResponse>() {

                    @Override
                    public void onResponse(
                            Call<AttendanceResponse> call,
                            Response<AttendanceResponse> response) {

                        if (response.isSuccessful()
                                && response.body() != null) {

                            AttendanceResponse attendance =
                                    response.body();

                            updateAttendanceUI(attendance);

                        } else if (response.code() == 404) {

                            // No attendance for today
                            showNotMarked();

                        } else {

                            Toast.makeText(
                                    MainActivity.this,
                                    "Unable to load today's attendance",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<AttendanceResponse> call,
                            Throwable t) {

                        Toast.makeText(
                                MainActivity.this,
                                "Unable to connect to server",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private void updateAttendanceUI(
            AttendanceResponse attendance) {

        String status = attendance.getStatus();

        if (status != null && !status.trim().isEmpty()) {

            tvAttendanceStatus.setText(
                    status.toUpperCase(Locale.getDefault())
            );

        } else {

            tvAttendanceStatus.setText("PRESENT");
        }

        tvAttendanceMessage.setText(
                "Attendance marked successfully"
        );

        // Check-in time
        String checkInTime =
                attendance.getCheckInTime();

        if (checkInTime != null
                && !checkInTime.trim().isEmpty()) {

            tvCheckInTime.setText(
                    formatTime(checkInTime)
            );

        } else {

            tvCheckInTime.setText("--:--");
        }

        // Check-out time
        String checkOutTime =
                attendance.getCheckOutTime();

        if (checkOutTime != null
                && !checkOutTime.trim().isEmpty()) {

            tvCheckOutTime.setText(
                    formatTime(checkOutTime)
            );

        } else {

            tvCheckOutTime.setText("--:--");
        }

        // Check-in already done
        if (checkInTime != null
                && !checkInTime.trim().isEmpty()) {

            btnCheckIn.setEnabled(false);
            btnCheckOut.setEnabled(true);

        } else {

            btnCheckIn.setEnabled(true);
            btnCheckOut.setEnabled(false);
        }

        // Check-out already done
        if (checkOutTime != null
                && !checkOutTime.trim().isEmpty()) {

            btnCheckOut.setEnabled(false);
        }
    }

    private void showNotMarked() {

        tvAttendanceStatus.setText("NOT MARKED");

        tvAttendanceMessage.setText(
                "Mark your attendance for today"
        );

        tvCheckInTime.setText("--:--");
        tvCheckOutTime.setText("--:--");

        btnCheckIn.setEnabled(true);
        btnCheckOut.setEnabled(false);
    }

    private String formatTime(String time) {

        if (time == null || time.trim().isEmpty()) {
            return "--:--";
        }

        try {

            // Backend may return HH:mm:ss
            if (time.length() >= 8) {

                return time.substring(0, 5);
            }

            return time;

        } catch (Exception e) {

            return time;
        }
    }

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

            // Next step:
            // Open CalendarActivity
        });


        // =========================================================
        // LOGOUT
        // =========================================================

        btnLogout.setOnClickListener(v -> logout());
    }

    private void logout() {

        sessionManager.logout();

        Intent intent =
                new Intent(
                        MainActivity.this,
                        LoginActivity.class
                );

        // Clear previous activities
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
         * CameraActivity se wapas aane ke baad
         * today's attendance dobara load hogi.
         *
         * Isse successful check-in/check-out ke baad
         * UI automatically refresh ho jayegi.
         */
        if (apiService != null) {
            loadTodayAttendance();
        }
    }
}
