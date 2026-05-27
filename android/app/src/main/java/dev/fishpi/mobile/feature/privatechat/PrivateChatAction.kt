package dev.fishpi.mobile.feature.privatechat

import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.data.EmojiItemView
import dev.fishpi.mobile.data.PrivateChatNotice
import dev.fishpi.mobile.data.PrivateChatSession

internal sealed interface PrivateChatAction {
    data object RefreshSessions : PrivateChatAction
    data class OpenSession(val session: PrivateChatSession) : PrivateChatAction
    data class OpenPeer(val peer: String) : PrivateChatAction
    data object CloseConversation : PrivateChatAction
    data object LoadMoreHistory : PrivateChatAction
    data class ApplyNotice(val notice: PrivateChatNotice, val notify: Boolean = false) : PrivateChatAction
    data class ChangeInput(val value: String) : PrivateChatAction
    data object SendText : PrivateChatAction
    data class UploadAttachment(val path: String) : PrivateChatAction
    data object ToggleEmoji : PrivateChatAction
    data object CloseEmoji : PrivateChatAction
    data class SelectEmojiGroup(val groupId: String) : PrivateChatAction
    data class PickEmoji(val item: EmojiItemView) : PrivateChatAction
    data object OpenTools : PrivateChatAction
    data object CloseTools : PrivateChatAction
    data object InputFocused : PrivateChatAction
    data object CancelQuote : PrivateChatAction
    data class QuoteMessage(val message: ChatRoomMessage) : PrivateChatAction
    data class RepeatMessage(val message: ChatRoomMessage) : PrivateChatAction
    data class RevokeMessage(val message: ChatRoomMessage) : PrivateChatAction
    data class ShowMessageActions(val message: ChatRoomMessage) : PrivateChatAction
    data object DismissMessageActions : PrivateChatAction
    data class ShowImagePreview(val url: String) : PrivateChatAction
    data object DismissImagePreview : PrivateChatAction
    data class ShowLinkPreview(val url: String) : PrivateChatAction
    data object DismissLinkPreview : PrivateChatAction
    data object KeepPositionConsumed : PrivateChatAction
    data class ShowError(val message: String) : PrivateChatAction
    data object ClearError : PrivateChatAction
}
