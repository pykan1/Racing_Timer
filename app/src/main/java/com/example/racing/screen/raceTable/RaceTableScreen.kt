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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.racing.ext.formatSeconds
import com.example.racing.ext.formatTimestampToDateTimeString
import com.example.racing.screen.home.RacingScreen

class RaceTableScreen(private val raceId: Long) : Screen {
    @Composable
    override fun Content() {
        val viewModel = hiltViewModel<RaceTableViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val filesPermission =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions()) { isGranted ->
                if (isGranted.all { it.value }) {
                    viewModel.createExcelFile(context)
                }

            }

        val filePermission =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    viewModel.createExcelFile(context)
                }

            }

        LaunchedEffect(viewModel) {
            viewModel.loadRace(raceId)
        }

        BackHandler {
            navigator.replace(RacingScreen())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Результаты заезда от ${state.raceDetailUI.raceUI.createRace.formatTimestampToDateTimeString()}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 10.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = state.raceDetailUI.raceUI.raceTitle,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 15.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Места",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 25.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 15.dp)
            ) {
                TableCell(
                    modifier = Modifier.weight(0.15f),
                    title = Pair("Место", false),
                    data = (1..state.raceDetailUI.drivers.size).map { Pair(it.toString(), false) }
                        .toList(),
                )
                TableCell(
                    modifier = Modifier.weight(0.30f),
                    title = Pair("Участник", false),
                    data = state.raceDetailUI.drivers.map {
                        Pair(
                            "${it.driverNumber}\n${it.name} ${it.lastName}",
                            false
                        )
                    },
                )
                TableCell(
                    modifier = Modifier.weight(0.35f),
                    title = Pair("Время", false),
                    data = state.raceDetailUI.drivers.map { driver ->
                        val time = state.raceDetailUI.circles.sumOf { circle ->
                            circle.drivers.sumOf {
                                println(it)
                                if (it.driverId == driver.driverId && !circle.isPenalty && it.useDuration) it.duration else 0
                            }
                        }.formatSeconds()
                        Pair(time, false)
                    },
                )
                TableCell(
                    modifier = Modifier.weight(0.20f),
                    title = Pair("Всего кругов", false),
                    data = state.raceDetailUI.drivers.map { driver ->
                        Pair(state.raceDetailUI.circles.count { circle ->
                            driver.driverId in circle.drivers.map { it.driverId }
                        }.toString(), false)
                    },
                )
            }
            Text(
                text = "По времени круга",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 20.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 15.dp)
            ) {
                TableCell(
                    modifier = Modifier.width(70.dp),
                    title = Pair("Участник", false),
                    data = state.raceDetailUI.drivers.map {
                        Pair(
                            it.driverNumber.toString(),
                            false
                        )
                    },
                )
                state.raceDetailUI.circles.forEachIndexed { index, item ->
                    TableCell(
                        modifier = Modifier.weight(1f),
                        title = Pair(
                            if (item.isPenalty) "Штрафной круг" else "Круг ${index + 1}",
                            item.isPenalty
                        ),
                        data = state.raceDetailUI.drivers.map { driver ->
                            item.drivers.find {
                                if (item.isPenalty) {
                                    it.driverId == driver.driverId && driver.driverId !in item.finishPenaltyDrivers
                                } else it.driverId == driver.driverId
                            }.let { driverCircle ->
                                Pair(
                                    driverCircle?.duration?.formatSeconds()
                                        .let {
                                            if (item.isPenalty && (driverCircle?.driverId
                                                    ?: 0) in item.drivers.map { it.driverId } && (driverCircle?.driverId
                                                    ?: 0) !in item.finishPenaltyDrivers
                                            ) "Не прошел штрафной круг" else it.orEmpty()
                                        },
                                    !(driverCircle?.useDuration ?: true)
                                )
                            }
                        },

                        )
                }
            }

            Text(
                text = "По кругам",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 20.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 15.dp)
            ) {
                TableCell(
                    modifier = Modifier.width(70.dp),
                    title = Pair("Круг", false),
                    data = state.raceDetailUI.circles.mapIndexed { index, circleUI ->
                        Pair(
                            if (!circleUI.isPenalty) (index + 1).toString() else "Штрафной",
                            false
                        )
                    },
                )

                TableCell(
                    modifier = Modifier.weight(1f),
                    title = Pair("Участники", false),
                    data = state.raceDetailUI.circles.map { item ->
                        Pair(
                            item.drivers.map { it.driverNumber.toString() }.joinToString(", "),
                            false
                        )
                    },
                )
            }

            Text(
                text = "Время заезда - ${state.raceDetailUI.raceUI.duration.formatSeconds()}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 20.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Порядок прохождения финишной линии - ${
                    state.raceDetailUI.raceUI.stackFinish.joinToString(
                        separator = ", "
                    )
                }",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 20.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Button(
                onClick = {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val readPermission = Environment.isExternalStorageManager()
                        if (readPermission) {
                            viewModel.createExcelFile(context)
//                            filePermission.launch(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                        } else {
                            context.openActionSettingsPage(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        }
                    } else {
                        filesPermission.launch(
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
                    .height(50.dp)
            ) {
                Text(text = "Экспорт", style = MaterialTheme.typography.titleMedium)
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
                        .height(40.dp)
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