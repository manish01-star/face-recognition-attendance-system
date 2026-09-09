package com.college.attendance.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.college.attendance.R;
import com.college.attendance.api.ApiClient;
import com.college.attendance.api.ApiService;
import com.college.attendance.dto.AttendancePolicyResponse;
import com.college.attendance.dto.AttendanceResponse;
import com.college.attendance.dto.HolidayResponse;
import com.college.attendance.dto.LeaveResponse;
import com.college.attendance.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CalendarActivity extends BaseActivity {

    // =========================================================
    // CURRENT TAB
    // =========================================================

    @Override
    protected Tab getCurrentTab() {
        return Tab.ATTENDANCE;
    }

    // =========================================================
    // VIEWS
    // =========================================================

    private ImageButton btnBack;
    private ImageButton btnPreviousMonth;
    private ImageButton btnNextMonth;

    private TextView tvMonthYear;

    private TextView tvPresentCount;
    private TextView tvAbsentCount;
    private TextView tvHalfDayCount;
    private TextView tvHolidayCount;

    private TextView tvAttendancePercentage;

    private TextView tvSelectedDate;
    private TextView tvSelectedStatus;
    private TextView tvSelectedCheckIn;
    private TextView tvSelectedCheckOut;
    private TextView tvSelectedWorkHours;

    private GridLayout calendarGrid;

    // =========================================================
    // API / SESSION
    // =========================================================

    private ApiService apiService;
    private SessionManager sessionManager;

    // =========================================================
    // CALENDAR
    // =========================================================

    private Calendar currentMonth;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
            );

    private final SimpleDateFormat monthFormat =
            new SimpleDateFormat(
                    "MMMM yyyy",
                    Locale.getDefault()
            );

    private final SimpleDateFormat displayDateFormat =
            new SimpleDateFormat(
                    "EEEE, dd MMMM yyyy",
                    Locale.getDefault()
            );

    // =========================================================
    // ATTENDANCE DATA
    // =========================================================

    private final Map<String, AttendanceResponse> attendanceMap =
            new HashMap<>();

    // =========================================================
    // HOLIDAY DATA
    // =========================================================

    private final Set<String> holidayDates =
            new HashSet<>();

    private final Map<String, String> holidayNames =
            new HashMap<>();

    // =========================================================
    // LEAVE DATA
    // =========================================================

    private final Set<String> approvedLeaveDates =
            new HashSet<>();

    private final Map<String, String> leaveTypes =
            new HashMap<>();

    // =========================================================
    // ATTENDANCE POLICY
    // =========================================================

    /*
     * Default policy:
     *
     * Saturday = Working Day
     * Sunday   = Off Day
     */
    private boolean saturdayOff = false;
    private boolean sundayOff = true;

    /*
     * Minimum working hours required for PRESENT.
     *
     * Default = 6 hours.
     */
    private double workingHoursThreshold = 6.0;

    private boolean policyLoaded = false;

    // =========================================================
    // SUMMARY
    // =========================================================

    private int presentCount = 0;
    private int absentCount = 0;
    private int halfDayCount = 0;
    private int holidayCount = 0;

    // =========================================================
    // STATE
    // =========================================================

    private boolean initialDataLoaded = false;

    private int holidayRequestYear = -1;

    private boolean sessionExpiredHandled = false;

    // =========================================================
    // LIFECYCLE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_calendar);

        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            goToLogin();
            return;
        }

        apiService = ApiClient.getApiService(this);

        initializeViews();

        currentMonth = Calendar.getInstance();

        currentMonth.set(
                Calendar.DAY_OF_MONTH,
                1
        );

        setupListeners();

        updateMonthTitle();

        renderCalendar();

        loadAllData();

        initialDataLoaded = true;
    }

    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initializeViews() {

        btnBack = findViewById(R.id.btnBack);

        btnPreviousMonth =
                findViewById(R.id.btnPreviousMonth);

        btnNextMonth =
                findViewById(R.id.btnNextMonth);

        tvMonthYear =
                findViewById(R.id.tvMonthYear);

        tvPresentCount =
                findViewById(R.id.tvPresentCount);

        tvAbsentCount =
                findViewById(R.id.tvAbsentCount);

        tvHalfDayCount =
                findViewById(R.id.tvHalfDayCount);

        tvHolidayCount =
                findViewById(R.id.tvHolidayCount);

        tvAttendancePercentage =
                findViewById(R.id.tvAttendancePercentage);

        tvSelectedDate =
                findViewById(R.id.tvSelectedDate);

        tvSelectedStatus =
                findViewById(R.id.tvSelectedStatus);

        tvSelectedCheckIn =
                findViewById(R.id.tvSelectedCheckIn);

        tvSelectedCheckOut =
                findViewById(R.id.tvSelectedCheckOut);

        tvSelectedWorkHours =
                findViewById(R.id.tvSelectedWorkHours);

        calendarGrid =
                findViewById(R.id.calendarGrid);
    }

    // =========================================================
    // LISTENERS
    // =========================================================

    private void setupListeners() {

        // =====================================================
        // BACK BUTTON
        // =====================================================

        btnBack.setOnClickListener(v -> finish());

        // =====================================================
        // PREVIOUS MONTH
        // =====================================================

        btnPreviousMonth.setOnClickListener(v -> {

            currentMonth.add(
                    Calendar.MONTH,
                    -1
            );

            currentMonth.set(
                    Calendar.DAY_OF_MONTH,
                    1
            );

            updateMonthTitle();

            renderCalendar();

            loadMonthlyData();
        });

        // =====================================================
        // NEXT MONTH
        // =====================================================

        btnNextMonth.setOnClickListener(v -> {

            currentMonth.add(
                    Calendar.MONTH,
                    1
            );

            currentMonth.set(
                    Calendar.DAY_OF_MONTH,
                    1
            );

            updateMonthTitle();

            renderCalendar();

            loadMonthlyData();
        });

        /*
         * Bottom navigation intentionally removed.
         *
         * BaseActivity handles:
         *
         * navHome
         * navAttendance
         * navLeave
         * navProfile
         */
    }

    // =========================================================
    // LOAD ALL DATA
    // =========================================================

    private void loadAllData() {

        loadAttendance();

        loadMonthlyData();

        loadLeaves();

        loadAttendancePolicy();
    }

    // =========================================================
    // ATTENDANCE API
    // =========================================================

    private void loadAttendance() {

        apiService.getMyAttendance()
                .enqueue(
                        new Callback<List<AttendanceResponse>>() {

                            @Override
                            public void onResponse(
                                    @NonNull Call<List<AttendanceResponse>> call,
                                    @NonNull Response<List<AttendanceResponse>> response) {

                                if (response.code() == 401) {
                                    handleSessionExpired();
                                    return;
                                }

                                if (response.code() == 403) {

                                    Toast.makeText(
                                            CalendarActivity.this,
                                            getString(
                                                    R.string.attendance_not_allowed
                                            ),
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    return;
                                }

                                if (!response.isSuccessful()
                                        || response.body() == null) {

                                    Toast.makeText(
                                            CalendarActivity.this,
                                            getString(
                                                    R.string.server_connection_failed
                                            ),
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    return;
                                }

                                attendanceMap.clear();

                                for (AttendanceResponse attendance
                                        : response.body()) {

                                    if (attendance == null) {
                                        continue;
                                    }

                                    String date =
                                            getAttendanceDate(
                                                    attendance
                                            );

                                    if (date != null
                                            && !date.isEmpty()) {

                                        attendanceMap.put(
                                                date,
                                                attendance
                                        );
                                    }
                                }

                                renderCalendar();

                                updateSummary();
                            }

                            @Override
                            public void onFailure(
                                    @NonNull Call<List<AttendanceResponse>> call,
                                    @NonNull Throwable t) {

                                Toast.makeText(
                                        CalendarActivity.this,
                                        getString(
                                                R.string.server_connection_failed
                                        ),
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                );
    }

    // =========================================================
    // HOLIDAY API
    // =========================================================

    private void loadMonthlyData() {

        if (currentMonth == null) {
            return;
        }

        final int requestedYear =
                currentMonth.get(Calendar.YEAR);

        holidayRequestYear =
                requestedYear;

        apiService.getHolidaysByYear(requestedYear)
                .enqueue(
                        new Callback<List<HolidayResponse>>() {

                            @Override
                            public void onResponse(
                                    @NonNull Call<List<HolidayResponse>> call,
                                    @NonNull Response<List<HolidayResponse>> response) {

                                if (response.code() == 401) {
                                    handleSessionExpired();
                                    return;
                                }

                                if (currentMonth == null
                                        || currentMonth.get(Calendar.YEAR)
                                        != requestedYear
                                        || holidayRequestYear
                                        != requestedYear) {

                                    return;
                                }

                                holidayDates.clear();

                                holidayNames.clear();

                                if (!response.isSuccessful()
                                        || response.body() == null) {

                                    renderCalendar();

                                    updateSummary();

                                    return;
                                }

                                for (HolidayResponse holiday
                                        : response.body()) {

                                    if (holiday == null) {
                                        continue;
                                    }

                                    String holidayDate =
                                            getHolidayDate(
                                                    holiday
                                            );

                                    if (holidayDate == null
                                            || holidayDate.isEmpty()) {

                                        continue;
                                    }

                                    holidayDates.add(
                                            holidayDate
                                    );

                                    String holidayName =
                                            getHolidayName(
                                                    holiday
                                            );

                                    if (holidayName != null
                                            && !holidayName.isEmpty()) {

                                        holidayNames.put(
                                                holidayDate,
                                                holidayName
                                        );
                                    }
                                }

                                renderCalendar();

                                updateSummary();
                            }

                            @Override
                            public void onFailure(
                                    @NonNull Call<List<HolidayResponse>> call,
                                    @NonNull Throwable t) {

                                if (currentMonth == null
                                        || currentMonth.get(Calendar.YEAR)
                                        != requestedYear
                                        || holidayRequestYear
                                        != requestedYear) {

                                    return;
                                }

                                holidayDates.clear();

                                holidayNames.clear();

                                renderCalendar();

                                updateSummary();
                            }
                        }
                );
    }

    // =========================================================
    // LEAVE API
    // =========================================================

    private void loadLeaves() {

        apiService.getMyLeaves()
                .enqueue(
                        new Callback<List<LeaveResponse>>() {

                            @Override
                            public void onResponse(
                                    @NonNull Call<List<LeaveResponse>> call,
                                    @NonNull Response<List<LeaveResponse>> response) {

                                if (response.code() == 401) {
                                    handleSessionExpired();
                                    return;
                                }

                                approvedLeaveDates.clear();

                                leaveTypes.clear();

                                if (!response.isSuccessful()
                                        || response.body() == null) {

                                    renderCalendar();

                                    updateSummary();

                                    return;
                                }

                                for (LeaveResponse leave
                                        : response.body()) {

                                    if (leave == null) {
                                        continue;
                                    }

                                    String status =
                                            getLeaveStatus(
                                                    leave
                                            );

                                    /*
                                     * ONLY APPROVED leave
                                     * calendar mein final Leave.
                                     */
                                    if (!"APPROVED".equalsIgnoreCase(
                                            status
                                    )) {

                                        continue;
                                    }

                                    String fromDate =
                                            getLeaveFromDate(
                                                    leave
                                            );

                                    String toDate =
                                            getLeaveToDate(
                                                    leave
                                            );

                                    if (fromDate == null
                                            || toDate == null) {

                                        continue;
                                    }

                                    addLeaveDates(
                                            fromDate,
                                            toDate,
                                            getLeaveType(leave)
                                    );
                                }

                                renderCalendar();

                                updateSummary();
                            }

                            @Override
                            public void onFailure(
                                    @NonNull Call<List<LeaveResponse>> call,
                                    @NonNull Throwable t) {

                                approvedLeaveDates.clear();

                                leaveTypes.clear();

                                renderCalendar();

                                updateSummary();
                            }
                        }
                );
    }

    // =========================================================
    // ATTENDANCE POLICY API
    // =========================================================

    private void loadAttendancePolicy() {

        apiService.getCurrentPolicy()
                .enqueue(
                        new Callback<AttendancePolicyResponse>() {

                            @Override
                            public void onResponse(
                                    @NonNull Call<AttendancePolicyResponse> call,
                                    @NonNull Response<AttendancePolicyResponse> response) {

                                if (response.code() == 401) {
                                    handleSessionExpired();
                                    return;
                                }

                                if (!response.isSuccessful()
                                        || response.body() == null) {

                                    saturdayOff = false;

                                    sundayOff = true;

                                    workingHoursThreshold = 6.0;

                                    policyLoaded = false;

                                    renderCalendar();

                                    updateSummary();

                                    return;
                                }

                                AttendancePolicyResponse policy =
                                        response.body();

                                saturdayOff =
                                        Boolean.TRUE.equals(
                                                policy.getSaturdayOff()
                                        );

                                sundayOff =
                                        Boolean.TRUE.equals(
                                                policy.getSundayOff()
                                        );

                                if (policy.getWorkingHours() != null) {

                                    try {

                                        workingHoursThreshold =
                                                policy.getWorkingHours()
                                                        .doubleValue();

                                        if (workingHoursThreshold <= 0) {

                                            workingHoursThreshold = 6.0;
                                        }

                                    } catch (Exception e) {

                                        workingHoursThreshold = 6.0;
                                    }

                                } else {

                                    workingHoursThreshold = 6.0;
                                }

                                policyLoaded = true;

                                renderCalendar();

                                updateSummary();
                            }

                            @Override
                            public void onFailure(
                                    @NonNull Call<AttendancePolicyResponse> call,
                                    @NonNull Throwable t) {

                                saturdayOff = false;

                                sundayOff = true;

                                workingHoursThreshold = 6.0;

                                policyLoaded = false;

                                renderCalendar();

                                updateSummary();
                            }
                        }
                );
    }

    // =========================================================
    // RENDER CALENDAR
    // =========================================================

    private void renderCalendar() {

        if (calendarGrid == null
                || currentMonth == null) {

            return;
        }

        calendarGrid.removeAllViews();

        Calendar firstDay =
                (Calendar) currentMonth.clone();

        firstDay.set(
                Calendar.DAY_OF_MONTH,
                1
        );

        int firstDayOfWeek =
                firstDay.get(
                        Calendar.DAY_OF_WEEK
                );

        int daysInMonth =
                currentMonth.getActualMaximum(
                        Calendar.DAY_OF_MONTH
                );

        int emptyBefore =
                firstDayOfWeek - 1;

        for (int i = 0;
             i < emptyBefore;
             i++) {

            addEmptyCalendarCell();
        }

        for (int day = 1;
             day <= daysInMonth;
             day++) {

            addDayCell(day);
        }

        int totalCells =
                emptyBefore + daysInMonth;

        int remaining =
                42 - totalCells;

        for (int i = 0;
             i < remaining;
             i++) {

            addEmptyCalendarCell();
        }
    }

    // =========================================================
    // ADD DAY CELL
    // =========================================================

    private void addDayCell(int day) {

        Calendar date =
                (Calendar) currentMonth.clone();

        date.set(
                Calendar.DAY_OF_MONTH,
                day
        );

        String dateKey =
                dateFormat.format(
                        date.getTime()
                );

        TextView dayView =
                new TextView(this);

        dayView.setText(
                String.valueOf(day)
        );

        dayView.setTextSize(13);

        dayView.setGravity(
                Gravity.CENTER
        );

        dayView.setIncludeFontPadding(false);

        GridLayout.LayoutParams params =
                new GridLayout.LayoutParams();

        params.width = 0;

        params.height =
                dpToPx(38);

        params.columnSpec =
                GridLayout.spec(
                        GridLayout.UNDEFINED,
                        1f
                );

        params.rowSpec =
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

        String status =
                getDayStatus(
                        date,
                        dateKey
                );

        applyDayBackground(
                dayView,
                status
        );

        dayView.setOnClickListener(
                v -> showDateDetails(
                        date,
                        dateKey,
                        status
                )
        );

        calendarGrid.addView(
                dayView
        );
    }

    // =========================================================
    // EMPTY CELL
    // =========================================================

    private void addEmptyCalendarCell() {

        TextView emptyView =
                new TextView(this);

        GridLayout.LayoutParams params =
                new GridLayout.LayoutParams();

        params.width = 0;

        params.height =
                dpToPx(38);

        params.columnSpec =
                GridLayout.spec(
                        GridLayout.UNDEFINED,
                        1f
                );

        params.rowSpec =
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

        emptyView.setLayoutParams(
                params
        );

        calendarGrid.addView(
                emptyView
        );
    }

    // =========================================================
    // GET DAY STATUS
    // =========================================================

    private String getDayStatus(
            Calendar date,
            String dateKey) {

        // =====================================================
        // PRIORITY 1 - APPROVED LEAVE
        // =====================================================

        if (approvedLeaveDates.contains(
                dateKey
        )) {

            return "LEAVE";
        }

        // =====================================================
        // PRIORITY 2 - ADMIN HOLIDAY
        // =====================================================

        if (holidayDates.contains(
                dateKey
        )) {

            return "HOLIDAY";
        }

        // =====================================================
        // PRIORITY 3 - POLICY OFF DAY
        // =====================================================

        int dayOfWeek =
                date.get(
                        Calendar.DAY_OF_WEEK
                );

        if (dayOfWeek == Calendar.SATURDAY
                && saturdayOff) {

            return "WEEKEND";
        }

        if (dayOfWeek == Calendar.SUNDAY
                && sundayOff) {

            return "WEEKEND";
        }

        // =====================================================
        // PRIORITY 4 - ATTENDANCE
        // =====================================================

        AttendanceResponse attendance =
                attendanceMap.get(
                        dateKey
                );

        if (attendance == null) {

            return "NONE";
        }

        String checkIn =
                attendance.getCheckInTime();

        String checkOut =
                attendance.getCheckOutTime();

        /*
         * Check-in without checkout = ABSENT
         */
        if (hasTimeValue(checkIn)
                && !hasTimeValue(checkOut)) {

            return "ABSENT";
        }

        /*
         * Missing check-in = ABSENT
         */
        if (!hasTimeValue(checkIn)) {

            return "ABSENT";
        }

        /*
         * Check-in + checkout
         */
        long workedSeconds =
                calculateWorkSeconds(
                        checkIn,
                        checkOut
                );

        if (workedSeconds < 0) {

            return "ABSENT";
        }

        long requiredSeconds =
                Math.round(
                        workingHoursThreshold
                                * 60.0
                                * 60.0
                );

        /*
         * Full working hours = PRESENT
         */
        if (workedSeconds >= requiredSeconds) {

            return "PRESENT";
        }

        /*
         * Less than required hours = HALF DAY
         */
        return "HALF_DAY";
    }

    // =========================================================
    // CHECK TIME VALUE
    // =========================================================

    private boolean hasTimeValue(
            String value) {

        return value != null
                && !value.trim().isEmpty()
                && !"-".equals(value.trim());
    }

    // =========================================================
    // CALCULATE WORK SECONDS
    // =========================================================

    private long calculateWorkSeconds(
            String checkIn,
            String checkOut) {

        if (!hasTimeValue(checkIn)
                || !hasTimeValue(checkOut)) {

            return -1;
        }

        try {

            String in =
                    extractTime(checkIn);

            String out =
                    extractTime(checkOut);

            SimpleDateFormat format =
                    new SimpleDateFormat(
                            "HH:mm:ss",
                            Locale.getDefault()
                    );

            format.setLenient(false);

            java.util.Date inDate =
                    format.parse(in);

            java.util.Date outDate =
                    format.parse(out);

            if (inDate == null
                    || outDate == null) {

                return -1;
            }

            long difference =
                    outDate.getTime()
                            - inDate.getTime();

            /*
             * Overnight attendance.
             */
            if (difference < 0) {

                difference +=
                        24L
                                * 60L
                                * 60L
                                * 1000L;
            }

            return difference / 1000L;

        } catch (Exception e) {

            return -1;
        }
    }

    // =========================================================
    // APPLY DAY BACKGROUND
    // =========================================================

    private void applyDayBackground(
            TextView dayView,
            String status) {

        GradientDrawable background =
                new GradientDrawable();

        background.setCornerRadius(
                dpToPx(9)
        );

        switch (status) {

            case "PRESENT":

                background.setColor(
                        Color.rgb(
                                220,
                                252,
                                231
                        )
                );

                dayView.setTextColor(
                        Color.rgb(
                                22,
                                101,
                                52
                        )
                );

                break;

            case "ABSENT":

                background.setColor(
                        Color.rgb(
                                254,
                                226,
                                226
                        )
                );

                dayView.setTextColor(
                        Color.rgb(
                                185,
                                28,
                                28
                        )
                );

                break;

            case "HALF_DAY":

                background.setColor(
                        Color.rgb(
                                255,
                                237,
                                213
                        )
                );

                dayView.setTextColor(
                        Color.rgb(
                                194,
                                65,
                                12
                        )
                );

                break;

            case "HOLIDAY":

                background.setColor(
                        Color.rgb(
                                243,
                                232,
                                255
                        )
                );

                dayView.setTextColor(
                        Color.rgb(
                                126,
                                34,
                                206
                        )
                );

                break;

            case "LEAVE":

                background.setColor(
                        Color.rgb(
                                219,
                                234,
                                254
                        )
                );

                dayView.setTextColor(
                        Color.rgb(
                                29,
                                78,
                                216
                        )
                );

                break;

            case "WEEKEND":

                background.setColor(
                        Color.rgb(
                                245,
                                243,
                                255
                        )
                );

                dayView.setTextColor(
                        Color.rgb(
                                109,
                                93,
                                163
                        )
                );

                break;

            default:

                background.setColor(
                        Color.WHITE
                );

                dayView.setTextColor(
                        Color.rgb(
                                40,
                                40,
                                40
                        )
                );

                break;
        }

        dayView.setBackground(
                background
        );
    }

    // =========================================================
    // SUMMARY
    // =========================================================

    private void updateSummary() {

        presentCount = 0;
        absentCount = 0;
        halfDayCount = 0;
        holidayCount = 0;

        if (currentMonth == null) {
            return;
        }

        int year =
                currentMonth.get(
                        Calendar.YEAR
                );

        int month =
                currentMonth.get(
                        Calendar.MONTH
                );

        int daysInMonth =
                currentMonth.getActualMaximum(
                        Calendar.DAY_OF_MONTH
                );

        for (int day = 1;
             day <= daysInMonth;
             day++) {

            Calendar date =
                    Calendar.getInstance();

            date.set(
                    year,
                    month,
                    day
            );

            String dateKey =
                    dateFormat.format(
                            date.getTime()
                    );

            String status =
                    getDayStatus(
                            date,
                            dateKey
                    );

            switch (status) {

                case "PRESENT":

                    presentCount++;

                    break;

                case "ABSENT":

                    absentCount++;

                    break;

                case "HALF_DAY":

                    halfDayCount++;

                    break;

                case "HOLIDAY":
                case "WEEKEND":

                    holidayCount++;

                    break;

                default:

                    break;
            }
        }

        tvPresentCount.setText(
                String.valueOf(
                        presentCount
                )
        );

        tvAbsentCount.setText(
                String.valueOf(
                        absentCount
                )
        );

        tvHalfDayCount.setText(
                String.valueOf(
                        halfDayCount
                )
        );

        tvHolidayCount.setText(
                String.valueOf(
                        holidayCount
                )
        );

        int workingDays =
                presentCount
                        + absentCount
                        + halfDayCount;

        double percentage = 0.0;

        if (workingDays > 0) {

            double effectivePresent =
                    presentCount
                            + (
                            halfDayCount * 0.5
                    );

            percentage =
                    (
                            effectivePresent
                                    / workingDays
                    ) * 100.0;
        }

        tvAttendancePercentage.setText(
                String.format(
                        Locale.getDefault(),
                        "%.1f%%",
                        percentage
                )
        );
    }

    // =========================================================
    // SHOW DATE DETAILS
    // =========================================================

    private void showDateDetails(
            Calendar date,
            String dateKey,
            String status) {

        String displayDate =
                displayDateFormat.format(
                        date.getTime()
                );

        tvSelectedDate.setText(
                displayDate
        );

        String statusText;

        switch (status) {

            case "PRESENT":

                statusText =
                        getString(
                                R.string.present
                        );

                break;

            case "ABSENT":

                statusText =
                        getString(
                                R.string.absent
                        );

                break;

            case "HALF_DAY":

                statusText =
                        getString(
                                R.string.half_day
                        );

                break;

            case "HOLIDAY":

                String holidayName =
                        holidayNames.get(
                                dateKey
                        );

                if (holidayName != null
                        && !holidayName.trim().isEmpty()) {

                    statusText =
                            getString(
                                    R.string.holiday
                            )
                                    + " - "
                                    + holidayName;

                } else {

                    statusText =
                            getString(
                                    R.string.holiday
                            );
                }

                break;

            case "LEAVE":

                String leaveType =
                        leaveTypes.get(
                                dateKey
                        );

                if (leaveType != null
                        && !leaveType.trim().isEmpty()) {

                    statusText =
                            getString(
                                    R.string.approved_leave
                            )
                                    + " - "
                                    + leaveType;

                } else {

                    statusText =
                            getString(
                                    R.string.approved_leave
                            );
                }

                break;

            case "WEEKEND":

                statusText =
                        getString(
                                R.string.holiday
                        );

                break;

            default:

                statusText =
                        getString(
                                R.string.no_attendance
                        );

                break;
        }

        tvSelectedStatus.setText(
                statusText
        );

        AttendanceResponse attendance =
                attendanceMap.get(
                        dateKey
                );

        if (attendance == null) {

            tvSelectedCheckIn.setText("-");

            tvSelectedCheckOut.setText("-");

            tvSelectedWorkHours.setText("-");

            return;
        }

        String checkIn =
                attendance.getCheckInTime();

        String checkOut =
                attendance.getCheckOutTime();

        tvSelectedCheckIn.setText(
                formatTime(checkIn)
        );

        tvSelectedCheckOut.setText(
                formatTime(checkOut)
        );

        tvSelectedWorkHours.setText(
                calculateWorkHours(
                        checkIn,
                        checkOut
                )
        );
    }

    // =========================================================
    // FORMAT TIME
    // =========================================================

    private String formatTime(
            String value) {

        if (value == null
                || value.trim().isEmpty()) {

            return "-";
        }

        String time =
                value.trim();

        if (time.length() >= 8
                && time.charAt(2) == ':'
                && time.charAt(5) == ':') {

            return time.substring(
                    0,
                    8
            );
        }

        if (time.contains("T")) {

            int index =
                    time.indexOf("T");

            if (index >= 0
                    && time.length() > index + 1) {

                String result =
                        time.substring(
                                index + 1
                        );

                if (result.length() >= 8) {

                    return result.substring(
                            0,
                            8
                    );
                }

                return result;
            }
        }

        return time;
    }

    // =========================================================
    // CALCULATE WORK HOURS - DISPLAY
    // =========================================================

    private String calculateWorkHours(
            String checkIn,
            String checkOut) {

        long totalSeconds =
                calculateWorkSeconds(
                        checkIn,
                        checkOut
                );

        if (totalSeconds < 0) {

            return "-";
        }

        long hours =
                totalSeconds / 3600;

        long minutes =
                (
                        totalSeconds % 3600
                ) / 60;

        long seconds =
                totalSeconds % 60;

        return String.format(
                Locale.getDefault(),
                "%02d:%02d:%02d",
                hours,
                minutes,
                seconds
        );
    }

    // =========================================================
    // EXTRACT TIME
    // =========================================================

    private String extractTime(
            String value) {

        if (value == null) {
            return "";
        }

        String time =
                value.trim();

        /*
         * ISO:
         * 2026-09-07T15:49:39
         */
        if (time.contains("T")) {

            time =
                    time.substring(
                            time.indexOf("T") + 1
                    );
        }

        /*
         * Remove timezone.
         *
         * Example:
         * 15:49:39+05:30
         */
        int plusIndex =
                time.indexOf("+");

        if (plusIndex > 0) {

            time =
                    time.substring(
                            0,
                            plusIndex
                    );
        }

        /*
         * Remove milliseconds.
         *
         * Example:
         * 15:49:39.123
         */
        int dotIndex =
                time.indexOf(".");

        if (dotIndex > 0) {

            time =
                    time.substring(
                            0,
                            dotIndex
                    );
        }

        if (time.length() >= 8) {

            return time.substring(
                    0,
                    8
            );
        }

        return time;
    }

    // =========================================================
    // ADD LEAVE DATES
    // =========================================================

    private void addLeaveDates(
            String fromDate,
            String toDate,
            String leaveType) {

        try {

            Calendar start =
                    parseDate(
                            fromDate
                    );

            Calendar end =
                    parseDate(
                            toDate
                    );

            if (start == null
                    || end == null) {

                return;
            }

            if (start.after(end)) {

                return;
            }

            while (!start.after(end)) {

                String date =
                        dateFormat.format(
                                start.getTime()
                        );

                approvedLeaveDates.add(
                        date
                );

                if (leaveType != null
                        && !leaveType.trim().isEmpty()) {

                    leaveTypes.put(
                            date,
                            leaveType
                    );
                }

                start.add(
                        Calendar.DAY_OF_MONTH,
                        1
                );
            }

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // PARSE DATE
    // =========================================================

    private Calendar parseDate(
            String value) {

        try {

            if (value == null
                    || value.trim().isEmpty()) {

                return null;
            }

            String date =
                    value.trim();

            if (date.length() >= 10) {

                date =
                        date.substring(
                                0,
                                10
                        );
            }

            java.util.Date parsedDate =
                    dateFormat.parse(
                            date
                    );

            if (parsedDate == null) {

                return null;
            }

            Calendar calendar =
                    Calendar.getInstance();

            calendar.setTime(
                    parsedDate
            );

            calendar.set(
                    Calendar.HOUR_OF_DAY,
                    0
            );

            calendar.set(
                    Calendar.MINUTE,
                    0
            );

            calendar.set(
                    Calendar.SECOND,
                    0
            );

            calendar.set(
                    Calendar.MILLISECOND,
                    0
            );

            return calendar;

        } catch (Exception e) {

            return null;
        }
    }

    // =========================================================
    // GET ATTENDANCE DATE
    // =========================================================

    private String getAttendanceDate(
            AttendanceResponse attendance) {

        String value =
                attendance.getAttendanceDate();

        if (value == null
                || value.trim().isEmpty()) {

            return null;
        }

        value =
                value.trim();

        if (value.length() >= 10) {

            return value.substring(
                    0,
                    10
            );
        }

        return value;
    }

    // =========================================================
    // HOLIDAY DATE
    // =========================================================

    private String getHolidayDate(
            HolidayResponse holiday) {

        if (holiday.getHolidayDate() == null) {

            return null;
        }

        String value =
                holiday.getHolidayDate()
                        .trim();

        if (value.length() >= 10) {

            return value.substring(
                    0,
                    10
            );
        }

        return value;
    }

    // =========================================================
    // HOLIDAY NAME
    // =========================================================

    private String getHolidayName(
            HolidayResponse holiday) {

        if (holiday.getHolidayName() == null) {

            return null;
        }

        return holiday.getHolidayName()
                .trim();
    }

    // =========================================================
    // LEAVE STATUS
    // =========================================================

    private String getLeaveStatus(
            LeaveResponse leave) {

        if (leave.getStatus() == null) {

            return null;
        }

        return String.valueOf(
                leave.getStatus()
        ).trim();
    }

    // =========================================================
    // LEAVE FROM DATE
    // =========================================================

    private String getLeaveFromDate(
            LeaveResponse leave) {

        if (leave.getFromDate() == null) {

            return null;
        }

        return String.valueOf(
                leave.getFromDate()
        ).trim();
    }

    // =========================================================
    // LEAVE TO DATE
    // =========================================================

    private String getLeaveToDate(
            LeaveResponse leave) {

        if (leave.getToDate() == null) {

            return null;
        }

        return String.valueOf(
                leave.getToDate()
        ).trim();
    }

    // =========================================================
    // LEAVE TYPE
    // =========================================================

    private String getLeaveType(
            LeaveResponse leave) {

        if (leave.getLeaveType() == null) {

            return null;
        }

        return String.valueOf(
                leave.getLeaveType()
        ).trim();
    }

    // =========================================================
    // MONTH TITLE
    // =========================================================

    private void updateMonthTitle() {

        if (tvMonthYear == null
                || currentMonth == null) {

            return;
        }

        tvMonthYear.setText(
                monthFormat.format(
                        currentMonth.getTime()
                )
        );
    }

    // =========================================================
    // SESSION EXPIRED
    // =========================================================

    private void handleSessionExpired() {

        if (sessionExpiredHandled) {
            return;
        }

        sessionExpiredHandled = true;

        if (sessionManager != null) {

            sessionManager.logout();
        }

        Toast.makeText(
                this,
                getString(
                        R.string.session_expired
                ),
                Toast.LENGTH_SHORT
        ).show();

        goToLogin();
    }

    // =========================================================
    // GO TO LOGIN
    // =========================================================

    private void goToLogin() {

        Intent intent =
                new Intent(
                        CalendarActivity.this,
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
    // DP TO PX
    // =========================================================

    private int dpToPx(int dp) {

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return Math.round(
                dp * density
        );
    }

    // =========================================================
    // ON RESUME
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();

        if (!initialDataLoaded) {
            return;
        }

        if (apiService != null
                && sessionManager != null
                && sessionManager.isLoggedIn()) {

            loadAttendance();

            loadMonthlyData();

            loadLeaves();

            loadAttendancePolicy();
        }
    }
}