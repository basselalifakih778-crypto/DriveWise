package com.example.drivewise.Data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao{
    @Query("Select * FROM cars ORDER BY id DESC")
    fun getALL():Flow<List<CarEntity>>

    @Insert
    suspend fun  insert(item: CarEntity)

    @Update
    suspend fun  update(item: CarEntity)

    @Delete
    suspend fun delete(item: CarEntity)
}