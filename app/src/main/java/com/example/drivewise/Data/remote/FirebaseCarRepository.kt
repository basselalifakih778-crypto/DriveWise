package com.example.drivewise.Data.remote

import com.example.drivewise.domain.model.Car
import com.example.drivewise.domain.repository.CarRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseCarRepository(
    private val firestore: FirebaseFirestore
) : CarRepository {

    private val carsCollection get() = firestore.collection("cars")

    override fun observeCars(): Flow<List<Car>> = callbackFlow {
        val registration = carsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val cars = snapshot?.documents?.mapNotNull {
                    it.toObject(Car::class.java)?.copy(id = it.id)
                }.orEmpty()
                trySend(cars)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun getCar(carId: String): Result<Car?> = runCatching {
        val snapshot = carsCollection.document(carId).get().await()
        snapshot.toObject(Car::class.java)?.copy(id = snapshot.id)
    }

    override suspend fun addCar(car: Car): Result<Unit> = runCatching {
        val doc = carsCollection.document()
        carsCollection.document(doc.id).set(car.copy(id = doc.id)).await()
    }

    override suspend fun updateCar(car: Car): Result<Unit> = runCatching {
        carsCollection.document(car.id).set(car).await()
    }

    override suspend fun deleteCar(carId: String): Result<Unit> = runCatching {
        carsCollection.document(carId).delete().await()
    }

    override suspend fun updateAvailability(carId: String, available: Boolean): Result<Unit> = runCatching {
        carsCollection.document(carId).update("available", available).await()
    }

    override suspend fun updatePricing(carId: String, newPrice: Double): Result<Unit> = runCatching {
        carsCollection.document(carId).update("dayPrice", newPrice).await()
    }
}

