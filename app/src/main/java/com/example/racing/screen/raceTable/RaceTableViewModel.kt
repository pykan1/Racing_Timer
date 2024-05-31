package com.example.racing.screen.raceTable

import androidx.lifecycle.viewModelScope
import com.example.racing.data.local.repositoryImpl.RaceRepositoryImpl
import com.example.racing.domain.models.CircleUI
import com.example.racing.domain.models.DriverResult
import com.example.racing.domain.models.DriverUI
import com.example.racing.screen.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class RaceTableViewModel @Inject constructor(private val raceRepositoryImpl: RaceRepositoryImpl) :
    BaseViewModel<RaceTableState>(RaceTableState.InitState) {

    fun loadRace(id: Long) {
        viewModelScope.launch {
            val raceDetail = raceRepositoryImpl.getRaceDetail(id)
            val rankDrivers = rankDrivers(raceDetail.circles, raceDetail.drivers).filterNotNull()
            setState(
                state.value.copy(
                    raceDetailUI = raceDetail.copy(
                        drivers = rankDrivers,
                        circles = raceDetail.circles.sortedBy { it.isPenalty })
                )
            )
        }
    }

    private fun rankDrivers(circles: List<CircleUI>, drivers: List<DriverUI>): List<DriverUI?> {
        val driverResults = mutableMapOf<Long, DriverResult>()

        // 1. Собираем информацию о каждом водителе
        circles.forEach { circle ->
            circle.drivers.forEach { driver ->
                val result =
                    driverResults.getOrPut(driver.driverId) { DriverResult(driver.driverId) }
                result.totalDuration += if (driver.useDuration) driver.duration else 0
                if (circle.isPenalty || !driver.useDuration) {
                    result.penaltyCircles += 1
                } else {
                    result.nonPenaltyCircles += 1
                }
            }
        }

        // 2. Группируем водителей по количеству не штрафных кругов
        val groupedByNonPenaltyCircles = driverResults.values.groupBy { it.nonPenaltyCircles }
        println(groupedByNonPenaltyCircles)
        // 3. Сортируем внутри каждой группы по общему времени
        val sortedGroups =
            groupedByNonPenaltyCircles.toSortedMap(compareByDescending { it }).values.flatMap { group ->
                group.sortedBy { it.totalDuration }
            }
        println(sortedGroups)

        // 4. Возвращаем отсортированный список driverId
        return sortedGroups.map { driver ->
            drivers.find { it.driverId == driver.driverId }
        }
    }

}