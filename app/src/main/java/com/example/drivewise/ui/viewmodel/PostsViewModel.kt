package com.example.drivewise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.drivewise.domain.model.Post
import com.example.drivewise.domain.repository.PostsRepository
import com.example.drivewise.ui.state.PostFormUiState
import com.example.drivewise.ui.state.PostsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PostsViewModel(
    private val repository: PostsRepository
) : ViewModel() {

    val postsState: StateFlow<PostsUiState> = repository.observePosts()
        .map<List<Post>, PostsUiState> { posts ->
            PostsUiState.Success(posts)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PostsUiState.Loading
        )

    private val _formState = MutableStateFlow(PostFormUiState())
    val formState: StateFlow<PostFormUiState> = _formState.asStateFlow()

    fun onTitleChanged(value: String) = _formState.update { it.copy(title = value) }
    fun onDescriptionChanged(value: String) = _formState.update { it.copy(description = value) }
    fun onImageUrlChanged(value: String) = _formState.update { it.copy(imageUrl = value) }

    fun createPost(adminId: String) {
        val current = _formState.value
        if (current.title.isBlank() || current.description.isBlank()) {
            _formState.update { it.copy(errorMessage = "Title and description are required") }
            return
        }
        viewModelScope.launch {
            _formState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val post = Post(
                title = current.title.trim(),
                description = current.description.trim(),
                imageUrl = current.imageUrl.ifBlank { null },
                createdBy = adminId
            )
            val result = repository.createPost(post)
            _formState.update {
                if (result.isSuccess) PostFormUiState() else it.copy(isSubmitting = false, errorMessage = result.exceptionOrNull()?.message)
            }
        }
    }
}

