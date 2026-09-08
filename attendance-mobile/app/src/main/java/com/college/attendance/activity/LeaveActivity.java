package com.college.attendance.activity;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.college.attendance.R;
import com.college.attendance.api.ApiClient;
import com.college.attendance.api.ApiService;
import com.college.attendance.dto.HolidayResponse;
import com.college.attendance.dto.LeaveApplyRequest;
import com.college.attendance.dto.LeaveResponse;
import com.college.attendance.dto.LeaveType;
import com.college.attendance.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LeaveActivity extends AppCompatActivity {

    // =========================================================
    // VIEWS
    // =========================================================

    private ImageButton btnBack;

    private LinearLayout holidayContainer;
    private LinearLayout leaveContainer;

    private TextView tvNoUpcomingHolidays;
    private TextView tvNoLeaves;

    private MaterialButton btnApplyLeave;

    // =========================================================
    // API / SESSION
    // =========================================================

    private ApiService apiService;
    private SessionManager sessionManager;

    // =========================================================
    // DATE
    // =========================================================

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private final SimpleDateFormat displayDateFormat =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    private String selectedFromDate;
    private String selectedToDate;

    // =========================================================
    // LIFECYCLE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_leave);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService(this);

        initializeViews();
        setupListeners();

        loadHolidays();
        loadMyLeaves();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (apiService != null) {
            loadHolidays();
            loadMyLeaves();
        }
    }

    // =========================================================
    // INITIALIZE
    // =========================================================

    private void initializeViews() {

        btnBack = findViewById(R.id.btnBack);

        holidayContainer =
                findViewById(R.id.holidayContainer);

        leaveContainer =
                findViewById(R.id.leaveContainer);

        tvNoUpcomingHolidays =
                findViewById(R.id.tvNoUpcomingHolidays);

        tvNoLeaves =
                findViewById(R.id.tvNoLeaves);

        btnApplyLeave =
                findViewById(R.id.btnApplyLeave);
    }

    // =========================================================
    // LISTENERS
    // =========================================================

    private void setupListeners() {

        btnBack.setOnClickListener(v -> finish());

        btnApplyLeave.setOnClickListener(
                v -> showApplyLeaveDialog()
        );

        /*
         * Bottom Navigation
         */

        View navHome = findViewById(R.id.navHome);
        View navCalendar = findViewById(R.id.navCalendar);
        View navLeave = findViewById(R.id.navLeave);

        navHome.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            LeaveActivity.this,
                            MainActivity.class
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
            );

            startActivity(intent);
        });

        navCalendar.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            LeaveActivity.this,
                            CalendarActivity.class
                    );

            startActivity(intent);

            finish();
        });

        /*
         * Already on Leave.
         */
        navLeave.setOnClickListener(v -> {
            // Nothing required.
        });
    }

    // =========================================================
    // LOAD HOLIDAYS
    // =========================================================

    private void loadHolidays() {

        apiService.getHolidays()
                .enqueue(new Callback<List<HolidayResponse>>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<List<HolidayResponse>> call,
                            @NonNull Response<List<HolidayResponse>> response) {

                        if (response.code() == 401) {
                            handleSessionExpired();
                            return;
                        }

                        if (!response.isSuccessful()
                                || response.body() == null) {

                            showNoHolidays();
                            return;
                        }

                        displayHolidays(response.body());
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<List<HolidayResponse>> call,
                            @NonNull Throwable t) {

                        showNoHolidays();
                    }
                });
    }

    // =========================================================
    // DISPLAY HOLIDAYS
    // =========================================================

    private void displayHolidays(
            List<HolidayResponse> holidays) {

        holidayContainer.removeAllViews();

        if (holidays == null
                || holidays.isEmpty()) {

            showNoHolidays();
            return;
        }

        /*
         * Sort by date.
         */
        Collections.sort(
                holidays,
                new Comparator<HolidayResponse>() {

                    @Override
                    public int compare(
                            HolidayResponse h1,
                            HolidayResponse h2) {

                        String d1 =
                                h1 != null
                                        ? h1.getHolidayDate()
                                        : "";

                        String d2 =
                                h2 != null
                                        ? h2.getHolidayDate()
                                        : "";

                        return d1.compareTo(d2);
                    }
                }
        );

        int displayedCount = 0;

        String today =
                dateFormat.format(
                        Calendar.getInstance().getTime()
                );

        for (HolidayResponse holiday : holidays) {

            if (holiday == null
                    || holiday.getHolidayDate() == null) {
                continue;
            }

            String holidayDate =
                    holiday.getHolidayDate().trim();

            if (holidayDate.length() >= 10) {
                holidayDate =
                        holidayDate.substring(0, 10);
            }

            /*
             * Only upcoming/current holidays.
             */
            if (holidayDate.compareTo(today) < 0) {
                continue;
            }

            addHolidayCard(holiday);

            displayedCount++;

            /*
             * Keep this section compact.
             */
            if (displayedCount >= 4) {
                break;
            }
        }

        if (displayedCount == 0) {
            showNoHolidays();
        } else {
            tvNoUpcomingHolidays.setVisibility(View.GONE);
        }
    }

    // =========================================================
    // HOLIDAY CARD
    // =========================================================

    private void addHolidayCard(
            HolidayResponse holiday) {

        MaterialCardView card =
                new MaterialCardView(this);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(58)
                );

        cardParams.setMargins(
                0,
                0,
                0,
                dpToPx(6)
        );

        card.setLayoutParams(cardParams);

        card.setCardBackgroundColor(
                Color.WHITE
        );

        card.setRadius(
                dpToPx(12)
        );

        card.setCardElevation(0);

        card.setStrokeWidth(1);

        card.setStrokeColor(
                Color.rgb(238, 238, 243)
        );

        LinearLayout content =
                new LinearLayout(this);

        content.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                )
        );

        content.setGravity(
                Gravity.CENTER_VERTICAL
        );

        content.setOrientation(
                LinearLayout.HORIZONTAL
        );

        content.setPadding(
                dpToPx(12),
                0,
                dpToPx(12),
                0
        );

        /*
         * Holiday icon.
         */
        TextView icon =
                new TextView(this);

        icon.setText("H");
        icon.setTextSize(12);
        icon.setGravity(Gravity.CENTER);
        icon.setTextColor(
                Color.rgb(126, 34, 206)
        );

        GradientDrawable iconBackground =
                new GradientDrawable();

        iconBackground.setShape(
                GradientDrawable.OVAL
        );

        iconBackground.setColor(
                Color.rgb(243, 232, 255)
        );

        icon.setBackground(iconBackground);

        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(
                        dpToPx(34),
                        dpToPx(34)
                );

        icon.setLayoutParams(iconParams);

        content.addView(icon);

        /*
         * Holiday name/date.
         */
        LinearLayout textContainer =
                new LinearLayout(this);

        textContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        textContainer.setGravity(
                Gravity.CENTER_VERTICAL
        );

        LinearLayout.LayoutParams textParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        textParams.setMargins(
                dpToPx(10),
                0,
                0,
                0
        );

        textContainer.setLayoutParams(textParams);

        TextView name =
                new TextView(this);

        String holidayName =
                holiday.getHolidayName();

        if (holidayName == null
                || holidayName.trim().isEmpty()) {

            holidayName =
                    getString(R.string.holiday);
        }

        name.setText(
                holidayName.trim()
        );

        name.setTextColor(
                Color.rgb(40, 40, 40)
        );

        name.setTextSize(13);
        name.setMaxLines(1);

        textContainer.addView(name);

        TextView date =
                new TextView(this);

        String holidayDate =
                holiday.getHolidayDate();

        date.setText(
                formatDisplayDate(holidayDate)
        );

        date.setTextColor(
                Color.rgb(125, 125, 125)
        );

        date.setTextSize(11);

        LinearLayout.LayoutParams dateParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        dateParams.setMargins(
                0,
                dpToPx(2),
                0,
                0
        );

        date.setLayoutParams(dateParams);

        textContainer.addView(date);

        content.addView(textContainer);

        card.addView(content);

        holidayContainer.addView(card);
    }

    // =========================================================
    // NO HOLIDAYS
    // =========================================================

    private void showNoHolidays() {

        holidayContainer.removeAllViews();

        tvNoUpcomingHolidays.setVisibility(
                View.VISIBLE
        );

        holidayContainer.addView(
                tvNoUpcomingHolidays
        );
    }

    // =========================================================
    // LOAD MY LEAVES
    // =========================================================

    private void loadMyLeaves() {

        apiService.getMyLeaves()
                .enqueue(new Callback<List<LeaveResponse>>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<List<LeaveResponse>> call,
                            @NonNull Response<List<LeaveResponse>> response) {

                        if (response.code() == 401) {
                            handleSessionExpired();
                            return;
                        }

                        if (!response.isSuccessful()
                                || response.body() == null) {

                            showNoLeaves();
                            return;
                        }

                        displayMyLeaves(
                                response.body()
                        );
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<List<LeaveResponse>> call,
                            @NonNull Throwable t) {

                        showNoLeaves();
                    }
                });
    }

    // =========================================================
    // DISPLAY MY LEAVES
    // =========================================================

    private void displayMyLeaves(
            List<LeaveResponse> leaves) {

        leaveContainer.removeAllViews();

        if (leaves == null
                || leaves.isEmpty()) {

            showNoLeaves();
            return;
        }

        tvNoLeaves.setVisibility(View.GONE);

        /*
         * Latest leave first.
         */
        Collections.sort(
                leaves,
                new Comparator<LeaveResponse>() {

                    @Override
                    public int compare(
                            LeaveResponse l1,
                            LeaveResponse l2) {

                        String d1 =
                                getDateString(
                                        l1.getFromDate()
                                );

                        String d2 =
                                getDateString(
                                        l2.getFromDate()
                                );

                        return d2.compareTo(d1);
                    }
                }
        );

        int count = 0;

        for (LeaveResponse leave : leaves) {

            if (leave == null) {
                continue;
            }

            addLeaveCard(leave);

            count++;

            /*
             * Keep latest 5 applications visible.
             */
            if (count >= 5) {
                break;
            }
        }

        if (count == 0) {
            showNoLeaves();
        }
    }

    // =========================================================
    // LEAVE CARD
    // =========================================================

    private void addLeaveCard(
            LeaveResponse leave) {

        MaterialCardView card =
                new MaterialCardView(this);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(
                0,
                0,
                0,
                dpToPx(8)
        );

        card.setLayoutParams(cardParams);

        card.setCardBackgroundColor(
                Color.WHITE
        );

        card.setRadius(
                dpToPx(14)
        );

        card.setCardElevation(1);

        card.setStrokeWidth(0);

        LinearLayout content =
                new LinearLayout(this);

        content.setOrientation(
                LinearLayout.VERTICAL
        );

        content.setPadding(
                dpToPx(13),
                dpToPx(11),
                dpToPx(13),
                dpToPx(11)
        );

        /*
         * Top row.
         */
        LinearLayout topRow =
                new LinearLayout(this);

        topRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        topRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        /*
         * Leave type.
         */
        TextView type =
                new TextView(this);

        String leaveType =
                getLeaveTypeText(leave);

        type.setText(leaveType);

        type.setTextColor(
                Color.rgb(45, 45, 45)
        );

        type.setTextSize(14);
        type.setTypeface(null, android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams typeParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        type.setLayoutParams(typeParams);

        topRow.addView(type);

        /*
         * Status badge.
         */
        TextView status =
                createStatusBadge(
                        getLeaveStatusText(leave)
                );

        topRow.addView(status);

        content.addView(topRow);

        /*
         * Date.
         */
        TextView dates =
                new TextView(this);

        String from =
                getDateString(
                        leave.getFromDate()
                );

        String to =
                getDateString(
                        leave.getToDate()
                );

        if (from.equals(to)) {

            dates.setText(
                    formatDisplayDate(from)
            );

        } else {

            dates.setText(
                    formatDisplayDate(from)
                            + " - "
                            + formatDisplayDate(to)
            );
        }

        dates.setTextColor(
                Color.rgb(95, 95, 95)
        );

        dates.setTextSize(12);

        LinearLayout.LayoutParams dateParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        dateParams.setMargins(
                0,
                dpToPx(5),
                0,
                0
        );

        dates.setLayoutParams(dateParams);

        content.addView(dates);

        /*
         * Description.
         */
        String description =
                leave.getDescription();

        if (description != null
                && !description.trim().isEmpty()) {

            TextView descriptionView =
                    new TextView(this);

            descriptionView.setText(
                    description.trim()
            );

            descriptionView.setTextColor(
                    Color.rgb(125, 125, 125)
            );

            descriptionView.setTextSize(11);

            descriptionView.setMaxLines(2);

            LinearLayout.LayoutParams descParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            descParams.setMargins(
                    0,
                    dpToPx(4),
                    0,
                    0
            );

            descriptionView.setLayoutParams(
                    descParams
            );

            content.addView(
                    descriptionView
            );
        }

        /*
         * Admin remark.
         */
        String adminRemark =
                leave.getAdminRemark();

        if (adminRemark != null
                && !adminRemark.trim().isEmpty()) {

            TextView remark =
                    new TextView(this);

            remark.setText(
                    "Admin: "
                            + adminRemark.trim()
            );

            remark.setTextColor(
                    Color.rgb(100, 100, 100)
            );

            remark.setTextSize(11);

            remark.setMaxLines(2);

            LinearLayout.LayoutParams remarkParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            remarkParams.setMargins(
                    0,
                    dpToPx(4),
                    0,
                    0
            );

            remark.setLayoutParams(
                    remarkParams
            );

            content.addView(remark);
        }

        card.addView(content);

        leaveContainer.addView(card);
    }

    // =========================================================
    // STATUS BADGE
    // =========================================================

    private TextView createStatusBadge(
            String status) {

        TextView badge =
                new TextView(this);

        badge.setText(
                status
        );

        badge.setTextSize(10);
        badge.setGravity(Gravity.CENTER);
        badge.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        badge.setPadding(
                dpToPx(9),
                dpToPx(5),
                dpToPx(9),
                dpToPx(5)
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setCornerRadius(
                dpToPx(20)
        );

        if ("APPROVED".equalsIgnoreCase(status)) {

            background.setColor(
                    Color.rgb(220, 252, 231)
            );

            badge.setTextColor(
                    Color.rgb(22, 101, 52)
            );

        } else if ("REJECTED".equalsIgnoreCase(status)) {

            background.setColor(
                    Color.rgb(254, 226, 226)
            );

            badge.setTextColor(
                    Color.rgb(185, 28, 28)
            );

        } else {

            background.setColor(
                    Color.rgb(255, 237, 213)
            );

            badge.setTextColor(
                    Color.rgb(194, 65, 12)
            );
        }

        badge.setBackground(background);

        return badge;
    }

    // =========================================================
    // NO LEAVES
    // =========================================================

    private void showNoLeaves() {

        leaveContainer.removeAllViews();

        tvNoLeaves.setVisibility(
                View.VISIBLE
        );

        leaveContainer.addView(
                tvNoLeaves
        );
    }

    // =========================================================
    // APPLY LEAVE DIALOG
    // =========================================================

    private void showApplyLeaveDialog() {

        selectedFromDate = null;
        selectedToDate = null;

        final Dialog dialog =
                new Dialog(this);

        dialog.requestWindowFeature(
                Window.FEATURE_NO_TITLE
        );

        dialog.setContentView(
                R.layout.dialog_apply_leave
        );

        Window window =
                dialog.getWindow();

        if (window != null) {

            window.setBackgroundDrawableResource(
                    android.R.color.transparent
            );

            WindowManager.LayoutParams params =
                    new WindowManager.LayoutParams();

            params.copyFrom(
                    window.getAttributes()
            );

            params.width =
                    WindowManager.LayoutParams.MATCH_PARENT;

            params.height =
                    WindowManager.LayoutParams.WRAP_CONTENT;

            params.dimAmount = 0.45f;

            window.setAttributes(params);
        }

        TextView tvFromDate =
                dialog.findViewById(
                        R.id.tvFromDate
                );

        TextView tvToDate =
                dialog.findViewById(
                        R.id.tvToDate
                );

        TextView tvLeaveType =
                dialog.findViewById(
                        R.id.tvLeaveType
                );

        EditText etDescription =
                dialog.findViewById(
                        R.id.etDescription
                );

        MaterialButton btnCancel =
                dialog.findViewById(
                        R.id.btnCancel
                );

        MaterialButton btnSubmit =
                dialog.findViewById(
                        R.id.btnSubmit
                );

        /*
         * From Date.
         */
        tvFromDate.setOnClickListener(
                v -> showDatePicker(
                        true,
                        tvFromDate
                )
        );

        /*
         * To Date.
         */
        tvToDate.setOnClickListener(
                v -> showDatePicker(
                        false,
                        tvToDate
                )
        );

        /*
         * Leave Type.
         */
        tvLeaveType.setOnClickListener(
                v -> showLeaveTypePicker(
                        tvLeaveType
                )
        );

        /*
         * Cancel.
         */
        btnCancel.setOnClickListener(
                v -> dialog.dismiss()
        );

        /*
         * Submit.
         */
        btnSubmit.setOnClickListener(v -> {

            String description =
                    etDescription.getText()
                            .toString()
                            .trim();

            String leaveType =
                    tvLeaveType.getTag() != null
                            ? tvLeaveType.getTag().toString()
                            : null;

            if (selectedFromDate == null) {

                Toast.makeText(
                        this,
                        getString(
                                R.string.select_from_date
                        ),
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (selectedToDate == null) {

                Toast.makeText(
                        this,
                        getString(
                                R.string.select_to_date
                        ),
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (leaveType == null
                    || leaveType.trim().isEmpty()) {

                Toast.makeText(
                        this,
                        getString(
                                R.string.select_leave_type
                        ),
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (description.isEmpty()) {

                Toast.makeText(
                        this,
                        getString(
                                R.string.description
                        ),
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (description.length() > 500) {

                Toast.makeText(
                        this,
                        "Description cannot exceed 500 characters",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (selectedFromDate.compareTo(
                    selectedToDate
            ) > 0) {

                Toast.makeText(
                        this,
                        "From date cannot be after to date",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            btnSubmit.setEnabled(false);

            applyLeave(
                    selectedFromDate,
                    selectedToDate,
                    leaveType,
                    description,
                    dialog,
                    btnSubmit
            );
        });

        dialog.show();
    }

    // =========================================================
    // DATE PICKER
    // =========================================================

    private void showDatePicker(
            boolean isFromDate,
            TextView targetView) {

        Calendar calendar =
                Calendar.getInstance();

        DatePickerDialog picker =
                new DatePickerDialog(
                        this,
                        (view, year, month, dayOfMonth) -> {

                            Calendar selected =
                                    Calendar.getInstance();

                            selected.set(
                                    year,
                                    month,
                                    dayOfMonth
                            );

                            String date =
                                    dateFormat.format(
                                            selected.getTime()
                                    );

                            /*
                             * Prevent past dates.
                             */
                            String today =
                                    dateFormat.format(
                                            Calendar.getInstance()
                                                    .getTime()
                                    );

                            if (date.compareTo(today) < 0) {

                                Toast.makeText(
                                        this,
                                        "Leave date cannot be in the past",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            if (!isFromDate
                                    && selectedFromDate != null
                                    && date.compareTo(
                                    selectedFromDate
                            ) < 0) {

                                Toast.makeText(
                                        this,
                                        "To date cannot be before from date",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            if (isFromDate) {

                                selectedFromDate =
                                        date;

                            } else {

                                selectedToDate =
                                        date;
                            }

                            targetView.setText(
                                    formatDisplayDate(date)
                            );

                        },
                        calendar.get(
                                Calendar.YEAR
                        ),
                        calendar.get(
                                Calendar.MONTH
                        ),
                        calendar.get(
                                Calendar.DAY_OF_MONTH
                        )
                );

        picker.getDatePicker()
                .setMinDate(
                        System.currentTimeMillis()
                );

        picker.show();
    }

    // =========================================================
    // LEAVE TYPE PICKER
    // =========================================================

    private void showLeaveTypePicker(
            TextView targetView) {

        final String[] labels = {
                getString(R.string.casual_leave),
                getString(R.string.sick_leave),
                getString(R.string.personal_leave),
                getString(R.string.other_leave)
        };

        final String[] values = {
                "CASUAL",
                "SICK",
                "PERSONAL",
                "OTHER"
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(
                        getString(
                                R.string.leave_type
                        )
                )
                .setItems(
                        labels,
                        (dialog, which) -> {

                            targetView.setText(
                                    labels[which]
                            );

                            targetView.setTag(
                                    values[which]
                            );
                        }
                )
                .show();
    }

    // =========================================================
    // APPLY LEAVE API
    // =========================================================

    private void applyLeave(
            String fromDate,
            String toDate,
            String leaveType,
            String description,
            Dialog dialog,
            MaterialButton btnSubmit) {

        LeaveApplyRequest request =
                new LeaveApplyRequest();

        request.setFromDate(fromDate);
        request.setToDate(toDate);

        /*
         * If LeaveApplyRequest expects enum.
         */
        try {

            request.setLeaveType(
                    LeaveType.valueOf(
                            leaveType
                    )
            );

        } catch (Exception e) {

            btnSubmit.setEnabled(true);

            Toast.makeText(
                    this,
                    "Invalid leave type",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        request.setDescription(
                description
        );

        apiService.applyLeave(request)
                .enqueue(
                        new Callback<LeaveResponse>() {

                            @Override
                            public void onResponse(
                                    @NonNull Call<LeaveResponse> call,
                                    @NonNull Response<LeaveResponse> response) {

                                btnSubmit.setEnabled(true);

                                if (response.code() == 401) {

                                    dialog.dismiss();

                                    handleSessionExpired();

                                    return;
                                }

                                if (!response.isSuccessful()) {

                                    String message =
                                            "Unable to apply leave";

                                    if (response.code() == 400) {

                                        message =
                                                "Invalid leave request";
                                    }

                                    Toast.makeText(
                                            LeaveActivity.this,
                                            message,
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    return;
                                }

                                dialog.dismiss();

                                Toast.makeText(
                                        LeaveActivity.this,
                                        getString(
                                                R.string.leave_applied_successfully
                                        ),
                                        Toast.LENGTH_SHORT
                                ).show();

                                loadMyLeaves();
                            }

                            @Override
                            public void onFailure(
                                    @NonNull Call<LeaveResponse> call,
                                    @NonNull Throwable t) {

                                btnSubmit.setEnabled(true);

                                Toast.makeText(
                                        LeaveActivity.this,
                                        getString(
                                                R.string.leave_application_failed
                                        ),
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                );
    }

    // =========================================================
    // LEAVE TYPE TEXT
    // =========================================================

    private String getLeaveTypeText(
            LeaveResponse leave) {

        if (leave.getLeaveType() == null) {
            return getString(R.string.leave_type);
        }

        String type =
                String.valueOf(
                        leave.getLeaveType()
                );

        switch (type.toUpperCase()) {

            case "CASUAL":
                return getString(
                        R.string.casual_leave
                );

            case "SICK":
                return getString(
                        R.string.sick_leave
                );

            case "PERSONAL":
                return getString(
                        R.string.personal_leave
                );

            case "OTHER":
                return getString(
                        R.string.other_leave
                );

            default:
                return type;
        }
    }

    // =========================================================
    // STATUS TEXT
    // =========================================================

    private String getLeaveStatusText(
            LeaveResponse leave) {

        if (leave.getStatus() == null) {
            return "PENDING";
        }

        return String.valueOf(
                leave.getStatus()
        ).toUpperCase();
    }

    // =========================================================
    // DATE HELPERS
    // =========================================================

    private String getDateString(
            Object date) {

        if (date == null) {
            return "";
        }

        String value =
                String.valueOf(date)
                        .trim();

        if (value.length() >= 10) {
            return value.substring(0, 10);
        }

        return value;
    }

    private String formatDisplayDate(
            String value) {

        if (value == null
                || value.trim().isEmpty()) {

            return "-";
        }

        try {

            String date =
                    value.trim();

            if (date.length() >= 10) {
                date =
                        date.substring(0, 10);
            }

            Calendar calendar =
                    Calendar.getInstance();

            calendar.setTime(
                    dateFormat.parse(date)
            );

            return displayDateFormat.format(
                    calendar.getTime()
            );

        } catch (Exception e) {

            return value;
        }
    }

    // =========================================================
    // SESSION
    // =========================================================

    private void handleSessionExpired() {

        sessionManager.logout();

        Toast.makeText(
                this,
                getString(
                        R.string.session_expired
                ),
                Toast.LENGTH_SHORT
        ).show();

        Intent intent =
                new Intent(
                        LeaveActivity.this,
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
}