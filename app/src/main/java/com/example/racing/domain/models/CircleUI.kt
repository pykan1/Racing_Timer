package com.example.racing.domain.models


data class CircleUI (
    val circleId: Long = 1,
    val raceId: Long,
    val isPenalty: Boolean,
    val drivers: List<DriverCircleUI>,
)