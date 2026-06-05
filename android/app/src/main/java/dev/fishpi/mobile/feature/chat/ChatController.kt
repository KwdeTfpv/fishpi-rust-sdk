package dev.fishpi.mobile.feature.chat

import android.content.Context
import dev.fishpi.mobile.shared.message.ChatQuote
import dev.fishpi.mobile.shared.message.repeatableDraftContent
import dev.fishpi.mobile.core.ui.UiController
import dev.fishpi.mobile.core.ui.UiLoadState
import dev.fishpi.mobile.data.ChatReactionUpdate
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.data.ChatRoomRealtimeClient
import dev.fishpi.mobile.data.EmojiGroupView
import dev.fishpi.mobile.data.EmojiItemView
import dev.fishpi.mobile.data.FishPiApiClient
import dev.fishpi.mobile.data.RedPacketOpenResult
import dev.fishpi.mobile.data.RedPacketStatusUpdate
import dev.fishpi.mobile.data.SessionStore
import dev.fishpi.mobile.data.UploadedChatFile
import dev.fishpi.mobile.feature.chat.mapper.toChatMessageUiModels
import dev.fishpi.mobile.feature.chat.barrage.ChatBarrageUiModel
import dev.fishpi.mobile.feature.chat.model.ChatComposerState
import dev.fishpi.mobile.feature.chat.model.ChatConnectionState
import dev.fishpi.mobile.feature.chat.model.ChatConnectionStatus
import dev.fishpi.mobile.feature.chat.model.ChatMentionCandidateUiModel
import dev.fishpi.mobile.feature.chat.model.ChatPendingAttachmentUiModel
import dev.fishpi.mobile.feature.redpacket.DefaultRedPacketMessage
import dev.fishpi.mobile.feature.redpacket.RedPacketFormState
import dev.fishpi.mobile.feature.redpacket.RedPacketFormValidator
import dev.fishpi.mobile.feature.redpacket.RedPacketState
import dev.fishpi.mobile.feature.redpacket.RedPacketTypeRockPaperScissors
import dev.fishpi.mobile.feature.redpacket.defaultRedPacketMessage
import dev.fishpi.mobile.feature.redpacket.toRedPacketResultUiModel
import dev.fishpi.mobile.plugin.PluginManager
import dev.fishpi.mobile.plugin.PluginMenuAction
import dev.fishpi.mobile.plugin.PluginToolbarEntry
import dev.fishpi.mobile.utils.appendDraftBlock
import dev.fishpi.mobile.utils.removeDraftBlock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


internal class ChatController(
    context: Context,
    private val apiKey: String,
    private val currentUsername: String,
    private val api: FishPiApiClient = FishPiApiClient.shared,
    private val realtime: ChatRoomRealtimeClient = ChatRoomRealtimeClient(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : UiController<ChatState, ChatAction> {
    private val rawMessages = MutableStateFlow<List<ChatRoomMessage>>(emptyList())
    private val requestedHistoryPages = mutableSetOf<Int>()
    private val store = SessionStore(context.applicationContext)
    private var livenessPollingJob: Job? = null
    private val pluginManager = PluginManager.init(context).also {
        it.apiKey = apiKey
        it.userName = currentUsername
    }

    private val _state = MutableStateFlow(ChatState(liveness = cachedHomeLiveness()))
    override val state: StateFlow<ChatState> = _state
    private val _effects = MutableSharedFlow<ChatEffect>(extraBufferCapacity = 16)
    val effects: SharedFlow<ChatEffect> = _effects.asSharedFlow()
    val legacyMessages: StateFlow<List<ChatRoomMessage>> = rawMessages.asStateFlow()

    private val _legacyComposerState = MutableStateFlow(ChatLegacyComposerState())
    val legacyComposerState: StateFlow<ChatLegacyComposerState> = _legacyComposerState.asStateFlow()

    private val _legacyInteractionState = MutableStateFlow(ChatLegacyInteractionState())
    val legacyInteractionState: StateFlow<ChatLegacyInteractionState> = _legacyInteractionState.asStateFlow()

    val pluginToolbarEntries: List<PluginToolbarEntry>
        get() = pluginManager.toolbarEntries
    val pluginMenuActions: List<PluginMenuAction>
        get() = pluginManager.menuActions
    private var realtimeConnectConfig: RealtimeConnectConfig? = null

    init {
        startLivenessPolling()
    }

    override fun dispatch(action: ChatAction) {
        when (action) {
            ChatAction.RefreshHistory -> refreshHistory()
            ChatAction.LoadMoreHistory -> loadMoreHistory()
            ChatAction.Reconnect -> reconnectRealtime()
            ChatAction.JumpToBottom -> _state.update {
                it.copy(
                    unreadNewMessages = 0,
                    scrollToBottomRequest = it.scrollToBottomRequest + 1,
                    shouldFollowBottom = true,
                )
            }
            ChatAction.MarkUnreadMessagesRead -> _state.update {
                it.copy(
                    unreadNewMessages = 0,
                    shouldFollowBottom = true,
                )
            }
            is ChatAction.NewMessagesRemainingChanged -> _state.update {
                val remaining = action.remaining.coerceAtLeast(0)
                if (remaining >= it.unreadNewMessages) {
                    it
                } else {
                    it.copy(
                        unreadNewMessages = remaining,
                        shouldFollowBottom = if (remaining == 0) true else it.shouldFollowBottom,
                    )
                }
            }
            is ChatAction.FollowBottomChanged -> _state.update {
                it.copy(
                    shouldFollowBottom = action.value,
                    unreadNewMessages = if (action.value) 0 else it.unreadNewMessages,
                )
            }
            is ChatAction.ChangeInput -> replaceDraft(action.value)
            is ChatAction.SendText -> sendText(action.content)
            is ChatAction.SetTopic -> setTopic(action.topic)
            is ChatAction.UploadAttachment -> uploadAttachment(action.path)
            ChatAction.ToggleEmoji -> toggleEmoji()
            ChatAction.OpenEmojiPanel -> openEmojiPanel()
            ChatAction.CloseEmojiPanel -> closeEmojiPanel()
            is ChatAction.SelectEmojiGroup -> loadEmojiGroup(action.groupId)
            is ChatAction.PickEmoji -> pickEmoji(action.name, action.url)
            is ChatAction.SearchMention -> searchMention(action.anchor, action.query)
            is ChatAction.PickMention -> pickMention(action.username)
            is ChatAction.RevokeMessage -> revokeMessage(action.message)
            is ChatAction.ReactToMessage -> reactToMessage(action.message, action.reaction)
            is ChatAction.RepeatMessage -> repeatMessage(action.message)
            is ChatAction.NotifyPluginMessage -> notifyPluginMessage(action.message, action.eventType)
            ChatAction.OpenBarragerComposer -> openBarragerComposer()
            ChatAction.DismissBarragerComposer -> dismissBarragerComposer()
            is ChatAction.ChangeBarragerContent -> changeBarragerContent(action.value)
            ChatAction.SendBarrager -> sendBarrager()
            is ChatAction.ClearBarrager -> clearBarrager(action.id)
            ChatAction.OpenRedPacketComposer -> openRedPacketComposer()
            ChatAction.DismissRedPacketComposer -> dismissRedPacketComposer()
            is ChatAction.ChangeRedPacketType -> changeRedPacketType(action.value)
            is ChatAction.ChangeRedPacketMoney -> updateInteraction { it.copy(redPacketMoney = action.value, redPacketMoneyError = null) }
            is ChatAction.ChangeRedPacketCount -> updateInteraction { it.copy(redPacketCount = action.value, redPacketCountError = null) }
            is ChatAction.ChangeRedPacketMessage -> updateInteraction { it.copy(redPacketMessage = action.value) }
            is ChatAction.ChangeRedPacketReceivers -> updateInteraction { it.copy(redPacketReceivers = action.value, redPacketReceiversError = null) }
            is ChatAction.ChangeRedPacketGesture -> updateInteraction { it.copy(redPacketGesture = action.value) }
            ChatAction.SendRedPacket -> sendRedPacket()
            is ChatAction.ClickRedPacket -> clickRedPacket(action.message)
            is ChatAction.OpenRedPacket -> openRedPacket(action.message, action.gesture)
            ChatAction.ClearGestureRedPacket -> updateInteraction { it.copy(gestureRedPacket = null) }
            ChatAction.ClearRedPacketResult -> updateInteraction { it.copy(redPacketResult = null, redPacketResultSource = null) }
            ChatAction.ClearRedPacketJumpTarget -> _state.update { it.copy(redPacketJumpTargetId = null) }
            else -> Unit
        }
    }

    fun setPluginScene(scene: String) {
        pluginManager.setScene(scene)
    }

    fun setPluginSystemMessageHandler(shouldFollowBottom: () -> Boolean) {
        pluginManager.onSystemMessage = { text ->
            val systemMessage = ChatRoomMessage(
                oId = "plugin:${System.currentTimeMillis()}",
                userName = "__plugin__",
                userNickname = "",
                content = text,
                time = java.time.Instant.now().toString(),
                type = "system",
            )
            onMessage(
                incoming = systemMessage,
                blocked = false,
                keepFollowing = shouldFollowBottom(),
                maxRetained = 520,
                trimTo = 440,
            )
        }
    }

    private fun notifyPluginMessage(message: ChatRoomMessage, eventType: String) {
        pluginManager.notify("message", message.toPluginJson(eventType).toString())
    }

    fun emitPluginToolbarAction(entry: PluginToolbarEntry, actionId: String) {
        pluginManager.emitToolbarAction(entry.pluginId, entry.id, actionId)
    }

    fun emitPluginMenuAction(action: PluginMenuAction, message: ChatRoomMessage) {
        pluginManager.emitMenuAction(action.pluginId, action.id, action.scene, message.toPluginJson("menuAction"))
    }

    fun connectRealtime(
        shouldBlockMessage: (ChatRoomMessage) -> Boolean,
        isRoomVisible: () -> Boolean,
        shouldFollowBottom: () -> Boolean,
        maxRetainedMessages: Int = 520,
        trimMessagesTo: Int = 440,
    ) {
        realtimeConnectConfig = RealtimeConnectConfig(
            shouldBlockMessage = shouldBlockMessage,
            isRoomVisible = isRoomVisible,
            shouldFollowBottom = shouldFollowBottom,
            maxRetainedMessages = maxRetainedMessages,
            trimMessagesTo = trimMessagesTo,
        )
        realtime.connect(
            apiKey = apiKey,
            selfUsername = currentUsername,
            onMessage = { incoming ->
                val isOwnMessage = incoming.userName.equals(currentUsername, ignoreCase = true)
                val followBottom = _state.value.shouldFollowBottom
                onMessage(
                    incoming = incoming,
                    blocked = shouldBlockMessage(incoming),
                    keepFollowing = isRoomVisible() && (isOwnMessage || followBottom || shouldFollowBottom()),
                    maxRetained = maxRetainedMessages,
                    trimTo = trimMessagesTo,
                )
            },
            onRevoke = ::onRevoke,
            onReaction = ::onReaction,
            onRedPacketStatus = ::onRedPacketStatus,
            onOnline = ::onOnline,
            onDiscussChanged = ::onDiscussChanged,
            onNodeChanged = ::onNodeChanged,
            onStatus = ::onStatus,
        )
    }

    fun disconnectRealtime(markPaused: Boolean = true) {
        realtime.disconnect()
        if (markPaused && !_state.value.isLoading) {
            updateConnectionStatus("聊天室实时连接已暂停")
        }
    }

    fun reconnectRealtime() {
        val config = realtimeConnectConfig
        if (config == null) {
            onStatus("聊天室未连接")
            return
        }
        if (!realtime.hasActiveHandle()) {
            onStatus("聊天室正在重连...")
            connectRealtime(
                shouldBlockMessage = config.shouldBlockMessage,
                isRoomVisible = config.isRoomVisible,
                shouldFollowBottom = config.shouldFollowBottom,
                maxRetainedMessages = config.maxRetainedMessages,
                trimMessagesTo = config.trimMessagesTo,
            )
            return
        }
        realtime.reconnect(::onStatus)
    }

    fun close() {
        realtime.disconnect()
        livenessPollingJob?.cancel()
        livenessPollingJob = null
    }

    private fun startLivenessPolling() {
        livenessPollingJob?.cancel()
        livenessPollingJob = scope.launch {
            while (true) {
                syncCachedLiveness()
                delay(ChatLivenessSyncIntervalMs)
            }
        }
    }

    private fun syncCachedLiveness() {
        _state.update { it.copy(liveness = cachedHomeLiveness()) }
    }

    private fun cachedHomeLiveness(): Double? =
        store.getHomeActivity(apiKey)
            ?.takeIf { it.isToday() }
            ?.activity
            ?.liveness
            ?.takeIf { it >= 0.0 }

    fun clearError() {
        _legacyComposerState.update { it.copy(error = null) }
        _state.update { it.copy(error = null) }
    }

    fun clearKeepPositionAfterPrependCount() {
        _state.update { it.copy(keepPositionAfterPrependCount = 0) }
    }

    fun setQuote(quote: ChatQuote?) {
        _legacyComposerState.update {
            it.copy(
                quote = quote,
                focusInputAfterQuote = quote != null,
            )
        }
        updateComposer {
            it.copy(
                quote = quote?.let { item ->
                    dev.fishpi.mobile.feature.chat.model.ChatQuoteUiModel(
                        text = item.preview,
                        imageUrls = item.imageUrls,
                    )
                },
            )
        }
    }

    fun removePendingAttachment(file: UploadedChatFile) {
        val nextInput = removeDraftBlock(_legacyComposerState.value.input, file.markdown)
        _legacyComposerState.update {
            it.copy(
                input = nextInput,
                pendingAttachments = it.pendingAttachments.filterNot { item -> item.markdown == file.markdown },
            )
        }
        syncComposerFromLegacy()
    }

    fun clearFocusInputRequest() {
        _legacyComposerState.update { it.copy(focusInputAfterQuote = false) }
    }

    fun openAttachmentPanel() {
        _legacyComposerState.update { it.copy(attachmentPanelOpen = true, emojiPanelOpen = false) }
        syncComposerFromLegacy()
    }

    fun closeAttachmentPanel() {
        _legacyComposerState.update { it.copy(attachmentPanelOpen = false) }
        syncComposerFromLegacy()
    }

    fun selectMentionCandidate(nextInput: String) {
        _legacyComposerState.update {
            it.copy(
                input = nextInput,
                atAnchor = null,
                atQuery = null,
                atCandidates = emptyList(),
            )
        }
        syncComposerFromLegacy()
    }

    fun refreshHistory(
        skipIfLoaded: Boolean = false,
        onSuccess: (List<ChatRoomMessage>) -> Unit = {},
        onFailure: (String) -> Unit = {},
        onFinally: () -> Unit = {},
    ) {
        if (skipIfLoaded && rawMessages.value.isNotEmpty()) {
            _state.update { it.copy(isLoading = false, loadState = UiLoadState.Idle) }
            onFinally()
            return
        }
        requestedHistoryPages.clear()
        _state.update {
            it.copy(
                isLoading = true,
                loadState = UiLoadState.Loading,
                duplicateHistoryPageStreak = 0,
                error = null,
            )
        }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    api.getChatRoomHistory(apiKey, selfUsername = currentUsername)
                }
            }.onSuccess { history ->
                rawMessages.value = history
                _state.update {
                    it.copy(
                        messages = history.toChatMessageUiModels(currentUsername),
                        isLoading = false,
                        loadState = UiLoadState.Idle,
                        nextHistoryPage = 2,
                        hasMoreHistory = history.isNotEmpty(),
                        duplicateHistoryPageStreak = 0,
                        error = null,
                        unreadNewMessages = 0,
                        scrollToBottomRequest = it.scrollToBottomRequest + 1,
                    )
                }
                onSuccess(history)
            }.onFailure { throwable ->
                val reason = throwable.message ?: "聊天室历史加载失败"
                _state.update {
                    it.copy(
                        isLoading = false,
                        loadState = UiLoadState.Error(reason),
                        error = reason,
                    )
                }
                onFailure(reason)
            }
            onFinally()
        }
    }

    fun loadMoreHistory(
        onSuccess: (page: Int, older: List<ChatRoomMessage>, addedCount: Int) -> Unit = { _, _, _ -> },
        onFailure: (String) -> Unit = {},
        onFinally: () -> Unit = {},
    ) {
        val snapshot = _state.value
        if (
            snapshot.isLoading ||
            snapshot.isLoadingMore ||
            rawMessages.value.isEmpty() ||
            snapshot.duplicateHistoryPageStreak >= MaxDuplicateHistoryPageStreak
        ) {
            return
        }
        val page = snapshot.nextHistoryPage
        if (page in requestedHistoryPages) {
            return
        }
        requestedHistoryPages.add(page)
        _state.update {
            it.copy(
                isLoadingMore = true,
                historyLoadState = UiLoadState.LoadingMore,
                error = null,
            )
        }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    api.getChatRoomHistory(apiKey, page = page, selfUsername = currentUsername)
                }
            }.onSuccess { older ->
                val previous = rawMessages.value
                val existingKeys = previous.mapTo(HashSet()) { it.historyStableKey() }
                val uniqueOlder = older.filterNot { it.historyStableKey() in existingKeys }
                val nextMessages = if (uniqueOlder.isNotEmpty()) uniqueOlder + previous else previous
                rawMessages.value = nextMessages
                val duplicateStreak = if (older.isEmpty() || uniqueOlder.isEmpty()) {
                    snapshot.duplicateHistoryPageStreak + 1
                } else {
                    0
                }
                _state.update {
                    it.copy(
                        messages = nextMessages.toChatMessageUiModels(currentUsername),
                        isLoadingMore = false,
                        historyLoadState = UiLoadState.Idle,
                        nextHistoryPage = page + 1,
                        hasMoreHistory = if (duplicateStreak > 0) {
                            duplicateStreak < MaxDuplicateHistoryPageStreak
                        } else {
                            true
                        },
                        duplicateHistoryPageStreak = duplicateStreak,
                        keepPositionAfterPrependCount = uniqueOlder.size,
                        error = null,
                    )
                }
                onSuccess(page, older, nextMessages.size - previous.size)
            }.onFailure { throwable ->
                val reason = throwable.message ?: "加载更早消息失败"
                _state.update {
                    it.copy(
                        isLoadingMore = false,
                        historyLoadState = UiLoadState.Error(reason),
                        error = reason,
                    )
                }
                onFailure(reason)
            }
            requestedHistoryPages.remove(page)
            onFinally()
        }
    }

    private fun onMessage(
        incoming: ChatRoomMessage,
        blocked: Boolean,
        keepFollowing: Boolean,
        maxRetained: Int,
        trimTo: Int,
    ) {
        if (incoming.isBarrager) {
            if (!blocked) {
                val now = System.currentTimeMillis()
                _state.update {
                    it.copy(
                        barrages = (it.barrages + incoming.toBarrageUiModel(now)).takeLast(24),
                    )
                }
            }
            return
        }
        val nextMessages = rawMessages.value + incoming
        val retainedMessages = if (keepFollowing && nextMessages.size > maxRetained) {
            nextMessages.takeLast(trimTo)
        } else {
            nextMessages
        }
        rawMessages.value = retainedMessages
        _state.update {
            it.copy(
                messages = retainedMessages.toChatMessageUiModels(currentUsername),
                unreadNewMessages = when {
                    blocked -> it.unreadNewMessages
                    keepFollowing -> it.unreadNewMessages
                    else -> it.unreadNewMessages + 1
                },
                scrollToBottomRequest = if (!blocked && keepFollowing) {
                    it.scrollToBottomRequest + 1
                } else {
                    it.scrollToBottomRequest
                },
            )
        }
    }

    private fun onRevoke(messageId: String) {
        if (messageId.isBlank()) return
        val nextMessages = rawMessages.value.map { message ->
            if (message.oId == messageId) {
                message.copy(revoked = true)
            } else {
                message
            }
        }
        rawMessages.value = nextMessages
        _state.update { it.copy(messages = nextMessages.toChatMessageUiModels(currentUsername)) }
    }

    private fun onReaction(update: ChatReactionUpdate) {
        if (update.messageId.isBlank()) return
        val nextMessages = rawMessages.value.map { message ->
            if (message.oId == update.messageId) {
                message.copy(
                    reactionSummary = update.summary,
                    currentUserReaction = update.summary.firstOrNull { it.selected }?.value.orEmpty(),
                )
            } else {
                message
            }
        }
        rawMessages.value = nextMessages
        _state.update { it.copy(messages = nextMessages.toChatMessageUiModels(currentUsername)) }
    }

    private fun onRedPacketStatus(update: RedPacketStatusUpdate) {
        val nextMessages = rawMessages.value.map { message ->
            if (message.oId == update.messageId && message.redPacket != null) {
                val finished = update.count > 0 && update.got >= update.count
                message.copy(
                    redPacket = message.redPacket.copy(
                        got = update.got,
                        count = update.count,
                        finished = finished,
                        openable = message.redPacket.openable && !finished,
                    ),
                )
            } else {
                message
            }
        }
        rawMessages.value = nextMessages
        val jumpTargetId = if (nextMessages.any { message ->
                message.oId == update.messageId &&
                    message.redPacket?.openable == true &&
                    !message.redPacket.finished
            }
        ) {
            update.messageId
        } else {
            null
        }
        _state.update {
            it.copy(
                messages = nextMessages.toChatMessageUiModels(currentUsername),
                redPacketJumpTargetId = jumpTargetId,
            )
        }
    }

    private fun onOnline(count: Int, topic: String, users: List<dev.fishpi.mobile.data.ChatOnlineUser>) {
        _state.update {
            it.copy(
                connection = it.connection.copy(
                    onlineCount = count.toLong(),
                    onlineUsers = users,
                    topic = topic.ifBlank { it.connection.topic },
                ),
            )
        }
    }

    private fun onDiscussChanged(topic: String) {
        val normalizedTopic = topic.trim()
        _state.update { it.copy(connection = it.connection.copy(topic = normalizedTopic)) }
        if (normalizedTopic.isBlank()) return

        val config = realtimeConnectConfig
        val keepFollowing = config?.let {
            it.isRoomVisible() && (_state.value.shouldFollowBottom || it.shouldFollowBottom())
        } ?: _state.value.shouldFollowBottom
        val systemMessage = ChatRoomMessage(
            oId = "discuss:${System.currentTimeMillis()}",
            userName = "__system__",
            userNickname = "",
            content = "话题已更改：$normalizedTopic",
            time = java.time.Instant.now().toString(),
            client = "system",
            type = "system",
        )
        onMessage(
            incoming = systemMessage,
            blocked = false,
            keepFollowing = keepFollowing,
            maxRetained = config?.maxRetainedMessages ?: 520,
            trimTo = config?.trimMessagesTo ?: 440,
        )
    }

    private fun onNodeChanged(nodeName: String) {
        _state.update { it.copy(connection = it.connection.copy(nodeName = nodeName.trim())) }
    }

    private fun onStatus(status: String) {
        val mappedStatus = mapChatRealtimeStatus(status)
        if (isChatConnectionStatus(status, mappedStatus)) {
            updateConnectionStatus(mappedStatus)
        }
        if (shouldRefreshAfterStatus(status, mappedStatus)) {
            val now = System.currentTimeMillis()
            if (now - lastRealtimeRefreshAtMs > 2500L && !_state.value.isLoading) {
                lastRealtimeRefreshAtMs = now
                refreshHistory()
            }
        }
    }

    private fun updateConnectionStatus(label: String) {
        _state.update {
            it.copy(
                connection = it.connection.copy(
                    status = label.toConnectionStatus(),
                    label = label,
                ),
            )
        }
    }

    private fun mapChatRealtimeStatus(raw: String): String = when {
        raw.contains("WebSocket reconnecting in", ignoreCase = true) -> "聊天室连接中断，正在重连..."
        raw.contains("WebSocket reconnect failed", ignoreCase = true) -> "聊天室重连失败，继续重试中..."
        raw.contains("WebSocket reconnected", ignoreCase = true) -> "聊天室连接已恢复"
        raw.contains("WebSocket disconnected", ignoreCase = true) -> "聊天室连接中断"
        else -> raw
    }

    private fun isChatConnectionStatus(raw: String, mapped: String): Boolean {
        val text = "$raw $mapped"
        return text.contains("聊天室") ||
            text.contains("WebSocket", ignoreCase = true) ||
            text.contains("chatroom", ignoreCase = true)
    }

    private fun shouldRefreshAfterStatus(raw: String, mapped: String): Boolean {
        val normalizedRaw = raw.trim().lowercase()
        val normalizedMapped = mapped.trim().lowercase()
        return (
            normalizedRaw.contains("websocket reconnected") ||
                normalizedRaw.contains("websocket connected") ||
                normalizedRaw.contains("chatroom connected") ||
                raw.contains("聊天室已连接") ||
                normalizedMapped.contains("连接已恢复")
            ) &&
            !normalizedRaw.contains("reconnecting") &&
            !raw.contains("正在连接")
    }

    private fun String.toConnectionStatus(): ChatConnectionStatus = when {
        contains("正在连接") || contains("重连", ignoreCase = true) -> ChatConnectionStatus.Reconnecting
        contains("已恢复") || contains("已连接") || contains("connected", ignoreCase = true) -> ChatConnectionStatus.Connected
        contains("失败") -> ChatConnectionStatus.Failed
        contains("中断") || contains("暂停") || contains("未连接") -> ChatConnectionStatus.Disconnected
        else -> ChatConnectionStatus.Connecting
    }

    private fun sendText(content: String) {
        val normalizedContent = content.trim()
        val composer = _legacyComposerState.value
        if (normalizedContent.isBlank() || composer.isSending) {
            return
        }
        val rawContent = composer.quote?.appendTo(normalizedContent) ?: normalizedContent
        _legacyComposerState.update { it.copy(isSending = true) }
        updateComposer { it.copy(isSending = true) }
        scope.launch {
            val sendContent = withContext(Dispatchers.Default) {
                pluginManager.applySendHook(rawContent)
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    api.sendChatRoomMessage(apiKey, sendContent)
                }
            }.onSuccess {
                _legacyComposerState.update {
                    it.copy(
                        input = "",
                        quote = null,
                        pendingAttachments = emptyList(),
                        emojiPanelOpen = false,
                        inputResetKey = it.inputResetKey + 1,
                        scrollToBottomRequest = it.scrollToBottomRequest + 1,
                        isSending = false,
                    )
                }
                _state.update {
                    it.copy(
                        scrollToBottomRequest = it.scrollToBottomRequest + 1,
                        composer = ChatComposerState(),
                    )
                }
            }.onFailure { throwable ->
                val reason = throwable.message ?: "发送失败"
                _legacyComposerState.update { it.copy(error = reason, isSending = false) }
                _state.update {
                    it.copy(
                        error = reason,
                        composer = it.composer.copy(isSending = false),
                    )
                }
            }
        }
    }

    private fun openBarragerComposer() {
        _legacyComposerState.update { it.copy(attachmentPanelOpen = false, emojiPanelOpen = false) }
        syncComposerFromLegacy()
        _state.update {
            it.copy(
                barrageComposer = it.barrageComposer.copy(
                    open = true,
                    error = null,
                    isLoadingCost = it.barrageComposer.costLabel == "发送弹幕会消耗积分",
                ),
            )
        }
        loadBarragerCost()
    }

    private fun dismissBarragerComposer() {
        _state.update {
            if (it.barrageComposer.isSending) {
                it
            } else {
                it.copy(barrageComposer = it.barrageComposer.copy(open = false, error = null))
            }
        }
    }

    private fun changeBarragerContent(value: String) {
        _state.update {
            it.copy(
                barrageComposer = it.barrageComposer.copy(
                    input = value.take(MaxBarragerLength),
                    error = null,
                ),
            )
        }
    }

    private fun loadBarragerCost() {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    api.getChatRoomBarragerCost(apiKey)
                }
            }.onSuccess { label ->
                _state.update {
                    it.copy(
                        barrageComposer = it.barrageComposer.copy(
                            costLabel = "消耗 $label",
                            isLoadingCost = false,
                        ),
                    )
                }
            }.onFailure { throwable ->
                _state.update {
                    it.copy(
                        barrageComposer = it.barrageComposer.copy(
                            costLabel = "发送弹幕会消耗积分",
                            isLoadingCost = false,
                            error = throwable.message ?: "弹幕花费获取失败",
                        ),
                    )
                }
            }
        }
    }

    private fun sendBarrager() {
        val snapshot = _state.value.barrageComposer
        val text = snapshot.input.trim()
        if (text.isBlank()) {
            _state.update {
                it.copy(barrageComposer = it.barrageComposer.copy(error = "先写一点弹幕内容"))
            }
            return
        }
        if (snapshot.isSending) return
        _state.update { it.copy(barrageComposer = it.barrageComposer.copy(isSending = true, error = null)) }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    api.sendChatRoomBarrager(apiKey, text, DefaultBarragerColor)
                }
            }.onSuccess {
                _state.update {
                    it.copy(
                        barrageComposer = it.barrageComposer.copy(
                            open = false,
                            input = "",
                            isSending = false,
                            error = null,
                        ),
                    )
                }
            }.onFailure { throwable ->
                _state.update {
                    it.copy(
                        barrageComposer = it.barrageComposer.copy(
                            isSending = false,
                            error = throwable.message ?: "弹幕发送失败",
                        ),
                    )
                }
            }
        }
    }

    private fun clearBarrager(id: String) {
        _state.update { state ->
            state.copy(barrages = state.barrages.filterNot { it.id == id })
        }
    }

    private fun uploadAttachment(path: String) {
        if (_legacyComposerState.value.isUploadingAttachment) return
        _legacyComposerState.update { it.copy(isUploadingAttachment = true) }
        updateComposer { it.copy(isUploadingAttachment = true) }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    api.uploadChatFile(apiKey, path)
                }
            }.onSuccess { file ->
                val nextInput = appendDraftBlock(_legacyComposerState.value.input, file.markdown)
                _legacyComposerState.update {
                    it.copy(
                        input = nextInput,
                        pendingAttachments = (it.pendingAttachments + file).distinctBy { item -> item.markdown },
                        focusInputAfterQuote = true,
                        isUploadingAttachment = false,
                    )
                }
                syncComposerFromLegacy()
            }.onFailure { throwable ->
                val reason = throwable.message ?: "上传媒体失败"
                _legacyComposerState.update { it.copy(isUploadingAttachment = false) }
                _state.update {
                    it.copy(
                        composer = it.composer.copy(isUploadingAttachment = false),
                    )
                }
                emitEffect(ChatEffect.ShowError(reason))
            }
        }
    }

    private fun toggleEmoji() {
        val targetOpen = !_legacyComposerState.value.emojiPanelOpen
        _legacyComposerState.update {
            it.copy(
                emojiPanelOpen = targetOpen,
                attachmentPanelOpen = if (targetOpen) false else it.attachmentPanelOpen,
            )
        }
        syncComposerFromLegacy()
        if (targetOpen && _legacyComposerState.value.emojiGroups.isEmpty()) {
            loadEmojiGroups()
        }
    }

    private fun openEmojiPanel() {
        if (!_legacyComposerState.value.emojiPanelOpen) {
            toggleEmoji()
        }
    }

    private fun closeEmojiPanel() {
        _legacyComposerState.update { it.copy(emojiPanelOpen = false) }
        syncComposerFromLegacy()
    }

    private fun loadEmojiGroups() {
        _legacyComposerState.update { it.copy(isLoadingEmojiPack = true, emojiPackError = null) }
        syncComposerFromLegacy()
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    api.getEmojiGroups(apiKey)
                }
            }.onSuccess { groups ->
                val firstGroupId = groups.firstOrNull()?.id?.takeIf(String::isNotBlank)
                _legacyComposerState.update {
                    it.copy(
                        emojiGroups = groups,
                        isLoadingEmojiPack = firstGroupId != null,
                    )
                }
                syncComposerFromLegacy()
                if (firstGroupId != null) {
                    loadEmojiGroup(firstGroupId)
                }
            }.onFailure { throwable ->
                val reason = throwable.message ?: "加载表情包分组失败"
                _legacyComposerState.update {
                    it.copy(
                        emojiPackError = reason,
                        isLoadingEmojiPack = false,
                    )
                }
                syncComposerFromLegacy()
            }
        }
    }

    private fun loadEmojiGroup(groupId: String) {
        if (groupId.isBlank()) return
        _legacyComposerState.update {
            it.copy(
                selectedEmojiGroupId = groupId,
                isLoadingEmojiPack = true,
                emojiPackError = null,
            )
        }
        syncComposerFromLegacy()
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    api.getEmojiGroupItems(apiKey, groupId)
                }
            }.onSuccess { items ->
                _legacyComposerState.update {
                    it.copy(
                        emojiItems = items,
                        isLoadingEmojiPack = false,
                    )
                }
                syncComposerFromLegacy()
            }.onFailure { throwable ->
                val reason = throwable.message ?: "加载表情包失败"
                _legacyComposerState.update {
                    it.copy(
                        emojiPackError = reason,
                        isLoadingEmojiPack = false,
                    )
                }
                syncComposerFromLegacy()
            }
        }
    }

    private fun pickEmoji(name: String, url: String) {
        val label = name.ifBlank { "表情" }
        replaceDraft(appendDraftBlock(_legacyComposerState.value.input, "![$label]($url)"))
    }

    private fun searchMention(anchor: Int?, query: String?) {
        _legacyComposerState.update {
            it.copy(
                atAnchor = anchor,
                atQuery = query,
                atCandidates = if (anchor == null || query.isNullOrBlank()) emptyList() else it.atCandidates,
            )
        }
        syncComposerFromLegacy()
        val keyword = query?.takeIf(String::isNotBlank) ?: return
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    api.searchAtUsers(keyword)
                }
            }.onSuccess { users ->
                _legacyComposerState.update {
                    if (it.atQuery == keyword) {
                        it.copy(atCandidates = users.take(8))
                    } else {
                        it
                    }
                }
                syncComposerFromLegacy()
            }.onFailure {
                _legacyComposerState.update {
                    it.copy(atAnchor = null, atQuery = null, atCandidates = emptyList())
                }
                syncComposerFromLegacy()
            }
        }
    }

    private fun pickMention(username: String) {
        val anchor = _legacyComposerState.value.atAnchor ?: return
        selectMentionCandidate(applyAtCandidate(_legacyComposerState.value.input, anchor, username))
    }

    private fun clickRedPacket(message: ChatRoomMessage) {
        val packet = message.redPacket ?: return
        val isMine = message.userName.equals(currentUsername, ignoreCase = true)
        when {
            isMine && (packet.openable || packet.finished) -> openRedPacket(message)
            packet.openable && packet.needGesture -> {
                _state.update { it.copy(overlay = dev.fishpi.mobile.feature.chat.model.ChatOverlayState.GestureRedPacket(message.oId)) }
                updateInteraction { it.copy(gestureRedPacket = message) }
            }
            packet.openable || packet.finished -> openRedPacket(message)
            else -> showUnavailableRedPacket(message)
        }
    }

    private fun openRedPacket(message: ChatRoomMessage, gesture: Int? = null) {
        val packet = message.redPacket ?: return
        if (message.oId.isBlank() || (!packet.openable && !packet.finished)) return
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    api.openRedPacket(apiKey, message.oId, gesture)
                }
            }.onSuccess { result ->
                _state.update { it.copy(overlay = dev.fishpi.mobile.feature.chat.model.ChatOverlayState.RedPacketResult(message.oId)) }
                updateInteraction {
                    it.copy(
                        gestureRedPacket = null,
                        redPacketResult = result,
                        redPacketResultSource = message,
                    )
                }
            }.onFailure { throwable ->
                failRedPacketOpening(throwable.message ?: "拆红包失败", message)
            }
        }
    }

    private fun showUnavailableRedPacket(message: ChatRoomMessage) {
        val packet = message.redPacket ?: return
        _state.update { it.copy(overlay = dev.fishpi.mobile.feature.chat.model.ChatOverlayState.RedPacketResult(message.oId)) }
        updateInteraction {
            it.copy(
                redPacketResultSource = message,
                redPacketResult = RedPacketOpenResult(
                    message = "这个红包不可领取",
                    count = packet.count,
                    got = packet.got,
                    gesture = packet.gesture,
                    who = emptyList(),
                ),
            )
        }
    }

    private fun failRedPacketOpening(reason: String, source: ChatRoomMessage) {
        val packet = source.redPacket
        if (
            reason.contains("已抢完") ||
            reason.contains("抢完") ||
            reason.contains("已领完") ||
            reason.contains("不可领取")
        ) {
            _state.update { it.copy(overlay = dev.fishpi.mobile.feature.chat.model.ChatOverlayState.RedPacketResult(source.oId)) }
            updateInteraction {
                it.copy(
                    redPacketResultSource = source,
                    redPacketResult = RedPacketOpenResult(
                        message = if (packet?.finished == true) "红包已抢完" else "这个红包不可领取",
                        count = packet?.count ?: 0,
                        got = packet?.got ?: 0,
                        gesture = packet?.gesture,
                        who = emptyList(),
                    ),
                )
            }
            return
        }
        setError(reason)
    }

    private fun sendRedPacket() {
        val snapshot = _legacyInteractionState.value
        if (snapshot.isSendingRedPacket) return
        val validation = RedPacketFormValidator.validate(snapshot.toRedPacketFormState())
        if (!validation.isValid) {
            updateInteraction {
                it.copy(
                    redPacketMoneyError = validation.moneyError,
                    redPacketCountError = validation.countError,
                    redPacketReceiversError = validation.receiversError,
                )
            }
            setError(validation.firstError ?: "红包表单填写有误")
            return
        }
        val money = validation.money ?: return
        val count = validation.count ?: return
        updateInteraction { it.copy(isSendingRedPacket = true) }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    api.sendRedPacket(
                        apiKey = apiKey,
                        type = snapshot.redPacketType,
                        money = money,
                        count = count,
                        message = snapshot.redPacketMessage,
                        receivers = snapshot.redPacketReceivers,
                        gesture = if (snapshot.redPacketType == RedPacketTypeRockPaperScissors) snapshot.redPacketGesture else null,
                    )
                }
            }.onSuccess {
                updateInteraction { it.copy(isSendingRedPacket = false, redPacketComposerOpen = false) }
                _state.update { it.copy(overlay = dev.fishpi.mobile.feature.chat.model.ChatOverlayState.None) }
            }.onFailure { throwable ->
                updateInteraction { it.copy(isSendingRedPacket = false) }
                setError(throwable.message ?: "发送红包失败")
            }
        }
    }

    private fun reactToMessage(message: ChatRoomMessage, value: String) {
        if (message.oId.isBlank() || message.revoked || value.isBlank()) return
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    api.reactChatRoomMessage(apiKey, message.oId, value)
                }
            }.onSuccess(::onReaction)
                .onFailure { setError(it.message ?: "贴表情失败") }
        }
    }

    private fun revokeMessage(message: ChatRoomMessage) {
        if (message.revoked || message.oId.isBlank() || message.type == "redPacket") return
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    api.revokeChatRoomMessage(apiKey, message.oId)
                }
            }.onSuccess {
                onRevoke(message.oId)
            }.onFailure {
                setError(it.message ?: "撤回失败")
            }
        }
    }

    private fun repeatMessage(message: ChatRoomMessage) {
        val content = message.repeatableDraftContent()
        if (message.revoked || content.isBlank()) return
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    api.sendChatRoomMessage(apiKey, content)
                }
            }.onFailure {
                setError(it.message ?: "复读失败")
            }
        }
    }

    private fun setTopic(topic: String) {
        val normalizedTopic = topic.trim()
        if (normalizedTopic.isBlank()) {
            setError("话题不能为空")
            return
        }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    api.setChatRoomDiscuss(apiKey, normalizedTopic)
                }
            }.onSuccess {
                _state.update { it.copy(connection = it.connection.copy(topic = normalizedTopic)) }
            }.onFailure {
                setError(it.message ?: "更改话题失败")
            }
        }
    }

    private fun openRedPacketComposer() {
        updateInteraction { it.copy(attachmentPanelOpen = false, redPacketComposerOpen = true) }
        _legacyComposerState.update { it.copy(attachmentPanelOpen = false) }
        syncComposerFromLegacy()
        _state.update { it.copy(overlay = dev.fishpi.mobile.feature.chat.model.ChatOverlayState.RedPacketComposer) }
        loadRedPacketBalance()
    }

    private fun dismissRedPacketComposer() {
        updateInteraction { if (it.isSendingRedPacket) it else it.copy(redPacketComposerOpen = false) }
        _state.update { it.copy(overlay = dev.fishpi.mobile.feature.chat.model.ChatOverlayState.None) }
    }

    private fun changeRedPacketType(value: String) {
        val previousDefault = defaultRedPacketMessage(_legacyInteractionState.value.redPacketType)
        val nextDefault = defaultRedPacketMessage(value)
        updateInteraction {
            it.copy(
                redPacketType = value,
                redPacketMoney = if (value == RedPacketTypeRockPaperScissors && (it.redPacketMoney.toIntOrNull() ?: 0) < 256) {
                    "256"
                } else {
                    it.redPacketMoney
                },
                redPacketMessage = if (it.redPacketMessage == previousDefault) nextDefault else it.redPacketMessage,
                redPacketMoneyError = null,
                redPacketCountError = null,
                redPacketReceiversError = null,
            )
        }
    }

    private fun loadRedPacketBalance() {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    api.getUserPoints(apiKey, currentUsername)
                }
            }.onSuccess { balance ->
                updateInteraction { it.copy(redPacketBalance = balance) }
            }
        }
    }

    private fun updateInteraction(transform: (ChatLegacyInteractionState) -> ChatLegacyInteractionState) {
        _legacyInteractionState.update(transform)
        val interaction = _legacyInteractionState.value
        _state.update { it.copy(redPacket = interaction.toRedPacketState(currentUsername)) }
    }

    private fun setError(reason: String) {
        _legacyComposerState.update { it.copy(error = reason) }
        _state.update { it.copy(error = reason) }
    }

    private fun emitEffect(effect: ChatEffect) {
        _effects.tryEmit(effect)
    }

    fun replaceDraft(value: String, requestFocus: Boolean = false, resetInput: Boolean = false) {
        _legacyComposerState.update {
            it.copy(
                input = value,
                focusInputAfterQuote = if (requestFocus) true else it.focusInputAfterQuote,
                inputResetKey = if (resetInput) it.inputResetKey + 1 else it.inputResetKey,
            )
        }
        syncComposerFromLegacy()
    }

    private fun updateComposer(transform: (ChatComposerState) -> ChatComposerState) {
        _state.update { it.copy(composer = transform(it.composer)) }
    }

    private fun syncComposerFromLegacy() {
        val legacy = _legacyComposerState.value
        _state.update {
            it.copy(
                error = legacy.error,
                composer = it.composer.copy(
                    input = legacy.input,
                    isSending = legacy.isSending,
                    isUploadingAttachment = legacy.isUploadingAttachment,
                    emojiPanelOpen = legacy.emojiPanelOpen,
                    attachmentPanelOpen = legacy.attachmentPanelOpen,
                    isLoadingEmojiPack = legacy.isLoadingEmojiPack,
                    emojiPackError = legacy.emojiPackError,
                    pendingAttachments = legacy.pendingAttachments.map { file ->
                        ChatPendingAttachmentUiModel(
                            url = file.url,
                            markdown = file.markdown,
                            type = file.type,
                        )
                    },
                    mentionAnchor = legacy.atAnchor,
                    mentionQuery = legacy.atQuery,
                    mentionCandidates = legacy.atCandidates.map { username ->
                        ChatMentionCandidateUiModel(
                            username = username,
                            displayName = username,
                        )
                    },
                    selectedEmojiGroupId = legacy.selectedEmojiGroupId,
                ),
            )
        }
    }

    private fun applyAtCandidate(input: String, anchor: Int, username: String): String {
        val safeAnchor = anchor.coerceIn(0, input.length)
        val before = input.substring(0, safeAnchor)
        val tail = input.substring(safeAnchor)
        val queryEnd = tail.indexOfFirst { it.isWhitespace() }.let { if (it < 0) tail.length else it }
        val after = tail.substring(queryEnd)
        return before + "@$username " + after.trimStart()
    }

    private fun ChatRoomMessage.historyStableKey(): String =
        oId.ifBlank { "$userName|$time|$content" }

    private fun ChatRoomMessage.toBarrageUiModel(now: Long): ChatBarrageUiModel =
        ChatBarrageUiModel(
            id = oId.ifBlank { "barrager:$now:${content.hashCode()}" },
            author = displayName,
            avatarUrl = userAvatarURL,
            content = content,
            color = barragerColor.ifBlank { DefaultBarragerColor },
            createdAtMs = now,
        )

    private companion object {
        const val MaxDuplicateHistoryPageStreak = 6
        const val MaxBarragerLength = 80
        const val DefaultBarragerColor = "#ffffff"
    }

    private var lastRealtimeRefreshAtMs: Long = 0L
}

internal data class ChatLegacyComposerState(
    val input: String = "",
    val quote: ChatQuote? = null,
    val focusInputAfterQuote: Boolean = false,
    val inputResetKey: Int = 0,
    val isSending: Boolean = false,
    val isUploadingAttachment: Boolean = false,
    val pendingAttachments: List<UploadedChatFile> = emptyList(),
    val attachmentPanelOpen: Boolean = false,
    val emojiPanelOpen: Boolean = false,
    val emojiGroups: List<EmojiGroupView> = emptyList(),
    val emojiItems: List<EmojiItemView> = emptyList(),
    val selectedEmojiGroupId: String = "",
    val isLoadingEmojiPack: Boolean = false,
    val emojiPackError: String? = null,
    val atAnchor: Int? = null,
    val atQuery: String? = null,
    val atCandidates: List<String> = emptyList(),
    val error: String? = null,
    val scrollToBottomRequest: Int = 0,
)

internal data class ChatLegacyInteractionState(
    val redPacketComposerOpen: Boolean = false,
    val redPacketType: String = "random",
    val redPacketMoney: String = "32",
    val redPacketCount: String = "1",
    val redPacketMessage: String = DefaultRedPacketMessage,
    val redPacketReceivers: String = "",
    val redPacketGesture: Int = 0,
    val redPacketBalance: Long? = null,
    val isSendingRedPacket: Boolean = false,
    val redPacketMoneyError: String? = null,
    val redPacketCountError: String? = null,
    val redPacketReceiversError: String? = null,
    val gestureRedPacket: ChatRoomMessage? = null,
    val redPacketResult: RedPacketOpenResult? = null,
    val redPacketResultSource: ChatRoomMessage? = null,
    val attachmentPanelOpen: Boolean = false,
) {
    fun toRedPacketFormState(): RedPacketFormState =
        RedPacketFormState(
            type = redPacketType,
            money = redPacketMoney,
            count = redPacketCount,
            message = redPacketMessage,
            receivers = redPacketReceivers,
            gesture = redPacketGesture,
            balance = redPacketBalance,
            isSending = isSendingRedPacket,
            moneyError = redPacketMoneyError,
            countError = redPacketCountError,
            receiversError = redPacketReceiversError,
        )

    fun toRedPacketState(selfUsername: String): RedPacketState =
        RedPacketState(
            composerOpen = redPacketComposerOpen,
            form = toRedPacketFormState(),
            gestureTargetMessageId = gestureRedPacket?.oId,
            selectedGesture = gestureRedPacket?.redPacket?.gesture,
            result = redPacketResult?.toRedPacketResultUiModel(
                senderName = redPacketResultSource?.userName.orEmpty(),
                senderAvatar = redPacketResultSource?.userAvatarURL.orEmpty(),
                packetMessage = redPacketResultSource?.redPacket?.message.orEmpty(),
                selfUsername = selfUsername,
                finished = redPacketResultSource?.redPacket?.finished == true ||
                    (redPacketResult.count > 0L && redPacketResult.got >= redPacketResult.count),
            ),
        )
}

private data class RealtimeConnectConfig(
    val shouldBlockMessage: (ChatRoomMessage) -> Boolean,
    val isRoomVisible: () -> Boolean,
    val shouldFollowBottom: () -> Boolean,
    val maxRetainedMessages: Int,
    val trimMessagesTo: Int,
)
