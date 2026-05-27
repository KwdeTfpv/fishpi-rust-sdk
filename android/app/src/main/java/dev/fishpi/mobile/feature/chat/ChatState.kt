package dev.fishpi.mobile.feature.chat

import dev.fishpi.mobile.core.ui.UiLoadState
import dev.fishpi.mobile.feature.chat.model.ChatComposerState
import dev.fishpi.mobile.feature.chat.model.ChatConnectionState
import dev.fishpi.mobile.feature.chat.model.ChatMessageUiModel
import dev.fishpi.mobile.feature.chat.model.ChatOverlayState
import dev.fishpi.mobile.feature.chat.barrage.ChatBarrageComposerState
import dev.fishpi.mobile.feature.chat.barrage.ChatBarrageUiModel
import dev.fishpi.mobile.feature.redpacket.RedPacketState

internal const val ChatLivenessSyncIntervalMs = 60 * 1000L

data class ChatState(
    val connection: ChatConnectionState = ChatConnectionState(),
    val messages: List<ChatMessageUiModel> = emptyList(),
    val loadState: UiLoadState = UiLoadState.Idle,
    val historyLoadState: UiLoadState = UiLoadState.Idle,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val nextHistoryPage: Int = 2,
    val hasMoreHistory: Boolean = true,
    val duplicateHistoryPageStreak: Int = 0,
    val error: String? = null,
    val scrollToBottomRequest: Int = 0,
    val keepPositionAfterPrependCount: Int = 0,
    val redPacketJumpTargetId: String? = null,
    val composer: ChatComposerState = ChatComposerState(),
    val redPacket: RedPacketState = RedPacketState(),
    val overlay: ChatOverlayState = ChatOverlayState.None,
    val unreadNewMessages: Int = 0,
    val canLoadMoreHistory: Boolean = false,
    val shouldFollowBottom: Boolean = true,
    val liveness: Double? = null,
    val barrages: List<ChatBarrageUiModel> = emptyList(),
    val barrageComposer: ChatBarrageComposerState = ChatBarrageComposerState(),
)
