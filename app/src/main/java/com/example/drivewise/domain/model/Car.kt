package com.example.drivewise.domain.model

data class Car(
    val id: String,
    val brand: String,
    val model: String,
    val year: Int,
    val transmission: String,
    val fuel: String,
    val dayPrice: Double,
    val  imageUrl: String,
    val  available:Boolean,
    val  features: List<String> = emptyList()

)



