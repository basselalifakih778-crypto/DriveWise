package com.example.drivewise.domain.model

data class Car(
    val id: String = "",
    val brand: String = "",
    val model: String = "",
    val year: Int = 0,
    val transmission: String = "",
    val fuel: String = "",
    val dayPrice: Double = 0.0,
    val imageUrl: String = "",
    val available: Boolean = true,
    val features: List<String> = emptyList(),
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
