package com.example.racing.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import com.example.racing.screen.base.DefaultBoxPage

class SettingsScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = hiltViewModel<SettingsViewModel>()
        val state by viewModel.state.collectAsState()
        DefaultBoxPage {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                OutlinedTextField(value = state.settingsUI.email, onValueChange = {
                    viewModel.changeEmail(it)
                }, modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp), leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AlternateEmail,
                        contentDescription = null
                    )
                }, placeholder = {
                    Text(
                        text = "Email для отправки результатов",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = MaterialTheme.typography.titleSmall.color.copy(
                                alpha = 0.5f
                            )
                        )
                    )
                })

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Виброотклик",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Switch(checked = state.settingsUI.vibration, onCheckedChange = {
                        viewModel.changeVibration()
                    }, modifier = Modifier.size(32.dp))

                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bleeper",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Switch(checked = state.settingsUI.bleeper, onCheckedChange = {
                        viewModel.changeBleeper()
                    }, modifier = Modifier.size(32.dp))

                }


            }
        }
    }
}