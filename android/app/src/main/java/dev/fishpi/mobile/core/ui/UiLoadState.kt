package dev.fishpi.mobile.core.ui

sealed interface UiLoadState {
    data object Idle : UiLoadState
    data object Loading : UiLoadState
    data object Refreshing : UiLoadState
    data object LoadingMore : UiLoadState
    data object Empty : UiLoadState
    data class Error(val message: String) : UiLoadState
}
