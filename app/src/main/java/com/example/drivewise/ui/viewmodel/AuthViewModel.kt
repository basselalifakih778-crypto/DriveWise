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

sealed interface AuthEvent {
    data class LoggedIn(val user: User) : AuthEvent
    data object RoleMismatch : AuthEvent
    data object LoggedOut : AuthEvent
    data object Registered : AuthEvent
}

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _event = MutableStateFlow<AuthEvent?>(null)
    val event: StateFlow<AuthEvent?> = _event.asStateFlow()

    fun onEmailChanged(value: String) = _state.update { it.copy(email = value) }
    fun onPasswordChanged(value: String) = _state.update { it.copy(password = value) }
    fun onConfirmPasswordChanged(value: String) = _state.update { it.copy(confirmPassword = value) }
    fun onRoleSelected(role: Role) = _state.update { it.copy(selectedRole = role) }

    fun register() {
        val current = _state.value
        if (current.password != current.confirmPassword) {
            _state.update { it.copy(errorMessage = "Passwords do not match") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.register(current.email.trim(), current.password.trim(), current.selectedRole)
            result.fold(
                onSuccess = {
                    repository.logout()
                    _event.value = AuthEvent.Registered
                    clearForm()
                },
                onFailure = { throwable ->
                    _state.update { it.copy(errorMessage = throwable.message) }
                }
            )
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun login() {
        val current = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.login(current.email.trim(), current.password.trim())
            result.fold(
                onSuccess = { user ->
                    if (user.role == current.selectedRole.key) {
                        _event.value = AuthEvent.LoggedIn(user)
                    } else {
                        repository.logout()
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

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _event.value = AuthEvent.LoggedOut
        }
    }

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

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun clearEvent() {
        _event.value = null
    }
}
