package com.example.racing.screen.raceTable

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.racing.domain.models.RaceUI
import com.example.racing.ext.formatTimestampToDateTimeString
import com.example.racing.screen.home.RacingScreen
import com.example.racing.screen.home.getButtonHeight

class RaceTableScreen(private val raceId: Long) : Screen {
    @Composable
    override fun Content() {
        val viewModel = hiltViewModel<RaceTableViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current

        // Инициализация
        LaunchedEffect(viewModel) {
            viewModel.loadRace(raceId, context)
        }

        // Диалог выбора заездов
        if (state.showRaceSelection) {
            RaceSelectionDialog(
                races = state.availableRaces,
                selectedRaces = state.mergedResults.races.map { it.raceId },
                onDismiss = { viewModel.toggleRaceSelection(false) },
                onRaceSelected = { raceId, selected ->
                    if (selected) {
                        viewModel.addRaceToMerge(raceId)
                    } else {
                        viewModel.removeRaceFromMerge(raceId)
                    }
                },
                onConfirm = {
                    viewModel.toggleRaceSelection(false)
                    viewModel.loadRace(raceId, context) // Перезагружаем данные
                }
            )
        }

        val filesPermissionOpenFile =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions()) { isGranted ->
                if (isGranted.all { it.value }) {
                    viewModel.createExcelFile(context) { context, file ->
                        viewModel.openFile(context, file)
                    }
                }
            }
        val filesPermissionShareFile =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions()) { isGranted ->
                if (isGranted.all { it.value }) {
                    viewModel.createExcelFile(context) { context, file ->
                        viewModel.shareFile(context, file)
                    }
                }
            }



        LaunchedEffect(viewModel) {
            viewModel.loadRace(raceId, context)
        }

        BackHandler {
            navigator.replaceAll(RacingScreen())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Заголовок с кнопкой добавления заезда
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Сводные результаты",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 10.dp)
                )

                IconButton(
                    onClick = { viewModel.toggleRaceSelection(true) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Добавить заезд"
                    )
                }
            }
            Spacer(Modifier.size(10.dp))

            // Чипы выбранных заездов
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.mergedResults.races) { race ->
                    RaceChip(
                        race = race,
                        onClose = {
                            if(state.mergedResults.races.size != 1) {
                                viewModel.removeRaceFromMerge(race.raceId)
                                viewModel.loadRace(raceId, context)
                            }
                        }
                    )
                }
            }

            // Таблица результатов
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                // Заголовок таблицы
                ResultsTableHeader(state)

                // Данные таблицы
                state.mergedResults.drivers.forEachIndexed { position, driverResult ->
                    ResultsTableRow(
                        position = position + 1,
                        driverResult = driverResult,
                        raceCount = state.mergedResults.races.size
                    )
                }
                Divider(modifier = Modifier.fillMaxWidth().height(1.dp))

            }
            Button(
                onClick = {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val readPermission = Environment.isExternalStorageManager()
                        if (readPermission) {
                            viewModel.createExcelFile(context) { context, file ->
                                viewModel.shareFile(context, file)
                            }
                        } else {
                            context.openActionSettingsPage(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        }
                    } else {
                        filesPermissionShareFile.launch(
                            arrayOf<String>(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                        )
                    }

                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .height(getButtonHeight())
            ) {
                Text(text = "Экспорт", style = MaterialTheme.typography.titleMedium)
            }

            Button(
                onClick = {
                    // Всегда создаем новый файл, независимо от существования
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val readPermission = Environment.isExternalStorageManager()
                        if (readPermission) {
                            viewModel.createExcelFile(context) { context, file ->
                                viewModel.openFile(context, file)
                            }
                        } else {
                            context.openActionSettingsPage(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        }
                    } else {
                        filesPermissionOpenFile.launch(
                            arrayOf<String>(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .height(getButtonHeight())
            ) {
                Text(
                    text = "Скачать файл", // Всегда показываем "Скачать файл"
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

    }

    @Composable
    private fun RowScope.TableCell(
        modifier: Modifier,
        title: Pair<String, Boolean>,
        data: List<Pair<String, Boolean>>, //if CrossLine
    ) {
        Column(
            modifier = modifier
                .border(width = 1.dp, color = Color.Black),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .then(
                        if (title.second) Modifier.background(
                            MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        ) else Modifier
                    )
            ) {
                Text(
                    text = title.first,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.W600,
                    modifier = Modifier
                        .padding(5.dp)
                        .fillMaxSize(),
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis
                )
            }
            data.forEach {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    Divider(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = it.first,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .padding(5.dp)
                            .fillMaxSize(),
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (it.second) TextDecoration.LineThrough else TextDecoration.None
                    )
                }
            }
        }

    }
}

// Компонент чипа заезда
@Composable
fun RaceChip(race: RaceUI, onClose: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = race.raceTitle.ifBlank { "Заезд" },
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Удалить",
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// Заголовок таблицы
@Composable
fun ResultsTableHeader(state: RaceTableState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant).height(45.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TableHeaderCell("МЕСТО", 0.08f)
        TableHeaderCell("Фамилия Имя", 0.17f)
        TableHeaderCell("Город", 0.12f)
        TableHeaderCell("Техника", 0.12f)
        TableHeaderCell("Звание", 0.1f)
        TableHeaderCell("№", 0.05f)

        // Колонки для каждого заезда
        state.mergedResults.races.forEachIndexed { index, item ->
            Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
            Column(
                modifier = Modifier.weight(0.18f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(item.raceTitle, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.size(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Text("Место", style = MaterialTheme.typography.labelSmall)
                    Text("Круги", style = MaterialTheme.typography.labelSmall)
                    Text("Штраф", style = MaterialTheme.typography.labelSmall)
                    Text("Очки", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp))

        TableHeaderCell("Сумма", 0.1f)
    }
}

@Composable
fun RowScope.TableHeaderCell(text: String, weight: Float) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(weight),
        maxLines = 1
    )
}

// Строка таблицы
@Composable
fun ResultsTableRow(position: Int, driverResult: MergedDriverResult, raceCount: Int) {
    val driver = driverResult.driver
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TableCell(position.toString(), 0.08f)
        TableCell("${driver.lastName} ${driver.name}", 0.17f)
        TableCell(driver.city, 0.12f)
        TableCell(driver.boatModel, 0.12f)
        TableCell(driver.rank, 0.1f)
        TableCell(driver.driverNumber.toString(), 0.05f)
        // Данные по заездам
        for (i in 0 until driverResult.results.size) {
            val result = driverResult.results.getOrNull(i)
            Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
            if (result != null) {
                Row(
                    modifier = Modifier.weight(0.18f),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    TableCell(result.position.toString(), 0.25f)
                    TableCell(result.laps.toString(), 0.25f)
                    TableCell(result.penaltyCount.toString(), 0.25f)
                    TableCell(result.points.toString(), 0.25f)
                }
            } else {
                Spacer(modifier = Modifier.weight(0.13f))
            }
        }
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp))

        TableCell(driverResult.totalPoints.toString(), 0.1f)
    }
}

@Composable
fun RowScope.TableCell(text: String, weight: Float) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(weight),
        maxLines = 1
    )
}

// Диалог выбора заездов
@Composable
fun RaceSelectionDialog(
    races: List<RaceUI>,
    selectedRaces: List<Long>,
    onDismiss: () -> Unit,
    onRaceSelected: (Long, Boolean) -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите заезды для объединения") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                races.forEach { race ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = race.raceId in selectedRaces,
                            onCheckedChange = { selected ->
                                onRaceSelected(race.raceId, selected)
                            }
                        )
                        Text(
                            text = race.raceTitle.ifBlank {
                                "Заезд от ${race.createRace.formatTimestampToDateTimeString()}"
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Применить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

internal fun Context.openPage(
    action: String,
    newData: Uri? = null,
    onError: (Exception) -> Unit,
) {
    try {
        val intent = Intent(action).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            newData?.let { data = it }
        }
        startActivity(intent)
    } catch (e: Exception) {
        onError(e)
    }
}

internal fun Context.openActionSettingsPage(action: String) {
    openPage(
        action = action,
        newData = Uri.parse("package:$packageName"),
        onError = { throw Exception() }
    )
}