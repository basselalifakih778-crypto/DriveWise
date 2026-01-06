/**
 * SessionManager.kt
 * ==================
 * Manages user session data using SharedPreferences.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. SHAREDPREFERENCES: Android's simple key-value storage for small data.
 *    - Persists across app restarts
 *    - Stored in XML file on device
 *    - Great for settings, login state, small user data
 *    - NOT for large data (use Room database or files instead)
 *
 * 2. COMPANION OBJECT: Holds constants (like static final in Java).
 *    Keys are defined once and reused throughout the class.
 *
 * 3. SESSION PATTERN: When user logs in, we save their info.
 *    On app restart, we check if session exists to skip login screen.
 *
 * WHY NOT JUST USE FIREBASE AUTH?
 * Firebase Auth does track sessions, but SharedPreferences gives us:
 * - Faster checks (local, no network)
 * - Offline access to user info (role, name)
 * - Full control over session lifecycle
 *
 * USED BY: LoginActivity (check/save session), Activities (get user info), logout flows
 */
package com.example.drivewise.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Helper class to manage user login session.
 *
 * Stores user data in SharedPreferences for quick access.
 *
 * @param context Android Context (usually Activity or Application)
 */
class SessionManager(context: Context) {

    // ─────────────────────────────────────────────────────────────────────────
    // SHAREDPREFERENCES INSTANCE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * SharedPreferences instance for reading/writing session data.
     *
     * MODE_PRIVATE: Only this app can access this file.
     * PREF_NAME: The filename for the preferences XML file.
     */
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREF_NAME,           // File name: "DriveWiseSession"
        Context.MODE_PRIVATE // Only this app can access
    )

    // ─────────────────────────────────────────────────────────────────────────
    // CONSTANTS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Companion object holds the keys used to store/retrieve data.
     * Using constants prevents typos and makes refactoring easier.
     */
    companion object {
        private const val PREF_NAME = "DriveWiseSession"    // Preferences file name
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"   // Boolean: is user logged in?
        private const val KEY_USER_ID = "userId"            // String: Firebase UID
        private const val KEY_USER_EMAIL = "userEmail"      // String: user's email
        private const val KEY_USER_ROLE = "userRole"        // String: "admin" or "client"
        private const val KEY_USER_NAME = "userName"        // String: user's display name
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SAVE SESSION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Saves user login session data.
     *
     * Called after successful login to persist user info.
     *
     * HOW SHARED PREFERENCES WORK:
     * 1. Call edit() to get an Editor
     * 2. Put values using putString(), putBoolean(), etc.
     * 3. Call apply() to save asynchronously (or commit() for synchronous)
     *
     * @param userId The user's Firebase UID
     * @param email The user's email
     * @param role The user's role ("admin" or "client")
     * @param name The user's display name
     */
    fun saveLoginSession(userId: String, email: String, role: String, name: String) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_ROLE, role)
            putString(KEY_USER_NAME, name)
            apply()  // Save asynchronously (faster, recommended)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHECK LOGIN STATUS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Checks if user is currently logged in.
     *
     * @return true if user has an active session, false otherwise
     */
    fun isLoggedIn(): Boolean {
        // getBoolean(key, defaultValue) returns the value or default if not found
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GETTERS FOR SESSION DATA
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Gets the current user's ID.
     * @return User ID or null if not logged in
     */
    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    /**
     * Gets the current user's email.
     * @return Email or null if not logged in
     */
    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    /**
     * Gets the current user's role.
     * @return "admin", "client", or null if not logged in
     */
    fun getUserRole(): String? {
        return prefs.getString(KEY_USER_ROLE, null)
    }

    /**
     * Gets the current user's display name.
     * @return Name or null if not logged in
     */
    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CLEAR SESSION (LOGOUT)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Clears all session data (logout).
     *
     * After calling this, isLoggedIn() will return false.
     * User will need to login again.
     */
    fun clearSession() {
        prefs.edit().clear().apply()  // Remove all data from this preferences file
    }
}

