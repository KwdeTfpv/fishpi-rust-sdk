package dev.fishpi.mobile.feature.chat.model

sealed interface ChatOverlayState {
    data object None : ChatOverlayState
    data class ImagePreview(val url: String) : ChatOverlayState
    data class LinkPreview(val url: String) : ChatOverlayState
    data class VideoPreview(val url: String) : ChatOverlayState
    data class MessageActions(val messageId: String) : ChatOverlayState
    data object AttachmentPanel : ChatOverlayState
    data object EmojiPanel : ChatOverlayState
    data object PluginSheet : ChatOverlayState
    data object BlockedMessages : ChatOverlayState
    data object RedPacketComposer : ChatOverlayState
    data class GestureRedPacket(val messageId: String) : ChatOverlayState
    data class RedPacketResult(val messageId: String) : ChatOverlayState
}
