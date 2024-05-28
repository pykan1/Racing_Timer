package com.example.racing.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey


@Entity(tableName = "race",
    foreignKeys = [ForeignKey(
        entity = RaceModel::class,
        parentColumns = ["raceId"],
        childColumns = ["raceId"],
        onDelete = ForeignKey.CASCADE
    )],)
data class RaceModel(
    @PrimaryKey(autoGenerate = true)
    val raceId: Long = 0,
    val raceTitle: String,
    val createRace: Long,
    val duration: Long,
    val finish: Boolean,
)