package com.example.racing.screen.tab.tabs

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.example.racing.screen.home.RacingScreen

object RacingTab: Tab {
    @Composable
    override fun Content() {
        Navigator(RacingScreen())
    }

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 1.toUShort(),
            title = "Заезды",
            icon =null
        )

}