package com.example.racing.screen.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.bottomSheet.BottomSheetNavigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.example.racing.screen.home.RacingScreen
import com.example.racing.screen.tab.MainTabScreen

@Composable
fun RootApp() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        BottomSheetNavigator(
            sheetShape = RoundedCornerShape(
                topStartPercent = 8,
                topEndPercent = 8
            )//, skipHalfExpanded = false
        ) {

            Navigator(MainTabScreen()) {
                CompositionLocalProvider(
                    RootNavigator provides it
                ) {
                    SlideTransition(it)
                }


            }
        }
    }

}