package com.example.racing.domain.models

data class DriverUI(
    val driverId: Long = 0,
    val driverNumber: Long,
    val name: String,
    val lastName: String,
    val city: String,
    val boatModel: String,
    val rank: String,
    val team: String
)

data class DriverCircleUI(
    val driverId: Long = 0,
    val driverNumber: Long,
    val name: String,
    val lastName: String,
    val city: String,
    val boatModel: String,
    val rank: String,
    val team: String,
    val duration: Long,
    val useDuration: Boolean
)