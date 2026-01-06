/**
 * ClientHomeActivity.kt
 * =======================
 * This is the main dashboard screen for client users.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. CLIENT DASHBOARD: Shows options available to clients:
 *    - Browse Cars: View available cars, make bookings
 *    - My Bookings: See booking history and status
 *    - My Profile: Update profile, upload documents
 *    - Logout
 *
 * 2. REAL-TIME STATUS: Uses Firestore listener to show live verification status.
 *    If admin verifies the client while they're on this screen, it updates immediately.
 *
 * 3. LISTENER LIFECYCLE: The Firestore listener is added in onResume() and
 *    removed in onPause(). This prevents:
 *    - Duplicate listeners
 *    - Memory leaks
 *    - Updates when Activity is not visible
 *
 * 4. WELCOME MESSAGE: Personalized greeting using the client's name from Firestore.
 *
 * CLIENT FEATURES:
 * - Browse Cars: View and book available cars
 * - My Bookings: Track booking status
 * - My Profile: Upload license/ID for verification
 * - Logout: Sign out and return to login
 */
package com.example.drivewise.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.drivewise.databinding.ActivityClientHomeBinding
import com.example.drivewise.util.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Client dashboard screen.
 *
 * Shows personalized welcome, verification status, and navigation cards.
 */
class ClientHomeActivity : AppCompatActivity() {

    /** ViewBinding for activity_client_home.xml */
    private lateinit var binding: ActivityClientHomeBinding

    /** Session manager for logout */
    private lateinit var sessionManager: SessionManager

    /** Firestore instance for real-time user data */
    private val firestore = FirebaseFirestore.getInstance()

    /** Firebase Auth for getting current user ID */
    private val auth = FirebaseAuth.getInstance()

    /**
     * Firestore listener registration.
     * We store this so we can remove the listener later.
     * This prevents memory leaks and duplicate listeners.
     */
    private var userDocumentListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        setupViews()
        loadUserInfo()
    }

    /**
     * Called when Activity becomes visible again.
     * Re-attach listener to get fresh data.
     */
    override fun onResume() {
        super.onResume()
        loadUserInfo()
    }

    /**
     * Called when Activity is no longer visible.
     * Remove listener to prevent updates while hidden.
     */
    override fun onPause() {
        super.onPause()
        userDocumentListener?.remove()
        userDocumentListener = null
    }

    /**
     * Called when Activity is destroyed.
     * Final cleanup of listener.
     */
    override fun onDestroy() {
        super.onDestroy()
        userDocumentListener?.remove()
    }

    /**
     * Sets up card click listeners for navigation.
     */
    private fun setupViews() {
        setSupportActionBar(binding.toolbar)

        // ─────────────────────────────────────────────────────────────────────
        // NAVIGATION CARDS
        // ─────────────────────────────────────────────────────────────────────

        // Browse Cars - View available cars, make bookings
        binding.cardBrowseCars.setOnClickListener {
            startActivity(Intent(this, BrowseCarsActivity::class.java))
        }

        // My Bookings - View booking history and status
        binding.cardMyBookings.setOnClickListener {
            startActivity(Intent(this, MyBookingsActivity::class.java))
        }

        // My Profile - Update profile, upload documents
        binding.cardMyProfile.setOnClickListener {
            startActivity(Intent(this, MyProfileActivity::class.java))
        }

        // ─────────────────────────────────────────────────────────────────────
        // LOGOUT
        // ─────────────────────────────────────────────────────────────────────

        binding.cardLogout.setOnClickListener {
            sessionManager.clearSession()
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    /**
     * Loads user info with real-time updates.
     *
     * Uses Firestore snapshot listener to:
     * - Show personalized welcome message
     * - Display live verification status
     */
    private fun loadUserInfo() {
        val userId = auth.currentUser?.uid ?: return

        // Remove any existing listener to avoid duplicates
        userDocumentListener?.remove()

        // ─────────────────────────────────────────────────────────────────────
        // REAL-TIME LISTENER
        // Fires immediately with current data, then again on any change
        // ─────────────────────────────────────────────────────────────────────

        userDocumentListener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                // Handle errors silently
                if (error != null) {
                    return@addSnapshotListener
                }

                // Process snapshot if it exists
                if (snapshot != null && snapshot.exists()) {
                    val fullName = snapshot.getString("fullName") ?: ""
                    val isVerified = snapshot.getBoolean("isVerified") ?: false

                    // ─────────────────────────────────────────────────────────
                    // UPDATE WELCOME MESSAGE
                    // ─────────────────────────────────────────────────────────

                    if (fullName.isNotEmpty()) {
                        binding.tvWelcome.text = "Welcome, $fullName!"
                    } else {
                        binding.tvWelcome.text = "Welcome!"
                    }

                    // ─────────────────────────────────────────────────────────
                    // UPDATE VERIFICATION STATUS
                    // Shows whether admin has verified client's documents
                    // ─────────────────────────────────────────────────────────

                    if (isVerified) {
                        binding.tvVerificationStatus.text = "✅ Verified"
                        binding.tvVerificationStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                    } else {
                        binding.tvVerificationStatus.text = "⚠️ Not Verified"
                        binding.tvVerificationStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
                    }
                }
            }
    }
}
