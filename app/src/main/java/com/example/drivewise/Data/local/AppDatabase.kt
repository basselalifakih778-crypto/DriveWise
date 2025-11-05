package com.example.drivewise.Data.local

import androidx.room.Database
import androidx.room.RoomDataase

@Database(entities=[CarEntity::class],version=1)
abstract class AppDatabase : RoomDatabase(){
    abstract fun CarDao(): CarDao
}