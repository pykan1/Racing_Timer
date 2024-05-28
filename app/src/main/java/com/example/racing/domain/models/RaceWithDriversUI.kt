package com.example.racing.domain.models

data class RaceWithDriversUI(
    val race: RaceUI,
    val drivers: List<DriverUI>
)