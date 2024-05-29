package com.example.racing.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "circleDriverCrossRef",
    primaryKeys = ["circleId", "driverId"],
    foreignKeys = [
        ForeignKey(entity = CircleModel::class, parentColumns = ["circleId"], childColumns = ["circleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = DriverModel::class, parentColumns = ["driverId"], childColumns = ["driverId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["circleId"]), Index(value = ["driverId"])]
)
data class CircleDriverCrossRef(
    val circleId: Long,
    val driverId: Long,
    val duration: Long,
)