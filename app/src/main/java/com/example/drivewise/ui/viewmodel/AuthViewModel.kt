/**
 * AuthViewModel.kt
 * ==================
 * This ViewModel handles all authentication logic (login, register, logout).
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. VIEWMODEL: A ViewModel survives configuration changes (like screen rotation).
 *    It holds UI state and business logic, keeping Activities/Fragments "dumb".
 *
 * 2. STATEFLOW: A reactive container that holds a value and notifies observers when it changes.
 *    - _state (private MutableStateFlow): Only ViewModel can update it
 *    - state (public StateFlow): Activities can only read/observe it
 *
 * 3. SEALED INTERFACE: AuthEvent is a sealed interface - it can only have a fixed set of
 *    implementations (LoggedIn, RoleMismatch, etc.). This enables exhaustive 'when' statements.
 *
 * 4. VIEWMODELSCOPE: A coroutine scope tied to the ViewModel's lifecycle.
 *    Coroutines launched here are automatically cancelled when ViewModel is cleared.
 *
 * 5. SEPARATION OF STATE vs EVENTS:
 *    - State: Continuous data (loading, errors, form values) - can be replayed
 *    - Events: One-time occurrences (navigation, toasts) - should be consumed once
 */
package com.example.drivewise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.drivewise.domain.model.Role
import com.example.drivewise.domain.model.User
import com.example.drivewise.domain.repository.AuthRepository
import com.example.drivewise.ui.state.AuthUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════════════
// AUTH EVENTS - One-time occurrences that UI should handle once
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Sealed interface representing one-time events from authentication.
 *
 * SEALED INTERFACE: Like an enum but each case can hold different data.
 * The compiler knows all possible subtypes, enabling exhaustive 'when' statements.
 *
 * WHY USE EVENTS?
 * - Toasts should show once, not every time UI recomposes
 * - Navigation should happen once, not on every config change
 * - Events are "consumed" after being handled
 */
sealed interface AuthEvent {
    /**
     * User successfully logged in.
     * @property user The authenticated user's profile
     */
    data class LoggedIn(val user: User) : AuthEvent

    /**
     * User logged in but selected wrong role (e.g., clicked "Admin" but is a client).
     */
    data object RoleMismatch : AuthEvent

    /**
     * User successfully logged out.
     */
    data object LoggedOut : AuthEvent

    /**
     * User successfully registered a new account.
     */
    data object Registered : AuthEvent
}

// ═══════════════════════════════════════════════════════════════════════════════
// AUTH VIEWMODEL
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * ViewModel for Login and Register screens.
 *
 * Manages:
 * - Form state (email, password, selected role)
 * - Loading state
 * - Error messages
 * - Authentication operations
 *
 * @param repository The AuthRepository implementation (injected via factory)
 */
class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    // ─────────────────────────────────────────────────────────────────────────
    // UI STATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Private mutable state - only this ViewModel can modify it.
     * Initialized with default values from AuthUiState.
     */
    private val _state = MutableStateFlow(AuthUiState())

    /**
     * Public read-only state for Activities to observe.
     * asStateFlow() creates an immutable view of _state.
     */
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // EVENTS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Private mutable event holder.
     * Null means no pending event.
     */
    private val _event = MutableStateFlow<AuthEvent?>(null)

    /**
     * Public read-only events for Activities to observe.
     */
    val event: StateFlow<AuthEvent?> = _event.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // FORM INPUT HANDLERS
    // These are called when user types in form fields
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updates email in state when user types.
     *
     * WHAT'S 'update'?
     * A helper that gets the current state, lets you modify it, and emits the new state.
     * Equivalent to: _state.value = _state.value.copy(email = value)
     */
    fun onEmailChanged(value: String) = _state.update { it.copy(email = value) }

    /**
     * Updates password in state when user types.
     */
    fun onPasswordChanged(value: String) = _state.update { it.copy(password = value) }

    /**
     * Updates confirm password in state when user types.
     * Only used in registration, not login.
     */
    fun onConfirmPasswordChanged(value: String) = _state.update { it.copy(confirmPassword = value) }

    /**
     * Updates selected role when user picks Admin or Client.
     */
    fun onRoleSelected(role: Role) = _state.update { it.copy(selectedRole = role) }

    // ─────────────────────────────────────────────────────────────────────────
    // REGISTER
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Attempts to register a new user.
     *
     * FLOW:
     * 1. Validate passwords match
     * 2. Show loading state
     * 3. Call repository.register()
     * 4. On success: emit Registered event, clear form
     * 5. On failure: show error message
     */
    fun register() {
        val current = _state.value

        // Validation: Check passwords match
        if (current.password != current.confirmPassword) {
            _state.update { it.copy(errorMessage = "Passwords do not match") }
            return
        }

        // Launch coroutine in viewModelScope (auto-cancelled if ViewModel dies)
        viewModelScope.launch {
            // Show loading, clear any previous error
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            // Call repository (suspend function - waits for Firebase)
            val result = repository.register(
                current.email.trim(),
                current.password.trim(),
                current.selectedRole
            )

            // Handle result using fold (like try/catch but for Result)
            result.fold(
                onSuccess = {
                    // Log out the newly created user (they need to login explicitly)
                    repository.logout()
                    // Emit success event
                    _event.value = AuthEvent.Registered
                    // Clear the form
                    clearForm()
                },
                onFailure = { throwable ->
                    // Show error message from exception
                    _state.update { it.copy(errorMessage = throwable.message) }
                }
            )

            // Hide loading indicator
            _state.update { it.copy(isLoading = false) }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Attempts to log in an existing user.
     *
     * FLOW:
     * 1. Show loading state
     * 2. Call repository.login()
     * 3. Check if user's actual role matches selected role
     * 4. If match: emit LoggedIn event
     * 5. If mismatch: logout and show error
     * 6. On failure: show error message
     */
    fun login() {
        val current = _state.value

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val result = repository.login(current.email.trim(), current.password.trim())

            result.fold(
                onSuccess = { user ->
                    // Check if selected role matches actual role
                    if (user.role == current.selectedRole.key) {
                        // Roles match - login successful!
                        _event.value = AuthEvent.LoggedIn(user)
                    } else {
                        // Role mismatch - user selected wrong role
                        repository.logout()  // Sign them out
                        _state.update {
                            it.copy(errorMessage = "This account is not a ${current.selectedRole.key}")
                        }
                        _event.value = AuthEvent.RoleMismatch
                    }
                },
                onFailure = { throwable ->
                    _state.update { it.copy(errorMessage = throwable.message) }
                }
            )

            _state.update { it.copy(isLoading = false) }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGOUT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Logs out the current user.
     */
    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _event.value = AuthEvent.LoggedOut
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITY METHODS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Clears all form fields.
     * Called after successful registration.
     */
    fun clearForm() {
        _state.update {
            it.copy(
                email = "",
                password = "",
                confirmPassword = "",
                isLoading = false,
                errorMessage = null
            )
        }
    }

    /**
     * Clears the error message.
     * Called when user starts typing again or dismisses error.
     */
    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    /**
     * Clears the current event.
     * Called after Activity has handled the event (shown toast, navigated, etc.)
     */
    fun clearEvent() {
        _event.value = null
    }
}
