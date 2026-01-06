/**
 * UserViewModel.kt
 * ==================
 * This ViewModel handles user profile operations, mainly for admin verification.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. OPTIMISTIC UI UPDATE: When verification changes, we immediately update
 *    the local list (state.users) without waiting for Firestore confirmation.
 *    This makes the UI feel faster and more responsive.
 *
 * 2. ADMIN-FOCUSED: This ViewModel is primarily used by admins to:
 *    - View list of all client users
 *    - Verify/unverify client documents
 *
 * USED BY: CustomerProfilesActivity, DocumentViewerActivity
 */
package com.example.drivewise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.drivewise.domain.model.User
import com.example.drivewise.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════════════
// USER UI STATE
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * UI state for user-related screens.
 *
 * @property users List of client users
 * @property isLoading Whether we're loading data
 * @property errorMessage Error to display, or null
 * @property selectedUser Currently selected user for detail view
 */
data class UserUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedUser: User? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// USER EVENTS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * One-time events from user operations.
 */
sealed interface UserEvent {
    /** User verification status was updated */
    data object UserVerified : UserEvent

    /** An error occurred */
    data class Error(val message: String) : UserEvent
}

// ═══════════════════════════════════════════════════════════════════════════════
// USER VIEWMODEL
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * ViewModel for user profile management (admin use).
 *
 * Provides:
 * - Real-time list of all client users
 * - Document verification toggle
 *
 * @param repository The UserRepository implementation
 */
class UserViewModel(
    private val repository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(UserUiState())
    val state: StateFlow<UserUiState> = _state.asStateFlow()

    private val _event = MutableStateFlow<UserEvent?>(null)
    val event: StateFlow<UserEvent?> = _event.asStateFlow()

    /**
     * Start observing clients when ViewModel is created.
     */
    init {
        observeClients()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OBSERVE ALL CLIENTS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Observes all client users with real-time updates.
     *
     * Only clients (not admins) are included.
     * Sorted by creation date (newest first).
     */
    private fun observeClients() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                repository.observeAllClients().collect { users ->
                    _state.update { it.copy(users = users, isLoading = false, errorMessage = null) }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load customers. Check Firestore permissions."
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOAD SINGLE USER
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Loads a single user's profile.
     *
     * Used by document viewer to show user details.
     *
     * @param userId The user's UID
     */
    fun loadUser(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.getUser(userId).fold(
                onSuccess = { user ->
                    _state.update { it.copy(selectedUser = user, isLoading = false) }
                },
                onFailure = { throwable ->
                    _state.update { it.copy(isLoading = false, errorMessage = throwable.message) }
                }
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE VERIFICATION STATUS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updates a user's document verification status.
     *
     * OPTIMISTIC UPDATE:
     * We immediately update the local list for responsive UI,
     * then confirm with Firestore. If Firestore fails, the real-time
     * listener will eventually correct the local state.
     *
     * @param userId The user's UID
     * @param isVerified Whether the user's documents are verified
     */
    fun updateVerification(userId: String, isVerified: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.updateUserVerification(userId, isVerified).fold(
                onSuccess = {
                    // OPTIMISTIC UPDATE: Update local list immediately
                    _state.update { currentState ->
                        val updatedUsers = currentState.users.map { user ->
                            if (user.uid == userId) {
                                // Create a copy with updated verification
                                user.copy(isVerified = isVerified)
                            } else {
                                // Keep other users unchanged
                                user
                            }
                        }
                        currentState.copy(users = updatedUsers, isLoading = false)
                    }
                    _event.value = UserEvent.UserVerified
                },
                onFailure = { throwable ->
                    _state.update { it.copy(isLoading = false, errorMessage = throwable.message) }
                    _event.value = UserEvent.Error(throwable.message ?: "Failed to update verification")
                }
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITY METHODS
    // ─────────────────────────────────────────────────────────────────────────

    fun clearEvent() {
        _event.value = null
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}

