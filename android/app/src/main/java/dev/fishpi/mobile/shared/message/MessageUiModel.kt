package dev.fishpi.mobile.shared.message

import dev.fishpi.mobile.data.ChatRoomMessage

internal data class MessageUiModel(
    val id: String,
    val authorUsername: String,
    val authorDisplayName: String,
    val authorAvatarUrl: String,
    val content: String,
    val markdown: String,
    val html: String,
    val imageUrls: List<String> = emptyList(),
    val linkUrls: List<String> = emptyList(),
    val time: String,
    val client: String = "",
    val kind: MessageKind = MessageKind.Message,
    val isMine: Boolean = false,
    val isRevoked: Boolean = false,
    val reactions: List<MessageReactionUiModel> = emptyList(),
    val redPacket: MessageRedPacketUiModel? = null,
    val legacyChatRoomMessage: ChatRoomMessage? = null,
)

internal enum class MessageKind {
    Message,
    System,
    Custom,
}

internal data class MessageReactionUiModel(
    val value: String,
    val emoji: String,
    val count: Long,
    val selected: Boolean,
)

internal data class MessageRedPacketUiModel(
    val type: String,
    val typeName: String,
    val money: Long,
    val count: Long,
    val got: Long,
    val message: String,
    val finished: Boolean,
    val openable: Boolean,
    val needGesture: Boolean,
)

