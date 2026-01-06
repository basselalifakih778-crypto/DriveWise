/**
 * CarEntity.kt
 * ==============
 * Room Entity representing a car in the local SQLite database.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. ENTITY: A data class that represents a table in the database.
 *    Each property = a column in the table.
 *    Each instance = a row in the table.
 *
 * 2. @Entity ANNOTATION: Tells Room this class is a database table.
 *    tableName defines the table name (default would be class name).
 *
 * 3. @PrimaryKey: Every table needs a primary key - a unique identifier for each row.
 *    autoGenerate = true means Room will auto-increment the ID for new entries.
 *
 * 4. CARENTITY vs CAR:
 *    - CarEntity: For local Room database (simpler, auto-generated int ID)
 *    - Car: For Firestore (has string ID, more fields like imageUrl, features)
 *    In a production app, you might use one model for both, or have mappers.
 *
 * ⚠️ NOTE: This entity is CURRENTLY NOT USED. See AppDatabase.kt for details.
 */
package com.example.drivewise.Data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a car in the local database.
 *
 * @Entity(tableName = "Cars") creates a table named "Cars".
 * Note: The DAO queries "cars" (lowercase) which may cause issues.
 * SQLite is case-insensitive by default, so it usually works.
 */
@Entity(tableName = "Cars")
data class CarEntity(
    /**
     * Primary key - auto-generated unique identifier.
     * Starts at 1 and increments for each new car.
     */
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** Car brand (e.g., "Toyota") */
    val brand: String,

    /** Car model (e.g., "Camry") */
    val model: String,

    /** Manufacturing year */
    val year: Int,

    /** Transmission type (e.g., "Automatic") */
    val transmission: String,

    /** Fuel type (e.g., "Petrol") */
    val fuel: String,

    /** Daily rental price */
    val dayPrice: Double,

    /** Whether the car is available for booking */
    val available: Boolean = true
)