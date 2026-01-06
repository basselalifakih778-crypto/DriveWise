package com.example.drivewise.Data.remote

import com.example.drivewise.domain.model.User
import com.example.drivewise.domain.repository.UserRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseUserRepository(
    private val firestore: FirebaseFirestore
) : UserRepository {

    private val usersCollection get() = firestore.collection("users")

    private fun DocumentSnapshot.toUserSafe(): User? {
        val base = toObject(User::class.java) ?: return null

        // Firestore sometimes ends up with inconsistent types (Boolean vs String/Number).
        // Coerce isVerified defensively so UI isn't wrong.
        val rawIsVerified = get("isVerified")
        val coercedIsVerified = when (rawIsVerified) {
            is Boolean -> rawIsVerified
            is Number -> rawIsVerified.toInt() != 0
            is String -> rawIsVerified.equals("true", ignoreCase = true) || rawIsVerified == "1"
            else -> base.isVerified
        }

        return base.copy(
            uid = id,
            isVerified = coercedIsVerified
        )
    }

    override fun observeAllClients(): Flow<List<User>> = callbackFlow {
        // Use simple query without ordering to avoid requiring an index
        // We'll sort in memory instead
        val registration = usersCollection
            .whereEqualTo("role", "client")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.toUserSafe()
                }.orEmpty()
                    .sortedByDescending { it.createdAt } // Sort in memory
                trySend(users)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun getUser(userId: String): Result<User?> = runCatching {
        val snapshot = usersCollection.document(userId).get().await()
        snapshot.toUserSafe()
    }

    override suspend fun updateUserVerification(userId: String, isVerified: Boolean): Result<Unit> = runCatching {
        usersCollection.document(userId).update("isVerified", isVerified).await()
    }
}
