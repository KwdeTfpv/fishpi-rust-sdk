package dev.fishpi.mobile.feature.privatechat

import dev.fishpi.mobile.core.ui.UiEffect
import dev.fishpi.mobile.data.PrivateChatNotice

internal sealed interface PrivateChatEffect : UiEffect {
    data class ShowError(val message: String) : PrivateChatEffect
    data class NotifyPrivateMessage(val notice: PrivateChatNotice) : PrivateChatEffect
    data class DetailActiveChanged(val active: Boolean) : PrivateChatEffect
    data class TotalUnreadChanged(val unread: Long) : PrivateChatEffect
}
