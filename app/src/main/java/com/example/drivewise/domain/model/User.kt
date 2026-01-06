package com.example.drivewise.domain.model

data class User(
    val uid: String = "",
    val email: String = "",
    val role: String = "client", // Store as string for Firestore: "admin" or "client"
    val fullName: String = "",
    val phone: String = "",
    val address: String = "",
    val licenseNumber: String = "",
    val licenseImageUrl: String = "",
    val idDocumentUrl: String = "",
    val profileImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isVerified: Boolean = false
) {
    // Helper to get Role enum
    fun getRoleEnum(): Role = Role.fromKey(role)
}
