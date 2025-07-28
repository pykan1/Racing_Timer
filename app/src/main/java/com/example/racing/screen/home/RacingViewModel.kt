package com.example.racing.screen.home

import androidx.lifecycle.viewModelScope
import com.example.racing.data.local.repositoryImpl.RaceRepositoryImpl
import com.example.racing.domain.models.DriverUI
import com.example.racing.domain.models.RaceUI
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

    fun copyRace(race: RaceUI) {
        viewModelScope.launch {
            // Генерация нового имени с нумерацией копий
            val newTitle = generateCopyName(race.raceTitle, state.value.races)
            val drivers = raceRepositoryImpl.getRaceDetail(race.raceId).drivers
            // Создание копии заезда
            raceRepositoryImpl.insertRace(
                title = newTitle,
                drivers = drivers.map { it.driverId }
            )

            // Обновление списка
            loadData()
        }
    }

    private fun generateCopyName(originalName: String, existingRaces: List<RaceUI>): String {
        val baseName = if (originalName.isBlank()) "Заезд" else originalName

        // Поиск максимального номера копии для этого имени
        val copyPattern = Regex("""$baseName \((\d+)\)$""")
        val maxCopyNumber = existingRaces
            .mapNotNull { it.raceTitle }
            .mapNotNull { copyPattern.find(it)?.groupValues?.get(1)?.toIntOrNull() }
            .maxOrNull() ?: 0

        // Если есть копии, увеличиваем номер, иначе создаем первую копию
        return if (maxCopyNumber > 0) {
            "$baseName (${maxCopyNumber + 1})"
        } else {
            "$baseName (1)"
        }
    }

    fun changeRaceTitle(it: String) {
        runBlocking {
            setState(state.value.copy(raceTitle = it))
        }
    }

    private fun getPlayers() {
        viewModelScope.launch {
            val drivers = raceRepositoryImpl.getDrivers().sortedBy { it.driverNumber }
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
            delay(300)
            val drivers = raceRepositoryImpl.searchDrivers(query = it).sortedBy { it.driverNumber }
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

    fun deleteRace(it: RaceUI) {
        viewModelScope.launch {
            viewModelScope.launch {
                raceRepositoryImpl.deleteRace(raceUI = it)
            }.join()
            loadData()
        }
    }
}