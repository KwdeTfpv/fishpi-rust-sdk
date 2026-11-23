package dev.fishpi.mobile.feature.chat

import dev.fishpi.mobile.feature.chat.model.ChatConnectionState
import dev.fishpi.mobile.feature.chat.barrage.ChatBarrageComposerState
import dev.fishpi.mobile.feature.chat.barrage.ChatBarrageUiModel
import dev.fishpi.mobile.feature.chat.appearance.AppearanceShopUiState
import dev.fishpi.mobile.data.ChatAppearanceSnapshot

internal const val ChatLivenessSyncIntervalMs = 60 * 1000L

enum class ChatAppearancePhase {
    Loading,
    Editing,
    Submitting,
    Verifying,
    Synced,
}

data class ChatAppearanceUiState(
    val current: ChatAppearanceSnapshot? = null,
    val bubbleId: String = "classic",
    val frameId: String = "none",
    val phase: ChatAppearancePhase = ChatAppearancePhase.Loading,
    val error: String? = null,
    val hint: String? = null,
    val pendingIdempotencyKey: String? = null,
    val verifyOnly: Boolean = false,
    val needsAuthorization: Boolean = false,
    val needsCaptcha: Boolean = false,
) {
    val isLoading: Boolean get() = phase == ChatAppearancePhase.Loading
    val isSaving: Boolean
        get() = phase == ChatAppearancePhase.Submitting || phase == ChatAppearancePhase.Verifying
}

data class ChatState(
    val connection: ChatConnectionState = ChatConnectionState(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val nextHistoryPage: Int = 2,
    val hasMoreHistory: Boolean = true,
    val duplicateHistoryPageStreak: Int = 0,
    val error: String? = null,
    val scrollToBottomRequest: Int = 0,
    val keepPositionAfterPrependCount: Int = 0,
    val redPacketJumpTargetId: String? = null,
    val locateMessageId: String? = null,
    val locateMessageRequest: Int = 0,
    val unreadNewMessages: Int = 0,
    val shouldFollowBottom: Boolean = true,
    val liveness: Double? = null,
    val barrages: List<ChatBarrageUiModel> = emptyList(),
    val barrageComposer: ChatBarrageComposerState = ChatBarrageComposerState(),
    val chatAppearance: ChatAppearanceUiState = ChatAppearanceUiState(),
    val appearanceShop: AppearanceShopUiState = AppearanceShopUiState(),
)
