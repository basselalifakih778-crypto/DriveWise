/**
 * Role.kt
 * ========
 * This file defines the Role enum - the possible user types in DriveWise.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. ENUM CLASS: An enum (enumeration) is a special class that represents a fixed set of constants.
 *    Unlike strings, enums are type-safe - the compiler won't let you use "admni" by mistake.
 *
 * 2. ENUM WITH PROPERTY: Each enum value has a 'key' property (the string stored in Firestore).
 *    Example: Role.ADMIN.key returns "admin"
 *
 * 3. COMPANION OBJECT: Like a static class in Java. It belongs to the enum class itself,
 *    not to individual enum values. Used here to provide a factory method (fromKey).
 *
 * WHY BOTH ENUM AND STRING?
 * - Firestore stores "admin" or "client" as strings
 * - Code uses Role.ADMIN or Role.CLIENT for type safety
 * - This enum bridges the two representations
 */
package com.example.drivewise.domain.model

/**
 * Represents the two types of users in the DriveWise system.
 *
 * @property key The string representation stored in Firestore
 *
 * ADMIN users can:
 * - Manage the car fleet (add, edit, delete cars)
 * - Approve or reject booking requests
 * - View and verify customer profiles/documents
 * - Create promotional posts
 *
 * CLIENT users can:
 * - Browse available cars
 * - Make booking requests
 * - Upload profile documents (license, ID)
 * - Upload car condition photos (pickup/return)
 */
enum class Role(val key: String) {
    ADMIN("admin"),   // Administrator role
    CLIENT("client"); // Customer/client role

    /**
     * Companion object contains static-like methods for the Role enum.
     *
     * In Kotlin, companion objects are like static methods in Java.
     * You call them on the class: Role.fromKey("admin")
     */
    companion object {
        /**
         * Converts a string key to a Role enum value.
         *
         * This is useful when reading from Firestore, which stores roles as strings.
         *
         * @param key The string role value (e.g., "admin", "client")
         * @return The matching Role enum, or CLIENT as the default if not found
         *
         * Example usage:
         *   val role = Role.fromKey("admin")  // Returns Role.ADMIN
         *   val role = Role.fromKey("xyz")    // Returns Role.CLIENT (default)
         *   val role = Role.fromKey(null)     // Returns Role.CLIENT (default)
         */
        fun fromKey(key: String?): Role = values().firstOrNull { it.key == key } ?: CLIENT
    }
}
