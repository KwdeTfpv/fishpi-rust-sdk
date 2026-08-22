package dev.fishpi.mobile.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val FloatingBottomBarClearance: Dp = 76.dp

val LocalBottomBarClearance = staticCompositionLocalOf { 0.dp }

@Composable
fun bottomBarContentPadding(extra: Dp = 0.dp): Dp =
    LocalBottomBarClearance.current +
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
        extra
