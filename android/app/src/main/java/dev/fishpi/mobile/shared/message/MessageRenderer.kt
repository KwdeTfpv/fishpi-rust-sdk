package dev.fishpi.mobile.shared.message

import androidx.compose.runtime.Composable

internal fun interface MessageRenderer {
    @Composable
    fun Render(
        state: MessageRenderState,
        dispatch: (MessageAction) -> Unit,
    )
}

