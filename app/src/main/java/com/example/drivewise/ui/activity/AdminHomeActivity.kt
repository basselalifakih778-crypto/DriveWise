/**
 * AdminHomeActivity.kt
 * ======================
 * This is the main dashboard screen for admin users.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. DASHBOARD PATTERN: Shows a grid of cards, each linking to a different feature.
 *    This is a common UX pattern for admin portals.
 *
 * 2. ROLE-BASED HOME: Users are routed here if their role is "admin".
 *    Clients go to ClientHomeActivity instead.
 *
 * 3. LOGOUT FLOW: When admin logs out:
 *    - Clear SessionManager (SharedPreferences)
 *    - Sign out from Firebase Auth
 *    - Navigate to LoginActivity with cleared back stack
 *
 * ADMIN FEATURES:
 * - Manage Cars: Add, edit, delete rental cars
 * - Manage Bookings: Approve, reject, complete bookings
 * - Customer Profiles: View clients, verify documents
 * - Logout: Sign out and return to login
 */
package com.example.drivewise.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.drivewise.databinding.ActivityAdminHomeBinding
import com.example.drivewise.util.SessionManager
import com.google.firebase.auth.FirebaseAuth

/**
 * Admin dashboard screen.
 *
 * Provides navigation to all admin features via card buttons.
 */
class AdminHomeActivity : AppCompatActivity() {

    /** ViewBinding for activity_admin_home.xml */
    private lateinit var binding: ActivityAdminHomeBinding

    /** Session manager for logout */
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        setupViews()
    }

    /**
     * Sets up card click listeners for navigation.
     */
    private fun setupViews() {
        setSupportActionBar(binding.toolbar)

        // ─────────────────────────────────────────────────────────────────────
        // NAVIGATION CARDS
        // Each card opens a different admin feature screen
        // ─────────────────────────────────────────────────────────────────────

        // Manage Cars - CRUD for rental car fleet
        binding.cardManageCars.setOnClickListener {
            startActivity(Intent(this, ManageCarsActivity::class.java))
        }

        // Manage Bookings - Approve/reject/complete booking requests
        binding.cardManageBookings.setOnClickListener {
            startActivity(Intent(this, ManageBookingsActivity::class.java))
        }

        // Customer Profiles - View clients, verify documents
        binding.cardCustomerProfiles.setOnClickListener {
            startActivity(Intent(this, CustomerProfilesActivity::class.java))
        }

        // ─────────────────────────────────────────────────────────────────────
        // LOGOUT
        // Clear session, sign out from Firebase, return to login
        // ─────────────────────────────────────────────────────────────────────

        binding.cardLogout.setOnClickListener {
            // Clear local session data
            sessionManager.clearSession()

            // Sign out from Firebase Auth
            FirebaseAuth.getInstance().signOut()

            // Navigate to login with cleared back stack
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
