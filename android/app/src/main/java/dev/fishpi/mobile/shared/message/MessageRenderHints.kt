package dev.fishpi.mobile.shared.message

import dev.fishpi.mobile.shared.message.ChatMessageRenderHints


internal data class MessageRenderHints(
    val imageUrls: List<String> = emptyList(),
    val videoUrls: List<String> = emptyList(),
    val linkUrls: List<String> = emptyList(),
    val legacyChatRenderHints: ChatMessageRenderHints? = null,
)

internal fun ChatMessageRenderHints.toMessageRenderHints(): MessageRenderHints =
    MessageRenderHints(
        videoUrls = videoLinks,
        linkUrls = previewLinks,
        legacyChatRenderHints = this,
    )


