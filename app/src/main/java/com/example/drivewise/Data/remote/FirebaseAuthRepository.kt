package com.example.drivewise.Data.remote

import com.example.drivewise.domain.model.Role
import com.example.drivewise.domain.model.User
import com.example.drivewise.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                _currentUser.value = null
            } else {
                firestore.collection(USER_COLLECTION)
                    .document(firebaseUser.uid)
                    .addSnapshotListener { snapshot, _ ->
                        _currentUser.value = snapshot?.toObject(User::class.java)
                    }
            }
        }
    }

    override suspend fun register(email: String, password: String, role: Role): Result<Unit> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: throw IllegalStateException("Missing uid")
        val user = User(uid = uid, email = email, role = role.key)
        firestore.collection(USER_COLLECTION).document(uid).set(user).await()
    }

    override suspend fun login(email: String, password: String): Result<User> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: throw IllegalStateException("Missing uid")
        val snapshot = firestore.collection(USER_COLLECTION).document(uid).get().await()
        snapshot.toObject(User::class.java) ?: throw IllegalStateException("User profile missing")
    }

    override suspend fun getUserRole(uid: String): Result<Role> = runCatching {
        val snapshot = firestore.collection(USER_COLLECTION).document(uid).get().await()
        Role.fromKey(snapshot.getString("role"))
    }

    override suspend fun logout() {
        auth.signOut()
        _currentUser.value = null
    }

    companion object {
        private const val USER_COLLECTION = "users"
    }
}
