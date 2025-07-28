package com.example.racing.screen.player

import com.example.racing.domain.models.DriverUI
import com.example.racing.screen.base.UiState

data class PlayerState(
    val drivers: List<DriverUI>,
    val driverName: String,
    val driverLastName: String,
    val alertDialog: Boolean,
    val driverNumber: Long?,
    val city: String,
    val boatModel: String,
    val rank: String,
    val team: String,
    val selectEditDriver: DriverUI?
): UiState {
    companion object {
        val InitState = PlayerState(
            emptyList(), "", "", false, null, "", "", "", "", null
        )
    }
}