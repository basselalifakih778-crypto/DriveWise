package com.example.drivewise.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.drivewise.databinding.ActivityClientHomeBinding
import com.example.drivewise.util.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ClientHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClientHomeBinding
    private lateinit var sessionManager: SessionManager
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var userDocumentListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        setupViews()
        loadUserInfo()
    }

    override fun onResume() {
        super.onResume()
        // Re-attach the listener when returning to this activity
        loadUserInfo()
    }

    override fun onPause() {
        super.onPause()
        // Remove listener when activity is paused to avoid duplicate listeners
        userDocumentListener?.remove()
        userDocumentListener = null
    }

    override fun onDestroy() {
        super.onDestroy()
        userDocumentListener?.remove()
    }

    private fun setupViews() {
        setSupportActionBar(binding.toolbar)

        binding.cardBrowseCars.setOnClickListener {
            startActivity(Intent(this, BrowseCarsActivity::class.java))
        }

        binding.cardMyBookings.setOnClickListener {
            startActivity(Intent(this, MyBookingsActivity::class.java))
        }

        binding.cardMyProfile.setOnClickListener {
            startActivity(Intent(this, MyProfileActivity::class.java))
        }

        binding.cardLogout.setOnClickListener {
            sessionManager.clearSession()
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadUserInfo() {
        val userId = auth.currentUser?.uid ?: return

        // Remove any existing listener to avoid duplicates
        userDocumentListener?.remove()

        // Use real-time listener to keep verification status in sync
        userDocumentListener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Silently fail
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val fullName = snapshot.getString("fullName") ?: ""
                    val isVerified = snapshot.getBoolean("isVerified") ?: false

                    // Update welcome message
                    if (fullName.isNotEmpty()) {
                        binding.tvWelcome.text = "Welcome, $fullName!"
                    } else {
                        binding.tvWelcome.text = "Welcome!"
                    }

                    // Update verification status
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
