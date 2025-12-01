package com.example.drivewise.domain.repository

import com.example.drivewise.domain.model.Role
import com.example.drivewise.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun register(email: String, password: String, role: Role): Result<Unit>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun logout()
    suspend fun getUserRole(uid: String): Result<Role>
}

