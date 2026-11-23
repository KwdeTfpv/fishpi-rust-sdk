package dev.fishpi.mobile.shared.message.native

import dev.fishpi.mobile.shared.message.RepeatStackInfo

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.Typeface
import android.graphics.drawable.Animatable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
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
import androidx.media3.common.Player
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
import dev.fishpi.mobile.shared.message.MessageUpdateKind
import dev.fishpi.mobile.shared.message.MessageUpdatePlanner
import dev.fishpi.mobile.shared.message.stableNativeKey
import dev.fishpi.mobile.shared.message.allRenderableImageUrls
import dev.fishpi.mobile.R
import dev.fishpi.mobile.chatui.MarkwonChatRenderer
import dev.fishpi.mobile.data.ChatContentKind
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.ui.overlay.PlaybackSession
import dev.fishpi.mobile.utils.adaptiveImageBoxSize
import dev.fishpi.mobile.utils.cleanImageSplitTextSegment
import dev.fishpi.mobile.utils.WhitespaceRegex
import dev.fishpi.mobile.utils.MarkdownMediaType
import dev.fishpi.mobile.utils.toFishPiGeneratedBadgeOrNull
import kotlin.math.roundToInt
import dev.fishpi.mobile.utils.toMeasuredMessageText

internal class NativeMessageAdapter(
    private var theme: NativeMessageTheme,
    private var markdownRenderer: MarkwonChatRenderer,
    var imageLoader: ImageLoader,
    var selfUsername: String,
    var showAvatars: Boolean,
    private val onImageClick: (images: List<String>, index: Int) -> Unit,
    private val onLinkClick: (String) -> Unit,
    private val onLongPress: (MessageActionAnchor) -> Unit,
    private val onAvatarClick: (String) -> Unit,
    private val onAvatarLongPress: (String) -> Unit,
    private val onRedPacketClick: (ChatRoomMessage) -> Unit,
    private val onRedPacketGestureClick: (ChatRoomMessage, Int) -> Unit,
    private val onReactionClick: (ChatRoomMessage, String) -> Unit,
    private val onRepeatClick: (ChatRoomMessage) -> Unit,
    private val onVideoFullscreenClick: (String, PlaybackSession) -> Unit,
) : RecyclerView.Adapter<NativeMessageViewHolder>() {
    var currentItems: List<ChatListItem> = emptyList()
        private set

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

    private val avatarDrawableStore = SharedAvatarDrawableStore()
    private val messageDrawableStore = SharedMessageDrawableStore()

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
                val key = items[index].stableNativeKey()
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
        val rowPad = (theme.spacingMicroDp * parent.resources.displayMetrics.density).toInt()
        return NativeMessageViewHolder(
            view = LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, rowPad, 0, rowPad)
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            },
            themeProvider = { theme },
            markdownRendererProvider = { markdownRenderer },
            imageLoaderProvider = { imageLoader },
            avatarDrawableStore = avatarDrawableStore,
            messageDrawableStore = messageDrawableStore,
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
        val key = currentItems[position].stableNativeKey()
        if (!pendingEntranceKeys.remove(key)) return
        playedEntranceKeys.add(key)
        holder.playEntranceAnimation()
        val appearance = currentItems[position].message.appearance
        if (appearance?.bubbleId == NightSakuraId || appearance?.frameId == NightSakuraId) {
            holder.playSakuraArrival()
        }
    }

    override fun onViewDetachedFromWindow(holder: NativeMessageViewHolder) {
        holder.pauseAnimations()
        super.onViewDetachedFromWindow(holder)
    }
}

internal const val NativeRedPacketBackground = 0xFFE94E2F.toInt()
internal const val NativeRedPacketText = 0xFFFFF7EC.toInt()
private const val PLAYED_ENTRANCE_CAP = 200
private const val NightSakuraId = ChatAppearanceSupport.NightSakura
private const val ENTRANCE_DURATION_MS = 260L
private const val ENTRANCE_RISE_DP = 16f
private const val ENTRANCE_START_SCALE = 0.92f
private val entranceInterpolator: android.view.animation.Interpolator =
    android.view.animation.PathInterpolator(0f, 0f, 0.2f, 1f)
private val ClientNameNonAlnumRegex = Regex("[^a-z0-9]")

internal data class NativeMusicCard(
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
        return oldItems[oldItemPosition].stableNativeKey() ==
            newItems[newItemPosition].stableNativeKey()
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition] == newItems[newItemPosition]
    }
}



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
    private var session: PlaybackSession? = null
    private var playerView: PlayerView? = null
    private var controlsVisible = true
    private var videoUrl: String = ""
    private var fullscreenCallback: (PlaybackSession) -> Unit = {}
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

    fun bind(url: String, onFullscreen: (PlaybackSession) -> Unit) {
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
        error.setOnClickListener {
            startPlayer(playImmediately = true)
        }
        fullscreen.setOnClickListener {
            startPlayer(playImmediately = false, restart = false)
            val activeSession = session ?: return@setOnClickListener
            activeSession.enterFullscreen()
            fullscreenCallback(activeSession)
        }
        openExternal.setOnClickListener { context.openSystemVideo(videoUrl) }
        setOnClickListener {
            val exo = session?.player
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

    private fun ensureSession(): PlaybackSession {
        val current = session
        if (current != null) return current
        return PlaybackSession(context).also { active ->
            active.setSource(videoUrl)
            active.player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    loading.visibility = if (playbackState == Player.STATE_BUFFERING) VISIBLE else GONE
                    if (playbackState == Player.STATE_READY && active.player.isPlaying) {
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
            session = active
        }
    }

    private fun startPlayer(playImmediately: Boolean, restart: Boolean = true) {
        if (videoUrl.isBlank()) return
        val activeSession = ensureSession()
        val activePlayer = activeSession.player
        if (activePlayer.playerError != null) {
            error.visibility = GONE
            loading.visibility = VISIBLE
            activeSession.retry()
            return
        }
        if (playerView == null) {
            error.visibility = GONE
            loading.visibility = VISIBLE
            val activePlayerView = PlayerView(context).apply {
                useController = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setShutterBackgroundColor(0xFF05070AL.toInt())
                setBackgroundColor(0xFF05070AL.toInt())
            }
            playerView = activePlayerView
            activeSession.bindInline(activePlayerView)
            addView(
                activePlayerView,
                0,
                LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            placeholder.visibility = GONE
            activeSession.prepare(videoUrl, playImmediately)
            return
        }
        if (restart) {
            activeSession.prepare(videoUrl, playImmediately)
        } else if (activePlayer.mediaItemCount == 0) {
            activeSession.prepare(videoUrl, playImmediately)
        } else if (playImmediately) {
            activePlayer.play()
        }
    }

    fun releasePlayer() {
        removeCallbacks(hideControlsRunnable)
        val activeSession = session
        val activeView = playerView
        if (activeView != null) {
            activeSession?.unbindInline(activeView)
            removeView(activeView)
        }
        playerView = null
        session = null
        activeSession?.releaseIfIdle()
    }

    private fun setControlsVisible(visible: Boolean) {
        controlsVisible = visible
        val visibility = if (visible) VISIBLE else GONE
        openExternal.visibility = visibility
        fullscreen.visibility = visibility
        if (session?.player?.isPlaying != true && error.visibility != VISIBLE) {
            play.visibility = VISIBLE
        } else if (session?.player?.isPlaying == true) {
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

internal class NativeMessageViewHolder(
    private val view: LinearLayout,
    private val themeProvider: () -> NativeMessageTheme,
    private val markdownRendererProvider: () -> MarkwonChatRenderer,
    private val imageLoaderProvider: () -> ImageLoader,
    private val avatarDrawableStore: SharedAvatarDrawableStore,
    private val messageDrawableStore: SharedMessageDrawableStore,
    private val onImageClick: (images: List<String>, index: Int) -> Unit,
    private val onLinkClick: (String) -> Unit,
    private val onLongPress: (MessageActionAnchor) -> Unit,
    private val onAvatarClick: (String) -> Unit,
    private val onAvatarLongPress: (String) -> Unit,
    private val onRedPacketClick: (ChatRoomMessage) -> Unit,
    private val onRedPacketGestureClick: (ChatRoomMessage, Int) -> Unit,
    private val onReactionClick: (ChatRoomMessage, String) -> Unit,
    private val onRepeatClick: (ChatRoomMessage) -> Unit,
    private val onVideoFullscreenClick: (String, PlaybackSession) -> Unit,
) : RecyclerView.ViewHolder(view) {
    private var sourcePopup: PopupWindow? = null
    private var boundItem: ChatListItem? = null
    private var boundMine = false
    private var boundAvatars = false
    private var boundTheme: NativeMessageTheme? = null
    private var boundBodyColor: Int = 0
    private var rowView: NativeMessageRowView? = null
    private var drawableCacheTheme: NativeMessageTheme? = null
    private val drawableCache = HashMap<Long, GradientDrawable>(24)
    private val bubbleBackgroundCache = HashMap<Int, GradientDrawable>(4)
    private val theme: NativeMessageTheme
        get() = themeProvider()
    private val bodyRenderer = MessageBodyRenderer(
        context = view.context,
        markdownRendererProvider = { markdownRendererProvider() },
        styleText = { textView -> textView.configureBodyText() },
        attachLongPress = { textView, message, isMine, anchorView ->
            textView.enableMessageLongPress(message, isMine, anchorView)
        },
    )
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
        stopSakuraArrival()
        bodyRenderer.cancel()
        boundItem = null
        rowView?.recycle()
        dismissSourcePopup()
        view.animate().cancel()
        resetEntranceTransforms()
        view.stopAnimatedDrawables()
        view.stopVideoViews()
        view.removeAllViews()
    }

    fun pauseAnimations() {
        view.pauseAnimatedDrawables()
        stopSakuraArrival()
    }

    fun resumeAnimations() {
        view.resumeAnimatedDrawables()
    }

    fun playSakuraArrival() {
        rowView?.playSakuraArrival()
    }

    fun stopSakuraArrival() {
        rowView?.stopSakuraArrival()
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
        val currentTheme = theme
        if (
            boundItem === item &&
            boundMine == isMine &&
            boundAvatars == showAvatars &&
            boundTheme === currentTheme &&
            view.childCount > 0
        ) {
            return
        }
        val updateKind = MessageUpdatePlanner.plan(
            previous = boundItem,
            next = item,
            sameContext = boundMine == isMine && boundAvatars == showAvatars && boundTheme === currentTheme,
        )
        if (updateKind != MessageUpdateKind.Full) {
            when (updateKind) {
                MessageUpdateKind.AppearanceOnly -> applyAppearanceOnly(item, isMine)
                MessageUpdateKind.ReactionsOnly -> applyReactionsOnly(item, isMine)
                MessageUpdateKind.AvatarOnly -> applyAvatarOnly(item, isMine)
                MessageUpdateKind.Full -> Unit
            }
            boundItem = item
            boundMine = isMine
            boundAvatars = showAvatars
            boundTheme = currentTheme
            return
        }
        boundItem = item
        boundMine = isMine
        boundAvatars = showAvatars
        boundTheme = currentTheme
        val message = item.message
        bodyRenderer.cancel()
        dismissSourcePopup()
        view.stopAnimatedDrawables()
        view.removeAllViews()

        item.separator?.let { separator ->
            view.addView(serviceText(separator))
        }

        if (message.contentKind == ChatContentKind.System) {
            view.addView(serviceText(message.content))
            return
        }

        val rowView = this.rowView ?: buildRowView().also { this.rowView = it }
        rowView.bindLayout(isMine, showAvatars)
        if (showAvatars) rowView.bindAvatar(avatarState(message, isMine))
        rowView.bindAuthor(message.authorLabel)
        rowView.content.removeAllViews()
        bindBubble(rowView.bubble, message, item, isMine)
        rowView.attachBubble()
        rowView.bindMeta(messageMetaLine(item, isMine))
        rowView.bindReactions(
            if (message.reactionSummary.isEmpty()) {
                null
            } else {
                reactionRow(message, isMine, rowView.bubble)
            },
        )
        rowView.bindRepeat(item.repeatStack?.let { stack -> repeatRow(message, stack, isMine) })
        view.addView(rowView)
    }

    private fun buildRowView(): NativeMessageRowView = NativeMessageRowView(
        context = view.context,
        themeProvider = themeProvider,
        avatarStore = avatarDrawableStore,
        imageLoaderProvider = imageLoaderProvider,
        onAvatarClick = { userName ->
            dismissSourcePopup()
            onAvatarClick(userName)
        },
        onAvatarLongPress = { userName ->
            dismissSourcePopup()
            onAvatarLongPress(userName)
        },
    )

    private fun serviceText(text: String): TextView {
        return TextView(view.context).apply {
            this.text = text
            setTextColor(NativeRedPacketText)
            textSize = theme.serviceTextSizeSp
            gravity = Gravity.CENTER
            background = cachedRoundRect(theme.serviceBackground, selectorRadius)
            setPadding(themeControlPx(), themePx(theme.spacingMicroDp), themeControlPx(), themePx(theme.spacingMicroDp))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = themePx(theme.spacingMicroDp)
                bottomMargin = themePx(theme.spacingMicroDp)
            }
        }
    }

    private fun resetDrawableCache() {
        val current = theme
        if (drawableCacheTheme !== current) {
            drawableCacheTheme = current
            drawableCache.clear()
            bubbleBackgroundCache.clear()
        }
    }

    private fun bubbleBackground(color: Int): GradientDrawable {
        resetDrawableCache()
        bubbleBackgroundCache[color]?.let { return it }
        val created = roundRect(color, boxRadius).apply {
            setStroke(themeBorderPx(), theme.bubbleBorder)
        }
        bubbleBackgroundCache[color] = created
        return created
    }

    private fun cachedRoundRect(color: Int, radiusDp: Float): GradientDrawable {
        resetDrawableCache()
        val key = (color.toLong() shl 32) or (radiusDp.toRawBits().toLong() and 0xFFFFFFFFL)
        drawableCache[key]?.let { return it }
        val created = GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp)
        }
        drawableCache[key] = created
        return created
    }


    private fun applyReactionsOnly(item: ChatListItem, isMine: Boolean) {
        val rowView = this.rowView ?: return
        val message = item.message
        rowView.bindReactions(
            if (message.reactionSummary.isEmpty()) {
                null
            } else {
                reactionRow(message, isMine, rowView.bubble)
            },
        )
    }

    private fun applyAvatarOnly(item: ChatListItem, mine: Boolean) {
        val rowView = this.rowView ?: return
        if (boundAvatars) rowView.bindAvatar(avatarState(item.message, mine))
    }

    private fun applyAppearanceOnly(item: ChatListItem, isMine: Boolean) {
        val rowView = this.rowView ?: return
        val message = item.message
        val standaloneCard = item.isMediaOnlyMessage() || message.redPacket != null
        val darkChrome = Color.luminance(theme.bubbleText) > 0.5f
        val nextColor = rowView.bubble.bindMessage(
            message = message,
            isMine = isMine,
            darkChrome = darkChrome,
            standaloneCard = standaloneCard,
            defaultBackground = {
                bubbleBackground(if (isMine) theme.outgoingBubble else theme.incomingBubble)
            },
            imageLoader = imageLoaderProvider,
        )
        if (nextColor != boundBodyColor) {
            bodyRenderer.applyTextColor(rowView.content, boundBodyColor, nextColor)
            boundBodyColor = nextColor
        }
        if (boundAvatars) rowView.bindAvatar(avatarState(message, isMine))
    }

    private fun bindBubble(
        frame: NativeChatBubbleView,
        message: ChatRoomMessage,
        item: ChatListItem,
        isMine: Boolean,
    ) {
        val standaloneCard = item.isMediaOnlyMessage() || message.redPacket != null
        val darkChrome = Color.luminance(theme.bubbleText) > 0.5f
        frame.enableMessageLongPress(message, isMine, frame)
        val bodyColor = frame.bindMessage(
            message = message,
            isMine = isMine,
            darkChrome = darkChrome,
            standaloneCard = standaloneCard,
            defaultBackground = {
                bubbleBackground(if (isMine) theme.outgoingBubble else theme.incomingBubble)
            },
            imageLoader = imageLoaderProvider,
        )
        boundBodyColor = bodyColor
        when {
            item.revoked -> frame.content.addText("[该消息已撤回]", theme.bodyTextSizeSp, bodyColor)
            message.contentKind == ChatContentKind.Barrage -> frame.content.addText(message.content, theme.bodyTextSizeSp, bodyColor)
            message.redPacket != null -> frame.content.addRedPacket(message, isMine, frame)
            else -> {
                val markdown = item.renderHints.markdownContent
                if (markdown.isNotBlank()) {
                    frame.content.addMarkdownContent(message, item, isMine, frame, bodyColor)
                } else if (item.renderHints.plainFallback.isNotBlank()) {
                    frame.content.addText(item.renderHints.plainFallback, theme.bodyTextSizeSp, bodyColor)
                }
                item.renderHints.videoLinks.forEach { url -> frame.content.addVideo(url, message, isMine, frame) }
                item.renderHints.previewLinks.take(1).forEach { url ->
                    frame.content.addLink(url, message, isMine, frame)
                }
            }
        }
    }



    private fun ChatListItem.isMediaOnlyMessage(): Boolean = renderHints.isMediaOnly

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
                topMargin = themePx(theme.metaTopMarginDp)
                leftMargin = themePx(theme.spacingMicroDp)
                rightMargin = themePx(theme.spacingMicroDp)
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
                        textSize = theme.metaTimeTextSizeSp
                        includeFontPadding = false
                        setPadding(themePx(theme.spacingMicroDp), 0, themePx(theme.spacingMicroDp), 0)
                    })
                }
                addView(icon)
            }
        }
    }

    private fun LinearLayout.addText(text: CharSequence, size: Float, color: Int = theme.bubbleText) {
        bodyRenderer.renderPlain(this, text, size, color)
    }

    private fun LinearLayout.addMarkdownContent(message: ChatRoomMessage, item: ChatListItem, isMine: Boolean, anchorView: View, bodyColor: Int) {
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
        val tokens = item.renderHints.imageTokens.take(4)
        if (tokens.isEmpty()) {
            addMarkdownText(message, item, content, anchorView, isMine, bodyColor)
            return
        }
        var cursor = 0
        tokens.forEachIndexed { index, token ->
            val before = content.substring(cursor, token.start).cleanImageSplitTextSegment()
            if (before.isNotBlank()) {
                addMarkdownText(message, item, before, anchorView, isMine, bodyColor, ":text$index")
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
            addMarkdownText(message, item, tail, anchorView, isMine, bodyColor, ":tail")
        }
    }

    private fun LinearLayout.addMarkdownText(
        message: ChatRoomMessage,
        item: ChatListItem,
        markdown: String,
        anchorView: View,
        isMine: Boolean,
        bodyColor: Int,
        keySuffix: String = "",
    ) {
        bodyRenderer.renderMarkdown(
            container = this,
            messageKey = item.stableNativeKey() + keySuffix,
            markdown = markdown,
            fallback = item.renderHints.plainFallback,
            message = message,
            isMine = isMine,
            anchorView = anchorView,
            color = bodyColor,
        )
    }

    private fun LinearLayout.addMusicCard(
        card: NativeMusicCard,
        message: ChatRoomMessage,
        isMine: Boolean,
        anchorView: View,
    ) {
        addView(
            MusicCardView(
                context = view.context,
                themeProvider = themeProvider,
                imageLoaderProvider = imageLoaderProvider,
                onOpenLink = {
                    dismissSourcePopup()
                    onLinkClick(it)
                },
                attachLongPress = { target ->
                    target.enableMessageLongPress(message, isMine, anchorView)
                },
            ).build(card),
        )
    }

    private fun LinearLayout.addFishPiGeneratedBadge(url: String, message: ChatRoomMessage, isMine: Boolean, anchorView: View) {
        val badge = url.toFishPiGeneratedBadgeOrNull() ?: run {
            addText(url, theme.bodyTextSizeSp)
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
                    topMargin = themePx(theme.spacingTightDp)
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
                            icon.applyLoadedDrawable(image.asDrawable(context.resources))
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
            background = cachedRoundRect(theme.quoteBackground.withAlpha(76), 8f)
            val maxWidthPx = dp(220)
            val maxHeightPx = dp(300)
            val minHeightPx = dp(96)
            layoutParams = LinearLayout.LayoutParams(maxWidthPx, dp(163)).apply {
                topMargin = dp(7)
            }
            setOnClickListener {
                dismissSourcePopup()
                val siblings = message.allRenderableImageUrls()
                val index = siblings.indexOf(url).takeIf { it >= 0 } ?: 0
                onImageClick(siblings.ifEmpty { listOf(url) }, index)
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
                topMargin = themePx(theme.spacingMicroDp)
                leftMargin = themePx(theme.spacingTightDp)
                rightMargin = themePx(theme.spacingTightDp)
                gravity = if (isMine) Gravity.END else Gravity.START
            }
            message.reactionSummary.take(6).forEach { reaction ->
                addView(TextView(context).apply {
                    text = "${reaction.emoji} ${reaction.count}"
                    setTextColor(if (reaction.selected) theme.accent else theme.weakText)
                    textSize = 13f
                    typeface = if (reaction.selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    background = cachedRoundRect(if (reaction.selected) theme.quoteBackground else 0x00000000, selectorRadius)
                    setPadding(themeControlPx(), themePx(theme.spacingMicroDp), themeControlPx(), themePx(theme.spacingMicroDp))
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
            background = cachedRoundRect(theme.quoteBackground.withAlpha(76), boxRadius)
            layoutParams = LinearLayout.LayoutParams(dp(230), dp(176)).apply {
                topMargin = dp(8)
            }
            addView(ChatVideoCard(context, theme).apply {
                bind(
                    url = url,
                    onFullscreen = { session ->
                        dismissSourcePopup()
                        onVideoFullscreenClick(url, session)
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
            background = cachedRoundRect(theme.quoteBackground, boxRadius)
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
        addView(
            RedPacketCardView(
                context = view.context,
                themeProvider = themeProvider,
                cardBackground = { color, radiusDp -> cachedRoundRect(color, radiusDp) },
                attachLongPress = { target ->
                    target.enableMessageLongPress(message, isMine, anchorView)
                },
                onCardClick = { onRedPacketClick(it) },
                onGestureClick = { clickedMessage, value -> onRedPacketGestureClick(clickedMessage, value) },
                dismissPopup = { dismissSourcePopup() },
            ).build(message, isMine),
        )
    }

    private fun metaSeparator(): View {
        return View(view.context).apply {
            background = cachedRoundRect(theme.timeText.withAlpha(42), 1f)
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
            textSize = theme.metaTimeTextSizeSp
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
            .replace(ClientNameNonAlnumRegex, "")
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

        return runCatching { ContextCompat.getDrawable(view.context, res) }.getOrNull()
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
            .replace(WhitespaceRegex, " ")
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

    private fun String.visibleTextForWidth(): String = toMeasuredMessageText()


    private fun avatarState(message: ChatRoomMessage, mine: Boolean): ChatAvatarState {
        val darkChrome = Color.luminance(theme.bubbleText) > 0.5f
        return ChatAvatarState(
            userName = message.userName,
            avatarUrl = message.userAvatarURL,
            initial = message.userName.ifBlank { message.displayName }.trim().take(1).ifBlank { "鱼" },
            appearance = message.appearance,
            mine = mine,
            darkChrome = darkChrome,
            themedInnerSizePx = dp(ChatAppearanceSupport.RoomAvatarInnerDp),
            fallbackTextColor = theme.accent,
            fallbackTextSizePx = theme.avatarFallbackTextSizeSp,
            defaultBackground = roundRect(theme.clientBackground, fieldRadius).apply {
                setStroke(themeBorderPx(), theme.bubbleBorder)
            },
            defaultInnerRadiusDp = fieldRadius,
            innerBackground = roundRect(theme.clientBackground, 8f),
            topMarginPx = themeItemPx(),
        )
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
                gravity = if (isMine) Gravity.END else Gravity.START
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
                ).apply { rightMargin = themePx(theme.spacingMicroDp) }
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
            layoutParams = LinearLayout.LayoutParams(themePx(theme.stackedAvatarSizeDp), themePx(theme.stackedAvatarSizeDp)).apply {
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
                        avatarSizePx = themePx(theme.stackedAvatarSizeDp),
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
            setPadding(themeControlPx(), themePx(theme.spacingTightDp), themeControlPx(), themePx(theme.spacingTightDp))
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
            onLongPress(anchorView.messageActionAnchor(message, isMine, boundItem?.revoked == true))
            true
        }
    }

    private fun View.messageActionAnchor(message: ChatRoomMessage, isMine: Boolean, revoked: Boolean): MessageActionAnchor {
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
            revoked = revoked,
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

    private fun themePx(value: Float): Int = (value * view.resources.displayMetrics.density).toInt()
}



private fun String.isAnimatedImageUrl(): Boolean {
    val normalized = substringBefore('#').lowercase()
    val path = normalized.substringBefore('?')
    return path.endsWith(".gif") || normalized.contains("image/gif")
}

private fun View.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

internal class MessageMediaImageView(context: Context) : ImageView(context) {
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

internal fun ImageView.applyLoadedDrawable(drawable: Drawable) {
    setImageDrawable(drawable)
    resumeLoadedAnimation()
}

private fun ImageView.resumeLoadedAnimation() {
    if (isAttachedToWindow) {
        (drawable as? Animatable)?.start()
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
    private var avatarSizePx: Int,
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

    fun setAvatarSize(sizePx: Int) {
        if (avatarSizePx == sizePx) return
        avatarSizePx = sizePx
        if (avatarUrl.isNotBlank() && isAttachedToWindow && isShown) {
            store.unsubscribe(avatarUrl, this)
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
        canvas.save()
        canvas.clipRect(left.toFloat(), top.toFloat(), (left + scaledWidth).toFloat(), (top + scaledHeight).toFloat())
        drawable.bounds = drawRect
        drawable.draw(canvas)
        canvas.restore()
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
