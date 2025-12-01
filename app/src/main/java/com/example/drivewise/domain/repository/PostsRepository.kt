package com.example.drivewise.domain.repository

import com.example.drivewise.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface PostsRepository {
    fun observePosts(): Flow<List<Post>>
    suspend fun createPost(post: Post): Result<Unit>
    suspend fun getPost(postId: String): Result<Post?>
}

