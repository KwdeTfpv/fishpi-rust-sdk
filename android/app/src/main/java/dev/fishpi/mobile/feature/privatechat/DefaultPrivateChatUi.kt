package dev.fishpi.mobile.feature.privatechat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import dev.fishpi.mobile.ui.motion.FishPiMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.SubcomposeAsyncImage
import dev.fishpi.mobile.AppSession
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.ui.components.ChatInputBar
import dev.fishpi.mobile.ui.components.rememberUiPageBrush
import dev.fishpi.mobile.ui.bottomBarContentPadding
import dev.fishpi.mobile.feature.chat.render.renderHintCacheKey
import dev.fishpi.mobile.shared.message.ChatListItem
import dev.fishpi.mobile.shared.message.ChatMessageRenderHints
import dev.fishpi.mobile.shared.message.ChatQuote
import dev.fishpi.mobile.ui.components.PlainBackButton
import dev.fishpi.mobile.ui.overlay.LocalImageViewer
import dev.fishpi.mobile.ui.overlay.LinkPreviewOverlay
import dev.fishpi.mobile.ui.components.LoadingScreen
import dev.fishpi.mobile.ui.components.FloatingNoticePill
import dev.fishpi.mobile.shared.message.ui.MessageActionBubbleMenu
import dev.fishpi.mobile.shared.message.canBeRevokedBy
import dev.fishpi.mobile.shared.message.copyToClipboard
import dev.fishpi.mobile.shared.message.copyableText
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.data.EmojiGroupView
import dev.fishpi.mobile.data.ComposerAttachment
import dev.fishpi.mobile.data.EmojiItemView
import dev.fishpi.mobile.feature.privatechat.mapper.isFileTransferPeer
import dev.fishpi.mobile.feature.privatechat.mapper.toPrivateChatSession
import dev.fishpi.mobile.feature.privatechat.model.PrivateSessionUiModel
import dev.fishpi.mobile.shared.message.native.MessageActionAnchor
import dev.fishpi.mobile.shared.message.native.NativeMessageList
import dev.fishpi.mobile.shared.message.native.rememberNativeMessageListController
import dev.fishpi.mobile.ui.components.ChatToolAction
import dev.fishpi.mobile.shared.message.messageTimeSeparator
import dev.fishpi.mobile.ui.media.rememberChatAttachmentPicker
import dev.fishpi.mobile.rememberFishPiImageLoader
import dev.fishpi.mobile.ui.components.silentTap
import dev.fishpi.mobile.ui.glass.FishPiGlassRole
import dev.fishpi.mobile.ui.glass.LocalFishPiGlassBackdrop
import dev.fishpi.mobile.ui.glass.fishPiGlassSurface
import dev.fishpi.mobile.shared.message.toRenderHints
import kotlinx.coroutines.delay

@Composable
internal fun DefaultPrivateChatUi(
    state: PrivateChatState,
    dispatch: (PrivateChatAction) -> Unit,
) {
    val environment = LocalPrivateChatUiEnvironment.current
    val conversation = state.conversation
    val imageViewer = LocalImageViewer.current
    val attachmentPicker = rememberChatAttachmentPicker(
        onPicked = { uri, isVideo -> dispatch(PrivateChatAction.PickAttachment(uri, isVideo)) },
        onError = { dispatch(PrivateChatAction.ShowError(it)) },
    )
    val privateToolActions = remember(attachmentPicker, dispatch) {
        listOf(
        ChatToolAction(
            id = "gallery",
            label = "相册",
            icon = Icons.Rounded.PhotoLibrary,
            enabled = true,
            onClick = {
                dispatch(PrivateChatAction.CloseTools)
                attachmentPicker.openGallery()
            },
        ),
        ChatToolAction(
            id = "camera",
            label = "拍照",
            icon = Icons.Rounded.AddAPhoto,
            enabled = true,
            onClick = {
                dispatch(PrivateChatAction.CloseTools)
                attachmentPicker.openCamera()
            },
        ),
        ChatToolAction(
            id = "file",
            label = "文件",
            icon = Icons.Rounded.AttachFile,
            enabled = true,
            onClick = {
                dispatch(PrivateChatAction.CloseTools)
                attachmentPicker.openFile()
            },
        ),
        )
    }

    AnimatedContent(
        targetState = conversation.selectedPeer,
        contentKey = { it != null },
        transitionSpec = {
            val enteringDetail = initialState == null && targetState != null
            FishPiMotion.pushTransform(enteringDetail).using(SizeTransform(clip = false))
        },
        label = "privateChatListDetail",
    ) { selectedPeer ->
    if (selectedPeer == null) {
        PrivateSessionList(
            sessions = state.sessions,
            isLoading = state.isLoadingSessions,
            error = state.sessionsError,
            onOpen = { dispatch(PrivateChatAction.OpenSession(it.toPrivateChatSession())) },
            listHeader = environment.listHeader,
        )
    } else {
        PrivateConversation(
            peer = selectedPeer,
            peerAvatar = conversation.selectedPeerAvatar,
            messages = conversation.messages,
            input = conversation.input,
            inputResetKey = conversation.inputResetKey,
            isLoading = conversation.isLoading,
            isLoadingMore = conversation.isLoadingMore,
            hasMore = conversation.hasMoreHistory,
            isSending = conversation.isSending,
            isUploadingAttachment = conversation.attachments.hasPendingUpload,
            attachments = conversation.attachments.items,
            cursorRequest = conversation.cursorRequest,
            status = conversation.status,
            error = conversation.error,
            selfUsername = environment.session.user.userName,
            scrollToBottomRequest = conversation.scrollToBottomRequest,
            keepPositionAfterPrependCount = conversation.keepPositionAfterPrependCount,
            onKeepPositionConsumed = { dispatch(PrivateChatAction.KeepPositionConsumed) },
            onBack = { dispatch(PrivateChatAction.CloseConversation) },
            onOpenUserProfile = environment.onOpenUserProfile,
            onLoadMore = { dispatch(PrivateChatAction.LoadMoreHistory) },
            onInputChange = { dispatch(PrivateChatAction.ChangeInput(it)) },
            onCursorChange = { dispatch(PrivateChatAction.ChangeCursor(it)) },
            onCursorRequestHandled = { dispatch(PrivateChatAction.ClearCursorRequest) },
            onRemoveAttachment = { dispatch(PrivateChatAction.RemoveAttachment(it.index)) },
            onRetryAttachment = { dispatch(PrivateChatAction.RetryAttachment(it.index)) },
            quote = conversation.quote,
            focusInputAfterQuote = conversation.focusInputAfterQuote,
            onInputFocused = { dispatch(PrivateChatAction.InputFocused) },
            onCancelQuote = { dispatch(PrivateChatAction.CancelQuote) },
            emojiPanelOpen = conversation.emojiPanelOpen,
            emojiGroups = conversation.emojiGroups,
            emojiItems = conversation.emojiItems,
            selectedEmojiGroupId = conversation.selectedEmojiGroupId,
            isLoadingEmojiPack = conversation.isLoadingEmojiPack,
            emojiPackError = conversation.emojiPackError,
            onToggleEmojiPanel = { dispatch(PrivateChatAction.ToggleEmoji) },
            onDismissEmojiPanel = { dispatch(PrivateChatAction.CloseEmoji) },
            onPickEmojiGroup = { dispatch(PrivateChatAction.SelectEmojiGroup(it)) },
            onPickEmoji = { dispatch(PrivateChatAction.PickEmoji(it)) },
            toolPanelOpen = conversation.attachmentPanelOpen,
            toolActions = privateToolActions,
            onOpenTools = { dispatch(PrivateChatAction.OpenTools) },
            onDismissToolPanel = { dispatch(PrivateChatAction.CloseTools) },
            onImageClick = { images, index -> imageViewer.open(images, index) },
            onLinkClick = { dispatch(PrivateChatAction.ShowLinkPreview(it)) },
            canRevoke = { message ->
                message.canBeRevokedBy(
                    session = environment.session,
                    allowRevokeOthers = false,
                    excludeRedPacket = false,
                )
            },
            onQuoteMessage = { dispatch(PrivateChatAction.QuoteMessage(it)) },
            onRepeatMessage = { dispatch(PrivateChatAction.RepeatMessage(it)) },
            onRevokeMessage = { dispatch(PrivateChatAction.RevokeMessage(it)) },
            onSend = { dispatch(PrivateChatAction.SendText) },
        )
    }
    }

    conversation.previewLinkUrl?.let { url ->
        LinkPreviewOverlay(
            url = url,
            apiKey = environment.session.apiKey,
            onDismiss = { dispatch(PrivateChatAction.DismissLinkPreview) },
        )
    }

    BackHandler(enabled = environment.active && conversation.previewLinkUrl != null) {
        dispatch(PrivateChatAction.DismissLinkPreview)
    }
    BackHandler(
        enabled = environment.active && conversation.selectedPeer != null &&
            conversation.actionMessage == null && conversation.previewLinkUrl == null,
    ) {
        dispatch(PrivateChatAction.CloseConversation)
    }
}

@Composable
internal fun ProvidePrivateChatUiEnvironment(
    environment: PrivateChatUiEnvironment,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalPrivateChatUiEnvironment provides environment, content = content)
}

internal data class PrivateChatUiEnvironment(
    val session: AppSession,
    val active: Boolean,
    val onOpenUserProfile: (String) -> Unit = {},
    val listHeader: @Composable (ColumnScope.() -> Unit)? = null,
)

private val LocalPrivateChatUiEnvironment = staticCompositionLocalOf<PrivateChatUiEnvironment> {
    error("PrivateChatUiEnvironment is not provided")
}

private val PrivateChatAvatarSize = 42

@Composable
private fun PrivateSessionList(
    sessions: List<PrivateSessionUiModel>,
    isLoading: Boolean,
    error: String?,
    onOpen: (PrivateSessionUiModel) -> Unit,
    listHeader: @Composable (ColumnScope.() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(rememberUiPageBrush()),
    ) {
        listHeader?.invoke(this)
        error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = FishPiTheme.spacingPage))
        }
        if (isLoading && sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (!isLoading && sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "还没有私聊会话")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = FishPiTheme.spacingPage,
                    top = 0.dp,
                    end = FishPiTheme.spacingPage,
                    bottom = bottomBarContentPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingSection),
            ) {
                items(sessions, key = { it.peer }) { item ->
                    PrivateSessionRow(item = item, onClick = { onOpen(item) })
                }
            }
        }
    }
}

@Composable
private fun PrivateSessionRow(
    item: PrivateSessionUiModel,
    onClick: () -> Unit,
) {
    val previewText = remember(item.preview) {
        item.preview.trim().ifBlank { "暂无预览" }
    }
    val displayName = remember(item.peer) {
        item.peer.toPrivateChatDisplayName()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(
                RoundedCornerShape(FishPiTheme.radiusBox),
            )
            .fishPiGlassSurface(
                backdrop = LocalFishPiGlassBackdrop.current,
                shape = RoundedCornerShape(FishPiTheme.radiusBox),
                containerColor = FishPiTheme.surface.copy(alpha = FishPiTheme.glass.elevatedAlpha),
                borderColor = FishPiTheme.outline.copy(alpha = 0.12f),
                borderWidth = FishPiTheme.borderWidth,
                role = FishPiGlassRole.Content,
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = FishPiTheme.spacingSection,
                vertical = FishPiTheme.spacingControl + 2.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingSection),
    ) {
        PrivateAvatar(url = item.avatar, name = displayName, sizeDp = PrivateChatAvatarSize)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = displayName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 72.dp),
                )
                Text(
                    text = previewText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = if (item.unread > 0) 34.dp else 0.dp),
                )
            }
            Text(
                text = item.time,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.TopEnd),
            )
            if (item.unread > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .clip(RoundedCornerShape(FishPiTheme.radiusSelector))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item.unread.coerceAtMost(99).toString(),
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                    )
                }
            }
        }
    }
}

private fun String.toPrivateChatDisplayName(): String =
    if (isFileTransferPeer()) "文件传输助手" else this

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PrivateConversation(
    peer: String,
    peerAvatar: String,
    messages: List<ChatRoomMessage>,
    input: String,
    inputResetKey: Int,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    isSending: Boolean,
    isUploadingAttachment: Boolean,
    attachments: List<ComposerAttachment>,
    cursorRequest: Int?,
    status: String,
    error: String?,
    selfUsername: String,
    scrollToBottomRequest: Int,
    keepPositionAfterPrependCount: Int,
    onKeepPositionConsumed: () -> Unit,
    onBack: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onLoadMore: () -> Unit,
    onInputChange: (String) -> Unit,
    onCursorChange: (Int) -> Unit,
    onCursorRequestHandled: () -> Unit,
    onRemoveAttachment: (ComposerAttachment) -> Unit,
    onRetryAttachment: (ComposerAttachment) -> Unit,
    quote: ChatQuote?,
    focusInputAfterQuote: Boolean,
    onInputFocused: () -> Unit,
    onCancelQuote: () -> Unit,
    emojiPanelOpen: Boolean,
    emojiGroups: List<EmojiGroupView>,
    emojiItems: List<EmojiItemView>,
    selectedEmojiGroupId: String,
    isLoadingEmojiPack: Boolean,
    emojiPackError: String?,
    onToggleEmojiPanel: () -> Unit,
    onDismissEmojiPanel: () -> Unit,
    onPickEmojiGroup: (String) -> Unit,
    onPickEmoji: (EmojiItemView) -> Unit,
    toolPanelOpen: Boolean,
    toolActions: List<ChatToolAction>,
    onOpenTools: () -> Unit,
    onDismissToolPanel: () -> Unit,
    onImageClick: (images: List<String>, index: Int) -> Unit,
    onLinkClick: (String) -> Unit,
    canRevoke: (ChatRoomMessage) -> Boolean,
    onQuoteMessage: (ChatRoomMessage) -> Unit,
    onRepeatMessage: (ChatRoomMessage) -> Unit,
    onRevokeMessage: (ChatRoomMessage) -> Unit,
    onSend: () -> Unit,
) {
    val listController = rememberNativeMessageListController()
    val context = LocalContext.current
    var actionAnchor by remember(peer) { mutableStateOf<MessageActionAnchor?>(null) }
    var rootOffsetInWindow by remember { mutableStateOf(IntOffset.Zero) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    val displayName = remember(peer) {
        peer.toPrivateChatDisplayName()
    }
    val profileClickable = remember(peer) {
        !peer.isFileTransferPeer()
    }
    val renderHintsCache = remember { mutableMapOf<String, ChatMessageRenderHints>() }
    val listItems = remember(messages) {
        if (renderHintsCache.size > 400) {
            renderHintsCache.clear()
        }
        messages.mapIndexed { index, message ->
            val key = message.renderHintCacheKey()
            ChatListItem(
                message = message,
                separator = messageTimeSeparator(messages.getOrNull(index - 1), message),
                renderHints = renderHintsCache.getOrPut(key) { message.toRenderHints() },
            )
        }
    }
    LaunchedEffect(keepPositionAfterPrependCount) {
        if (keepPositionAfterPrependCount > 0) {
            listController.keepPositionAfterPrepend(keepPositionAfterPrependCount)
            onKeepPositionConsumed()
        }
    }
    var historyNotice by remember(peer) { mutableStateOf<String?>(null) }
    var historyLoadRequested by remember(peer) { mutableStateOf(false) }
    LaunchedEffect(isLoadingMore, hasMore, messages.size, historyLoadRequested) {
        when {
            isLoadingMore -> historyNotice = "正在加载更早消息"
            historyLoadRequested && !hasMore && messages.isNotEmpty() -> {
                historyNotice = "没有更早消息了"
                delay(1400)
                if (historyNotice == "没有更早消息了") {
                    historyNotice = null
                }
                historyLoadRequested = false
            }
            historyNotice == "正在加载更早消息" -> historyNotice = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(rememberUiPageBrush())
            .windowInsetsPadding(WindowInsets.imeAnimationTarget.union(WindowInsets.navigationBars))
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInWindow()
                rootOffsetInWindow = IntOffset(position.x.toInt(), position.y.toInt())
                rootSize = coordinates.size
            },
    ) {
        TopAppBar(
            modifier = Modifier
                .fillMaxWidth(),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = FishPiTheme.surface.copy(alpha = FishPiTheme.glass.elevatedAlpha),
                titleContentColor = FishPiTheme.onSurface,
                navigationIconContentColor = FishPiTheme.onSurface,
            ),
            navigationIcon = {
                PlainBackButton(onClick = onBack, tint = FishPiTheme.onSurface)
            },
            title = {
                Row(
                    modifier = Modifier.clickable(enabled = profileClickable) {
                        onOpenUserProfile(peer)
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PrivateAvatar(url = peerAvatar, name = displayName, sizeDp = 36)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = displayName,
                            color = FishPiTheme.onSurface,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            val connectionColor = when {
                                status.contains("已连接") -> FishPiTheme.accent
                                status.contains("失败") -> FishPiTheme.error
                                status.contains("断开") || status.contains("暂停") -> FishPiTheme.warning
                                else -> FishPiTheme.warning
                            }
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(connectionColor),
                            )
                            Text(
                                text = status.ifBlank { "私聊" },
                                color = FishPiTheme.weakText,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            },
        )
        error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = FishPiTheme.spacingPage))
        }
        if (isLoading) {
            LoadingScreen("加载私聊历史...")
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            ) {
                NativeMessageList(
                    items = listItems,
                    selfUsername = selfUsername,
                    showAvatars = true,
                    scrollToBottomRequest = scrollToBottomRequest,
                    redPacketJumpTargetId = null,
                    contentTopPaddingDp = 8,
                    modifier = Modifier.fillMaxSize(),
                    onLoadMore = { if (hasMore && !isLoadingMore) onLoadMore() },
                    onNearTopChanged = { nearTop ->
                        if (nearTop && hasMore && !isLoadingMore) {
                            historyLoadRequested = true
                            onLoadMore()
                        }
                    },
                    onNearBottomChanged = {},
                    onImageClick = onImageClick,
                    onLinkClick = onLinkClick,
                    onLongPress = { anchor -> actionAnchor = anchor },
                    onAvatarClick = { username ->
                        if (username.isNotBlank() && !username.isFileTransferPeer()) {
                            onOpenUserProfile(username)
                        }
                    },
                    onAvatarLongPress = {},
                    onRedPacketClick = {},
                    onReactionClick = { _, _ -> },
                    onTapBlankArea = {
                        onDismissEmojiPanel()
                        onDismissToolPanel()
                    },
                    controller = listController,
                )
                if (emojiPanelOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(6f)
                            .silentTap { onDismissEmojiPanel() },
                    )
                }
                historyNotice?.let { notice ->
                    FloatingNoticePill(
                        text = notice,
                        onClick = {},
                        enabled = false,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp),
                    )
                }
            }
        }
        ChatInputBar(
            input = input,
            quote = quote,
            focusInputAfterQuote = focusInputAfterQuote,
            inputResetKey = inputResetKey,
            isUploadingAttachment = isUploadingAttachment,
            attachments = attachments,
            atCandidates = emptyList(),
            sendOnEnter = true,
            emojiPanelOpen = emojiPanelOpen,
            emojiGroups = emojiGroups,
            emojiItems = emojiItems,
            selectedEmojiGroupId = selectedEmojiGroupId,
            isLoadingEmojiPack = isLoadingEmojiPack,
            emojiPackError = emojiPackError,
            toolPanelOpen = toolPanelOpen,
            toolActions = toolActions,
            onInputChange = onInputChange,
            onCancelQuote = onCancelQuote,
            onRemoveAttachment = onRemoveAttachment,
            onRetryAttachment = onRetryAttachment,
            onInputFocused = onInputFocused,
            onOpenTools = onOpenTools,
            onDismissToolPanel = onDismissToolPanel,
            onToggleEmojiPanel = onToggleEmojiPanel,
            onDismissEmojiPanel = onDismissEmojiPanel,
            onPickEmojiGroup = onPickEmojiGroup,
            onPickEmoji = onPickEmoji,
            onPickAtUser = {},
            onCursorPositionChange = onCursorChange,
            inputCursorPositionRequest = cursorRequest,
            onInputCursorPositionRequestHandled = onCursorRequestHandled,
            onSend = { onSend() },
        )
        actionAnchor?.let { anchor ->
            MessageActionBubbleMenu(
                anchor = anchor,
                rootOffsetInWindow = rootOffsetInWindow,
                rootSize = rootSize,
                canRevoke = canRevoke(anchor.message),
                onDismiss = { actionAnchor = null },
                onCopyContent = {
                    context.copyToClipboard("私聊内容", anchor.message.copyableText())
                },
                onCopyUsername = {
                    context.copyToClipboard(
                        "用户名",
                        anchor.message.userName.ifBlank { anchor.message.displayName },
                    )
                },
                onQuote = { onQuoteMessage(anchor.message) },
                onRepeat = { onRepeatMessage(anchor.message) },
                onRevoke = { onRevokeMessage(anchor.message) },
                onMentionUser = null,
                onReaction = null,
            )
        }
    }
}

@Composable
private fun PrivateAvatar(url: String, name: String, sizeDp: Int = 44) {
    val modifier = Modifier
        .size(sizeDp.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer)
    if (url.isBlank()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(text = name.firstOrNull()?.toString() ?: "鱼", color = MaterialTheme.colorScheme.primary)
        }
        return
    }
    SubcomposeAsyncImage(
        model = url,
        imageLoader = rememberFishPiImageLoader(),
        contentDescription = "$name 头像",
        contentScale = ContentScale.Crop,
        error = {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Text(text = name.firstOrNull()?.toString() ?: "鱼", color = MaterialTheme.colorScheme.primary)
            }
        },
        modifier = modifier,
    )
}
