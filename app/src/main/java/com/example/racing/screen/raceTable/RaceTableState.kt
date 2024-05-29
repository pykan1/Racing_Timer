package com.example.racing.screen.raceTable

import com.example.racing.domain.models.RaceDetailUI
import com.example.racing.screen.base.UiState

data class RaceTableState(
    val raceDetailUI: RaceDetailUI

): UiState {
    companion object {
        val InitState = RaceTableState(
            raceDetailUI = RaceDetailUI.Default
        )
    }
}