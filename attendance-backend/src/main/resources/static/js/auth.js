const AUTH_TOKEN_KEY = "accessToken";

/**
 * Get JWT token
 */
function getToken() {
    return localStorage.getItem(AUTH_TOKEN_KEY);
}

/**
 * Check whether user is logged in
 */
function isLoggedIn() {
    return !!getToken();
}

/**
 * Logout user
 */
function logout() {

    localStorage.removeItem(AUTH_TOKEN_KEY);

    window.location.href = "login.html";
}

/**
 * Protect page
 *
 * Call this at the beginning of every
 * protected page.
 */
function requireAuth() {

    const token = getToken();

    if (!token) {
        window.location.href = "login.html";
        return false;
    }

    return true;
}

/**
 * Common API fetch
 *
 * Automatically adds:
 * Authorization: Bearer <token>
 */
async function apiFetch(url, options = {}) {

    const token = getToken();

    if (!token) {
        logout();
        return null;
    }

    const headers = {
        ...(options.headers || {}),
        "Authorization": `Bearer ${token}`
    };

    console.log("API URL:", url);
    console.log("Method:", options.method || "GET");
    console.log("JWT present:", !!token);
    console.log("JWT length:", token.length);

    const response = await fetch(url, {
        ...options,
        headers
    });

    console.log("Response Status:", response.status);
    console.log("Response Status Text:", response.statusText);
    console.log(
        "Response Content-Type:",
        response.headers.get("content-type")
    );

    if (response.status === 401) {
        logout();
        return null;
    }

    // IMPORTANT:
    // Don't swallow 403.
    // Return response so caller can inspect backend message.
    return response;
}