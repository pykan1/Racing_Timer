package com.example.racing.domain.models

data class DriverWithRacesUI(
    val driver: DriverUI,
    val races: List<RaceUI>
)