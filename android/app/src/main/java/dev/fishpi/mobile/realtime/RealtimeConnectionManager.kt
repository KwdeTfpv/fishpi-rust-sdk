package dev.fishpi.mobile.realtime

import dev.fishpi.mobile.data.ChatOnlineUser
import dev.fishpi.mobile.data.ChatReactionUpdate
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.data.ChatRoomRealtimeClient
import dev.fishpi.mobile.data.NoticeRealtimeClient
import dev.fishpi.mobile.data.PrivateChatNotice
import dev.fishpi.mobile.data.PrivateChatRealtimeClient
import dev.fishpi.mobile.data.RedPacketStatusUpdate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

// 聊天室实时事件，扇出给 UI 渲染与通知两个消费者
sealed interface ChatRoomEvent {
    data class Message(val message: ChatRoomMessage) : ChatRoomEvent
    data class Revoke(val messageId: String) : ChatRoomEvent
    data class Reaction(val update: ChatReactionUpdate) : ChatRoomEvent
    data class RedPacketStatus(val update: RedPacketStatusUpdate) : ChatRoomEvent
    data class Online(val count: Int, val topic: String, val users: List<ChatOnlineUser>) : ChatRoomEvent
    data class DiscussChanged(val topic: String) : ChatRoomEvent
    data class NodeChanged(val nodeName: String) : ChatRoomEvent
    data class Status(val status: String) : ChatRoomEvent
}

data class RealtimeSession(val apiKey: String, val username: String)

// 进程级单例：三条实时连接的唯一持有者，事件通过 SharedFlow 扇出
object RealtimeConnectionManager {
    private val chatRoom = ChatRoomRealtimeClient()
    private val privateOverview = PrivateChatRealtimeClient()
    private val notice = NoticeRealtimeClient()

    private val _chatEvents = MutableSharedFlow<ChatRoomEvent>(
        replay = 0,
        extraBufferCapacity = 128,
    )
    val chatEvents: SharedFlow<ChatRoomEvent> = _chatEvents.asSharedFlow()

    private val _privateNotices = MutableSharedFlow<PrivateChatNotice>(
        replay = 0,
        extraBufferCapacity = 64,
    )
    val privateNotices: SharedFlow<PrivateChatNotice> = _privateNotices.asSharedFlow()

    private val _noticeEvents = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 16,
    )
    val noticeEvents: SharedFlow<Unit> = _noticeEvents.asSharedFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    @Volatile
    private var session: RealtimeSession? = null

    @Volatile
    private var started = false

    fun currentSession(): RealtimeSession? = session

    @Synchronized
    fun start(next: RealtimeSession) {
        if (started && session == next) return
        if (started) stop()
        session = next
        started = true

        chatRoom.connect(
            apiKey = next.apiKey,
            selfUsername = next.username,
            onMessage = { emitChat(ChatRoomEvent.Message(it)) },
            onRevoke = { emitChat(ChatRoomEvent.Revoke(it)) },
            onReaction = { emitChat(ChatRoomEvent.Reaction(it)) },
            onRedPacketStatus = { emitChat(ChatRoomEvent.RedPacketStatus(it)) },
            onOnline = { count, topic, users -> emitChat(ChatRoomEvent.Online(count, topic, users)) },
            onDiscussChanged = { emitChat(ChatRoomEvent.DiscussChanged(it)) },
            onNodeChanged = { emitChat(ChatRoomEvent.NodeChanged(it)) },
            onStatus = {
                _connected.value = isConnectedStatus(it)
                emitChat(ChatRoomEvent.Status(it))
            },
        )
        privateOverview.connectOverview(
            apiKey = next.apiKey,
            selfUsername = next.username,
            onNotice = { _privateNotices.tryEmit(it) },
            onStatus = {},
        )
        notice.connect(
            apiKey = next.apiKey,
            onNotice = { _noticeEvents.tryEmit(Unit) },
        )
    }

    @Synchronized
    fun stop() {
        started = false
        session = null
        _connected.value = false
        chatRoom.disconnect()
        privateOverview.disconnect()
        notice.disconnect()
    }

    fun reconnectChatRoom(onStatus: (String) -> Unit) {
        if (!chatRoom.hasActiveHandle()) {
            val current = session
            if (current == null) {
                onStatus("聊天室未连接")
                return
            }
            onStatus("聊天室正在重连...")
            start(current)
            return
        }
        chatRoom.reconnect(onStatus)
    }

    fun pauseChatEvents() = chatRoom.pauseEvents()

    fun resumeChatEvents() = chatRoom.resumeEvents()

    fun hasActiveChatHandle(): Boolean = chatRoom.hasActiveHandle()

    private fun emitChat(event: ChatRoomEvent) {
        _chatEvents.tryEmit(event)
    }

    private fun isConnectedStatus(raw: String): Boolean {
        val text = raw.lowercase()
        return (text.contains("connected") || raw.contains("已连接") || raw.contains("已恢复")) &&
            !text.contains("disconnect") && !raw.contains("中断") && !raw.contains("失败")
    }
}
