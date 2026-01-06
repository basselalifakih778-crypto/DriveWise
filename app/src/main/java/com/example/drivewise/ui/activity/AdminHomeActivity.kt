package com.example.drivewise.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.drivewise.databinding.ActivityAdminHomeBinding
import com.example.drivewise.util.SessionManager
import com.google.firebase.auth.FirebaseAuth

class AdminHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminHomeBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        setupViews()
    }

    private fun setupViews() {
        setSupportActionBar(binding.toolbar)

        binding.cardManageCars.setOnClickListener {
            startActivity(Intent(this, ManageCarsActivity::class.java))
        }

        binding.cardManageBookings.setOnClickListener {
            startActivity(Intent(this, ManageBookingsActivity::class.java))
        }

        binding.cardCustomerProfiles.setOnClickListener {
            startActivity(Intent(this, CustomerProfilesActivity::class.java))
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
}
