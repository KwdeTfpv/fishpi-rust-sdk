package dev.fishpi.mobile.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

internal fun Modifier.silentTap(onTap: () -> Unit): Modifier =
    pointerInput(onTap) {
        detectTapGestures(
            onTap = { onTap() },
        )
    }

internal fun Modifier.consumeTaps(): Modifier =
    silentTap {}

