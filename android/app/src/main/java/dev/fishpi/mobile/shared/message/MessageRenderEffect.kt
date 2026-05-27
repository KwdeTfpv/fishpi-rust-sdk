package dev.fishpi.mobile.shared.message

internal sealed interface MessageRenderEffect {
    data object ScrollToBottom : MessageRenderEffect
    data class ScrollToMessage(val messageId: String) : MessageRenderEffect
}

