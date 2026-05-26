package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ResidentDao {
    @Query("SELECT * FROM residents WHERE userId = :userId AND UPPER(block) = UPPER(:block) AND UPPER(buildingNumber) = UPPER(:buildingNumber) AND UPPER(name) = UPPER(:name) LIMIT 1")
    suspend fun getResidentByUniqueComposite(userId: Long, block: String, buildingNumber: String, name: String): Resident?

    @Query("SELECT * FROM residents WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllResidents(userId: Long): Flow<List<Resident>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResident(resident: Resident)

    @Delete
    suspend fun deleteResident(resident: Resident)
}
