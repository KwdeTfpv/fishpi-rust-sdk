package dev.fishpi.mobile.feature.breezemoon

import dev.fishpi.mobile.data.BreezemoonView
import dev.fishpi.mobile.data.EmojiGroupView
import dev.fishpi.mobile.data.EmojiItemView
import dev.fishpi.mobile.data.FishPiUser
import dev.fishpi.mobile.data.MedalView

internal data class BreezemoonState(
    val items: List<BreezemoonView> = emptyList(),
    val nextPage: Int = 2,
    val hasMore: Boolean = true,
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val isUploadingAttachment: Boolean = false,
    val attachmentPanelOpen: Boolean = false,
    val composeInput: String = "",
    val error: String? = null,
    val shouldScrollToBottom: Boolean = false,
    val emojiPanelOpen: Boolean = false,
    val emojiGroups: List<EmojiGroupView> = emptyList(),
    val emojiItems: List<EmojiItemView> = emptyList(),
    val selectedEmojiGroupId: String = "",
    val isLoadingEmojiPack: Boolean = false,
    val emojiPackError: String? = null,
    val profileUsername: String? = null,
    val profileUser: FishPiUser? = null,
    val profileMedals: List<MedalView> = emptyList(),
    val isLoadingProfile: Boolean = false,
    val profileError: String? = null,
)
