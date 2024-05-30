package com.example.racing.screen.race

import androidx.lifecycle.viewModelScope
import com.example.racing.data.local.repositoryImpl.RaceRepositoryImpl
import com.example.racing.data.local.repositoryImpl.StoreManager
import com.example.racing.data.mapper.toDriverCircleUI
import com.example.racing.domain.models.CircleUI
import com.example.racing.domain.models.DriverCircleUI
import com.example.racing.ext.getCurrentTimeInMillis
import com.example.racing.screen.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject


@HiltViewModel
class RaceViewModel @Inject constructor(
    private val raceRepositoryImpl: RaceRepositoryImpl,
    private val storeManager: StoreManager
) :
    BaseViewModel<RaceState>(RaceState.InitState) {
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            storeManager.getSettings().collect {
                setState(
                    state.value.copy(
                        settings = it
                    )
                )
            }
        }
        loadPlayers()
    }

    fun loadRace(id: Long) {
        viewModelScope.launch {
            val race = raceRepositoryImpl.getRaceById(id)
            setState(
                state.value.copy(
                    race = race
                )
            )
        }
    }

    fun changeIsTimer() {
        viewModelScope.launch {
            if (state.value.startTimer) {
                saveRace()
            }
            setState(
                state.value.copy(
                    startTimer = !state.value.startTimer
                )
            )
        }
    }

    fun changeSeconds() {
        viewModelScope.launch {
            setState(state.value.copy(seconds = state.value.seconds + 1))
        }
    }

    fun addCircle(driverUI: DriverCircleUI) {
        viewModelScope.launch {
            var isChange = false
            val circles = if (state.value.circles.lastOrNull()?.drivers?.map { it.driverId }
                    ?.contains(driverUI.driverId) == true || state.value.circles.isEmpty()) {
                state.value.circles + CircleUI(
                    circleId = getCurrentTimeInMillis(),
                    raceId = state.value.race.raceId,
                    drivers = listOf(driverUI.copy(duration = state.value.seconds - state.value.circles.sumOf {
                        it.drivers.find { it.driverId == driverUI.driverId }?.duration ?: 0
                    })),
                    isPenalty = false
                )
            } else state.value.circles.mapIndexed { index, circleUI ->
                if (!circleUI.drivers.map { it.driverId }
                        .contains(driverUI.driverId) && !isChange
                ) {
                    isChange = true
                    circleUI.copy(drivers = circleUI.drivers + driverUI.copy(
                        duration = state.value.seconds - state.value.circles.sumOf {
                            it.drivers.find { it.driverId == driverUI.driverId }?.duration ?: 0
                        }
                    ))
                } else {
                    circleUI
                }
            }
            setState(
                state.value.copy(
                    circles = circles,
                    driversIdStack = state.value.driversIdStack + driverUI.driverNumber
                )
            )
        }
    }

    fun saveDrivers() {
        viewModelScope.launch {
            setState(
                state.value.copy(
                    saveDrivers = true,
                    driversAlert = false
                )
            )
        }
    }

    fun driversAlert() {
        viewModelScope.launch {
            setState(
                state.value.copy(
                    driversAlert = !state.value.driversAlert
                )
            )
        }
    }

    fun loadPlayers() {
        viewModelScope.launch {
            val drivers = raceRepositoryImpl.getDrivers()
            setState(
                state.value.copy(
                    drivers = drivers.map { it.toDriverCircleUI() }
                )
            )
        }
    }

    private fun saveRace() {
        viewModelScope.launch {
            raceRepositoryImpl.saveRace(
                race = state.value.race.copy(duration = state.value.seconds),
                circles = state.value.circles,
                selectUsers = state.value.selectDrivers,
                raceStack = state.value.driversIdStack
            )
            setState(
                state.value.copy(
                    saveRace = true
                )
            )
        }
    }

    fun changeSearchPlayers(it: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400)
            val drivers = raceRepositoryImpl.searchDrivers(query = it)
            setState(state.value.copy(drivers = drivers.map { it.toDriverCircleUI() }))
        }
        runBlocking {
            setState(state.value.copy(searchDriver = it))
        }
    }

    fun changeSelectPlayers(driverUI: DriverCircleUI) {
        viewModelScope.launch {
            setState(
                state.value.copy(
                    selectDrivers = if (driverUI in state.value.selectDrivers) state.value.selectDrivers - driverUI else state.value.selectDrivers + driverUI
                )
            )
        }
    }
}