package com.college.attendance.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.college.attendance.R;

public abstract class BaseActivity extends AppCompatActivity {

    private static final int COLOR_SELECTED = Color.parseColor("#7162E8");
    private static final int COLOR_UNSELECTED = Color.parseColor("#8A8A8A");

    private LinearLayout navHome;
    private LinearLayout navAttendance;
    private LinearLayout navLeave;
    private LinearLayout navProfile;

    private ImageView iconHome;
    private ImageView iconAttendance;
    private ImageView iconLeave;
    private ImageView iconProfile;

    private TextView textHome;
    private TextView textAttendance;
    private TextView textLeave;
    private TextView textProfile;

    @Override
    protected void onResume() {
        super.onResume();

        setupBottomNavigation();
    }

    private void setupBottomNavigation() {

        navHome = findViewById(R.id.navHome);
        navAttendance = findViewById(R.id.navAttendance);
        navLeave = findViewById(R.id.navLeave);
        navProfile = findViewById(R.id.navProfile);

        iconHome = findViewById(R.id.iconHome);
        iconAttendance = findViewById(R.id.iconAttendance);
        iconLeave = findViewById(R.id.iconLeave);
        iconProfile = findViewById(R.id.iconProfile);

        textHome = findViewById(R.id.textHome);
        textAttendance = findViewById(R.id.textAttendance);
        textLeave = findViewById(R.id.textLeave);
        textProfile = findViewById(R.id.textProfile);

        // If this Activity doesn't contain bottom navigation
        if (navHome == null
                || navAttendance == null
                || navLeave == null
                || navProfile == null) {
            return;
        }

        navHome.setOnClickListener(v -> openHome());

        navAttendance.setOnClickListener(v -> openAttendance());

        navLeave.setOnClickListener(v -> openLeave());

        navProfile.setOnClickListener(v -> openProfile());

        updateSelectedState();
    }

    // =========================================================
    // NAVIGATION
    // =========================================================

    private void openHome() {

        if (getCurrentTab() == Tab.HOME) {
            return;
        }

        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void openAttendance() {

        if (getCurrentTab() == Tab.ATTENDANCE) {
            return;
        }

        startActivity(new Intent(this, CalendarActivity.class));
        finish();
    }

    private void openLeave() {

        if (getCurrentTab() == Tab.LEAVE) {
            return;
        }

        startActivity(new Intent(this, LeaveActivity.class));
        finish();
    }

    private void openProfile() {

        if (getCurrentTab() == Tab.PROFILE) {
            return;
        }

        startActivity(new Intent(this, ProfileActivity.class));
        finish();
    }

    // =========================================================
    // CURRENT TAB
    // =========================================================

    protected abstract Tab getCurrentTab();

    public enum Tab {
        HOME,
        ATTENDANCE,
        LEAVE,
        PROFILE
    }

    // =========================================================
    // SELECTED STATE
    // =========================================================

    private void updateSelectedState() {

        resetNavigationState();

        Tab currentTab = getCurrentTab();

        if (currentTab == null) {
            return;
        }

        switch (currentTab) {

            case HOME:
                setSelected(iconHome, textHome);
                break;

            case ATTENDANCE:
                setSelected(iconAttendance, textAttendance);
                break;

            case LEAVE:
                setSelected(iconLeave, textLeave);
                break;

            case PROFILE:
                setSelected(iconProfile, textProfile);
                break;
        }
    }

    // =========================================================
    // RESET
    // =========================================================

    private void resetNavigationState() {

        setUnselected(iconHome, textHome);
        setUnselected(iconAttendance, textAttendance);
        setUnselected(iconLeave, textLeave);
        setUnselected(iconProfile, textProfile);
    }

    // =========================================================
    // SELECTED
    // =========================================================

    private void setSelected(
            ImageView icon,
            TextView text) {

        if (icon != null) {
            icon.setColorFilter(COLOR_SELECTED);
        }

        if (text != null) {
            text.setTextColor(COLOR_SELECTED);
            text.setTypeface(null, Typeface.BOLD);
        }
    }

    // =========================================================
    // UNSELECTED
    // =========================================================

    private void setUnselected(
            ImageView icon,
            TextView text) {

        if (icon != null) {
            icon.setColorFilter(COLOR_UNSELECTED);
        }

        if (text != null) {
            text.setTextColor(COLOR_UNSELECTED);
            text.setTypeface(null, Typeface.NORMAL);
        }
    }
}