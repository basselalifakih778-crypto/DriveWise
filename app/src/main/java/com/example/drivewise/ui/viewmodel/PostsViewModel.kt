/**
 * PostsViewModel.kt
 * ==================
 * This ViewModel handles posts display and creation.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. STATEIN: Converts a cold Flow to a hot StateFlow. This means:
 *    - Cold Flow: Only runs when collected (lazy)
 *    - StateFlow: Always has a current value, runs even without collectors
 *
 * 2. MAP TRANSFORMATION: We transform List<Post> to PostsUiState.
 *    This wraps the data in a type that also represents Loading/Error states.
 *
 * 3. SEALED CLASS FOR UI STATE: PostsUiState can be Loading, Success, or Error.
 *    The UI uses 'when' to handle each case differently.
 *
 * USED BY: PostsListActivity, CreatePostActivity
 */
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

/**
 * ViewModel for posts listing and creation.
 *
 * Manages two separate states:
 * - postsState: The list of posts (for PostsListActivity)
 * - formState: The post creation form (for CreatePostActivity)
 *
 * @param repository The PostsRepository implementation
 */
class PostsViewModel(
    private val repository: PostsRepository
) : ViewModel() {

    // ─────────────────────────────────────────────────────────────────────────
    // POSTS LIST STATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * StateFlow of posts UI state.
     *
     * HOW THIS WORKS:
     * 1. repository.observePosts() returns Flow<List<Post>>
     * 2. .map { } transforms each List<Post> to PostsUiState.Success
     * 3. .stateIn() converts the Flow to a StateFlow
     *
     * STATEIN PARAMETERS:
     * - scope: viewModelScope (auto-cancels when ViewModel is cleared)
     * - started: WhileSubscribed(5000) - keeps running 5 seconds after last collector
     *   (prevents restart on quick config change like rotation)
     * - initialValue: PostsUiState.Loading - shown before first data arrives
     *
     * WHY THIS PATTERN?
     * It's a concise way to expose repository data as UI state.
     * Errors from the Flow would need try-catch, but this simple version
     * just wraps successful data.
     */
    val postsState: StateFlow<PostsUiState> = repository.observePosts()
        .map<List<Post>, PostsUiState> { posts ->
            // Transform list of posts to Success state
            PostsUiState.Success(posts)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),  // 5s timeout
            initialValue = PostsUiState.Loading
        )

    // ─────────────────────────────────────────────────────────────────────────
    // POST FORM STATE (for CreatePostActivity)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * State for the post creation form.
     * Tracks title, description, image URL, loading, and errors.
     */
    private val _formState = MutableStateFlow(PostFormUiState())
    val formState: StateFlow<PostFormUiState> = _formState.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // FORM INPUT HANDLERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updates title when user types.
     */
    fun onTitleChanged(value: String) = _formState.update { it.copy(title = value) }

    /**
     * Updates description when user types.
     */
    fun onDescriptionChanged(value: String) = _formState.update { it.copy(description = value) }

    /**
     * Updates image URL when user types.
     */
    fun onImageUrlChanged(value: String) = _formState.update { it.copy(imageUrl = value) }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE POST
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new post from the form data.
     *
     * FLOW:
     * 1. Validate title and description are not empty
     * 2. Show loading state
     * 3. Create Post object with form data
     * 4. Call repository.createPost()
     * 5. On success: reset form (empty PostFormUiState)
     * 6. On failure: show error message
     *
     * @param adminId The UID of the admin creating the post
     */
    fun createPost(adminId: String) {
        val current = _formState.value

        // Validation
        if (current.title.isBlank() || current.description.isBlank()) {
            _formState.update { it.copy(errorMessage = "Title and description are required") }
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isSubmitting = true, errorMessage = null) }

            // Create Post object
            val post = Post(
                title = current.title.trim(),
                description = current.description.trim(),
                imageUrl = current.imageUrl.ifBlank { null },  // null if empty
                createdBy = adminId
            )

            // Call repository
            val result = repository.createPost(post)

            // Update state based on result
            _formState.update {
                if (result.isSuccess) {
                    // Success: Reset form to initial empty state
                    PostFormUiState()
                } else {
                    // Failure: Show error, stop loading
                    it.copy(isSubmitting = false, errorMessage = result.exceptionOrNull()?.message)
                }
            }
        }
    }
}

