package dev.fishpi.mobile.feature.privatechat

import dev.fishpi.mobile.core.ui.UiController
import android.content.Context
import dev.fishpi.mobile.data.AttachmentUpload
import dev.fishpi.mobile.data.AttachmentUploadQueue
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.data.ComposerAttachments
import dev.fishpi.mobile.data.EmojiItemView
import dev.fishpi.mobile.data.FishPiApiClient
import dev.fishpi.mobile.data.PrivateChatNotice
import dev.fishpi.mobile.data.PrivateChatRealtimeClient
import dev.fishpi.mobile.data.PrivateChatSession
import dev.fishpi.mobile.feature.privatechat.mapper.sortedByLatest
import dev.fishpi.mobile.feature.privatechat.mapper.toPrivateSessionUiModel
import dev.fishpi.mobile.feature.privatechat.mapper.withFileTransferSession
import dev.fishpi.mobile.feature.privatechat.model.PrivateConversationState
import dev.fishpi.mobile.shared.message.markMessageRevoked
import dev.fishpi.mobile.shared.message.repeatableDraftContent
import dev.fishpi.mobile.shared.message.toQuote
import dev.fishpi.mobile.utils.appendDraftBlock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

internal class PrivateChatController(
    context: Context,
    private val apiKey: String,
    private val selfUsername: String,
    private val api: FishPiApiClient = FishPiApiClient.shared,
    private val overviewRealtime: PrivateChatRealtimeClient = PrivateChatRealtimeClient(),
    private val conversationRealtime: PrivateChatRealtimeClient = PrivateChatRealtimeClient(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : UiController<PrivateChatState, PrivateChatAction> {
    private val _state = MutableStateFlow(PrivateChatState())
    override val state: StateFlow<PrivateChatState> = _state

    private val _effects = MutableSharedFlow<PrivateChatEffect>(extraBufferCapacity = 16)
    val effects: SharedFlow<PrivateChatEffect> = _effects.asSharedFlow()

    private val uploadQueue = AttachmentUploadQueue.shared(context)

    private var historyJob: Job? = null
    private var conversationGeneration = 0L

    init {
        observeAttachmentUploads()
    }

    override fun dispatch(action: PrivateChatAction) {
        when (action) {
            PrivateChatAction.RefreshSessions -> refreshSessions()
            is PrivateChatAction.OpenSession -> openSession(action.session)
            is PrivateChatAction.OpenPeer -> openPeer(action.peer)
            PrivateChatAction.CloseConversation -> closeConversation()
            PrivateChatAction.LoadMoreHistory -> loadMoreHistory()
            is PrivateChatAction.ApplyNotice -> applyNotice(action.notice, notify = action.notify)
            is PrivateChatAction.ChangeInput -> updateConversation {
                it.copy(input = action.value, attachments = it.attachments.syncReferences(action.value))
            }
            is PrivateChatAction.ChangeCursor -> updateConversation { it.copy(cursorPosition = action.position) }
            PrivateChatAction.ClearCursorRequest -> updateConversation { it.copy(cursorRequest = null) }
            PrivateChatAction.SendText -> sendText()
            is PrivateChatAction.PickAttachment -> pickAttachment(action.uri, action.isVideo)
            is PrivateChatAction.RemoveAttachment -> removeAttachment(action.index)
            is PrivateChatAction.RetryAttachment -> retryAttachment(action.index)
            PrivateChatAction.ToggleEmoji -> toggleEmoji()
            PrivateChatAction.CloseEmoji -> updateConversation { it.copy(emojiPanelOpen = false) }
            is PrivateChatAction.SelectEmojiGroup -> loadEmojiGroup(action.groupId)
            is PrivateChatAction.PickEmoji -> pickEmoji(action.item)
            PrivateChatAction.OpenTools -> updateConversation { it.copy(attachmentPanelOpen = !it.attachmentPanelOpen, emojiPanelOpen = false) }
            PrivateChatAction.CloseTools -> updateConversation { it.copy(attachmentPanelOpen = false) }
            PrivateChatAction.InputFocused -> updateConversation { it.copy(focusInputAfterQuote = false) }
            PrivateChatAction.CancelQuote -> updateConversation { it.copy(quote = null) }
            is PrivateChatAction.QuoteMessage -> updateConversation { it.copy(quote = action.message.toQuote(), focusInputAfterQuote = true) }
            is PrivateChatAction.RepeatMessage -> repeatMessage(action.message)
            is PrivateChatAction.RevokeMessage -> revokeMessage(action.message)
            is PrivateChatAction.ShowMessageActions -> updateConversation { it.copy(actionMessage = action.message) }
            PrivateChatAction.DismissMessageActions -> updateConversation { it.copy(actionMessage = null) }
            is PrivateChatAction.ShowLinkPreview -> updateConversation { it.copy(previewLinkUrl = action.url) }
            PrivateChatAction.DismissLinkPreview -> updateConversation { it.copy(previewLinkUrl = null) }
            PrivateChatAction.KeepPositionConsumed -> updateConversation { it.copy(keepPositionAfterPrependCount = 0) }
            is PrivateChatAction.ShowError -> showTransientError(action.message)
            PrivateChatAction.ClearError -> _state.update { it.copy(error = null) }
        }
    }

    fun refreshSessions() {
        _state.update { it.copy(isLoadingSessions = true, error = null) }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    api.getPrivateChatSessions(apiKey, selfUsername).sortedByLatest()
                }
            }.onSuccess { sessions ->
                setSessions(sessions)
                _state.update { it.copy(isLoadingSessions = false) }
            }.onFailure {
                setError(it.message ?: "加载私聊会话失败")
                _state.update { state -> state.copy(isLoadingSessions = false) }
            }
        }
    }

    fun connectOverview(enabled: Boolean) {
        if (!enabled) {
            overviewRealtime.disconnect()
            return
        }
        overviewRealtime.connectOverview(
            apiKey = apiKey,
            selfUsername = selfUsername,
            onNotice = { notice -> applyNotice(notice, notify = true) },
            onStatus = {},
        )
    }

    fun connectConversation(enabled: Boolean) {
        val peer = _state.value.conversation.selectedPeer
        if (!enabled || peer.isNullOrBlank()) {
            conversationRealtime.disconnect()
            if (!peer.isNullOrBlank()) {
                updateConversation { it.copy(status = "私聊实时连接已暂停") }
            }
            return
        }
        conversationRealtime.connect(
            apiKey = apiKey,
            selfUsername = selfUsername,
            peer = peer,
            onMessage = ::onRealtimeMessage,
            onNotice = { notice -> applyNotice(notice, notify = true) },
            onRevoke = ::onRealtimeRevoke,
            onStatus = { status -> updateConversation { it.copy(status = status) } },
        )
    }

    fun close() {
        overviewRealtime.disconnect()
        conversationRealtime.disconnect()
    }

    private fun openPeer(peer: String) {
        val target = peer.trim()
        if (target.isBlank()) return
        val existing = _state.value.rawSessions.firstOrNull { it.peer.equals(target, ignoreCase = true) }
        openSession(
            existing ?: PrivateChatSession(
                peer = target,
                preview = "",
                time = "",
                avatar = "",
                unread = 0,
                sort = System.currentTimeMillis(),
            ),
        )
    }

    private fun openSession(session: PrivateChatSession) {
        val peer = session.peer.trim()
        if (peer.isBlank()) return
        conversationGeneration += 1
        val generation = conversationGeneration
        historyJob?.cancel()
        conversationRealtime.disconnect()
        val nextConversation = PrivateConversationState(
            selectedPeer = peer,
            selectedPeerAvatar = session.avatar,
            isLoading = true,
        )
        _state.update { it.copy(conversation = nextConversation, error = null) }
        clearUnread(peer)
        emitDetailActive(true)
        historyJob = scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    withTimeout(16_000) {
                        api.getPrivateChatHistory(apiKey = apiKey, peer = peer, selfUsername = selfUsername)
                    }
                }
            }.onSuccess { history ->
                if (generation != conversationGeneration) return@onSuccess
                if (_state.value.conversation.selectedPeer != peer) return@onSuccess
                val current = _state.value.conversation.messages
                val merged = (history + current).distinctBy { it.stableMessageKey() }
                updateConversation {
                    it.copy(
                        messages = merged,
                        isLoading = false,
                        nextHistoryPage = 2,
                        hasMoreHistory = history.size >= PrivateHistoryPageSize,
                        scrollToBottomRequest = if (merged.isNotEmpty()) it.scrollToBottomRequest + 1 else it.scrollToBottomRequest,
                    )
                }
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) { api.markPrivateChatRead(apiKey, peer) }
                    }
                }
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                if (generation != conversationGeneration) return@onFailure
                if (_state.value.conversation.selectedPeer != peer) return@onFailure
                setError(error.message ?: "加载私聊历史失败")
                updateConversation { state -> state.copy(isLoading = false) }
            }
        }
    }

    private fun closeConversation() {
        conversationGeneration += 1
        historyJob?.cancel()
        conversationRealtime.disconnect()
        _state.update { it.copy(conversation = PrivateConversationState()) }
        emitDetailActive(false)
        refreshSessions()
    }

    private fun loadMoreHistory() {
        val snapshot = _state.value.conversation
        val peer = snapshot.selectedPeer ?: return
        if (snapshot.isLoading || snapshot.isLoadingMore || !snapshot.hasMoreHistory) return
        val page = snapshot.nextHistoryPage
        val generation = conversationGeneration
        updateConversation { it.copy(isLoadingMore = true) }
        historyJob = scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    withTimeout(16_000) {
                        api.getPrivateChatHistory(
                            apiKey = apiKey,
                            peer = peer,
                            page = page,
                            selfUsername = selfUsername,
                        )
                    }
                }
            }.onSuccess { older ->
                if (generation != conversationGeneration) return@onSuccess
                if (_state.value.conversation.selectedPeer != peer) return@onSuccess
                val current = _state.value.conversation.messages
                val existing = current.mapTo(HashSet()) { it.stableMessageKey() }
                val unique = older.filterNot { it.stableMessageKey() in existing }
                updateConversation {
                    it.copy(
                        messages = (unique + current).distinctBy { message -> message.stableMessageKey() },
                        isLoadingMore = false,
                        nextHistoryPage = page + 1,
                        hasMoreHistory = older.size >= PrivateHistoryPageSize && unique.isNotEmpty(),
                        keepPositionAfterPrependCount = unique.size,
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                if (generation != conversationGeneration) return@onFailure
                if (_state.value.conversation.selectedPeer != peer) return@onFailure
                setError(error.message ?: "加载更早私聊失败")
                updateConversation { state -> state.copy(isLoadingMore = false) }
            }
        }
    }

    private fun applyNotice(notice: PrivateChatNotice, notify: Boolean) {
        val peer = notice.peer.trim()
        if (peer.isBlank()) return
        val activePeer = _state.value.conversation.selectedPeer
        val existing = _state.value.rawSessions.firstOrNull { it.peer.equals(peer, ignoreCase = true) }
        val nextUnread = if (activePeer.equals(peer, ignoreCase = true)) 0 else (existing?.unread ?: 0) + 1
        upsertSession(
            PrivateChatSession(
                peer = peer,
                preview = notice.preview,
                time = "刚刚",
                avatar = notice.avatar.ifBlank { existing?.avatar.orEmpty() },
                unread = nextUnread,
                sort = System.currentTimeMillis(),
            ),
        )
        if (notify) {
            emitEffect(PrivateChatEffect.NotifyPrivateMessage(notice))
        }
    }

    private fun onRealtimeMessage(incoming: ChatRoomMessage) {
        val peer = _state.value.conversation.selectedPeer ?: return
        if (
            !incoming.userName.equals(peer, ignoreCase = true) &&
            !incoming.userName.equals(selfUsername, ignoreCase = true)
        ) {
            return
        }
        updateConversation {
            val nextMessages = (it.messages + incoming).distinctBy { message -> message.stableMessageKey() }
            it.copy(
                messages = nextMessages,
                scrollToBottomRequest = it.scrollToBottomRequest + 1,
            )
        }
        updateSessionPreview(
            peer = peer,
            preview = incoming.content,
            avatar = incoming.userAvatarURL.takeIf { incoming.userName.equals(peer, ignoreCase = true) }.orEmpty(),
            unread = 0,
        )
    }

    private fun onRealtimeRevoke(messageId: String) {
        if (messageId.isBlank()) return
        updateConversation { it.copy(messages = it.messages.markMessageRevoked(messageId)) }
    }

    private fun sendText() {
        val snapshot = _state.value.conversation
        val content = snapshot.input.trim()
        val peer = snapshot.selectedPeer.orEmpty()
        val attachments = snapshot.attachments.syncReferences(snapshot.input)
        if (peer.isBlank() || snapshot.isSending) return
        if (content.isBlank() && !attachments.hasSendableAttachment) return
        updateConversation { it.copy(isSending = true) }
        scope.launch {
            uploadQueue.awaitSettled(attachments.pendingUploadIds)
            val settled = attachments.syncFrom(uploadQueue.uploads.value)
            val resolved = settled.resolve(snapshot.input).trim()
            if (resolved.isBlank()) {
                val reason = settled.items.firstOrNull { it.error != null }?.error
                    ?: "没有可发送的内容"
                updateConversation { it.copy(attachments = settled, isSending = false) }
                setError(reason)
                return@launch
            }
            val failed = settled.items.filter {
                it.referenced && it.status == AttachmentUpload.Status.Failed
            }
            if (failed.isNotEmpty()) {
                showTransientError("图${failed.joinToString("、") { it.index.toString() }} 上传失败，已跳过")
            }
            val sendContent = snapshot.quote?.appendTo(
                text = resolved,
                targetUrl = "https://fishpi.cn/chat#${snapshot.quote.messageId}",
                title = "私聊引用",
            ) ?: resolved
            runCatching {
                withContext(Dispatchers.IO) {
                    val sentViaRealtime = runCatching { conversationRealtime.send(sendContent) }.getOrDefault(false)
                    if (!sentViaRealtime) {
                        val reconnected = conversationRealtime.reconnect()
                        if (!reconnected) error("私聊重连失败")
                        conversationRealtime.send(sendContent)
                    }
                }
            }.onSuccess {
                uploadQueue.remove(settled.allUploadIds)
                updateSessionPreview(peer, resolved, unread = 0)
                updateConversation {
                    it.copy(
                        input = "",
                        cursorPosition = 0,
                        attachments = ComposerAttachments(),
                        inputResetKey = it.inputResetKey + 1,
                        quote = null,
                        emojiPanelOpen = false,
                        isSending = false,
                    )
                }
            }.onFailure {
                setError(it.message ?: "私聊发送失败")
                updateConversation { state -> state.copy(isSending = false) }
            }
        }
    }

    private fun pickAttachment(uri: android.net.Uri, isVideo: Boolean) {
        val current = _state.value.conversation
        val uploadId = uploadQueue.enqueue(apiKey, uri, isVideo)
        val (nextAttachments, nextInput, nextCursor) = current.attachments.append(
            text = current.input,
            cursor = current.cursorPosition,
            uploadId = uploadId,
            previewUri = uri,
            isVideo = isVideo,
        )
        updateConversation {
            it.copy(
                input = nextInput,
                cursorPosition = nextCursor,
                cursorRequest = nextCursor,
                attachments = nextAttachments,
                inputResetKey = it.inputResetKey + 1,
            )
        }
    }

    private fun removeAttachment(index: Int) {
        val current = _state.value.conversation
        val removed = current.attachments.items.firstOrNull { it.index == index } ?: return
        val (nextAttachments, nextInput) = current.attachments.removeAt(current.input, index)
        uploadQueue.remove(removed.uploadId)
        updateConversation {
            it.copy(
                input = nextInput,
                attachments = nextAttachments,
                inputResetKey = it.inputResetKey + 1,
            )
        }
    }

    private fun retryAttachment(index: Int) {
        val target = _state.value.conversation.attachments.items
            .firstOrNull { it.index == index } ?: return
        uploadQueue.retry(apiKey, target.uploadId)
    }

    private fun observeAttachmentUploads() {
        scope.launch {
            uploadQueue.uploads.collect { uploads ->
                val current = _state.value.conversation.attachments
                if (current.isEmpty) return@collect
                val synced = current.syncFrom(uploads)
                if (synced == current) return@collect
                updateConversation { it.copy(attachments = synced) }
            }
        }
    }

    private fun toggleEmoji() {
        val targetOpen = !_state.value.conversation.emojiPanelOpen
        updateConversation { it.copy(emojiPanelOpen = targetOpen, attachmentPanelOpen = if (targetOpen) false else it.attachmentPanelOpen) }
        if (targetOpen && _state.value.conversation.emojiGroups.isEmpty()) {
            loadEmojiGroups()
        }
    }

    private fun loadEmojiGroups() {
        updateConversation { it.copy(isLoadingEmojiPack = true, emojiPackError = null) }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.getEmojiGroups(apiKey) }
            }.onSuccess { groups ->
                val firstGroupId = groups.firstOrNull()?.id?.takeIf(String::isNotBlank)
                updateConversation { it.copy(emojiGroups = groups, isLoadingEmojiPack = firstGroupId != null) }
                if (firstGroupId != null) loadEmojiGroup(firstGroupId)
            }.onFailure { throwable ->
                updateConversation {
                    it.copy(
                        emojiPackError = throwable.message ?: "加载表情包分组失败",
                        isLoadingEmojiPack = false,
                    )
                }
            }
        }
    }

    private fun loadEmojiGroup(groupId: String) {
        if (groupId.isBlank()) return
        updateConversation {
            it.copy(
                selectedEmojiGroupId = groupId,
                isLoadingEmojiPack = true,
                emojiPackError = null,
            )
        }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.getEmojiGroupItems(apiKey, groupId) }
            }.onSuccess { items ->
                updateConversation { it.copy(emojiItems = items, isLoadingEmojiPack = false) }
            }.onFailure { throwable ->
                updateConversation {
                    it.copy(
                        emojiPackError = throwable.message ?: "加载表情包失败",
                        isLoadingEmojiPack = false,
                    )
                }
            }
        }
    }

    private fun pickEmoji(item: EmojiItemView) {
        updateConversation {
            it.copy(
                input = appendDraftBlock(it.input, "![${item.name.ifBlank { "表情" }}](${item.url})"),
                inputResetKey = it.inputResetKey + 1,
            )
        }
    }

    private fun repeatMessage(message: ChatRoomMessage) {
        if (message.revoked) return
        val content = message.repeatableDraftContent(includeImageFallback = true)
        if (content.isBlank()) return
        updateConversation {
            it.copy(
                input = appendDraftBlock(it.input, content),
                inputResetKey = it.inputResetKey + 1,
            )
        }
    }

    private fun revokeMessage(message: ChatRoomMessage) {
        if (message.revoked || message.oId.isBlank()) return
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.revokePrivateChatMessage(apiKey, message.oId) }
            }.onSuccess {
                onRealtimeRevoke(message.oId)
            }.onFailure {
                setError(it.message ?: "私聊撤回失败")
            }
        }
    }

    private fun setSessions(sessions: List<PrivateChatSession>) {
        val sorted = sessions.withFileTransferSession().sortedByLatest()
        _state.update {
            it.copy(
                rawSessions = sorted,
                sessions = sorted.map { item -> item.toPrivateSessionUiModel() },
                totalUnread = sorted.sumOf { item -> item.unread },
            )
        }
        emitEffect(PrivateChatEffect.TotalUnreadChanged(sorted.sumOf { it.unread }))
    }

    private fun upsertSession(session: PrivateChatSession) {
        setSessions(listOf(session) + _state.value.rawSessions.filterNot { it.peer.equals(session.peer, ignoreCase = true) })
    }

    private fun updateSessionPreview(peer: String, preview: String, avatar: String = "", unread: Long = 0) {
        val target = peer.trim()
        if (target.isBlank()) return
        val existing = _state.value.rawSessions.firstOrNull { it.peer.equals(target, ignoreCase = true) }
        upsertSession(
            PrivateChatSession(
                peer = target,
                preview = preview,
                time = "刚刚",
                avatar = avatar.ifBlank { existing?.avatar.orEmpty() },
                unread = unread,
                sort = System.currentTimeMillis(),
            ),
        )
    }

    private fun clearUnread(peer: String) {
        setSessions(
            _state.value.rawSessions.map {
                if (it.peer.equals(peer, ignoreCase = true)) it.copy(unread = 0) else it
            },
        )
    }

    private fun updateConversation(transform: (PrivateConversationState) -> PrivateConversationState) {
        _state.update { it.copy(conversation = transform(it.conversation)) }
    }

    private fun setError(reason: String) {
        _state.update { it.copy(error = reason) }
        emitEffect(PrivateChatEffect.ShowError(reason))
    }

    private fun showTransientError(reason: String) {
        emitEffect(PrivateChatEffect.ShowError(reason))
    }

    private fun emitDetailActive(active: Boolean) {
        emitEffect(PrivateChatEffect.DetailActiveChanged(active))
    }

    private fun emitEffect(effect: PrivateChatEffect) {
        _effects.tryEmit(effect)
    }

    private fun ChatRoomMessage.stableMessageKey(): String =
        oId.ifBlank { "$time|$userName|$content" }

    private companion object {
        const val PrivateHistoryPageSize = 50
    }
}

