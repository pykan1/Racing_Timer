package com.example.racing.screen.settings

import com.example.racing.domain.models.SettingsUI
import com.example.racing.screen.base.UiState

data class SettingsState(
    val settingsUI: SettingsUI
): UiState {
    companion object {
        val InitState = SettingsState(
            settingsUI = SettingsUI.Default
        )
    }
}