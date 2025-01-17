package com.example.racing.domain.models

data class DriverUI(
    val driverId: Long = 0,
    val driverNumber: Long,
    val name: String,
    val lastName: String
)

data class DriverCircleUI(
    val driverId: Long = 0,
    val driverNumber: Long,
    val name: String,
    val lastName: String,
    val duration: Long,
    val useDuration: Boolean
)