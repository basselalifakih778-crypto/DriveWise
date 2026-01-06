/**
 * FirebaseUserRepository.kt
 * ==========================
 * This is the Firebase implementation of UserRepository.
 * It handles user profile operations, particularly for client verification.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. ADMIN-FOCUSED: This repository is mainly used by admins to:
 *    - View all client profiles
 *    - Verify/unverify client documents
 *
 * 2. DEFENSIVE TYPE HANDLING: Firestore can store data with inconsistent types
 *    (e.g., isVerified might be true, "true", 1, etc.).
 *    The toUserSafe() method handles this gracefully.
 *
 * 3. IN-MEMORY SORTING: Instead of using Firestore orderBy (which requires indexes),
 *    we fetch all clients and sort in Kotlin. This is fine for small datasets.
 *
 * FIRESTORE COLLECTION: "users" (same as AuthRepository)
 */
package com.example.drivewise.Data.remote

import com.example.drivewise.domain.model.User
import com.example.drivewise.domain.repository.UserRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Firestore implementation of UserRepository.
 *
 * Manages user profiles in the "users" collection.
 *
 * @param firestore The Firestore database instance
 */
class FirebaseUserRepository(
    private val firestore: FirebaseFirestore
) : UserRepository {

    /**
     * Reference to the "users" collection.
     */
    private val usersCollection get() = firestore.collection("users")

    // ═══════════════════════════════════════════════════════════════════════════
    // DEFENSIVE TYPE CONVERSION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Extension function to safely convert a Firestore document to a User.
     *
     * WHY THIS EXISTS:
     * Firestore's automatic type conversion can fail if data was saved with
     * inconsistent types. For example, isVerified might be stored as:
     * - Boolean: true
     * - String: "true"
     * - Number: 1
     *
     * This function handles all cases to prevent crashes.
     *
     * EXTENSION FUNCTION: Adds a new method to DocumentSnapshot class.
     * Called like: document.toUserSafe() instead of toUserSafe(document)
     *
     * @return User object with correctly typed isVerified, or null if conversion fails
     */
    private fun DocumentSnapshot.toUserSafe(): User? {
        // First, try standard conversion
        val base = toObject(User::class.java) ?: return null

        // Get the raw isVerified value (could be any type)
        val rawIsVerified = get("isVerified")

        // Coerce to Boolean based on actual type
        val coercedIsVerified = when (rawIsVerified) {
            is Boolean -> rawIsVerified                                           // Already boolean
            is Number -> rawIsVerified.toInt() != 0                              // 0 = false, else true
            is String -> rawIsVerified.equals("true", ignoreCase = true) || rawIsVerified == "1"
            else -> base.isVerified                                              // Fall back to default
        }

        // Return user with correct uid and isVerified
        return base.copy(
            uid = id,                    // Document ID is the user's UID
            isVerified = coercedIsVerified
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // OBSERVE ALL CLIENTS (REAL-TIME)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Observes all client users with real-time updates.
     *
     * Filters to only users with role="client".
     * Sorted in memory by createdAt (newest first).
     *
     * NOTE: We sort in Kotlin instead of Firestore to avoid needing a composite index.
     * This is fine for small datasets (<1000 users).
     *
     * @return Flow emitting list of client users whenever any client profile changes
     */
    override fun observeAllClients(): Flow<List<User>> = callbackFlow {
        // Query only for clients (not admins)
        val registration = usersCollection
            .whereEqualTo("role", "client")  // Filter: only clients
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                // Convert documents to Users using our safe conversion
                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.toUserSafe()
                }.orEmpty()
                    .sortedByDescending { it.createdAt }  // Sort in memory (newest first)

                // Emit the clients list
                trySend(users)
            }

        awaitClose { registration.remove() }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GET SINGLE USER
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Fetches a single user by ID.
     *
     * @param userId The user's Firebase Auth UID (also Firestore document ID)
     * @return Result containing the User, or null if not found
     */
    override suspend fun getUser(userId: String): Result<User?> = runCatching {
        val snapshot = usersCollection.document(userId).get().await()
        snapshot.toUserSafe()  // Use our safe conversion
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE USER VERIFICATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Updates a user's verification status.
     *
     * Called by admin when they verify (or un-verify) a client's documents.
     * Only updates the isVerified field.
     *
     * @param userId The user's Firebase Auth UID
     * @param isVerified Whether the user's documents are verified
     * @return Result.success if updated successfully
     */
    override suspend fun updateUserVerification(userId: String, isVerified: Boolean): Result<Unit> = runCatching {
        // update() only changes the specified field
        usersCollection.document(userId).update("isVerified", isVerified).await()
    }
}
