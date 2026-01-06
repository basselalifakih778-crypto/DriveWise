/**
 * PostFormUiState.kt
 * ====================
 * This file defines the UI state for the "Create Post" form.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. FORM STATE: This state holds the current values of all form fields,
 *    plus submission status and errors.
 *
 * 2. RESET ON SUCCESS: When a post is created successfully, the ViewModel
 *    returns a fresh PostFormUiState() with all default values (empty fields).
 *    This clears the form.
 *
 * USED BY: CreatePostActivity, PostsViewModel
 */
package com.example.drivewise.ui.state

/**
 * Data class representing the state of the post creation form.
 *
 * @property title Current value of the title input field
 * @property description Current value of the description input field
 * @property imageUrl Current value of the image URL input field
 * @property isSubmitting Whether the form is currently being submitted
 * @property errorMessage Error message to display, or null if no error
 */
data class PostFormUiState(
    val title: String = "",           // Title field value
    val description: String = "",     // Description field value
    val imageUrl: String = "",        // Image URL field value
    val isSubmitting: Boolean = false, // Is the form being submitted?
    val errorMessage: String? = null  // Error to display
)

