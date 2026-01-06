/**
 * PostsUiState.kt
 * =================
 * This file defines the UI state for the posts list screen.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. SEALED INTERFACE: Like an enum but each case can hold different data.
 *    The three states are mutually exclusive - posts are either:
 *    - Loading (no data yet)
 *    - Error (something went wrong)
 *    - Success (we have posts to display)
 *
 * 2. EXHAUSTIVE WHEN: Because it's sealed, the compiler knows all possible cases.
 *    If you use 'when' on PostsUiState, the compiler ensures you handle all cases.
 *
 * 3. DATA OBJECT vs DATA CLASS:
 *    - data object Loading: No data needed, just a marker
 *    - data class Error: Holds the error message
 *    - data class Success: Holds the list of posts
 *
 * USAGE IN UI:
 *   when (state) {
 *       is PostsUiState.Loading -> showSpinner()
 *       is PostsUiState.Error -> showError(state.message)
 *       is PostsUiState.Success -> showPosts(state.posts)
 *   }
 */
package com.example.drivewise.ui.state

import com.example.drivewise.domain.model.Post

/**
 * Sealed interface representing the three possible states of the posts list.
 *
 * WHY SEALED?
 * - All implementations must be defined in this file
 * - Compiler knows all cases, enabling exhaustive 'when' checks
 * - Prevents adding new states without handling them everywhere
 */
sealed interface PostsUiState {

    /**
     * Posts are currently being loaded from the server.
     * UI should show a loading spinner.
     */
    data object Loading : PostsUiState

    /**
     * An error occurred while loading posts.
     * UI should show the error message.
     *
     * @property message The error message to display
     */
    data class Error(val message: String) : PostsUiState

    /**
     * Posts loaded successfully.
     * UI should display the list (or empty state if list is empty).
     *
     * @property posts The list of posts to display
     */
    data class Success(val posts: List<Post>) : PostsUiState
}

