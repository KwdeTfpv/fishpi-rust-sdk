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
import java.util.concurrent.atomic.AtomicLong

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
            onOnline = { count, topic, users -> emitChat(ChatRoomEvent.Online(count, topic, users)) },
            onDiscussChanged = { emitChat(ChatRoomEvent.DiscussChanged(it)) },
            onNodeChanged = { emitChat(ChatRoomEvent.NodeChanged(it)) },
            onStatus = {
                _connected.value = isConnectedStatus(it)
                emitChat(ChatRoomEvent.Status(it))
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
        _noticeStatus.value = ""
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

    private fun isConnectedStatus(raw: String): Boolean {
        val text = raw.lowercase()
        return (text.contains("connected") || raw.contains("已连接") || raw.contains("已恢复")) &&
            !text.contains("disconnect") && !raw.contains("中断") && !raw.contains("失败")
    }
}
