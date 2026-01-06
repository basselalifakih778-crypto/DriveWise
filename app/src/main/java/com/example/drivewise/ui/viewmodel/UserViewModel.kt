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

data class UserUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedUser: User? = null
)

sealed interface UserEvent {
    data object UserVerified : UserEvent
    data class Error(val message: String) : UserEvent
}

class UserViewModel(
    private val repository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(UserUiState())
    val state: StateFlow<UserUiState> = _state.asStateFlow()

    private val _event = MutableStateFlow<UserEvent?>(null)
    val event: StateFlow<UserEvent?> = _event.asStateFlow()

    init {
        observeClients()
    }

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

    fun updateVerification(userId: String, isVerified: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.updateUserVerification(userId, isVerified).fold(
                onSuccess = {
                    // Update the local state immediately for UI responsiveness
                    _state.update { currentState ->
                        val updatedUsers = currentState.users.map { user ->
                            if (user.uid == userId) {
                                user.copy(isVerified = isVerified)
                            } else {
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

    fun clearEvent() {
        _event.value = null
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}

