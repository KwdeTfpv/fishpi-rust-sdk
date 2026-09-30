package dev.fishpi.mobile.feature.chat.model

import dev.fishpi.mobile.data.ComposerAttachment

data class ChatComposerState(
    val input: String = "",
    val cursorPosition: Int = 0,
    val isSending: Boolean = false,
    val isUploadingAttachment: Boolean = false,
    val emojiPanelOpen: Boolean = false,
    val attachmentPanelOpen: Boolean = false,
    val isLoadingEmojiPack: Boolean = false,
    val emojiPackError: String? = null,
    val quote: ChatQuoteUiModel? = null,
    val attachments: List<ComposerAttachment> = emptyList(),
    val mentionCandidates: List<ChatMentionCandidateUiModel> = emptyList(),
    val mentionAnchor: Int? = null,
    val mentionQuery: String? = null,
    val selectedEmojiGroupId: String = "",
)

data class ChatMentionCandidateUiModel(
    val username: String,
    val displayName: String,
    val avatarUrl: String = "",
)
