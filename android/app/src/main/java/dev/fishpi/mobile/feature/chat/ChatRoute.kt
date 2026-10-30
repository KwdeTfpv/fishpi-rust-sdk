package dev.fishpi.mobile.feature.chat

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import dev.fishpi.mobile.FishPiNotifier
import dev.fishpi.mobile.LocalAppSession
import dev.fishpi.mobile.data.ChatFilterConfig
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.plugin.PluginMenuAction
import dev.fishpi.mobile.plugin.PluginToolbarEntry
import dev.fishpi.mobile.shared.message.ChatQuote

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
    val uiStates = rememberChatUiStates(chatController)
    val overlays = remember { ChatRouteOverlayState() }

    val latestOnCycleTheme by rememberUpdatedState(onCycleTheme)
    val latestOnOpenNotice by rememberUpdatedState(onOpenNotice)
    val latestOnFollowBottomChanged by rememberUpdatedState(onFollowBottomChanged)
    val latestOnFollowBottomProbeChanged by rememberUpdatedState(onFollowBottomProbeChanged)
    val latestOnBlockedRequestHandled by rememberUpdatedState(onBlockedRequestHandled)
    val latestOnOpenUserProfile by rememberUpdatedState(onOpenUserProfile)
    val latestOnBack by rememberUpdatedState(onBack)

    LaunchedEffect(chatController, chatFilters) {
        chatController.setChatFilters(chatFilters)
    }

    LaunchedEffect(chatController) {
        chatController.effects.collect { effect ->
            when (effect) {
                is ChatEffect.ShowError -> FishPiNotifier.error(effect.message)
                is ChatEffect.ShowMessage -> FishPiNotifier.show(effect.message)
                else -> Unit
            }
        }
    }

    val bridge = remember(chatController, uiStates, overlays) {
        ChatRouteBridge(
            chatController = chatController,
            states = uiStates,
            overlays = overlays,
        )
    }
    val dispatch = remember(chatController) {
        { action: ChatAction -> chatController.dispatch(action) }
    }

    val environment = remember(
        bridge,
        chatFilters,
        openBlockedRequest,
        active,
        themeLabel,
        noticeUnread,
    ) {
        DefaultChatUiEnvironment(
            chatFilters = chatFilters,
            openBlockedRequest = openBlockedRequest,
            active = active,
            themeLabel = themeLabel,
            noticeUnread = noticeUnread,
            onCycleTheme = { latestOnCycleTheme() },
            onOpenNotice = { latestOnOpenNotice() },
            onFollowBottomChanged = { latestOnFollowBottomChanged(it) },
            onFollowBottomProbeChanged = { latestOnFollowBottomProbeChanged(it) },
            onBlockedRequestHandled = { latestOnBlockedRequestHandled() },
            onOpenUserProfile = { latestOnOpenUserProfile(it) },
            onBack = { latestOnBack() },
            bridge = bridge,
        )
    }

    ProvideDefaultChatUiEnvironment(environment = environment) {
        ChatRouteContent(uiStates = uiStates, dispatch = dispatch)
    }

    if (overlays.pluginManagerOpen.value) {
        BackHandler {
            overlays.pluginManagerOpen.value = false
        }
        dev.fishpi.mobile.plugin.PluginListSheet(
            onDismiss = { overlays.pluginManagerOpen.value = false },
        )
    }

    if (overlays.emojiManagerOpen.value) {
        val appSession = LocalAppSession.current
        dev.fishpi.mobile.feature.emoji.EmojiManagerScreen(
            apiKey = appSession.apiKey,
            onDismiss = {
                overlays.emojiManagerOpen.value = false
                chatController.refreshEmojiGroups()
            },
        )
    }
}

private class ChatRouteBridge(
    private val chatController: ChatController,
    private val states: ChatUiStates,
    private val overlays: ChatRouteOverlayState,
) : DefaultChatUiBridge {

    private val controllerState get() = states.chatState.value
    private val composerState get() = states.composer.value
    private val interactionState get() = states.interaction.value

    override val input get() = states.draft.value
    override val messages get() = states.messages.value
    override val renderModel get() = states.renderModel.value
    override val isLoading get() = controllerState.isLoading
    override val isLoadingMore get() = controllerState.isLoadingMore
    override val hasMoreHistory get() = controllerState.hasMoreHistory
    override val duplicateHistoryPageStreak get() = controllerState.duplicateHistoryPageStreak
    override val isSending get() = composerState.isSending
    override val connectionLabel get() = controllerState.connection.label
    override val chatNodeName get() = controllerState.connection.nodeName
    override val chatTopic get() = controllerState.connection.topic
    override val chatOnlineCount get() = controllerState.connection.onlineCount.toInt()
    override val chatOnlineUsers get() = controllerState.connection.onlineUsers
    override val error get() = overlays.routeError.value ?: controllerState.error ?: composerState.error
    override val unreadNewMessages get() = controllerState.unreadNewMessages
    override val redPacketJumpTargetId get() = controllerState.redPacketJumpTargetId
    override val locateMessageId get() = controllerState.locateMessageId
    override val locateMessageRequest get() = controllerState.locateMessageRequest
    override val previewLinkUrl get() = overlays.previewLinkUrl.value
    override val actionMessage get() = overlays.actionMessage.value
    override val quote get() = composerState.quote
    override val focusInputAfterQuote get() = composerState.focusInputAfterQuote
    override val inputResetKey get() = composerState.inputResetKey
    override val emojiPanelOpen get() = composerState.emojiPanelOpen
    override val emojiGroups get() = composerState.emojiGroups
    override val emojiItems get() = composerState.emojiItems
    override val selectedEmojiGroupId get() = composerState.selectedEmojiGroupId
    override val isLoadingEmojiPack get() = composerState.isLoadingEmojiPack
    override val emojiPackError get() = composerState.emojiPackError
    override val favoriteEmojiUrl get() = composerState.favoriteEmojiUrl
    override val isFavoritingEmoji get() = composerState.isFavoritingEmoji
    override val attachmentPanelOpen get() = composerState.attachmentPanelOpen
    override val redPacketComposerOpen get() = interactionState.redPacketComposerOpen
    override val redPacketType get() = interactionState.redPacketType
    override val redPacketMoney get() = interactionState.redPacketMoney
    override val redPacketCount get() = interactionState.redPacketCount
    override val redPacketMessage get() = interactionState.redPacketMessage
    override val redPacketReceivers get() = interactionState.redPacketReceivers
    override val redPacketGesture get() = interactionState.redPacketGesture
    override val isSendingRedPacket get() = interactionState.isSendingRedPacket
    override val redPacketBalance get() = interactionState.redPacketBalance
    override val redPacketMoneyError get() = interactionState.redPacketMoneyError
    override val redPacketCountError get() = interactionState.redPacketCountError
    override val redPacketReceiversError get() = interactionState.redPacketReceiversError
    override val isUploadingAttachment get() = composerState.attachments.hasPendingUpload
    override val atCandidates get() = states.mentionCandidates.value
    override val gestureRedPacket get() = interactionState.gestureRedPacket
    override val redPacketResult get() = interactionState.redPacketResult
    override val redPacketResultSource get() = interactionState.redPacketResultSource
    override val attachments get() = composerState.attachments.items
    override val composerCursorRequest get() = composerState.cursorRequest
    override val blockedMessagesOpen get() = overlays.blockedMessagesOpen.value
    override val scrollToBottomRequest get() = controllerState.scrollToBottomRequest
    override val keepPositionAfterPrependCount get() = controllerState.keepPositionAfterPrependCount
    override val pluginToolbarEntries get() = chatController.pluginToolbarEntries
    override val pluginMenuActions get() = chatController.pluginMenuActions

    override fun setPluginScene(scene: String) = chatController.setPluginScene(scene)

    override fun releasePluginScene() = chatController.releasePluginScene()

    override fun setPluginSystemMessageHandler(shouldFollowBottom: () -> Boolean) =
        chatController.setPluginSystemMessageHandler(shouldFollowBottom)

    override fun clearError() {
        overlays.routeError.value = null
        chatController.clearError()
    }

    override fun showError(reason: String) {
        FishPiNotifier.error(reason)
    }

    override fun closeAttachmentPanel() = chatController.closeAttachmentPanel()

    override fun refreshHistory(
        skipIfLoaded: Boolean,
        onSuccess: (List<ChatRoomMessage>) -> Unit,
        onFailure: (String) -> Unit,
        onFinally: () -> Unit,
    ) = chatController.refreshHistory(skipIfLoaded, onSuccess, onFailure, onFinally)

    override fun loadMoreHistory(
        onSuccess: (Int, List<ChatRoomMessage>, Int) -> Unit,
        onFailure: (String) -> Unit,
        onFinally: () -> Unit,
    ) = chatController.loadMoreHistory(onSuccess, onFailure, onFinally)

    override fun finishInitialLoading() = Unit

    override fun requestJumpToLatest() {
        chatController.dispatch(ChatAction.JumpToBottom)
    }

    override fun onLocateMessageHandled() {
        chatController.clearLocateMessage()
    }

    override fun showBlockedMessages() {
        overlays.blockedMessagesOpen.value = true
    }

    override fun clearKeepPositionAfterPrependCount() =
        chatController.clearKeepPositionAfterPrependCount()

    override fun onNearBottomChanged(nearBottom: Boolean) {
        chatController.dispatch(ChatAction.FollowBottomChanged(nearBottom))
    }

    override fun showLinkPreview(url: String) {
        overlays.previewLinkUrl.value = url
    }

    override fun replaceDraft(value: String, requestFocus: Boolean, resetInput: Boolean) =
        chatController.replaceDraft(value, requestFocus, resetInput)

    override fun setQuote(quote: ChatQuote?) = chatController.setQuote(quote)

    override fun clearCursorRequest() = chatController.clearCursorRequest()

    override fun clearFocusInputRequest() = chatController.clearFocusInputRequest()

    override fun openAttachmentPanel() = chatController.openAttachmentPanel()

    override fun openPluginManager() {
        overlays.pluginManagerOpen.value = true
    }

    override fun openEmojiManager() {
        overlays.emojiManagerOpen.value = true
    }

    override fun favoriteEmoji(url: String) {
        chatController.favoriteEmoji(url)
    }

    override fun confirmFavoriteEmoji(groupId: String, name: String) {
        chatController.confirmFavoriteEmoji(groupId, name)
    }

    override fun dismissFavoriteEmoji() {
        chatController.dismissFavoriteEmoji()
    }

    override fun emitPluginToolbarAction(entry: PluginToolbarEntry, actionId: String) =
        chatController.emitPluginToolbarAction(entry, actionId)

    override fun emitPluginMenuAction(action: PluginMenuAction, message: ChatRoomMessage) =
        chatController.emitPluginMenuAction(action, message)

    override fun dismissLinkPreview() {
        overlays.previewLinkUrl.value = null
    }

    override fun dismissMessageActions() {
        overlays.actionMessage.value = null
    }

    override fun dismissBlockedMessages() {
        overlays.blockedMessagesOpen.value = false
    }

    override fun showMessageActions(message: ChatRoomMessage) {
        overlays.actionMessage.value = message
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

@Composable
private fun ChatRouteContent(
    uiStates: ChatUiStates,
    dispatch: (ChatAction) -> Unit,
) {
    DefaultChatUi(
        state = uiStates.chatState.value,
        dispatch = dispatch,
    )
}
