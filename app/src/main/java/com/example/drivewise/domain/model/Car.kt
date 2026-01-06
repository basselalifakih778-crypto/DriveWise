/**
 * Car.kt
 * =======
 * This file defines the Car data model - represents a rental car in DriveWise.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. DATA CLASS: Automatically generates useful methods (equals, hashCode, toString, copy).
 *
 * 2. DEFAULT VALUES: Every property has a default value so Firestore can create instances
 *    using reflection (it needs a no-argument constructor).
 *
 * 3. IMMUTABILITY: Properties are 'val' (read-only). To modify a car, use copy():
 *    val updatedCar = car.copy(available = false)
 *
 * FIRESTORE COLLECTION: "cars"
 * Each document represents one car available for rental.
 */
package com.example.drivewise.domain.model

/**
 * Represents a rental car in the DriveWise fleet.
 *
 * Admins create and manage cars. Clients browse and book them.
 *
 * @property id Unique identifier (Firestore document ID)
 * @property brand Car manufacturer (e.g., "Toyota", "BMW")
 * @property model Car model name (e.g., "Camry", "X5")
 * @property year Manufacturing year (e.g., 2023)
 * @property transmission Either "Automatic", "Manual", or "CVT"
 * @property fuel Fuel type: "Petrol", "Diesel", "Electric", or "Hybrid"
 * @property dayPrice Daily rental price in dollars
 * @property imageUrl URL to car image stored in Firebase Storage
 * @property available Whether the car is currently available for booking
 * @property features List of features (e.g., ["GPS", "Bluetooth", "Sunroof"])
 * @property description Detailed description of the car
 * @property createdAt Timestamp when the car was added to the system
 */
data class Car(
    val id: String = "",                 // Firestore document ID
    val brand: String = "",              // e.g., "Toyota"
    val model: String = "",              // e.g., "Camry"
    val year: Int = 0,                   // e.g., 2023
    val transmission: String = "",       // "Automatic", "Manual", "CVT"
    val fuel: String = "",               // "Petrol", "Diesel", "Electric", "Hybrid"
    val dayPrice: Double = 0.0,          // Price per day in $
    val imageUrl: String = "",           // Firebase Storage URL for car photo
    val available: Boolean = true,       // Can clients book this car?
    val features: List<String> = emptyList(),  // ["GPS", "Bluetooth", etc.]
    val description: String = "",        // Detailed car description
    val createdAt: Long = System.currentTimeMillis()  // When car was added
)
