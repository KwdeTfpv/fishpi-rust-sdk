package dev.fishpi.mobile.core.ui

import androidx.compose.runtime.Composable

typealias UiRenderer<S, A> = @Composable (
    state: S,
    dispatch: (A) -> Unit,
) -> Unit
