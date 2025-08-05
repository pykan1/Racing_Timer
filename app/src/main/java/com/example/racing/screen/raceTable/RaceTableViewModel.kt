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
import com.example.racing.ext.formatSeconds
import com.example.racing.ext.formatTimestampToDateTimeString
import com.example.racing.screen.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.ss.util.CellReference
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    ) { override fun toString(): String = secondsToTime(totalDuration) }

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

    private suspend fun generateMergedFileName(): String {
        // Меняем расширение на .xlsx
        val firstRace = mergedRaces.firstOrNull()?.let { raceRepositoryImpl.getRaceDetail(it) }
        val title = firstRace?.raceUI?.raceTitle?.replace(Regex("[/\\\\?%*:|\"<>]"), "_") ?: "Race"
        val dateFormat = SimpleDateFormat("dd:MM:yyyy_HH:mm:ss", Locale.getDefault())
        return "${title}_финал_${dateFormat.format(Date())}.xlsx"
    }

    fun createExcelFile(context: Context, doAfter: (Context, File) -> Unit = { _, _ -> }) {
        viewModelScope.launch(Dispatchers.IO) { // Используем Dispatchers.IO для работы с файлами
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
                    }
                } catch (e: IOException) {
                    Log.e("CreateExcelFile", "IOException: ${e.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Ошибка создания файла", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // =================================================================
    // ============ НОВАЯ ФУНКЦИЯ СОЗДАНИЯ EXCEL (XLSX) =================
    // =================================================================
    private suspend fun createExcel(
        file: File,
        context: Context,
        doAfter: (Context, File) -> Unit
    ) {
        // Убедимся, что данные и сортировка актуальны
        merge()
        val mergedResult = state.value.mergedResults
        // Получаем полные данные для генерации детальных листов
        val raceDetails = mergedRaces.map { raceRepositoryImpl.getRaceDetail(it) }

        // Создаем книгу Excel
        val workbook: Workbook = XSSFWorkbook()

        // --- ЛИСТ 1: СВОДНЫЕ РЕЗУЛЬТАТЫ (Ваша текущая логика) ---
        createSummarySheet(workbook, mergedResult)

        // --- ЛИСТ 2, 3, ...: ДЕТАЛЬНЫЕ РЕЗУЛЬТАТЫ ПО КАЖДОМУ ЗАЕЗДУ ---
        raceDetails.forEach { raceDetail ->
            createDetailedRaceSheet(workbook, raceDetail)
        }

        // --- ЗАПИСЬ В ФАЙЛ ---
        try {
            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
            }
            workbook.close()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Файл создан", Toast.LENGTH_SHORT).show()
                setState(state.value.copy(fileExist = true))
                doAfter(context, file)
            }
        } catch (e: IOException) {
            Log.e("CreateExcel", "Ошибка записи в файл: ${e.message}")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Ошибка записи файла", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createSummarySheet(workbook: Workbook, mergedResult: MergedResult) {
        val sheet = workbook.createSheet("Сводные результаты")
        val numRaces = mergedResult.races.size
        val numDrivers = mergedResult.drivers.size
        var rowIndex = 0

        // Заголовок
        sheet.createRow(rowIndex++).createCell(0).setCellValue("Сводные результаты заездов")
        rowIndex++ // Пустая строка

        // Заголовки заездов с объединением ячеек
        val raceTitleHeaderRow = sheet.createRow(rowIndex++)
        var currentCellIndex = 6
        mergedResult.races.forEach { race ->
            val cell = raceTitleHeaderRow.createCell(currentCellIndex)
            cell.setCellValue("Заезд - ${race.raceTitle}")
            sheet.addMergedRegion(
                CellRangeAddress(
                    rowIndex - 1,
                    rowIndex - 1,
                    currentCellIndex,
                    currentCellIndex + 3
                )
            )
            currentCellIndex += 4
        }

        // Основные заголовки таблицы
        val mainHeaderRow = sheet.createRow(rowIndex++)
        val mainHeader = mutableListOf("Место", "Фамилия Имя", "Город", "Техника", "Звание", "Ст. номер")
        repeat(numRaces) {
            mainHeader.addAll(listOf("Место в заезде", "Круги", "Штрафы", "Очки"))
        }
        mainHeader.add("Сумма очков")
        mainHeader.forEachIndexed { index, title -> mainHeaderRow.createCell(index).setCellValue(title) }

        // Данные участников
        val mainTableStartRow = rowIndex
        val mainTableLastRow = mainTableStartRow + numDrivers - 1
        val pointsTableStartRow = mainTableLastRow + 3
        val sumPointsColumnIndex = 6 + numRaces * 4
        val sumPointsColumnLetter = CellReference.convertNumToColString(sumPointsColumnIndex)

        mergedResult.drivers.forEachIndexed { idx, driverResult ->
            val currentRowNum = rowIndex
            val dataRow = sheet.createRow(rowIndex++)
            var cellIdx = 0

            val rankRange = "$sumPointsColumnLetter${mainTableStartRow + 1}:$sumPointsColumnLetter${mainTableLastRow + 1}"
            dataRow.createCell(cellIdx++).cellFormula = "IFERROR(RANK($sumPointsColumnLetter${currentRowNum + 1}, $rankRange, 0), " +
                    "\"=RANK(\"&$sumPointsColumnLetter${currentRowNum + 1}&\", \"&ADDRESS(${mainTableStartRow + 1}, COLUMN($sumPointsColumnLetter${mainTableStartRow + 1}))&\":\"&ADDRESS($mainTableLastRow, COLUMN($sumPointsColumnLetter${mainTableLastRow}))&\", 0)\")"
            // Данные гонщика
            val driver = driverResult.driver
            dataRow.createCell(cellIdx++).setCellValue("${driver.lastName} ${driver.name}")
            dataRow.createCell(cellIdx++).setCellValue(driver.city)
            dataRow.createCell(cellIdx++).setCellValue(driver.boatModel)
            dataRow.createCell(cellIdx++).setCellValue(driver.rank)
            dataRow.createCell(cellIdx++).setCellValue(driver.driverNumber.toDouble())

            // Данные по заездам
            val pointsFormulaParts = mutableListOf<String>()
            for (raceIdx in 0 until numRaces) {
                val result = driverResult.results[raceIdx]
                dataRow.createCell(cellIdx++).setCellValue(if (result.position > 0) result.position.toDouble() else 0.0)
                dataRow.createCell(cellIdx++).setCellValue(result.laps.toDouble())
                dataRow.createCell(cellIdx++).setCellValue(result.penaltyCount.toDouble())

                // Очки (Формула)
                val placeColLetter = CellReference.convertNumToColString(cellIdx - 3)
                val pointsColLetter = CellReference.convertNumToColString(cellIdx)
                dataRow.createCell(cellIdx++).cellFormula = "IF(OR($placeColLetter${currentRowNum + 1}<=0, " +
                        "$placeColLetter${currentRowNum + 1}>20), 0, " +
                        "VLOOKUP($placeColLetter${currentRowNum + 1}, " +
                        "\$X\$${pointsTableStartRow + 1}:\$Y\$${pointsTableStartRow + 20}, 2, FALSE))"

                pointsFormulaParts.add("$pointsColLetter${currentRowNum + 1}")
            }

            // Сумма очков (Формула)
            dataRow.createCell(cellIdx).cellFormula = "SUM(${pointsFormulaParts.joinToString(",")})"
        }

        // Пустые строки перед таблицей очков
        rowIndex += 2

        // Создаем именованный диапазон для таблицы очков
        val pointsTableRange = "X${pointsTableStartRow + 1}:Y${pointsTableStartRow + 20}"
        val name = workbook.createName()
        name.nameName = "PointsTable"
        name.refersToFormula = "${sheet.sheetName}!$pointsTableRange"

        // Таблица очков для VLOOKUP
        val pointsHeaderRow = sheet.createRow(rowIndex++)
        pointsHeaderRow.createCell(23).setCellValue("Место")
        pointsHeaderRow.createCell(24).setCellValue("Очки")

        pointsSystem.forEachIndexed { index, points ->
            val pointsRow = sheet.createRow(rowIndex++)
            pointsRow.createCell(23).setCellValue((index + 1).toDouble())
            pointsRow.createCell(24).setCellValue(points.toDouble())
        }
    }

    private fun createDetailedRaceSheet(workbook: Workbook, raceDetail: RaceDetailUI) {
        // Создаем безопасное имя для листа
        val safeSheetName = WorkbookUtil.createSafeSheetName("Заезд ${raceDetail.raceUI.raceTitle}")
        val sheet = workbook.createSheet(safeSheetName)
        var rowIndex = 0

//        fun formatMillisToTime(millis: Long): String {
//            if (millis <= 0) return "00:00.000"
//            val sdf = SimpleDateFormat("mm:ss.SSS", Locale.getDefault())
//            return sdf.format(Date(millis))
//        }

        // Общая информация о заезде
        sheet.createRow(rowIndex++).createCell(0).setCellValue("Результаты заезда от ${raceDetail.raceUI.createRace.formatTimestampToDateTimeString()}")
        sheet.createRow(rowIndex++).createCell(0).setCellValue("Название заезда: ${raceDetail.raceUI.raceTitle}")
        rowIndex++

        // Таблица 1: Итоговые места
        sheet.createRow(rowIndex++).createCell(0).setCellValue("Итоговые места")
        val placements = calculateDriverPlacements(raceDetail) // Используем актуальный расчет мест
        val pHeaderRow = sheet.createRow(rowIndex++)
        pHeaderRow.createCell(0).setCellValue("Место")
        pHeaderRow.createCell(1).setCellValue("Ст. номер")
        pHeaderRow.createCell(2).setCellValue("Имя Фамилия")
        pHeaderRow.createCell(3).setCellValue("Время")
        pHeaderRow.createCell(4).setCellValue("Всего кругов")
        pHeaderRow.createCell(5).setCellValue("Штрафы")

        placements.forEach { placement ->
            val time = raceDetail.circles.sumOf { circle ->
                circle.drivers.sumOf {
                    if (it.driverId == placement.driver.driverId && it.useDuration) it.duration else 0
                }
            }.formatSeconds()
            val row = sheet.createRow(rowIndex++)
            row.createCell(0).setCellValue(placement.place.toDouble())
            row.createCell(1).setCellValue(placement.driver.driverNumber.toDouble())
            row.createCell(2).setCellValue("${placement.driver.name} ${placement.driver.lastName}")
            row.createCell(3).setCellValue(time)
            row.createCell(4).setCellValue(placement.laps.toDouble())
            row.createCell(5).setCellValue(placement.penaltyCount.toDouble())
        }
        rowIndex++

        // Таблица 2: Время по кругам
        sheet.createRow(rowIndex++).createCell(0).setCellValue("По времени круга")
        val lapHeaderRow = sheet.createRow(rowIndex++)
        lapHeaderRow.createCell(0).setCellValue("Ст. номер")
        raceDetail.circles.forEachIndexed { index, _ ->
            lapHeaderRow.createCell(index + 1).setCellValue("Круг ${index + 1}")
        }

        raceDetail.drivers.forEach { driver ->
            val row = sheet.createRow(rowIndex++)
            row.createCell(0).setCellValue(driver.driverNumber.toDouble())
            raceDetail.circles.forEachIndexed { cIndex, circle ->
                val driverCircle = circle.drivers.find { it.driverId == driver.driverId }
                val time = driverCircle?.duration?.let { secondsToTime(it) } ?: "-" // DNS - Did Not Start

                val cellText = when {
                    // Эта логика кажется немного запутанной в оригинале, я упростил до ключевых состояний
                    driverCircle == null -> "-" // Не проехал круг
                    !driverCircle.useDuration -> "$time (Штраф)" // Время не засчитано, штраф
                    else -> time // Обычное время
                }
                row.createCell(cIndex + 1).setCellValue(cellText)
            }
        }
        rowIndex++

        // Дополнительная информация
        sheet.createRow(rowIndex++).createCell(0).setCellValue("Время заезда: ${secondsToTime(raceDetail.raceUI.duration)}")
        val finishOrderRow = sheet.createRow(rowIndex++)
        finishOrderRow.createCell(0).setCellValue("Порядок прохождения финишной линии:")
        val finishOrder = raceDetail.raceUI.stackFinish.joinToString(", ")
        finishOrderRow.createCell(1).setCellValue(finishOrder)
    }

    // --- ОБНОВЛЕННЫЕ ФУНКЦИИ ДЛЯ РАБОТЫ С XLSX ---

    fun shareFile(context: Context, file: File) {
        viewModelScope.launch {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                // Правильный MIME-тип для .xlsx
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(state.value.settingsUI.email))
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    """Результаты заезда "${state.value.mergedResults.races.firstOrNull()?.raceTitle ?: ""}" """
                )
                putExtra(Intent.EXTRA_TEXT, "Результаты заезда в прикрепленном файле.")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(Intent.createChooser(intent, "Отправить Email"))
            } else {
                Toast.makeText(context, "Нет приложения для отправки Email", Toast.LENGTH_LONG).show()
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
            // Правильный MIME-тип для .xlsx
            setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Нет приложения для открытия XLSX файла", Toast.LENGTH_LONG).show()
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
        viewModelScope.launch(Dispatchers.IO) {
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
private fun secondsToTime(totalSeconds: Long): String {
    return totalSeconds.formatSeconds()
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