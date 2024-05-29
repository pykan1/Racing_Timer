package com.example.racing.screen.race

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.racing.ext.formatSeconds
import com.example.racing.ext.formatTimestampToDateTimeString
import com.example.racing.screen.base.DefaultBoxPage
import com.example.racing.screen.home.RaceAlertDialog
import com.example.racing.screen.raceTable.RaceTableScreen
import kotlinx.coroutines.delay

class RaceScreen(private val raceId: Long) : Screen {
    @Composable
    override fun Content() {
        val viewModel = hiltViewModel<RaceViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        LaunchedEffect(viewModel) {
            viewModel.loadRace(raceId)
            viewModel.loadPlayers()
        }

        LaunchedEffect(key1 = state.startTimer, key2 = state.seconds) {
            if (state.startTimer) {
                delay(1000L)
                viewModel.changeSeconds()
            }
        }

        LaunchedEffect(state.saveRace) {
            if(state.saveRace) {
                navigator.push(RaceTableScreen(state.race.raceId))
            }
        }

        DefaultBoxPage {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (state.saveDrivers) {
                    true -> {
                        RaceTimerContent(viewModel, state)
                    }

                    false -> {
                        Box(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Перед началом заезда, выберите участников из базы",
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = { viewModel.driversAlert() },
                                modifier = Modifier
                                    .height(50.dp)
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter),
                            ) {
                                Text(
                                    text = "Выбрать участников заезда",
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (state.driversAlert) {
                    RaceAlertDialog(
                        title = state.race.raceTitle.let { if (it.isBlank()) "Заезд от ${state.race.createRace.formatTimestampToDateTimeString()}" else it },
                        onDismiss = { viewModel.driversAlert() }) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {


                            OutlinedTextField(value = state.searchDriver, onValueChange = {
                                viewModel.changeSearchPlayers(it)
                            }, modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp), leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null
                                )
                            }, placeholder = {
                                Text(
                                    text = "Поиск участников",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = MaterialTheme.typography.titleSmall.color.copy(
                                            alpha = 0.5f
                                        )
                                    )
                                )
                            })

                            LazyColumn(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(15.dp),
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                items(state.drivers) { driver ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            viewModel.changeSelectPlayers(driver)
                                        }) {
                                        Checkbox(
                                            checked = driver in state.selectDrivers,
                                            onCheckedChange = {
                                                viewModel.changeSelectPlayers(driver)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        )

                                        Text(
                                            text = "${driver.driverNumber} ${driver.lastName} ${driver.name}",
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(start = 10.dp)
                                        )
                                    }
                                }
                            }

                            Button(
                                enabled = state.selectDrivers.isNotEmpty(),
                                onClick = {
                                    viewModel.saveDrivers()
                                }, modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        bottom = 20.dp,
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 15.dp
                                    )
                                    .height(50.dp)
                            ) {
                                Icon(imageVector = Icons.Outlined.Create, contentDescription = null)

                                Text(
                                    text = "Выбрать участников",
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(start = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RaceTimerContent(viewModel: RaceViewModel, state: RaceState) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = state.seconds.formatSeconds())
            Button(
                onClick = { viewModel.changeIsTimer() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(50.dp)
                    .width(220.dp)
            ) {
                Text(
                    text = if (state.startTimer) "Завершить заезд" else "Старт",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(100.dp),
                modifier = Modifier
                    .padding(top = 30.dp)
                    .heightIn(max = 250.dp)
            ) {
                items(state.selectDrivers) {

                    Column(
                        modifier = Modifier
                            .size(90.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.inverseSurface,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable(enabled = state.startTimer) { viewModel.addCircle(it) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = it.driverNumber.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 5.dp, end = 5.dp),
                        )
                        Text(
                            text = "${it.name} ${it.lastName}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 10.dp, start = 5.dp, end = 5.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                    }

                }
            }

            Text(
                text = state.driversIdStack.joinToString(", "),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

}