package dev.fishpi.mobile.realtime

import dev.fishpi.mobile.data.ChatOnlineUser
import dev.fishpi.mobile.data.ChatReactionUpdate
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.data.ChatRoomRealtimeClient
import dev.fishpi.mobile.data.NoticeRealtimeClient
import dev.fishpi.mobile.data.PrivateChatNotice
import dev.fishpi.mobile.data.PrivateChatRealtimeClient
import dev.fishpi.mobile.data.RedPacketStatusUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

enum class RealtimeStatus {
    Connecting,
    Connected,
    Reconnecting,
    Failed,
    Disconnected,
}

// 聊天室实时事件，扇出给 UI 渲染与通知两个消费者
sealed interface ChatRoomEvent {
    data class Message(val message: ChatRoomMessage) : ChatRoomEvent
    data class Revoke(val messageId: String) : ChatRoomEvent
    data class Reaction(val update: ChatReactionUpdate) : ChatRoomEvent
    data class RedPacketStatus(val update: RedPacketStatusUpdate) : ChatRoomEvent
    data class Online(val count: Int, val topic: String, val users: List<ChatOnlineUser>) : ChatRoomEvent
    data class DiscussChanged(val topic: String) : ChatRoomEvent
    data class NodeChanged(val nodeName: String) : ChatRoomEvent
    data class Status(val status: RealtimeStatus, val label: String) : ChatRoomEvent
}

data class RealtimeSession(val apiKey: String, val username: String)

data class ChatRoomSnapshot(
    val nodeName: String = "",
    val topic: String = "",
    val onlineCount: Int = 0,
    val onlineUsers: List<ChatOnlineUser> = emptyList(),
    val status: RealtimeStatus = RealtimeStatus.Disconnected,
    val label: String = "",
)

// 进程级单例：三条实时连接的唯一持有者，事件通过 SharedFlow 扇出
object RealtimeConnectionManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val chatRoom = ChatRoomRealtimeClient()
    private val privateOverview = PrivateChatRealtimeClient()
    private val notice = NoticeRealtimeClient()

    @Volatile
    private var noticeReconnectJob: Job? = null

    @Volatile
    private var privateOverviewReconnectJob: Job? = null

    private val droppedChatEvents = AtomicLong()
    private val droppedPrivateNotices = AtomicLong()

    private val chatEventQueue = Channel<ChatRoomEvent>(
        capacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        onUndeliveredElement = { droppedChatEvents.incrementAndGet() },
    )
    private val _chatEvents = MutableSharedFlow<ChatRoomEvent>(
        replay = 0,
        extraBufferCapacity = 128,
    )
    val chatEvents: SharedFlow<ChatRoomEvent> = _chatEvents.asSharedFlow()

    private val privateNoticeQueue = Channel<PrivateChatNotice>(
        capacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        onUndeliveredElement = { droppedPrivateNotices.incrementAndGet() },
    )
    private val _privateNotices = MutableSharedFlow<PrivateChatNotice>(
        replay = 0,
        extraBufferCapacity = 64,
    )
    val privateNotices: SharedFlow<PrivateChatNotice> = _privateNotices.asSharedFlow()

    private val _noticeEvents = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val noticeEvents: SharedFlow<Unit> = _noticeEvents.asSharedFlow()

    init {
        scope.launch { for (event in chatEventQueue) _chatEvents.emit(event) }
        scope.launch { for (notice in privateNoticeQueue) _privateNotices.emit(notice) }
    }

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _chatRoomSnapshot = MutableStateFlow(ChatRoomSnapshot())
    val chatRoomSnapshot: StateFlow<ChatRoomSnapshot> = _chatRoomSnapshot.asStateFlow()

    private val _noticeStatus = MutableStateFlow("")
    val noticeStatus: StateFlow<String> = _noticeStatus.asStateFlow()

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
            onOnline = { count, topic, users ->
                _chatRoomSnapshot.update {
                    it.copy(onlineCount = count, topic = topic.trim(), onlineUsers = users)
                }
                emitChat(ChatRoomEvent.Online(count, topic, users))
            },
            onDiscussChanged = { topic ->
                _chatRoomSnapshot.update { it.copy(topic = topic.trim()) }
                emitChat(ChatRoomEvent.DiscussChanged(topic))
            },
            onNodeChanged = { nodeName ->
                _chatRoomSnapshot.update { it.copy(nodeName = nodeName.trim()) }
                emitChat(ChatRoomEvent.NodeChanged(nodeName))
            },
            onStatus = { raw ->
                val status = classifyChatStatus(raw)
                val label = chatStatusLabel(raw)
                _connected.value = status == RealtimeStatus.Connected
                _chatRoomSnapshot.update { it.copy(status = status, label = label) }
                emitChat(ChatRoomEvent.Status(status, label))
            },
        )
        connectPrivateOverview(next)
        notice.connect(
            apiKey = next.apiKey,
            onNotice = { _noticeEvents.tryEmit(Unit) },
            onStatus = { _noticeStatus.value = it },
        )

        noticeReconnectJob?.cancel()
        noticeReconnectJob = scope.launch {
            while (true) {
                delay(15 * 60 * 1000L)
                val current = session ?: break
                notice.disconnect()
                notice.connect(
                    apiKey = current.apiKey,
                    onNotice = { _noticeEvents.tryEmit(Unit) },
                    onStatus = { _noticeStatus.value = it },
                )
            }
        }
        privateOverviewReconnectJob?.cancel()
        privateOverviewReconnectJob = scope.launch {
            while (true) {
                delay(15 * 60 * 1000L)
                val current = session ?: break
                connectPrivateOverview(current)
            }
        }
    }

    @Synchronized
    fun stop() {
        noticeReconnectJob?.cancel()
        noticeReconnectJob = null
        privateOverviewReconnectJob?.cancel()
        privateOverviewReconnectJob = null
        started = false
        session = null
        _connected.value = false
        _chatRoomSnapshot.value = ChatRoomSnapshot()
        _noticeStatus.value = ""
        chatRoom.disconnect()
        privateOverview.disconnect()
        notice.disconnect()
    }

    fun reconnectChatRoom(onStatus: (ChatRoomEvent.Status) -> Unit) {
        if (!chatRoom.hasActiveHandle()) {
            val current = session
            if (current == null) {
                onStatus(chatStatusEvent("聊天室未连接"))
                return
            }
            onStatus(chatStatusEvent("聊天室正在重连..."))
            start(current)
            return
        }
        chatRoom.reconnect { raw -> onStatus(chatStatusEvent(raw)) }
    }

    fun pauseChatEvents() = chatRoom.pauseEvents()

    fun resumeChatEvents() = chatRoom.resumeEvents()

    fun hasActiveChatHandle(): Boolean = chatRoom.hasActiveHandle()

    fun hasActivePrivateOverviewHandle(): Boolean = privateOverview.hasActiveHandle()

    private fun connectPrivateOverview(current: RealtimeSession) {
        privateOverview.connectOverview(
            apiKey = current.apiKey,
            selfUsername = current.username,
            onNotice = { notice -> privateNoticeQueue.trySend(notice) },
            onStatus = {},
        )
    }

    private fun emitChat(event: ChatRoomEvent) {
        chatEventQueue.trySend(event)
    }

    fun droppedChatEventCount(): Long = droppedChatEvents.get()

    fun droppedPrivateNoticeCount(): Long = droppedPrivateNotices.get()

    private fun chatStatusEvent(raw: String): ChatRoomEvent.Status =
        ChatRoomEvent.Status(classifyChatStatus(raw), chatStatusLabel(raw))

    private fun classifyChatStatus(raw: String): RealtimeStatus {
        val text = raw.trim()
        val lower = text.lowercase()
        return when {
            lower.contains("reconnect failed") || text.contains("重连失败") -> RealtimeStatus.Reconnecting
            lower.contains("reconnecting") || text.contains("正在重连") || text.contains("重连中") -> RealtimeStatus.Reconnecting
            lower.contains("reconnected") || text.contains("已重连") || text.contains("已恢复") -> RealtimeStatus.Connected
            lower.contains("disconnected") || lower.contains("disconnect") || lower.contains("not connected") ||
                text.contains("中断") || text.contains("断开") || text.contains("未连接") ||
                text.contains("暂停") -> RealtimeStatus.Disconnected
            lower.contains("connected") || text.contains("已连接") -> RealtimeStatus.Connected
            lower.contains("failed") || text.contains("失败") -> RealtimeStatus.Failed
            text.contains("正在连接") || lower.contains("connecting") -> RealtimeStatus.Connecting
            else -> RealtimeStatus.Connecting
        }
    }

    private fun chatStatusLabel(raw: String): String {
        val text = raw.trim()
        return when {
            text.contains("WebSocket reconnecting in", ignoreCase = true) -> "聊天室连接中断，正在重连..."
            text.contains("WebSocket reconnect failed", ignoreCase = true) -> "聊天室重连失败，继续重试中..."
            text.contains("WebSocket reconnected", ignoreCase = true) -> "聊天室连接已恢复"
            text.contains("WebSocket disconnected", ignoreCase = true) -> "聊天室连接中断"
            text.equals("WebSocket connected", ignoreCase = true) -> "聊天室已连接"
            text == "聊天室已重连" -> "聊天室连接已恢复"
            else -> text
        }
    }
}
