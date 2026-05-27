package dev.fishpi.mobile.feature.chat

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import dev.fishpi.mobile.data.ChatFilterConfig
import dev.fishpi.mobile.data.UploadedChatFile
import dev.fishpi.mobile.shared.message.ChatQuote
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.plugin.PluginToolbarEntry

@Composable
internal fun ChatRoute(
    chatFilters: ChatFilterConfig,
    openBlockedRequest: Int,
    active: Boolean,
    chatController: ChatController,
    themeLabel: String = "",
    noticeUnread: Long = 0L,
    onCycleTheme: () -> Unit = {},
    onOpenNotice: () -> Unit = {},
    onFollowBottomChanged: (Boolean) -> Unit = {},
    onFollowBottomProbeChanged: ((() -> Boolean)?) -> Unit = {},
    onBlockedRequestHandled: () -> Unit = {},
    onOpenUserProfile: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val controllerState by chatController.state.collectAsState()
    val legacyMessages by chatController.legacyMessages.collectAsState()
    val legacyComposerState by chatController.legacyComposerState.collectAsState()
    val legacyInteractionState by chatController.legacyInteractionState.collectAsState()
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var previewLinkUrl by remember { mutableStateOf<String?>(null) }
    var actionMessage by remember { mutableStateOf<ChatRoomMessage?>(null) }
    var blockedMessagesOpen by remember { mutableStateOf(false) }
    var pluginManagerOpen by remember { mutableStateOf(false) }
    var routeError by remember { mutableStateOf<String?>(null) }

    ProvideDefaultChatUiEnvironment(
        environment = DefaultChatUiEnvironment(
            chatFilters = chatFilters,
            openBlockedRequest = openBlockedRequest,
            active = active,
            themeLabel = themeLabel,
            noticeUnread = noticeUnread,
            onCycleTheme = onCycleTheme,
            onOpenNotice = onOpenNotice,
            bridge = object : DefaultChatUiBridge {
                override val input get() = legacyComposerState.input
                override val messages get() = legacyMessages
                override val isLoading get() = controllerState.isLoading
                override val isLoadingMore get() = controllerState.isLoadingMore
                override val hasMoreHistory get() = controllerState.hasMoreHistory
                override val duplicateHistoryPageStreak get() = controllerState.duplicateHistoryPageStreak
                override val isSending get() = legacyComposerState.isSending
                override val connectionLabel get() = controllerState.connection.label
                override val chatNodeName get() = controllerState.connection.nodeName
                override val chatTopic get() = controllerState.connection.topic
                override val chatOnlineCount get() = controllerState.connection.onlineCount.toInt()
                override val chatOnlineUsers get() = controllerState.connection.onlineUsers
                override val error get() = routeError ?: controllerState.error ?: legacyComposerState.error
                override val unreadNewMessages get() = controllerState.unreadNewMessages
                override val redPacketJumpTargetId get() = controllerState.redPacketJumpTargetId
                override val previewImageUrl get() = previewImageUrl
                override val previewLinkUrl get() = previewLinkUrl
                override val actionMessage get() = actionMessage
                override val quote get() = legacyComposerState.quote
                override val focusInputAfterQuote get() = legacyComposerState.focusInputAfterQuote
                override val inputResetKey get() = legacyComposerState.inputResetKey
                override val emojiPanelOpen get() = legacyComposerState.emojiPanelOpen
                override val emojiGroups get() = legacyComposerState.emojiGroups
                override val emojiItems get() = legacyComposerState.emojiItems
                override val selectedEmojiGroupId get() = legacyComposerState.selectedEmojiGroupId
                override val isLoadingEmojiPack get() = legacyComposerState.isLoadingEmojiPack
                override val emojiPackError get() = legacyComposerState.emojiPackError
                override val attachmentPanelOpen get() = legacyComposerState.attachmentPanelOpen
                override val redPacketComposerOpen get() = legacyInteractionState.redPacketComposerOpen
                override val redPacketType get() = legacyInteractionState.redPacketType
                override val redPacketMoney get() = legacyInteractionState.redPacketMoney
                override val redPacketCount get() = legacyInteractionState.redPacketCount
                override val redPacketMessage get() = legacyInteractionState.redPacketMessage
                override val redPacketReceivers get() = legacyInteractionState.redPacketReceivers
                override val redPacketGesture get() = legacyInteractionState.redPacketGesture
                override val isSendingRedPacket get() = legacyInteractionState.isSendingRedPacket
                override val redPacketBalance get() = legacyInteractionState.redPacketBalance
                override val redPacketMoneyError get() = legacyInteractionState.redPacketMoneyError
                override val redPacketCountError get() = legacyInteractionState.redPacketCountError
                override val redPacketReceiversError get() = legacyInteractionState.redPacketReceiversError
                override val isUploadingAttachment get() = legacyComposerState.isUploadingAttachment
                override val atCandidates get() = legacyComposerState.atCandidates
                override val gestureRedPacket get() = legacyInteractionState.gestureRedPacket
                override val redPacketResult get() = legacyInteractionState.redPacketResult
                override val redPacketResultSource get() = legacyInteractionState.redPacketResultSource
                override val pendingAttachments get() = legacyComposerState.pendingAttachments
                override val blockedMessagesOpen get() = blockedMessagesOpen
                override val scrollToBottomRequest get() = controllerState.scrollToBottomRequest
                override val keepPositionAfterPrependCount get() = controllerState.keepPositionAfterPrependCount
                override val pluginToolbarEntries get() = chatController.pluginToolbarEntries

                override fun setPluginScene(scene: String) = chatController.setPluginScene(scene)
                override fun setPluginSystemMessageHandler(shouldFollowBottom: () -> Boolean) = chatController.setPluginSystemMessageHandler(shouldFollowBottom)
                override fun clearError() {
                    routeError = null
                    chatController.clearError()
                }
                override fun showError(reason: String) {
                    routeError = reason
                }
                override fun closeAttachmentPanel() = chatController.closeAttachmentPanel()
                override fun refreshHistory(skipIfLoaded: Boolean, onSuccess: (List<ChatRoomMessage>) -> Unit, onFailure: (String) -> Unit, onFinally: () -> Unit) =
                    chatController.refreshHistory(skipIfLoaded, onSuccess, onFailure, onFinally)
                override fun loadMoreHistory(onSuccess: (Int, List<ChatRoomMessage>, Int) -> Unit, onFailure: (String) -> Unit, onFinally: () -> Unit) =
                    chatController.loadMoreHistory(onSuccess, onFailure, onFinally)
                override fun finishInitialLoading() = Unit
                override fun requestJumpToLatest() {
                    chatController.dispatch(ChatAction.JumpToBottom)
                }
                override fun showBlockedMessages() {
                    blockedMessagesOpen = true
                }
                override fun clearKeepPositionAfterPrependCount() = chatController.clearKeepPositionAfterPrependCount()
                override fun onNearBottomChanged(nearBottom: Boolean) {
                    chatController.dispatch(ChatAction.FollowBottomChanged(nearBottom))
                }
                override fun showImagePreview(url: String) {
                    previewImageUrl = url
                }
                override fun showLinkPreview(url: String) {
                    previewLinkUrl = url
                }
                override fun replaceDraft(value: String, requestFocus: Boolean, resetInput: Boolean) = chatController.replaceDraft(value, requestFocus, resetInput)
                override fun setQuote(quote: ChatQuote?) = chatController.setQuote(quote)
                override fun removePendingAttachment(file: UploadedChatFile) = chatController.removePendingAttachment(file)
                override fun clearFocusInputRequest() = chatController.clearFocusInputRequest()
                override fun openAttachmentPanel() = chatController.openAttachmentPanel()
                override fun openPluginManager() {
                    pluginManagerOpen = true
                }
                override fun emitPluginToolbarAction(entry: PluginToolbarEntry, actionId: String) = chatController.emitPluginToolbarAction(entry, actionId)
                override fun dismissImagePreview() {
                    previewImageUrl = null
                }
                override fun dismissLinkPreview() {
                    previewLinkUrl = null
                }
                override fun dismissMessageActions() {
                    actionMessage = null
                }
                override fun dismissBlockedMessages() {
                    blockedMessagesOpen = false
                }
                override fun showMessageActions(message: ChatRoomMessage) {
                    actionMessage = message
                }
            },
            onFollowBottomChanged = onFollowBottomChanged,
            onFollowBottomProbeChanged = onFollowBottomProbeChanged,
            onBlockedRequestHandled = onBlockedRequestHandled,
            onOpenUserProfile = onOpenUserProfile,
            onBack = onBack,
        ),
    ) {
        DefaultChatUi(
            state = controllerState,
            dispatch = chatController::dispatch,
        )
    }

    if (pluginManagerOpen) {
        BackHandler {
            pluginManagerOpen = false
        }
        dev.fishpi.mobile.plugin.PluginListSheet(
            onDismiss = { pluginManagerOpen = false },
        )
    }
}

@Composable
internal fun ChatRealtimeRouteLifecycle(
    chatController: ChatController,
    enabled: Boolean,
    chatFilters: ChatFilterConfig,
    isRoomVisible: () -> Boolean,
    shouldFollowBottom: () -> Boolean,
) {
    val latestChatFilters by rememberUpdatedState(chatFilters)
    val latestRoomVisible by rememberUpdatedState(isRoomVisible)
    val latestShouldFollowBottom by rememberUpdatedState(shouldFollowBottom)

    DisposableEffect(chatController, enabled) {
        if (enabled) {
            chatController.connectRealtime(
                shouldBlockMessage = { incoming -> latestChatFilters.blocksChatMessage(incoming) },
                isRoomVisible = { latestRoomVisible() },
                shouldFollowBottom = { latestShouldFollowBottom() },
            )
        } else {
            chatController.disconnectRealtime(markPaused = true)
        }
        onDispose {
            chatController.disconnectRealtime(markPaused = false)
        }
    }
}
