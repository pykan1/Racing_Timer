package com.example.racing.screen.raceTable

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import com.example.racing.ext.formatSeconds
import com.example.racing.ext.formatTimestampToDateTimeString

class RaceTableScreen(private val raceId: Long) : Screen {
    @Composable
    override fun Content() {
        val viewModel = hiltViewModel<RaceTableViewModel>()
        val state by viewModel.state.collectAsState()
        LaunchedEffect(viewModel) {
            viewModel.loadRace(raceId)
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
                    title = "Место",
                    data = (1..state.raceDetailUI.drivers.size).map { it.toString() }.toList()
                )
                TableCell(
                    modifier = Modifier.weight(0.30f),
                    title = "Участник",
                    data = state.raceDetailUI.drivers.map { "${it.driverNumber}\n${it.name} ${it.lastName}" }
                )
                TableCell(
                    modifier = Modifier.weight(0.35f),
                    title = "Время",
                    data = state.raceDetailUI.drivers.map { driver ->
                        val time = state.raceDetailUI.circles.sumOf { circle ->
                            circle.drivers.sumOf { if (it.driverId == driver.driverId) it.duration else 0 }
                        }.formatSeconds()
                        time
                    }
                )
                TableCell(
                    modifier = Modifier.weight(0.20f),
                    title = "Всего кругов",
                    data = state.raceDetailUI.drivers.map { driver ->
                        state.raceDetailUI.circles.count { circle ->
                            driver.driverId in circle.drivers.map { it.driverId }
                        }.toString()
                    }
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
                    title = "Участник",
                    data = state.raceDetailUI.drivers.map { it.driverNumber.toString() }
                )
                state.raceDetailUI.circles.forEachIndexed { index, item ->
                    TableCell(
                        modifier = Modifier.weight(1f),
                        title = "Круг ${index + 1}",
                        data = state.raceDetailUI.drivers.map { driver ->
                            item.drivers.find { it.driverId == driver.driverId }?.duration?.formatSeconds()
                                .orEmpty()
                        }
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
                    title = "Круг",
                    data = List(state.raceDetailUI.circles.size) { index -> (index + 1).toString() }
                )

                TableCell(
                    modifier = Modifier.weight(1f),
                    title = "Участники",
                    data = state.raceDetailUI.circles.map { item ->
                        item.drivers.map { it.driverNumber.toString() }.joinToString(", ")
                    }
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
                text = "Порядок прохождения финишной линии - ${state.raceDetailUI.raceUI.stackFinish.joinToString(separator = ", ")}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 20.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

    }

    @Composable
    private fun RowScope.TableCell(modifier: Modifier, title: String, data: List<String>) {
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
            ) {
                Text(
                    text = title,
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
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .padding(5.dp)
                            .fillMaxSize(),
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

    }
}