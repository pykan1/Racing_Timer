package com.example.racing.screen.raceTable

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import com.example.racing.data.local.repositoryImpl.RaceRepositoryImpl
import com.example.racing.data.local.repositoryImpl.StoreManager
import com.example.racing.domain.models.CircleUI
import com.example.racing.domain.models.DriverResult
import com.example.racing.domain.models.DriverUI
import com.example.racing.domain.models.RaceDetailUI
import com.example.racing.domain.models.RaceUI
import com.example.racing.ext.formatTimestampToDateTimeString
import com.example.racing.screen.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import javax.inject.Inject


@HiltViewModel
class RaceTableViewModel @Inject constructor(
    private val raceRepositoryImpl: RaceRepositoryImpl,
    private val storeManager: StoreManager,
) :
    BaseViewModel<RaceTableState>(RaceTableState.InitState) {
    // Добавляем состояние для объединенных заездов
    private var mergedRaces = mutableListOf<Long>()
    // Таблица очков UIM (20 значений)
    private val pointsSystem = listOf(25, 20, 16, 13, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 0, 0, 0, 0)

    fun loadRace(id: Long, context: Context) {
        viewModelScope.launch {
            if (mergedRaces.isEmpty()) mergedRaces.add(id)
            val availableRaces = raceRepositoryImpl.getRaces()
            val raceDetails = mergedRaces.map { raceRepositoryImpl.getRaceDetail(it) }
            val mergedResult = mergeRaceResults(raceDetails)

            val fileName = generateMergedFileName()
            val path = context.getExternalFilesDir(null)
            val isExist = path?.let { File(it, fileName).exists() } ?: false

            setState(
                state.value.copy(
                    availableRaces = availableRaces.filter { it.finish },
                    mergedResults = mergedResult,
                    fileExist = isExist
                )
            )
            storeManager.getSettings().collect {
                setState(state.value.copy(settingsUI = it))
            }
            merge()
        }
    }

    // Внутренние классы для расчета результатов
    private data class DriverStats(
        val driverId: Long,
        var nonPenaltyCircles: Int = 0,
        var penaltyCircles: Int = 0,
        var totalDuration: Long = 0
    )

    private data class DriverPlacement(
        val driver: DriverUI,
        val place: Int,
        val laps: Int,
        val penaltyCount: Int
    )

    private fun calculateDriverPlacements(raceDetail: RaceDetailUI): List<DriverPlacement> {
        val driverStatsMap = mutableMapOf<Long, DriverStats>()

        raceDetail.circles.forEach { circle ->
            circle.drivers.forEach { driver ->
                if (raceDetail.drivers.any { it.driverId == driver.driverId }) {
                    val stats = driverStatsMap.getOrPut(driver.driverId) { DriverStats(driver.driverId) }
                    if (!driver.useDuration) {
                        stats.penaltyCircles++
                    } else {
                        stats.nonPenaltyCircles++
                        stats.totalDuration += driver.duration
                    }
                }
            }
        }

        val driversWithStats = driverStatsMap.values.toList()
        val groupedByNonPenalty = driversWithStats.groupBy { it.nonPenaltyCircles }

        val sortedGroups = groupedByNonPenalty
            .toSortedMap(compareByDescending { it })
            .values
            .flatMap { group -> group.sortedBy { it.totalDuration } }

        val placedDrivers = sortedGroups.mapNotNull { stats ->
            raceDetail.drivers.find { it.driverId == stats.driverId }?.let { driver ->
                Pair(driver, stats)
            }
        }

        val driversWithoutStats = raceDetail.drivers.filter { driver ->
            driver.driverId !in driverStatsMap.keys
        }

        val allDriversWithPlace = placedDrivers + driversWithoutStats.map { driver ->
            Pair(driver, null)
        }

        return allDriversWithPlace.mapIndexed { index, (driver, stats) ->
            if (stats != null) {
                DriverPlacement(
                    driver = driver,
                    place = index + 1,
                    laps = stats.nonPenaltyCircles + stats.penaltyCircles,
                    penaltyCount = stats.penaltyCircles
                )
            } else {
                DriverPlacement(
                    driver = driver,
                    place = index + 1,
                    laps = 0,
                    penaltyCount = 0
                )
            }
        }
    }

    private fun mergeRaceResults(raceDetails: List<RaceDetailUI>): MergedResult {
        val allDrivers = raceDetails.flatMap { it.drivers }.distinctBy { it.driverId }
        val racePlacements = raceDetails.associateWith { calculateDriverPlacements(it) }

        val driverResults = allDrivers.map { driver ->
            val results = raceDetails.map { raceDetail ->
                val placements = racePlacements[raceDetail] ?: emptyList()
                val placement = placements.find { it.driver.driverId == driver.driverId }

                if (placement != null) {
                    RaceResult(
                        position = placement.place,
                        points = if (placement.place in 1..pointsSystem.size) pointsSystem[placement.place - 1] else 0,
                        laps = placement.laps,
                        penaltyCount = placement.penaltyCount
                    )
                } else {
                    RaceResult(0, 0, 0, 0) // Гонщик не участвовал
                }
            }

            MergedDriverResult(
                driver = driver,
                results = results,
                totalPoints = results.sumOf { it.points }
            )
        }

        // Логика сортировки для правильного ПОРЯДКА в таблице
        val sortedResults = driverResults.sortedWith { a, b ->
            // 1. Сортируем по сумме очков (по убыванию)
            val pointsCompare = b.totalPoints.compareTo(a.totalPoints)
            if (pointsCompare != 0) return@sortedWith pointsCompare

            // 2. Если очки равны, смотрим на результаты заездов, начиная с последнего
            for (i in a.results.indices.reversed()) {
                val aPos = a.results.getOrNull(i)?.position ?: Int.MAX_VALUE
                val bPos = b.results.getOrNull(i)?.position ?: Int.MAX_VALUE
                val validAPos = if (aPos == 0) Int.MAX_VALUE else aPos
                val validBPos = if (bPos == 0) Int.MAX_VALUE else bPos
                val posCompare = validAPos.compareTo(validBPos)
                if (posCompare != 0) return@sortedWith posCompare
            }
            0 // Полностью равны
        }

        return MergedResult(
            races = raceDetails.map { it.raceUI },
            drivers = sortedResults
        )
    }

    private fun generateMergedFileName(): String {
        val sortedIds = mergedRaces.sorted()
        val idsString = sortedIds.joinToString("_") { it.toString() }
        return "merged_${idsString}.csv"
    }

    fun createExcelFile(context: Context, doAfter: (Context, File) -> Unit = { _, _ -> }) {
        val fileName = generateMergedFileName()
        val path = context.getExternalFilesDir(null)

        path?.let {
            val directory = File(it.absolutePath)
            if (!directory.exists()) directory.mkdirs()
            val file = File(directory, fileName)
            try {
                if (file.exists()) file.delete()
                if (file.createNewFile()) {
                    createExcel(file, context, doAfter)
                } else {
                    doAfter(context, file)
                    setState(state.value.copy(fileExist = true))
                    Log.e("CreateExcelFile", "File creation failed, but doAfter called")
                }
            } catch (e: IOException) {
                Log.e("CreateExcelFile", "IOException: ${e.message}")
            }
        }
    }

    // =================================================================
    // ============ ИЗМЕНЕННАЯ ФУНКЦИЯ СОЗДАНИЯ EXCEL ====================
    // =================================================================
    private fun createExcel(
        file: File,
        context: Context,
        doAfter: (Context, File) -> Unit
    ) {
        merge() // Убедимся, что данные и сортировка актуальны
        val csvBuilder = StringBuilder()
        val mergedResult = state.value.mergedResults
        val numRaces = mergedResult.races.size
        val numDrivers = mergedResult.drivers.size

        // ----- Вспомогательные функции -----
        fun indexToExcelColumn(index: Int): String {
            var idx = index
            var str = ""
            while (idx >= 0) {
                str = ('A' + idx % 26) + str
                idx = idx / 26 - 1
            }
            return str
        }

        fun escapeCsvValue(value: Any): String {
            val strValue = value.toString()
            return if (strValue.contains(",") || strValue.contains("\"") || strValue.startsWith("=")) {
                "\"${strValue.replace("\"", "\"\"")}\""
            } else {
                strValue
            }
        }

        // ----- 1. Заголовки -----
        csvBuilder.append("Сводные результаты заездов\n\n")

        val raceTitleHeader = mutableListOf<String>()
        raceTitleHeader.addAll(List(6) { "" })
        mergedResult.races.forEach { race ->
            raceTitleHeader.add(escapeCsvValue("Заезд - ${race.raceTitle}"))
            raceTitleHeader.addAll(List(3) { "" })
        }
        csvBuilder.append(raceTitleHeader.joinToString(",")).append("\n")

        val mainHeader = mutableListOf(
            "Место", "Фамилия Имя", "Город", "Техника", "Звание", "Ст. номер"
        )
        repeat(numRaces) {
            mainHeader.addAll(listOf("Место в заезде", "Круги", "Штрафы", "Очки"))
        }
        mainHeader.add("Сумма очков")
        csvBuilder.append(mainHeader.joinToString(",") { escapeCsvValue(it) }).append("\n")

        // ----- 2. Вычисление динамических позиций -----
        val mainTableStartRow = 5
        val mainTableLastRow = mainTableStartRow + numDrivers - 1
        val pointsTableStartRow = mainTableLastRow + 3

        // ----- 3. Данные участников -----
        mergedResult.drivers.forEachIndexed { idx, driverResult ->
            val currentRow = mainTableStartRow + idx
            val rowData = mutableListOf<String>()

            // === ВОЗВРАЩАЕМ ФОРМУЛУ RANK.EQ ===
            val sumPointsColumnIndex = 6 + numRaces * 4
            val sumPointsColumnLetter = indexToExcelColumn(sumPointsColumnIndex)
            val rankRange = "\$${sumPointsColumnLetter}\$${mainTableStartRow}:\$${sumPointsColumnLetter}\$${mainTableLastRow}"
            rowData.add(
                escapeCsvValue("=RANK.EQ(${sumPointsColumnLetter}${currentRow}, ${rankRange}, 0)")
            )

            // Данные гонщика
            val driver = driverResult.driver
            rowData.add(escapeCsvValue("${driver.lastName} ${driver.name}"))
            rowData.add(escapeCsvValue(driver.city))
            rowData.add(escapeCsvValue(driver.boatModel))
            rowData.add(escapeCsvValue(driver.rank))
            rowData.add(escapeCsvValue(driver.driverNumber))

            // Данные по заездам
            val pointsFormulaParts = mutableListOf<String>()
            for (raceIdx in 0 until numRaces) {
                val result = driverResult.results[raceIdx]
                rowData.add(escapeCsvValue(result.position))
                rowData.add(escapeCsvValue(result.laps))
                rowData.add(escapeCsvValue(result.penaltyCount))

                val placeColIndex = 6 + raceIdx * 4
                val placeColLetter = indexToExcelColumn(placeColIndex)
                val pointsTableRange = "\$X\$${pointsTableStartRow + 1}:\$Y\$${pointsTableStartRow + 20}"
                rowData.add(
                    escapeCsvValue("=IF(OR(${placeColLetter}${currentRow}<=0, ${placeColLetter}${currentRow}>20), 0, VLOOKUP(${placeColLetter}${currentRow}, ${pointsTableRange}, 2, FALSE))")
                )

                val pointsColLetter = indexToExcelColumn(placeColIndex + 3)
                pointsFormulaParts.add("$pointsColLetter$currentRow")
            }

            // Формула суммы очков
            rowData.add(escapeCsvValue("=SUM(${pointsFormulaParts.joinToString(",")})"))
            csvBuilder.append(rowData.joinToString(",")).append("\n")
        }

        // ----- 4. Таблица очков (внизу) -----
        csvBuilder.append("\n\n")
        val pointsHeaderPadding = List(23) { "" }
        csvBuilder.append(pointsHeaderPadding.joinToString(","))
        csvBuilder.append(",").append(escapeCsvValue("Место")).append(",").append(escapeCsvValue("Очки")).append("\n")
        pointsSystem.forEachIndexed { index, points ->
            csvBuilder.append(pointsHeaderPadding.joinToString(","))
            csvBuilder.append(",").append(escapeCsvValue(index + 1)).append(",").append(escapeCsvValue(points)).append("\n")
        }

        // ----- 5. Запись в файл -----
        try {
            OutputStreamWriter(file.outputStream(), StandardCharsets.UTF_16).use { it.write(csvBuilder.toString()) }
            Toast.makeText(context, "Файл создан", Toast.LENGTH_SHORT).show()
            setState(state.value.copy(fileExist = true))
            doAfter(context, file)
        } catch (e: IOException) {
            Log.e("CreateExcel", "Ошибка записи в файл: ${e.message}")
            Toast.makeText(context, "Ошибка создания файла", Toast.LENGTH_SHORT).show()
        }
    }

    // Остальной код ViewModel без изменений...
    fun shareFile(context: Context, file: File) {
        viewModelScope.launch {
            val uri: Uri =
                FileProvider.getUriForFile(
                    context,
                    context.applicationContext.packageName + ".provider",
                    file
                )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(state.value.settingsUI.email))
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    """Результаты заезда "${state.value.raceDetailUI.raceUI.raceTitle}" """
                )
                putExtra(Intent.EXTRA_TEXT, "Результаты заезда")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(Intent.createChooser(intent, "Отправить Email"))
            } else {
                Toast.makeText(context, "Нет приложения для отправки Email", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }
    fun openFile(context: Context, file: File) {
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".provider",
                file
            )
        } else {
            Uri.fromFile(file)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/csv")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Нет приложения для открытия файла", Toast.LENGTH_LONG).show()
        }
    }


    private fun generateFileName(mergedResult: MergedResult): String {
        val raceTitles = mergedResult.races.joinToString("_") { it.raceTitle }
        val raceDates = mergedResult.races.joinToString("_") {
            it.createRace.formatTimestampToDateTimeString().replace(" ", "_")
        }
        return "/${raceTitles}_$raceDates.csv"
    }

    fun addRaceToMerge(raceId: Long) {
        if (!mergedRaces.contains(raceId)) {
            mergedRaces.add(raceId)
            merge()
        }
    }

    fun removeRaceFromMerge(raceId: Long) {
        mergedRaces.remove(raceId)
        merge()
    }

    fun merge() {
        viewModelScope.launch {
            val raceDetails = mergedRaces.map { raceRepositoryImpl.getRaceDetail(it) }
            val mergedResult = mergeRaceResults(raceDetails)
            setState(
                state.value.copy(
                    mergedResults = mergedResult,
                )
            )
        }
    }

    fun toggleRaceSelection(show: Boolean) {
        viewModelScope.launch {
            if (show) {
                val availableRaces = raceRepositoryImpl.getFinishedRacesExcept(mergedRaces)
                setState(state.value.copy(
                    showRaceSelection = true,
                    availableRaces = availableRaces
                ))
            } else {
                setState(state.value.copy(showRaceSelection = false))
            }
        }
    }

    suspend fun RaceRepositoryImpl.getFinishedRacesExcept(excludeIds: List<Long>): List<RaceUI> {
        return getRaces().filter {
            it.finish && it.raceId !in excludeIds
        }
    }
}


data class MergedResult(
    val races: List<RaceUI>,
    val drivers: List<MergedDriverResult>
)

data class MergedDriverResult(
    val driver: DriverUI,
    val results: List<RaceResult>,
    val totalPoints: Int
)

data class RaceResult(
    val position: Int,
    val points: Int,
    val laps: Int,
    val penaltyCount: Int
)