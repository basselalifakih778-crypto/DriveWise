/**
 * FirebaseAuthRepository.kt
 * ==========================
 * This is the Firebase implementation of AuthRepository.
 * It handles all authentication and user creation using Firebase services.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. IMPLEMENTATION: This class implements the AuthRepository interface.
 *    It provides the actual Firebase logic for each method defined in the interface.
 *
 * 2. FIREBASE AUTH vs FIRESTORE:
 *    - Firebase Auth: Handles email/password authentication (login, register)
 *    - Firestore: Database where we store user profiles with extra data (role, name, etc.)
 *    We use BOTH because Auth only stores email/password, not custom user data.
 *
 * 3. AUTH STATE LISTENER: Firebase can notify us when auth state changes
 *    (user logs in, logs out, session expires). We use this to update currentUser.
 *
 * 4. AWAIT EXTENSION: .await() converts Firebase's Task/ListenableFuture to a
 *    Kotlin coroutine. This lets us use async Firebase calls in suspend functions.
 *
 * 5. RUNCATCHING: A Kotlin helper that wraps code in try/catch and returns Result.
 *    If the code succeeds → Result.success(value)
 *    If an exception is thrown → Result.failure(exception)
 */
package com.example.drivewise.Data.remote

// Firebase imports
import com.example.drivewise.domain.model.Role
import com.example.drivewise.domain.model.User
import com.example.drivewise.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Kotlin coroutines and Flow imports
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await  // Converts Firebase Task to suspend function

/**
 * Firebase implementation of AuthRepository.
 *
 * Combines Firebase Auth (for authentication) with Firestore (for user profiles).
 *
 * @param auth Firebase Auth instance for login/register/logout
 * @param firestore Firestore instance for storing/retrieving user profiles
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    // ═══════════════════════════════════════════════════════════════════════════
    // CURRENT USER STATE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * MutableStateFlow holding the current user.
     * Private because only this class should update it.
     *
     * WHY STATEFLOW?
     * - It's a "hot" flow that always has a current value
     * - New collectors immediately get the current value
     * - Perfect for representing current state (like "who is logged in?")
     */
    private val _currentUser = MutableStateFlow<User?>(null)

    /**
     * Public read-only version of current user.
     * asStateFlow() converts MutableStateFlow to immutable StateFlow.
     * ViewModels can collect this but can't modify it directly.
     */
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION - AUTH STATE LISTENER
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Init block runs when this class is instantiated.
     * We set up a listener that fires whenever auth state changes.
     */
    init {
        // Add a listener that fires when:
        // - User logs in
        // - User logs out
        // - App starts and Firebase checks existing session
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser

            if (firebaseUser == null) {
                // No user is logged in
                _currentUser.value = null
            } else {
                // User is logged in - fetch their profile from Firestore
                // Using addSnapshotListener for real-time updates to profile
                firestore.collection(USER_COLLECTION)
                    .document(firebaseUser.uid)
                    .addSnapshotListener { snapshot, _ ->
                        // Convert Firestore document to User object
                        _currentUser.value = snapshot?.toObject(User::class.java)
                    }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REGISTER
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Registers a new user with email, password, and role.
     *
     * STEPS:
     * 1. Create Firebase Auth account (email + password)
     * 2. Get the auto-generated UID
     * 3. Create a User document in Firestore with the role and other data
     *
     * @param email User's email
     * @param password User's password
     * @param role ADMIN or CLIENT
     * @return Result.success if everything worked, Result.failure if any step failed
     */
    override suspend fun register(email: String, password: String, role: Role): Result<Unit> = runCatching {
        // Step 1: Create Firebase Auth account
        // .await() suspends until the operation completes
        val result = auth.createUserWithEmailAndPassword(email, password).await()

        // Step 2: Get the user ID
        val uid = result.user?.uid ?: throw IllegalStateException("Missing uid")

        // Step 3: Create User object with role
        val user = User(uid = uid, email = email, role = role.key)

        // Step 4: Save user profile to Firestore
        firestore.collection(USER_COLLECTION).document(uid).set(user).await()

        // If we reach here, registration succeeded (Unit means "no meaningful return value")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LOGIN
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Logs in an existing user and returns their profile.
     *
     * STEPS:
     * 1. Authenticate with Firebase Auth
     * 2. Fetch user profile from Firestore
     * 3. Return the User object
     *
     * @param email User's email
     * @param password User's password
     * @return Result containing the User if successful
     */
    override suspend fun login(email: String, password: String): Result<User> = runCatching {
        // Step 1: Sign in with Firebase Auth
        val result = auth.signInWithEmailAndPassword(email, password).await()

        // Step 2: Get the user ID
        val uid = result.user?.uid ?: throw IllegalStateException("Missing uid")

        // Step 3: Fetch user profile from Firestore
        val snapshot = firestore.collection(USER_COLLECTION).document(uid).get().await()

        // Step 4: Convert document to User object
        // toObject() uses reflection to map Firestore fields to User properties
        snapshot.toObject(User::class.java) ?: throw IllegalStateException("User profile missing")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GET USER ROLE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Fetches just the role of a specific user.
     *
     * Used to verify that a user logging in as "admin" actually has admin role.
     *
     * @param uid The user's Firebase Auth UID
     * @return Result containing their Role
     */
    override suspend fun getUserRole(uid: String): Result<Role> = runCatching {
        // Fetch the user document
        val snapshot = firestore.collection(USER_COLLECTION).document(uid).get().await()

        // Get the "role" field and convert to Role enum
        // getString() returns null if field doesn't exist
        Role.fromKey(snapshot.getString("role"))
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LOGOUT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Logs out the current user.
     *
     * Firebase Auth will trigger the AuthStateListener, which will set _currentUser to null.
     * We also explicitly set it to null for immediate UI update.
     */
    override suspend fun logout() {
        auth.signOut()          // Tell Firebase to sign out
        _currentUser.value = null  // Immediately update local state
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Companion object holds constants that belong to the class, not instances.
     * Like "static final" in Java.
     */
    companion object {
        // The Firestore collection name where user profiles are stored
        private const val USER_COLLECTION = "users"
    }
}
