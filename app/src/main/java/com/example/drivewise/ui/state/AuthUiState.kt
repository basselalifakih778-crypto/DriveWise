/**
 * AuthUiState.kt
 * ================
 * This file defines the UI state for authentication screens (Login and Register).
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. UI STATE: A single object that contains ALL the data needed to render a screen.
 *    Instead of having separate variables for email, password, loading, error,
 *    we bundle them together in one data class.
 *
 * 2. SINGLE SOURCE OF TRUTH: The ViewModel holds ONE AuthUiState.
 *    When anything changes (user types, loading starts), we create a NEW state
 *    with the updated value using copy().
 *
 * 3. IMMUTABILITY: Data classes with 'val' properties can't be modified.
 *    This prevents bugs where state changes unexpectedly.
 *
 * EXAMPLE:
 *   Old state: AuthUiState(email="", password="", isLoading=false)
 *   User types "a" in email field
 *   New state: AuthUiState(email="a", password="", isLoading=false)
 */
package com.example.drivewise.ui.state

import com.example.drivewise.domain.model.Role

/**
 * Data class representing the current state of Login/Register screens.
 *
 * @property email The current value in the email text field
 * @property password The current value in the password text field
 * @property confirmPassword The current value in confirm password field (Register only)
 * @property selectedRole Which role radio button is selected (ADMIN or CLIENT)
 * @property isLoading Whether an auth operation is in progress (show spinner)
 * @property errorMessage Error message to display, or null if no error
 */
data class AuthUiState(
    val email: String = "",              // Email input field value
    val password: String = "",           // Password input field value
    val confirmPassword: String = "",    // Confirm password (Register screen only)
    val selectedRole: Role = Role.CLIENT, // Selected role (default: CLIENT)
    val isLoading: Boolean = false,      // Show loading spinner?
    val errorMessage: String? = null     // Error to display, null = no error
)

