package com.example.racing.data.local.repositoryImpl

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.racing.domain.models.SettingsUI
import com.example.racing.screen.base.BLEEPER
import com.example.racing.screen.base.EMAIL
import com.example.racing.screen.base.VIBRATION
import kotlinx.coroutines.flow.map

private val Context.dateStore: DataStore<Preferences> by preferencesDataStore("settings")

class StoreManager(private val context: Context) {
    suspend fun saveSettings(settingsUI: SettingsUI) {
        context.dateStore.edit { pref ->
            pref[booleanPreferencesKey(BLEEPER)] = settingsUI.bleeper
            pref[booleanPreferencesKey(VIBRATION)] = settingsUI.vibration
            pref[stringPreferencesKey(EMAIL)] = settingsUI.email
        }
    }

    fun getSettings() = context.dateStore.data.map { pref ->
        return@map SettingsUI(
            bleeper = pref[booleanPreferencesKey(BLEEPER)]?: false,
            vibration = pref[booleanPreferencesKey(VIBRATION)]?: false,
            email = pref[stringPreferencesKey(EMAIL)].orEmpty()
        )

    }
}