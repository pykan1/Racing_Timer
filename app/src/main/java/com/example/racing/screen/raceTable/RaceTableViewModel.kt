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
    fun loadRace(id: Long, context: Context) {
        viewModelScope.launch {
            val availableRaces = raceRepositoryImpl.getRaces()
            if (mergedRaces.isEmpty()) {
                mergedRaces.add(id)
            }

            val raceDetails = mergedRaces.map { raceId ->
                raceRepositoryImpl.getRaceDetail(raceId)
            }

            val mergedResult = mergeRaceResults(raceDetails)

            // Генерируем имя файла для объединенного отчета
            val fileName = generateMergedFileName()
            val path = context.getExternalFilesDir(null)
            val isExist = path?.let {
                File(it, fileName).exists()
            } ?: false

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
    private fun generateMergedFileName(): String {
        // Сортируем ID заездов по возрастанию для единообразия
        val sortedIds = mergedRaces.sorted()
        val idsString = sortedIds.joinToString("_") { it.toString() }
        return "merged_${idsString}.csv"
    }

    private fun rankDrivers(circles: List<CircleUI>, drivers: List<DriverUI>): List<DriverUI?> {
        val driverResults = mutableMapOf<Long, DriverResult>()

        // 1. Собираем информацию о каждом водителе
        circles.forEach { circle ->
            circle.drivers.forEach { driver ->
                val result =
                    driverResults.getOrPut(driver.driverId) { DriverResult(driver.driverId) }
                result.totalDuration += driver.duration
                if (!driver.useDuration) {
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

    fun createExcelFile(context: Context, doAfter: (Context, File) -> Unit = { _, _ -> }) {
        // Генерируем уникальное имя файла на основе ID заездов
        val fileName = generateMergedFileName()
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
                if (file.exists()) {
                    // Файл уже существует, перезаписываем его
                    file.delete()
                }

                val fileCreated = file.createNewFile()
                if (fileCreated) {
                    createExcel(file, context, doAfter)
                } else {
                    // Не удалось создать файл, но все равно вызываем doAfter
                    doAfter(context, file)
                    setState(state.value.copy(fileExist = true))
                    Log.e("CreateExcelFile", "File creation failed")
                }
            } catch (e: IOException) {
                Log.e("CreateExcelFile", "IOException: ${e.message}")
            }
        }
    }

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

    private fun createExcel(
        file: File,
        context: Context,
        doAfter: (Context, File) -> Unit = { _, _ -> }
    ) {
        merge()
        val csvBuilder = StringBuilder()
        val mergedResult = state.value.mergedResults

        // Заголовок
        csvBuilder.append("Сводные результаты заездов\n")
        mergedResult.races.forEachIndexed { index, race ->
            csvBuilder.append("Заезд ${index + 1}: ${race.raceTitle} (${race.createRace.formatTimestampToDateTimeString()})\n")
        }
        csvBuilder.append("\n")

        // Основные заголовки
        csvBuilder.append("МЕСТО,Фамилия Имя,Город,Техника,Звание,Ст. номер")

        // Добавляем объединенные заголовки для каждого заезда
        mergedResult.races.forEachIndexed { index, race ->
            // Добавляем название заезда как объединенную ячейку
            csvBuilder.append(",Заезд $index: \"${race.raceTitle}\"")
            // Добавляем 3 пустые ячейки для объединения (всего 4 колонки)
            csvBuilder.append(",,,")
        }
        // Заголовок для суммы очков
        csvBuilder.append(",Сумма очков\n")

        // Подзаголовки для каждой группы
        // Основные заголовки (пустые)
        csvBuilder.append(",,,,,")
        mergedResult.races.forEach { _ ->
            // Подзаголовки для каждой группы столбцов
            csvBuilder.append(",Место,Круги,Штрафы,Очки")
        }
        csvBuilder.append(",Сумма очков\n")

        // Данные
        mergedResult.drivers.forEachIndexed { position, driverResult ->
            val driver = driverResult.driver
            csvBuilder.append("${position + 1},${driver.lastName} ${driver.name},${driver.city},${driver.boatModel},${driver.rank},${driver.driverNumber}")

            driverResult.results.forEach { result ->
                csvBuilder.append(",${result.position},${result.laps},${result.penaltyCount},${result.points}")
            }

            // Заполняем пустые колонки, если заездов меньше, чем объявлено
            for (i in driverResult.results.size until mergedResult.races.size) {
                csvBuilder.append(",,,,")
            }

            csvBuilder.append(",${driverResult.totalPoints}\n")
        }

        // Write to file
        OutputStreamWriter(file.outputStream(), StandardCharsets.UTF_16).use { writer ->
            writer.write(csvBuilder.toString())
        }

        val toast = Toast.makeText(context, "Файл создан", Toast.LENGTH_SHORT)
        toast.show()
        setState(state.value.copy(fileExist = true))
        doAfter(context, file)
    }

    fun openFile(context: Context, file: File) {
        // Open the file
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

    // Функция для добавления заезда к объединенным
    fun addRaceToMerge(raceId: Long) {
        if (!mergedRaces.contains(raceId)) {
            mergedRaces.add(raceId)
            merge()
        }
    }

    // Функция для удаления заезда из объединенных
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

    // Функция для расчета объединенных результатов
    private fun mergeRaceResults(raceDetails: List<RaceDetailUI>): MergedResult {
        val allDrivers = raceDetails.flatMap { it.drivers }.distinctBy { it.driverId }
        val pointsSystem = listOf(25, 20, 16, 13, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1)

        val driverResults = allDrivers.map { driver ->
            val results = raceDetails.map { raceDetail ->
                val driverInRace = raceDetail.drivers.find { it.driverId == driver.driverId }
                val position = raceDetail.drivers.indexOfFirst { it.driverId == driver.driverId } + 1
                val points = if (position <= pointsSystem.size) pointsSystem[position - 1] else 0

                RaceResult(
                    position = position,
                    points = points,
                    laps = raceDetail.circles.count { circle ->
                        circle.drivers.any { it.driverId == driver.driverId }
                    },
                    penaltyCount = raceDetail.circles.sumOf { circle ->
                        circle.drivers.count {
                            it.driverId == driver.driverId && !it.useDuration
                        }
                    }
                )
            }

            MergedDriverResult(
                driver = driver,
                results = results,
                totalPoints = results.sumOf { it.points }
            )
        }

        // Сортируем по сумме очков (при равных - по лучшему результату во втором заезде)
        val sortedResults = driverResults.sortedWith(compareByDescending<MergedDriverResult> { it.totalPoints }
            .thenByDescending { it.results.getOrNull(1)?.points ?: 0 }
            .thenByDescending { it.results.getOrNull(0)?.points ?: 0 })

        return MergedResult(
            races = raceDetails.map { it.raceUI },
            drivers = sortedResults
        )
    }

    fun toggleRaceSelection(show: Boolean) {
        viewModelScope.launch {
            if (show) {
                // Загружаем доступные заезды
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
