package dev.fishpi.mobile.shared.message

import dev.fishpi.mobile.data.ChatRoomMessage

internal data class RepeatStackInfo(
    val count: Int,
    val participantUsernames: List<String>,
    val participantAvatars: List<String>,
)

internal data class ChatListItem(
    val message: ChatRoomMessage,
    val separator: String?,
    val renderHints: ChatMessageRenderHints,
    val repeatStack: RepeatStackInfo? = null,
)

