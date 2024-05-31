package com.example.racing.domain.models

data class DriverResult(
    val driverId: Long,
    var totalDuration: Long = 0,
    var nonPenaltyCircles: Int = 0,
    var penaltyCircles: Int = 0
)
