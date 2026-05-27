package dev.fishpi.mobile.feature.chat.model

import dev.fishpi.mobile.data.ChatOnlineUser

data class ChatConnectionState(
    val status: ChatConnectionStatus = ChatConnectionStatus.Disconnected,
    val label: String = "",
    val nodeName: String = "",
    val topic: String = "",
    val onlineCount: Long = 0,
    val onlineUsers: List<ChatOnlineUser> = emptyList(),
)

enum class ChatConnectionStatus {
    Disconnected,
    Connecting,
    Connected,
    Reconnecting,
    Failed,
}
