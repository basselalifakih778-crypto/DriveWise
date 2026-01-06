/**
 * AuthRepository.kt
 * ==================
 * This file defines the AuthRepository interface - the contract for authentication operations.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. INTERFACE: An interface defines WHAT operations are available, but not HOW they work.
 *    The actual implementation is in FirebaseAuthRepository.
 *    This separation allows us to:
 *    - Swap Firebase for another auth provider without changing ViewModels
 *    - Create fake implementations for testing
 *
 * 2. REPOSITORY PATTERN: Repositories abstract away data sources.
 *    The ViewModel doesn't know or care if data comes from Firebase, a REST API, or local DB.
 *
 * 3. SUSPEND FUNCTIONS: The 'suspend' keyword means these functions are asynchronous.
 *    They can pause execution (e.g., waiting for network) without blocking the main thread.
 *
 * 4. RESULT TYPE: Kotlin's Result<T> wraps either a success value or an exception.
 *    This lets us handle errors gracefully: result.fold(onSuccess = {}, onFailure = {})
 *
 * 5. FLOW: A stream of values over time. currentUser emits whenever the auth state changes.
 */
package com.example.drivewise.domain.repository

import com.example.drivewise.domain.model.Role
import com.example.drivewise.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Contract for authentication and user session management.
 *
 * This interface is implemented by FirebaseAuthRepository.
 * ViewModels depend on this interface, not the concrete implementation.
 */
interface AuthRepository {

    /**
     * A Flow that emits the currently logged-in user, or null if not logged in.
     *
     * WHY FLOW?
     * - Auth state can change at any time (user logs out, session expires)
     * - Flow automatically notifies observers of changes
     * - UI can react immediately to auth state changes
     */
    val currentUser: Flow<User?>

    /**
     * Registers a new user with email, password, and role.
     *
     * This function:
     * 1. Creates a Firebase Auth account
     * 2. Creates a user document in Firestore "users" collection
     *
     * @param email User's email address
     * @param password User's chosen password
     * @param role Either Role.ADMIN or Role.CLIENT
     * @return Result.success(Unit) if successful, Result.failure(exception) if not
     */
    suspend fun register(email: String, password: String, role: Role): Result<Unit>

    /**
     * Logs in an existing user with email and password.
     *
     * This function:
     * 1. Authenticates with Firebase Auth
     * 2. Fetches the user profile from Firestore
     *
     * @param email User's email address
     * @param password User's password
     * @return Result containing the User object if successful
     */
    suspend fun login(email: String, password: String): Result<User>

    /**
     * Logs out the current user.
     *
     * Signs out from Firebase Auth and clears the currentUser Flow.
     */
    suspend fun logout()

    /**
     * Fetches the role of a specific user.
     *
     * Used to verify a user's role matches what they selected at login.
     *
     * @param uid The user's Firebase Auth UID
     * @return Result containing the user's Role
     */
    suspend fun getUserRole(uid: String): Result<Role>
}

