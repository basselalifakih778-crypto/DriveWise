package com.example.drivewise.Data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CarEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun carDao(): CarDao
}