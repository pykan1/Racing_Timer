package com.example.racing.screen.tab.tabs

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.example.racing.screen.settings.SettingsScreen

object SettingTab: Tab {
    @Composable
    override fun Content() {
        Navigator(SettingsScreen())
    }

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 3.toUShort(),
            title = "Настройки",
            icon =null
        )
}