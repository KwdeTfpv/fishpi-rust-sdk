package dev.fishpi.mobile.feature.chat.model

data class ChatMessageUiModel(
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
    val kind: ChatMessageKind = ChatMessageKind.Message,
    val isMine: Boolean = false,
    val isRevoked: Boolean = false,
    val reactions: List<ChatReactionUiModel> = emptyList(),
    val currentUserReaction: String = "",
    val quote: ChatQuoteUiModel? = null,
    val redPacket: ChatRedPacketUiModel? = null,
)

enum class ChatMessageKind {
    Message,
    System,
    Custom,
}

data class ChatReactionUiModel(
    val value: String,
    val emoji: String,
    val count: Long,
    val selected: Boolean,
)

data class ChatQuoteUiModel(
    val text: String,
    val imageUrls: List<String> = emptyList(),
)

data class ChatRedPacketUiModel(
    val type: String,
    val typeName: String,
    val money: Long,
    val count: Long,
    val got: Long,
    val message: String,
    val summary: String = "",
    val finished: Boolean,
    val openable: Boolean,
    val needGesture: Boolean,
    val receivers: List<String> = emptyList(),
    val gesture: Int? = null,
)
