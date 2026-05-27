package dev.fishpi.mobile.feature.privatechat

import dev.fishpi.mobile.data.PrivateChatSession
import dev.fishpi.mobile.feature.privatechat.model.PrivateConversationState
import dev.fishpi.mobile.feature.privatechat.model.PrivateSessionUiModel

internal data class PrivateChatState(
    val sessions: List<PrivateSessionUiModel> = emptyList(),
    val rawSessions: List<PrivateChatSession> = emptyList(),
    val isLoadingSessions: Boolean = true,
    val error: String? = null,
    val conversation: PrivateConversationState = PrivateConversationState(),
    val totalUnread: Long = 0,
)
