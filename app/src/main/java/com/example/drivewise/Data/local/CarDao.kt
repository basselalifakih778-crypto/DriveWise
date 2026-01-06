/**
 * CarDao.kt
 * ===========
 * Data Access Object (DAO) for Car entities in Room database.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. DAO (Data Access Object): An interface that defines database operations.
 *    Room generates the SQL implementation at compile time.
 *
 * 2. ANNOTATIONS: Tell Room what SQL to generate:
 *    - @Query: Custom SQL queries
 *    - @Insert: INSERT INTO...
 *    - @Update: UPDATE...SET...
 *    - @Delete: DELETE FROM...WHERE...
 *
 * 3. SUSPEND FUNCTIONS: For write operations (insert, update, delete).
 *    These run on a background thread (required by Room for main-thread safety).
 *
 * 4. FLOW RETURN: For read operations. The Flow automatically re-emits
 *    when underlying data changes (like Firestore's realtime updates, but local).
 *
 * ⚠️ NOTE: This DAO is CURRENTLY NOT USED. See AppDatabase.kt for details.
 */
package com.example.drivewise.Data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO interface for car database operations.
 *
 * Room generates the implementation based on these annotations.
 */
@Dao
interface CarDao {

    /**
     * Gets all cars from the database as a Flow.
     *
     * @Query defines custom SQL. Room validates this at compile time.
     * Returns Flow, so collectors get automatic updates when data changes.
     *
     * @return Flow emitting List<CarEntity> whenever cars table changes
     */
    @Query("Select * FROM cars ORDER BY id DESC")
    fun getALL(): Flow<List<CarEntity>>

    /**
     * Inserts a new car into the database.
     *
     * @Insert annotation handles the SQL generation.
     * Suspend function runs on background thread.
     *
     * @param item The car to insert
     */
    @Insert
    suspend fun insert(item: CarEntity)

    /**
     * Updates an existing car in the database.
     *
     * Room matches by primary key (id) to find which row to update.
     *
     * @param item The car with updated fields
     */
    @Update
    suspend fun update(item: CarEntity)

    /**
     * Deletes a car from the database.
     *
     * Room matches by primary key (id) to find which row to delete.
     *
     * @param item The car to delete
     */
    @Delete
    suspend fun delete(item: CarEntity)
}