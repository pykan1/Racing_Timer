package com.example.racing.data.models

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class DriverWithRacesModel(
    @Embedded val driver: DriverModel,
    @Relation(
        parentColumn = "driverId",
        entityColumn = "raceId",
        associateBy = Junction(RaceDriverCrossRefModel::class)
    )
    val races: List<RaceModel>
)