package com.college.attendance.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.college.attendance.R;
import com.college.attendance.api.ApiClient;
import com.college.attendance.api.ApiService;
import com.college.attendance.dto.LoginRequest;
import com.college.attendance.dto.LoginResponse;
import com.college.attendance.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;

    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        apiService = ApiClient.getApiService(this);

        // Already logged in
        if (sessionManager.isLoggedIn()) {
            openMainActivity();
            return;
        }

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> login());
    }

    private void login() {

        String username =
                etUsername.getText().toString().trim();

        String password =
                etPassword.getText().toString().trim();

        // Username validation
        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Username is required");
            etUsername.requestFocus();
            return;
        }

        // Password validation
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);

        LoginRequest loginRequest =
                new LoginRequest(username, password);

        apiService.login(loginRequest)
                .enqueue(new Callback<LoginResponse>() {

                    @Override
                    public void onResponse(
                            Call<LoginResponse> call,
                            Response<LoginResponse> response) {

                        btnLogin.setEnabled(true);

                        if (response.isSuccessful()
                                && response.body() != null) {

                            LoginResponse loginResponse =
                                    response.body();

                            String accessToken =
                                    loginResponse.getAccessToken();

                            if (accessToken == null
                                    || accessToken.trim().isEmpty()) {

                                Toast.makeText(
                                        LoginActivity.this,
                                        "Login failed: Access token not received",
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            // Save session
                            sessionManager.saveSession(
                                    loginResponse.getAccessToken(),
                                    loginResponse.getUsername(),
                                    loginResponse.getRole()
                            );

                            Toast.makeText(
                                    LoginActivity.this,
                                    "Login successful",
                                    Toast.LENGTH_SHORT
                            ).show();

                            openMainActivity();

                        } else {

                            Toast.makeText(
                                    LoginActivity.this,
                                    "Invalid username or password",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<LoginResponse> call,
                            Throwable t) {

                        btnLogin.setEnabled(true);

                        Toast.makeText(
                                LoginActivity.this,
                                "Unable to connect to server",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void openMainActivity() {

        Intent intent =
                new Intent(
                        LoginActivity.this,
                        MainActivity.class
                );

        startActivity(intent);

        // User cannot go back to Login
        finish();
    }
}