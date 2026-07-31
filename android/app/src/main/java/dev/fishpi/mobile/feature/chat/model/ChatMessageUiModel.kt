package dev.fishpi.mobile.feature.chat.model

data class ChatQuoteUiModel(
    val text: String,
    val imageUrls: List<String> = emptyList(),
)
