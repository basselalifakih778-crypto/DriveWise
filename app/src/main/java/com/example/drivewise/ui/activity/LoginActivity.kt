/**
 * LoginActivity.kt
 * ==================
 * This Activity handles user login - the first screen users see (launcher).
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. LAUNCHER ACTIVITY: This Activity has the MAIN + LAUNCHER intent filter
 *    in AndroidManifest.xml, making it the app's entry point.
 *
 * 2. SESSION CHECK: Before showing the login form, we check if user is already
 *    logged in (using SessionManager). If yes, skip to the appropriate home screen.
 *
 * 3. ROLE-BASED NAVIGATION: After login, users are routed to different screens:
 *    - Admin → AdminHomeActivity
 *    - Client → ClientHomeActivity
 *
 * 4. CLEAR TASK FLAGS: When navigating to home, we use flags to clear the
 *    back stack. This prevents the user from pressing back to return to login.
 *
 * FLOW:
 * 1. onCreate checks if already logged in
 * 2. If logged in → navigate to home (skip login screen)
 * 3. If not → show login form
 * 4. User enters credentials, selects role, clicks Login
 * 5. AuthViewModel validates with Firebase
 * 6. On success → save session, navigate to home
 * 7. On failure → show error message
 */
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

/**
 * Login screen - the app's entry point.
 *
 * Allows users to:
 * - Enter email and password
 * - Select their role (Admin or Client)
 * - Login to their account
 * - Navigate to registration
 */
class LoginActivity : AppCompatActivity() {

    // ═══════════════════════════════════════════════════════════════════════════
    // PROPERTIES
    // ═══════════════════════════════════════════════════════════════════════════

    /** ViewBinding for activity_login.xml */
    private lateinit var binding: ActivityLoginBinding

    /** SessionManager for checking/saving login state */
    private lateinit var sessionManager: SessionManager

    /**
     * AuthViewModel for handling login logic.
     * Created using our factory with FirebaseAuthRepository injected.
     */
    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(
            FirebaseAuthRepository(
                FirebaseAuth.getInstance(),
                FirebaseFirestore.getInstance()
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SessionManager
        sessionManager = SessionManager(this)

        // ─────────────────────────────────────────────────────────────────────
        // CHECK IF ALREADY LOGGED IN
        // Skip login screen if user has active session
        // ─────────────────────────────────────────────────────────────────────
        if (sessionManager.isLoggedIn()) {
            // Get saved role and navigate to appropriate home screen
            navigateToHome(sessionManager.getUserRole() ?: "client")
            return  // Don't continue with login screen setup
        }

        // User is not logged in - show the login form
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeState()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UI SETUP
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Sets up UI interactions.
     */
    private fun setupViews() {
        // ─────────────────────────────────────────────────────────────────────
        // TEXT INPUT LISTENERS
        // Update ViewModel state as user types
        // ─────────────────────────────────────────────────────────────────────

        binding.etEmail.addTextChangedListener {
            viewModel.onEmailChanged(it?.toString() ?: "")
        }

        binding.etPassword.addTextChangedListener {
            viewModel.onPasswordChanged(it?.toString() ?: "")
        }

        // ─────────────────────────────────────────────────────────────────────
        // ROLE SELECTION
        // ─────────────────────────────────────────────────────────────────────

        binding.rgRole.setOnCheckedChangeListener { _, checkedId ->
            val role = when (checkedId) {
                com.example.drivewise.R.id.rbAdmin -> Role.ADMIN
                else -> Role.CLIENT
            }
            viewModel.onRoleSelected(role)
        }

        // ─────────────────────────────────────────────────────────────────────
        // BUTTONS
        // ─────────────────────────────────────────────────────────────────────

        // Login button - tell ViewModel to start login process
        binding.btnLogin.setOnClickListener {
            viewModel.login()
        }

        // Register link - navigate to RegisterActivity
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STATE OBSERVATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Observes ViewModel state and events.
     */
    private fun observeState() {
        // ─────────────────────────────────────────────────────────────────────
        // OBSERVE UI STATE (loading, errors)
        // ─────────────────────────────────────────────────────────────────────

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    // Show/hide loading indicator
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    // Disable login button while loading
                    binding.btnLogin.isEnabled = !state.isLoading

                    // Show/hide error message
                    state.errorMessage?.let { error ->
                        binding.tvError.text = error
                        binding.tvError.visibility = View.VISIBLE
                    } ?: run {
                        binding.tvError.visibility = View.GONE
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // OBSERVE ONE-TIME EVENTS (login success, role mismatch)
        // ─────────────────────────────────────────────────────────────────────

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when (event) {
                        is AuthEvent.LoggedIn -> {
                            // ─────────────────────────────────────────────────
                            // LOGIN SUCCESS
                            // Save session and navigate to home
                            // ─────────────────────────────────────────────────

                            sessionManager.saveLoginSession(
                                userId = event.user.uid,
                                email = event.user.email,
                                role = event.user.role,
                                name = event.user.fullName
                            )

                            navigateToHome(event.user.role)
                        }

                        is AuthEvent.RoleMismatch -> {
                            // User selected wrong role (e.g., clicked Admin but is a Client)
                            Toast.makeText(
                                this@LoginActivity,
                                "Invalid role for this account",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        AuthEvent.Registered -> {
                            // This shouldn't happen in LoginActivity, but handle gracefully
                            Toast.makeText(
                                this@LoginActivity,
                                "Registration successful! Please login.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        else -> {
                            // Handle other events (null, LoggedOut)
                        }
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Navigates to the appropriate home screen based on user role.
     *
     * @param role The user's role ("admin" or "client")
     */
    private fun navigateToHome(role: String) {
        // Choose destination based on role
        val intent = when (role) {
            Role.ADMIN.key, "admin" -> Intent(this, AdminHomeActivity::class.java)
            else -> Intent(this, ClientHomeActivity::class.java)
        }

        // FLAGS EXPLANATION:
        // FLAG_ACTIVITY_NEW_TASK: Start a new task (back stack)
        // FLAG_ACTIVITY_CLEAR_TASK: Clear all previous activities from back stack
        // Together: User can't press back to return to login screen
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()  // Close LoginActivity
    }
}

