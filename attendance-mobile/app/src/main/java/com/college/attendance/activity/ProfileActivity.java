package com.college.attendance.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.college.attendance.R;
import com.college.attendance.utils.SessionManager;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvProfileInitial;
    private TextView tvProfileUsername;
    private TextView tvProfileRole;
    private TextView tvUsernameValue;
    private TextView tvRoleValue;

    private ImageButton btnBack;
    private TextView btnLogout;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);

        initializeViews();
        setupProfile();
        setupClickListeners();
    }

    private void initializeViews() {

        btnBack = findViewById(R.id.btnBack);
        btnLogout = findViewById(R.id.btnLogout);

        tvProfileInitial = findViewById(R.id.tvProfileInitial);
        tvProfileUsername = findViewById(R.id.tvProfileUsername);
        tvProfileRole = findViewById(R.id.tvProfileRole);

        tvUsernameValue = findViewById(R.id.tvUsernameValue);
        tvRoleValue = findViewById(R.id.tvRoleValue);
    }

    private void setupProfile() {

        String username = sessionManager.getUsername();
        String role = sessionManager.getRole();

        if (username == null || username.trim().isEmpty()) {
            username = "User";
        }

        if (role == null || role.trim().isEmpty()) {
            role = "USER";
        }

        username = username.trim();
        role = role.trim().toUpperCase();

        tvProfileUsername.setText(username);
        tvProfileRole.setText(role);

        tvUsernameValue.setText(username);
        tvRoleValue.setText(role);

        /*
         * First letter avatar
         */
        String firstLetter = username.substring(0, 1).toUpperCase();
        tvProfileInitial.setText(firstLetter);

        GradientDrawable avatarBackground = new GradientDrawable();
        avatarBackground.setShape(GradientDrawable.OVAL);
        avatarBackground.setColor(Color.rgb(113, 98, 232));

        tvProfileInitial.setBackground(avatarBackground);
    }

    private void setupClickListeners() {

        btnBack.setOnClickListener(v -> finish());

        btnLogout.setOnClickListener(v -> logout());
    }

    private void logout() {

        sessionManager.logout();

        Intent intent = new Intent(
                ProfileActivity.this,
                LoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }
}