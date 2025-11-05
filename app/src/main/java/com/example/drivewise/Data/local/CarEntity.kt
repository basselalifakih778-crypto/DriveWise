package com.example.drivewise.Data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="Cars")
data class CarEntity(
    @PrimaryKey(autoGenerate= true) val id: Int=0,
    val brand: String,
    val model: String,
    val year: Int,
    val transmission: String,
    val fuel: String,
    val dayPrice: Double,
    val  available:Boolean=true
)