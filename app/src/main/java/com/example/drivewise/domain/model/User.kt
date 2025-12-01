package com.example.drivewise.domain.model

data class User(
    val uid: String = "",
    val email: String = "",
    val role: String = "client" // Store as string for Firestore: "admin" or "client"
) {
    // Helper to get Role enum
    fun getRoleEnum(): Role = Role.fromKey(role)
}
