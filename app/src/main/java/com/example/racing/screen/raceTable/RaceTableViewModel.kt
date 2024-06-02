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

    fun loadRace(id: Long, context: Context) {
        viewModelScope.launch {
            val raceDetail = raceRepositoryImpl.getRaceDetail(id)
            val rankDrivers = rankDrivers(raceDetail.circles, raceDetail.drivers).filterNotNull()
            val fileName =
                "/${raceDetail.raceUI.raceTitle}_${raceDetail.raceUI.createRace.formatTimestampToDateTimeString()}.csv"
            // Получение директории приложения
            val path = context.getExternalFilesDir(null)
            // Проверка существования файла
            val isExist = path?.let {
                val directory = File(it.absolutePath)

                if (!directory.exists()) {
                    val directoryCreated = directory.mkdirs()
                    if (!directoryCreated) {
                        Log.e("CreateExcelFile", "Failed to create directory")
                    }
                }

                File(directory, fileName).exists()
            } ?: false
            setState(
                state.value.copy(
                    raceDetailUI = raceDetail.copy(
                        drivers = rankDrivers,
                        circles = raceDetail.circles),
                    fileExist = isExist
                )
            )
            storeManager.getSettings().collect {
                setState(
                    state.value.copy(
                        settingsUI = it
                    )
                )
            }
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
                    createExcel(file, context, doAfter)
                } else {
                    doAfter(context, file)
                    setState(state.value.copy(fileExist = true))
                    Log.e("CreateExcelFile", "File already exists")
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
        val csvBuilder = StringBuilder()

        // Header rows
        csvBuilder.append("Результаты заезда от ${state.value.raceDetailUI.raceUI.createRace.formatTimestampToDateTimeString()}\n")
        csvBuilder.append("Название заезда,${state.value.raceDetailUI.raceUI.raceTitle}\n")
        csvBuilder.append("\n")
        csvBuilder.append("Места\n")
        // First table headers
        csvBuilder.append("Место,Участник,Имя,Фамилия,Время,Всего кругов\n")

        // First table data
        state.value.raceDetailUI.drivers.forEachIndexed { index, driver ->
            val time = state.value.raceDetailUI.circles.sumOf { circle ->
                circle.drivers.sumOf {
                    if (it.driverId == driver.driverId && it.useDuration) it.duration else 0
                }
            }.formatSeconds()

            val totalCircles = state.value.raceDetailUI.circles.count { circle ->
                driver.driverId in circle.drivers.map { it.driverId }
            }

            csvBuilder.append("${index + 1},${driver.driverNumber},${driver.name},${driver.lastName},${time},${totalCircles}\n")
        }
        csvBuilder.append("\n")
        csvBuilder.append("По времени круга")
        // Second table headers
        csvBuilder.append("\nУчастник,")
        state.value.raceDetailUI.circles.forEachIndexed { index, circle ->
            csvBuilder.append("Круг ${index + 1},")
        }
        csvBuilder.append("\n")

        // Second table data
        state.value.raceDetailUI.drivers.forEach { driver ->
            csvBuilder.append("${driver.driverNumber},")
            state.value.raceDetailUI.circles.forEach { circle ->
                val driverCircle = circle.drivers.find { it.driverId == driver.driverId }
                val time = driverCircle?.duration?.formatSeconds() ?: ""
                val circleData =
                    if (driver.driverId in circle.penaltyFor && driver.driverId !in circle.finishPenaltyDrivers) {
                        "$time (Не прошел штрафной буй)"
                    } else {
                        if (driverCircle?.useDuration == true && driverCircle.driverId in circle.finishPenaltyDrivers) {
                            "$time(с буем)"
                        } else {
                            time
                        }
                    }
                csvBuilder.append("${circleData},")
            }
            csvBuilder.append("\n")
        }
        csvBuilder.append("\nПо кругам")
        // Third table headers
        csvBuilder.append("\nКруг,Участники\n")

        // Third table data
        state.value.raceDetailUI.circles.forEachIndexed { index, circle ->
            csvBuilder.append("Круг ${(index + 1)},")
            csvBuilder.append(circle.drivers.joinToString(", ") { it.driverNumber.toString() })
            csvBuilder.append("\n")
        }

        csvBuilder.append("\nВремя заезда,${state.value.raceDetailUI.raceUI.duration.formatSeconds()}\n")
        csvBuilder.append("Порядок прохождения финишной линии,")
        state.value.raceDetailUI.raceUI.stackFinish.forEach {
            csvBuilder.append("$it,")
        }

        // Write to file with UTF-16 encoding
        OutputStreamWriter(file.outputStream(), StandardCharsets.UTF_16).use { writer ->
            writer.write(csvBuilder.toString())
        }

        // Notify user
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

}
