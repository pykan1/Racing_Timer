package com.example.racing.screen.home

import androidx.lifecycle.viewModelScope
import com.example.racing.data.local.repositoryImpl.RaceRepositoryImpl
import com.example.racing.domain.models.DriverUI
import com.example.racing.screen.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class RacingViewModel @Inject constructor(private val raceRepositoryImpl: RaceRepositoryImpl) :
    BaseViewModel<RacingState>(RacingState.InitState) {

    fun loadData() {
        viewModelScope.launch {
            val races = raceRepositoryImpl.getRaces()
            setState(state.value.copy(races = races))
            getPlayers()
        }
    }

    fun changeAlertDialog() {
        viewModelScope.launch {
            if (state.value.alertDialog) {
                clearAlert()
            }
            setState(state.value.copy(alertDialog = !state.value.alertDialog))
        }
    }

    fun changeRaceTitle(it: String) {
        runBlocking {
            setState(state.value.copy(raceTitle = it))
        }
    }

    private fun getPlayers() {
        viewModelScope.launch {
            val drivers = raceRepositoryImpl.getDrivers()
            setState(
                state.value.copy(
                    players = drivers
                )
            )
        }
    }


    private var searchJob: Job? = null
    fun changeSearchPlayers(it: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400)
            val drivers = raceRepositoryImpl.searchDrivers(query = it)
            setState(state.value.copy(players = drivers))
        }
        runBlocking {
            setState(state.value.copy(findPlayer = it))
        }
    }

    fun changeSelectPlayers(driverUI: DriverUI) {
        viewModelScope.launch {
            setState(
                state.value.copy(
                    selectPlayers = if (driverUI in state.value.selectPlayers) state.value.selectPlayers - driverUI else state.value.selectPlayers + driverUI
                )
            )
        }
    }

    private fun clearAlert() {
        viewModelScope.launch {
            setState(
                state.value.copy(
                    raceTitle = "",
                    findPlayer = "",
                    selectPlayers = emptyList(),
                    players = emptyList(),
                    alertDialog = false
                )
            )
        }
    }

    fun saveRace() {
        viewModelScope.launch {
            viewModelScope.launch {
                raceRepositoryImpl.insertRace(
                    state.value.raceTitle,
                    state.value.selectPlayers.map { it.driverId })
                clearAlert()

            }.join()
            loadData()
        }
    }
}