package dev.fishpi.mobile.shared.message.native

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import dev.fishpi.mobile.data.ChatRoomMessage
import coil3.ImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Size
import kotlin.math.roundToInt

internal sealed interface ChatBubbleSkin {
    val kind: ChatBubbleSkinKind

    data object Default : ChatBubbleSkin {
        override val kind: ChatBubbleSkinKind get() = ChatBubbleSkinKind.Default
    }

    data object Standalone : ChatBubbleSkin {
        override val kind: ChatBubbleSkinKind get() = ChatBubbleSkinKind.Standalone
    }

    data class Decorated(val visual: ChatBubbleVisual) : ChatBubbleSkin {
        override val kind: ChatBubbleSkinKind get() = ChatBubbleSkinKind.Decorated
    }
}

internal enum class ChatBubbleSkinKind { Default, Standalone, Decorated }

private data class ChatBubbleGeometry(
    val paddingLeft: Int,
    val paddingTop: Int,
    val paddingRight: Int,
    val paddingBottom: Int,
    val reserveLeft: Int,
    val reserveRight: Int,
    val reserveBottom: Int,
    val minWidth: Int,
)

internal class NativeChatBubbleView(
    context: android.content.Context,
    private val themeProvider: () -> NativeMessageTheme,
) : FrameLayout(context), SakuraAnimatable {
    private val theme: NativeMessageTheme
        get() = themeProvider()

    private var skin: ChatBubbleSkin = ChatBubbleSkin.Default
    private var geometry: ChatBubbleGeometry? = null
    private var sticker: ImageView? = null
    private var stickerUrl: String? = null
    private var defaultBackgroundTheme: NativeMessageTheme? = null
    private var defaultBackgroundDrawable: android.graphics.drawable.Drawable? = null

    private fun cachedDefaultBackground(create: () -> android.graphics.drawable.Drawable): android.graphics.drawable.Drawable {
        val currentTheme = theme
        val cached = defaultBackgroundDrawable
        if (cached != null && defaultBackgroundTheme === currentTheme) return cached
        val created = create()
        defaultBackgroundTheme = currentTheme
        defaultBackgroundDrawable = created
        return created
    }

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

    fun bind(next: ChatBubbleSkin, imageLoader: () -> ImageLoader) {
        val previous = skin
        val visual = (next as? ChatBubbleSkin.Decorated)?.visual
        skin = next
        if (visual != null) {
            background = visual.background
            content.setPadding(0, 0, 0, 0)
        }
        ((previous as? ChatBubbleSkin.Decorated)?.visual?.background as? SakuraDuoDrawable)?.playing = false
        visual?.let {
            val drawable = it.background as? SakuraDuoDrawable ?: return@let
            drawable.playing = it.sakura && isAttachedToWindow
        }
        applySticker(visual?.sticker, imageLoader)
        val nextGeometry = visual?.let {
            ChatBubbleGeometry(
                paddingLeft = it.paddingLeft,
                paddingTop = it.paddingTop,
                paddingRight = it.paddingRight,
                paddingBottom = it.paddingBottom,
                reserveLeft = it.reserveLeft,
                reserveRight = it.reserveRight,
                reserveBottom = it.reserveBottom,
                minWidth = it.minWidth,
            )
        }
        if (previous.kind != next.kind || geometry != nextGeometry) {
            geometry = nextGeometry
            requestLayout()
        }
        invalidate()
    }

    fun bindMessage(
        message: ChatRoomMessage,
        isMine: Boolean,
        darkChrome: Boolean,
        standaloneCard: Boolean,
        defaultBackground: () -> android.graphics.drawable.Drawable,
        imageLoader: () -> ImageLoader,
    ): Int {
        val visual = if (standaloneCard) {
            null
        } else {
            ChatAppearanceRenderer.bubble(
                snapshot = message.appearance,
                dark = darkChrome,
                isMine = isMine,
                resources = resources,
                density = resources.displayMetrics.density,
            )
        }
        bind(
            next = when {
                visual != null -> ChatBubbleSkin.Decorated(visual)
                standaloneCard -> ChatBubbleSkin.Standalone
                else -> ChatBubbleSkin.Default
            },
            imageLoader = imageLoader,
        )
        if (visual == null) {
            val next = if (standaloneCard) null else cachedDefaultBackground(defaultBackground)
            if (background !== next) background = next
        }
        if (foreground != null) foreground = null
        if (elevation != 0f) elevation = 0f
        val topMargin = themePx(theme.bubbleTopMarginDp)
        val params = layoutParams
        if (params !is LinearLayout.LayoutParams || params.topMargin != topMargin) {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                this.topMargin = topMargin
            }
        }
        if (visual != null || standaloneCard) {
            if (content.paddingLeft != 0 || content.paddingTop != 0 || content.paddingRight != 0 || content.paddingBottom != 0) {
                content.setPadding(0, 0, 0, 0)
            }
        } else {
            val control = (theme.spacingControlDp * resources.displayMetrics.density).roundToInt()
            val left = control + themePx(theme.bubblePaddingHorizontalExtraDp)
            val top = control + themePx(theme.bubblePaddingTopExtraDp)
            val right = control + themePx(theme.bubblePaddingHorizontalExtraDp)
            val bottom = control + themePx(theme.bubblePaddingBottomExtraDp)
            if (content.paddingLeft != left || content.paddingTop != top ||
                content.paddingRight != right || content.paddingBottom != bottom
            ) {
                content.setPadding(left, top, right, bottom)
            }
        }
        return visual?.textColor ?: theme.bubbleText
    }

    override fun playSakuraArrival() {
        val visual = (skin as? ChatBubbleSkin.Decorated)?.visual ?: return
        (visual.background as? SakuraDuoDrawable)?.playing = true
    }

    override fun stopSakuraArrival() {
        val visual = (skin as? ChatBubbleSkin.Decorated)?.visual ?: return
        (visual.background as? SakuraDuoDrawable)?.playing = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val visual = (skin as? ChatBubbleSkin.Decorated)?.visual ?: return
        if (visual.sakura) (visual.background as? SakuraDuoDrawable)?.playing = true
    }

    override fun onDetachedFromWindow() {
        stopSakuraArrival()
        super.onDetachedFromWindow()
    }

    private fun themePx(value: Float): Int = (value * resources.displayMetrics.density).toInt()

    private fun applySticker(badge: ChatBubbleSticker?, imageLoader: () -> ImageLoader) {
        if (badge == null) {
            sticker?.let {
                it.setImageDrawable(null)
                removeView(it)
            }
            sticker = null
            stickerUrl = null
            return
        }
        val view = sticker ?: ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            addView(this, LayoutParams(badge.sizePx, badge.sizePx))
            sticker = this
        }
        val params = view.layoutParams as LayoutParams
        params.width = badge.sizePx
        params.height = badge.sizePx
        params.gravity = (if (badge.onStart) Gravity.START else Gravity.END) or Gravity.BOTTOM
        params.marginStart = badge.marginStartPx
        params.marginEnd = badge.marginStartPx
        params.bottomMargin = badge.marginBottomPx
        view.layoutParams = params
        if (stickerUrl == badge.url) return
        stickerUrl = badge.url
        view.setImageDrawable(null)
        val request = ImageRequest.Builder(context)
            .data(badge.url)
            .size(Size(badge.sizePx, badge.sizePx))
            .precision(Precision.INEXACT)
            .target(
                onSuccess = { image ->
                    if (sticker === view && stickerUrl == badge.url) {
                        view.applyLoadedDrawable(image.asDrawable(context.resources))
                    }
                },
            )
            .build()
        imageLoader().enqueue(request)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val appearance = geometry
        if (appearance == null) {
            measureChild(content, widthMeasureSpec, heightMeasureSpec)
            val desiredWidth = content.measuredWidth.coerceAtLeast(themePx(theme.bubbleMinWidthDp))
            val desiredHeight = content.measuredHeight
            setMeasuredDimension(
                resolveSize(desiredWidth, widthMeasureSpec),
                resolveSize(desiredHeight, heightMeasureSpec),
            )
            return
        }
        val horizontal = appearance.paddingLeft + appearance.paddingRight +
            appearance.reserveLeft + appearance.reserveRight
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val availableWidth = if (widthMode == MeasureSpec.UNSPECIFIED) {
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        } else {
            MeasureSpec.makeMeasureSpec(
                (MeasureSpec.getSize(widthMeasureSpec) - horizontal).coerceAtLeast(0),
                MeasureSpec.AT_MOST,
            )
        }
        measureChild(content, availableWidth, heightMeasureSpec)
        val badge = (skin as? ChatBubbleSkin.Decorated)?.visual?.sticker
        val stickerView = sticker
        if (badge != null && stickerView != null) {
            val exact = MeasureSpec.makeMeasureSpec(badge.sizePx, MeasureSpec.EXACTLY)
            stickerView.measure(exact, exact)
        }
        setMeasuredDimension(
            resolveSize(
                maxOf(content.measuredWidth + horizontal, appearance.minWidth),
                widthMeasureSpec,
            ),
            resolveSize(
                content.measuredHeight + appearance.paddingTop + appearance.paddingBottom +
                    appearance.reserveBottom,
                heightMeasureSpec,
            ),
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val appearance = geometry
        super.onLayout(changed, left, top, right, bottom)
        if (appearance == null) {
            content.layout(0, 0, content.measuredWidth, content.measuredHeight)
            return
        }
        val contentLeft = appearance.reserveLeft + appearance.paddingLeft
        content.layout(
            contentLeft,
            appearance.paddingTop,
            contentLeft + content.measuredWidth,
            appearance.paddingTop + content.measuredHeight,
        )
    }

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        super.dispatchDraw(canvas)
        val appearance = (skin as? ChatBubbleSkin.Decorated)?.visual ?: return
        appearance.paintBadge(canvas, resources.displayMetrics.density)
    }
}
