package dev.fishpi.mobile.shared.message

internal data class MessageRenderState(
    val items: List<ChatListItem> = emptyList(),
    val selfUsername: String = "",
    val showAvatars: Boolean = true,
    val scrollToBottomRequest: Int = 0,
    val allowScrollToBottom: Boolean = true,
    val redPacketJumpTargetId: String? = null,
    val active: Boolean = true,
    val contentTopPaddingDp: Int = 0,
)



