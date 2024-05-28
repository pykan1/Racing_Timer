package com.example.racing.screen.tab.tabs

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.example.racing.screen.player.PlayerScreen

object BasePlayersTab: Tab {
    @Composable
    override fun Content() {
        Navigator(PlayerScreen())
    }

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 2.toUShort(),
            title = "База участников",
            icon = null
        )
}