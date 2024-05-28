package com.example.racing.data.models

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class RaceWithDriversModel(
    @Embedded val race: RaceModel,
    @Relation(
        parentColumn = "raceId",
        entityColumn = "driverId",
        associateBy = Junction(RaceDriverCrossRefModel::class)
    )
    val drivers: List<DriverModel>,
)