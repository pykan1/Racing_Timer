package com.example.racing.screen.player

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
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
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "${it.driverId} ${it.name} ${it.lastName}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.W400,
                                    modifier = Modifier
                                        .padding(start = 10.dp)
                                        .weight(1f)
                                )

                                IconButton(
                                    onClick = { viewModel.deletePlayer(it) },
                                    modifier = Modifier.padding(horizontal = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Divider(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.changeAlert()
                    }, modifier = Modifier
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

            if (state.alertDialog) {
                RaceAlertDialog(
                    title = "Новый участник",
                    onDismiss = { viewModel.changeAlert() }) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.driverName,
                            onValueChange = {
                                viewModel.changeName(it)
                            },
                            placeholder = {
                                Text(
                                    text = "Имя участника",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = MaterialTheme.typography.titleSmall.color.copy(alpha = 0.5f)
                                    )
                                )
                            })

                        OutlinedTextField(value = state.driverLastName, onValueChange = {
                            viewModel.changeLastName(it)
                        }, modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp), placeholder = {
                            Text(
                                text = "Фамилия участника",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = MaterialTheme.typography.titleSmall.color.copy(alpha = 0.5f)
                                )
                            )
                        })

                        Button(
                            enabled = state.driverName.isNotBlank() && state.driverLastName.isNotBlank(),
                            onClick = {
                                viewModel.createPlayer()
                            }, modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp, start = 16.dp, end = 16.dp, top = 20.dp)
                                .height(50.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.Create, contentDescription = null)

                            Text(
                                text = "Создать участника",
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