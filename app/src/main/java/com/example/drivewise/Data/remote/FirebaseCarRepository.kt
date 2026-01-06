/**
 * FirebaseCarRepository.kt
 * =========================
 * This is the Firebase implementation of CarRepository.
 * It handles all car fleet CRUD operations using Firestore.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. CALLBACKFLOW: Bridges Firestore's callback-based API with Kotlin Flow.
 *    Firestore uses listeners (callbacks), but we want to expose data as Flow.
 *    callbackFlow lets us "emit" data whenever the Firestore listener fires.
 *
 * 2. SNAPSHOT LISTENER: Firestore's addSnapshotListener() fires:
 *    - Once immediately with current data
 *    - Again whenever data changes in the database
 *    This enables real-time updates - if another admin adds a car, everyone sees it.
 *
 * 3. DOCUMENT vs COLLECTION:
 *    - Collection = folder of documents ("cars" collection)
 *    - Document = single item (one car)
 *    - Document ID = unique identifier for that car
 *
 * 4. DATA MAPPING: Firestore stores data as key-value pairs.
 *    toObject(Car::class.java) converts a Firestore document to a Car instance.
 *    We then use copy(id = it.id) to ensure the document ID is included.
 */
package com.example.drivewise.Data.remote

import com.example.drivewise.domain.model.Car
import com.example.drivewise.domain.repository.CarRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose  // Suspend until Flow is cancelled
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow    // Bridge between callbacks and Flow
import kotlinx.coroutines.tasks.await          // Convert Firebase Task to suspend function

/**
 * Firebase Firestore implementation of CarRepository.
 *
 * All car data is stored in the "cars" collection in Firestore.
 *
 * @param firestore The Firestore database instance
 */
class FirebaseCarRepository(
    private val firestore: FirebaseFirestore
) : CarRepository {

    /**
     * Reference to the "cars" collection.
     * Using 'get()' makes this a computed property - evaluated each time it's accessed.
     * This is fine since Firestore collection references are lightweight.
     */
    private val carsCollection get() = firestore.collection("cars")

    // ═══════════════════════════════════════════════════════════════════════════
    // OBSERVE CARS (REAL-TIME)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Observes all cars with real-time updates.
     *
     * HOW CALLBACKFLOW WORKS:
     * 1. We create a Flow using callbackFlow { }
     * 2. Inside, we set up a Firestore listener
     * 3. When listener fires, we use trySend() to emit data to the Flow
     * 4. When the Flow is cancelled (no more collectors), awaitClose runs cleanup
     *
     * @return Flow that emits List<Car> whenever the cars collection changes
     */
    override fun observeCars(): Flow<List<Car>> = callbackFlow {
        // Set up the Firestore listener
        // orderBy sorts results (newest cars first)
        val registration = carsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                // Handle errors by closing the Flow
                if (error != null) {
                    close(error)  // This terminates the Flow with an error
                    return@addSnapshotListener
                }

                // Convert Firestore documents to Car objects
                val cars = snapshot?.documents?.mapNotNull { doc ->
                    // toObject converts Firestore data to Car class
                    // copy(id = doc.id) adds the document ID since it's not stored in the document itself
                    doc.toObject(Car::class.java)?.copy(id = doc.id)
                }.orEmpty()  // If snapshot is null, return empty list

                // Send the cars list to anyone collecting this Flow
                trySend(cars)
            }

        // awaitClose is called when the Flow is cancelled (e.g., Activity destroyed)
        // We MUST remove the listener to prevent memory leaks
        awaitClose { registration.remove() }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GET SINGLE CAR
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Fetches a single car by ID (one-time read, not real-time).
     *
     * @param carId The Firestore document ID
     * @return Result containing the Car, or null if not found
     */
    override suspend fun getCar(carId: String): Result<Car?> = runCatching {
        // Get the document (one-time fetch)
        val snapshot = carsCollection.document(carId).get().await()

        // Convert to Car object, adding the document ID
        snapshot.toObject(Car::class.java)?.copy(id = snapshot.id)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ADD CAR
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Adds a new car to the fleet.
     *
     * STEPS:
     * 1. Create a new document reference (auto-generates ID)
     * 2. Save the car with that ID included in the data
     *
     * @param car The car to add (id field will be overwritten with auto-generated ID)
     * @return Result.success if added successfully
     */
    override suspend fun addCar(car: Car): Result<Unit> = runCatching {
        // Create a reference to a new document with auto-generated ID
        val doc = carsCollection.document()

        // Save the car, including the auto-generated ID in the data
        carsCollection.document(doc.id).set(car.copy(id = doc.id)).await()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE CAR
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Updates an existing car.
     *
     * Uses set() which replaces the entire document.
     * The car.id must be a valid existing document ID.
     *
     * @param car The updated car data (must have valid id)
     * @return Result.success if updated successfully
     */
    override suspend fun updateCar(car: Car): Result<Unit> = runCatching {
        // set() replaces the entire document with new data
        carsCollection.document(car.id).set(car).await()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DELETE CAR
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Deletes a car from the fleet.
     *
     * @param carId The document ID of the car to delete
     * @return Result.success if deleted successfully
     */
    override suspend fun deleteCar(carId: String): Result<Unit> = runCatching {
        carsCollection.document(carId).delete().await()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE AVAILABILITY
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Updates only the availability field of a car.
     *
     * Uses update() instead of set() - this only changes specified fields,
     * leaving other fields unchanged. More efficient than updating the whole document.
     *
     * @param carId The car's document ID
     * @param available The new availability status
     * @return Result.success if updated successfully
     */
    override suspend fun updateAvailability(carId: String, available: Boolean): Result<Unit> = runCatching {
        // update() only changes the specified field(s)
        carsCollection.document(carId).update("available", available).await()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE PRICING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Updates only the daily price of a car.
     *
     * @param carId The car's document ID
     * @param newPrice The new daily rental price
     * @return Result.success if updated successfully
     */
    override suspend fun updatePricing(carId: String, newPrice: Double): Result<Unit> = runCatching {
        carsCollection.document(carId).update("dayPrice", newPrice).await()
    }
}

