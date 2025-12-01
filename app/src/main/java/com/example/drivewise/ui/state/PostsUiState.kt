package com.example.drivewise.ui.state

import com.example.drivewise.domain.model.Post

sealed interface PostsUiState {
    data object Loading : PostsUiState
    data class Error(val message: String) : PostsUiState
    data class Success(val posts: List<Post>) : PostsUiState
}

