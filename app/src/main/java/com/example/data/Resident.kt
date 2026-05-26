package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "residents")
data class Resident(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long, // Scopes data uniquely to the logged-in user
    val buildingNumber: String,
    val block: String,
    val name: String,
    val phoneNumber: String,
    val familySize: Int,
    val isTenant: Boolean = false,
    val ownerName: String? = null,
    val ownerPhone: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
