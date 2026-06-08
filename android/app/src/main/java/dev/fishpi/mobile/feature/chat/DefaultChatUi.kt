package dev.fishpi.mobile.feature.chat

import dev.fishpi.mobile.ui.components.*
import dev.fishpi.mobile.feature.chat.ui.ChatLivenessFloatingOrb

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.InsertEmoticon
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import dev.fishpi.mobile.*
import dev.fishpi.mobile.shared.message.*
import dev.fishpi.mobile.shared.message.ui.MessageActionBubbleMenu
import dev.fishpi.mobile.shared.message.ui.MessageActionSpec
import dev.fishpi.mobile.shared.message.ui.MessageActionSheet
import dev.fishpi.mobile.ui.components.ErrorState
import dev.fishpi.mobile.ui.components.LoadingScreen
import dev.fishpi.mobile.ui.components.consumeTaps
import dev.fishpi.mobile.ui.components.fishPiChatWallpaper
import dev.fishpi.mobile.ui.components.silentTap
import dev.fishpi.mobile.ui.media.rememberChatAttachmentPicker
import dev.fishpi.mobile.ui.overlay.ImagePreviewOverlay
import dev.fishpi.mobile.ui.overlay.LinkPreviewOverlay
import dev.fishpi.mobile.ui.overlay.VideoPlaybackOverlay
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.data.ChatFilterConfig
import dev.fishpi.mobile.data.ChatOnlineUser
import dev.fishpi.mobile.data.EmojiGroupView
import dev.fishpi.mobile.data.EmojiItemView
import dev.fishpi.mobile.data.FishPiUser
import dev.fishpi.mobile.data.MedalView
import dev.fishpi.mobile.data.RedPacketOpenResult
import dev.fishpi.mobile.data.UploadedChatFile
import dev.fishpi.mobile.feature.chat.barrage.ChatBarrageComposerState
import dev.fishpi.mobile.feature.chat.barrage.ChatBarrageUiModel
import dev.fishpi.mobile.feature.chat.barrage.ui.ChatBarrageComposerDialog
import dev.fishpi.mobile.feature.chat.barrage.ui.ChatBarrageOverlay
import dev.fishpi.mobile.shared.message.native.MessageActionAnchor
import dev.fishpi.mobile.shared.message.native.NativeMessageList
import dev.fishpi.mobile.chatui.NativeChatRoomHeader
import dev.fishpi.mobile.shared.message.native.rememberNativeMessageListController
import dev.fishpi.mobile.feature.redpacket.DefaultRedPacketCard
import dev.fishpi.mobile.feature.redpacket.DefaultRedPacketGestureUi
import dev.fishpi.mobile.feature.redpacket.DefaultRedPacketResultUi
import dev.fishpi.mobile.feature.redpacket.DefaultRedPacketSendUi
import dev.fishpi.mobile.feature.redpacket.RedPacketAction
import dev.fishpi.mobile.feature.redpacket.RedPacketFormState
import dev.fishpi.mobile.feature.redpacket.toRedPacketResultUiModel
import dev.fishpi.mobile.ui.components.ChatToolAction
import dev.fishpi.mobile.ui.animal.AnimalPanel
import dev.fishpi.mobile.ui.animal.AnimalStatusPill
import dev.fishpi.mobile.plugin.PluginToolbarAction
import dev.fishpi.mobile.plugin.PluginToolbarEntry
import dev.fishpi.mobile.plugin.PluginMenuAction
import dev.fishpi.mobile.utils.appendDraftBlock
import dev.fishpi.mobile.utils.appendMentionDraft
import dev.fishpi.mobile.utils.isDirectVideoUrl
import java.net.URLEncoder
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ChatIconButtonSize = 36.dp
private const val ChatPluginFloatPrefsName = "chat_plugin_float"
private const val ChatPluginFloatXKey = "x"
private const val ChatPluginFloatYKey = "y"
private const val ChatPluginFloatHiddenKey = "hidden"
private const val ChatPluginFloatUnset = Int.MIN_VALUE

@Composable
internal fun DefaultChatUi(
    state: ChatState,
    dispatch: (ChatAction) -> Unit,
) {
    val environment = LocalDefaultChatUiEnvironment.current
    DefaultChatUiContent(
        chatFilters = environment.chatFilters,
        openBlockedRequest = environment.openBlockedRequest,
        active = environment.active,
        bridge = environment.bridge,
        themeLabel = environment.themeLabel,
        noticeUnread = environment.noticeUnread,
        onCycleTheme = environment.onCycleTheme,
        onOpenNotice = environment.onOpenNotice,
        onFollowBottomChanged = environment.onFollowBottomChanged,
        onFollowBottomProbeChanged = environment.onFollowBottomProbeChanged,
        onBlockedRequestHandled = environment.onBlockedRequestHandled,
        onOpenUserProfile = environment.onOpenUserProfile,
        onBack = environment.onBack,
        liveness = state.liveness,
        barrages = state.barrages,
        barrageComposer = state.barrageComposer,
        dispatch = dispatch,
    )
}

@Composable
internal fun ProvideDefaultChatUiEnvironment(
    environment: DefaultChatUiEnvironment,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalDefaultChatUiEnvironment provides environment, content = content)
}

internal data class DefaultChatUiEnvironment(
    val chatFilters: ChatFilterConfig,
    val openBlockedRequest: Int,
    val active: Boolean,
    val bridge: DefaultChatUiBridge,
    val themeLabel: String = "",
    val noticeUnread: Long = 0L,
    val onCycleTheme: () -> Unit = {},
    val onOpenNotice: () -> Unit = {},
    val onFollowBottomChanged: (Boolean) -> Unit = {},
    val onFollowBottomProbeChanged: ((() -> Boolean)?) -> Unit = {},
    val onBlockedRequestHandled: () -> Unit = {},
    val onOpenUserProfile: (String) -> Unit = {},
    val onBack: () -> Unit = {},
)

private val LocalDefaultChatUiEnvironment = staticCompositionLocalOf<DefaultChatUiEnvironment> {
    error("DefaultChatUiEnvironment is not provided")
}

internal interface DefaultChatUiBridge {
    val input: String
    val messages: List<ChatRoomMessage>
    val isLoading: Boolean
    val isLoadingMore: Boolean
    val hasMoreHistory: Boolean
    val duplicateHistoryPageStreak: Int
    val isSending: Boolean
    val connectionLabel: String
    val chatNodeName: String
    val chatTopic: String
    val chatOnlineCount: Int
    val chatOnlineUsers: List<ChatOnlineUser>
    val error: String?
    val unreadNewMessages: Int
    val redPacketJumpTargetId: String?
    val previewImageUrl: String?
    val previewLinkUrl: String?
    val actionMessage: ChatRoomMessage?
    val quote: ChatQuote?
    val focusInputAfterQuote: Boolean
    val inputResetKey: Int
    val emojiPanelOpen: Boolean
    val emojiGroups: List<EmojiGroupView>
    val emojiItems: List<EmojiItemView>
    val selectedEmojiGroupId: String
    val isLoadingEmojiPack: Boolean
    val emojiPackError: String?
    val attachmentPanelOpen: Boolean
    val redPacketComposerOpen: Boolean
    val redPacketType: String
    val redPacketMoney: String
    val redPacketCount: String
    val redPacketMessage: String
    val redPacketReceivers: String
    val redPacketGesture: Int
    val isSendingRedPacket: Boolean
    val redPacketBalance: Long?
    val redPacketMoneyError: String?
    val redPacketCountError: String?
    val redPacketReceiversError: String?
    val isUploadingAttachment: Boolean
    val atCandidates: List<String>
    val gestureRedPacket: ChatRoomMessage?
    val redPacketResult: RedPacketOpenResult?
    val redPacketResultSource: ChatRoomMessage?
    val pendingAttachments: List<UploadedChatFile>
    val blockedMessagesOpen: Boolean
    val scrollToBottomRequest: Int
    val keepPositionAfterPrependCount: Int
    val pluginToolbarEntries: List<PluginToolbarEntry>
    val pluginMenuActions: List<PluginMenuAction>

    fun setPluginScene(scene: String)
    fun setPluginSystemMessageHandler(shouldFollowBottom: () -> Boolean)
    fun clearError()
    fun showError(reason: String)
    fun closeAttachmentPanel()
    fun refreshHistory(skipIfLoaded: Boolean = false, onSuccess: (List<ChatRoomMessage>) -> Unit = {}, onFailure: (String) -> Unit = {}, onFinally: () -> Unit = {})
    fun loadMoreHistory(onSuccess: (page: Int, older: List<ChatRoomMessage>, addedCount: Int) -> Unit = { _, _, _ -> }, onFailure: (String) -> Unit = {}, onFinally: () -> Unit = {})
    fun finishInitialLoading()
    fun requestJumpToLatest()
    fun showBlockedMessages()
    fun clearKeepPositionAfterPrependCount()
    fun onNearBottomChanged(nearBottom: Boolean)
    fun showImagePreview(url: String)
    fun showLinkPreview(url: String)
    fun replaceDraft(value: String, requestFocus: Boolean = false, resetInput: Boolean = false)
    fun setQuote(quote: ChatQuote?)
    fun removePendingAttachment(file: UploadedChatFile)
    fun clearFocusInputRequest()
    fun openAttachmentPanel()
    fun openPluginManager()
    fun emitPluginToolbarAction(entry: PluginToolbarEntry, actionId: String)
    fun emitPluginMenuAction(action: PluginMenuAction, message: ChatRoomMessage)
    fun dismissImagePreview()
    fun dismissLinkPreview()
    fun dismissMessageActions()
    fun dismissBlockedMessages()
    fun showMessageActions(message: ChatRoomMessage)
}

@Composable
private fun DefaultChatUiContent(
    chatFilters: ChatFilterConfig,
    openBlockedRequest: Int,
    active: Boolean,
    bridge: DefaultChatUiBridge,
    themeLabel: String = "",
    noticeUnread: Long = 0L,
    onCycleTheme: () -> Unit = {},
    onOpenNotice: () -> Unit = {},
    onFollowBottomChanged: (Boolean) -> Unit = {},
    onFollowBottomProbeChanged: ((() -> Boolean)?) -> Unit = {},
    onBlockedRequestHandled: () -> Unit = {},
    onOpenUserProfile: (String) -> Unit = {},
    onBack: () -> Unit = {},
    liveness: Double?,
    barrages: List<ChatBarrageUiModel>,
    barrageComposer: ChatBarrageComposerState,
    dispatch: (ChatAction) -> Unit,
) {
    val session = LocalAppSession.current
    val maxRetainedChatMessages = 520
    val trimChatMessagesTo = 440
    val chatListController = rememberNativeMessageListController()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val imageLoader = rememberFishPiImageLoader()
    val input = bridge.input
    val messages = bridge.messages
    val isLoading = bridge.isLoading
    val isLoadingMore = bridge.isLoadingMore
    val hasMoreHistory = bridge.hasMoreHistory
    val duplicateHistoryPageStreak = bridge.duplicateHistoryPageStreak
    val isSending = bridge.isSending
    val connectionLabel = bridge.connectionLabel
    val chatNodeName = bridge.chatNodeName
    val chatTopic = bridge.chatTopic
    val chatOnlineCount = bridge.chatOnlineCount
    val chatOnlineUsers = bridge.chatOnlineUsers
    val error = bridge.error

    val unreadNewMessages = bridge.unreadNewMessages
    val redPacketJumpTargetId = bridge.redPacketJumpTargetId
    val previewImageUrl = bridge.previewImageUrl
    val previewLinkUrl = bridge.previewLinkUrl
    var previewVideoUrl by remember { mutableStateOf<String?>(null) }
    var onlineUsersOpen by remember { mutableStateOf(false) }
    var chatActionsOpen by remember { mutableStateOf(false) }
    var topicEditorOpen by remember { mutableStateOf(false) }
    var topicDraft by remember { mutableStateOf("") }
    val pluginFloatPrefs = remember(context) {
        context.getSharedPreferences(ChatPluginFloatPrefsName, Context.MODE_PRIVATE)
    }
    var pluginFloatOpen by remember(pluginFloatPrefs) {
        mutableStateOf(!pluginFloatPrefs.getBoolean(ChatPluginFloatHiddenKey, false))
    }
    var expandedPluginId by remember { mutableStateOf<String?>(null) }
    var pluginFloatOffset by remember(pluginFloatPrefs) {
        mutableStateOf(
            pluginFloatPrefs.readPluginFloatOffset()
        )
    }
    val actionMessage = bridge.actionMessage
    val quote = bridge.quote
    val focusInputAfterQuote = bridge.focusInputAfterQuote
    val inputResetKey = bridge.inputResetKey
    val emojiPanelOpen = bridge.emojiPanelOpen
    val emojiGroups = bridge.emojiGroups
    val emojiItems = bridge.emojiItems
    val selectedEmojiGroupId = bridge.selectedEmojiGroupId
    val isLoadingEmojiPack = bridge.isLoadingEmojiPack
    val emojiPackError = bridge.emojiPackError
    val attachmentPanelOpen = bridge.attachmentPanelOpen
    val redPacketComposerOpen = bridge.redPacketComposerOpen
    val redPacketType = bridge.redPacketType
    val redPacketMoney = bridge.redPacketMoney
    val redPacketCount = bridge.redPacketCount
    val redPacketMessage = bridge.redPacketMessage
    val redPacketReceivers = bridge.redPacketReceivers
    val redPacketGesture = bridge.redPacketGesture
    val isSendingRedPacket = bridge.isSendingRedPacket
    val redPacketBalance = bridge.redPacketBalance
    val redPacketMoneyError = bridge.redPacketMoneyError
    val redPacketCountError = bridge.redPacketCountError
    val redPacketReceiversError = bridge.redPacketReceiversError

    val isUploadingAttachment = bridge.isUploadingAttachment
    val atCandidates = bridge.atCandidates
    val gestureRedPacket = bridge.gestureRedPacket
    val redPacketResult = bridge.redPacketResult
    val redPacketResultSource = bridge.redPacketResultSource
    val pendingAttachments = bridge.pendingAttachments
    val blockedMessagesOpen = bridge.blockedMessagesOpen
    var chatListNearBottom by remember { mutableStateOf(true) }
    var chatListNearTop by remember { mutableStateOf(false) }
    var inputCursorPositionRequest by remember { mutableStateOf<Int?>(null) }
    var inputCursorPosition by remember { mutableStateOf(input.length) }
    var actionAnchor by remember { mutableStateOf<MessageActionAnchor?>(null) }
    var pluginToolbarDismissRequest by remember { mutableStateOf(0) }
    val scrollToBottomRequest = bridge.scrollToBottomRequest
    var chatBoxOffsetInWindow by remember { mutableStateOf(IntOffset.Zero) }
    var chatBoxSize by remember { mutableStateOf(IntSize.Zero) }
    val keepPositionAfterPrependCount = bridge.keepPositionAfterPrependCount
    val preloadedImageUrls = remember { LinkedHashSet<String>() }
    val avatarRequestCache = remember { LinkedHashMap<String, ImageRequest>() }
    val renderHintCache = remember { LinkedHashMap<String, ChatMessageRenderHints>() }
    val listItemCache = remember { LinkedHashMap<String, ChatListItem>() }

    LaunchedEffect(Unit) {
        bridge.setPluginScene("chatRoom")
        bridge.setPluginSystemMessageHandler {
            chatListNearBottom && actionAnchor == null
        }
    }

    LaunchedEffect(error, messages.isNotEmpty()) {
        val message = error
        if (message != null && messages.isNotEmpty()) {
            FishPiNotifier.error(message)
            bridge.clearError()
        }
    }

    val chatMessageBuckets = remember(messages, chatFilters) {
        val blocked = ArrayList<ChatRoomMessage>()
        val visible = ArrayList<ChatRoomMessage>()
        messages.forEach { message ->
            if (chatFilters.blocksChatMessage(message)) {
                blocked.add(message)
            } else {
                visible.add(message)
            }
        }
        ChatMessageBuckets(
            blocked = blocked,
            visible = visible,
        )
    }
    val blockedMessages = chatMessageBuckets.blocked
    val visibleMessages = chatMessageBuckets.visible
    val avatarModels = remember(visibleMessages) {
        visibleMessages
            .asSequence()
            .map { it.userAvatarURL }
            .filter { it.isNotBlank() }
            .distinct()
            .associateWith { url ->
                avatarRequestCache.getOrPut(url) {
                    ImageRequest.Builder(context)
                        .data(url)
                        .size(Size(72, 72))
                        .build()
                }
            }
            .also {
                avatarRequestCache.retainRecentAvatarRequests(visibleMessages)
            }
    }
    val visibleMessageList = remember(visibleMessages, avatarModels) {
        val groups = buildStackedItems(visibleMessages)
        var msgIndex = 0
        val itemEndMessageIndexes = ArrayList<Int>(groups.size)
        val items = groups.map { group ->
            val message = group.messages.first()
            val previousMsg = if (msgIndex > 0) visibleMessages[msgIndex - 1] else null
            msgIndex += group.messages.size
            itemEndMessageIndexes.add(msgIndex - 1)

            val hintKey = message.renderHintCacheKey()
            val previousKey = previousMsg?.renderHintCacheKey().orEmpty()
            val itemKey = "$previousKey->$hintKey-${group.messages.size}"

            val repeatStack = if (group.messages.size > 1) {
                val participants = group.messages
                    .asSequence()
                    .map { it.userName.trim() to it.userAvatarURL }
                    .filter { (username, _) -> username.isNotBlank() }
                    .toList()
                RepeatStackInfo(
                    count = group.messages.size,
                    participantUsernames = participants.map { it.first },
                    participantAvatars = participants.map { it.second },
                )
            } else null

            listItemCache.getOrPut(itemKey) {
                ChatListItem(
                    message = message,
                    separator = if (repeatStack != null) null
                                else messageTimeSeparator(previousMsg, message),
                    renderHints = renderHintCache.getOrPut(hintKey) {
                        message.toRenderHints(avatarModel = avatarModels[message.userAvatarURL])
                    },
                    repeatStack = repeatStack,
                )
            }
        }
        renderHintCache.retainRecentRenderHints(visibleMessages)
        listItemCache.retainRecentListItems(visibleMessages)
        VisibleMessageList(
            items = items,
            itemEndMessageIndexes = itemEndMessageIndexes,
        )
    }
    val visibleMessageItems = visibleMessageList.items
    val visibleItemEndMessageIndexes = visibleMessageList.itemEndMessageIndexes
    val pluginToolbarGroups = remember(bridge.pluginToolbarEntries) {
        bridge.pluginToolbarEntries
            .groupBy { it.pluginId }
            .map { (pluginId, entries) ->
                ChatPluginToolbarGroup(
                    pluginId = pluginId,
                    title = entries.firstOrNull { it.title.isNotBlank() }?.title?.trim()
                        ?: pluginId.substringAfterLast('/').ifBlank { "插件" },
                    entries = entries,
                )
            }
    }
    LaunchedEffect(pluginToolbarGroups) {
        if (expandedPluginId != null && pluginToolbarGroups.none { it.pluginId == expandedPluginId }) {
            expandedPluginId = null
        }
    }
    val dispatchedIds = remember { mutableSetOf<String>() }
    LaunchedEffect(visibleMessages) {
        visibleMessages.forEach { msg ->
            if (dispatchedIds.add(msg.oId)) {
                val eventType = when { msg.redPacket != null -> "redPacket"; msg.type == "system" -> "system"; else -> "msg" }
                dispatch(ChatAction.NotifyPluginMessage(msg, eventType))
            }
        }
    }

    fun findRedPacketIndex(targetId: String): Int {
        return visibleMessages.indexOfFirst { message ->
            message.oId == targetId && message.redPacket?.openable == true
        }
    }

    fun openUserProfile(username: String) {
        val target = username.trim()
        if (target.isBlank()) return
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onOpenUserProfile(target)
    }

    val attachmentPicker = rememberChatAttachmentPicker(
        onPickedPath = { path -> dispatch(ChatAction.UploadAttachment(path)) },
        onError = { message -> bridge.showError(message) },
    )
    val chatToolActions = listOf(
        ChatToolAction(
            id = "gallery",
            label = "相册",
            icon = Icons.Rounded.PhotoLibrary,
            enabled = !isUploadingAttachment,
            iconTint = FishPiTheme.toolGallery,
            iconBackground = FishPiTheme.toolGallery.copy(alpha = 0.12f),
            onClick = {
                bridge.closeAttachmentPanel()
                attachmentPicker.openGallery()
            },
        ),
        ChatToolAction(
            id = "camera",
            label = "拍照",
            icon = Icons.Rounded.AddAPhoto,
            enabled = !isUploadingAttachment,
            iconTint = FishPiTheme.toolCamera,
            iconBackground = FishPiTheme.toolCamera.copy(alpha = 0.12f),
            onClick = {
                bridge.closeAttachmentPanel()
                attachmentPicker.openCamera()
            },
        ),
        ChatToolAction(
            id = "file",
            label = "文件",
            icon = Icons.Rounded.AttachFile,
            enabled = !isUploadingAttachment,
            iconTint = FishPiTheme.accent,
            iconBackground = FishPiTheme.accent.copy(alpha = 0.10f),
            onClick = {
                bridge.closeAttachmentPanel()
                attachmentPicker.openFile()
            },
        ),
        ChatToolAction(
            id = "red-packet",
            label = "红包",
            icon = Icons.Rounded.CardGiftcard,
            enabled = !isUploadingAttachment,
            iconTint = FishPiTheme.toolRedPacket,
            iconBackground = FishPiTheme.toolRedPacket.copy(alpha = 0.12f),
            onClick = {
                dispatch(ChatAction.OpenRedPacketComposer)
            },
        ),
        ChatToolAction(
            id = "barrager",
            label = "弹幕",
            icon = Icons.Rounded.AlternateEmail,
            enabled = !isUploadingAttachment,
            iconTint = FishPiTheme.accent,
            iconBackground = FishPiTheme.accent.copy(alpha = 0.10f),
            onClick = {
                bridge.closeAttachmentPanel()
                dispatch(ChatAction.OpenBarragerComposer)
            },
        ),
        ChatToolAction(
            id = "topic",
            label = "话题",
            icon = Icons.Rounded.Tag,
            enabled = true,
            iconTint = FishPiTheme.accent,
            iconBackground = FishPiTheme.accent.copy(alpha = 0.10f),
            onClick = {
                bridge.closeAttachmentPanel()
                topicDraft = ""
                topicEditorOpen = true
            },
        ),
        ChatToolAction(
            id = "plugins",
            label = "插件",
            icon = Icons.Rounded.Extension,
            enabled = true,
            iconTint = FishPiTheme.accent,
            iconBackground = FishPiTheme.accent.copy(alpha = 0.10f),
            onClick = {
                bridge.closeAttachmentPanel()
                bridge.openPluginManager()
            },
        ),
    )

    fun isNearBottom(currentVisibleMessages: List<ChatRoomMessage> = visibleMessages): Boolean {
        return active && (currentVisibleMessages.isEmpty() || chatListController.isNearBottom())
    }

    fun notifyFollowBottomState() {
        onFollowBottomChanged(active && actionAnchor == null && isNearBottom())
    }

    DisposableEffect(Unit) {
        onFollowBottomProbeChanged {
            actionAnchor == null && isNearBottom()
        }
        onDispose {
            onFollowBottomProbeChanged(null)
        }
    }

    fun refresh() {
        bridge.refreshHistory(
            onFailure = { reason ->
                bridge.showError(reason)
            },
            onFinally = {
                bridge.finishInitialLoading()
            },
        )
    }

    fun loadMoreHistory() {
        chatListController.capturePrependAnchor()
        bridge.loadMoreHistory(
            onFailure = { reason ->
                bridge.showError(reason)
            },
        )
    }

    LaunchedEffect(session.apiKey) {
        if (messages.isEmpty()) {
            refresh()
        } else {
            bridge.finishInitialLoading()
            bridge.requestJumpToLatest()
        }
    }

    LaunchedEffect(active, chatListNearBottom, actionAnchor, visibleMessages.size) {
        notifyFollowBottomState()
    }

    LaunchedEffect(openBlockedRequest) {
        if (openBlockedRequest > 0) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            bridge.showBlockedMessages()
            onBlockedRequestHandled()
        }
    }

    LaunchedEffect(keepPositionAfterPrependCount) {
        if (keepPositionAfterPrependCount > 0) {
            chatListController.keepPositionAfterPrepend(keepPositionAfterPrependCount)
            bridge.clearKeepPositionAfterPrependCount()
        }
    }

    LaunchedEffect(visibleMessages) {
        delay(180)
        val candidates = visibleMessages
            .takeLast(60)
            .flatMap { it.preloadImages() }
            .distinctBy { it.url }
            .take(90)
            .filterNot { it.url in preloadedImageUrls }

        candidates.forEach { item ->
            imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(item.url)
                    .size(Size(item.width, item.height))
                    .build(),
            )
            preloadedImageUrls.add(item.url)
            if (preloadedImageUrls.size > 420) {
                val oldest = preloadedImageUrls.iterator()
                repeat(80) {
                    if (oldest.hasNext()) {
                        oldest.next()
                        oldest.remove()
                    }
                }
            }
        }
    }

    LaunchedEffect(input) {
        val at = extractAtQuery(input)
        if (at == null || at.second.isBlank()) {
            dispatch(ChatAction.SearchMention(at?.first, at?.second))
            return@LaunchedEffect
        }

        delay(220)
        dispatch(ChatAction.SearchMention(at.first, at.second))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .fishPiChatWallpaper(),
    ) {
        when {
            isLoading && messages.isEmpty() -> LoadingScreen("加载聊天室历史...")
            error != null && messages.isEmpty() -> ErrorState(error.orEmpty(), onRetry = { refresh() })
            else -> Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        val position = coordinates.positionInWindow()
                        chatBoxOffsetInWindow = IntOffset(position.x.toInt(), position.y.toInt())
                        chatBoxSize = coordinates.size
                    },
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                ChatRoomContextBar(
                    nodeName = chatNodeName,
                    topic = chatTopic,
                    onlineCount = chatOnlineCount,
                    visibleCount = visibleMessages.size,
                    connectionLabel = connectionLabel,
                    themeLabel = themeLabel,
                    noticeUnread = noticeUnread,
                    onlineUsers = chatOnlineUsers,
                    onQuoteTopic = {
                        bridge.replaceDraft(appendTopicReferenceDraft(input, chatTopic), requestFocus = true, resetInput = true)
                    },
                    onRefresh = { refresh() },
                    onReconnect = { dispatch(ChatAction.Reconnect) },
                    onOpenBlocked = { bridge.showBlockedMessages() },
                    onCycleTheme = onCycleTheme,
                    onOpenNotice = onOpenNotice,
                    onOpenOnlineUsers = { onlineUsersOpen = true },
                    onOpenChatActions = { chatActionsOpen = true },
                    onOpenPlugins = { bridge.openPluginManager() },
                    onBack = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    NativeMessageList(
                        items = visibleMessageItems,
                        selfUsername = session.user.userName,
                        showAvatars = chatFilters.showAvatars,
                        scrollToBottomRequest = scrollToBottomRequest,
                        allowScrollToBottom = actionAnchor == null,
                        redPacketJumpTargetId = redPacketJumpTargetId,
                        active = active,
                        contentTopPaddingDp = 0,
                        drawBackground = false,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = FishPiTheme.spacingPage),
                        onLoadMore = { loadMoreHistory() },
                        onNearBottomChanged = { nearBottom ->
                            chatListNearBottom = nearBottom
                            if (active) {
                                bridge.onNearBottomChanged(nearBottom)
                            }
                            notifyFollowBottomState()
                        },
                        onNearTopChanged = { nearTop ->
                            chatListNearTop = nearTop
                        },
                        onVisibleRangeChanged = { _, lastVisible, _ ->
                            if (active && unreadNewMessages > 0 && visibleMessages.isNotEmpty() && lastVisible >= 0) {
                                val lastReadMessageIndex = visibleItemEndMessageIndexes
                                    .getOrNull(lastVisible)
                                    ?: return@NativeMessageList
                                val remaining = (visibleMessages.lastIndex - lastReadMessageIndex)
                                    .coerceAtLeast(0)
                                    .coerceAtMost(unreadNewMessages)
                                if (remaining < unreadNewMessages) {
                                    dispatch(ChatAction.NewMessagesRemainingChanged(remaining))
                                }
                            }
                        },
                        onImageClick = { bridge.showImagePreview(it) },
                        onLinkClick = { bridge.showLinkPreview(it) },
                        onLongPress = { anchor -> actionAnchor = anchor },
                        onAvatarClick = { openUserProfile(it) },
                        onAvatarLongPress = { username ->
                            val (nextInput, cursor) = insertMentionDraft(input, inputCursorPosition, username)
                            bridge.replaceDraft(nextInput, requestFocus = true, resetInput = true)
                            inputCursorPositionRequest = cursor
                            inputCursorPosition = cursor
                        },
                        onRedPacketClick = { message ->
                            dispatch(ChatAction.ClickRedPacket(message))
                        },
                        onRedPacketGestureClick = { message, gesture ->
                            dispatch(ChatAction.OpenRedPacket(message, gesture))
                        },
                        onReactionClick = { message, value ->
                            dispatch(ChatAction.ReactToMessage(message, value))
                        },
                        onRepeatClick = { message ->
                            dispatch(ChatAction.RepeatMessage(message))
                        },
                        onVideoFullscreenClick = { url ->
                            previewVideoUrl = url
                        },
                        onTapBlankArea = {
                            bridge.closeAttachmentPanel()
                            dispatch(ChatAction.CloseEmojiPanel)
                            expandedPluginId = null
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        },
                        controller = chatListController,
                    )
                    ChatBarrageOverlay(
                        barrages = barrages,
                        onFinished = { dispatch(ChatAction.ClearBarrager(it)) },
                        modifier = Modifier
                            .matchParentSize()
                            .zIndex(3f),
                    )
                if (emojiPanelOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(6f)
                            .silentTap { dispatch(ChatAction.CloseEmojiPanel) },
                    )
                }

                if (chatListNearTop) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 14.dp)
                            .zIndex(5f),
                    ) {
                        ChatOverlayPill(
                            text = when {
                                isLoadingMore -> "正在加载更早消息..."
                                !hasMoreHistory || duplicateHistoryPageStreak >= 6 -> "已经到最早消息了"
                                else -> "加载更多"
                            },
                            onClick = {
                                if (!isLoadingMore && hasMoreHistory && duplicateHistoryPageStreak < 6) {
                                    loadMoreHistory()
                                }
                            },
                        )
                    }
                }

                ChatFloatingActions(
                    unreadNewMessages = unreadNewMessages,
                    redPacketJumpTargetId = redPacketJumpTargetId,
                    onJumpToLatest = { bridge.requestJumpToLatest() },
                    onJumpToRedPacket = { targetId ->
                        val index = findRedPacketIndex(targetId)
                        if (index >= 0) {
                            chatListController.scrollToMessage(targetId)
                        }
                        dispatch(ChatAction.ClearRedPacketJumpTarget)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = FishPiTheme.spacingPage + FishPiTheme.spacingControl, bottom = 7.dp)
                        .zIndex(5f),
                )

                ChatLivenessFloatingOrb(
                    liveness = liveness,
                    positionKey = session.user.userName.ifBlank { "chat-room" },
                    modifier = Modifier
                        .matchParentSize()
                        .zIndex(4f),
                )
                if (pluginToolbarGroups.isNotEmpty()) {
                    ChatPluginFloatingPanel(
                        groups = pluginToolbarGroups,
                        expandedPluginId = expandedPluginId,
                        expanded = pluginFloatOpen,
                        offset = pluginFloatOffset,
                        onOffsetChange = { nextOffset ->
                            pluginFloatOffset = nextOffset
                            if (!nextOffset.isUnsetPluginOffset()) {
                                pluginFloatPrefs.edit()
                                    .putInt(ChatPluginFloatXKey, nextOffset.x)
                                    .putInt(ChatPluginFloatYKey, nextOffset.y)
                                    .apply()
                            }
                        },
                        onDismiss = {
                            expandedPluginId = null
                        },
                        onSelectPlugin = { group ->
                            expandedPluginId = if (expandedPluginId == group.pluginId) null else group.pluginId
                        },
                        onAction = { entry, action ->
                            if (action.subtitle.isNotBlank()) FishPiNotifier.show(action.subtitle)
                            bridge.emitPluginToolbarAction(entry, action.id)
                        },
                        modifier = Modifier
                            .matchParentSize()
                            .zIndex(7f),
                    )
                }
                }
            }
        }

        ChatInputBar(
            input = input,
            quote = quote,
            focusInputAfterQuote = focusInputAfterQuote,
            inputResetKey = inputResetKey,
            isUploadingAttachment = isUploadingAttachment,
            pendingAttachments = pendingAttachments,
            atCandidates = atCandidates,
            sendOnEnter = chatFilters.sendOnEnter,
            emojiPanelOpen = emojiPanelOpen,
            emojiGroups = emojiGroups,
            emojiItems = emojiItems,
            selectedEmojiGroupId = selectedEmojiGroupId,
            isLoadingEmojiPack = isLoadingEmojiPack,
            emojiPackError = emojiPackError,
            toolPanelOpen = attachmentPanelOpen,
            toolActions = chatToolActions,
            pluginToolbarEntries = bridge.pluginToolbarEntries,
            pluginToolbarDismissRequest = pluginToolbarDismissRequest,
            currentTopicLabel = chatTopic.trim().takeIf { it.isNotBlank() && it != "暂无" }?.let { "#$it#" }.orEmpty(),
            onInputChange = { dispatch(ChatAction.ChangeInput(it)) },
            onCancelQuote = { bridge.setQuote(null) },
            onRemoveAttachment = { file ->
                bridge.removePendingAttachment(file)
            },
            onInputFocused = { bridge.clearFocusInputRequest() },
            onOpenTools = { bridge.openAttachmentPanel() },
            onDismissToolPanel = { bridge.closeAttachmentPanel() },
            onPluginToolbarAction = { entry, actionId ->
                bridge.emitPluginToolbarAction(entry, actionId)
            },
            onPickCurrentTopic = {},
            onToggleEmojiPanel = { dispatch(ChatAction.ToggleEmoji) },
            onDismissEmojiPanel = { dispatch(ChatAction.CloseEmojiPanel) },
            onPickEmojiGroup = { dispatch(ChatAction.SelectEmojiGroup(it)) },
            onPickEmoji = { item ->
                dispatch(ChatAction.PickEmoji(item.name, item.url))
            },
            onPickAtUser = { username ->
                dispatch(ChatAction.PickMention(username))
            },
            onCursorPositionChange = { inputCursorPosition = it },
            inputCursorPositionRequest = inputCursorPositionRequest,
            onInputCursorPositionRequestHandled = { inputCursorPositionRequest = null },
            onSend = { sendText ->
                val content = normalizeTopicReferenceDraft(sendText).trim()
                if (content.isBlank() || isSending) {
                    return@ChatInputBar
                }
                dispatch(ChatAction.SendText(content))
            },
            modifier = Modifier
                .padding(
                    start = FishPiTheme.spacingPage,
                    end = FishPiTheme.spacingPage,
                    top = 2.dp,
                    bottom = 8.dp,
                )
                .imePadding(),
        )
    }

    ChatOverlayHost {
        if (onlineUsersOpen) {
            ChatOnlineUsersPage(
                onlineCount = chatOnlineCount,
                users = chatOnlineUsers,
                onDismiss = { onlineUsersOpen = false },
                onOpenUser = { username ->
                    openUserProfile(username)
                },
            )
        }
        if (chatActionsOpen) {
            ChatActionsSheet(
                onlineCount = chatOnlineCount,
                themeLabel = themeLabel,
                noticeUnread = noticeUnread,
                pluginFloatAvailable = pluginToolbarGroups.isNotEmpty(),
                pluginFloatVisible = pluginFloatOpen,
                onDismiss = { chatActionsOpen = false },
                onOpenNotice = {
                    chatActionsOpen = false
                    onOpenNotice()
                },
                onOpenOnlineUsers = {
                    chatActionsOpen = false
                    onlineUsersOpen = true
                },
                onRefresh = {
                    chatActionsOpen = false
                    refresh()
                },
                onReconnect = {
                    chatActionsOpen = false
                    dispatch(ChatAction.Reconnect)
                },
                onCycleTheme = {
                    chatActionsOpen = false
                    onCycleTheme()
                },
                onOpenBlocked = {
                    chatActionsOpen = false
                    bridge.showBlockedMessages()
                },
                onTogglePluginFloat = {
                    chatActionsOpen = false
                    pluginFloatOpen = !pluginFloatOpen
                    if (!pluginFloatOpen) {
                        expandedPluginId = null
                    }
                    pluginFloatPrefs.edit()
                        .putBoolean(ChatPluginFloatHiddenKey, !pluginFloatOpen)
                        .apply()
                },
            )
        }
        previewImageUrl?.let { imageUrl ->
            ImagePreviewOverlay(imageUrl = imageUrl, onDismiss = { bridge.dismissImagePreview() })
        }

        previewLinkUrl?.let { linkUrl ->
            LinkPreviewOverlay(url = linkUrl, apiKey = session.apiKey, onDismiss = { bridge.dismissLinkPreview() })
        }

        previewVideoUrl?.let { videoUrl ->
            VideoPlaybackOverlay(url = videoUrl, onDismiss = { previewVideoUrl = null })
        }

        actionAnchor?.let { anchor ->
            val pluginMessageActions = bridge.pluginMenuActions.map { pluginAction ->
                MessageActionSpec(
                    label = pluginAction.label,
                    icon = Icons.Rounded.Extension,
                    enabled = pluginAction.enabled,
                    onClick = { bridge.emitPluginMenuAction(pluginAction, anchor.message) },
                )
            }
            MessageActionBubbleMenu(
            anchor = anchor,
            rootOffsetInWindow = chatBoxOffsetInWindow,
            rootSize = chatBoxSize,
            canRevoke = anchor.message.canBeRevokedBy(session),
            onDismiss = { actionAnchor = null },
            onCopyContent = { context.copyToClipboard("消息内容", anchor.message.copyableText()) },
            onCopyUsername = {
                context.copyToClipboard("用户名", anchor.message.userName.ifBlank { anchor.message.displayName })
            },
            onCopyImageLinks = { context.copyToClipboard("图片链接", anchor.message.imageUrls.joinToString("\n")) },
            onCopyLinks = { context.copyToClipboard("链接", anchor.message.linkUrls.joinToString("\n")) },
            onMentionUser = {
                bridge.replaceDraft(appendMentionDraft(input, anchor.message.userName), requestFocus = true, resetInput = true)
            },
            onQuote = { bridge.setQuote(anchor.message.toQuote()) },
            onReaction = { value ->
                dispatch(ChatAction.ReactToMessage(anchor.message, value))
            },
            onRepeat = {
                dispatch(ChatAction.RepeatMessage(anchor.message))
            },
            onRevoke = {
                dispatch(ChatAction.RevokeMessage(anchor.message))
            },
            extraActions = pluginMessageActions,
            )
        }

        actionMessage?.let { message ->
            val pluginMessageActions = bridge.pluginMenuActions.map { pluginAction ->
                MessageActionSpec(
                    label = pluginAction.label,
                    icon = Icons.Rounded.Extension,
                    enabled = pluginAction.enabled,
                    onClick = { bridge.emitPluginMenuAction(pluginAction, message) },
                )
            }
            MessageActionSheet(
            message = message,
            canRevoke = message.canBeRevokedBy(session),
            onDismiss = { bridge.dismissMessageActions() },
            onCopyContent = { context.copyToClipboard("消息内容", message.copyableText()) },
            onCopyUsername = {
                context.copyToClipboard("用户名", message.userName.ifBlank { message.displayName })
            },
            onCopyImageLinks = { context.copyToClipboard("图片链接", message.imageUrls.joinToString("\n")) },
            onCopyLinks = { context.copyToClipboard("链接", message.linkUrls.joinToString("\n")) },
            onMentionUser = {
                bridge.replaceDraft(appendMentionDraft(input, message.userName), requestFocus = true, resetInput = true)
            },
            onQuote = {
                bridge.setQuote(message.toQuote())
            },
            onReaction = { value ->
                dispatch(ChatAction.ReactToMessage(message, value))
            },
            onRepeat = {
                dispatch(ChatAction.RepeatMessage(message))
            },
            onRevoke = {
                dispatch(ChatAction.RevokeMessage(message))
            },
            extraActions = pluginMessageActions,
            )
        }

        if (barrageComposer.open) {
            ChatBarrageComposerDialog(
                state = barrageComposer,
                onChange = { dispatch(ChatAction.ChangeBarragerContent(it)) },
                onSend = { dispatch(ChatAction.SendBarrager) },
                onDismiss = { dispatch(ChatAction.DismissBarragerComposer) },
            )
        }

        if (topicEditorOpen) {
            ChatTopicEditorDialog(
                value = topicDraft,
                currentTopic = chatTopic,
                onChange = { topicDraft = it.take(64) },
                onConfirm = {
                    dispatch(ChatAction.SetTopic(topicDraft))
                    topicEditorOpen = false
                },
                onDismiss = { topicEditorOpen = false },
            )
        }

        if (redPacketComposerOpen) {
            Dialog(
            onDismissRequest = { dispatch(ChatAction.DismissRedPacketComposer) },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            DefaultRedPacketSendUi(
                form = RedPacketFormState(
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
                ),
                dispatch = { action ->
                    when (action) {
                        RedPacketAction.SendClicked -> dispatch(ChatAction.SendRedPacket)
                        RedPacketAction.Dismiss -> dispatch(ChatAction.DismissRedPacketComposer)
                        is RedPacketAction.TypeChanged -> dispatch(ChatAction.ChangeRedPacketType(action.value))
                        is RedPacketAction.MoneyChanged -> dispatch(ChatAction.ChangeRedPacketMoney(action.value))
                        is RedPacketAction.CountChanged -> dispatch(ChatAction.ChangeRedPacketCount(action.value))
                        is RedPacketAction.MessageChanged -> dispatch(ChatAction.ChangeRedPacketMessage(action.value))
                        is RedPacketAction.ReceiversChanged -> dispatch(ChatAction.ChangeRedPacketReceivers(action.value))
                        is RedPacketAction.GesturePicked -> dispatch(ChatAction.ChangeRedPacketGesture(action.value))
                        RedPacketAction.OpenClicked,
                        RedPacketAction.DismissResult -> Unit
                    }
                },
            )
            }
        }

        gestureRedPacket?.let { message ->
            DefaultRedPacketGestureUi(
                selectedGesture = message.redPacket?.gesture,
                dispatch = { action ->
                    when (action) {
                        RedPacketAction.Dismiss -> dispatch(ChatAction.ClearGestureRedPacket)
                        is RedPacketAction.GesturePicked -> dispatch(ChatAction.OpenRedPacket(message, action.value))
                        RedPacketAction.SendClicked,
                        RedPacketAction.OpenClicked,
                        RedPacketAction.DismissResult,
                        is RedPacketAction.TypeChanged,
                        is RedPacketAction.MoneyChanged,
                        is RedPacketAction.CountChanged,
                        is RedPacketAction.MessageChanged,
                        is RedPacketAction.ReceiversChanged -> Unit
                    }
                },
            )
        }

        redPacketResult?.let { result ->
            Dialog(
            onDismissRequest = { dispatch(ChatAction.ClearRedPacketResult) },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            DefaultRedPacketResultUi(
                state = result.toRedPacketResultUiModel(
                    senderName = redPacketResultSource?.userName.orEmpty(),
                    senderAvatar = redPacketResultSource?.userAvatarURL.orEmpty(),
                    packetMessage = redPacketResultSource?.redPacket?.message.orEmpty(),
                    selfUsername = session.user.userName,
                    finished = redPacketResultSource?.redPacket?.finished == true || result.count > 0L && result.got >= result.count,
                ),
                dispatch = { action ->
                    if (action is RedPacketAction.DismissResult) {
                        dispatch(ChatAction.ClearRedPacketResult)
                    }
                },
            )
            }
        }

        if (blockedMessagesOpen) {
            Dialog(
            onDismissRequest = { bridge.dismissBlockedMessages() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            BlockedMessagesOverlay(
                messages = blockedMessages,
                selfUsername = session.user.userName,
                onImageClick = {
                    bridge.dismissBlockedMessages()
                    bridge.showImagePreview(it)
                },
                onLinkClick = {
                    bridge.dismissBlockedMessages()
                    bridge.showLinkPreview(it)
                },
                onAvatarClick = {
                    bridge.dismissBlockedMessages()
                    openUserProfile(it)
                },
                onAvatarLongPress = { username ->
                    bridge.dismissBlockedMessages()
                    val (nextInput, cursor) = insertMentionDraft(input, inputCursorPosition, username)
                    bridge.replaceDraft(nextInput, requestFocus = true, resetInput = true)
                    inputCursorPositionRequest = cursor
                    inputCursorPosition = cursor
                },
                onRedPacketClick = { message ->
                    bridge.dismissBlockedMessages()
                    dispatch(ChatAction.ClickRedPacket(message))
                },
                onReactionClick = { message, value ->
                    dispatch(ChatAction.ReactToMessage(message, value))
                },
                onLongPress = { anchor ->
                    bridge.dismissBlockedMessages()
                    bridge.showMessageActions(anchor.message)
                },
                onDismiss = { bridge.dismissBlockedMessages() },
            )
            }
        }
    }

    BackHandler(enabled = active && previewImageUrl != null) {
        bridge.dismissImagePreview()
    }

    BackHandler(enabled = active && previewLinkUrl != null) {
        bridge.dismissLinkPreview()
    }

    BackHandler(enabled = active && previewVideoUrl != null) {
        previewVideoUrl = null
    }

    BackHandler(enabled = active && actionAnchor != null) {
        actionAnchor = null
    }

    BackHandler(enabled = active && actionMessage != null) {
        bridge.dismissMessageActions()
    }

    BackHandler(enabled = active && attachmentPanelOpen) {
        bridge.closeAttachmentPanel()
    }

    BackHandler(enabled = active && barrageComposer.open && !barrageComposer.isSending) {
        dispatch(ChatAction.DismissBarragerComposer)
    }

    BackHandler(enabled = active && topicEditorOpen) {
        topicEditorOpen = false
    }

    BackHandler(enabled = active && redPacketComposerOpen && !isSendingRedPacket) {
        dispatch(ChatAction.DismissRedPacketComposer)
    }

    BackHandler(enabled = active && gestureRedPacket != null) {
        dispatch(ChatAction.ClearGestureRedPacket)
    }

    BackHandler(enabled = active && redPacketResult != null) {
        dispatch(ChatAction.ClearRedPacketResult)
    }

    BackHandler(enabled = active && blockedMessagesOpen) {
        bridge.dismissBlockedMessages()
    }
}

private data class ChatMessageBuckets(
    val blocked: List<ChatRoomMessage>,
    val visible: List<ChatRoomMessage>,
)

private data class VisibleMessageList(
    val items: List<ChatListItem>,
    val itemEndMessageIndexes: List<Int>,
)

private fun ChatRoomMessage.renderHintCacheKey(): String {
    val redPacketKey = redPacket?.let { packet ->
        listOf(
            packet.type,
            packet.money.toString(),
            packet.count.toString(),
            packet.got.toString(),
            packet.finished.toString(),
            packet.openable.toString(),
            packet.needGesture.toString(),
            packet.message,
        ).joinToString(":")
    }.orEmpty()
    return listOf(
        stableMessageIdentity(),
        type,
        revoked.toString(),
        userAvatarURL,
        client,
        time,
        contentHtml.hashCode().toString(),
        linkUrls.hashCode().toString(),
        imageUrls.hashCode().toString(),
        reactionSummary.hashCode().toString(),
        currentUserReaction,
        redPacketKey,
    ).joinToString("|")
}

private fun LinkedHashMap<String, ChatMessageRenderHints>.retainRecentRenderHints(
    visibleMessages: List<ChatRoomMessage>,
) {
    val activeKeys = visibleMessages.takeLast(180).mapTo(HashSet()) { it.renderHintCacheKey() }
    entries.removeIf { (key, _) -> key !in activeKeys }
}

private fun LinkedHashMap<String, ChatListItem>.retainRecentListItems(
    visibleMessages: List<ChatRoomMessage>,
) {
    val recentMessages = visibleMessages.takeLast(180)
    val firstRecentIndex = visibleMessages.size - recentMessages.size
    val activeKeys = recentMessages.mapIndexedTo(HashSet()) { index, message ->
        val previousKey = when {
            index > 0 -> recentMessages[index - 1].renderHintCacheKey()
            firstRecentIndex > 0 -> visibleMessages[firstRecentIndex - 1].renderHintCacheKey()
            else -> ""
        }
        "$previousKey->${message.renderHintCacheKey()}"
    }
    entries.removeIf { (key, _) -> key !in activeKeys }
}

private fun LinkedHashMap<String, ImageRequest>.retainRecentAvatarRequests(
    visibleMessages: List<ChatRoomMessage>,
) {
    val activeUrls = visibleMessages
        .takeLast(220)
        .mapNotNullTo(HashSet()) { message -> message.userAvatarURL.takeIf { it.isNotBlank() } }
    entries.removeIf { (url, _) -> url !in activeUrls }
}

private data class ChatPreloadImage(
    val url: String,
    val width: Int,
    val height: Int,
)

private fun ChatRoomMessage.preloadImages(): List<ChatPreloadImage> {
    return buildList {
        userAvatarURL.takeIf { it.isNotBlank() }?.let {
            add(ChatPreloadImage(it, 72, 72))
        }
        quote?.imageUrls
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() && !it.isAnimatedListImageUrl() }
            ?.let {
                add(ChatPreloadImage(it, 96, 96))
            }
        imageUrls
            .firstOrNull()
            ?.takeIf { it.isNotBlank() && !it.isAnimatedListImageUrl() }
            ?.let {
                add(ChatPreloadImage(it, 440, 326))
            }
    }
}

private fun String.isAnimatedListImageUrl(): Boolean {
    val path = substringBefore('?').substringBefore('#').lowercase()
    return path.endsWith(".gif") || contains("image/gif", ignoreCase = true)
}

private fun ChatRoomMessage.stableListKey(): String =
    oId.ifBlank { "$time:$userName:${content.hashCode()}" }

private fun ChatRoomMessage.stableMessageIdentity(): String =
    oId.ifBlank { "$time:${displayName}:$content" }

private fun shouldStack(message: ChatRoomMessage): Boolean {
    return message.type != "system" && message.type != "redPacket" && message.redPacket == null
}

private fun ChatRoomMessage.repeatStackKey(): String {
    val mediaUrls = (allRenderableImageUrls() + linkUrls.filter { it.isDirectVideoUrl() })
        .distinct()
        .joinToString("\u001F")
    return listOf(content, mediaUrls).joinToString("\u001E")
}

private data class StackGroup(val messages: List<ChatRoomMessage>)

private fun buildStackedItems(messages: List<ChatRoomMessage>): List<StackGroup> {
    val groups = mutableListOf<StackGroup>()
    var i = 0
    while (i < messages.size) {
        val current = messages[i]
        if (!shouldStack(current)) {
            groups.add(StackGroup(listOf(current)))
            i++
            continue
        }
        val currentKey = current.repeatStackKey()
        var j = i + 1
        while (j < messages.size &&
            shouldStack(messages[j]) &&
            messages[j].repeatStackKey() == currentKey
        ) {
            j++
        }
        groups.add(StackGroup(messages.subList(i, j)))
        i = j
    }
    return groups
}

internal fun ChatFilterConfig.blocksChatMessage(message: ChatRoomMessage): Boolean {
    val sender = message.userName.trim()
    val author = message.displayName.trim()
    blockedUsers.forEach { item ->
        val blocked = item.trim()
        if (blocked.isNotBlank() &&
            (sender.equals(blocked, ignoreCase = true) || author.equals(blocked, ignoreCase = true))
        ) {
            return true
        }
    }

    val content = message.content.lowercase()
    blockedKeywords.forEach { item ->
        val keyword = item.trim().lowercase()
        if (keyword.isNotBlank() && content.contains(keyword)) {
            return true
        }
    }

    blockedPrefixKeywords.forEach { item ->
        val prefix = item.trim().lowercase()
        if (prefix.isNotBlank() && content.startsWith(prefix)) {
            return true
        }
    }

    if (matchesBlockedRegex(message.content)) {
        return true
    }

    return false
}

private fun ChatFilterConfig.activeRuleCount(): Int =
    blockedUsers.size + blockedKeywords.size + blockedPrefixKeywords.size + blockedRegex.size

private fun List<String>.withUniqueRule(rule: String, exactCase: Boolean = false): List<String> {
    val value = rule.trim()
    if (value.isBlank()) {
        return this
    }
    val exists = if (exactCase) {
        any { it == value }
    } else {
        any { it.equals(value, ignoreCase = true) }
    }
    return if (exists) this else this + value
}

private fun formatTopicReferenceMarkdown(topic: String): String {
    val text = topic.trim().trim('#').trim()
    return if (text.isBlank()) "" else "*`# $text #`*"
}

private fun appendTopicReferenceDraft(current: String, topic: String): String {
    val reference = formatTopicReferenceMarkdown(topic)
    if (reference.isBlank()) {
        return current
    }
    return appendDraftBlock(current, reference)
}

private fun replaceTopicTriggerDraft(current: String, topic: String): String {
    val reference = formatTopicReferenceMarkdown(topic)
    if (reference.isBlank()) {
        return current
    }
    val trimmed = current.trimEnd()
    val tokenStart = trimmed.indexOfLast { it.isWhitespace() } + 1
    val token = trimmed.substring(tokenStart)
    if (!token.startsWith("#") || token.count { it == '#' } != 1) {
        return appendDraftBlock(current, reference)
    }
    val prefix = trimmed.substring(0, tokenStart)
    return (prefix + reference).trimEnd()
}

private fun normalizeTopicReferenceDraft(input: String): String {
    val text = input.trim()
    val match = Regex("""\*`#\s*.+?\s*#`\*""").find(text) ?: return input
    val reference = match.value
    val before = text.substring(0, match.range.first).trim()
    val after = text.substring(match.range.last + 1).trim()

    return buildList {
        if (before.isNotBlank()) add(before)
        if (after.isNotBlank()) add(after)
        add(reference)
    }.joinToString("\n")
}

private fun extractAtQuery(input: String): Pair<Int, String>? {
    if (input.lastOrNull()?.isWhitespace() == true) {
        return null
    }
    val text = input.trimEnd()
    val atIndex = maxOf(text.lastIndexOf('@'), text.lastIndexOf('＠'))
    if (atIndex < 0) {
        return null
    }
    val prefix = text.substring(0, atIndex)
    val before = prefix.lastOrNull()
    if (before != null && !before.isWhitespace() && before !in "([{<\"'，。！？、；：") {
        return null
    }
    val query = text.substring(atIndex + 1).trim()
    if (query.isEmpty() || query.any { it.isWhitespace() }) {
        return null
    }
    return atIndex to query
}

@Composable
internal fun ChatFilterSettingsOverlay(
    config: ChatFilterConfig,
    onSave: (ChatFilterConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    var userInput by remember { mutableStateOf("") }
    var keywordInput by remember { mutableStateOf("") }
    var prefixInput by remember { mutableStateOf("") }
    var regexInput by remember { mutableStateOf("") }
    var regexError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FishPiTheme.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(FishPiTheme.spacingPage),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "聊天室过滤",
                        color = FishPiTheme.accent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    FishPiIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = "关闭过滤设置",
                        onClick = onDismiss,
                    )
                }
            }
            item {
                ChatAvatarSettingRow(
                    showAvatars = config.showAvatars,
                    onToggle = { onSave(config.copy(showAvatars = !config.showAvatars)) },
                )
            }
            item {
                ChatSendOnEnterSettingRow(
                    sendOnEnter = config.sendOnEnter,
                    onToggle = { onSave(config.copy(sendOnEnter = !config.sendOnEnter)) },
                )
            }
            item {
                FilterRuleEditor(
                    title = "屏蔽用户",
                    hint = "输入用户名",
                    value = userInput,
                    rules = config.blockedUsers,
                    onValueChange = { userInput = it },
                    onAdd = {
                        val next = config.copy(blockedUsers = config.blockedUsers.withUniqueRule(userInput))
                        userInput = ""
                        onSave(next)
                    },
                    onRemove = { rule ->
                        onSave(config.copy(blockedUsers = config.blockedUsers.filterNot { it == rule }))
                    },
                )
            }
            item {
                FilterRuleEditor(
                    title = "关键词屏蔽",
                    hint = "输入关键词",
                    value = keywordInput,
                    rules = config.blockedKeywords,
                    onValueChange = { keywordInput = it },
                    onAdd = {
                        val next = config.copy(blockedKeywords = config.blockedKeywords.withUniqueRule(keywordInput))
                        keywordInput = ""
                        onSave(next)
                    },
                    onRemove = { rule ->
                        onSave(config.copy(blockedKeywords = config.blockedKeywords.filterNot { it == rule }))
                    },
                )
            }
            item {
                FilterRuleEditor(
                    title = "开头关键词屏蔽",
                    hint = "输入开头关键词",
                    value = prefixInput,
                    rules = config.blockedPrefixKeywords,
                    onValueChange = { prefixInput = it },
                    onAdd = {
                        val next = config.copy(
                            blockedPrefixKeywords = config.blockedPrefixKeywords.withUniqueRule(prefixInput),
                        )
                        prefixInput = ""
                        onSave(next)
                    },
                    onRemove = { rule ->
                        onSave(config.copy(blockedPrefixKeywords = config.blockedPrefixKeywords.filterNot { it == rule }))
                    },
                )
            }
            item {
                FilterRuleEditor(
                    title = "正则屏蔽",
                    hint = "输入正则，如 (?i)广告|推广",
                    value = regexInput,
                    rules = config.blockedRegex,
                    onValueChange = {
                        regexInput = it
                        regexError = null
                    },
                    onAdd = {
                        val raw = regexInput.trim()
                        val compiled = runCatching { Regex(raw) }
                        if (raw.isBlank()) {
                            return@FilterRuleEditor
                        }
                        if (compiled.isFailure) {
                            regexError = "正则无效：${compiled.exceptionOrNull()?.message.orEmpty()}"
                            return@FilterRuleEditor
                        }
                        val next = config.copy(blockedRegex = config.blockedRegex.withUniqueRule(raw, exactCase = true))
                        regexInput = ""
                        regexError = null
                        onSave(next)
                    },
                    onRemove = { rule ->
                        onSave(config.copy(blockedRegex = config.blockedRegex.filterNot { it == rule }))
                    },
                )
                regexError?.let {
                    Text(text = it, color = FishPiErrorRed, modifier = Modifier.padding(top = 6.dp))
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(FishPiTheme.radiusBox))
                        .background(FishPiErrorRed.copy(alpha = 0.08f))
                        .clickable {
                            onSave(ChatFilterConfig(
                                blockedUsers = emptyList(),
                                blockedKeywords = emptyList(),
                                blockedPrefixKeywords = emptyList(),
                                blockedRegex = emptyList(),
                                showAvatars = config.showAvatars,
                                sendOnEnter = config.sendOnEnter,
                            ))
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = FishPiErrorRed,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "清空全部过滤规则",
                        color = FishPiErrorRed,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(FishPiTheme.radiusBox))
                        .background(FishPiTheme.surfaceContainer)
                        .clickable {
                            onSave(ChatFilterConfig(
                                showAvatars = config.showAvatars,
                                sendOnEnter = config.sendOnEnter,
                            ))
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = FishPiTheme.weakText,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "恢复默认过滤规则",
                        color = FishPiTheme.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatSendOnEnterSettingRow(
    sendOnEnter: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FishPiTheme.radiusBox))
            .background(FishPiTheme.surfaceContainer)
            .clickable(onClick = onToggle)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "回车发送", color = FishPiTheme.accent, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (sendOnEnter) "键盘发送键或实体回车会直接发送" else "回车保留为换行，只点发送按钮发送",
                color = FishPiTheme.onSurface.copy(alpha = 0.58f),
            )
        }
        CapsuleSwitch(checked = sendOnEnter)
    }
}

@Composable
private fun ChatAvatarSettingRow(
    showAvatars: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FishPiTheme.radiusBox))
            .background(FishPiTheme.surfaceContainer)
            .clickable(onClick = onToggle)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "显示聊天室头像", color = FishPiTheme.accent, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (showAvatars) "消息左侧显示发送者头像" else "隐藏头像，保留紧凑消息布局",
                color = FishPiTheme.onSurface.copy(alpha = 0.58f),
            )
        }
        CapsuleSwitch(checked = showAvatars)
    }
}

@Composable
private fun CapsuleSwitch(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 58.dp, height = 32.dp)
            .clip(RoundedCornerShape(FishPiTheme.radiusSelector))
            .background(if (checked) FishPiTheme.accent else FishPiTheme.surface),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(24.dp)
                .clip(RoundedCornerShape(FishPiTheme.radiusSelector))
                .background(FishPiTheme.background),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (checked) "开" else "关",
                color = if (checked) FishPiTheme.accent else FishPiTheme.onSurface.copy(alpha = 0.58f),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun FilterRuleEditor(
    title: String,
    hint: String,
    value: String,
    rules: List<String>,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FishPiTheme.radiusBox))
            .background(FishPiTheme.surfaceContainer)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = title, color = FishPiTheme.accent, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                label = hint,
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(FishPiTheme.radiusBox))
                    .background(if (value.isNotBlank()) FishPiTheme.accent.copy(alpha = 0.12f) else FishPiTheme.surface)
                    .clickable(enabled = value.isNotBlank()) { onAdd() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "添加",
                    tint = if (value.isNotBlank()) FishPiTheme.accent else FishPiTheme.weakText,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        if (rules.isEmpty()) {
            Text(text = "暂无规则", color = FishPiTheme.weakText, fontSize = 13.sp)
        } else {
            rules.forEach { rule ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(FishPiTheme.radiusField))
                        .background(FishPiTheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = rule, modifier = Modifier.weight(1f), color = FishPiTheme.onSurface)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { onRemove(rule) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "删除",
                            tint = FishPiErrorRed,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockedMessagesOverlay(
    messages: List<ChatRoomMessage>,
    selfUsername: String,
    onImageClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onAvatarClick: (String) -> Unit,
    onAvatarLongPress: (String) -> Unit,
    onRedPacketClick: (ChatRoomMessage) -> Unit,
    onReactionClick: (ChatRoomMessage, String) -> Unit,
    onLongPress: (MessageActionAnchor) -> Unit,
    onDismiss: () -> Unit,
) {
    val displayMessages = remember(messages) { messages.asReversed() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FishPiTheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FishPiTheme.surface)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "已屏蔽消息",
                    color = FishPiTheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Text(
                    text = if (messages.isEmpty()) "暂无被屏蔽消息" else "${messages.size} 条消息被当前规则隐藏",
                    color = FishPiTheme.weakText,
                    fontSize = 12.sp,
                )
            }
            FishPiIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = "关闭已屏蔽消息",
                onClick = onDismiss,
            )
        }

        if (displayMessages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "暂无被屏蔽消息", color = FishPiTheme.weakText)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(
                    items = displayMessages,
                    key = { _, message -> message.oId.ifBlank { "${message.userName}-${message.time}-${message.content.hashCode()}" } },
                ) { index, message ->
                    BlockedMessageRow(
                        message = message,
                        previous = displayMessages.getOrNull(index - 1),
                        isMine = message.userName.equals(selfUsername, ignoreCase = true),
                        onImageClick = onImageClick,
                        onLinkClick = onLinkClick,
                        onAvatarClick = onAvatarClick,
                        onAvatarLongPress = onAvatarLongPress,
                        onRedPacketClick = onRedPacketClick,
                        onReactionClick = onReactionClick,
                        onLongPress = onLongPress,
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockedMessageRow(
    message: ChatRoomMessage,
    previous: ChatRoomMessage?,
    isMine: Boolean,
    onImageClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onAvatarClick: (String) -> Unit,
    onAvatarLongPress: (String) -> Unit,
    onRedPacketClick: (ChatRoomMessage) -> Unit,
    onReactionClick: (ChatRoomMessage, String) -> Unit,
    onLongPress: (MessageActionAnchor) -> Unit,
) {
    val hints = remember(message) { message.toRenderHints() }
    val imageUrls = remember(message) { message.allRenderableImageUrls().take(4) }
    var bubbleRect by remember { mutableStateOf(Rect()) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        messageTimeSeparator(previous, message)?.let { separator ->
            Text(
                text = separator,
                color = FishPiTheme.weakText,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top,
        ) {
            if (!isMine) {
                BlockedMessageAvatar(message, onAvatarClick, onAvatarLongPress)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(
                modifier = Modifier.widthIn(max = 300.dp),
                horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isMine && hints.clientLabel.isNotBlank()) {
                        BlockedMessageClientBadge(hints.clientLabel)
                    }
                    Text(
                        text = message.authorLabel,
                        color = FishPiTheme.weakText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (!isMine && hints.clientLabel.isNotBlank()) {
                        BlockedMessageClientBadge(hints.clientLabel)
                    }
                }
                Surface(
                    color = if (isMine) FishPiTheme.accent.copy(alpha = 0.16f) else FishPiTheme.surface,
                    shape = RoundedCornerShape(FishPiTheme.radiusBox),
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            val offset = coordinates.positionInWindow()
                            bubbleRect = Rect(
                                offset.x.roundToInt(),
                                offset.y.roundToInt(),
                                (offset.x + coordinates.size.width).roundToInt(),
                                (offset.y + coordinates.size.height).roundToInt(),
                            )
                        }
                        .pointerInput(message.oId, isMine) {
                            detectTapGestures(
                                onLongPress = {
                                    onLongPress(MessageActionAnchor(message, bubbleRect, isMine))
                                },
                            )
                        },
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        message.redPacket?.let { packet ->
                            DefaultRedPacketCard(preview = packet, onClick = { onRedPacketClick(message) })
                        } ?: run {
                            val text = hints.plainFallback.ifBlank { hints.markdownContent }
                            if (text.isNotBlank()) {
                                Text(
                                    text = text,
                                    color = FishPiTheme.onSurface,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                        imageUrls.forEach { url ->
                            SubcomposeAsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(FishPiTheme.radiusField))
                                    .clickable { onImageClick(url) },
                            )
                        }
                        hints.previewLinks.take(3).forEach { url ->
                            Text(
                                text = url,
                                color = FishPiTheme.accent,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable { onLinkClick(url) },
                            )
                        }
                    }
                }
                if (message.reactionSummary.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(message.reactionSummary) { _, reaction ->
                            Surface(
                                color = if (reaction.selected) FishPiTheme.accent.copy(alpha = 0.14f) else FishPiTheme.surface,
                                shape = RoundedCornerShape(FishPiTheme.radiusSelector),
                                modifier = Modifier.clickable { onReactionClick(message, reaction.value) },
                            ) {
                                Text(
                                    text = "${reaction.emoji} ${reaction.count}",
                                    color = FishPiTheme.onSurface,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
            if (isMine) {
                Spacer(modifier = Modifier.width(8.dp))
                BlockedMessageAvatar(message, onAvatarClick, onAvatarLongPress)
            }
        }
    }
}

@Composable
private fun BlockedMessageAvatar(
    message: ChatRoomMessage,
    onAvatarClick: (String) -> Unit,
    onAvatarLongPress: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(FishPiTheme.accent.copy(alpha = 0.18f))
            .pointerInput(message.userName) {
                detectTapGestures(
                    onTap = { onAvatarClick(message.userName) },
                    onLongPress = { onAvatarLongPress(message.userName) },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (message.userAvatarURL.isNotBlank()) {
            SubcomposeAsyncImage(
                model = message.userAvatarURL,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = message.displayName.trim().take(1).ifBlank { "鱼" },
                color = FishPiTheme.accent,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun BlockedMessageClientBadge(client: String) {
    Text(
        text = client,
        color = FishPiTheme.weakText,
        fontSize = 9.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(FishPiTheme.background)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

@Composable
internal fun ChatUserProfileOverlay(
    username: String,
    user: FishPiUser?,
    medals: List<MedalView>,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FishPiTheme.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "用户资料",
                    color = FishPiTheme.accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                FishPiIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "关闭用户资料",
                    onClick = onDismiss,
                )
            }

            when {
                isLoading -> LoadingScreen("加载 @$username 的资料...")
                error != null -> ErrorState(message = error, onRetry = onRetry)
                user == null -> ErrorState(message = "没有读取到 @$username 的资料", onRetry = onRetry)
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        FishPiUserSummaryCard(
                            user = user,
                            medals = medals,
                            apiKey = LocalAppSession.current.apiKey,
                        )
                    }
                }
            }
        }
    }
}

private fun applyAtCandidate(input: String, anchor: Int, username: String): String {
    val safeAnchor = anchor.coerceIn(0, input.length)
    val before = input.substring(0, safeAnchor)
    val afterQuery = input
        .substring(safeAnchor)
        .indexOfFirst { it.isWhitespace() }
        .takeIf { it >= 0 }
        ?.let { offset -> input.substring(safeAnchor + offset) }
        .orEmpty()
    return "$before@$username $afterQuery".trimEnd().let { "$it " }
}

private fun insertMentionDraft(current: String, cursor: Int, username: String): Pair<String, Int> {
    val user = username.trim()
    if (user.isBlank()) {
        return current to cursor.coerceIn(0, current.length)
    }
    val insertAt = cursor.coerceIn(0, current.length)
    val before = current.substring(0, insertAt)
    val after = current.substring(insertAt)
    val mention = "@$user "
    val prefix = if (before.isNotEmpty() && !before.last().isWhitespace()) " " else ""
    val suffix = if (after.isNotEmpty() && !after.first().isWhitespace()) " " else ""
    val inserted = prefix + mention + suffix
    val next = before + inserted + after
    return next to (insertAt + prefix.length + mention.length)
}

@Composable
private fun ChatRoomContextBar(
    nodeName: String,
    topic: String,
    onlineCount: Int,
    visibleCount: Int,
    connectionLabel: String,
    themeLabel: String,
    noticeUnread: Long,
    onlineUsers: List<ChatOnlineUser>,
    onQuoteTopic: () -> Unit,
    onRefresh: () -> Unit,
    onReconnect: () -> Unit,
    onOpenBlocked: () -> Unit,
    onCycleTheme: () -> Unit,
    onOpenNotice: () -> Unit,
    onOpenOnlineUsers: () -> Unit,
    onOpenChatActions: () -> Unit,
    onOpenPlugins: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val connected = chatConnectionConnected(connectionLabel)
    val nodeTitle = nodeName.trim().takeIf { it.isNotBlank() && it != "暂无" } ?: "聊天室"
    val topicLabel = topic.trim().takeIf { it.isNotBlank() && it != "暂无" }?.let { "#$it#" } ?: "引用话题"
        IslandChatContextBar(
        nodeTitle = nodeTitle,
        topicLabel = topicLabel,
        onlineCount = onlineCount,
        visibleCount = visibleCount,
        connectionLabel = connectionLabel,
        connected = connected,
        themeLabel = themeLabel,
        noticeUnread = noticeUnread,
        onlineUsers = onlineUsers,
        onQuoteTopic = onQuoteTopic,
        onRefresh = onRefresh,
        onReconnect = onReconnect,
        onOpenBlocked = onOpenBlocked,
        onCycleTheme = onCycleTheme,
        onOpenNotice = onOpenNotice,
        onOpenOnlineUsers = onOpenOnlineUsers,
        onOpenChatActions = onOpenChatActions,
        onOpenPlugins = onOpenPlugins,
        onBack = onBack,
        modifier = modifier,
    )
    return
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(FishPiTheme.radiusBox),
        color = FishPiTheme.surface.copy(alpha = 0.82f),
        border = BorderStroke(FishPiTheme.borderWidth, FishPiTheme.outline.copy(alpha = 0.12f)),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = "聊天室",
                        color = FishPiTheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        ChatConnectionChip(
                            label = if (connectionLabel.isBlank()) "连接中" else connectionLabel,
                            connected = connected,
                            onClick = onReconnect,
                        )
                        Text(
                            text = "$onlineCount 在线 · $visibleCount 条消息",
                            color = FishPiTheme.weakText,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ChatTopicChip(
                    text = topicLabel,
                    onClick = onQuoteTopic,
                    modifier = Modifier.weight(1f),
                )
                ChatHeaderIconAction(
                    icon = Icons.Rounded.Refresh,
                    contentDescription = "刷新聊天室",
                    onClick = onRefresh,
                )
                ChatHeaderIconAction(
                    icon = Icons.Rounded.Palette,
                    contentDescription = if (themeLabel.isBlank()) "切换主题" else "主题：$themeLabel",
                    onClick = onCycleTheme,
                )
                ChatHeaderIconAction(
                    icon = Icons.Rounded.VisibilityOff,
                    contentDescription = "查看已屏蔽消息",
                    onClick = onOpenBlocked,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IslandChatContextBar(
    nodeTitle: String,
    topicLabel: String,
    onlineCount: Int,
    visibleCount: Int,
    connectionLabel: String,
    connected: Boolean,
    themeLabel: String,
    noticeUnread: Long,
    onlineUsers: List<ChatOnlineUser>,
    onQuoteTopic: () -> Unit,
    onRefresh: () -> Unit,
    onReconnect: () -> Unit,
    onOpenBlocked: () -> Unit,
    onCycleTheme: () -> Unit,
    onOpenNotice: () -> Unit,
    onOpenOnlineUsers: () -> Unit,
    onOpenChatActions: () -> Unit,
    onOpenPlugins: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalFishPiPalette.current
    val headerText = palette.onSurface
    val density = LocalDensity.current
    var titleWidthPx by remember(nodeTitle) { mutableStateOf(0) }
    val statusDotOffset = with(density) { (titleWidthPx / 2).toDp() + 4.dp }
    Column(
        modifier = modifier.background(palette.surface),
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 4.dp, top = 12.dp, end = 4.dp),
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(44.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlainBackButton(
                    onClick = onBack,
                    contentDescription = "返回聊天",
                    tint = headerText.copy(alpha = 0.86f),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 58.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = nodeTitle,
                    color = headerText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.onGloballyPositioned { titleWidthPx = it.size.width },
                )
                ChatConnectionDot(
                    label = if (connectionLabel.isBlank()) "连接中" else connectionLabel,
                    connected = connected,
                    onClick = onReconnect,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = statusDotOffset),
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                ChatActionOverflowMenu(noticeUnread = noticeUnread, onClick = onOpenChatActions)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatOnlineUsersPage(
    onlineCount: Int,
    users: List<ChatOnlineUser>,
    onDismiss: () -> Unit,
    onOpenUser: (String) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FishPiTheme.background)
                .navigationBarsPadding(),
        ) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FishPiTheme.surface,
                    titleContentColor = FishPiTheme.onSurface,
                    actionIconContentColor = FishPiTheme.onSurface,
                ),
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "在线用户",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text(
                            text = "$onlineCount 人在线",
                            color = FishPiTheme.weakText,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭")
                    }
                },
            )
            if (users.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "正在等待在线列表同步",
                        color = FishPiTheme.weakText,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    itemsIndexed(users) { _, user ->
                        ChatOnlineUserRow(
                            user = user,
                            onClick = { onOpenUser(user.userName) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatOnlineUserRow(
    user: ChatOnlineUser,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FishPiTheme.radiusBox))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ChatOnlineAvatar(user = user)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = user.userName,
                color = FishPiTheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "在线",
                color = FishPiTheme.weakText,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ChatOnlineAvatar(user: ChatOnlineUser) {
    val context = LocalContext.current
    val model = remember(user.avatarUrl) {
        ImageRequest.Builder(context)
            .data(user.avatarUrl)
            .size(Size(96, 96))
            .build()
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(FishPiTheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (user.avatarUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = user.userName.take(1).uppercase(),
                color = FishPiTheme.weakText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ChatActionOverflowMenu(
    noticeUnread: Long,
    onClick: () -> Unit,
) {
    val palette = LocalFishPiPalette.current
    val description = if (noticeUnread > 0) "更多聊天室操作，$noticeUnread 条通知未读" else "更多聊天室操作"
    Box(
        modifier = Modifier
            .size(40.dp)
            .semantics { contentDescription = description }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(width = 21.dp, height = 10.dp)
                .offset(y = 0.5.dp),
        ) {
            val radius = 1.18.dp.toPx()
            val centerY = size.height / 2f
            val firstX = size.width * 0.18f
            val gap = size.width * 0.32f
            repeat(3) { index ->
                drawCircle(
                    color = palette.onSurface.copy(alpha = 0.78f),
                    radius = radius,
                    center = Offset(firstX + gap * index, centerY),
                )
            }
        }
        if (noticeUnread > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 1.dp, end = 0.dp)
                    .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                    .clip(RoundedCornerShape(FishPiTheme.radiusSelector))
                    .background(palette.accent)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (noticeUnread > 99) "99+" else noticeUnread.toString(),
                    color = palette.background,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ChatTopicEditorDialog(
    value: String,
    currentTopic: String,
    onChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val trimmed = value.trim()
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            shape = RoundedCornerShape(18.dp),
            color = FishPiTheme.surface,
            border = BorderStroke(FishPiTheme.borderWidth, FishPiTheme.outline.copy(alpha = 0.16f)),
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "更改话题",
                            color = FishPiTheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "修改话题需要16积分，将自动从账户中扣除；\n最大长度64字符，不合法字符将被自动过滤",
                            color = FishPiTheme.weakText,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    FishPiIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = "关闭话题输入",
                        onClick = onDismiss,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(FishPiTheme.radiusBox + 4.dp),
                    color = FishPiTheme.surfaceContainer,
                    border = BorderStroke(FishPiTheme.borderWidth, FishPiTheme.outline.copy(alpha = 0.14f)),
                    shadowElevation = 0.dp,
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = onChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        textStyle = TextStyle(
                            color = FishPiTheme.onSurface,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                        ),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (trimmed.isNotBlank()) onConfirm()
                        }),
                        decorationBox = { inner ->
                            if (value.isBlank()) {
                                Text(
                                    text = "输入新的聊天室话题",
                                    color = FishPiTheme.weakText,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            inner()
                        },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${value.length}/64",
                        color = FishPiTheme.weakText,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f),
                    )
                    FishPiPillButton(
                        text = "保存话题",
                        enabled = trimmed.isNotBlank(),
                        onClick = onConfirm,
                        containerColor = FishPiTheme.accent,
                        contentColor = Color.White,
                    )
                }
            }
        }
    }
}

private data class ChatPluginToolbarGroup(
    val pluginId: String,
    val title: String,
    val entries: List<PluginToolbarEntry>,
)

@Composable
private fun ChatPluginFloatingPanel(
    groups: List<ChatPluginToolbarGroup>,
    expandedPluginId: String?,
    expanded: Boolean,
    offset: IntOffset,
    onOffsetChange: (IntOffset) -> Unit,
    onDismiss: () -> Unit,
    onSelectPlugin: (ChatPluginToolbarGroup) -> Unit,
    onAction: (PluginToolbarEntry, PluginToolbarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalFishPiPalette.current
    val density = LocalDensity.current
    val selectedGroup = groups.firstOrNull { it.pluginId == expandedPluginId }
    var parentSize by remember { mutableStateOf(IntSize.Zero) }
    var panelSize by remember { mutableStateOf(IntSize.Zero) }
    val defaultMargin = with(density) { 14.dp.roundToPx() }
    val bottomLift = with(density) { 94.dp.roundToPx() }
    val bookmarkInset = with(density) { 8.dp.roundToPx() }
    val snapThreshold = with(density) { 22.dp.roundToPx() }
    if (!expanded) return
    LaunchedEffect(offset, parentSize, panelSize, defaultMargin, bottomLift, bookmarkInset) {
        if (
            offset.isUnsetPluginOffset() &&
            parentSize.width > 0 &&
            parentSize.height > 0 &&
            panelSize.width > 0 &&
            panelSize.height > 0
        ) {
            onOffsetChange(
                coercePluginFloatingOffset(
                    offset = IntOffset(
                        x = parentSize.width - panelSize.width - defaultMargin,
                        y = parentSize.height - panelSize.height - bottomLift,
                    ),
                    parentSize = parentSize,
                    panelSize = panelSize,
                    edgeInset = bookmarkInset,
                )
            )
        }
    }
    val clampedOffset = remember(offset, parentSize, panelSize, bookmarkInset) {
        coercePluginFloatingOffset(
            offset = if (offset.isUnsetPluginOffset()) IntOffset.Zero else offset,
            parentSize = parentSize,
            panelSize = panelSize,
            edgeInset = bookmarkInset,
        )
    }
    val latestClampedOffset by rememberUpdatedState(clampedOffset)
    val expandUp = parentSize.height > 0 && clampedOffset.y + (panelSize.height / 2) > parentSize.height / 2
    val actions = remember(selectedGroup) {
        selectedGroup
            ?.entries
            ?.flatMap { entry -> entry.actions.map { entry to it } }
            .orEmpty()
    }
    val stackActions = selectedGroup != null && (
        actions.size > 2 ||
            clampedOffset.x < with(density) { 56.dp.roundToPx() } ||
            (parentSize.width > 0 && parentSize.width - (clampedOffset.x + panelSize.width) < with(density) { 56.dp.roundToPx() })
        )
    Box(
        modifier = modifier
            .onSizeChanged { parentSize = it },
    ) {
        Surface(
            modifier = Modifier
                .offset { clampedOffset }
                .onSizeChanged { panelSize = it }
                .widthIn(min = 116.dp, max = 260.dp)
                .pointerInput(parentSize, panelSize) {
                    var dragOffset = IntOffset.Zero
                    detectDragGestures(
                        onDragStart = {
                            dragOffset = latestClampedOffset
                        },
                        onDragEnd = {
                            onOffsetChange(
                                snapPluginFloatingOffset(
                                    offset = dragOffset,
                                    parentSize = parentSize,
                                    panelSize = panelSize,
                                    edgeInset = bookmarkInset,
                                    threshold = snapThreshold,
                                )
                            )
                        },
                        onDragCancel = {
                            onOffsetChange(
                                coercePluginFloatingOffset(
                                    offset = dragOffset,
                                    parentSize = parentSize,
                                    panelSize = panelSize,
                                    edgeInset = bookmarkInset,
                                )
                            )
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        dragOffset = IntOffset(
                            x = dragOffset.x + dragAmount.x.roundToInt(),
                            y = dragOffset.y + dragAmount.y.roundToInt(),
                        )
                        onOffsetChange(
                            coercePluginFloatingOffset(
                                offset = dragOffset,
                                parentSize = parentSize,
                                panelSize = panelSize,
                                edgeInset = bookmarkInset,
                            )
                        )
                    }
                },
            shape = RoundedCornerShape(FishPiTheme.radiusBox + 4.dp),
            color = palette.surface.copy(alpha = 0.96f),
            border = BorderStroke(FishPiTheme.borderWidth, palette.outline.copy(alpha = 0.14f)),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
        ) {
            PluginFloatingContent(
                groups = groups,
                selectedGroup = selectedGroup,
                selectedActions = actions,
                expandUp = expandUp,
                stackActions = stackActions,
                onDismiss = onDismiss,
                onSelectPlugin = onSelectPlugin,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun PluginFloatingContent(
    groups: List<ChatPluginToolbarGroup>,
    selectedGroup: ChatPluginToolbarGroup?,
    selectedActions: List<Pair<PluginToolbarEntry, PluginToolbarAction>>,
    expandUp: Boolean,
    stackActions: Boolean,
    onDismiss: () -> Unit,
    onSelectPlugin: (ChatPluginToolbarGroup) -> Unit,
    onAction: (PluginToolbarEntry, PluginToolbarAction) -> Unit,
) {
    val palette = LocalFishPiPalette.current

    @Composable
    fun SelectedPluginRow() {
        if (selectedGroup == null) return
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            PluginFloatingChip(
                text = selectedGroup.title,
                selected = true,
                onClick = { onSelectPlugin(selectedGroup) },
                modifier = Modifier.weight(1f, fill = false),
            )
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "收起插件动作",
                    tint = palette.weakText,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }

    @Composable
    fun ChipFlow() {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (selectedGroup == null) {
                groups.forEach { group ->
                    val enabled = group.entries.any { entry -> entry.actions.any { it.enabled } }
                    PluginFloatingChip(
                        text = group.title,
                        enabled = enabled,
                        selected = false,
                        onClick = { onSelectPlugin(group) },
                    )
                }
            } else {
                selectedActions.forEach { (entry, action) ->
                    PluginFloatingChip(
                        text = action.label,
                        enabled = action.enabled,
                        selected = false,
                        onClick = { onAction(entry, action) },
                    )
                }
            }
        }
    }

    @Composable
    fun ActionStack() {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            selectedActions.forEach { (entry, action) ->
                PluginFloatingChip(
                    text = action.label,
                    enabled = action.enabled,
                    selected = false,
                    onClick = { onAction(entry, action) },
                    modifier = Modifier.widthIn(max = 172.dp),
                )
            }
        }
    }

    Column(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (selectedGroup != null && expandUp) {
            if (stackActions) ActionStack() else ChipFlow()
            SelectedPluginRow()
        } else {
            SelectedPluginRow()
            if (stackActions) ActionStack() else ChipFlow()
        }
    }
}

@Composable
private fun PluginFloatingChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
) {
    val palette = LocalFishPiPalette.current
    val shape = RoundedCornerShape(FishPiTheme.radiusSelector)
    Row(
        modifier = modifier
            .clip(shape)
            .background(
                when {
                    selected -> palette.accent.copy(alpha = 0.12f)
                    else -> palette.surfaceContainer.copy(alpha = 0.72f)
                }
            )
            .border(
                FishPiTheme.borderWidth,
                when {
                    selected -> palette.accent.copy(alpha = 0.26f)
                    else -> palette.outline.copy(alpha = 0.13f)
                },
                shape,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(
                    when {
                        !enabled -> palette.weakText.copy(alpha = 0.30f)
                        selected -> palette.accent
                        else -> palette.accent.copy(alpha = 0.74f)
                    }
                ),
        )
        Text(
            text = text,
            color = when {
                !enabled -> palette.weakText.copy(alpha = 0.50f)
                selected -> palette.accent
                else -> palette.onSurface
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun IntOffset.isUnsetPluginOffset(): Boolean =
    x == ChatPluginFloatUnset || y == ChatPluginFloatUnset

private fun SharedPreferences.readPluginFloatOffset(): IntOffset {
    val hasX = contains(ChatPluginFloatXKey)
    val hasY = contains(ChatPluginFloatYKey)
    if (!hasX || !hasY) {
        return IntOffset(ChatPluginFloatUnset, ChatPluginFloatUnset)
    }
    val x = getInt(ChatPluginFloatXKey, ChatPluginFloatUnset)
    val y = getInt(ChatPluginFloatYKey, ChatPluginFloatUnset)
    if (x == 0 && y == 0) {
        edit()
            .remove(ChatPluginFloatXKey)
            .remove(ChatPluginFloatYKey)
            .apply()
        return IntOffset(ChatPluginFloatUnset, ChatPluginFloatUnset)
    }
    return IntOffset(x, y)
}

private fun coercePluginFloatingOffset(
    offset: IntOffset,
    parentSize: IntSize,
    panelSize: IntSize,
    edgeInset: Int,
): IntOffset {
    if (offset.isUnsetPluginOffset()) {
        return offset
    }
    if (parentSize.width <= 0 || parentSize.height <= 0 || panelSize.width <= 0 || panelSize.height <= 0) {
        return offset
    }
    val minX = -edgeInset
    val maxX = (parentSize.width - panelSize.width + edgeInset).coerceAtLeast(minX)
    val minY = edgeInset
    val maxY = (parentSize.height - panelSize.height - edgeInset).coerceAtLeast(minY)
    return IntOffset(
        x = offset.x.coerceIn(minX, maxX),
        y = offset.y.coerceIn(minY, maxY),
    )
}

private fun snapPluginFloatingOffset(
    offset: IntOffset,
    parentSize: IntSize,
    panelSize: IntSize,
    edgeInset: Int,
    threshold: Int,
): IntOffset {
    val clamped = coercePluginFloatingOffset(offset, parentSize, panelSize, edgeInset)
    if (parentSize.width <= 0 || panelSize.width <= 0) {
        return clamped
    }
    val rightBookmarkX = parentSize.width - panelSize.width + edgeInset
    val snappedX = when {
        clamped.x <= threshold -> -edgeInset
        parentSize.width - (clamped.x + panelSize.width) <= threshold -> rightBookmarkX
        else -> clamped.x
    }
    return clamped.copy(x = snappedX)
}

@Composable
private fun ChatActionsSheet(
    onlineCount: Int,
    themeLabel: String,
    noticeUnread: Long,
    pluginFloatAvailable: Boolean,
    pluginFloatVisible: Boolean,
    onDismiss: () -> Unit,
    onOpenNotice: () -> Unit,
    onOpenOnlineUsers: () -> Unit,
    onRefresh: () -> Unit,
    onReconnect: () -> Unit,
    onCycleTheme: () -> Unit,
    onOpenBlocked: () -> Unit,
    onTogglePluginFloat: () -> Unit,
) {
    val palette = LocalFishPiPalette.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.onSurface.copy(alpha = 0.045f))
                .silentTap(onDismiss),
            contentAlignment = Alignment.TopEnd,
        ) {
            Surface(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 60.dp, end = 10.dp, start = 18.dp)
                    .widthIn(max = 286.dp)
                    .consumeTaps(),
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 8.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                color = palette.surface,
                shadowElevation = 0.dp,
                border = BorderStroke(FishPiTheme.borderWidth, palette.outline.copy(alpha = 0.18f)),
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            Text(
                                text = "聊天室操作",
                                color = palette.onSurface,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                            )
                            Text(
                                text = "通知、在线和显示控制",
                                color = palette.weakText,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        ChatActionSheetItem(
                            title = "通知",
                            detail = if (noticeUnread > 0) "未读 $noticeUnread" else "消息通知",
                            icon = Icons.Rounded.Notifications,
                            badgeText = noticeUnread.takeIf { it > 0 }?.let { if (it > 99) "99+" else it.toString() },
                            onClick = onOpenNotice,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        ChatActionSheetItem(
                            title = "在线用户",
                            detail = "$onlineCount 人",
                            icon = Icons.Rounded.AlternateEmail,
                            onClick = onOpenOnlineUsers,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (pluginFloatAvailable) {
                            ChatActionSheetItem(
                                title = if (pluginFloatVisible) "隐藏插件浮窗" else "显示插件浮窗",
                                detail = if (pluginFloatVisible) "插件按钮将从聊天区移除" else "恢复插件快捷按钮",
                                icon = if (pluginFloatVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Extension,
                                onClick = onTogglePluginFloat,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ChatActionSheetMiniItem(
                            title = "重连",
                            icon = Icons.Rounded.Refresh,
                            onClick = onReconnect,
                            modifier = Modifier.weight(1f),
                        )
                        ChatActionSheetMiniItem(
                            title = "刷新",
                            icon = Icons.Rounded.Refresh,
                            onClick = onRefresh,
                            modifier = Modifier.weight(1f),
                        )
                        ChatActionSheetMiniItem(
                            title = "主题",
                            icon = Icons.Rounded.Palette,
                            onClick = onCycleTheme,
                            modifier = Modifier.weight(1f),
                        )
                        ChatActionSheetMiniItem(
                            title = "屏蔽",
                            icon = Icons.Rounded.VisibilityOff,
                            onClick = onOpenBlocked,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatActionSheetMiniItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalFishPiPalette.current
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(13.dp),
        color = palette.background.copy(alpha = 0.58f),
        contentColor = palette.onSurface,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(FishPiTheme.borderWidth, palette.outline.copy(alpha = 0.10f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.accent.copy(alpha = 0.82f),
                modifier = Modifier.size(17.dp),
            )
            Text(
                text = title,
                color = palette.onSurface,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ChatActionSheetItem(
    title: String,
    detail: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
) {
    val palette = LocalFishPiPalette.current
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(FishPiTheme.radiusBox),
        color = palette.background.copy(alpha = 0.72f),
        contentColor = palette.onSurface,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(FishPiTheme.borderWidth, palette.outline.copy(alpha = 0.13f)),
    ) {
        Row(
        modifier = Modifier
            .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 10.dp, bottomEnd = 4.dp))
                    .background(palette.accent.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.accent.copy(alpha = 0.86f),
                    modifier = Modifier.size(16.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = title,
                    color = palette.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = detail,
                    color = palette.weakText,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            badgeText?.let { badge ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(FishPiTheme.radiusSelector))
                        .background(palette.accent)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = badge,
                        color = palette.background,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatHeaderIconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconActionButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        size = 34.dp,
        iconSize = 18.dp,
    )
}

@Composable
private fun ChatTopicChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = FishPiTheme.onSurface.copy(alpha = 0.82f),
    weakColor: Color = FishPiTheme.weakText,
    dotColor: Color = FishPiTheme.accent.copy(alpha = 0.72f),
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "引用",
            color = weakColor,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

private fun chatConnectionConnected(status: String): Boolean {
    val text = status.trim()
    return text == "已连接" ||
        text.contains("已连接") ||
        text.contains("已恢复") ||
        text.contains("连接已恢复") ||
        text.contains("connected", ignoreCase = true) ||
        text.contains("reconnected", ignoreCase = true)
}

@Composable
private fun chatConnectionDotColor(status: String, connected: Boolean): Color {
    val text = status.trim()
    if (connected) return FishPiTheme.success
    val reconnecting = text.isBlank() ||
        text.contains("连接中") ||
        text.contains("重连") ||
        text.contains("恢复中") ||
        text.contains("connecting", ignoreCase = true) ||
        text.contains("reconnect", ignoreCase = true)
    return if (reconnecting) FishPiTheme.warning else FishPiTheme.error
}

@Composable
private fun ChatConnectionDot(
    label: String,
    connected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .silentTap(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(chatConnectionDotColor(label, connected)),
        )
    }
}

@Composable
private fun ChatConnectionChip(
    label: String,
    connected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (connected) FishPiTheme.success else FishPiTheme.error
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(FishPiTheme.radiusSelector))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = if (connected) "已连接" else label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun ChatOverlayHost(content: @Composable () -> Unit) {
    content()
}

@Composable
private fun ChatFloatingActions(
    unreadNewMessages: Int,
    redPacketJumpTargetId: String?,
    onJumpToLatest: () -> Unit,
    onJumpToRedPacket: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        redPacketJumpTargetId?.let { targetId ->
            ChatOverlayPill(
                text = "跳转到红包",
                onClick = { onJumpToRedPacket(targetId) },
            )
        }

        if (unreadNewMessages > 0) {
            ChatUnreadCountButton(
                count = unreadNewMessages,
                onClick = onJumpToLatest,
            )
        }
    }
}

@Composable
private fun ChatUnreadCountButton(
    count: Int,
    onClick: () -> Unit,
) {
    val palette = LocalFishPiPalette.current
    val label = if (count > 99) "99+" else count.toString()
    Surface(
        onClick = onClick,
        shape = ChatUnreadDropShape,
        color = palette.surface.copy(alpha = 0.96f),
        contentColor = palette.accent,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(FishPiTheme.borderWidth, palette.accent.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minWidth = if (count > 99) 50.dp else 42.dp, minHeight = 32.dp)
                .padding(start = 9.dp, top = 5.dp, end = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = palette.accent.copy(alpha = 0.82f),
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = label,
                color = palette.accent,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

private val ChatUnreadDropShape = GenericShape { size, _ ->
    val width = size.width
    val height = size.height
    val bodyBottom = height * 0.76f

    moveTo(width * 0.50f, height)
    quadraticTo(width * 0.58f, bodyBottom + height * 0.02f, width * 0.66f, bodyBottom)
    lineTo(width * 0.78f, bodyBottom)
    quadraticTo(width, bodyBottom, width, bodyBottom * 0.52f)
    quadraticTo(width, 0f, width * 0.72f, 0f)
    lineTo(width * 0.28f, 0f)
    quadraticTo(0f, 0f, 0f, bodyBottom * 0.52f)
    quadraticTo(0f, bodyBottom, width * 0.22f, bodyBottom)
    lineTo(width * 0.34f, bodyBottom)
    quadraticTo(width * 0.42f, bodyBottom + height * 0.02f, width * 0.50f, height)
    close()
}

@Composable
private fun ChatOverlayPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FloatingNoticePill(text = text, onClick = onClick, modifier = modifier, enabled = enabled)
}

@Composable
private fun TimeSeparatorBar(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(FishPiTheme.radiusField))
                .background(FishPiTheme.surfaceContainer)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = text, color = FishPiTheme.accent)
        }
    }
}

@Composable
private fun HistoryLoadState(
    isLoadingMore: Boolean,
    hasMoreHistory: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        val text = when {
            isLoadingMore -> "正在加载更早消息..."
            !hasMoreHistory -> "已经到最早消息了"
            else -> "上滑加载更早消息"
        }
        Text(text = text, color = FishPiTheme.accent)
    }
}




