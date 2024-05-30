package com.example.racing.screen.settings

import androidx.lifecycle.viewModelScope
import com.example.racing.data.local.repositoryImpl.StoreManager
import com.example.racing.screen.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject


@HiltViewModel
class SettingsViewModel @Inject constructor(private val storeManager: StoreManager) :
    BaseViewModel<SettingsState>(initialVal = SettingsState.InitState) {

    init {
        viewModelScope.launch {
            storeManager.getSettings().collect {
                setState(
                    state.value.copy(settingsUI = it)
                )
            }
        }
    }


    fun changeBleeper() {
        viewModelScope.launch {
            val setting = state.value.settingsUI.copy(
                bleeper = !state.value.settingsUI.bleeper
            )
            setState(
                state.value.copy(
                    settingsUI = setting
                )
            )
            storeManager.saveSettings(settingsUI = setting)
        }
    }

    fun changeVibration() {
        viewModelScope.launch {
            val settingsState = state.value.copy(
                settingsUI = state.value.settingsUI.copy(
                    vibration = !state.value.settingsUI.vibration
                )
            )
            setState(
                settingsState
            )
            storeManager.saveSettings(settingsUI = settingsState.settingsUI)
        }
    }

    fun changeEmail(it: String) {
        runBlocking {
            viewModelScope.launch {
                val settings = state.value.settingsUI.copy(
                    email = it
                )
                setState(
                    state.value.copy(
                        settingsUI = settings
                    )
                )

                storeManager.saveSettings(settingsUI = settings)
            }
        }
    }


}