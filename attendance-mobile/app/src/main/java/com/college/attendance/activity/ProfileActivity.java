package com.college.attendance.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.college.attendance.R;
import com.college.attendance.api.ApiClient;
import com.college.attendance.api.ApiService;
import com.college.attendance.dto.UserProfileResponse;
import com.college.attendance.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends BaseActivity {

    // ============================================================
    // HEADER
    // ============================================================

    private ImageButton btnBack;
    private MaterialButton btnLogout;

    // ============================================================
    // PROFILE HERO
    // ============================================================

    private ImageView ivProfileImage;
    private TextView tvProfileUsername;
    private TextView tvProfileRole;

    // ============================================================
    // ACCOUNT INFORMATION
    // ============================================================

    private TextView tvUserIdValue;
    private TextView tvUsernameValue;
    private TextView tvEmailValue;
    private TextView tvPhoneValue;
    private TextView tvRoleValue;
    private TextView tvRollNumberValue;
    private TextView tvEmployeeNumberValue;

    // ============================================================
    // CONDITIONAL LAYOUTS
    // ============================================================

    private View layoutRollNumber;
    private View layoutEmployeeNumber;

    // ============================================================
    // SESSION
    // ============================================================

    private SessionManager sessionManager;

    // ============================================================
    // CURRENT TAB
    // ============================================================

    @Override
    protected Tab getCurrentTab() {
        return Tab.PROFILE;
    }

    // ============================================================
    // ON CREATE
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);

        initializeViews();
        setupClickListeners();

        // Load complete profile from backend
        loadProfile();
    }

    // ============================================================
    // INITIALIZE VIEWS
    // ============================================================

    private void initializeViews() {

        // ========================================================
        // HEADER
        // ========================================================

        btnBack = findViewById(R.id.btnBack);
        btnLogout = findViewById(R.id.btnLogout);

        // ========================================================
        // PROFILE HERO
        // ========================================================

        ivProfileImage =
                findViewById(R.id.ivProfileImage);

        tvProfileUsername =
                findViewById(R.id.tvProfileUsername);

        tvProfileRole =
                findViewById(R.id.tvProfileRole);

        // ========================================================
        // ACCOUNT INFORMATION
        // ========================================================

        tvUserIdValue =
                findViewById(R.id.tvUserIdValue);

        tvUsernameValue =
                findViewById(R.id.tvUsernameValue);

        tvEmailValue =
                findViewById(R.id.tvEmailValue);

        tvPhoneValue =
                findViewById(R.id.tvPhoneValue);

        tvRoleValue =
                findViewById(R.id.tvRoleValue);

        tvRollNumberValue =
                findViewById(R.id.tvRollNumberValue);

        tvEmployeeNumberValue =
                findViewById(R.id.tvEmployeeNumberValue);

        // ========================================================
        // CONDITIONAL LAYOUTS
        // ========================================================

        layoutRollNumber =
                findViewById(R.id.layoutRollNumber);

        layoutEmployeeNumber =
                findViewById(R.id.layoutEmployeeNumber);
    }

    // ============================================================
    // LOAD PROFILE FROM API
    // ============================================================

    private void loadProfile() {

        ApiService apiService =
                ApiClient.getApiService(this);

        Call<UserProfileResponse> call =
                apiService.getMyProfile();

        call.enqueue(new Callback<UserProfileResponse>() {

            @Override
            public void onResponse(
                    Call<UserProfileResponse> call,
                    Response<UserProfileResponse> response) {

                if (response.isSuccessful()
                        && response.body() != null) {

                    UserProfileResponse profile =
                            response.body();

                    populateProfile(profile);

                } else {

                    Toast.makeText(
                            ProfileActivity.this,
                            "Unable to load profile",
                            Toast.LENGTH_SHORT
                    ).show();

                    // Fallback to session data
                    loadSessionProfile();
                }
            }

            @Override
            public void onFailure(
                    Call<UserProfileResponse> call,
                    Throwable t) {

                Toast.makeText(
                        ProfileActivity.this,
                        "Profile loading failed",
                        Toast.LENGTH_SHORT
                ).show();

                // Fallback to session data
                loadSessionProfile();
            }
        });
    }

    // ============================================================
    // POPULATE PROFILE
    // ============================================================

    private void populateProfile(
            UserProfileResponse profile) {

        // ========================================================
        // NAME
        // ========================================================

        String name = profile.getName();

        if (name == null
                || name.trim().isEmpty()) {

            name = profile.getUsername();
        }

        if (name == null
                || name.trim().isEmpty()) {

            name = "User";
        }

        // ========================================================
        // ROLE
        // ========================================================

        String role = profile.getRole();

        if (role == null
                || role.trim().isEmpty()) {

            role = "USER";
        }

        name = name.trim();
        role = role.trim().toUpperCase();

        // ========================================================
        // PROFILE HEADER
        // ========================================================

        tvProfileUsername.setText(name);
        tvProfileRole.setText(role);

        // ========================================================
        // USER ID
        // ========================================================

        setText(
                tvUserIdValue,
                profile.getUserId()
        );

        // ========================================================
        // USERNAME
        // ========================================================

        setText(
                tvUsernameValue,
                profile.getUsername()
        );

        // ========================================================
        // EMAIL
        // ========================================================

        setText(
                tvEmailValue,
                profile.getEmail()
        );

        // ========================================================
        // PHONE
        // ========================================================

        setText(
                tvPhoneValue,
                profile.getPhone()
        );

        // ========================================================
        // ROLE
        // ========================================================

        setText(
                tvRoleValue,
                profile.getRole()
        );

        // ========================================================
        // STUDENT - ROLL NUMBER
        // ========================================================

        if (profile.getRollNumber() != null
                && !profile.getRollNumber()
                .trim()
                .isEmpty()) {

            layoutRollNumber.setVisibility(
                    View.VISIBLE
            );

            tvRollNumberValue.setText(
                    profile.getRollNumber()
            );

        } else {

            layoutRollNumber.setVisibility(
                    View.GONE
            );
        }

        // ========================================================
        // TEACHER - EMPLOYEE NUMBER
        // ========================================================

        if (profile.getEmployeeNumber() != null
                && !profile.getEmployeeNumber()
                .trim()
                .isEmpty()) {

            layoutEmployeeNumber.setVisibility(
                    View.VISIBLE
            );

            tvEmployeeNumberValue.setText(
                    profile.getEmployeeNumber()
            );

        } else {

            layoutEmployeeNumber.setVisibility(
                    View.GONE
            );
        }

        // ========================================================
        // PROFILE IMAGE
        // ========================================================

        loadProfileImage(
                profile.getProfileImageUrl()
        );
    }

    // ============================================================
    // LOAD PROFILE IMAGE
    // ============================================================

    private void loadProfileImage(
            String profileImageUrl) {

        // --------------------------------------------------------
        // No image URL
        // --------------------------------------------------------

        if (profileImageUrl == null
                || profileImageUrl.trim().isEmpty()) {

            ivProfileImage.setImageResource(
                    R.drawable.ic_person
            );

            return;
        }

        profileImageUrl =
                profileImageUrl.trim();

        // --------------------------------------------------------
        // If backend already returns complete URL
        // --------------------------------------------------------

        String imageUrl;

        if (profileImageUrl.startsWith("http://")
                || profileImageUrl.startsWith("https://")) {

            imageUrl = profileImageUrl;

        } else {

            // ----------------------------------------------------
            // Relative URL from backend
            //
            // Example:
            // /uploads/profiles/8.jpg
            //
            // Becomes:
            // http://192.168.137.1:8080/uploads/profiles/8.jpg
            // ----------------------------------------------------

            imageUrl =
                    "http://192.168.137.1:8080"
                            + profileImageUrl;
        }

        // --------------------------------------------------------
        // Load image using Glide
        // --------------------------------------------------------

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .centerCrop()
                .into(ivProfileImage);
    }

    // ============================================================
    // FALLBACK - SESSION DATA
    // ============================================================

    private void loadSessionProfile() {

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

        username = username.trim();
        role = role.trim().toUpperCase();

        // Header
        tvProfileUsername.setText(username);
        tvProfileRole.setText(role);

        // Account
        tvUsernameValue.setText(username);
        tvRoleValue.setText(role);

        // Default image
        ivProfileImage.setImageResource(
                R.drawable.ic_person
        );
    }

    // ============================================================
    // SET TEXT SAFELY
    // ============================================================

    private void setText(
            TextView textView,
            Object value) {

        if (value == null) {

            textView.setText("-");

            return;
        }

        String text =
                String.valueOf(value).trim();

        if (text.isEmpty()) {

            textView.setText("-");

        } else {

            textView.setText(text);
        }
    }

    // ============================================================
    // CLICK LISTENERS
    // ============================================================

    private void setupClickListeners() {

        // Back
        btnBack.setOnClickListener(
                v -> finish()
        );

        // Logout
        btnLogout.setOnClickListener(
                v -> logout()
        );
    }

    // ============================================================
    // LOGOUT
    // ============================================================

    private void logout() {

        sessionManager.logout();

        Intent intent =
                new Intent(
                        ProfileActivity.this,
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

