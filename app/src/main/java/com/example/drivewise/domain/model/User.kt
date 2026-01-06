/**
 * User.kt
 * ========
 * This file defines the User data model - the blueprint for user data in DriveWise.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. DATA CLASS: A special Kotlin class that automatically generates:
 *    - equals() / hashCode() - for comparing users
 *    - toString() - for debugging ("User(uid=..., email=...)")
 *    - copy() - for creating modified copies
 *    - componentN() - for destructuring
 *
 * 2. FIRESTORE MAPPING: This class is designed to be stored in/loaded from Firebase Firestore.
 *    Firestore can automatically convert documents to/from this class using toObject().
 *    That's why we need default values for all properties (Firestore requires a no-arg constructor).
 *
 * 3. ROLE SYSTEM: Users can be either "admin" or "client".
 *    - Admin: Can manage cars, approve bookings, view customer profiles
 *    - Client: Can browse cars, make bookings, upload documents
 *
 * FIRESTORE COLLECTION: "users"
 * Each document ID = the user's Firebase Auth UID
 */
package com.example.drivewise.domain.model

/**
 * Represents a user in the DriveWise system.
 *
 * This class is stored in the Firestore "users" collection.
 * Both admins and clients use this same model.
 *
 * @property uid The unique identifier from Firebase Auth (document ID in Firestore)
 * @property email The user's email address (used for login)
 * @property role Either "admin" or "client" - stored as String for Firestore compatibility
 * @property fullName The user's display name
 * @property phone The user's phone number (optional)
 * @property address The user's address (optional, used for clients)
 * @property licenseNumber Driver's license number (required for clients to rent cars)
 * @property licenseImageUrl URL to uploaded license image in Firebase Storage
 * @property idDocumentUrl URL to uploaded ID document image in Firebase Storage
 * @property profileImageUrl URL to user's profile picture in Firebase Storage
 * @property createdAt Timestamp when the account was created (milliseconds since epoch)
 * @property isVerified Whether admin has verified this user's documents (clients only)
 */
data class User(
    val uid: String = "",                    // Firebase Auth user ID
    val email: String = "",                  // Login email
    val role: String = "client",             // "admin" or "client" - see Role enum
    val fullName: String = "",               // Display name
    val phone: String = "",                  // Contact phone
    val address: String = "",                // User's address
    val licenseNumber: String = "",          // Driver's license number
    val licenseImageUrl: String = "",        // Firebase Storage URL for license photo
    val idDocumentUrl: String = "",          // Firebase Storage URL for ID document
    val profileImageUrl: String = "",        // Firebase Storage URL for profile picture
    val createdAt: Long = System.currentTimeMillis(),  // Account creation timestamp
    val isVerified: Boolean = false          // Has admin verified this user's documents?
) {
    /**
     * Converts the role string to a Role enum for type-safe comparisons.
     *
     * WHY USE BOTH STRING AND ENUM?
     * - String: Easy to store in Firestore (no serialization issues)
     * - Enum: Type-safe in code (compiler catches typos like "clinet")
     *
     * @return Role.ADMIN or Role.CLIENT based on the role string
     */
    fun getRoleEnum(): Role = Role.fromKey(role)
}
