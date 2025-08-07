package com.example.racing.screen.race

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.racing.ext.formatSeconds
import com.example.racing.ext.formatTimestampToDateTimeString
import com.example.racing.screen.base.DefaultBoxPage
import com.example.racing.screen.home.RaceAlertDialog
import com.example.racing.screen.home.getButtonHeight
import com.example.racing.screen.raceTable.RaceTableScreen
import kotlinx.coroutines.delay

class RaceScreen(private val raceId: Long) : Screen {

    override val key: ScreenKey = raceId.toString()

    @Composable
    override fun Content() {
        val viewModel = hiltViewModel<RaceViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        LaunchedEffect(viewModel) {
            viewModel.loadRace(raceId)
            viewModel.loadPlayers()
        }

        LaunchedEffect(key1 = state.startTimer, key2 = state.seconds) {
            if (state.startTimer) {
                delay(995L)
                viewModel.changeSeconds()
            }
        }

        LaunchedEffect(state.saveRace) {
            if (state.saveRace) {
                navigator.push(RaceTableScreen(state.race.raceId))
            }
        }

        DefaultBoxPage {
            Scaffold(floatingActionButton = {
                if (state.startTimer) {
                    state.circles.findLast {
                        (state.driversIdStack.lastOrNull()
                            ?: -1) in it.drivers.map { it.driverNumber }
                    }?.let { circle ->
                        circle.drivers.find {
                            it.driverNumber == (state.driversIdStack.lastOrNull() ?: -1)
                        }?.let {
                            if (it.useDuration) {
                                FloatingActionButton(
                                    modifier = Modifier.size(155.dp),
                                    onClick = {
                                        sound(state = state, context = context, vib = vib)
                                        viewModel.minusCircle(driverUI = it)
                                    }
                                ) {
                                    Text(
                                        text = "ШТРАФ (${it.driverNumber})",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            }
                        }
                    }
                } else {
                    FloatingActionButton(
                        modifier = Modifier.size(72.dp),
                        onClick = {
                            viewModel.driversAlert()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            modifier = Modifier
                                .size(44.dp)
                                .align(Alignment.Center),
                            contentDescription = null
                        )
                    }
                }
            }) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(it),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (state.selectDrivers.isEmpty()) {
                        false -> {
                            RaceTimerContent(viewModel, state)
                        }

                        true -> {
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
                                        .height(getButtonHeight())
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
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .verticalScroll(
                                        rememberScrollState()
                                    )
                            ) {
                                OutlinedTextField(
                                    value = state.searchDriver, onValueChange = {
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
                                                color = MaterialTheme.typography.titleSmall.color
                                            )
                                        )
                                    })

                                LazyColumn(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                        .heightIn(max = 300.dp),
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
                                        .height(getButtonHeight())
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Create,
                                        contentDescription = null
                                    )

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

            // В UI
            if (state.showResetConfirmation) {
                AlertDialog(
                    onDismissRequest = { viewModel.showResetConfirmation(false) },
                    title = { Text("Подтверждение фальстарта") },
                    text = { Text("Все текущие результаты будут сброшены. Вы уверены?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.resetRace(raceId)
                                viewModel.showResetConfirmation(false)
                            }
                        ) {
                            Text("Да")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = { viewModel.showResetConfirmation(false) }
                        ) {
                            Text("Отмена")
                        }
                    }
                )
            }   // В UI
            if (state.showEndRace) {
                AlertDialog(
                    onDismissRequest = { viewModel.showEndRace(false) },
                    title = { Text("Подтверждение завершения заезда") },
                    text = { Text("Все текущие результаты будут сохранены. Заезд будет закончен. Закончить?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                sound(state, vib, context)
                                viewModel.changeIsTimer()
                                viewModel.showEndRace(false)
                            }
                        ) {
                            Text("Да")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = { viewModel.showEndRace(false) }
                        ) {
                            Text("Отмена")
                        }
                    }
                )
            }
        }
    }

    @Composable
    private fun RaceTimerContent(viewModel: RaceViewModel, state: RaceState) {
        val context = LocalContext.current
        val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.seconds.formatSeconds(),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 5.dp)
            )
            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()

            ) {
                Button(
                    onClick = {
                        if (state.startTimer) {
                            viewModel.showEndRace(true)
                        } else {
                            sound(state, vib, context)
                            viewModel.changeIsTimer()
                        }
                    },
                    modifier = Modifier
                        .animateContentSize()
                        .weight(1f)
                        .height(getButtonHeight())
                        .animateContentSize()
                ) {
                    Text(
                        text = if (state.startTimer) "Завершить заезд" else "Старт",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (state.startTimer) {
                    Spacer(Modifier.size(16.dp))
                    Button(
                        onClick = {
                            viewModel.showResetConfirmation(true)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier
                            .width(220.dp)
                            .height(getButtonHeight())
                    ) {
                        Text(
                            text = "Фальстарт",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(top = 30.dp, bottom = 5.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(state.selectDrivers) { driver ->
                    Column(
                        modifier = Modifier
                            .size(120.dp)
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.inverseSurface,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clip(RoundedCornerShape(10.dp)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(enabled = state.startTimer) {
                                    sound(state, vib, context)
                                    viewModel.addCircle(driver, useDuration = true)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = driver.driverNumber.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(start = 5.dp, end = 5.dp),
                            )
                        }
                    }

                }
            }

            Text(
                text = state.driversIdStack.joinToString(", "),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 15.dp),
            )
        }
    }

    private fun sound(
        state: RaceState,
        vib: Vibrator,
        context: Context
    ) {
        println(state.settings)
        if (state.settings.vibration) {
            if (Build.VERSION.SDK_INT >= 26) {

                vib.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vib.vibrate(400)
            }
        }
        if (state.settings.bleeper) {
            val notification =
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone =
                RingtoneManager.getRingtone(context, notification)
            ringtone.play()
        }
    }

}