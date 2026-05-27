package dev.fishpi.mobile.shared.message

import dev.fishpi.mobile.data.ChatRoomMessage

internal sealed interface MessageAction {
    data class ImageClicked(val url: String) : MessageAction
    data class LinkClicked(val url: String) : MessageAction
    data class VideoFullscreenClicked(val url: String) : MessageAction
    data class AvatarClicked(val username: String) : MessageAction
    data class AvatarLongPressed(val username: String) : MessageAction
    data class MessageLongPressed(val anchor: dev.fishpi.mobile.shared.message.native.MessageActionAnchor) : MessageAction
    data class RedPacketClicked(val legacyMessage: ChatRoomMessage) : MessageAction
    data class RedPacketGestureClicked(val legacyMessage: ChatRoomMessage, val gesture: Int) : MessageAction
    data class ReactionClicked(val legacyMessage: ChatRoomMessage, val reaction: String) : MessageAction
    data class RepeatClicked(val legacyMessage: ChatRoomMessage) : MessageAction
    data object LoadMoreRequested : MessageAction
    data object BlankAreaTapped : MessageAction
    data class NearBottomChanged(val value: Boolean) : MessageAction
    data class NearTopChanged(val value: Boolean) : MessageAction
}
