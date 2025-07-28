package com.example.racing.screen.raceTable

import com.example.racing.domain.models.RaceDetailUI
import com.example.racing.domain.models.RaceUI
import com.example.racing.domain.models.SettingsUI
import com.example.racing.screen.base.UiState

data class RaceTableState(
    val raceDetailUI: RaceDetailUI,
    val fileExist: Boolean,
    val settingsUI: SettingsUI,
    val showRaceSelection: Boolean = false,
    val mergedResults: MergedResult = MergedResult(emptyList(), emptyList()),
    val availableRaces: List<RaceUI> = emptyList()

): UiState {
    companion object {
        val InitState = RaceTableState(
            raceDetailUI = RaceDetailUI.Default,
            fileExist = false,
            settingsUI = SettingsUI.Default
        )
    }
}