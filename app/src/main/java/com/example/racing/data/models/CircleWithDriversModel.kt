package com.example.racing.data.models

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class CircleWithDrivers(
    @Embedded val circle: CircleModel,
    @Relation(
        parentColumn = "circleId",
        entityColumn = "driverId",
        associateBy = Junction(CircleDriverCrossRef::class)
    )
    val drivers: List<DriverModel>
)