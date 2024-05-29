package com.example.racing.screen.race

import com.example.racing.domain.models.CircleUI
import com.example.racing.domain.models.DriverCircleUI
import com.example.racing.domain.models.RaceUI
import com.example.racing.screen.base.UiState

data class RaceState(
    val race: RaceUI,
    val searchDriver: String,
    val drivers: List<DriverCircleUI>,
    val selectDrivers: List<DriverCircleUI>,
    val saveDrivers: Boolean,
    val seconds: Long,
    val circles: List<CircleUI>,
    val driversAlert: Boolean,
    val startTimer: Boolean,
    val driversIdStack: List<Long>,
    val saveRace: Boolean
) : UiState {
    companion object {
        val InitState = RaceState(
            RaceUI.Default,
            "", emptyList(), emptyList(), false, 0, emptyList(), false, false, emptyList(), saveRace = false
        )
    }
}