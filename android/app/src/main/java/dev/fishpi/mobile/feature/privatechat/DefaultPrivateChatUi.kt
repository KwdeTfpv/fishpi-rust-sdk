package dev.fishpi.mobile.feature.privatechat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.SubcomposeAsyncImage
import dev.fishpi.mobile.AppSession
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.ui.components.ChatInputBar
import dev.fishpi.mobile.shared.message.ChatListItem
import dev.fishpi.mobile.shared.message.ChatQuote
import dev.fishpi.mobile.ui.components.FishPiIconButton
import dev.fishpi.mobile.ui.components.PlainBackButton
import dev.fishpi.mobile.ui.overlay.ImagePreviewOverlay
import dev.fishpi.mobile.ui.overlay.LinkPreviewOverlay
import dev.fishpi.mobile.ui.components.LoadingScreen
import dev.fishpi.mobile.ui.components.FloatingNoticePill
import dev.fishpi.mobile.shared.message.ui.MessageActionSheet
import dev.fishpi.mobile.ui.components.QuoteThumbnail
import dev.fishpi.mobile.shared.message.canBeRevokedBy
import dev.fishpi.mobile.shared.message.copyToClipboard
import dev.fishpi.mobile.shared.message.copyableText
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.data.EmojiGroupView
import dev.fishpi.mobile.data.EmojiItemView
import dev.fishpi.mobile.feature.privatechat.mapper.toPrivateChatSession
import dev.fishpi.mobile.feature.privatechat.model.PrivateSessionUiModel
import dev.fishpi.mobile.shared.message.native.NativeMessageList
import dev.fishpi.mobile.shared.message.native.rememberNativeMessageListController
import dev.fishpi.mobile.ui.components.ChatToolAction
import dev.fishpi.mobile.shared.message.messageTimeSeparator
import dev.fishpi.mobile.ui.media.rememberChatAttachmentPicker
import dev.fishpi.mobile.rememberFishPiImageLoader
import dev.fishpi.mobile.ui.components.silentTap
import dev.fishpi.mobile.shared.message.toRenderHints
import kotlinx.coroutines.delay

@Composable
internal fun DefaultPrivateChatUi(
    state: PrivateChatState,
    dispatch: (PrivateChatAction) -> Unit,
) {
    val environment = LocalPrivateChatUiEnvironment.current
    val conversation = state.conversation
    val context = LocalContext.current
    val attachmentPicker = rememberChatAttachmentPicker(
        onPickedPath = { dispatch(PrivateChatAction.UploadAttachment(it)) },
        onError = { dispatch(PrivateChatAction.ShowError(it)) },
    )
    val privateToolActions = listOf(
        ChatToolAction(
            id = "gallery",
            label = "相册",
            icon = Icons.Rounded.PhotoLibrary,
            enabled = !conversation.isUploadingAttachment,
            onClick = {
                dispatch(PrivateChatAction.CloseTools)
                attachmentPicker.openGallery()
            },
        ),
        ChatToolAction(
            id = "camera",
            label = "拍照",
            icon = Icons.Rounded.AddAPhoto,
            enabled = !conversation.isUploadingAttachment,
            onClick = {
                dispatch(PrivateChatAction.CloseTools)
                attachmentPicker.openCamera()
            },
        ),
    )

    if (conversation.selectedPeer == null) {
        PrivateSessionList(
            sessions = state.sessions,
            isLoading = state.isLoadingSessions,
            error = state.error,
            onOpen = { dispatch(PrivateChatAction.OpenSession(it.toPrivateChatSession())) },
            listHeader = environment.listHeader,
        )
    } else {
        PrivateConversation(
            peer = conversation.selectedPeer,
            peerAvatar = conversation.selectedPeerAvatar,
            messages = conversation.messages,
            input = conversation.input,
            inputResetKey = conversation.inputResetKey,
            isLoading = conversation.isLoading,
            isLoadingMore = conversation.isLoadingMore,
            hasMore = conversation.hasMoreHistory,
            isSending = conversation.isSending,
            isUploadingAttachment = conversation.isUploadingAttachment,
            status = conversation.status,
            error = state.error,
            selfUsername = environment.session.user.userName,
            scrollToBottomRequest = conversation.scrollToBottomRequest,
            keepPositionAfterPrependCount = conversation.keepPositionAfterPrependCount,
            onKeepPositionConsumed = { dispatch(PrivateChatAction.KeepPositionConsumed) },
            onBack = { dispatch(PrivateChatAction.CloseConversation) },
            onLoadMore = { dispatch(PrivateChatAction.LoadMoreHistory) },
            onInputChange = { dispatch(PrivateChatAction.ChangeInput(it)) },
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
            onImageClick = { dispatch(PrivateChatAction.ShowImagePreview(it)) },
            onLinkClick = { dispatch(PrivateChatAction.ShowLinkPreview(it)) },
            onMessageLongPress = { dispatch(PrivateChatAction.ShowMessageActions(it)) },
            onSend = { dispatch(PrivateChatAction.SendText) },
        )
    }

    conversation.actionMessage?.let { message ->
        MessageActionSheet(
            message = message,
            canRevoke = message.canBeRevokedBy(
                session = environment.session,
                allowRevokeOthers = false,
                excludeRedPacket = false,
            ),
            onDismiss = { dispatch(PrivateChatAction.DismissMessageActions) },
            onCopyContent = { context.copyToClipboard("私聊内容", message.copyableText()) },
            onCopyUsername = {
                context.copyToClipboard("用户名", message.userName.ifBlank { message.displayName })
            },
            onCopyImageLinks = { context.copyToClipboard("私聊图片链接", message.imageUrls.joinToString("\n")) },
            onCopyLinks = { context.copyToClipboard("私聊链接", message.linkUrls.joinToString("\n")) },
            onQuote = { dispatch(PrivateChatAction.QuoteMessage(message)) },
            onReaction = {},
            onRepeat = { dispatch(PrivateChatAction.RepeatMessage(message)) },
            onRevoke = { dispatch(PrivateChatAction.RevokeMessage(message)) },
            showReactions = false,
        )
    }

    conversation.previewImageUrl?.let { url ->
        ImagePreviewOverlay(
            imageUrl = url,
            onDismiss = { dispatch(PrivateChatAction.DismissImagePreview) },
        )
    }
    conversation.previewLinkUrl?.let { url ->
        LinkPreviewOverlay(
            url = url,
            apiKey = environment.session.apiKey,
            onDismiss = { dispatch(PrivateChatAction.DismissLinkPreview) },
        )
    }

    BackHandler(enabled = environment.active && conversation.actionMessage != null) {
        dispatch(PrivateChatAction.DismissMessageActions)
    }
    BackHandler(enabled = environment.active && conversation.previewImageUrl != null) {
        dispatch(PrivateChatAction.DismissImagePreview)
    }
    BackHandler(enabled = environment.active && conversation.previewLinkUrl != null) {
        dispatch(PrivateChatAction.DismissLinkPreview)
    }
    BackHandler(
        enabled = environment.active && conversation.selectedPeer != null &&
            conversation.actionMessage == null && conversation.previewImageUrl == null && conversation.previewLinkUrl == null,
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
    val listHeader: @Composable (ColumnScope.() -> Unit)? = null,
)

private val LocalPrivateChatUiEnvironment = staticCompositionLocalOf<PrivateChatUiEnvironment> {
    error("PrivateChatUiEnvironment is not provided")
}

private val PrivateChatScreenPadding = 16.dp
private val PrivateChatSectionGap = 12.dp
private val PrivateChatInlineGap = 16.dp
private val PrivateChatCardHorizontalPadding = 16.dp
private val PrivateChatCardVerticalPadding = 12.dp
private val PrivateChatCardRadius = 16.dp
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
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            ),
    ) {
        listHeader?.invoke(this)
        error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 14.dp))
        }
        if (!isLoading && sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "还没有私聊会话")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = PrivateChatScreenPadding,
                    top = 0.dp,
                    end = PrivateChatScreenPadding,
                    bottom = PrivateChatSectionGap,
                ),
                verticalArrangement = Arrangement.spacedBy(PrivateChatSectionGap),
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(
                RoundedCornerShape(PrivateChatCardRadius),
            )
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = PrivateChatCardHorizontalPadding,
                vertical = PrivateChatCardVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PrivateChatInlineGap),
    ) {
        PrivateAvatar(url = item.avatar, name = item.peer, sizeDp = PrivateChatAvatarSize)
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
                    text = item.peer,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 72.dp),
                )
                Text(
                    text = item.preview.ifBlank { "暂无预览" },
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
                        .clip(RoundedCornerShape(10.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
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
    status: String,
    error: String?,
    selfUsername: String,
    scrollToBottomRequest: Int,
    keepPositionAfterPrependCount: Int,
    onKeepPositionConsumed: () -> Unit,
    onBack: () -> Unit,
    onLoadMore: () -> Unit,
    onInputChange: (String) -> Unit,
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
    onImageClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onMessageLongPress: (ChatRoomMessage) -> Unit,
    onSend: () -> Unit,
) {
    val listController = rememberNativeMessageListController()
    val listItems = remember(messages) {
        messages.mapIndexed { index, message ->
            ChatListItem(
                message = message,
                separator = messageTimeSeparator(messages.getOrNull(index - 1), message),
                renderHints = message.toRenderHints(),
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
            .background(FishPiTheme.chatBackground)
            .imePadding(),
    ) {
        TopAppBar(
            modifier = Modifier
                .fillMaxWidth(),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = FishPiTheme.surface.copy(alpha = 0.96f),
                titleContentColor = FishPiTheme.onSurface,
                navigationIconContentColor = FishPiTheme.onSurface,
            ),
            navigationIcon = {
                PlainBackButton(onClick = onBack, tint = FishPiTheme.onSurface)
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PrivateAvatar(url = peerAvatar, name = peer, sizeDp = 36)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = peer,
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
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(FishPiTheme.accent),
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
            Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 14.dp))
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
                    onLongPress = { anchor -> onMessageLongPress(anchor.message) },
                    onAvatarClick = {},
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
            pendingAttachments = emptyList(),
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
            onRemoveAttachment = {},
            onInputFocused = onInputFocused,
            onOpenTools = onOpenTools,
            onDismissToolPanel = onDismissToolPanel,
            onToggleEmojiPanel = onToggleEmojiPanel,
            onDismissEmojiPanel = onDismissEmojiPanel,
            onPickEmojiGroup = onPickEmojiGroup,
            onPickEmoji = onPickEmoji,
            onPickAtUser = {},
            inputCursorPositionRequest = null,
            onInputCursorPositionRequestHandled = {},
            onSend = { onSend() },
        )
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

@Composable
private fun PrivateQuotePreviewBar(
    quote: ChatQuote,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        quote.imageUrls.firstOrNull()?.let { url ->
            QuoteThumbnail(url = url)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "正在引用 @${quote.username}",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = quote.preview, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        FishPiIconButton(
            icon = Icons.Rounded.Close,
            contentDescription = "取消引用",
            onClick = onCancel,
            background = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f),
            sizeDp = 32,
            iconSizeDp = 18,
        )
    }
}


