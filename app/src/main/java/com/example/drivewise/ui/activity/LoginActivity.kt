package com.example.drivewise.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.drivewise.databinding.ActivityLoginBinding
import com.example.drivewise.domain.model.Role
import com.example.drivewise.ui.viewmodel.AuthEvent
import com.example.drivewise.ui.viewmodel.AuthViewModel
import com.example.drivewise.Data.remote.FirebaseAuthRepository
import com.example.drivewise.util.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(
            FirebaseAuthRepository(
                FirebaseAuth.getInstance(),
                FirebaseFirestore.getInstance()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        // Check if user is already logged in
        if (sessionManager.isLoggedIn()) {
            navigateToHome(sessionManager.getUserRole() ?: "client")
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeState()
    }

    private fun setupViews() {
        binding.etEmail.addTextChangedListener {
            viewModel.onEmailChanged(it?.toString() ?: "")
        }

        binding.etPassword.addTextChangedListener {
            viewModel.onPasswordChanged(it?.toString() ?: "")
        }

        binding.rgRole.setOnCheckedChangeListener { _, checkedId ->
            val role = when (checkedId) {
                com.example.drivewise.R.id.rbAdmin -> Role.ADMIN
                else -> Role.CLIENT
            }
            viewModel.onRoleSelected(role)
        }

        binding.btnLogin.setOnClickListener {
            viewModel.login()
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.btnLogin.isEnabled = !state.isLoading

                    state.errorMessage?.let { error ->
                        binding.tvError.text = error
                        binding.tvError.visibility = View.VISIBLE
                    } ?: run {
                        binding.tvError.visibility = View.GONE
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when (event) {
                        is AuthEvent.LoggedIn -> {
                            // Save login session
                            sessionManager.saveLoginSession(
                                userId = event.user.uid,
                                email = event.user.email,
                                role = event.user.role,
                                name = event.user.fullName
                            )

                            navigateToHome(event.user.role)
                        }
                        is AuthEvent.RoleMismatch -> {
                            Toast.makeText(this@LoginActivity, "Invalid role for this account", Toast.LENGTH_LONG).show()
                        }
                        AuthEvent.Registered -> {
                            Toast.makeText(this@LoginActivity, "Registration successful! Please login.", Toast.LENGTH_SHORT).show()
                        }
                        else -> { /* Handle other events */ }
                    }
                }
            }
        }
    }

    private fun navigateToHome(role: String) {
        val intent = when (role) {
            Role.ADMIN.key, "admin" -> Intent(this, AdminHomeActivity::class.java)
            else -> Intent(this, ClientHomeActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

