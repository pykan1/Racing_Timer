package com.example.racing.screen.raceTable

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.example.racing.data.local.repositoryImpl.RaceRepositoryImpl
import com.example.racing.domain.models.CircleUI
import com.example.racing.domain.models.DriverResult
import com.example.racing.domain.models.DriverUI
import com.example.racing.ext.formatSeconds
import com.example.racing.ext.formatTimestampToDateTimeString
import com.example.racing.screen.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import javax.inject.Inject


@HiltViewModel
class RaceTableViewModel @Inject constructor(
    private val raceRepositoryImpl: RaceRepositoryImpl,
) :
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

        // 3. Сортируем внутри каждой группы по общему времени
        val sortedGroups =
            groupedByNonPenaltyCircles.toSortedMap(compareByDescending { it }).values.flatMap { group ->
                group.sortedBy { it.totalDuration }
            }

        // 4. Возвращаем отсортированный список driverId
        return sortedGroups.map { driver ->
            drivers.find { it.driverId == driver.driverId }
        }
    }

    fun createExcelFile(context: Context) {
        val fileName =
            "${state.value.raceDetailUI.raceUI.raceTitle}_${state.value.raceDetailUI.raceUI.createRace.formatTimestampToDateTimeString()}.csv"
        val path = context.getExternalFilesDir(null)
        path?.let {
            val directory = File(it.absolutePath)

            if (!directory.exists()) {
                val directoryCreated = directory.mkdirs()
                if (!directoryCreated) {
                    Log.e("CreateExcelFile", "Failed to create directory")
                    return
                }
            }

            val file = File(directory, fileName)
            try {
                val fileCreated = file.createNewFile()
                if (fileCreated) {
                    createExcel(file, context)
                } else {
                    Log.e("CreateExcelFile", "File already exists")
                }
            } catch (e: IOException) {
                Log.e("CreateExcelFile", "IOException: ${e.message}")
            }
        }
    }

    private fun createExcel(file: File, context: Context) {
        val csvBuilder = StringBuilder()
        val raceDetailUI = state.value.raceDetailUI
        // Header rows
        csvBuilder.append("Результаты заезда от ${raceDetailUI.raceUI.createRace.formatTimestampToDateTimeString()}\n")
        csvBuilder.append("${raceDetailUI.raceUI.raceTitle}\n")

        // First table headers
        csvBuilder.append("Место,Участник,Время,Всего кругов\n")

        // First table data
        raceDetailUI.drivers.forEachIndexed { index, driver ->
            val time = raceDetailUI.circles.sumOf { circle ->
                circle.drivers.sumOf {
                    if (it.driverId == driver.driverId && !circle.isPenalty && it.useDuration) it.duration else 0
                }
            }.formatSeconds()

            val totalCircles = raceDetailUI.circles.count { circle ->
                driver.driverId in circle.drivers.map { it.driverId }
            }

            csvBuilder.append("${index + 1},${driver.driverNumber} ${driver.name} ${driver.lastName},${time},${totalCircles}\n")
        }

        // Second table headers
        csvBuilder.append("\nУчастник,")
        raceDetailUI.circles.forEachIndexed { index, circle ->
            csvBuilder.append(if (circle.isPenalty) "Штрафной круг," else "Круг ${index + 1},")
        }
        csvBuilder.append("\n")

        // Second table data
        raceDetailUI.drivers.forEach { driver ->
            csvBuilder.append("${driver.driverNumber},")
            raceDetailUI.circles.forEach { circle ->
                val driverCircle = circle.drivers.find { it.driverId == driver.driverId }
                val time = driverCircle?.duration?.formatSeconds() ?: ""
                val circleData = if (circle.isPenalty && driver.driverId !in circle.finishPenaltyDrivers) {
                    "Не прошел штрафной круг"
                } else {
                    time
                }
                csvBuilder.append("${circleData},")
            }
            csvBuilder.append("\n")
        }

        // Third table headers
        csvBuilder.append("\nКруг,Участники\n")

        // Third table data
        raceDetailUI.circles.forEachIndexed { index, circle ->
            csvBuilder.append("${if (!circle.isPenalty) (index + 1).toString() else "Штрафной"},")
            csvBuilder.append(circle.drivers.joinToString(", ") { it.driverNumber.toString() })
            csvBuilder.append("\n")
        }

        // Write to file
        file.writeText(csvBuilder.toString())

        // Notify user
        val toast = Toast.makeText(context, "Файл создан", Toast.LENGTH_SHORT)
        toast.show()
    }
}
