package com.example.racing.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "driver")
data class DriverModel(
    @PrimaryKey(autoGenerate = true) val driverId: Long = 0,
    val name: String,
    val lastName: String,
)