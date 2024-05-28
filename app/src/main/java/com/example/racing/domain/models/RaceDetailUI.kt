package com.example.racing.domain.models

data class RaceDetailUI (
    val raceUI: RaceUI,
    val drivers: List<DriverUI>,
    val circleUI: List<CircleUI>
)