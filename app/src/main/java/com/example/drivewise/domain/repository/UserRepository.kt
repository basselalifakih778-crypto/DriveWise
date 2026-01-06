package com.example.drivewise.domain.repository

import com.example.drivewise.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeAllClients(): Flow<List<User>>
    suspend fun getUser(userId: String): Result<User?>
    suspend fun updateUserVerification(userId: String, isVerified: Boolean): Result<Unit>
}

