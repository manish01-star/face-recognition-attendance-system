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


        // =========================================================
        // INITIALIZE
        // =========================================================

        sessionManager =
                new SessionManager(this);

        apiService =
                ApiClient.getApiService(this);


        // =========================================================
        // CHECK EXISTING LOGIN
        // =========================================================

        if (sessionManager.isLoggedIn()) {

            openMainActivity();

            return;
        }


        // =========================================================
        // INITIALIZE VIEWS
        // =========================================================

        etUsername =
                findViewById(R.id.etUsername);

        etPassword =
                findViewById(R.id.etPassword);

        btnLogin =
                findViewById(R.id.btnLogin);


        // =========================================================
        // LOGIN BUTTON
        // =========================================================

        btnLogin.setOnClickListener(
                v -> login()
        );
    }


    // =============================================================
    // LOGIN
    // =============================================================

    private void login() {

        String username =
                etUsername
                        .getText()
                        .toString()
                        .trim();

        String password =
                etPassword
                        .getText()
                        .toString();


        // =========================================================
        // USERNAME VALIDATION
        // =========================================================

        if (TextUtils.isEmpty(username)) {

            etUsername.setError(
                    "Username is required"
            );

            etUsername.requestFocus();

            return;
        }


        // =========================================================
        // PASSWORD VALIDATION
        // =========================================================

        if (TextUtils.isEmpty(password)) {

            etPassword.setError(
                    "Password is required"
            );

            etPassword.requestFocus();

            return;
        }


        // =========================================================
        // DISABLE BUTTON
        // =========================================================

        btnLogin.setEnabled(false);


        // =========================================================
        // LOGIN REQUEST
        // =========================================================

        LoginRequest loginRequest =
                new LoginRequest(
                        username,
                        password
                );


        // =========================================================
        // CALL API
        // =========================================================

        apiService
                .login(loginRequest)
                .enqueue(
                        new Callback<LoginResponse>() {

                            @Override
                            public void onResponse(
                                    Call<LoginResponse> call,
                                    Response<LoginResponse> response) {

                                btnLogin.setEnabled(true);


                                // =================================================
                                // SUCCESS
                                // =================================================

                                if (
                                        response.isSuccessful()
                                                && response.body() != null
                                ) {

                                    LoginResponse loginResponse =
                                            response.body();


                                    // =================================================
                                    // ACCESS TOKEN
                                    // =================================================

                                    String accessToken =
                                            loginResponse.getAccessToken();


                                    if (
                                            accessToken == null
                                                    || accessToken.trim().isEmpty()
                                    ) {

                                        Toast.makeText(
                                                LoginActivity.this,
                                                "Login failed: Access token not received",
                                                Toast.LENGTH_LONG
                                        ).show();

                                        return;
                                    }


                                    // =================================================
                                    // ROLE
                                    // =================================================

                                    String role =
                                            loginResponse.getRole();


                                    if (
                                            role == null
                                                    || role.trim().isEmpty()
                                    ) {

                                        Toast.makeText(
                                                LoginActivity.this,
                                                "Login failed: User role not received",
                                                Toast.LENGTH_LONG
                                        ).show();

                                        return;
                                    }


                                    // =================================================
                                    // SAVE SESSION
                                    // =================================================

                                    /*
                                     * Existing SessionManager
                                     * accepts 3 arguments.
                                     *
                                     * accessToken
                                     * username
                                     * role
                                     */

                                    sessionManager.saveSession(
                                            loginResponse.getAccessToken(),
                                            loginResponse.getUsername(),
                                            loginResponse.getRole()
                                    );


                                    // =================================================
                                    // LOGIN SUCCESS
                                    // =================================================

                                    Toast.makeText(
                                            LoginActivity.this,
                                            "Login successful",
                                            Toast.LENGTH_SHORT
                                    ).show();


                                    // =================================================
                                    // OPEN MAIN ACTIVITY
                                    // =================================================

                                    openMainActivity();

                                }


                                // =================================================
                                // LOGIN ERROR
                                // =================================================

                                else {

                                    if (response.code() == 401) {

                                        Toast.makeText(
                                                LoginActivity.this,
                                                "Invalid username or password",
                                                Toast.LENGTH_LONG
                                        ).show();

                                    } else if (response.code() == 403) {

                                        Toast.makeText(
                                                LoginActivity.this,
                                                "Access denied",
                                                Toast.LENGTH_LONG
                                        ).show();

                                    } else {

                                        Toast.makeText(
                                                LoginActivity.this,
                                                "Login failed. HTTP "
                                                        + response.code(),
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }
                                }
                            }


                            // =========================================================
                            // NETWORK FAILURE
                            // =========================================================

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
                        }
                );
    }


    // =============================================================
    // OPEN MAIN ACTIVITY
    // =============================================================

    private void openMainActivity() {

        Intent intent =
                new Intent(
                        LoginActivity.this,
                        MainActivity.class
                );


        startActivity(intent);


        /*
         * LoginActivity ko back stack se remove
         * kar diya jayega.
         */

        finish();
    }
}
