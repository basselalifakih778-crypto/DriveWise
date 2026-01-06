/**
 * RegisterActivity.kt
 * ====================
 * This Activity handles new user registration in the DriveWise app.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. ACTIVITY: An Activity is a single screen in Android. This screen shows a registration form.
 *
 * 2. VIEWBINDING: Instead of using findViewById() to get references to UI elements,
 *    we use ViewBinding which auto-generates a "binding" object. It's safer and faster.
 *    Example: binding.etEmail gives us direct access to the email EditText from XML.
 *
 * 3. VIEWMODEL: We use MVVM architecture. The ViewModel (AuthViewModel) holds all the
 *    business logic and state. The Activity only handles UI display and user interactions.
 *
 * 4. MANUAL DEPENDENCY INJECTION: We don't use Hilt/Dagger here. Instead, we manually
 *    create the FirebaseAuthRepository and pass it to the ViewModel via a Factory.
 *
 * 5. STATEFLOW & EVENTS: The ViewModel exposes:
 *    - state: Current UI state (loading, error messages, form values)
 *    - event: One-time events (registration success, errors)
 *
 * FLOW: User fills form → ViewModel validates → Repository calls Firebase → Result flows back
 */
package com.example.drivewise.ui.activity

// Android framework imports
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels          // Kotlin extension to create ViewModels easily
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener  // Extension to listen for text changes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope     // Coroutine scope tied to Activity lifecycle
import androidx.lifecycle.repeatOnLifecycle  // Ensures collection only happens when Activity is visible

// Project-specific imports
import com.example.drivewise.databinding.ActivityRegisterBinding  // Auto-generated from activity_register.xml
import com.example.drivewise.domain.model.Role                     // Enum: ADMIN or CLIENT
import com.example.drivewise.ui.viewmodel.AuthEvent               // One-time events from ViewModel
import com.example.drivewise.ui.viewmodel.AuthViewModel           // ViewModel that handles auth logic
import com.example.drivewise.Data.remote.FirebaseAuthRepository   // Firebase implementation of AuthRepository
import com.example.drivewise.R                                     // Resource references (R.id.rbAdmin, etc.)
import kotlinx.coroutines.launch                                   // To start coroutines

/**
 * RegisterActivity - The registration screen for new users.
 *
 * Users can:
 * - Enter email, password, confirm password
 * - Select their role (Admin or Client)
 * - Submit the registration form
 * - Navigate back to login screen
 */
class RegisterActivity : AppCompatActivity() {

    // ═══════════════════════════════════════════════════════════════════════════
    // PROPERTIES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * ViewBinding instance for activity_register.xml
     * 'lateinit' means we promise to initialize it before using it (in onCreate)
     * This gives us type-safe access to all views in the layout
     */
    private lateinit var binding: ActivityRegisterBinding

    /**
     * The ViewModel that handles all registration logic.
     *
     * 'by viewModels { factory }' is a Kotlin delegate that:
     * 1. Creates the ViewModel lazily (only when first accessed)
     * 2. Survives configuration changes (like screen rotation)
     * 3. Uses our custom factory to inject the repository dependency
     *
     * WHY A FACTORY?
     * ViewModels can't have constructor parameters by default.
     * The factory pattern lets us pass the FirebaseAuthRepository to AuthViewModel.
     */
    private val viewModel: AuthViewModel by viewModels {
        // Create the factory, passing in the repository
        AuthViewModelFactory(
            // Create the Firebase repository with Auth and Firestore instances
            FirebaseAuthRepository(
                com.google.firebase.auth.FirebaseAuth.getInstance(),      // Firebase Auth SDK
                com.google.firebase.firestore.FirebaseFirestore.getInstance()  // Firestore database
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LIFECYCLE METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Called when the Activity is first created.
     * This is where we set up the UI and start observing data.
     *
     * @param savedInstanceState - Bundle containing saved state (if Activity was recreated)
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout using ViewBinding
        // This converts XML to actual View objects and gives us the 'binding' object
        binding = ActivityRegisterBinding.inflate(layoutInflater)

        // Set the inflated view as the content of this Activity
        setContentView(binding.root)

        // Set up click listeners and text change listeners
        setupViews()

        // Start observing ViewModel state and events
        observeState()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UI SETUP
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Sets up all the UI interactions (text listeners, click handlers).
     *
     * PATTERN: Every time the user types or clicks, we notify the ViewModel.
     * The ViewModel updates its state, and we observe those changes in observeState().
     * This keeps the Activity "dumb" - it only forwards events and displays state.
     */
    private fun setupViews() {
        // ─────────────────────────────────────────────────────────────────────
        // TEXT CHANGE LISTENERS
        // These fire every time the user types a character
        // ─────────────────────────────────────────────────────────────────────

        // When email field changes, tell the ViewModel
        binding.etEmail.addTextChangedListener {
            // 'it' is the Editable text; convert to String, or empty string if null
            viewModel.onEmailChanged(it?.toString() ?: "")
        }

        // When password field changes, tell the ViewModel
        binding.etPassword.addTextChangedListener {
            viewModel.onPasswordChanged(it?.toString() ?: "")
        }

        // When confirm password field changes, tell the ViewModel
        binding.etConfirmPassword.addTextChangedListener {
            viewModel.onConfirmPasswordChanged(it?.toString() ?: "")
        }

        // ─────────────────────────────────────────────────────────────────────
        // ROLE SELECTION (Radio Group)
        // ─────────────────────────────────────────────────────────────────────

        // When user selects a role (Admin or Client radio button)
        binding.rgRole.setOnCheckedChangeListener { _, checkedId ->
            // Determine which role was selected based on the radio button ID
            val role = when (checkedId) {
                R.id.rbAdmin -> Role.ADMIN   // Admin radio button selected
                else -> Role.CLIENT          // Client radio button selected (default)
            }
            // Tell the ViewModel which role is selected
            viewModel.onRoleSelected(role)
        }

        // ─────────────────────────────────────────────────────────────────────
        // BUTTON CLICK HANDLERS
        // ─────────────────────────────────────────────────────────────────────

        // When Register button is clicked, tell ViewModel to start registration
        binding.btnRegister.setOnClickListener {
            // ViewModel will validate inputs and call Firebase
            viewModel.register()
        }

        // When "Already have an account? Login" text is clicked
        binding.tvLogin.setOnClickListener {
            // finish() closes this Activity and returns to the previous one (LoginActivity)
            finish()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STATE OBSERVATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Observes the ViewModel's state and events using Kotlin Coroutines and Flow.
     *
     * KEY CONCEPTS:
     * - lifecycleScope: A coroutine scope that's cancelled when Activity is destroyed
     * - repeatOnLifecycle: Only collects when Activity is at least STARTED (visible)
     *   This prevents wasting resources when the app is in the background
     * - StateFlow.collect: Receives new values whenever the state changes
     *
     * WHY TWO SEPARATE COLLECTORS?
     * 1. 'state' = Continuous UI state (loading spinner, error message, form values)
     *    This can emit the same value multiple times and we always want to show it.
     *
     * 2. 'event' = One-time events (registration success, navigation)
     *    These should only be handled once (e.g., don't show the same Toast twice)
     */
    private fun observeState() {

        // ─────────────────────────────────────────────────────────────────────
        // OBSERVE UI STATE (loading, errors)
        // ─────────────────────────────────────────────────────────────────────

        lifecycleScope.launch {
            // Only collect when Activity is visible (STARTED or RESUMED)
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect emissions from the state StateFlow
                viewModel.state.collect { state ->

                    // Show/hide loading spinner based on isLoading flag
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    // Disable register button while loading (prevents double-submit)
                    binding.btnRegister.isEnabled = !state.isLoading

                    // Handle error message display
                    state.errorMessage?.let { error ->
                        // If there's an error message, show it
                        binding.tvError.text = error
                        binding.tvError.visibility = View.VISIBLE
                    } ?: run {
                        // If errorMessage is null, hide the error TextView
                        binding.tvError.visibility = View.GONE
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // OBSERVE ONE-TIME EVENTS (registration success, role mismatch)
        // ─────────────────────────────────────────────────────────────────────

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect one-time events from the ViewModel
                viewModel.event.collect { event ->
                    // Use 'when' to handle different event types
                    when (event) {
                        // Registration was successful
                        is AuthEvent.Registered -> {
                            // Show success message to user
                            Toast.makeText(
                                this@RegisterActivity,  // Context (this Activity)
                                "Registration successful! Please login.",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Close this Activity and go back to LoginActivity
                            finish()
                        }

                        // Role mismatch error (shouldn't happen in registration, but handled)
                        is AuthEvent.RoleMismatch -> {
                            Toast.makeText(
                                this@RegisterActivity,
                                "Invalid role",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        // Handle any other events (LoggedIn, LoggedOut, null)
                        else -> {
                            // No action needed for other events in this Activity
                        }
                    }
                }
            }
        }
    }
}

