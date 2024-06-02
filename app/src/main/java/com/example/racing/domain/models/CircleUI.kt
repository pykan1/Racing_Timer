package com.example.racing.domain.models


data class CircleUI (
    val circleId: Long = 1,
    val raceId: Long,
    val penaltyFor: List<Long>,
    val drivers: List<DriverCircleUI>,
    val finishPenaltyDrivers: List<Long>,
)