package com.example.racing.domain.models

data class RaceDetailUI (
    val raceUI: RaceUI,
    val drivers: List<DriverUI>,
    val circles: List<CircleUI>
) {
    companion object {
        val Default = RaceDetailUI(
            raceUI = RaceUI.Default,
            drivers = emptyList(),
            circles = emptyList()
        )
    }
}