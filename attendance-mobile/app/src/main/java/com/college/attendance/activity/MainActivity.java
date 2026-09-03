package com.college.attendance.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.college.attendance.R;
import com.college.attendance.api.ApiClient;
import com.college.attendance.api.ApiService;
import com.college.attendance.dto.AttendanceResponse;
import com.college.attendance.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private TextView tvUsername;
    private TextView tvRole;
    private TextView tvTodayDate;

    private TextView tvAttendanceStatus;
    private TextView tvCheckInTime;
    private TextView tvCheckOutTime;

    private MaterialButton btnCheckIn;
    private MaterialButton btnCheckOut;

    private ImageButton btnPreviousMonth;
    private ImageButton btnNextMonth;

    private TextView tvCalendarMonth;
    private GridLayout calendarGrid;

    private TextView tvSelectedDate;
    private TextView tvSelectedStatus;
    private TextView tvSelectedCheckIn;
    private TextView tvSelectedCheckOut;

    private SessionManager sessionManager;
    private ApiService apiService;

    /*
     * Key   = yyyy-MM-dd
     * Value = AttendanceResponse
     */
    private final Map<String, AttendanceResponse> attendanceMap =
            new HashMap<>();

    private final Calendar currentCalendar =
            Calendar.getInstance();

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
            );

    private final SimpleDateFormat displayDateFormat =
            new SimpleDateFormat(
                    "dd MMMM yyyy",
                    Locale.getDefault()
            );

    private final SimpleDateFormat monthFormat =
            new SimpleDateFormat(
                    "MMMM yyyy",
                    Locale.getDefault()
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initializeViews();

        sessionManager = new SessionManager(this);

        /*
         * If user is not logged in,
         * open LoginActivity.
         */
        if (!sessionManager.isLoggedIn()) {
            openLogin();
            return;
        }

        apiService = ApiClient.getApiService(this);

        setupUserInfo();
        setupButtons();

        /*
         * Calendar initially opens on current month.
         */
        Calendar today = Calendar.getInstance();

        currentCalendar.set(
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                1
        );

        updateTodayDate();

        /*
         * Draw calendar immediately.
         * Attendance will be marked after API response.
         */
        renderCalendar();
    }

    private void initializeViews() {

        tvWelcome = findViewById(R.id.tvWelcome);
        tvUsername = findViewById(R.id.tvUsername);
        tvRole = findViewById(R.id.tvRole);
        tvTodayDate = findViewById(R.id.tvTodayDate);

        tvAttendanceStatus =
                findViewById(R.id.tvAttendanceStatus);

        tvCheckInTime =
                findViewById(R.id.tvCheckInTime);

        tvCheckOutTime =
                findViewById(R.id.tvCheckOutTime);

        btnCheckIn =
                findViewById(R.id.btnCheckIn);

        btnCheckOut =
                findViewById(R.id.btnCheckOut);

        btnPreviousMonth =
                findViewById(R.id.btnPreviousMonth);

        btnNextMonth =
                findViewById(R.id.btnNextMonth);

        tvCalendarMonth =
                findViewById(R.id.tvCalendarMonth);

        calendarGrid =
                findViewById(R.id.calendarGrid);

        tvSelectedDate =
                findViewById(R.id.tvSelectedDate);

        tvSelectedStatus =
                findViewById(R.id.tvSelectedStatus);

        tvSelectedCheckIn =
                findViewById(R.id.tvSelectedCheckIn);

        tvSelectedCheckOut =
                findViewById(R.id.tvSelectedCheckOut);
    }

    private void setupUserInfo() {

        String username = sessionManager.getUsername();
        String role = sessionManager.getRole();

        if (username == null || username.trim().isEmpty()) {
            username = "User";
        }

        if (role == null || role.trim().isEmpty()) {
            role = "USER";
        }

        tvWelcome.setText("Welcome");
        tvUsername.setText(username);
        tvRole.setText(role);
    }

    private void setupButtons() {

        /*
         * CHECK IN
         */
        btnCheckIn.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    CameraActivity.class
            );

            intent.putExtra(
                    "ATTENDANCE_ACTION",
                    "CHECK_IN"
            );

            startActivity(intent);
        });

        /*
         * CHECK OUT
         */
        btnCheckOut.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    CameraActivity.class
            );

            intent.putExtra(
                    "ATTENDANCE_ACTION",
                    "CHECK_OUT"
            );

            startActivity(intent);
        });

        /*
         * PREVIOUS MONTH
         */
        btnPreviousMonth.setOnClickListener(v -> {

            currentCalendar.add(
                    Calendar.MONTH,
                    -1
            );

            renderCalendar();
        });

        /*
         * NEXT MONTH
         */
        btnNextMonth.setOnClickListener(v -> {

            currentCalendar.add(
                    Calendar.MONTH,
                    1
            );

            renderCalendar();
        });
    }

    private void updateTodayDate() {

        Calendar today = Calendar.getInstance();

        tvTodayDate.setText(
                "Today, " +
                        displayDateFormat.format(
                                today.getTime()
                        )
        );
    }

    /*
     * ==========================================
     * ACTIVITY RESUME
     * ==========================================
     */

    @Override
    protected void onResume() {
        super.onResume();

        /*
         * This runs:
         *
         * 1. When MainActivity opens
         * 2. When coming back from CameraActivity
         *
         * Therefore today's attendance and
         * calendar are automatically refreshed.
         */
        if (sessionManager != null
                && sessionManager.isLoggedIn()
                && apiService != null) {

            loadTodayAttendance();
            loadAllAttendance();
        }
    }

    /*
     * ==========================================
     * TODAY ATTENDANCE
     * ==========================================
     */

    private void loadTodayAttendance() {

        String today =
                dateFormat.format(
                        Calendar.getInstance().getTime()
                );

        apiService
                .getMyAttendanceByDate(today)
                .enqueue(new Callback<AttendanceResponse>() {

                    @Override
                    public void onResponse(
                            Call<AttendanceResponse> call,
                            Response<AttendanceResponse> response) {

                        if (response.isSuccessful()
                                && response.body() != null) {

                            updateAttendanceUI(
                                    response.body()
                            );

                            return;
                        }

                        if (response.code() == 404) {

                            showNotMarked();

                            return;
                        }

                        if (response.code() == 401) {

                            handleSessionExpired();

                            return;
                        }

                        if (response.code() == 403) {

                            Toast.makeText(
                                    MainActivity.this,
                                    "You are not authorized.",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        showNotMarked();
                    }

                    @Override
                    public void onFailure(
                            Call<AttendanceResponse> call,
                            Throwable t) {

                        showNotMarked();
                    }
                });
    }

    private void updateAttendanceUI(
            AttendanceResponse attendance) {

        String status = attendance.getStatus();

        if (status == null
                || status.trim().isEmpty()) {

            status = "PRESENT";
        }

        tvAttendanceStatus.setText(
                "Status: " + status
        );

        String checkIn =
                attendance.getCheckInTime();

        String checkOut =
                attendance.getCheckOutTime();

        if (checkIn == null
                || checkIn.trim().isEmpty()) {

            tvCheckInTime.setText(
                    "Check-in: --"
            );

        } else {

            tvCheckInTime.setText(
                    "Check-in: " + checkIn
            );
        }

        if (checkOut == null
                || checkOut.trim().isEmpty()) {

            tvCheckOutTime.setText(
                    "Check-out: --"
            );

        } else {

            tvCheckOutTime.setText(
                    "Check-out: " + checkOut
            );
        }
    }

    private void showNotMarked() {

        tvAttendanceStatus.setText(
                "Attendance not marked"
        );

        tvCheckInTime.setText(
                "Check-in: --"
        );

        tvCheckOutTime.setText(
                "Check-out: --"
        );
    }

    /*
     * ==========================================
     * COMPLETE ATTENDANCE
     * ==========================================
     */

    private void loadAllAttendance() {

        apiService
                .getMyAttendance()
                .enqueue(new Callback<List<AttendanceResponse>>() {

                    @Override
                    public void onResponse(
                            Call<List<AttendanceResponse>> call,
                            Response<List<AttendanceResponse>> response) {

                        if (response.code() == 401) {

                            handleSessionExpired();

                            return;
                        }

                        if (response.code() == 403) {

                            Toast.makeText(
                                    MainActivity.this,
                                    "You are not authorized.",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        if (!response.isSuccessful()
                                || response.body() == null) {

                            return;
                        }

                        attendanceMap.clear();

                        List<AttendanceResponse> list =
                                response.body();

                        for (AttendanceResponse attendance : list) {

                            if (attendance == null) {
                                continue;
                            }

                            String date =
                                    attendance.getAttendanceDate();

                            if (date == null
                                    || date.trim().isEmpty()) {
                                continue;
                            }

                            /*
                             * Expected:
                             *
                             * 2026-09-03
                             *
                             * If backend returns:
                             *
                             * 2026-09-03T00:00:00
                             *
                             * only date portion is used.
                             */
                            date = date.trim();

                            if (date.length() >= 10) {
                                date = date.substring(0, 10);
                            }

                            attendanceMap.put(
                                    date,
                                    attendance
                            );
                        }

                        /*
                         * Refresh calendar with attendance marks.
                         */
                        renderCalendar();
                    }

                    @Override
                    public void onFailure(
                            Call<List<AttendanceResponse>> call,
                            Throwable t) {

                        /*
                         * Calendar remains visible even
                         * if attendance API fails.
                         */
                    }
                });
    }

    /*
     * ==========================================
     * CALENDAR
     * ==========================================
     */

    private void renderCalendar() {

        calendarGrid.removeAllViews();

        tvCalendarMonth.setText(
                monthFormat.format(
                        currentCalendar.getTime()
                )
        );

        Calendar firstDay =
                (Calendar) currentCalendar.clone();

        firstDay.set(
                Calendar.DAY_OF_MONTH,
                1
        );

        /*
         * Sunday = 1
         * Monday = 2
         * ...
         * Saturday = 7
         *
         * Convert to zero-based index.
         */
        int firstDayPosition =
                firstDay.get(Calendar.DAY_OF_WEEK) - 1;

        int daysInMonth =
                currentCalendar.getActualMaximum(
                        Calendar.DAY_OF_MONTH
                );

        /*
         * Empty cells before first day.
         */
        for (int i = 0; i < firstDayPosition; i++) {

            addEmptyCalendarCell();
        }

        /*
         * Month dates.
         */
        for (int day = 1;
             day <= daysInMonth;
             day++) {

            Calendar dateCalendar =
                    (Calendar) currentCalendar.clone();

            dateCalendar.set(
                    Calendar.DAY_OF_MONTH,
                    day
            );

            String dateKey =
                    dateFormat.format(
                            dateCalendar.getTime()
                    );

            AttendanceResponse attendance =
                    attendanceMap.get(dateKey);

            addCalendarDay(
                    day,
                    dateKey,
                    attendance
            );
        }

        /*
         * Complete 6 rows x 7 columns.
         */
        int totalCells =
                firstDayPosition + daysInMonth;

        int remainingCells =
                42 - totalCells;

        for (int i = 0; i < remainingCells; i++) {

            addEmptyCalendarCell();
        }
    }

    private void addEmptyCalendarCell() {

        TextView emptyView =
                new TextView(this);

        GridLayout.LayoutParams params =
                new GridLayout.LayoutParams();

        params.width = 0;
        params.height = dpToPx(48);

        params.columnSpec =
                GridLayout.spec(
                        GridLayout.UNDEFINED,
                        1f
                );

        emptyView.setLayoutParams(params);

        calendarGrid.addView(emptyView);
    }

    private void addCalendarDay(
            int day,
            String dateKey,
            AttendanceResponse attendance) {

        TextView dayView =
                new TextView(this);

        GridLayout.LayoutParams params =
                new GridLayout.LayoutParams();

        params.width = 0;
        params.height = dpToPx(48);

        params.columnSpec =
                GridLayout.spec(
                        GridLayout.UNDEFINED,
                        1f
                );

        params.setMargins(
                dpToPx(2),
                dpToPx(2),
                dpToPx(2),
                dpToPx(2)
        );

        dayView.setLayoutParams(params);

        dayView.setGravity(
                Gravity.CENTER
        );

        dayView.setText(
                String.valueOf(day)
        );

        dayView.setTextSize(14);

        dayView.setTextColor(
                Color.rgb(31, 41, 55)
        );

        /*
         * Attendance exists.
         */
        if (attendance != null) {

            dayView.setBackgroundResource(
                    R.drawable.calendar_present
            );

            dayView.setTextColor(
                    Color.rgb(21, 128, 61)
            );

            dayView.setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
            );
        }

        /*
         * Today without attendance.
         */
        String today =
                dateFormat.format(
                        Calendar.getInstance().getTime()
                );

        if (today.equals(dateKey)
                && attendance == null) {

            dayView.setTextColor(
                    Color.rgb(37, 99, 235)
            );

            dayView.setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
            );
        }

        /*
         * Click date.
         */
        dayView.setOnClickListener(v ->
                showSelectedDate(
                        dateKey,
                        attendance
                )
        );

        calendarGrid.addView(dayView);
    }

    /*
     * ==========================================
     * SELECTED DATE DETAILS
     * ==========================================
     */

    private void showSelectedDate(
            String dateKey,
            AttendanceResponse attendance) {

        try {

            Calendar selectedCalendar =
                    Calendar.getInstance();

            selectedCalendar.setTime(
                    dateFormat.parse(dateKey)
            );

            tvSelectedDate.setText(
                    displayDateFormat.format(
                            selectedCalendar.getTime()
                    )
            );

        } catch (Exception e) {

            tvSelectedDate.setText(
                    dateKey
            );
        }

        /*
         * No attendance record.
         */
        if (attendance == null) {

            tvSelectedStatus.setText(
                    "Status: Not marked"
            );

            tvSelectedCheckIn.setText(
                    "Check-in: --"
            );

            tvSelectedCheckOut.setText(
                    "Check-out: --"
            );

            return;
        }

        String status =
                attendance.getStatus();

        if (status == null
                || status.trim().isEmpty()) {

            status = "PRESENT";
        }

        tvSelectedStatus.setText(
                "Status: " + status
        );

        String checkIn =
                attendance.getCheckInTime();

        String checkOut =
                attendance.getCheckOutTime();

        tvSelectedCheckIn.setText(
                "Check-in: " +
                        (
                                checkIn == null
                                        || checkIn.trim().isEmpty()
                                        ? "--"
                                        : checkIn
                        )
        );

        tvSelectedCheckOut.setText(
                "Check-out: " +
                        (
                                checkOut == null
                                        || checkOut.trim().isEmpty()
                                        ? "--"
                                        : checkOut
                        )
        );
    }

    /*
     * ==========================================
     * SESSION
     * ==========================================
     */

    private void handleSessionExpired() {

        sessionManager.logout();

        Toast.makeText(
                MainActivity.this,
                "Session expired. Please login again.",
                Toast.LENGTH_SHORT
        ).show();

        openLogin();
    }

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

    /*
     * ==========================================
     * DP -> PX
     * ==========================================
     */

    private int dpToPx(int dp) {

        return (int) (
                dp *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }
}

