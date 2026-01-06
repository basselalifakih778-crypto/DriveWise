/**
 * AppDatabase.kt
 * ================
 * Room database definition for local data storage.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. ROOM DATABASE: Android's abstraction layer over SQLite.
 *    - Easier than raw SQL
 *    - Compile-time SQL validation
 *    - Works well with Kotlin coroutines and Flow
 *
 * 2. DATABASE vs FIRESTORE:
 *    - Firestore: Cloud database, syncs across devices, requires internet
 *    - Room: Local SQLite database, works offline, device-specific
 *
 * 3. ABSTRACT CLASS: Room generates the implementation at compile time.
 *    You define WHAT (entities, DAOs), Room provides HOW.
 *
 * ⚠️ NOTE: This database is CURRENTLY NOT USED in the app.
 * The app uses Firestore for all data. This Room setup exists but
 * hasn't been wired up. It could be used for:
 * - Offline caching
 * - Local-first architecture
 * - Draft/temp data storage
 *
 * TO USE THIS DATABASE:
 * 1. Create an instance using Room.databaseBuilder()
 * 2. Call carDao() to get the DAO
 * 3. Use DAO methods to read/write data
 *
 * Example:
 *   val db = Room.databaseBuilder(context, AppDatabase::class.java, "drivewise-db").build()
 *   val cars = db.carDao().getAll().collect { ... }
 */
package com.example.drivewise.Data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The Room database for DriveWise.
 *
 * @Database annotation tells Room:
 * - entities: Which data classes are tables (CarEntity)
 * - version: Schema version (increment when changing table structure)
 */
@Database(
    entities = [CarEntity::class],  // Tables in this database
    version = 1                      // Schema version
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Provides access to car data operations.
     *
     * Room generates the implementation of CarDao at compile time.
     */
    abstract fun carDao(): CarDao
}