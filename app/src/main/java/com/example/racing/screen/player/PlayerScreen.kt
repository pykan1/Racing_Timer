package com.example.racing.screen.player

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.DirectionsBoat
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import com.example.racing.screen.base.DefaultBoxPage
import com.example.racing.screen.home.RaceAlertDialog

class PlayerScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = hiltViewModel<PlayerViewModel>()
        val state by viewModel.state.collectAsState()
        DefaultBoxPage {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .animateContentSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 50.dp),
                ) {
                    items(state.drivers) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    // Первая строка - номер и ФИО
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Номер участника в кружке
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape
                                                )
                                        ) {
                                            Text(
                                                text = it.driverNumber.toString(),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }

                                        // Фамилия и имя
                                        Column(
                                            modifier = Modifier
                                                .padding(start = 12.dp)
                                                .weight(1f)
                                        ) {
                                            Text(
                                                text = it.lastName,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = it.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Кнопки действий
                                        Row {
                                            IconButton(
                                                onClick = { viewModel.selectEditDriver(it) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Edit,
                                                    contentDescription = "Edit",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.deletePlayer(it) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }

                                    // Вторая строка - дополнительная информация
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Город
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1.5f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.LocationOn,
                                                contentDescription = "City",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = it.city,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(start = 4.dp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                overflow = TextOverflow.Ellipsis,
                                                maxLines = 1,
                                            )
                                        }
                                        Spacer(Modifier.size(5.dp))

                                        // Звание
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.MilitaryTech,
                                                contentDescription = "Rank",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = it.rank,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(start = 4.dp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                overflow = TextOverflow.Ellipsis,
                                                maxLines = 1,
                                            )
                                        }
                                        Spacer(Modifier.size(5.dp))

                                        // Модель техники (если нужно)
                                        if (it.boatModel.isNotBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.DirectionsBoat,
                                                    contentDescription = "Boat",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = it.boatModel,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.padding(start = 4.dp),
                                                    overflow = TextOverflow.Ellipsis,
                                                    maxLines = 1,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { viewModel.changeAlert() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp, start = 25.dp, end = 25.dp)
                        .height(50.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Icon(imageVector = Icons.Outlined.Edit, contentDescription = null)
                    Text(
                        text = "Создать участника",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }

            // Объединенное условие для создания/редактирования
            if (state.alertDialog || state.selectEditDriver != null) {
                CreateDriverDialog(
                    title = if (state.selectEditDriver != null) "Редактировать участника" else "Новый участник",
                    driverNumber = state.driverNumber?.toString(),
                    driverName = state.driverName,
                    driverLastName = state.driverLastName,
                    city = state.city,
                    boatModel = state.boatModel,
                    rank = state.rank,
                    team = state.team,
                    onDismiss = {
                        viewModel.clearAlert()
                        viewModel.selectEditDriver(null)
                    },
                    onDriverNumberChange = { viewModel.changeDriverNumber(it) },
                    onNameChange = { viewModel.changeName(it) },
                    onLastNameChange = { viewModel.changeLastName(it) },
                    onCityChange = { viewModel.changeCity(it) },
                    onBoatModelChange = { viewModel.changeBoatModel(it) },
                    onRankChange = { viewModel.changeRank(it) },
                    onTeamChange = { viewModel.changeTeam(it) },
                    onConfirm = {
                        if (state.selectEditDriver != null) {
                            viewModel.updatePlayer()
                        } else {
                            viewModel.createPlayer()
                        }
                    },
                    isCreateEnabled = state.driverName.isNotBlank() && state.driverLastName.isNotBlank(),
                    confirmButtonText = if (state.selectEditDriver != null) "Сохранить" else "Создать"
                )
            }
        }
    }
}

@Composable
fun CreateDriverDialog(
    title: String,
    driverNumber: String?,
    driverName: String,
    driverLastName: String,
    city: String,
    boatModel: String,
    rank: String,
    team: String,
    onDismiss: () -> Unit,
    onDriverNumberChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onBoatModelChange: (String) -> Unit,
    onRankChange: (String) -> Unit,
    onTeamChange: (String) -> Unit,
    onConfirm: () -> Unit, // Переименовано с onCreate
    isCreateEnabled: Boolean,
    confirmButtonText: String // Новый параметр для текста кнопки
) {
    RaceAlertDialog(
        title = title,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = driverNumber.orEmpty(),
                onValueChange = onDriverNumberChange,
                label = {
                    Text(
                        text = "Номер участника",
                        style = MaterialTheme.typography.titleSmall
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                value = driverName,
                onValueChange = onNameChange,
                label = {
                    Text(
                        text = "Имя участника",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            )

            OutlinedTextField(
                value = driverLastName,
                onValueChange = onLastNameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                label = {
                    Text(
                        text = "Фамилия участника",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            )

            OutlinedTextField(
                value = city,
                onValueChange = onCityChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                label = {
                    Text(
                        text = "Город",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            )

            OutlinedTextField(
                value = boatModel,
                onValueChange = onBoatModelChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                label = {
                    Text(
                        text = "Модель техники",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            )

            OutlinedTextField(
                value = rank,
                onValueChange = onRankChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                label = {
                    Text(
                        text = "Звание",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            )

            OutlinedTextField(
                value = team,
                onValueChange = onTeamChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                label = {
                    Text(
                        text = "Команда",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            )

            Button(
                enabled = isCreateEnabled,
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp, start = 16.dp, end = 16.dp, top = 20.dp)
                    .height(50.dp)
            ) {
                Icon(imageVector = Icons.Outlined.Create, contentDescription = null)
                Text(
                    text = confirmButtonText, // Используем переданный текст
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        }
    }
}