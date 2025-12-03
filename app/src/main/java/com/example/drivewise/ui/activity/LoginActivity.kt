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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
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
                            when (event.user.role) {
                                Role.ADMIN.key -> {
                                    startActivity(Intent(this@LoginActivity, AdminHomeActivity::class.java))
                                    finish()
                                }
                                Role.CLIENT.key -> {
                                    startActivity(Intent(this@LoginActivity, ClientHomeActivity::class.java))
                                    finish()
                                }
                            }
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
}

