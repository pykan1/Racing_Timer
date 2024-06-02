package com.example.racing.screen.raceTable

import com.example.racing.domain.models.RaceDetailUI
import com.example.racing.domain.models.SettingsUI
import com.example.racing.screen.base.UiState

data class RaceTableState(
    val raceDetailUI: RaceDetailUI,
    val fileExist: Boolean,
    val settingsUI: SettingsUI

): UiState {
    companion object {
        val InitState = RaceTableState(
            raceDetailUI = RaceDetailUI.Default,
            fileExist = false,
            settingsUI = SettingsUI.Default
        )
    }
}