package com.example.racing.screen.tab

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import com.example.racing.screen.tab.tabs.BasePlayersTab
import com.example.racing.screen.tab.tabs.RacingTab
import com.example.racing.screen.tab.tabs.SettingTab
import kotlinx.coroutines.launch

class MainTabScreen(
    private val tab: Tab = RacingTab,
) :
    Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val tabs = listOf(
            RacingTab, BasePlayersTab, SettingTab
        )
        val navigator = LocalNavigator.currentOrThrow
        val drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        TabNavigator(
            tab = tab
        ) {
            val tabNavigator = LocalTabNavigator.current
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        Scaffold(topBar = {
                            TopAppBar(
                                modifier = Modifier.fillMaxWidth(),
                                title = {
                                    Text(
                                        text = "Uglich-Extreme",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.W600,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(it),
                            ) {
                                tabs.forEachIndexed { index, tab ->
                                    Column(modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            tabNavigator.current = tab
                                            scope.launch {
                                                drawerState.close()
                                            }
                                        }) {
                                        Text(
                                            text = tab.options.title,
                                            fontSize = 16.sp,
                                            modifier = Modifier
                                                .padding(start = 16.dp, top = 10.dp)
                                        )

                                        Divider(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) {
                Scaffold(topBar = {

                    TopAppBar(
                        navigationIcon = {
                            if (navigator.canPop) {
                                IconButton(onClick = { navigator.pop() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                        contentDescription = null, modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .clickable {
                                            scope.launch {
                                                if (drawerState.isOpen) {
                                                    drawerState.close()
                                                } else {
                                                    drawerState.open()
                                                }
                                            }
                                        },
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        title = {
                            Text(
                                text = it.current.options.title,
                                fontWeight = FontWeight.W500,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                    )
                }) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        it.current.Content()
                    }
                }
            }
        }
    }
}