package dev.fishpi.mobile.shared.message.native

import dev.fishpi.mobile.shared.message.RepeatStackInfo

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.Typeface
import android.graphics.drawable.Animatable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.net.Uri
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil3.ImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Size
import dev.fishpi.mobile.shared.message.ChatListItem
import dev.fishpi.mobile.shared.message.allRenderableImageUrls
import dev.fishpi.mobile.R
import dev.fishpi.mobile.chatui.MarkwonChatRenderer
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.feature.redpacket.RedPacketGestureOptions
import dev.fishpi.mobile.feature.redpacket.toRedPacketUiModel
import dev.fishpi.mobile.utils.adaptiveImageBoxSize
import dev.fishpi.mobile.utils.cleanImageSplitTextSegment
import dev.fishpi.mobile.utils.decodeBasicHtmlEntities
import dev.fishpi.mobile.utils.extractImageTokens
import dev.fishpi.mobile.utils.HtmlTagRegex
import dev.fishpi.mobile.utils.MarkdownMediaType
import dev.fishpi.mobile.utils.toFishPiGeneratedBadgeOrNull
import kotlinx.coroutines.Job
import kotlin.math.roundToInt

internal class NativeMessageAdapter(
    private var theme: NativeMessageTheme,
    private var markdownRenderer: MarkwonChatRenderer,
    var imageLoader: ImageLoader,
    var selfUsername: String,
    var showAvatars: Boolean,
    private val onImageClick: (String) -> Unit,
    private val onLinkClick: (String) -> Unit,
    private val onLongPress: (MessageActionAnchor) -> Unit,
    private val onAvatarClick: (String) -> Unit,
    private val onAvatarLongPress: (String) -> Unit,
    private val onRedPacketClick: (ChatRoomMessage) -> Unit,
    private val onRedPacketGestureClick: (ChatRoomMessage, Int) -> Unit,
    private val onReactionClick: (ChatRoomMessage, String) -> Unit,
    private val onRepeatClick: (ChatRoomMessage) -> Unit,
    private val onVideoFullscreenClick: (String) -> Unit,
) : RecyclerView.Adapter<NativeMessageViewHolder>() {
    var currentItems: List<ChatListItem> = emptyList()
        private set
    private val avatarDrawableStore = SharedAvatarDrawableStore()
    private val messageDrawableStore = SharedMessageDrawableStore()

    private val pendingEntranceKeys = HashSet<String>()
    private val playedEntranceKeys = object : LinkedHashSet<String>() {
        override fun add(element: String): Boolean {
            val added = super.add(element)
            if (added && size > PLAYED_ENTRANCE_CAP) {
                iterator().let { if (it.hasNext()) { it.next(); it.remove() } }
            }
            return added
        }
    }

    fun updatePresentation(
        theme: NativeMessageTheme,
        markdownRenderer: MarkwonChatRenderer,
        imageLoader: ImageLoader,
        selfUsername: String,
        showAvatars: Boolean,
    ): Boolean {
        val presentationChanged = this.theme != theme ||
            this.markdownRenderer !== markdownRenderer ||
            this.showAvatars != showAvatars ||
            !this.selfUsername.equals(selfUsername, ignoreCase = false)
        this.theme = theme
        this.markdownRenderer = markdownRenderer
        this.imageLoader = imageLoader
        this.selfUsername = selfUsername
        this.showAvatars = showAvatars
        if (presentationChanged && currentItems.isNotEmpty()) {
            notifyItemRangeChanged(0, currentItems.size)
        }
        return presentationChanged
    }

    fun submit(items: List<ChatListItem>): Boolean {
        val previous = currentItems
        if (previous === items || previous.sameContentAs(items)) {
            return false
        }
        if (previous.isNotEmpty() && items.size > previous.size && items.startsWithSameContent(previous)) {
            for (index in previous.size until items.size) {
                val key = items[index].message.stableNativeKey()
                if (key !in playedEntranceKeys) {
                    pendingEntranceKeys.add(key)
                }
            }
            currentItems = items
            avatarDrawableStore.retainAvatarUrls(items)
            messageDrawableStore.retainMediaUrls(items)
            notifyItemRangeInserted(previous.size, items.size - previous.size)
            return true
        }
        val diff = DiffUtil.calculateDiff(ChatItemDiff(previous, items))
        currentItems = items
        avatarDrawableStore.retainAvatarUrls(items)
        messageDrawableStore.retainMediaUrls(items)
        diff.dispatchUpdatesTo(this)
        return true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NativeMessageViewHolder {
        return NativeMessageViewHolder(
            view = LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(4), 0, dp(4))
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            },
            themeProvider = { theme },
            markdownRendererProvider = { markdownRenderer },
            imageLoaderProvider = { imageLoader },
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
            avatarDrawableStore = avatarDrawableStore,
            messageDrawableStore = messageDrawableStore,
        )
    }

    override fun getItemCount(): Int = currentItems.size

    override fun onBindViewHolder(holder: NativeMessageViewHolder, position: Int) {
        val item = currentItems[position]
        val mine = item.message.userName.equals(selfUsername, ignoreCase = true)
        holder.bind(
            item = item,
            isMine = mine,
            showAvatars = showAvatars,
        )
    }

    override fun onViewRecycled(holder: NativeMessageViewHolder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    override fun onViewAttachedToWindow(holder: NativeMessageViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.resumeAnimations()
        maybePlayEntrance(holder)
    }

    private fun maybePlayEntrance(holder: NativeMessageViewHolder) {
        val position = holder.bindingAdapterPosition
        if (position == RecyclerView.NO_POSITION || position >= currentItems.size) return
        val key = currentItems[position].message.stableNativeKey()
        if (!pendingEntranceKeys.remove(key)) return
        playedEntranceKeys.add(key)
        holder.playEntranceAnimation()
    }

    override fun onViewDetachedFromWindow(holder: NativeMessageViewHolder) {
        holder.pauseAnimations()
        super.onViewDetachedFromWindow(holder)
    }
}

private const val NativeRedPacketBackground = 0xFFE94E2F.toInt()
private const val NativeRedPacketText = 0xFFFFF7EC.toInt()
private const val PLAYED_ENTRANCE_CAP = 200
private const val ENTRANCE_DURATION_MS = 260L
private const val ENTRANCE_RISE_DP = 16f
private const val ENTRANCE_START_SCALE = 0.92f
private val entranceInterpolator: android.view.animation.Interpolator =
    android.view.animation.PathInterpolator(0f, 0f, 0.2f, 1f)
private val HtmlAnchorTextRegex = Regex(
    "<a\\b[^>]*>(.*?)</a>",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
private val MarkdownVisibleTextRegex = Regex("!?\\[([^]]*)]\\(([^)]*)\\)")

private data class NativeMusicCard(
    val coverUrl: String,
    val sourceUrl: String,
    val title: String,
    val from: String,
)

private class ChatItemDiff(
    private val oldItems: List<ChatListItem>,
    private val newItems: List<ChatListItem>,
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldItems.size

    override fun getNewListSize(): Int = newItems.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition].message.stableNativeKey() ==
            newItems[newItemPosition].message.stableNativeKey()
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition] == newItems[newItemPosition]
    }
}

private fun ChatRoomMessage.stableNativeKey(): String =
    echoKey.ifBlank { oId.ifBlank { "$time:$userName:${content.hashCode()}" } }

private fun List<ChatListItem>.sameContentAs(other: List<ChatListItem>): Boolean {
    if (size != other.size) return false
    for (index in indices) {
        if (this[index] != other[index]) return false
    }
    return true
}

private fun List<ChatListItem>.startsWithSameContent(prefix: List<ChatListItem>): Boolean {
    if (size < prefix.size) return false
    for (index in prefix.indices) {
        if (this[index] != prefix[index]) return false
    }
    return true
}

private class ChatVideoCard(
    context: android.content.Context,
    private val theme: NativeMessageTheme,
) : FrameLayout(context) {
    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var controlsVisible = true
    private var videoUrl: String = ""
    private var fullscreenCallback: () -> Unit = {}
    private val hideControlsRunnable = Runnable { setControlsVisible(false) }
    private val placeholder = TextView(context).apply {
        text = "视频"
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(0xFFEDEFF5.toInt())
        gravity = Gravity.CENTER
        background = roundRect(0xFF05070AL.toInt(), 12f)
    }
    private val loading = ProgressBar(context).apply {
        isIndeterminate = true
        visibility = GONE
    }
    private val error = TextView(context).apply {
        text = "无法播放"
        textSize = 12f
        setTextColor(0xFFEDEFF5.toInt())
        gravity = Gravity.CENTER
        visibility = GONE
        setPadding(dp(10), dp(6), dp(10), dp(6))
        background = roundRect(0x66000000, 999f)
    }
    private val play = videoIconButton(R.drawable.ic_video_play, "播放")
    private val openExternal = videoIconButton(R.drawable.ic_video_open_external, "系统播放器")
    private val fullscreen = videoIconButton(R.drawable.ic_video_fullscreen, "全屏")

    init {
        background = roundRect(0xFF05070AL.toInt(), 12f)
        clipToOutline = true
        addView(placeholder, LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        ))
        addView(play, LayoutParams(dp(52), dp(52), Gravity.CENTER))
        addView(loading, LayoutParams(dp(28), dp(28), Gravity.CENTER))
        addView(error, LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER,
        ))
        addView(openExternal, LayoutParams(dp(44), dp(44), Gravity.TOP or Gravity.END).apply {
            topMargin = dp(8)
            rightMargin = dp(8)
        })
        addView(fullscreen, LayoutParams(dp(44), dp(44), Gravity.BOTTOM or Gravity.END).apply {
            rightMargin = dp(8)
            bottomMargin = dp(8)
        })
    }

    fun bind(url: String, onFullscreen: () -> Unit) {
        releasePlayer()
        videoUrl = url
        fullscreenCallback = onFullscreen
        error.visibility = GONE
        loading.visibility = GONE
        play.visibility = VISIBLE
        placeholder.visibility = VISIBLE
        setControlsVisible(true)
        play.setOnClickListener {
            startPlayer(playImmediately = true)
        }
        fullscreen.setOnClickListener { fullscreenCallback() }
        openExternal.setOnClickListener { context.openSystemVideo(videoUrl) }
        setOnClickListener {
            val exo = player
            if (exo == null) {
                startPlayer(playImmediately = true)
            } else if (exo.isPlaying && !controlsVisible) {
                setControlsVisible(true)
                scheduleHideControls()
            } else if (exo.isPlaying) {
                exo.pause()
            } else {
                exo.play()
            }
        }
    }

    private fun startPlayer(playImmediately: Boolean) {
        if (videoUrl.isBlank()) return
        val existing = player
        if (existing != null) {
            if (playImmediately) existing.play()
            return
        }
        error.visibility = GONE
        loading.visibility = VISIBLE
        val activePlayerView = PlayerView(context).apply {
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            setShutterBackgroundColor(0xFF05070AL.toInt())
            setBackgroundColor(0xFF05070AL.toInt())
        }
        playerView = activePlayerView
        addView(
            activePlayerView,
            0,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        placeholder.visibility = GONE
        val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
        player = ExoPlayer.Builder(context).build().also { exo ->
            activePlayerView.player = exo
            exo.setMediaItem(mediaItem)
            exo.prepare()
            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    loading.visibility = if (playbackState == Player.STATE_BUFFERING) VISIBLE else GONE
                    if (playbackState == Player.STATE_READY && exo.isPlaying) {
                        scheduleHideControls()
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    play.visibility = if (isPlaying) GONE else VISIBLE
                    if (isPlaying) scheduleHideControls() else setControlsVisible(true)
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    loading.visibility = GONE
                    play.visibility = GONE
                    this@ChatVideoCard.error.visibility = VISIBLE
                    setControlsVisible(true)
                }
            })
        }
        if (playImmediately) {
            player?.play()
        }
    }

    fun releasePlayer() {
        removeCallbacks(hideControlsRunnable)
        playerView?.player = null
        player?.release()
        player = null
        playerView?.let(::removeView)
        playerView = null
    }

    private fun setControlsVisible(visible: Boolean) {
        controlsVisible = visible
        val visibility = if (visible) VISIBLE else GONE
        openExternal.visibility = visibility
        fullscreen.visibility = visibility
        if (player?.isPlaying != true && error.visibility != VISIBLE) {
            play.visibility = VISIBLE
        } else if (player?.isPlaying == true) {
            play.visibility = GONE
        }
    }

    private fun scheduleHideControls() {
        removeCallbacks(hideControlsRunnable)
        postDelayed(hideControlsRunnable, 1800)
    }

    private fun videoIconButton(@DrawableRes icon: Int, description: String): ImageButton {
        return ImageButton(context).apply {
            contentDescription = description
            setImageDrawable(ContextCompat.getDrawable(context, icon))
            background = roundRect(0x73000000, 999f)
            scaleType = ImageView.ScaleType.CENTER
            setPadding(dp(10))
        }
    }

    private fun roundRect(color: Int, radiusDp: Float): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radiusDp * resources.displayMetrics.density
    }
}

private class ChatBubbleFrame(context: android.content.Context) : FrameLayout(context) {
    val content: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }

    init {
        clipChildren = false
        clipToPadding = false
        addView(content, LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChild(content, widthMeasureSpec, heightMeasureSpec)
        val desiredWidth = content.measuredWidth.coerceAtLeast(dp(42))
        val desiredHeight = content.measuredHeight
        val resolvedWidth = resolveSize(desiredWidth, widthMeasureSpec)
        val resolvedHeight = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(resolvedWidth, resolvedHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        content.layout(0, 0, content.measuredWidth, content.measuredHeight)
    }
}

internal class NativeMessageViewHolder(
    private val view: LinearLayout,
    private val themeProvider: () -> NativeMessageTheme,
    private val markdownRendererProvider: () -> MarkwonChatRenderer,
    private val imageLoaderProvider: () -> ImageLoader,
    private val onImageClick: (String) -> Unit,
    private val onLinkClick: (String) -> Unit,
    private val onLongPress: (MessageActionAnchor) -> Unit,
    private val onAvatarClick: (String) -> Unit,
    private val onAvatarLongPress: (String) -> Unit,
    private val onRedPacketClick: (ChatRoomMessage) -> Unit,
    private val onRedPacketGestureClick: (ChatRoomMessage, Int) -> Unit,
    private val onReactionClick: (ChatRoomMessage, String) -> Unit,
    private val onRepeatClick: (ChatRoomMessage) -> Unit,
    private val onVideoFullscreenClick: (String) -> Unit,
    private val avatarDrawableStore: SharedAvatarDrawableStore,
    private val messageDrawableStore: SharedMessageDrawableStore,
) : RecyclerView.ViewHolder(view) {
    private var renderJob: Job? = null
    private var sourcePopup: PopupWindow? = null
    private val theme: NativeMessageTheme
        get() = themeProvider()
    private val markdownRenderer: MarkwonChatRenderer
        get() = markdownRendererProvider()
    private val boxRadius: Float
        get() = theme.radiusBoxDp
    private val fieldRadius: Float
        get() = theme.radiusFieldDp
    private val selectorRadius: Float
        get() = theme.radiusSelectorDp
    private fun themeBorderPx(): Int = dp(theme.borderWidthDp).roundToInt().coerceAtLeast(0)
    private fun themeItemPx(): Int = dp(theme.spacingItemDp).roundToInt()
    private fun themeControlPx(): Int = dp(theme.spacingControlDp).roundToInt()

    fun recycle() {
        renderJob?.cancel()
        renderJob = null
        dismissSourcePopup()
        view.animate().cancel()
        resetEntranceTransforms()
        view.stopAnimatedDrawables()
        view.stopVideoViews()
        view.removeAllViews()
    }

    fun pauseAnimations() {
        view.pauseAnimatedDrawables()
    }

    fun resumeAnimations() {
        view.resumeAnimatedDrawables()
    }

    fun playEntranceAnimation() {
        view.animate().cancel()
        val riseFromPx = ENTRANCE_RISE_DP * view.resources.displayMetrics.density
        view.alpha = 0f
        view.translationY = riseFromPx
        view.scaleX = ENTRANCE_START_SCALE
        view.scaleY = ENTRANCE_START_SCALE
        view.post {
            if (!view.isAttachedToWindow) {
                resetEntranceTransforms()
                return@post
            }
            view.pivotX = view.width / 2f
            view.pivotY = view.height.toFloat()
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(ENTRANCE_DURATION_MS)
                .setInterpolator(entranceInterpolator)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        resetEntranceTransforms()
                        view.animate().setListener(null)
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        resetEntranceTransforms()
                    }
                })
                .start()
        }
    }

    private fun resetEntranceTransforms() {
        view.alpha = 1f
        view.translationY = 0f
        view.scaleX = 1f
        view.scaleY = 1f
    }

    fun bind(
        item: ChatListItem,
        isMine: Boolean,
        showAvatars: Boolean,
    ) {
        val context = view.context
        val message = item.message
        renderJob?.cancel()
        renderJob = null
        dismissSourcePopup()
        view.stopAnimatedDrawables()
        view.removeAllViews()

        item.separator?.let { separator ->
            view.addView(serviceText(separator))
        }

        if (message.type == "system") {
            view.addView(serviceText(message.content))
            return
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (isMine) Gravity.END or Gravity.TOP else Gravity.START or Gravity.TOP
        }

        if (showAvatars && !isMine) {
            row.addView(avatarView(message))
        }

        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (isMine) Gravity.END else Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                leftMargin = if (isMine) 0 else themeItemPx()
                rightMargin = if (isMine) themeItemPx() else 0
            }
        }

        val author = authorLine(message, item, isMine)
        val bubble = bubbleView(message, item, isMine)
        column.addView(author)
        column.addView(bubble)
        messageMetaLine(item, isMine)?.let { column.addView(it) }
        if (message.reactionSummary.isNotEmpty()) {
            column.addView(reactionRow(message, isMine, bubble))
        }
        item.repeatStack?.let { repeatStack ->
            column.addView(repeatRow(message, repeatStack, isMine))
        }
        row.addView(column)

        if (showAvatars && isMine) {
            row.addView(avatarView(message))
        }

        view.addView(row)
    }

    private fun serviceText(text: String): TextView {
        return TextView(view.context).apply {
            this.text = text
            setTextColor(NativeRedPacketText)
            textSize = 11f
            gravity = Gravity.CENTER
            background = roundRect(theme.serviceBackground, selectorRadius)
            setPadding(themeControlPx(), dp(4), themeControlPx(), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(4)
                bottomMargin = dp(4)
            }
        }
    }

    private fun authorLine(message: ChatRoomMessage, item: ChatListItem, isMine: Boolean): LinearLayout {
        return LinearLayout(view.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (isMine) Gravity.END or Gravity.CENTER_VERTICAL else Gravity.START or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(dp(284), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(5)
            }
            setPadding(0, 0, 0, 0)
            addView(authorName(message.authorLabel))
        }
    }

    private fun authorName(name: String): TextView {
        return TextView(view.context).apply {
            text = name
            setTextColor(theme.authorName)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
        }
    }

    private fun clientBadge(client: String): TextView {
        return TextView(view.context).apply {
            text = client
            setTextColor(theme.clientText)
            textSize = 9.5f
            includeFontPadding = false
            background = roundRect(theme.clientBackground, fieldRadius * 0.45f)
            val horizontalPadding = (themeControlPx() * 0.5f).roundToInt()
            setPadding(horizontalPadding, dp(1), horizontalPadding, dp(1))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                leftMargin = (themeItemPx() * 0.6f).roundToInt()
                rightMargin = (themeItemPx() * 0.6f).roundToInt()
            }
        }
    }

    private fun bubbleBackground(color: Int): GradientDrawable =
        roundRect(color, boxRadius).apply {
            setStroke(themeBorderPx(), theme.bubbleBorder)
        }

    private fun bubbleView(message: ChatRoomMessage, item: ChatListItem, isMine: Boolean): ChatBubbleFrame {
        val mediaOnly = item.isMediaOnlyMessage()
        val standaloneCard = mediaOnly || message.redPacket != null
        return ChatBubbleFrame(view.context).apply {
            background = if (standaloneCard) null else bubbleBackground(if (isMine) theme.outgoingBubble else theme.incomingBubble)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(2)
            }
            elevation = 0f
            enableMessageLongPress(message, isMine, this)
            content.setPadding(
                if (standaloneCard) 0 else themeControlPx() + dp(1),
                if (standaloneCard) 0 else themeControlPx() - dp(2),
                if (standaloneCard) 0 else themeControlPx() + dp(1),
                if (standaloneCard) 0 else themeControlPx() - dp(1),
            )

            when {
                message.revoked -> content.addText("[该消息已撤回]", 15f)
                message.redPacket != null -> content.addRedPacket(message, isMine, this)
                else -> {
                    val markdown = item.renderHints.markdownContent
                    if (markdown.isNotBlank()) {
                        content.addMarkdownContent(message, item, isMine, this)
                    } else if (item.renderHints.plainFallback.isNotBlank()) {
                        content.addText(item.renderHints.plainFallback, 15f)
                    }
                    item.renderHints.videoLinks.forEach { url -> content.addVideo(url, message, isMine, this) }
                    item.renderHints.previewLinks.take(1).forEach { url -> content.addLink(url, message, isMine, this) }
                }
            }
        }
    }

    private fun ChatListItem.isMediaOnlyMessage(): Boolean {
        if (message.revoked || message.redPacket != null) return false
        val markdown = renderHints.markdownContent.trim()
        val text = renderHints.plainFallback.trim()
        val hasVideo = renderHints.videoLinks.isNotEmpty()
        val tokens = markdown.extractImageTokens()
        val hasRenderableMedia = tokens.any { it.type == MarkdownMediaType.Image || it.type == MarkdownMediaType.Video } || hasVideo
        if (!hasRenderableMedia) return false
        val stripped = buildString {
            var cursor = 0
            tokens.forEach { token ->
                if (token.start > cursor) {
                    append(markdown.substring(cursor, token.start.coerceAtMost(markdown.length)))
                }
                cursor = token.end.coerceAtMost(markdown.length)
            }
            if (cursor < markdown.length) {
                append(markdown.substring(cursor))
            }
        }.cleanImageSplitTextSegment().trim()
        val hasBodyText = if (markdown.isNotBlank()) {
            stripped.isNotBlank()
        } else {
            text.isNotBlank()
        }
        return !hasBodyText
    }

    private fun messageMetaLine(item: ChatListItem, isMine: Boolean): LinearLayout? {
        val time = item.renderHints.timeLabel.trim()
        val client = item.renderHints.clientLabel.trim()
        if (time.isBlank() && client.isBlank()) return null
        return LinearLayout(view.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (isMine) Gravity.END or Gravity.CENTER_VERTICAL else Gravity.START or Gravity.CENTER_VERTICAL
            alpha = 0.62f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = if (isMine) Gravity.END else Gravity.START
                topMargin = dp(3)
                leftMargin = dp(4)
                rightMargin = dp(4)
            }
            if (time.isNotBlank()) {
                addView(metaTime(time))
            }
            clientTypeIcon(client)?.let { icon ->
                if (time.isNotBlank()) {
                    addView(TextView(context).apply {
                        text = "·"
                        setTextColor(theme.timeText)
                        alpha = 0.72f
                        textSize = 10f
                        includeFontPadding = false
                        setPadding(dp(4), 0, dp(4), 0)
                    })
                }
                addView(icon)
            }
        }
    }

    private fun LinearLayout.addText(text: CharSequence, size: Float, color: Int = theme.bubbleText) {
        addView(TextView(context).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            configureBodyText()
        })
    }

    private fun LinearLayout.addMarkdownContent(message: ChatRoomMessage, item: ChatListItem, isMine: Boolean, anchorView: View) {
        message.music?.let { music ->
            addMusicCard(
                card = NativeMusicCard(
                    coverUrl = music.coverUrl.trim(),
                    sourceUrl = music.source.trim(),
                    title = music.title.trim(),
                    from = music.from.trim(),
                ),
                message = message,
                isMine = isMine,
                anchorView = anchorView,
            )
            return
        }

        val content = item.renderHints.markdownContent.trim()
        if (content.isBlank()) return
        val tokens = content.extractImageTokens().take(4)
        if (tokens.isEmpty()) {
            addMarkdownText(message, item, content, anchorView, isMine)
            return
        }
        var cursor = 0
        tokens.forEachIndexed { index, token ->
            val before = content.substring(cursor, token.start).cleanImageSplitTextSegment()
            if (before.isNotBlank()) {
                addMarkdownText(message, item, before, anchorView, isMine, ":text$index")
            }
            when (token.type) {
                MarkdownMediaType.Image -> addImage(token.url, message, isMine, anchorView)
                MarkdownMediaType.Video -> addVideo(token.url, message, isMine, anchorView)
                MarkdownMediaType.FishPiGeneratedBadge -> addFishPiGeneratedBadge(token.url, message, isMine, anchorView)
            }
            cursor = token.end
        }
        val tail = content.substring(cursor).cleanImageSplitTextSegment()
        if (tail.isNotBlank()) {
            addMarkdownText(message, item, tail, anchorView, isMine, ":tail")
        }
    }

    private fun LinearLayout.addMarkdownText(
        message: ChatRoomMessage,
        item: ChatListItem,
        markdown: String,
        anchorView: View,
        isMine: Boolean,
        keySuffix: String = "",
    ) {
        val textView = TextView(context).apply {
            configureBodyText()
            enableMessageLongPress(message, isMine, anchorView)
        }
        addView(textView)
        renderJob = markdownRenderer.renderInto(
            textView = textView,
            messageKey = message.stableNativeKey() + keySuffix,
            markdown = markdown,
            fallback = item.renderHints.plainFallback,
        )
    }

    private fun LinearLayout.addMusicCard(
        card: NativeMusicCard,
        message: ChatRoomMessage,
        isMine: Boolean,
        anchorView: View,
    ) {
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            enableMessageLongPress(message, isMine, anchorView)
            background = roundRect(theme.quoteBackground.withAlpha(150), fieldRadius).apply {
                setStroke(themeBorderPx(), theme.bubbleBorder)
            }
            isClickable = card.sourceUrl.isNotBlank()
            isFocusable = card.sourceUrl.isNotBlank()
            if (card.sourceUrl.isNotBlank()) {
                setOnClickListener {
                    dismissSourcePopup()
                    onLinkClick(card.sourceUrl)
                }
            }
            setPadding(themeControlPx(), themeControlPx(), themeControlPx(), themeControlPx())
            layoutParams = LinearLayout.LayoutParams(dp(236), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(2)
                bottomMargin = dp(6)
            }

            addView(musicCover(card.coverUrl), LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                rightMargin = themeItemPx()
            })

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                addView(TextView(context).apply {
                    text = card.title.ifBlank { "音乐" }
                    setTextColor(theme.bubbleText)
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    includeFontPadding = false
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                })
                addView(TextView(context).apply {
                    text = card.from.ifBlank { "音乐分享" }
                    setTextColor(theme.weakText)
                    textSize = 11f
                    includeFontPadding = false
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dp(5)
                    }
                })
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        })
    }

    private fun musicCover(url: String): FrameLayout {
        return FrameLayout(view.context).apply {
            background = roundRect(theme.clientBackground, fieldRadius)
            clipToOutline = true
            val placeholder = TextView(context).apply {
                text = "♪"
                setTextColor(theme.accent)
                textSize = 22f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                includeFontPadding = false
            }
            addView(placeholder, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ))
            if (url.isBlank()) return@apply
            val image = MessageMediaImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                visibility = View.INVISIBLE
                tag = url
            }
            addView(image, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ))
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(Size(dp(48), dp(48)))
                .precision(Precision.EXACT)
                .target(
                    onSuccess = { result ->
                        if (image.tag == url) {
                            image.setMediaDrawable(result.asDrawable(context.resources))
                            image.visibility = View.VISIBLE
                            placeholder.visibility = View.GONE
                        }
                    },
                    onError = {
                        if (image.tag == url) {
                            image.visibility = View.INVISIBLE
                            placeholder.visibility = View.VISIBLE
                        }
                    },
                )
                .build()
            imageLoaderProvider().enqueue(request)
        }
    }

    private fun LinearLayout.addFishPiGeneratedBadge(url: String, message: ChatRoomMessage, isMine: Boolean, anchorView: View) {
        val badge = url.toFishPiGeneratedBadgeOrNull() ?: run {
            addText(url, 15f)
            return
        }
        addView(FrameLayout(context).apply {
            enableMessageLongPress(message, isMine, anchorView)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(25),
            ).apply {
                topMargin = dp(7)
            }
            val label = TextView(context).apply {
                text = badge.text
                setTextColor(badge.fontColor)
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER_VERTICAL
                includeFontPadding = false
                maxWidth = dp(208)
                background = roundRect(badge.backColor, 100f).apply {
                    setStroke(dp(1), 0xFFCECECE.toInt())
                }
                setPadding(dp(15), 0, dp(10), 0)
            }
            addView(
                label,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(21),
                ).apply {
                    leftMargin = dp(12)
                    topMargin = dp(2)
                },
            )
            val iconFrame = FrameLayout(context).apply {
                background = roundRect(Color.TRANSPARENT, 100f).apply {
                    setStroke(dp(1), 0xFFCECECE.toInt())
                }
                clipToOutline = true
            }
            addView(
                iconFrame,
                FrameLayout.LayoutParams(dp(25), dp(25)).apply {
                    leftMargin = 0
                    topMargin = 0
                },
            )
            val icon = ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                tag = badge.imageUrl
            }
            iconFrame.addView(
                icon,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            val request = ImageRequest.Builder(context)
                .data(badge.imageUrl)
                .size(Size(75, 75))
                .precision(Precision.EXACT)
                .target(
                    onSuccess = { image ->
                        if (icon.tag == badge.imageUrl) {
                            icon.setImageDrawable(image.asDrawable(context.resources))
                        }
                    },
                )
                .build()
            imageLoaderProvider().enqueue(request)
        })
    }

    private fun LinearLayout.addImage(url: String, message: ChatRoomMessage, isMine: Boolean, anchorView: View) {
        addView(FrameLayout(context).apply {
            enableMessageLongPress(message, isMine, anchorView)
            background = roundRect(theme.quoteBackground.withAlpha(76), 8f)
            val maxWidthPx = dp(220)
            val maxHeightPx = dp(300)
            val minHeightPx = dp(96)
            layoutParams = LinearLayout.LayoutParams(maxWidthPx, dp(163)).apply {
                topMargin = dp(7)
            }
            setOnClickListener {
                dismissSourcePopup()
                onImageClick(url)
            }
            val placeholder = TextView(context).apply {
                text = if (url.substringBefore('?').lowercase().endsWith(".gif")) "GIF" else "图片"
                setTextColor(theme.accent)
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }
            addView(placeholder, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ))
            val imageView = MessageMediaImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                visibility = View.INVISIBLE
                tag = url
            }
            if (url.isAnimatedImageUrl()) {
                val decodeWidthPx = minOf(maxWidthPx, dp(176))
                val decodeHeightPx = minOf(maxHeightPx, dp(240))
                val animatedView = SharedMessageImageView(
                    context = context,
                    store = messageDrawableStore,
                    imageLoaderProvider = imageLoaderProvider,
                    url = url,
                    decodeWidthPx = decodeWidthPx,
                    decodeHeightPx = decodeHeightPx,
                    onDrawableReady = { drawable ->
                        val nextSize = adaptiveImageBoxSize(
                            imageWidth = drawable.intrinsicWidth,
                            imageHeight = drawable.intrinsicHeight,
                            maxWidth = maxWidthPx,
                            maxHeight = maxHeightPx,
                            minHeight = minHeightPx,
                            fallbackWidth = maxWidthPx,
                            fallbackHeight = dp(163),
                        )
                        layoutParams = (layoutParams as LinearLayout.LayoutParams).apply {
                            width = nextSize.first
                            height = nextSize.second
                        }
                        requestLayout()
                        placeholder.visibility = View.GONE
                    },
                    onError = {
                        placeholder.visibility = View.VISIBLE
                    },
                )
                addView(animatedView, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ))
            } else {
                addView(imageView, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ))
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(Size(maxWidthPx, maxHeightPx))
                    .precision(Precision.EXACT)
                    .target(
                        onSuccess = { image ->
                            if (imageView.tag == url) {
                                val drawable = image.asDrawable(context.resources)
                                val nextSize = adaptiveImageBoxSize(
                                    imageWidth = drawable.intrinsicWidth,
                                    imageHeight = drawable.intrinsicHeight,
                                    maxWidth = maxWidthPx,
                                    maxHeight = maxHeightPx,
                                    minHeight = minHeightPx,
                                    fallbackWidth = maxWidthPx,
                                    fallbackHeight = dp(163),
                                )
                                layoutParams = (layoutParams as LinearLayout.LayoutParams).apply {
                                    width = nextSize.first
                                    height = nextSize.second
                                }
                                requestLayout()
                                imageView.setMediaDrawable(drawable)
                                imageView.visibility = View.VISIBLE
                                placeholder.visibility = View.GONE
                            }
                        },
                        onError = {
                            if (imageView.tag == url) {
                                imageView.visibility = View.GONE
                                placeholder.visibility = View.VISIBLE
                            }
                        },
                    )
                    .build()
                imageLoaderProvider().enqueue(request)
            }
        })
    }

    private fun reactionRow(message: ChatRoomMessage, isMine: Boolean, anchorView: View): LinearLayout {
        return LinearLayout(view.context).apply {
            enableMessageLongPress(message, isMine, anchorView)
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(4)
                leftMargin = dp(2)
                rightMargin = dp(2)
            }
            message.reactionSummary.take(6).forEach { reaction ->
                addView(TextView(context).apply {
                    text = "${reaction.emoji} ${reaction.count}"
                    setTextColor(if (reaction.selected) theme.accent else theme.weakText)
                    textSize = 13f
                    typeface = if (reaction.selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    background = roundRect(if (reaction.selected) theme.quoteBackground else 0x00000000, selectorRadius)
                    setPadding(themeControlPx(), dp(4), themeControlPx(), dp(4))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        rightMargin = dp(6)
                    }
                    setOnClickListener {
                        dismissSourcePopup()
                        onReactionClick(message, reaction.value)
                    }
                })
            }
        }
    }

    private fun LinearLayout.addVideo(url: String, message: ChatRoomMessage, isMine: Boolean, anchorView: View) {
        addView(FrameLayout(context).apply {
            enableMessageLongPress(message, isMine, anchorView)
            background = roundRect(theme.quoteBackground.withAlpha(76), boxRadius)
            layoutParams = LinearLayout.LayoutParams(dp(230), dp(176)).apply {
                topMargin = dp(8)
            }
            addView(ChatVideoCard(context, theme).apply {
                bind(
                    url = url,
                    onFullscreen = {
                        dismissSourcePopup()
                        onVideoFullscreenClick(url)
                    },
                )
            }, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ))
        })
    }

    private fun LinearLayout.addLink(url: String, message: ChatRoomMessage, isMine: Boolean, anchorView: View) {
        addView(TextView(context).apply {
            enableMessageLongPress(message, isMine, anchorView)
            text = "🔗 ${runCatching { java.net.URI(url).host }.getOrNull().orEmpty().ifBlank { url }}"
            setTextColor(theme.accent)
            textSize = 13f
            background = roundRect(theme.quoteBackground, boxRadius)
            setPadding(themeControlPx())
            layoutParams = LinearLayout.LayoutParams(dp(230), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(7)
            }
            setOnClickListener {
                dismissSourcePopup()
                onLinkClick(url)
            }
        })
    }

    private fun LinearLayout.addRedPacket(message: ChatRoomMessage, isMine: Boolean, anchorView: View) {
        val packet = message.redPacket?.toRedPacketUiModel()
        val showGestureActions = packet?.needGesture == true && packet.openable && !packet.finished && !isMine
        val action = when {
            packet == null -> ""
            packet.finished -> "已抢完"
            isMine -> "查看详情"
            !packet.openable -> "不可领取"
            showGestureActions -> "猜拳领取"
            packet.needGesture -> "猜拳领取"
            else -> "拆红包"
        }
        val amount = packet
            ?.money
            ?.takeIf { it > 0 }
            ?.let { "$it 积分" }
            .orEmpty()
        val progress = packet?.let { "已领 ${it.got}/${it.count}" }.orEmpty()

        val card = LinearLayout(context).apply {
            enableMessageLongPress(message, isMine, anchorView)
            orientation = LinearLayout.VERTICAL
            background = roundRect(NativeRedPacketBackground, boxRadius)
            setPadding(themeControlPx() + dp(3), themeControlPx() + dp(1), themeControlPx() + dp(3), themeControlPx())
            layoutParams = LinearLayout.LayoutParams(dp(220), ViewGroup.LayoutParams.WRAP_CONTENT)
            alpha = if (message.redPacket?.openable == true || isMine) 1f else 0.72f
            if (!showGestureActions) {
                setOnClickListener {
                    dismissSourcePopup()
                    onRedPacketClick(message)
                }
            }

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(TextView(context).apply {
                    text = "🧧 ${packet?.typeName ?: "红包"}"
                    setTextColor(NativeRedPacketText)
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                })
                if (amount.isNotBlank()) {
                    addView(TextView(context).apply {
                        text = amount
                        setTextColor(NativeRedPacketBackground)
                        textSize = 11f
                        typeface = Typeface.DEFAULT_BOLD
                        includeFontPadding = false
                        background = roundRect(NativeRedPacketText, selectorRadius)
                        setPadding(themeControlPx(), dp(3), themeControlPx(), dp(3))
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        ).apply {
                            marginStart = dp(8)
                        }
                    })
                }
            })

            packet?.message?.takeIf { it.isNotBlank() }?.let { packetMessage ->
                addView(TextView(context).apply {
                    text = packetMessage
                    setTextColor(NativeRedPacketText.withAlpha(232))
                    textSize = 13f
                    maxLines = 2
                    ellipsize = TextUtils.TruncateAt.END
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dp(7)
                    }
                })
            }

            if (progress.isNotBlank() || action.isNotBlank()) {
                addView(TextView(context).apply {
                    text = listOf(progress, action).filter(String::isNotBlank).joinToString(" · ")
                    setTextColor(NativeRedPacketText.withAlpha(210))
                    textSize = 12f
                    typeface = Typeface.DEFAULT_BOLD
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dp(9)
                    }
                })
            }
        }

        if (showGestureActions) {
            addView(FrameLayout(context).apply {
                enableMessageLongPress(message, isMine, anchorView)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                addView(card, FrameLayout.LayoutParams(dp(220), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                })
                addView(redPacketGestureRail(message), FrameLayout.LayoutParams(dp(74), dp(116)).apply {
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    leftMargin = dp(196)
                })
            })
        } else {
            addView(card)
        }
    }

    private fun redPacketGestureRail(message: ChatRoomMessage): FrameLayout =
        FrameLayout(view.context).apply {
            RedPacketGestureOptions.forEachIndexed { index, option ->
                addView(redPacketGestureButton(message, option.value, option.label), FrameLayout.LayoutParams(dp(42), dp(42)).apply {
                    gravity = Gravity.TOP or Gravity.END
                    topMargin = when (index) {
                        0 -> dp(1)
                        1 -> dp(37)
                        else -> dp(73)
                    }
                    marginEnd = if (index == 1) dp(0) else dp(26)
                })
            }
        }

    private fun redPacketGestureButton(message: ChatRoomMessage, value: Int, label: String): ImageButton =
        ImageButton(view.context).apply {
            setImageResource(redPacketGestureDrawable(value))
            background = ColorDrawable(Color.TRANSPARENT)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(0)
            contentDescription = label
            setOnClickListener {
                dismissSourcePopup()
                onRedPacketGestureClick(message, value)
            }
        }

    @DrawableRes
    private fun redPacketGestureDrawable(value: Int): Int =
        when (value) {
            0 -> R.drawable.redpacket_gesture_rock
            1 -> R.drawable.redpacket_gesture_scissors
            else -> R.drawable.redpacket_gesture_paper
        }

    private fun metaSeparator(): View {
        return View(view.context).apply {
            background = roundRect(theme.timeText.withAlpha(42), 1f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1),
            ).apply {
                topMargin = dp(6)
                bottomMargin = dp(3)
            }
        }
    }

    private fun metaTime(time: String): TextView {
        return TextView(view.context).apply {
            text = time
            setTextColor(theme.timeText)
            alpha = 0.76f
            textSize = 10f
            includeFontPadding = false
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
    }

    private fun clientTypeIcon(client: String): ImageView? {
        val icon = clientIconFor(client) ?: return null
        return ImageView(view.context).apply {
            val sourceText = client.toClientSourceText()
            contentDescription = "来源 $sourceText"
            setImageDrawable(icon)
            setColorFilter(theme.clientText)
            alpha = 0.72f
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            layoutParams = LinearLayout.LayoutParams(dp(11), dp(11))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                showSourcePopup(this, sourceText)
            }
        }
    }

    private fun clientIconFor(client: String): Drawable? {
        val key = client.trim().lowercase()
        if (key.isBlank() || key == "system") return null
        val clientName = key
            .substringBefore('/')
            .substringBefore(' ')
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
        @DrawableRes val res = when {
            clientName == "web" -> R.drawable.ic_client_fish
            clientName == "pc" -> R.drawable.ic_client_pc
            clientName == "mobile" -> R.drawable.ic_client_mobile
            clientName == "windows" || clientName == "win" -> R.drawable.ic_client_windows
            clientName == "macos" || clientName == "mac" -> R.drawable.ic_client_apple
            clientName == "linux" -> R.drawable.ic_client_linux
            clientName == "ios" || clientName == "iphone" || clientName == "ipad" -> R.drawable.ic_client_apple
            clientName == "android" -> R.drawable.ic_client_android
            clientName == "idea" -> R.drawable.ic_client_idea
            clientName == "chrome" -> R.drawable.ic_client_chrome
            clientName == "edge" -> R.drawable.ic_client_edge
            clientName == "utools" -> R.drawable.ic_client_utools
            clientName == "vscode" -> R.drawable.ic_client_vscode
            clientName == "python" -> R.drawable.ic_client_python
            clientName == "golang" || clientName == "go" -> R.drawable.ic_client_golang
            clientName == "rust" -> R.drawable.ic_client_rust
            clientName == "harmony" || clientName == "harmonyos" -> R.drawable.ic_client_harmony
            clientName == "cli" -> R.drawable.ic_client_cli
            clientName == "bird" -> R.drawable.ic_client_bird
            clientName == "icenet" -> R.drawable.ic_client_icenet
            clientName == "elvesonline" -> R.drawable.ic_client_other
            clientName == "other" -> R.drawable.ic_client_other
            key.contains("safari") -> R.drawable.ic_client_safari
            key.contains("firefox") -> R.drawable.ic_client_firefox
            key.contains("私聊") || key.contains("chat") -> R.drawable.ic_client_chat
            key.contains("barrager") || key.contains("弹幕") || key.contains("danmu") -> R.drawable.ic_client_danmu
            else -> R.drawable.ic_client_other
        }
        return ContextCompat.getDrawable(view.context, res)
    }

    private fun showSourcePopup(anchor: View, sourceText: String) {
        dismissSourcePopup()
        val content = TextView(view.context).apply {
            text = sourceText
            setTextColor(theme.bubbleText)
            textSize = 12f
            includeFontPadding = false
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            background = roundRect(theme.quoteBackground, fieldRadius).apply {
                setStroke(themeBorderPx(), theme.bubbleBorder)
            }
            setPadding(themeControlPx(), dp(6), themeControlPx(), dp(6))
        }
        sourcePopup = PopupWindow(
            content,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true,
        ).apply {
            isOutsideTouchable = true
            elevation = dp(6).toFloat()
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            showAsDropDown(anchor, -dp(34), dp(4))
        }
    }

    private fun dismissSourcePopup() {
        sourcePopup?.dismiss()
        sourcePopup = null
    }

    private fun String.toClientSourceText(): String {
        val source = trim()
            .removePrefix("client:")
            .removePrefix("Client:")
            .replace('_', ' ')
            .replace('/', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
        return source.ifBlank { "未知来源" }
    }

    private fun TextView.configureBodyText() {
        includeFontPadding = false
        maxLines = Int.MAX_VALUE
        ellipsize = null
        setHorizontallyScrolling(false)
        maxWidth = dp(250)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        setLineSpacing(0f, 1.03f)
    }

    private fun String.visibleTextForWidth(): String {
        val withoutAnchors = HtmlAnchorTextRegex.replace(this) { match ->
            match.groupValues.getOrNull(1).orEmpty()
        }
        val withoutMarkdownLinks = MarkdownVisibleTextRegex.replace(withoutAnchors) { match ->
            match.groupValues.getOrNull(1).orEmpty()
        }
        return withoutMarkdownLinks
            .replace(HtmlTagRegex, "")
            .decodeBasicHtmlEntities()
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun avatarView(message: ChatRoomMessage): FrameLayout {
        return FrameLayout(view.context).apply {
            background = roundRect(theme.clientBackground, fieldRadius).apply {
                setStroke(themeBorderPx(), theme.bubbleBorder)
            }
            clipToOutline = true
            elevation = dp(1).toFloat()
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                topMargin = themeItemPx()
            }
            setOnClickListener {
                dismissSourcePopup()
                onAvatarClick(message.userName)
            }
            setOnLongClickListener {
                dismissSourcePopup()
                onAvatarLongPress(message.userName)
                true
            }
            val label = TextView(context).apply {
                text = message.userName.ifBlank { message.displayName }.trim().take(1).ifBlank { "鱼" }
                setTextColor(theme.accent)
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }
            addView(label, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ))
            if (message.userAvatarURL.isNotBlank()) {
                val avatarSizePx = dp(38)
                val image = SharedAvatarImageView(
                    context = context,
                    store = avatarDrawableStore,
                    imageLoaderProvider = imageLoaderProvider,
                    avatarSizePx = avatarSizePx,
                ).apply {
                    visibility = View.INVISIBLE
                    setAvatarUrl(message.userAvatarURL)
                    onAvailabilityChanged = { available ->
                        visibility = if (available) View.VISIBLE else View.INVISIBLE
                        label.visibility = if (available) View.GONE else View.VISIBLE
                    }
                }
                addView(image, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ))
            }
        }
    }

    private fun repeatRow(
        message: ChatRoomMessage,
        stack: RepeatStackInfo,
        isMine: Boolean,
    ): LinearLayout {
        val accentColor = theme.accent
        return LinearLayout(view.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (isMine) Gravity.END or Gravity.CENTER_VERTICAL
                      else Gravity.START or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(3)
            }
            setPadding((themeItemPx() * 0.5f).roundToInt(), 0, (themeItemPx() * 0.5f).roundToInt(), 0)

            // Avatar stack — keep original order from repeat messages.
            val avatars = stack.participantAvatars
            val maxAvatarsPerRow = 12
            val avatarRows = avatars.chunked(maxAvatarsPerRow)

            val avatarBlock = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { rightMargin = dp(4) }
            }
            avatarRows.forEach { rowAvatars ->
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
                rowAvatars.forEachIndexed { i, url ->
                    row.addView(repeatAvatar(url, i))
                }
                avatarBlock.addView(row)
            }
            addView(avatarBlock)

            // "+N"
            addView(TextView(context).apply {
                text = "+${stack.count}"
                setTextColor(accentColor)
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { rightMargin = dp(6) }
            })

            // "+1" button
            addView(repeatButton(message, accentColor))
        }
    }

    private fun repeatAvatar(url: String, index: Int): View {
        return FrameLayout(view.context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(20), dp(20)).apply {
                marginStart = if (index > 0) dp(-6) else 0
            }
            background = roundRect(0x00000000, fieldRadius).apply {
                setStroke(themeBorderPx(), theme.bubbleBorder)
            }
            clipToOutline = true
            if (url.isNotBlank()) {
                addView(
                    SharedAvatarImageView(
                        context = context,
                        store = avatarDrawableStore,
                        imageLoaderProvider = imageLoaderProvider,
                        avatarSizePx = dp(20),
                    ).apply {
                        setAvatarUrl(url)
                    },
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
        }
    }

    private fun repeatButton(message: ChatRoomMessage, accentColor: Int): TextView {
        return TextView(view.context).apply {
            text = "+1"
            setTextColor(accentColor)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(boxRadius)
                setColor(accentColor and 0x33FFFFFF)
                setStroke(themeBorderPx(), accentColor and 0x66FFFFFF)
            }
            setPadding(themeControlPx(), dp(2), themeControlPx(), dp(2))
            setOnClickListener {
                dismissSourcePopup()
                onRepeatClick(message)
            }
        }
    }

    private fun View.enableMessageLongPress(
        message: ChatRoomMessage,
        isMine: Boolean,
        anchorView: View = this,
    ) {
        isLongClickable = true
        setOnLongClickListener {
            dismissSourcePopup()
            onLongPress(anchorView.messageActionAnchor(message, isMine))
            true
        }
    }

    private fun View.messageActionAnchor(message: ChatRoomMessage, isMine: Boolean): MessageActionAnchor {
        val location = IntArray(2)
        getLocationInWindow(location)
        return MessageActionAnchor(
            message = message,
            rectInWindow = android.graphics.Rect(
                location[0],
                location[1],
                location[0] + width,
                location[1] + height,
            ),
            isMine = isMine,
        )
    }

    private fun roundRect(color: Int, radiusDp: Float): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp)
        }

    private fun Int.withAlpha(alpha: Int): Int =
        (alpha.coerceIn(0, 255) shl 24) or (this and 0x00FFFFFF)

    private fun dp(value: Int): Int = (value * view.resources.displayMetrics.density).toInt()

    private fun dp(value: Float): Float = value * view.resources.displayMetrics.density
}

private fun View.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

private fun String.isAnimatedImageUrl(): Boolean {
    val normalized = substringBefore('#').lowercase()
    val path = normalized.substringBefore('?')
    return path.endsWith(".gif") || normalized.contains("image/gif")
}

internal class SharedAvatarDrawableStore {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val entries = LinkedHashMap<String, Entry>()

    fun subscribe(
        url: String,
        view: SharedAvatarImageView,
        imageLoader: ImageLoader,
        avatarSizePx: Int,
    ) {
        if (url.isBlank()) {
            view.onSharedAvatarAvailabilityChanged(false)
            return
        }
        val entry = entries.getOrPut(url) { Entry(url) }
        entry.subscribers.add(view)
        view.onSharedAvatarAvailabilityChanged(entry.drawable != null)
        entry.drawable?.let {
            if (entry.subscribers.isNotEmpty()) {
                (it as? Animatable)?.start()
            }
            view.postInvalidateOnAnimation()
        }
        if (entry.drawable == null && !entry.loading) {
            load(entry, view, imageLoader, avatarSizePx)
        }
    }

    fun unsubscribe(url: String, view: SharedAvatarImageView) {
        val entry = entries[url] ?: return
        entry.subscribers.remove(view)
        if (entry.subscribers.isEmpty()) {
            (entry.drawable as? Animatable)?.stop()
        }
    }

    fun drawableFor(url: String): Drawable? = entries[url]?.drawable

    fun retainAvatarUrls(items: List<ChatListItem>) {
        val activeUrls = items
            .takeLast(220)
            .flatMap { item ->
                buildList {
                    item.message.userAvatarURL.takeIf(String::isNotBlank)?.let(::add)
                    item.repeatStack?.participantAvatars
                        ?.filter(String::isNotBlank)
                        ?.let(::addAll)
                }
            }
            .toHashSet()
        val iterator = entries.iterator()
        while (iterator.hasNext()) {
            val (_, entry) = iterator.next()
            if (entry.url !in activeUrls && entry.subscribers.isEmpty()) {
                (entry.drawable as? Animatable)?.stop()
                entry.drawable?.callback = null
                iterator.remove()
            }
        }
    }

    private fun load(
        entry: Entry,
        view: SharedAvatarImageView,
        imageLoader: ImageLoader,
        avatarSizePx: Int,
    ) {
        entry.loading = true
        val context = view.context.applicationContext
        val resources = view.resources
        val request = ImageRequest.Builder(context)
            .data(entry.url)
            .size(Size(avatarSizePx, avatarSizePx))
            .precision(Precision.EXACT)
            .target(
                onSuccess = { result ->
                    if (entries[entry.url] === entry) {
                        val drawable = result.asDrawable(resources)
                        entry.loading = false
                        entry.drawable?.callback = null
                        entry.drawable = drawable
                        drawable.callback = entry.callback
                        if (entry.subscribers.isNotEmpty()) {
                            (drawable as? Animatable)?.start()
                        }
                        entry.subscribers.forEach { subscriber ->
                            subscriber.onSharedAvatarAvailabilityChanged(true)
                            subscriber.postInvalidateOnAnimation()
                        }
                    }
                },
                onError = {
                    if (entries[entry.url] === entry) {
                        entry.loading = false
                        entry.subscribers.forEach { subscriber ->
                            subscriber.onSharedAvatarAvailabilityChanged(false)
                        }
                    }
                },
            )
            .build()
        imageLoader.enqueue(request)
    }

    private inner class Entry(val url: String) {
        var loading: Boolean = false
        var drawable: Drawable? = null
        val subscribers = LinkedHashSet<SharedAvatarImageView>()
        val callback = object : Drawable.Callback {
            override fun invalidateDrawable(who: Drawable) {
                subscribers.forEach { it.postInvalidateOnAnimation() }
            }

            override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
                mainHandler.postAtTime(what, `when`)
            }

            override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                mainHandler.removeCallbacks(what)
            }
        }
    }
}

internal class SharedAvatarImageView(
    context: Context,
    private val store: SharedAvatarDrawableStore,
    private val imageLoaderProvider: () -> ImageLoader,
    private val avatarSizePx: Int,
) : View(context) {
    private val drawRect = Rect()
    private var avatarUrl: String = ""
    var onAvailabilityChanged: (Boolean) -> Unit = {}

    override fun hasOverlappingRendering(): Boolean = false

    fun setAvatarUrl(url: String) {
        if (avatarUrl == url) return
        release()
        avatarUrl = url
        if (isAttachedToWindow && isShown) {
            subscribe()
        }
    }

    fun release() {
        if (avatarUrl.isNotBlank()) {
            store.unsubscribe(avatarUrl, this)
        }
    }

    fun resumeAvatar() {
        if (isAttachedToWindow && isShown && avatarUrl.isNotBlank()) {
            subscribe()
        }
    }

    fun onSharedAvatarAvailabilityChanged(available: Boolean) {
        onAvailabilityChanged(available)
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        subscribe()
    }

    override fun onDetachedFromWindow() {
        release()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible) {
            subscribe()
        } else {
            release()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val drawable = store.drawableFor(avatarUrl) ?: return
        val drawableWidth = drawable.intrinsicWidth.takeIf { it > 0 } ?: width
        val drawableHeight = drawable.intrinsicHeight.takeIf { it > 0 } ?: height
        val scale = maxOf(
            width.toFloat() / drawableWidth.toFloat(),
            height.toFloat() / drawableHeight.toFloat(),
        )
        val scaledWidth = (drawableWidth * scale).toInt()
        val scaledHeight = (drawableHeight * scale).toInt()
        val left = (width - scaledWidth) / 2
        val top = (height - scaledHeight) / 2
        drawRect.set(left, top, left + scaledWidth, top + scaledHeight)
        drawable.bounds = drawRect
        drawable.draw(canvas)
    }

    private fun subscribe() {
        store.subscribe(
            url = avatarUrl,
            view = this,
            imageLoader = imageLoaderProvider(),
            avatarSizePx = avatarSizePx,
        )
    }
}

internal class SharedMessageDrawableStore {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val entries = LinkedHashMap<String, Entry>()

    fun subscribe(
        url: String,
        decodeWidthPx: Int,
        decodeHeightPx: Int,
        view: SharedMessageImageView,
        imageLoader: ImageLoader,
    ) {
        if (url.isBlank()) {
            view.onSharedMessageDrawableError()
            return
        }
        val key = key(url, decodeWidthPx, decodeHeightPx)
        val entry = entries.getOrPut(key) { Entry(key, url) }
        entry.subscribers.add(view)
        entry.drawable?.let { drawable ->
            if (entry.subscribers.isNotEmpty()) {
                (drawable as? Animatable)?.start()
            }
            view.onSharedMessageDrawableReady(drawable)
            view.postInvalidateOnAnimation()
        }
        if (entry.drawable == null && !entry.loading) {
            load(entry, view, imageLoader, decodeWidthPx, decodeHeightPx)
        }
    }

    fun unsubscribe(
        url: String,
        decodeWidthPx: Int,
        decodeHeightPx: Int,
        view: SharedMessageImageView,
    ) {
        val entry = entries[key(url, decodeWidthPx, decodeHeightPx)] ?: return
        entry.subscribers.remove(view)
        if (entry.subscribers.isEmpty()) {
            (entry.drawable as? Animatable)?.stop()
        }
    }

    fun drawableFor(url: String, decodeWidthPx: Int, decodeHeightPx: Int): Drawable? =
        entries[key(url, decodeWidthPx, decodeHeightPx)]?.drawable

    fun retainMediaUrls(items: List<ChatListItem>) {
        val activeUrls = items
            .takeLast(180)
            .flatMap { it.message.allRenderableImageUrls() }
            .filter { it.isAnimatedImageUrl() }
            .toHashSet()
        val iterator = entries.iterator()
        while (iterator.hasNext()) {
            val (_, entry) = iterator.next()
            if (entry.url !in activeUrls && entry.subscribers.isEmpty()) {
                (entry.drawable as? Animatable)?.stop()
                entry.drawable?.callback = null
                iterator.remove()
            }
        }
    }

    private fun load(
        entry: Entry,
        view: SharedMessageImageView,
        imageLoader: ImageLoader,
        decodeWidthPx: Int,
        decodeHeightPx: Int,
    ) {
        entry.loading = true
        val context = view.context.applicationContext
        val resources = view.resources
        val request = ImageRequest.Builder(context)
            .data(entry.url)
            .size(Size(decodeWidthPx, decodeHeightPx))
            .precision(Precision.EXACT)
            .target(
                onSuccess = { result ->
                    if (entries[entry.key] === entry) {
                        val drawable = result.asDrawable(resources)
                        entry.loading = false
                        entry.drawable?.callback = null
                        entry.drawable = drawable
                        drawable.callback = entry.callback
                        if (entry.subscribers.isNotEmpty()) {
                            (drawable as? Animatable)?.start()
                        }
                        entry.subscribers.forEach { subscriber ->
                            subscriber.onSharedMessageDrawableReady(drawable)
                            subscriber.postInvalidateOnAnimation()
                        }
                    }
                },
                onError = {
                    if (entries[entry.key] === entry) {
                        entry.loading = false
                        entry.subscribers.forEach { subscriber ->
                            subscriber.onSharedMessageDrawableError()
                        }
                    }
                },
            )
            .build()
        imageLoader.enqueue(request)
    }

    private fun key(url: String, decodeWidthPx: Int, decodeHeightPx: Int): String =
        "$decodeWidthPx:$decodeHeightPx:$url"

    private inner class Entry(
        val key: String,
        val url: String,
    ) {
        var loading: Boolean = false
        var drawable: Drawable? = null
        val subscribers = LinkedHashSet<SharedMessageImageView>()
        val callback = object : Drawable.Callback {
            override fun invalidateDrawable(who: Drawable) {
                subscribers.forEach { it.postInvalidateOnAnimation() }
            }

            override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
                mainHandler.postAtTime(what, `when`)
            }

            override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                mainHandler.removeCallbacks(what)
            }
        }
    }
}

internal class SharedMessageImageView(
    context: Context,
    private val store: SharedMessageDrawableStore,
    private val imageLoaderProvider: () -> ImageLoader,
    private val url: String,
    private val decodeWidthPx: Int,
    private val decodeHeightPx: Int,
    private val onDrawableReady: (Drawable) -> Unit,
    private val onError: () -> Unit,
) : View(context) {
    private val drawRect = Rect()
    private var ready = false

    override fun hasOverlappingRendering(): Boolean = false

    fun release() {
        store.unsubscribe(url, decodeWidthPx, decodeHeightPx, this)
    }

    fun resumeMedia() {
        if (isAttachedToWindow && isShown) {
            subscribe()
        }
    }

    fun onSharedMessageDrawableReady(drawable: Drawable) {
        if (!ready) {
            ready = true
            onDrawableReady(drawable)
        }
        invalidate()
    }

    fun onSharedMessageDrawableError() {
        ready = false
        onError()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        subscribe()
    }

    override fun onDetachedFromWindow() {
        release()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible) {
            subscribe()
        } else {
            release()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val drawable = store.drawableFor(url, decodeWidthPx, decodeHeightPx) ?: return
        val drawableWidth = drawable.intrinsicWidth.takeIf { it > 0 } ?: width
        val drawableHeight = drawable.intrinsicHeight.takeIf { it > 0 } ?: height
        val scale = minOf(
            width.toFloat() / drawableWidth.toFloat(),
            height.toFloat() / drawableHeight.toFloat(),
        )
        val scaledWidth = (drawableWidth * scale).toInt()
        val scaledHeight = (drawableHeight * scale).toInt()
        val left = (width - scaledWidth) / 2
        val top = (height - scaledHeight) / 2
        drawRect.set(left, top, left + scaledWidth, top + scaledHeight)
        drawable.bounds = drawRect
        drawable.draw(canvas)
    }

    private fun subscribe() {
        store.subscribe(
            url = url,
            decodeWidthPx = decodeWidthPx,
            decodeHeightPx = decodeHeightPx,
            view = this,
            imageLoader = imageLoaderProvider(),
        )
    }
}

private class MessageMediaImageView(context: Context) : ImageView(context) {
    private var animatedDrawable: Animatable? = null

    override fun hasOverlappingRendering(): Boolean = false

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        resumeAnimationIfVisible()
    }

    override fun onDetachedFromWindow() {
        pauseAnimation()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible) resumeAnimationIfVisible() else pauseAnimation()
    }

    fun setMediaDrawable(drawable: Drawable) {
        animatedDrawable?.stop()
        setImageDrawable(drawable)
        animatedDrawable = drawable as? Animatable
        setLayerType(View.LAYER_TYPE_NONE, null)
        if (animatedDrawable != null) {
            resumeAnimationIfVisible()
        }
    }

    fun clearMediaDrawable() {
        animatedDrawable?.stop()
        animatedDrawable = null
        setImageDrawable(null)
        setLayerType(View.LAYER_TYPE_NONE, null)
    }

    fun pauseAnimation() {
        animatedDrawable?.stop()
    }

    fun resumeAnimationIfVisible() {
        if (isAttachedToWindow && isShown) {
            animatedDrawable?.start()
        }
    }
}

private fun View.stopAnimatedDrawables() {
    when (this) {
        is SharedMessageImageView -> {
            release()
        }
        is SharedAvatarImageView -> {
            release()
        }
        is MessageMediaImageView -> {
            clearMediaDrawable()
        }
        is ImageView -> {
            (drawable as? Animatable)?.stop()
            setImageDrawable(null)
        }
        is ViewGroup -> {
            for (index in 0 until childCount) {
                getChildAt(index).stopAnimatedDrawables()
            }
        }
    }
}

private fun View.pauseAnimatedDrawables() {
    when (this) {
        is SharedMessageImageView -> {
            release()
        }
        is SharedAvatarImageView -> {
            release()
        }
        is MessageMediaImageView -> {
            pauseAnimation()
        }
        is ImageView -> {
            (drawable as? Animatable)?.stop()
        }
        is ViewGroup -> {
            for (index in 0 until childCount) {
                getChildAt(index).pauseAnimatedDrawables()
            }
        }
    }
}

private fun View.resumeAnimatedDrawables() {
    when (this) {
        is SharedMessageImageView -> {
            resumeMedia()
        }
        is SharedAvatarImageView -> {
            resumeAvatar()
        }
        is MessageMediaImageView -> {
            resumeAnimationIfVisible()
        }
        is ImageView -> {
            if (isAttachedToWindow && isShown) {
                (drawable as? Animatable)?.start()
            }
        }
        is ViewGroup -> {
            for (index in 0 until childCount) {
                getChildAt(index).resumeAnimatedDrawables()
            }
        }
    }
}

private fun View.stopVideoViews() {
    when (this) {
        is ChatVideoCard -> {
            releasePlayer()
        }
        is ViewGroup -> {
            for (index in 0 until childCount) {
                getChildAt(index).stopVideoViews()
            }
        }
    }
}

internal fun android.content.Context.openSystemVideo(url: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(url), "video/*")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "没有可用播放器", Toast.LENGTH_SHORT).show()
    } catch (_: Throwable) {
        Toast.makeText(this, "无法打开视频", Toast.LENGTH_SHORT).show()
    }
}




