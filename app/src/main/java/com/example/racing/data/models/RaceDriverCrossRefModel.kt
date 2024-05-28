package com.example.racing.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    primaryKeys = ["raceId", "driverId"],
    foreignKeys = [
        ForeignKey(entity = RaceModel::class, parentColumns = ["raceId"], childColumns = ["raceId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = DriverModel::class, parentColumns = ["driverId"], childColumns = ["driverId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["raceId"]), Index(value = ["driverId"])]
)
data class RaceDriverCrossRefModel(
    val raceId: Long,
    val driverId: Long
)