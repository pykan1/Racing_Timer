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


    fun addCircle(driverUI: DriverCircleUI, useDuration: Boolean) {
        viewModelScope.launch {
            var isChange = false
            val circles = if (state.value.circles.lastOrNull()?.drivers?.map { it.driverId }
                    ?.contains(driverUI.driverId) == true || state.value.circles.isEmpty()) {
                state.value.circles + CircleUI(
                    circleId = getCurrentTimeInMillis(),
                    raceId = state.value.race.raceId,
                    drivers = listOf(driverUI.copy(duration = state.value.seconds - state.value.circles.sumOf {
                        it.drivers.find { it.driverId == driverUI.driverId }?.duration ?: 0
                    }, useDuration = useDuration)),
                    penaltyFor = listOf(),
                    finishPenaltyDrivers = emptyList(),
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
                    ),
                        finishPenaltyDrivers = circleUI.finishPenaltyDrivers,
                        penaltyFor = if (isPenalty) circleUI.penaltyFor + driverUI.driverId else circleUI.penaltyFor)
                } else {
                    circleUI
                }
            }
            setState(
                state.value.copy(
                    circles = circles,
                    driversIdStack = state.value.driversIdStack.let {  it + driverUI.driverNumber }
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
            var circles = mutableListOf<CircleUI>()
            circles.addAll(state.value.circles)
            state.value.circles.forEach { circle ->
                state.value.selectDrivers.forEach { driver ->
                    if (driver.driverId in circle.penaltyFor && driver.driverId in circle.drivers.map { it.driverId } && driver.driverId !in circle.finishPenaltyDrivers) {
                        circles.findLast { driver.driverId in it.penaltyFor && driver.driverId in it.drivers.map { it.driverId } && driver.driverId !in circle.finishPenaltyDrivers && driver.useDuration }
                            ?.let { circle ->
                                circles = circles.map {
                                    if (it.circleId == circle.circleId) {
                                        it.copy(
                                            drivers = it.drivers.map {
                                                if (it.driverId == driver.driverId) it.copy(
                                                    useDuration = false //todo
                                                ) else it
                                            }
                                        )
                                    } else it
                                }.toMutableList()
                            }
                    }
                }
            }
            raceRepositoryImpl.saveRace(
                race = state.value.race.copy(duration = state.value.seconds),
                circles = circles,
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