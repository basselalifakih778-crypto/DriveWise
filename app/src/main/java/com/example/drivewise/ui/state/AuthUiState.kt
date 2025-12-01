package com.example.drivewise.ui.state

import com.example.drivewise.domain.model.Role

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val selectedRole: Role = Role.CLIENT,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

