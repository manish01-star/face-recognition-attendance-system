package com.college.attendance.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "college_attendance_session";

    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE = "role";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    /**
     * Save login session.
     */
    public void saveSession(
            String accessToken,
            String username,
            String role
    ) {
        preferences.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_USERNAME, username)
                .putString(KEY_ROLE, role)
                .apply();
    }

    /**
     * Get JWT access token.
     */
    public String getAccessToken() {
        return preferences.getString(KEY_ACCESS_TOKEN, null);
    }

    /**
     * Get logged-in username.
     */
    public String getUsername() {
        return preferences.getString(KEY_USERNAME, null);
    }

    /**
     * Get logged-in user's role.
     */
    public String getRole() {
        return preferences.getString(KEY_ROLE, null);
    }

    /**
     * Check whether user is logged in.
     */
    public boolean isLoggedIn() {
        String token = getAccessToken();

        return token != null && !token.trim().isEmpty();
    }

    /**
     * Clear complete login session.
     */
    public void logout() {
        preferences.edit()
                .clear()
                .apply();
    }
}