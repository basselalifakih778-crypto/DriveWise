package com.example.drivewise.domain.model

data class Post(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)

