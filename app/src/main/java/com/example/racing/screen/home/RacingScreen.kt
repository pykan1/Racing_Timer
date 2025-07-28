package com.example.racing.screen.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.racing.domain.models.RaceUI
import com.example.racing.ext.formatTimestampToDateTimeString
import com.example.racing.screen.base.DefaultBoxPage
import com.example.racing.screen.race.RaceScreen
import com.example.racing.screen.raceTable.RaceTableScreen

class RacingScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = hiltViewModel<RacingViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(viewModel) {
            viewModel.loadData()
        }
        DefaultBoxPage {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .animateContentSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 70.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.races) { race ->
                        RaceCard(
                            race = race,
                            onRaceClick = {
                                if (race.finish) {
                                    navigator.push(RaceTableScreen(race.raceId))
                                } else {
                                    navigator.push(RaceScreen(race.raceId))
                                }
                            },
                            onCopyClick = { viewModel.copyRace(race) },
                            onDeleteClick = { viewModel.deleteRace(race) }
                        )
                    }
                }

                Button(
                    onClick = { viewModel.changeAlertDialog() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp, start = 25.dp, end = 25.dp)
                        .height(50.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Icon(imageVector = Icons.Outlined.Edit, contentDescription = null)
                    Text(
                        text = "Создать заезд",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }

            if (state.alertDialog) {
                RaceAlertDialog(
                    title = "Новый заезд",
                    onDismiss = { viewModel.changeAlertDialog() }) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.raceTitle,
                            onValueChange = { viewModel.changeRaceTitle(it) },
                            placeholder = {
                                Text(
                                    text = "Название заезда",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        )

                        OutlinedTextField(
                            value = state.findPlayer,
                            onValueChange = { viewModel.changeSearchPlayers(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null)
                            },
                            placeholder = {
                                Text(
                                    text = "Поиск участников",
                                    style = MaterialTheme.typography.titleSmall
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
                            items(state.players) { driver ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        viewModel.changeSelectPlayers(driver)
                                    }) {
                                    Checkbox(
                                        checked = driver in state.selectPlayers,
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
                            enabled = state.selectPlayers.isNotEmpty(),
                            onClick = { viewModel.saveRace() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp, start = 16.dp, end = 16.dp, top = 15.dp)
                                .height(50.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.Create, contentDescription = null)
                            Text(
                                text = "Создать заезд",
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
fun RaceCard(
    race: RaceUI,
    onRaceClick: () -> Unit,
    onCopyClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onRaceClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка состояния
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (race.finish) MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (race.finish) Icons.AutoMirrored.Filled.Assignment
                    else Icons.Default.Timer,
                    contentDescription = null,
                    tint = if (race.finish) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Информация о заезде
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = if (race.raceTitle.isBlank()) "Заезд от ${race.createRace.formatTimestampToDateTimeString()}"
                    else race.raceTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = race.createRace.formatTimestampToDateTimeString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Кнопки действий
            Row {
                IconButton(
                    onClick = onCopyClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Копировать",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
internal fun RaceAlertDialog(
    modifier: Modifier = Modifier,
    title: String,
    onDismiss: () -> Unit,
    content: @Composable (() -> Unit?)? = null,
) {
    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = { onDismiss() }
    ) {
        Column(
            modifier = modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                )
                .heightIn(max = 500.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onDismiss() }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
            Divider(modifier = Modifier.fillMaxWidth())
            content?.invoke()
        }
    }
}