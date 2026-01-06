/**
 * UserRepository.kt
 * ==================
 * This file defines the UserRepository interface - the contract for user profile operations.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. SEPARATION FROM AUTH: AuthRepository handles login/register.
 *    UserRepository handles user profiles and verification.
 *
 * 2. CLIENT VERIFICATION: Admins verify clients by checking their uploaded
 *    documents (license, ID). This repository supports that workflow.
 *
 * 3. WHY ONLY CLIENTS? observeAllClients() filters to role="client" because
 *    admins typically only need to manage client profiles, not other admins.
 */
package com.example.drivewise.domain.repository

import com.example.drivewise.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Contract for user profile management operations.
 *
 * Implemented by FirebaseUserRepository.
 * Used by UserViewModel (and CustomerViewModel) to manage user profiles.
 */
interface UserRepository {

    /**
     * Observes all client users in real-time.
     *
     * Used by admins on the Customer Profiles screen.
     * Filters to only users with role="client".
     * Sorted by createdAt (newest first).
     *
     * @return Flow emitting list of client users whenever any changes
     */
    fun observeAllClients(): Flow<List<User>>

    /**
     * Fetches a single user by ID.
     *
     * Used to load full user profile details.
     *
     * @param userId The user's Firebase Auth UID (also Firestore document ID)
     * @return Result containing the User, or null if not found
     */
    suspend fun getUser(userId: String): Result<User?>

    /**
     * Updates a user's verification status.
     *
     * Called by admin when they verify or un-verify a client's documents.
     * Only updates the isVerified field, not the whole document.
     *
     * @param userId The user's Firebase Auth UID
     * @param isVerified Whether the user's documents are verified
     * @return Result.success if updated successfully
     */
    suspend fun updateUserVerification(userId: String, isVerified: Boolean): Result<Unit>
}

