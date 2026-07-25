package dev.fishpi.mobile.shared.message.native

import android.view.MotionEvent
import android.view.View
import android.graphics.drawable.ColorDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.asDrawable
import coil3.request.ImageRequest
import dev.fishpi.mobile.shared.message.ChatListItem
import dev.fishpi.mobile.LocalFishPiPalette
import dev.fishpi.mobile.LocalFishPiThemeTokens
import dev.fishpi.mobile.chatui.ChatMarkdownRenderCache
import dev.fishpi.mobile.chatui.MarkwonChatRenderer
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.rememberFishPiImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

@Composable
internal fun rememberNativeMessageListController(): NativeMessageListController =
    remember { NativeMessageListController() }

@Composable
internal fun NativeMessageList(
    items: List<ChatListItem>,
    selfUsername: String,
    showAvatars: Boolean,
    scrollToBottomRequest: Int,
    allowScrollToBottom: Boolean = true,
    redPacketJumpTargetId: String?,
    active: Boolean = true,
    contentTopPaddingDp: Int = 0,
    drawBackground: Boolean = true,
    modifier: Modifier = Modifier,
    onLoadMore: () -> Unit,
    onNearBottomChanged: (Boolean) -> Unit,
    onNearTopChanged: (Boolean) -> Unit = {},
    onVisibleRangeChanged: (firstVisible: Int, lastVisible: Int, itemCount: Int) -> Unit = { _, _, _ -> },
    onImageClick: (images: List<String>, index: Int) -> Unit,
    onLinkClick: (String) -> Unit,
    onLongPress: (MessageActionAnchor) -> Unit,
    onAvatarClick: (String) -> Unit,
    onAvatarLongPress: (String) -> Unit,
    onRedPacketClick: (ChatRoomMessage) -> Unit,
    onRedPacketGestureClick: (ChatRoomMessage, Int) -> Unit = { _, _ -> },
    onReactionClick: (ChatRoomMessage, String) -> Unit,
    onRepeatClick: (ChatRoomMessage) -> Unit = {},
    onVideoFullscreenClick: (String) -> Unit = {},
    onTapBlankArea: () -> Unit = {},
    controller: NativeMessageListController = rememberNativeMessageListController(),
) {
    val palette = LocalFishPiPalette.current
    val tokens = LocalFishPiThemeTokens.current
    val context = LocalContext.current
    val theme = remember(palette, tokens) { NativeMessageTheme.fromTheme(palette, tokens) }
    val imageLoader = rememberFishPiImageLoader()
    val markdownCache = remember(palette) { ChatMarkdownRenderCache() }
    val renderScope = remember(palette) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    val handledScrollToBottomRequest = remember { mutableStateOf(scrollToBottomRequest) }
    val currentOnNearBottomChanged = rememberUpdatedState(onNearBottomChanged)
    val currentOnNearTopChanged = rememberUpdatedState(onNearTopChanged)
    val currentOnVisibleRangeChanged = rememberUpdatedState(onVisibleRangeChanged)
    val currentOnTapBlankArea = rememberUpdatedState(onTapBlankArea)
    val markdownRenderer = remember(palette) {
        MarkwonChatRenderer(
            context = context,
            theme = theme,
            cache = markdownCache,
            scope = renderScope,
            onLinkClick = onLinkClick,
            onMentionClick = onAvatarClick,
        )
    }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            RecyclerView(context).apply {
                applyMessageListBackground(theme, imageLoader, drawBackground)
                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                itemAnimator = null
                clipToPadding = true
                setPadding(0, contentTopPaddingDp.dp(context), 0, 0)
                layoutManager = LinearLayoutManager(context).apply {
                    stackFromEnd = true
                }
                val chatAdapter = NativeMessageAdapter(
                    theme = theme,
                    markdownRenderer = markdownRenderer,
                    imageLoader = imageLoader,
                    selfUsername = selfUsername,
                    showAvatars = showAvatars,
                    onImageClick = onImageClick,
                    onLinkClick = onLinkClick,
                    onLongPress = onLongPress,
                    onAvatarClick = onAvatarClick,
                    onAvatarLongPress = onAvatarLongPress,
                    onRedPacketClick = onRedPacketClick,
                    onRedPacketGestureClick = onRedPacketGestureClick,
                    onReactionClick = onReactionClick,
                    onRepeatClick = onRepeatClick,
                    onVideoFullscreenClick = onVideoFullscreenClick,
                )
                adapter = chatAdapter
                controller.recyclerView = this
                controller.adapter = chatAdapter
                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        currentOnTapBlankArea.value()
                    }
                    false
                }
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        dispatchEdgeState(
                            recyclerView,
                            chatAdapter,
                            controller,
                            currentOnNearTopChanged.value,
                            currentOnNearBottomChanged.value,
                            currentOnVisibleRangeChanged.value,
                        )
                    }

                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        dispatchEdgeState(
                            recyclerView,
                            chatAdapter,
                            controller,
                            currentOnNearTopChanged.value,
                            currentOnNearBottomChanged.value,
                            currentOnVisibleRangeChanged.value,
                        )
                    }
                })
            }
        },
        update = { view ->
            val chatAdapter = view.adapter as NativeMessageAdapter
            val presentationChanged = chatAdapter.updatePresentation(
                theme = theme,
                markdownRenderer = markdownRenderer,
                imageLoader = imageLoader,
                selfUsername = selfUsername,
                showAvatars = showAvatars,
            )
            view.applyMessageListBackground(theme, imageLoader, drawBackground)
            val topPaddingPx = contentTopPaddingDp.dp(view.context)
            val paddingChanged = view.paddingTop != topPaddingPx
            if (paddingChanged) {
                view.setPadding(0, topPaddingPx, 0, 0)
            }
            if (!active) {
                return@AndroidView
            }
            val itemsChanged = chatAdapter.submit(items)
            if (itemsChanged) {
                controller.restorePrependAnchorIfNeeded()
            }
            if (itemsChanged || presentationChanged || paddingChanged || controller.hasPendingScrollToBottom) {
                view.postOnAnimation {
                    controller.onItemsChanged()
                    dispatchEdgeState(
                        view,
                        chatAdapter,
                        controller,
                        currentOnNearTopChanged.value,
                        currentOnNearBottomChanged.value,
                        currentOnVisibleRangeChanged.value,
                    )
                }
            }
        },
    )

    LaunchedEffect(active, scrollToBottomRequest, allowScrollToBottom) {
        if (!active || scrollToBottomRequest <= handledScrollToBottomRequest.value) {
            return@LaunchedEffect
        }
        if (allowScrollToBottom) {
            handledScrollToBottomRequest.value = scrollToBottomRequest
            controller.requestScrollToBottom()
        }
    }

    LaunchedEffect(active, redPacketJumpTargetId) {
        if (active) {
            redPacketJumpTargetId?.let(controller::scrollToMessage)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            controller.recyclerView = null
            controller.adapter = null
        }
    }

    DisposableEffect(markdownCache, renderScope) {
        onDispose {
            markdownCache.clear()
            renderScope.cancel()
        }
    }
}

private fun Int.dp(context: android.content.Context): Int =
    (this * context.resources.displayMetrics.density).toInt()

private fun dispatchEdgeState(
    recyclerView: RecyclerView,
    adapter: NativeMessageAdapter,
    controller: NativeMessageListController,
    onNearTopChanged: (Boolean) -> Unit,
    onNearBottomChanged: (Boolean) -> Unit,
    onVisibleRangeChanged: (firstVisible: Int, lastVisible: Int, itemCount: Int) -> Unit,
) {
    val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
    val first = lm.findFirstVisibleItemPosition()
    val last = lm.findLastVisibleItemPosition()
    val count = adapter.itemCount
    if (count > 0 && (first == RecyclerView.NO_POSITION || last == RecyclerView.NO_POSITION)) {
        return
    }
    val nearTop = count == 0 || first <= 2 || !recyclerView.canScrollVertically(-1)
    val nearBottom = controller.isNearBottom()
    if (controller.consumeNearTopChanged(nearTop)) {
        onNearTopChanged(nearTop)
    }
    if (controller.consumeNearBottomChanged(nearBottom)) {
        onNearBottomChanged(nearBottom)
    }
    if (controller.consumeVisibleRangeChanged(first, last, count)) {
        onVisibleRangeChanged(first, last, count)
    }
}

private fun RecyclerView.applyMessageListBackground(
    theme: NativeMessageTheme,
    imageLoader: coil3.ImageLoader,
    drawBackground: Boolean,
) {
    if (!drawBackground) {
        tag = null
        background = ColorDrawable(android.graphics.Color.TRANSPARENT)
        return
    }
    val uri = theme.wallpaperImageUri
    if (uri.isNullOrBlank()) {
        tag = null
        background = theme.wallpaperDrawable()
        return
    }
    if (tag == uri && background != null) return
    tag = uri
    background = theme.wallpaperDrawable()
    val request = ImageRequest.Builder(context)
        .data(uri)
        .target(
            onSuccess = { image ->
                if (tag == uri) {
                    background = image.asDrawable(resources)
                }
            },
            onError = {
                if (tag == uri) {
                    background = theme.wallpaperDrawable()
                }
            },
        )
        .build()
    imageLoader.enqueue(request)
}


