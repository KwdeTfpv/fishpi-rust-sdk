package dev.fishpi.mobile.core.ui

import kotlinx.coroutines.flow.StateFlow

interface UiController<S, A> {
    val state: StateFlow<S>

    fun dispatch(action: A)
}
