package dev.fishpi.mobile.shared.message

internal fun interface MessageFilter {
    fun shouldHide(message: MessageUiModel): Boolean
}

