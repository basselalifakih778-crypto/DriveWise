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
import com.example.drivewise.databinding.ActivityRegisterBinding
import com.example.drivewise.domain.model.Role
import com.example.drivewise.ui.viewmodel.AuthEvent
import com.example.drivewise.ui.viewmodel.AuthViewModel
import com.example.drivewise.Data.remote.FirebaseAuthRepository
import com.example.drivewise.R
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(
            FirebaseAuthRepository(
                com.google.firebase.auth.FirebaseAuth.getInstance(),
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
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

        binding.etConfirmPassword.addTextChangedListener {
            viewModel.onConfirmPasswordChanged(it?.toString() ?: "")
        }

        binding.rgRole.setOnCheckedChangeListener { _, checkedId ->
            val role = when (checkedId) {
                R.id.rbAdmin -> Role.ADMIN
                else -> Role.CLIENT
            }
            viewModel.onRoleSelected(role)
        }

        binding.btnRegister.setOnClickListener {
            viewModel.register()
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.btnRegister.isEnabled = !state.isLoading

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
                        is AuthEvent.Registered -> {
                            Toast.makeText(this@RegisterActivity, "Registration successful! Please login.", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        is AuthEvent.RoleMismatch -> {
                            Toast.makeText(this@RegisterActivity, "Invalid role", Toast.LENGTH_LONG).show()
                        }
                        else -> { /* Handle other events */ }
                    }
                }
            }
        }
    }
}

