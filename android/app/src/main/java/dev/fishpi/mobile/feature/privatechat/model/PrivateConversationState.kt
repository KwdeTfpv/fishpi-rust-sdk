package dev.fishpi.mobile.feature.privatechat.model

import dev.fishpi.mobile.shared.message.ChatQuote
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.data.EmojiGroupView
import dev.fishpi.mobile.data.EmojiItemView

internal data class PrivateConversationState(
    val selectedPeer: String? = null,
    val selectedPeerAvatar: String = "",
    val messages: List<ChatRoomMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val nextHistoryPage: Int = 2,
    val hasMoreHistory: Boolean = true,
    val status: String = "私聊未连接",
    val scrollToBottomRequest: Int = 0,
    val keepPositionAfterPrependCount: Int = 0,
    val input: String = "",
    val inputResetKey: Int = 0,
    val quote: ChatQuote? = null,
    val focusInputAfterQuote: Boolean = false,
    val isSending: Boolean = false,
    val isUploadingAttachment: Boolean = false,
    val attachmentPanelOpen: Boolean = false,
    val emojiPanelOpen: Boolean = false,
    val emojiGroups: List<EmojiGroupView> = emptyList(),
    val emojiItems: List<EmojiItemView> = emptyList(),
    val selectedEmojiGroupId: String = "",
    val isLoadingEmojiPack: Boolean = false,
    val emojiPackError: String? = null,
    val actionMessage: ChatRoomMessage? = null,
    val previewImageUrl: String? = null,
    val previewLinkUrl: String? = null,
)

