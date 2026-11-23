package dev.fishpi.mobile.shared.message.native

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.drawable.Drawable
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal enum class ChatSakuraStyle { Letter, Moon }

internal data class SakuraStop(val position: Float, val color: Int)

internal data class SakuraGradient(val angleDeg: Float, val stops: List<SakuraStop>)

internal data class SakuraColors(
    val ink: Int,
    val edgeSolid: Int?,
    val edgeGradient: SakuraGradient?,
    val wash: SakuraGradient,
    val flower: SakuraGradient,
    val petal: Int,
    val petalTip: Int,
    val trimShadow: Int,
    val trimLight: Int,
    val frameFill: SakuraGradient,
    val frameConic: Boolean,
    val frameRing: Int?,
    val frameShadow: Int,
    val arcStart: Int,
    val arcEnd: Int,
)

internal object ChatSakura {
    private val letterLight = SakuraColors(
        ink = 0xFF492842.toInt(),
        edgeSolid = 0xFFCB789A.toInt(),
        edgeGradient = null,
        wash = SakuraGradient(
            146f,
            listOf(
                SakuraStop(0.02f, 0xFFFFF7F2.toInt()),
                SakuraStop(0.65f, 0xFFFBE6ED.toInt()),
                SakuraStop(1f, 0xFFF4BFD4.toInt()),
            ),
        ),
        flower = SakuraGradient(
            145f,
            listOf(
                SakuraStop(0.04f, 0xFFFFE3DC.toInt()),
                SakuraStop(0.48f, 0xFFDA709F.toInt()),
                SakuraStop(0.78f, 0xFFCF597C.toInt()),
            ),
        ),
        petal = 0xFFD9689C.toInt(),
        petalTip = 0xFFFFD9E7.toInt(),
        trimShadow = 0xFFEDB1C5.toInt(),
        trimLight = 0xFFFFE8EF.toInt(),
        frameFill = SakuraGradient(
            155f,
            listOf(
                SakuraStop(0f, 0xFFF9DAE5.toInt()),
                SakuraStop(0.61f, 0xFFB75F80.toInt()),
                SakuraStop(1f, 0xFFFFD2A3.toInt()),
            ),
        ),
        frameConic = false,
        frameRing = 0x57B16686,
        frameShadow = 0x45AB587E,
        arcStart = 0,
        arcEnd = 0,
    )

    private val letterDark = letterLight.copy(
        ink = 0xFFFFECF9.toInt(),
        edgeSolid = 0xFFD08AA7.toInt(),
        wash = SakuraGradient(
            145f,
            listOf(
                SakuraStop(0f, 0xFF6A354E.toInt()),
                SakuraStop(0.55f, 0xFF50283F.toInt()),
                SakuraStop(1f, 0xFF733750.toInt()),
            ),
        ),
        flower = SakuraGradient(
            145f,
            listOf(
                SakuraStop(0f, 0xFFFFE2D3.toInt()),
                SakuraStop(0.65f, 0xFFFF9AC6.toInt()),
                SakuraStop(1f, 0xFFE66B9E.toInt()),
            ),
        ),
        petal = 0xFFFF9EC7.toInt(),
        petalTip = 0xFFFFE4EF.toInt(),
        trimShadow = 0xFF9C5A7B.toInt(),
        trimLight = 0xFFD38FB2.toInt(),
        frameFill = SakuraGradient(
            155f,
            listOf(
                SakuraStop(0f, 0xFFFFE4DB.toInt()),
                SakuraStop(0.61f, 0xFFD6689F.toInt()),
                SakuraStop(1f, 0xFFFFD2A3.toInt()),
            ),
        ),
    )

    private val moonLight = SakuraColors(
        ink = 0xFF492842.toInt(),
        edgeSolid = null,
        edgeGradient = SakuraGradient(
            169f,
            listOf(
                SakuraStop(0f, 0xFFCA8EA8.toInt()),
                SakuraStop(0.73f, 0xFF9465A1.toInt()),
                SakuraStop(1f, 0xFFE9B67E.toInt()),
            ),
        ),
        wash = SakuraGradient(
            154f,
            listOf(
                SakuraStop(0f, 0xFFF8EAF6.toInt()),
                SakuraStop(0.65f, 0xFFE8D5EF.toInt()),
                SakuraStop(1f, 0xFFE6C4E6.toInt()),
            ),
        ),
        flower = SakuraGradient(
            155f,
            listOf(
                SakuraStop(0f, 0xFFFCE1D9.toInt()),
                SakuraStop(0.60f, 0xFFBD5B91.toInt()),
                SakuraStop(1f, 0xFF9D507C.toInt()),
            ),
        ),
        petal = 0xFFB23079.toInt(),
        petalTip = 0xFFF4BEDA.toInt(),
        trimShadow = 0xFFB273A0.toInt(),
        trimLight = 0xFFD6AAC7.toInt(),
        frameFill = SakuraGradient(
            -50f,
            listOf(
                SakuraStop(0f, 0xFFAF679D.toInt()),
                SakuraStop(0.23f, 0xFFFBD6D9.toInt()),
                SakuraStop(0.55f, 0xFF775F8D.toInt()),
                SakuraStop(0.75f, 0xFFFDC995.toInt()),
                SakuraStop(1f, 0xFFAF679D.toInt()),
            ),
        ),
        frameConic = true,
        frameRing = null,
        frameShadow = 0x577D4D8A,
        arcStart = 0xFF9C649C.toInt(),
        arcEnd = 0xFFD0939F.toInt(),
    )

    private val moonDark = moonLight.copy(
        ink = 0xFFFFECF9.toInt(),
        edgeGradient = SakuraGradient(
            165f,
            listOf(
                SakuraStop(0f, 0xFFE5A9CE.toInt()),
                SakuraStop(0.72f, 0xFF9067AA.toInt()),
                SakuraStop(1f, 0xFFEEC5A0.toInt()),
            ),
        ),
        wash = SakuraGradient(
            145f,
            listOf(
                SakuraStop(0f, 0xFF552E4B.toInt()),
                SakuraStop(0.60f, 0xFF3B2747.toInt()),
                SakuraStop(1f, 0xFF29263F.toInt()),
            ),
        ),
        flower = SakuraGradient(
            155f,
            listOf(
                SakuraStop(0f, 0xFFFFE3D1.toInt()),
                SakuraStop(0.70f, 0xFFF3A6D4.toInt()),
                SakuraStop(1f, 0xFFC57BBB.toInt()),
            ),
        ),
        petal = 0xFFFF98D2.toInt(),
        petalTip = 0xFFFFF0D3.toInt(),
        trimShadow = 0xFFEFB4D5.toInt(),
        trimLight = 0xFFCA8DB8.toInt(),
        arcStart = 0xFFF0BCE8.toInt(),
        arcEnd = 0xFFFFCFB1.toInt(),
    )

    fun colors(style: ChatSakuraStyle, dark: Boolean): SakuraColors = when (style) {
        ChatSakuraStyle.Letter -> if (dark) letterDark else letterLight
        ChatSakuraStyle.Moon -> if (dark) moonDark else moonLight
    }

    fun bubble(style: ChatSakuraStyle, isMine: Boolean, dark: Boolean, density: Float): ChatBubbleVisual {
        val colors = colors(style, dark)
        return ChatBubbleVisual(
            background = SakuraDuoBubbleDrawable(style, colors, isMine, density),
            paddingLeft = dp(if (isMine) 22f else 15f, density),
            paddingTop = dp(8f, density),
            paddingRight = dp(if (isMine) 15f else if (style == ChatSakuraStyle.Moon) 24f else 20f, density),
            paddingBottom = dp(10f, density),
            reserveLeft = 0,
            reserveRight = 0,
            reserveBottom = 0,
            textColor = colors.ink,
            badge = null,
            badgeOnLeft = false,
            sakura = true,
        )
    }

    fun frame(style: ChatSakuraStyle, dark: Boolean, density: Float, own: Boolean): ChatAvatarFrameVisual {
        val colors = colors(style, dark)
        return ChatAvatarFrameVisual(
            background = SakuraDuoFrameDrawable(style, colors, own, density),
            borderPx = 0f,
            paddingPx = 3f * density,
            badge = null,
            sakura = true,
            innerBackground = SakuraInnerDrawable(style, colors),
        )
    }

    private fun dp(value: Float, density: Float): Int = (value * density).roundToInt()
}

private fun SakuraGradient.paint(bounds: RectF, conic: Boolean = false): Shader = when {
    conic -> SweepGradient(
        bounds.centerX(),
        bounds.centerY(),
        stops.map { it.color }.toIntArray(),
        stops.map { it.position }.toFloatArray(),
    ).let { shader ->
        val matrix = Matrix()
        matrix.setRotate(angleDeg - 90f, bounds.centerX(), bounds.centerY())
        shader.setLocalMatrix(matrix)
        shader
    }

    else -> {
        val radians = angleDeg * PI / 180.0
        val dx = sin(radians).toFloat()
        val dy = -cos(radians).toFloat()
        val half = (abs(bounds.width() * dx) + abs(bounds.height() * dy)) / 2f
        val cx = bounds.centerX()
        val cy = bounds.centerY()
        LinearGradient(
            cx - dx * half,
            cy - dy * half,
            cx + dx * half,
            cy + dy * half,
            stops.map { it.color }.toIntArray(),
            stops.map { it.position }.toFloatArray(),
            Shader.TileMode.CLAMP,
        )
    }
}

internal abstract class SakuraDuoDrawable : Drawable() {
    private var animator: ValueAnimator? = null
    protected var progress = -1f
    protected val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    protected val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    protected var boundsRect = RectF()

    var playing: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (value) start() else stop()
        }

    private fun start() {
        if (animator != null) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = SakuraDuo.durationMs
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = null
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidateSelf()
            }
            start()
        }
    }

    private fun stop() {
        animator?.cancel()
        animator = null
        progress = -1f
        invalidateSelf()
    }

    override fun onBoundsChange(bounds: android.graphics.Rect) {
        boundsRect.set(bounds)
        rebuild(bounds.width().toFloat(), bounds.height().toFloat())
        invalidateSelf()
    }

    protected abstract fun rebuild(width: Float, height: Float)

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

internal class SakuraDuoBubbleDrawable(
    private val style: ChatSakuraStyle,
    private val colors: SakuraColors,
    private val own: Boolean,
    private val density: Float,
) : SakuraDuoDrawable() {
    private val rim = Path()
    private val fill = Path()
    private val trim = Path()
    private val seal = Path()
    private var sealCenterX = 0f
    private var sealCenterY = 0f
    private var arc: RectF? = null
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = android.graphics.BlurMaskFilter(3.5f * density, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }

    private val paperMirrored: Boolean = own
    private val petalFlip: Boolean = own != (style == ChatSakuraStyle.Letter)
    private var shadowBitmap: Bitmap? = null
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun drawDropShadow(canvas: Canvas, w: Float, h: Float) {
        var bitmap = shadowBitmap
        if (bitmap == null) {
            val pad = (5f * density).roundToInt().coerceAtLeast(1)
            bitmap = Bitmap.createBitmap(
                w.roundToInt() + pad * 2,
                h.roundToInt() + pad * 2,
                Bitmap.Config.ARGB_8888,
            )
            val bitmapCanvas = Canvas(bitmap)
            val shadowPath = Path(rim).apply { offset(pad.toFloat(), pad.toFloat() + 3f * density) }
            shadowPaint.color = 0x30874267
            shadowPaint.maskFilter = android.graphics.BlurMaskFilter(2f * density, android.graphics.BlurMaskFilter.Blur.NORMAL)
            bitmapCanvas.drawPath(shadowPath, shadowPaint)
            shadowBitmap = bitmap
        }
        canvas.drawBitmap(bitmap, -5f * density, -5f * density, null)
    }

    override fun rebuild(width: Float, height: Float) {
        rim.reset()
        SakuraDuo.contour(style, width, height, 0f, 0f, density, rim)
        fill.reset()
        SakuraDuo.contour(style, width, height, 1f * density, 1f * density, density, fill)
        shadowBitmap?.recycle()
        shadowBitmap = null
        trim.reset()
        if (style == ChatSakuraStyle.Letter) {
            val trimLeft = width - 1f * density - 15f * density
            val trimTop = 1f * density
            trim.moveTo(trimLeft, trimTop)
            trim.lineTo(trimLeft + 6.75f * density, trimTop)
            trim.lineTo(trimLeft + 15f * density, trimTop + 7.98f * density)
            trim.lineTo(trimLeft + 15f * density, trimTop + 14f * density)
            trim.lineTo(trimLeft + 5.25f * density, trimTop + 10.64f * density)
            trim.close()
        }
        seal.reset()
        val sealSize = 19f * density
        val sealLeft = if (paperMirrored) -6f * density else width + 6f * density - sealSize
        val sealTop = height + 7f * density - sealSize
        SakuraDuo.flower(seal, sealLeft, sealTop, sealSize)
        sealCenterX = sealLeft + sealSize / 2f
        sealCenterY = sealTop + sealSize / 2f
        arc = if (style == ChatSakuraStyle.Moon) {
            val arcWidth = 116f * density
            val arcHeight = 29f * density
            val right = if (paperMirrored) 4f * density + arcWidth else width - 4f * density
            val bottom = height - 2f * density
            RectF(right - arcWidth, bottom - arcHeight, right, bottom)
        } else {
            null
        }
    }

    override fun draw(canvas: Canvas) {
        val w = boundsRect.width()
        val h = boundsRect.height()
        if (w <= 0f || h <= 0f) return
        val decoSave = canvas.save()
        canvas.clipRect(-7f * density, -42f * density, w + 7f * density, h + 14f * density)
        drawDropShadow(canvas, w, h)
        val save = canvas.save()
        if (paperMirrored) canvas.scale(-1f, 1f, w / 2f, h / 2f)
        paint.shader = colors.edgeGradient?.paint(boundsRect)
        paint.color = colors.edgeSolid ?: Color.TRANSPARENT
        canvas.drawPath(rim, paint)
        paint.shader = colors.wash.paint(boundsRect)
        canvas.drawPath(fill, paint)
        if (style == ChatSakuraStyle.Letter) {
            val trimLeft = w - 1f * density - 15f * density
            val trimTop = 1f * density
            paint.shader = SakuraGradient(
                145f,
                listOf(
                    SakuraStop(0.50f, Color.TRANSPARENT),
                    SakuraStop(0.52f, colors.trimShadow),
                    SakuraStop(1f, colors.trimLight),
                ),
            ).paint(RectF(trimLeft, trimTop, trimLeft + 15f * density, trimTop + 14f * density))
            canvas.drawPath(trim, paint)
        } else {
            paint.shader = null
            val trimRect = RectF(w - 23f * density, 4f * density, w - 5f * density, h - 4f * density)
            stroke.color = colors.trimLight
            stroke.strokeWidth = 1f * density
            stroke.alpha = 199
            val trimLineEnd = trimRect.right - 9f * density
            canvas.drawLine(trimRect.left, trimRect.top, trimLineEnd, trimRect.top, stroke)
            canvas.drawLine(trimRect.left, trimRect.bottom, trimLineEnd, trimRect.bottom, stroke)
            stroke.color = colors.trimShadow
            stroke.strokeWidth = 2f * density
            canvas.drawArc(trimRect, -90f, 180f, false, stroke)
            stroke.alpha = 255
        }
        canvas.restoreToCount(save)

        paint.shader = colors.flower.paint(RectF(0f, 0f, w, h))
        val sealSave = canvas.save()
        if (progress >= 0f) {
            val sealDelay = if (paperMirrored) 240f / SakuraDuo.durationMs else 0f
            var sealT = progress - sealDelay
            if (sealT < 0f) sealT += 1f
            val bloom = SakuraDuo.sealBloom(sealT)
            canvas.rotate(SakuraDuo.sealRotation(sealT), sealCenterX, sealCenterY)
            canvas.scale(bloom, bloom, sealCenterX, sealCenterY)
        }
        canvas.drawPath(seal, paint)
        canvas.restoreToCount(sealSave)
        paint.shader = null

        if (progress >= 0f) {
            SakuraDuo.drawPetals(
                canvas = canvas,
                style = style,
                colors = colors,
                density = density,
                progress = progress,
                anchorX = if (paperMirrored) 9f * density else w - 9f * density,
                anchorY = h - 9f * density,
                mirrored = petalFlip,
                own = own,
                frame = false,
            )
        }

        val arcRect = arc
        if (style == ChatSakuraStyle.Moon && arcRect != null && progress >= 0f) {
            val arcDelay = if (paperMirrored) 0.24f else 0.09f
            var arcT = progress - arcDelay
            if (arcT < 0f) arcT += 1f
            val arcState = SakuraDuo.arcState(arcT, density)
            if (arcState.alpha > 0.01f) {
                val originX = if (paperMirrored) arcRect.left else arcRect.right
                val originY = arcRect.centerY()
                val arcSave = canvas.save()
                canvas.translate(0f, arcState.translateYPx)
                canvas.rotate(arcState.rotation, originX, originY)
                canvas.scale(arcState.scaleX, arcState.scaleY, originX, originY)
                val sideAngle = if (paperMirrored) 180f else 270f
                glowPaint.style = Paint.Style.STROKE
                glowPaint.strokeWidth = 3f * density
                glowPaint.color = colors.petal
                glowPaint.alpha = (arcState.alpha * 140f).roundToInt()
                val glowSave = canvas.save()
                canvas.translate(0f, 3f * density)
                canvas.drawArc(arcRect, 0f, 180f, false, glowPaint)
                canvas.drawArc(arcRect, sideAngle, 90f, false, glowPaint)
                canvas.restoreToCount(glowSave)
                glowPaint.style = Paint.Style.FILL
                stroke.strokeWidth = 1f * density
                stroke.alpha = (arcState.alpha * 255f).roundToInt()
                stroke.color = colors.petal
                canvas.drawArc(arcRect, 0f, 180f, false, stroke)
                stroke.color = if (paperMirrored) colors.petal else colors.petalTip
                canvas.drawArc(arcRect, sideAngle, 90f, false, stroke)
                stroke.alpha = 255
                canvas.restoreToCount(arcSave)
            }
        }
        canvas.restoreToCount(decoSave)
    }
}

internal class SakuraDuoFrameDrawable(
    private val style: ChatSakuraStyle,
    private val colors: SakuraColors,
    private val own: Boolean,
    private val density: Float,
) : SakuraDuoDrawable() {
    private val frame = Path()
    private val seal = Path()
    private var sealCenterX = 0f
    private var sealCenterY = 0f
    private var shadowBitmap: Bitmap? = null
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val mirrored: Boolean = own

    override fun rebuild(width: Float, height: Float) {
        frame.reset()
        SakuraDuo.frameContour(style, width, height, frame)
        seal.reset()
        val sealSize = 18f * density
        val sealLeft = if (own) -6f * density else width + 6f * density - sealSize
        val sealTop = height + 7f * density - sealSize
        SakuraDuo.flower(seal, sealLeft, sealTop, sealSize)
        sealCenterX = sealLeft + sealSize / 2f
        sealCenterY = sealTop + sealSize / 2f
        shadowBitmap?.recycle()
        shadowBitmap = null
    }

    override fun draw(canvas: Canvas) {
        val w = boundsRect.width()
        val h = boundsRect.height()
        if (w <= 0f || h <= 0f) return
        val decoSave = canvas.save()
        canvas.clipRect(-7f * density, -42f * density, w + 7f * density, h + 14f * density)
        drawShadow(canvas, w, h)
        paint.shader = colors.frameFill.paint(boundsRect, conic = colors.frameConic)
        canvas.drawPath(frame, paint)
        paint.shader = null
        colors.frameRing?.let { ringColor ->
            stroke.color = ringColor
            stroke.strokeWidth = 2f * density
            canvas.drawPath(frame, stroke)
            paint.shader = colors.frameFill.paint(boundsRect, conic = colors.frameConic)
            canvas.drawPath(frame, paint)
            paint.shader = null
        }
        paint.shader = colors.flower.paint(RectF(0f, 0f, w, h))
        val sealSave = canvas.save()
        if (progress >= 0f) {
            val sealDelay = if (mirrored) 240f / SakuraDuo.durationMs else 0f
            var sealT = progress - sealDelay
            if (sealT < 0f) sealT += 1f
            val bloom = SakuraDuo.sealBloom(sealT)
            canvas.rotate(SakuraDuo.sealRotation(sealT), sealCenterX, sealCenterY)
            canvas.scale(bloom, bloom, sealCenterX, sealCenterY)
        }
        canvas.drawPath(seal, paint)
        canvas.restoreToCount(sealSave)
        paint.shader = null
        if (style == ChatSakuraStyle.Moon && arcStartVisible()) {
            val halfW = w / 2f
            val halfH = h / 2f
            val arcSave = canvas.save()
            if (mirrored) canvas.scale(-1f, 1f, halfW, halfH)
            canvas.rotate(18f, halfW, halfH)
            stroke.strokeWidth = 1f * density
            stroke.shader = SweepGradient(
                halfW,
                halfH,
                intArrayOf(colors.arcStart, colors.arcEnd, colors.arcStart),
                floatArrayOf(0f, 0.55f, 1f),
            )
            canvas.drawArc(-4f * density, -4f * density, w + 4f * density, h + 3f * density, -90f, 145f, false, stroke)
            stroke.shader = null
            canvas.restoreToCount(arcSave)
        }
        if (progress >= 0f) {
            SakuraDuo.drawPetals(
                canvas = canvas,
                style = style,
                colors = colors,
                density = density,
                progress = progress,
                anchorX = w / 2f,
                anchorY = h / 2f,
                mirrored = mirrored,
                own = own,
                frame = true,
            )
        }
        canvas.restoreToCount(decoSave)
    }

    private fun arcStartVisible(): Boolean = colors.arcStart != 0

    private fun drawShadow(canvas: Canvas, w: Float, h: Float) {
        var bitmap = shadowBitmap
        if (bitmap == null) {
            val pad = (6f * density).roundToInt().coerceAtLeast(1)
            bitmap = Bitmap.createBitmap(
                w.roundToInt() + pad * 2,
                h.roundToInt() + pad * 2,
                Bitmap.Config.ARGB_8888,
            )
            val bitmapCanvas = Canvas(bitmap)
            val shadowPath = Path(frame).apply { offset(pad.toFloat(), pad.toFloat() + 2f * density) }
            shadowPaint.color = colors.frameShadow
            shadowPaint.maskFilter = android.graphics.BlurMaskFilter(2.5f * density, android.graphics.BlurMaskFilter.Blur.NORMAL)
            bitmapCanvas.drawPath(shadowPath, shadowPaint)
            shadowBitmap = bitmap
        }
        canvas.drawBitmap(bitmap, -6f * density, -6f * density, null)
    }
}

internal class SakuraInnerDrawable(
    private val style: ChatSakuraStyle,
    private val colors: SakuraColors,
) : Drawable() {
    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun writeContour(width: Float, height: Float, target: Path) {
        target.reset()
        SakuraDuo.frameContour(style, width, height, target)
    }

    override fun onBoundsChange(bounds: android.graphics.Rect) {
        path.reset()
        SakuraDuo.frameContour(style, bounds.width().toFloat(), bounds.height().toFloat(), path)
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        val rect = RectF(0f, 0f, bounds.width().toFloat(), bounds.height().toFloat())
        paint.shader = colors.wash.paint(rect)
        canvas.drawPath(path, paint)
        paint.shader = null
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

internal object SakuraDuo {
    const val durationMs = 5000L

    fun contour(style: ChatSakuraStyle, width: Float, height: Float, insetX: Float, insetY: Float, density: Float, path: Path) {
        val left = insetX
        val top = insetY
        val right = width - insetX
        val bottom = height - insetY
        val h = bottom - top
        when (style) {
            ChatSakuraStyle.Letter -> {
                path.moveTo(left + 7f * density, top)
                path.lineTo(right - 13f * density, top)
                path.cubicTo(right - 6f * density, top, right, top + 6f * density, right, top + 12f * density)
                path.lineTo(right, bottom - 12f * density)
                path.cubicTo(right, bottom - 5f * density, right - 6f * density, bottom, right - 15f * density, bottom)
                path.lineTo(left + 15f * density, bottom)
                path.cubicTo(left + 7f * density, bottom, left + 1f * density, bottom - 5f * density, left + 1f * density, bottom - 14f * density)
                path.lineTo(left + 1f * density, top + 15f * density)
                path.cubicTo(left + 1f * density, top + 11f * density, left, top + 9f * density, left, top + 6f * density)
                path.cubicTo(left, top + 2f * density, left + 3f * density, top, left + 7f * density, top)
                path.close()
            }

            ChatSakuraStyle.Moon -> {
                path.moveTo(left + 8f * density, top)
                path.lineTo(right - 23f * density, top)
                path.cubicTo(right - 10f * density, top, right, top + h * 0.23f, right, top + h * 0.5f)
                path.cubicTo(right, top + h * 0.77f, right - 10f * density, bottom, right - 23f * density, bottom)
                path.lineTo(left + 14f * density, bottom)
                path.cubicTo(left + 6f * density, bottom, left, bottom - 6f * density, left, bottom - 14f * density)
                path.lineTo(left, top + 8f * density)
                path.cubicTo(left, top + 3f * density, left + 3f * density, top, left + 8f * density, top)
                path.close()
            }
        }
    }

    fun frameContour(style: ChatSakuraStyle, width: Float, height: Float, path: Path) {
        val radii = if (style == ChatSakuraStyle.Letter) {
            floatArrayOf(
                0.48f, 0.48f,
                0.39f, 0.39f,
                0.48f, 0.48f,
                0.35f, 0.35f,
            )
        } else {
            floatArrayOf(
                0.13f, 0.13f,
                0.46f, 0.46f,
                0.22f, 0.22f,
                0.44f, 0.44f,
            )
        }
        path.reset()
        path.addRoundRect(
            RectF(0f, 0f, width, height),
            floatArrayOf(
                radii[0] * width, radii[1] * height,
                radii[2] * width, radii[3] * height,
                radii[4] * width, radii[5] * height,
                radii[6] * width, radii[7] * height,
            ),
            Path.Direction.CW,
        )
    }

    fun flower(path: Path, left: Float, top: Float, size: Float) {
        path.reset()
        val scale = size / 30f
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        matrix.postTranslate(left + size / 2f, top + size / 2f)
        val petal = Path().apply {
            moveTo(0f, -1f)
            cubicTo(-5f, -3f, -6f, -8f, -3.4f, -13f)
            quadTo(-2.3f, -14f, -1.5f, -12.6f)
            lineTo(0f, -11f)
            lineTo(1.5f, -12.6f)
            quadTo(2.3f, -14f, 3.4f, -13f)
            cubicTo(6f, -8f, 5f, -3f, 0f, -1f)
            close()
        }
        for (index in 0 until 5) {
            val rotation = Matrix()
            rotation.setRotate(index * 72f)
            val rotated = Path(petal)
            rotated.transform(rotation)
            path.addPath(rotated)
        }
        val center = Path().apply { addCircle(0f, 0f, 1.4f, Path.Direction.CW) }
        path.addPath(center)
        path.transform(matrix)
    }

    fun drawPetals(
        canvas: Canvas,
        style: ChatSakuraStyle,
        colors: SakuraColors,
        density: Float,
        progress: Float,
        anchorX: Float,
        anchorY: Float,
        mirrored: Boolean,
        own: Boolean,
        frame: Boolean,
    ) {
        val points = if (frame) {
            arrayOf(
                floatArrayOf(24f, -14f, 39f, 1f, 6f),
                floatArrayOf(8f, -29f, -10f, -39f, 7f),
                floatArrayOf(-24f, -10f, -42f, 8f, 6f),
                floatArrayOf(-14f, 19f, -36f, 26f, 5f),
                floatArrayOf(23f, 19f, 48f, 9f, 7f),
            )
        } else if (style == ChatSakuraStyle.Moon) {
            arrayOf(
                floatArrayOf(-11f, -25f, -67f, -28f, 8f),
                floatArrayOf(-37f, -13f, -104f, 3f, 9f),
                floatArrayOf(9f, -18f, -37f, -36f, 7f),
                floatArrayOf(-22f, 19f, -71f, 20f, 5f),
                floatArrayOf(-61f, -22f, -123f, -10f, 10f),
                floatArrayOf(26f, -12f, 41f, 17f, 7f),
                floatArrayOf(-11f, -18f, -95f, -35f, 7f),
                floatArrayOf(37f, 8f, 49f, 29f, 5f),
                floatArrayOf(-46f, 10f, -89f, 33f, 6f),
            )
        } else {
            arrayOf(
                floatArrayOf(31f, -10f, 57f, 20f, 8f),
                floatArrayOf(43f, 5f, 83f, 35f, 10f),
                floatArrayOf(9f, -27f, 37f, -5f, 7f),
                floatArrayOf(27f, 20f, 64f, 38f, 9f),
                floatArrayOf(-17f, -23f, -35f, 11f, 6f),
                floatArrayOf(51f, -16f, 105f, 24f, 8f),
                floatArrayOf(-27f, -9f, -56f, 24f, 8f),
                floatArrayOf(18f, -18f, 74f, 5f, 5f),
            )
        }
        val direction = if (mirrored) -1f else 1f
        points.forEachIndexed { index, point ->
            val delay = (index * 48 + if (own) 200 else 0) / 5000f
            var t = progress - delay
            if (t < 0f) t += 1f
            val state = driftState(t, index)
            if (state.alpha <= 0.01f) return@forEachIndexed
            val x1 = direction * point[0] * density
            val y1 = point[1] * density
            val x2 = direction * point[2] * density
            val y2 = point[3] * density
            val size = point[4] * density
            val turn = direction * (180f + index * 16f)
            val translateX = when {
                state.phase < 0.14f -> lerp(0f, x1, state.phase / 0.14f)
                else -> lerp(x1, x2, (state.phase - 0.14f) / 0.86f)
            }
            val translateY = when {
                state.phase < 0.14f -> lerp(-3f * density, y1, state.phase / 0.14f)
                else -> lerp(y1, y2, (state.phase - 0.14f) / 0.86f)
            }
            val rotate = when {
                state.phase < 0.14f -> lerp(0f, 103f, state.phase / 0.14f)
                else -> lerp(103f, turn, (state.phase - 0.14f) / 0.86f)
            }
            canvas.save()
            canvas.translate(anchorX + translateX, anchorY + translateY)
            canvas.rotate(rotate)
            canvas.scale(state.scale, state.scale)
            drawPetal(canvas, style, colors, size, index)
            canvas.restore()
        }
    }

    fun driftState(t: Float, index: Int): PetalState {
        val local = t.coerceIn(0f, 1f)
        val alpha: Float
        val scale: Float
        when {
            local < 0.06f -> {
                alpha = lerp(0f, 0.9f, ease(local / 0.06f))
                scale = lerp(0.4f, 0.4f, 0f)
            }
            local < 0.14f -> {
                alpha = lerp(0.9f, 0.86f, ease((local - 0.06f) / 0.08f))
                scale = lerp(0.4f, 1f, ease((local - 0.06f) / 0.08f))
            }
            local < 0.28f -> {
                alpha = lerp(0.86f, 0f, ease((local - 0.14f) / 0.14f))
                scale = lerp(1f, 0.63f, ease((local - 0.14f) / 0.14f))
            }
            else -> {
                alpha = 0f
                scale = 0.63f
            }
        }
        return PetalState(alpha = alpha, scale = scale, phase = local)
    }

    private fun drawPetal(canvas: Canvas, style: ChatSakuraStyle, colors: SakuraColors, size: Float, index: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(
            -size / 2f,
            -size / 2f,
            size / 2f,
            size / 2f,
            colors.petalTip,
            colors.petal,
            Shader.TileMode.CLAMP,
        )
        if (style == ChatSakuraStyle.Moon && index % 3 == 2) {
            val path = Path()
            path.moveTo(0f, -size / 2f)
            path.lineTo(size * 0.10f, -size * 0.11f)
            path.lineTo(size / 2f, 0f)
            path.lineTo(size * 0.10f, size * 0.11f)
            path.lineTo(0f, size / 2f)
            path.lineTo(-size * 0.10f, size * 0.11f)
            path.lineTo(-size / 2f, 0f)
            path.lineTo(-size * 0.10f, -size * 0.11f)
            path.close()
            canvas.drawPath(path, paint)
        } else {
            val w = size
            val h = size / 1.45f
            val path = Path()
            path.moveTo(-w / 2f, 0f)
            path.lineTo(-w * 0.38f, -h * 0.28f)
            path.lineTo(-w * 0.12f, -h * 0.46f)
            path.lineTo(w * 0.21f, -h / 2f)
            path.lineTo(w / 2f, -h * 0.30f)
            path.lineTo(w * 0.36f, -h * 0.11f)
            path.lineTo(w * 0.48f, h * 0.03f)
            path.lineTo(w * 0.26f, h * 0.31f)
            path.lineTo(-w * 0.05f, h / 2f)
            path.lineTo(-w * 0.29f, h * 0.32f)
            path.close()
            canvas.drawPath(path, paint)
        }
    }

    fun sealBloom(t: Float): Float = when {
        t < 0.06f -> lerp(1f, 1.34f, ease(t / 0.06f))
        t < 0.16f -> lerp(1.34f, 1f, ease((t - 0.06f) / 0.10f))
        else -> 1f
    }

    fun sealRotation(t: Float): Float = when {
        t < 0.06f -> lerp(0f, 16f, ease(t / 0.06f))
        t < 0.16f -> lerp(16f, 0f, ease((t - 0.06f) / 0.10f))
        else -> 0f
    }

    fun arcAlpha(t: Float): Float = when {
        t < 0.08f -> lerp(0f, 0.74f, ease(t / 0.08f))
        t < 0.22f -> lerp(0.74f, 0f, ease((t - 0.08f) / 0.14f))
        else -> 0f
    }

    fun arcState(t: Float, density: Float): SakuraArcState {
        val eased = cubicBezierEase(0f, 0f, 0.58f, 1f, (t.coerceAtMost(0.22f) / 0.22f).coerceIn(0f, 1f))
        return SakuraArcState(
            alpha = arcAlpha(t),
            scaleX = lerp(0.25f, 1.08f, eased),
            scaleY = lerp(0.65f, 1.18f, eased),
            rotation = lerp(-8f, 3f, eased),
            translateYPx = 6f * density * eased,
        )
    }

    fun ease(t: Float): Float = cubicBezierEase(0.18f, 0.51f, 0.54f, 1f, t.coerceIn(0f, 1f))

    private fun lerp(start: Float, end: Float, fraction: Float): Float = start + (end - start) * fraction
}

internal data class PetalState(val alpha: Float, val scale: Float, val phase: Float)

internal data class SakuraArcState(
    val alpha: Float,
    val scaleX: Float,
    val scaleY: Float,
    val rotation: Float,
    val translateYPx: Float,
)

internal interface SakuraAnimatable {
    fun playSakuraArrival()

    fun stopSakuraArrival()
}

internal fun cubicBezierEase(x1: Float, y1: Float, x2: Float, y2: Float, t: Float): Float {
    if (t <= 0f) return 0f
    if (t >= 1f) return 1f
    var low = 0f
    var high = 1f
    var u = t
    repeat(24) {
        u = (low + high) / 2f
        val x = bezierAxis(u, x1, x2)
        if (x < t) low = u else high = u
    }
    return bezierAxis(u, y1, y2)
}

private fun bezierAxis(u: Float, p1: Float, p2: Float): Float {
    val inv = 1f - u
    return 3f * inv * inv * u * p1 + 3f * inv * u * u * p2 + u * u * u
}
