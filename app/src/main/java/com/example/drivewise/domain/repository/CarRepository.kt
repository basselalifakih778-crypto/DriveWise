package com.example.drivewise.domain.repository

import com.example.drivewise.domain.model.Car
import kotlinx.coroutines.flow.Flow

interface CarRepository {
    fun observeCars(): Flow<List<Car>>
    suspend fun getCar(carId: String): Result<Car?>
    suspend fun addCar(car: Car): Result<Unit>
    suspend fun updateCar(car: Car): Result<Unit>
    suspend fun deleteCar(carId: String): Result<Unit>
    suspend fun updateAvailability(carId: String, available: Boolean): Result<Unit>
    suspend fun updatePricing(carId: String, newPrice: Double): Result<Unit>
}

