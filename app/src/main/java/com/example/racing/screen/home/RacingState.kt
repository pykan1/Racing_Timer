package com.example.racing.screen.home

import com.example.racing.domain.models.DriverUI
import com.example.racing.domain.models.RaceUI
import com.example.racing.screen.base.UiState

data class RacingState(
    val races: List<RaceUI>,
    val alertDialog: Boolean,
    val raceTitle: String,
    val findPlayer: String,
    val players: List<DriverUI>,
    val selectPlayers: List<DriverUI>
) : UiState {
    companion object {
        val InitState = RacingState(
            races = emptyList(),
            alertDialog = false,
            raceTitle = "",
            findPlayer = "",
            players = emptyList(),
            selectPlayers = emptyList()

        )
    }
}