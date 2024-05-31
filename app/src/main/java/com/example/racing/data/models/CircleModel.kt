package com.example.racing.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "circle")
data class CircleModel (
    @PrimaryKey(autoGenerate = true)
    val circleId: Long = 0,
    val raceId: Long,
    val isPenalty: Boolean,
    val finishPenaltyDrivers: String,
)