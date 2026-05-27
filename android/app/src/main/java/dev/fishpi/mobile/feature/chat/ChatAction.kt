package dev.fishpi.mobile.feature.chat

import dev.fishpi.mobile.data.ChatRoomMessage

sealed interface ChatAction {
    data object RefreshHistory : ChatAction
    data object LoadMoreHistory : ChatAction
    data object Reconnect : ChatAction
    data object JumpToBottom : ChatAction
    data object MarkUnreadMessagesRead : ChatAction
    data class FollowBottomChanged(val value: Boolean) : ChatAction
    data class NewMessagesRemainingChanged(val remaining: Int) : ChatAction

    data class ChangeInput(val value: String) : ChatAction
    data class ChangeCursor(val position: Int) : ChatAction
    data class SendText(val content: String) : ChatAction
    data class SetTopic(val topic: String) : ChatAction
    data class UploadAttachment(val path: String) : ChatAction

    data class OpenUserProfile(val username: String) : ChatAction
    data class OpenImagePreview(val url: String) : ChatAction
    data class OpenLinkPreview(val url: String) : ChatAction
    data class OpenVideoPreview(val url: String) : ChatAction
    data object DismissOverlay : ChatAction

    data class QuoteMessage(val messageId: String) : ChatAction
    data class RevokeMessage(val message: ChatRoomMessage) : ChatAction
    data class ReactToMessage(val message: ChatRoomMessage, val reaction: String) : ChatAction
    data class RepeatMessage(val message: ChatRoomMessage) : ChatAction

    data class NotifyPluginMessage(val message: ChatRoomMessage, val eventType: String) : ChatAction

    data object OpenAttachmentPanel : ChatAction
    data object CloseAttachmentPanel : ChatAction
    data object ToggleEmoji : ChatAction
    data object OpenEmojiPanel : ChatAction
    data object CloseEmojiPanel : ChatAction
    data class SelectEmojiGroup(val groupId: String) : ChatAction
    data class PickEmoji(val name: String, val url: String) : ChatAction
    data class SearchMention(val anchor: Int?, val query: String?) : ChatAction
    data class PickMention(val username: String) : ChatAction

    data object OpenBarragerComposer : ChatAction
    data object DismissBarragerComposer : ChatAction
    data class ChangeBarragerContent(val value: String) : ChatAction
    data object SendBarrager : ChatAction
    data class ClearBarrager(val id: String) : ChatAction

    data object OpenRedPacketComposer : ChatAction
    data object DismissRedPacketComposer : ChatAction
    data class ChangeRedPacketType(val value: String) : ChatAction
    data class ChangeRedPacketMoney(val value: String) : ChatAction
    data class ChangeRedPacketCount(val value: String) : ChatAction
    data class ChangeRedPacketMessage(val value: String) : ChatAction
    data class ChangeRedPacketReceivers(val value: String) : ChatAction
    data class ChangeRedPacketGesture(val value: Int) : ChatAction
    data object SendRedPacket : ChatAction
    data class ClickRedPacket(val message: ChatRoomMessage) : ChatAction
    data class OpenRedPacket(val message: ChatRoomMessage, val gesture: Int? = null) : ChatAction
    data object ClearGestureRedPacket : ChatAction
    data object ClearRedPacketResult : ChatAction
    data object ClearRedPacketJumpTarget : ChatAction
}
