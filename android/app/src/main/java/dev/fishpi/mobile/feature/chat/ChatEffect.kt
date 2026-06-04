package dev.fishpi.mobile.feature.chat

import dev.fishpi.mobile.core.ui.UiEffect

sealed interface ChatEffect : UiEffect {
    data class ShowMessage(val message: String) : ChatEffect
    data class ShowError(val message: String) : ChatEffect
    data class OpenUserProfile(val username: String) : ChatEffect
    data class OpenExternalUrl(val url: String) : ChatEffect
    data object RequestInputFocus : ChatEffect
    data object ScrollToBottom : ChatEffect
}
