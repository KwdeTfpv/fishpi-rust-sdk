package dev.fishpi.mobile.shared.message.native

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import coil3.ImageLoader
import dev.fishpi.mobile.data.ChatAppearanceSnapshot
import kotlin.math.roundToInt

internal data class ChatAvatarState(
    val userName: String,
    val avatarUrl: String,
    val initial: String,
    val appearance: ChatAppearanceSnapshot?,
    val mine: Boolean,
    val darkChrome: Boolean,
    val themedInnerSizePx: Int,
    val fallbackTextColor: Int,
    val fallbackTextSizePx: Float,
    val defaultBackground: Drawable,
    val defaultInnerRadiusDp: Float,
    val innerBackground: Drawable,
    val topMarginPx: Int,
)

internal class NativeChatAvatarView(
    context: Context,
    private val store: SharedAvatarDrawableStore,
    private val imageLoaderProvider: () -> ImageLoader,
    private val onAvatarClick: (String) -> Unit,
    private val onAvatarLongPress: (String) -> Unit,
) : FrameLayout(context), SakuraAnimatable {
    private val density = resources.displayMetrics.density
    private val label = TextView(context).apply {
        gravity = Gravity.CENTER
        typeface = Typeface.DEFAULT_BOLD
        maxLines = 1
    }
    private val avatar = SharedAvatarImageView(
        context = context,
        store = store,
        imageLoaderProvider = imageLoaderProvider,
        avatarSizePx = 0,
    )
    private var frame: ChatAvatarFrameVisual? = null
    private var frameSizePx = 0
    private var innerSizePx = 0
    private var defaultBackground: Drawable? = null
    private var defaultInnerRadiusPx = 0f
    private var boundUserName = ""
    private val avatarClip = Path()
    private var avatarClipSize = 0
    private val defaultClip = Path()
    private var defaultClipValid = false

    init {
        clipChildren = false
        clipToPadding = false
        addView(label, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(avatar, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        avatar.visibility = INVISIBLE
        avatar.onAvailabilityChanged = { available ->
            avatar.visibility = if (available) VISIBLE else INVISIBLE
            label.visibility = if (available) GONE else VISIBLE
        }
        setOnClickListener {
            if (boundUserName.isNotBlank()) onAvatarClick(boundUserName)
        }
        setOnLongClickListener {
            if (boundUserName.isNotBlank()) {
                onAvatarLongPress(boundUserName)
                true
            } else {
                false
            }
        }
    }

    fun bind(state: ChatAvatarState) {
        boundUserName = state.userName
        val visual = ChatAppearanceRenderer.avatarFrame(
            snapshot = state.appearance,
            dark = state.darkChrome,
            resources = resources,
            density = density,
            isMine = state.mine,
        )
        val oldVisual = frame
        frame = visual
        val inner = state.themedInnerSizePx
        innerSizePx = inner
        val geometry = visual?.geometry(inner)
        val ringPx = geometry?.ringPx ?: 0
        frameSizePx = geometry?.frameSizePx ?: inner
        val slotRingPx = (ChatAppearanceSupport.RoomAvatarSlotRingDp * density).roundToInt()
        val padPx = if (visual != null) ringPx else slotRingPx
        if (padPx != paddingLeft || padPx != paddingTop ||
            padPx != paddingRight || padPx != paddingBottom
        ) {
            setPadding(padPx, padPx, padPx, padPx)
        }
        if (visual != null) {
            if (background != null) background = null
            if (clipToOutline) clipToOutline = false
            if (elevation != 0f) elevation = 0f
            defaultBackground = null
        } else {
            if (background != null) background = null
            if (clipToOutline) clipToOutline = false
            if (elevation != 0f) elevation = 0f
            if (defaultBackground !== state.defaultBackground) {
                defaultBackground = state.defaultBackground
                defaultClipValid = false
            }
            defaultInnerRadiusPx = state.defaultInnerRadiusDp * density
        }
        if (label.text?.toString() != state.initial) label.text = state.initial
        if (label.currentTextColor != state.fallbackTextColor) label.setTextColor(state.fallbackTextColor)
        if (label.textSize != state.fallbackTextSizePx) {
            label.setTextSize(TypedValue.COMPLEX_UNIT_PX, state.fallbackTextSizePx)
        }
        val slotSizePx = inner + 2 * slotRingPx
        val params = layoutParams
        if (params !is LinearLayout.LayoutParams || params.width != slotSizePx ||
            params.height != slotSizePx || params.topMargin != state.topMarginPx
        ) {
            val current = layoutParams as? LinearLayout.LayoutParams
            layoutParams = LinearLayout.LayoutParams(slotSizePx, slotSizePx).apply {
                topMargin = state.topMarginPx
                if (current != null) {
                    leftMargin = current.leftMargin
                    rightMargin = current.rightMargin
                }
            }
        }
        val avatarParams = avatar.layoutParams
        if (avatarParams.width != inner || avatarParams.height != inner) {
            avatar.layoutParams = LayoutParams(inner, inner)
        }
        avatar.setAvatarSize(inner)
        val innerBackground = if (visual != null) visual.innerBackground ?: state.innerBackground else null
        if (avatar.background !== innerBackground) avatar.background = innerBackground
        if (avatar.clipToOutline != (visual != null)) avatar.clipToOutline = visual != null
        if (visual !== oldVisual) avatarClipSize = 0
        if (visual != null && visual !== oldVisual) {
            ((oldVisual?.background) as? SakuraDuoDrawable)?.playing = false
            (visual.background as? SakuraDuoDrawable)?.playing = visual.sakura && isAttachedToWindow
        }
        if (state.avatarUrl.isBlank()) {
            avatar.setAvatarUrl("")
            avatar.visibility = INVISIBLE
            label.visibility = VISIBLE
        } else {
            avatar.setAvatarUrl(state.avatarUrl)
        }
        visibility = VISIBLE
    }

    fun recycle() {
        stopSakuraArrival()
        avatar.setAvatarUrl("")
        avatar.visibility = INVISIBLE
        label.visibility = VISIBLE
        boundUserName = ""
    }

    override fun playSakuraArrival() {
        ((frame?.background) as? SakuraDuoDrawable)?.playing = true
    }

    override fun stopSakuraArrival() {
        ((frame?.background) as? SakuraDuoDrawable)?.playing = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val visual = frame ?: return
        if (visual.sakura) (visual.background as? SakuraDuoDrawable)?.playing = true
    }

    override fun onDetachedFromWindow() {
        stopSakuraArrival()
        super.onDetachedFromWindow()
    }

    override fun dispatchDraw(canvas: Canvas) {
        val visual = frame
        if (visual == null) {
            val bg = defaultBackground
            val pad = paddingLeft
            val inner = innerSizePx
            if (bg == null || inner <= 0) {
                super.dispatchDraw(canvas)
                return
            }
            bg.setBounds(pad, pad, pad + inner, pad + inner)
            bg.draw(canvas)
            if (!defaultClipValid) {
                defaultClip.reset()
                defaultClip.addRoundRect(
                    pad.toFloat(),
                    pad.toFloat(),
                    (pad + inner).toFloat(),
                    (pad + inner).toFloat(),
                    defaultInnerRadiusPx,
                    defaultInnerRadiusPx,
                    Path.Direction.CW,
                )
                defaultClipValid = true
            }
            val clipSave = canvas.save()
            canvas.clipPath(defaultClip)
            super.dispatchDraw(canvas)
            canvas.restoreToCount(clipSave)
            return
        }
        visual.background.setBounds(0, 0, frameSizePx, frameSizePx)
        visual.background.draw(canvas)
        val inner = visual.innerBackground
        val innerSize = frameSizePx - paddingLeft - paddingRight
        if (inner is SakuraInnerDrawable && innerSize > 0) {
            if (avatarClipSize != innerSize) {
                inner.writeContour(innerSize.toFloat(), innerSize.toFloat(), avatarClip)
                avatarClipSize = innerSize
            }
            val clipSave = canvas.save()
            canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())
            canvas.clipPath(avatarClip)
            canvas.translate(-paddingLeft.toFloat(), -paddingTop.toFloat())
            super.dispatchDraw(canvas)
            canvas.restoreToCount(clipSave)
        } else {
            super.dispatchDraw(canvas)
        }
        val badge = visual.badge ?: return
        val boxLeft = 0f
        val boxTop = 0f
        val boxRight = boxLeft + frameSizePx
        val boxBottom = boxTop + frameSizePx
        ChatAppearanceRenderer.drawBadge(
            canvas = canvas,
            badge = badge,
            boxLeft = boxLeft,
            boxTop = boxTop,
            boxRight = boxRight,
            boxBottom = boxBottom,
            onLeft = false,
            density = density,
        )
    }
}
