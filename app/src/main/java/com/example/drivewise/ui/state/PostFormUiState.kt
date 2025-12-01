package com.example.drivewise.ui.state

data class PostFormUiState(
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

