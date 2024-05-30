package com.example.racing.domain.models

data class SettingsUI(
    val bleeper: Boolean,
    val vibration: Boolean,
    val email: String
) {
    companion object {
        val Default = SettingsUI(false, false, "")
    }
}