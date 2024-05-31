package com.example.racing.screen.player

import androidx.lifecycle.viewModelScope
import com.example.racing.data.local.repositoryImpl.RaceRepositoryImpl
import com.example.racing.domain.models.DriverUI
import com.example.racing.screen.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(private val raceRepositoryImpl: RaceRepositoryImpl) :
    BaseViewModel<PlayerState>(PlayerState.InitState) {

    init {
        loadDrivers()
    }

    private fun loadDrivers() {
        viewModelScope.launch {
            val drivers = raceRepositoryImpl.getDrivers()
            setState(state.value.copy(drivers = drivers))
        }
    }

    fun changeAlert() {
        viewModelScope.launch {
            if (state.value.alertDialog) {
                clearAlert()
            } else {
                setState(
                    state.value.copy(
                        alertDialog = !state.value.alertDialog
                    )
                )
            }
        }
    }

    fun changeName(it: String) {
        runBlocking {
            setState(state.value.copy(driverName = it))
        }
    }

    fun changeLastName(it: String) {
        runBlocking {
            setState(state.value.copy(driverLastName = it))
        }
    }

    fun changeDriverNumber(it: String) {
        viewModelScope.launch {
            setState(
                state.value.copy(
                    driverNumber = it.filter { it.isDigit() }.toLongOrNull() ?: 0
                )
            )
        }
    }

    private fun clearAlert() {
        viewModelScope.launch {
            setState(
                state.value.copy(
                    driverName = "",
                    driverLastName = "",
                    alertDialog = false,
                    driverNumber = null
                )
            )
        }
    }

    fun deletePlayer(driverUI: DriverUI) {
        viewModelScope.launch {
            raceRepositoryImpl.deleteDriver(driverUI)
            setState(state.value.copy(drivers = state.value.drivers.filter { it.driverId != driverUI.driverId }))
        }
    }

    fun createPlayer() {
        viewModelScope.launch {
            raceRepositoryImpl.createDriver(
                DriverUI(
                    name = state.value.driverName,
                    lastName = state.value.driverLastName,
                    driverNumber = state.value.driverNumber?: 0
                )
            )
            clearAlert()
            loadDrivers()
        }
    }

}