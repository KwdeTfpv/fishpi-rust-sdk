package dev.fishpi.mobile.shared.message.native

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.roundToInt

internal enum class ChatBubbleMotif { None, Paw, Leaf, Bell, Fish }

internal enum class ChatBubbleShape { MemoV1, LotusV1, CatV1, FishV1 }

internal data class ChatBubblePalette(
    val background: Int,
    val ownBackground: Int,
    val border: Int,
    val ink: Int,
    val text: Int,
)

internal data class ChatBubbleStyle(
    val light: ChatBubblePalette,
    val dark: ChatBubblePalette,
    val radius: Float,
    val borderWidth: Float,
    val shadow: Boolean,
    val motif: ChatBubbleMotif,
    val imageUrl: String,
    val shape: ChatBubbleShape?,
) {
    fun palette(dark: Boolean): ChatBubblePalette = if (dark) this.dark else this.light

    companion object {
        private val HEX = Regex("^#[a-fA-F0-9]{6}$")
        private val IMAGE = Regex(
            "^https://(?:file|pic)\\.fishpi\\.cn/[a-zA-Z0-9/_-][a-zA-Z0-9/_.-]*\\.(?:png|webp|jpg|jpeg|avif)$",
        )
        private const val MAX_IMAGE_LENGTH = 512

        private const val CACHE_LIMIT = 64
        private val cache = object : LinkedHashMap<String, ChatBubbleStyle?>(CACHE_LIMIT, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ChatBubbleStyle?>?): Boolean =
                size > CACHE_LIMIT
        }

        fun decode(json: String?): ChatBubbleStyle? {
            if (json.isNullOrBlank()) return null
            synchronized(cache) {
                if (cache.containsKey(json)) return cache[json]
            }
            val style = runCatching { JSONObject(json) }.getOrNull()?.flatten()?.let { parseFields(it) }
            synchronized(cache) { cache[json] = style }
            return style
        }

        internal fun parseFields(fields: Map<String, Any?>): ChatBubbleStyle? {
            val light = palette(fields["light"]) ?: return null
            val dark = palette(fields["dark"]) ?: return null
            val radius = intField(fields["radius"], 6, 24) ?: return null
            val borderWidth = intField(fields["borderWidth"], 0, 2) ?: return null
            val shadow = fields["shadow"] as? Boolean ?: return null
            val motif = when (fields["motif"]) {
                "none" -> ChatBubbleMotif.None
                "paw" -> ChatBubbleMotif.Paw
                "leaf" -> ChatBubbleMotif.Leaf
                "bell" -> ChatBubbleMotif.Bell
                "fish" -> ChatBubbleMotif.Fish
                else -> return null
            }
            val image = fields["image"] as? String ?: return null
            if (!isImage(image)) return null
            val shape = when (fields["shape"]) {
                null -> null
                "memo-v1" -> ChatBubbleShape.MemoV1
                "lotus-v1" -> ChatBubbleShape.LotusV1
                "cat-v1" -> ChatBubbleShape.CatV1
                "fish-v1" -> ChatBubbleShape.FishV1
                else -> return null
            }
            return ChatBubbleStyle(
                light = light,
                dark = dark,
                radius = radius.toFloat(),
                borderWidth = borderWidth.toFloat(),
                shadow = shadow,
                motif = motif,
                imageUrl = image,
                shape = shape,
            )
        }

        private fun palette(value: Any?): ChatBubblePalette? {
            val row = value as? Map<*, *> ?: return null
            return ChatBubblePalette(
                background = color(row["background"]) ?: return null,
                ownBackground = color(row["ownBackground"]) ?: return null,
                border = color(row["border"]) ?: return null,
                ink = color(row["ink"]) ?: return null,
                text = color(row["text"]) ?: return null,
            )
        }

        private fun color(value: Any?): Int? {
            val text = value as? String ?: return null
            if (!HEX.matches(text)) return null
            return 0xFF000000.toInt() or text.substring(1).toInt(16)
        }

        private fun intField(value: Any?, min: Int, max: Int): Int? {
            val number = value as? Number ?: return null
            val whole = number.toDouble()
            if (whole != whole.toInt().toDouble()) return null
            val result = whole.toInt()
            return if (result in min..max) result else null
        }

        private fun JSONObject.flatten(): Map<String, Any?> {
            val names = keys()
            val result = LinkedHashMap<String, Any?>()
            while (names.hasNext()) {
                val name = names.next()
                result[name] = when (val value = opt(name)) {
                    is JSONObject -> value.flatten()
                    JSONObject.NULL -> null
                    else -> value
                }
            }
            return result
        }

        private fun isImage(value: String): Boolean {
            if (value.isEmpty()) return true
            if (value.length > MAX_IMAGE_LENGTH) return false
            if (value.contains("..")) return false
            return IMAGE.matches(value)
        }
    }
}

internal sealed interface ChatShapeCommand {
    data class MoveTo(val x: Float, val y: Float) : ChatShapeCommand
    data class LineTo(val x: Float, val y: Float) : ChatShapeCommand
    data class QuadTo(val controlX: Float, val controlY: Float, val x: Float, val y: Float) : ChatShapeCommand
    data class CubicTo(
        val control1X: Float,
        val control1Y: Float,
        val control2X: Float,
        val control2Y: Float,
        val x: Float,
        val y: Float,
    ) : ChatShapeCommand
    object Close : ChatShapeCommand
}

internal object ChatBubbleShapePaths {
    fun commands(shape: ChatBubbleShape, width: Float, height: Float): List<ChatShapeCommand> {
        val w = max(width, 82f)
        val h = max(height, 38f)
        val mid = h / 2f
        return when (shape) {
            ChatBubbleShape.MemoV1 -> listOf(
                ChatShapeCommand.MoveTo(0f, 8f),
                ChatShapeCommand.QuadTo(0f, 0f, 8f, 0f),
                ChatShapeCommand.LineTo(w - 15f, 0f),
                ChatShapeCommand.LineTo(w - 1f, 14f),
                ChatShapeCommand.QuadTo(w, 15f, w, 17f),
                ChatShapeCommand.LineTo(w, h - 9f),
                ChatShapeCommand.QuadTo(w, h, w - 9f, h),
                ChatShapeCommand.LineTo(8f, h),
                ChatShapeCommand.QuadTo(0f, h, 0f, h - 8f),
                ChatShapeCommand.Close,
            )
            ChatBubbleShape.LotusV1 -> listOf(
                ChatShapeCommand.MoveTo(0f, 16f),
                ChatShapeCommand.CubicTo(3f, 8f, 10f, 0f, 19f, 0f),
                ChatShapeCommand.LineTo(w - 13f, 0f),
                ChatShapeCommand.QuadTo(w - 4f, 4f, w, 2f),
                ChatShapeCommand.CubicTo(w - 2f, 6f, w - 1f, 12f, w - 5f, 18f),
                ChatShapeCommand.LineTo(w - 10f, h - 10f),
                ChatShapeCommand.CubicTo(w - 13f, h - 2f, w - 20f, h, w - 31f, h),
                ChatShapeCommand.CubicTo(w * .55f, h - 7f, 33f, h, 20f, h - 3f),
                ChatShapeCommand.CubicTo(5f, h, 0f, h - 9f, 0f, 16f),
                ChatShapeCommand.Close,
            )
            ChatBubbleShape.CatV1 -> listOf(
                ChatShapeCommand.MoveTo(0f, 20f),
                ChatShapeCommand.QuadTo(0f, 15f, 4f, 11f),
                ChatShapeCommand.LineTo(8f, 2f),
                ChatShapeCommand.QuadTo(10f, 0f, 13f, 2f),
                ChatShapeCommand.LineTo(21f, 9f),
                ChatShapeCommand.LineTo(33f, 9f),
                ChatShapeCommand.LineTo(41f, 2f),
                ChatShapeCommand.QuadTo(44f, 0f, 46f, 2f),
                ChatShapeCommand.LineTo(50f, 9f),
                ChatShapeCommand.LineTo(w - 12f, 9f),
                ChatShapeCommand.CubicTo(w - 5f, 9f, w, 14f, w, 21f),
                ChatShapeCommand.LineTo(w, h - 11f),
                ChatShapeCommand.QuadTo(w, h, w - 11f, h),
                ChatShapeCommand.LineTo(11f, h),
                ChatShapeCommand.QuadTo(0f, h, 0f, h - 11f),
                ChatShapeCommand.Close,
            )
            ChatBubbleShape.FishV1 -> listOf(
                ChatShapeCommand.MoveTo(0f, mid),
                ChatShapeCommand.CubicTo(0f, 9f, 7f, 0f, 16f, 0f),
                ChatShapeCommand.LineTo(w - 40f, 0f),
                ChatShapeCommand.QuadTo(w - 26f, 0f, w - 20f, mid - 9f),
                ChatShapeCommand.LineTo(w - 4f, mid - 16f),
                ChatShapeCommand.QuadTo(w, mid - 17f, w, mid - 12f),
                ChatShapeCommand.LineTo(w - 6f, mid),
                ChatShapeCommand.LineTo(w, mid + 12f),
                ChatShapeCommand.QuadTo(w, mid + 17f, w - 4f, mid + 16f),
                ChatShapeCommand.LineTo(w - 20f, mid + 9f),
                ChatShapeCommand.QuadTo(w - 26f, h, w - 40f, h),
                ChatShapeCommand.LineTo(16f, h),
                ChatShapeCommand.CubicTo(7f, h, 0f, h - 9f, 0f, mid),
                ChatShapeCommand.Close,
            )
        }
    }

    fun build(shape: ChatBubbleShape, width: Float, height: Float): Path {
        val path = Path()
        for (command in commands(shape, width, height)) {
            when (command) {
                is ChatShapeCommand.MoveTo -> path.moveTo(command.x, command.y)
                is ChatShapeCommand.LineTo -> path.lineTo(command.x, command.y)
                is ChatShapeCommand.QuadTo -> path.quadTo(command.controlX, command.controlY, command.x, command.y)
                is ChatShapeCommand.CubicTo -> path.cubicTo(
                    command.control1X,
                    command.control1Y,
                    command.control2X,
                    command.control2Y,
                    command.x,
                    command.y,
                )
                ChatShapeCommand.Close -> path.close()
            }
        }
        return path
    }
}


internal data class ChatManagedBubbleLayout(
    val paddingTopDp: Int,
    val paddingRightDp: Int,
    val paddingBottomDp: Int,
    val paddingLeftDp: Int,
    val stickerSizeDp: Int,
    val stickerMarginDp: Int,
    val stickerBottomDp: Int,
    val stickerOnStart: Boolean,
    val minWidthDp: Int,
)

internal fun managedBubbleLayout(
    shape: ChatBubbleShape?,
    own: Boolean,
    hasMotif: Boolean,
    hasImage: Boolean,
): ChatManagedBubbleLayout {
    val padding = when (shape) {
        ChatBubbleShape.MemoV1 -> intArrayOf(11, 46, 10, 30)
        ChatBubbleShape.LotusV1 -> intArrayOf(12, 49, 13, 17)
        ChatBubbleShape.CatV1 -> intArrayOf(16, 46, 9, 17)
        ChatBubbleShape.FishV1 -> intArrayOf(11, 35, 10, 44)
        null -> intArrayOf(8, 12, 8, 12)
    }
    var left = padding[3]
    var right = padding[1]
    if (own) {
        val swap = left
        left = right
        right = swap
    }
    if (shape == null && (hasMotif || hasImage)) {
        if (own) left = 30 else right = 30
    }
    val stickerOnStart = if (shape == ChatBubbleShape.FishV1) !own else own
    val stickerMargin = when (shape) {
        ChatBubbleShape.FishV1 -> 7
        ChatBubbleShape.LotusV1 -> 12
        null -> 7
        else -> 9
    }
    return ChatManagedBubbleLayout(
        paddingTopDp = padding[0],
        paddingRightDp = right,
        paddingBottomDp = padding[2],
        paddingLeftDp = left,
        stickerSizeDp = if (shape != null) 34 else 15,
        stickerMarginDp = stickerMargin,
        stickerBottomDp = if (shape != null) 1 else 6,
        stickerOnStart = stickerOnStart,
        minWidthDp = if (shape != null) 82 else 0,
    )
}

internal class ChatManagedBubbleDrawable(
    private val style: ChatBubbleStyle,
    private val dark: Boolean,
    private val own: Boolean,
    private val density: Float,
) : Drawable() {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.style = Paint.Style.STROKE }
    private val decorationPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val motifPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val leafFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val leafStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bellRing = RectF()
    private val pawPaints = ArrayList<Pair<RectF, Paint>>()
    private val motifSize = 15f * density
    private val holePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val contour = Path()
    private val shadowContour = Path()
    private val motifPath = Path()
    private val catBar = RectF()
    private val catNub = Path()
    private val fishEye = RectF()
    private val mirrorMatrix = Matrix()

    private val palette = style.palette(dark)
    private val fillColor = if (own) palette.ownBackground else palette.background
    private val radiusPx = style.radius * density
    private val borderPx = style.borderWidth * density
    private val smallPx = 5f * density
    private val shadowColor = colorMix(palette.border, 0.27f, Color.TRANSPARENT)
    private val memoLineColor = Color.argb(
        (255 * 0.18f).roundToInt(),
        Color.red(palette.border),
        Color.green(palette.border),
        Color.blue(palette.border),
    )
    private val hasShape = style.shape != null

    private var boxWidth = 0f
    private var boxHeight = 0f
    private var measured = false

    init {
        buildMotif()
        mirrorMatrix.setScale(-1f, 1f)
    }

    override fun onBoundsChange(bounds: Rect) {
        boxWidth = bounds.width().toFloat()
        boxHeight = bounds.height().toFloat()
        measured = boxWidth > 0f && boxHeight > 0f
        if (measured) rebuild()
    }

    private fun rebuild() {
        val shape = style.shape
        contour.reset()
        when (shape) {
            null -> {
                val inset = borderPx / 2f
                val radii = if (own) {
                    floatArrayOf(radiusPx, radiusPx, smallPx, smallPx, radiusPx, radiusPx, radiusPx, radiusPx)
                } else {
                    floatArrayOf(smallPx, smallPx, radiusPx, radiusPx, radiusPx, radiusPx, radiusPx, radiusPx)
                }
                contour.addRoundRect(
                    inset,
                    inset,
                    boxWidth - inset,
                    boxHeight - inset,
                    radii,
                    Path.Direction.CW,
                )
                fillPaint.shader = null
                fillPaint.color = fillColor
            }
            else -> {
                val built = ChatBubbleShapePaths.build(shape, boxWidth, boxHeight)
                contour.set(built)
                val highlight = if (dark) palette.border else Color.WHITE
                fillPaint.shader = LinearGradient(
                    0f,
                    0f,
                    boxWidth * 0.2f,
                    boxHeight,
                    colorMix(fillColor, 0.72f, highlight),
                    fillColor,
                    Shader.TileMode.CLAMP,
                )
                catBar.set(9f * density, 5f * density, 45f * density, 10f * density)
                val cx = boxWidth - 15f * density
                val cy = boxHeight / 2f
                fishEye.set(cx - 3f * density, cy - 8.5f * density, cx + 3f * density, cy + 8.5f * density)
            }
        }
        shadowContour.set(contour)
        if (own) {
            mirrorMatrix.postTranslate(boxWidth, 0f)
            shadowContour.transform(mirrorMatrix)
            mirrorMatrix.setScale(-1f, 1f)
        }
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        if (!measured) return
        if (hasShape) {
            if (style.shadow) {
                decorationPaint.color = shadowColor
                decorationPaint.style = Paint.Style.FILL
                canvas.save()
                canvas.translate(0f, 2f * density)
                canvas.drawPath(shadowContour, decorationPaint)
                canvas.restore()
            }
            canvas.save()
            if (own) canvas.scale(-1f, 1f, boxWidth / 2f, boxHeight / 2f)
            canvas.drawPath(contour, fillPaint)
            drawStroke(canvas)
            drawShapeDetails(canvas)
            canvas.restore()
            return
        }
        canvas.drawPath(contour, fillPaint)
        if (style.shadow) {
            decorationPaint.color = shadowColor
            decorationPaint.style = Paint.Style.FILL
            canvas.save()
            canvas.clipPath(contour)
            canvas.drawRect(0f, boxHeight - 2f * density, boxWidth, boxHeight, decorationPaint)
            canvas.restore()
        }
        drawStroke(canvas)
        drawMotif(canvas)
    }

    private fun drawStroke(canvas: Canvas) {
        if (borderPx <= 0f) return
        strokePaint.color = palette.border
        strokePaint.strokeWidth = borderPx
        canvas.drawPath(contour, strokePaint)
    }

    private fun drawShapeDetails(canvas: Canvas) {
        canvas.save()
        canvas.clipPath(contour)
        when (style.shape) {
            ChatBubbleShape.MemoV1 -> {
                decorationPaint.color = memoLineColor
                decorationPaint.style = Paint.Style.STROKE
                decorationPaint.strokeWidth = density
                var y = 15.5f * density
                while (y < boxHeight) {
                    canvas.drawLine(0f, y, boxWidth, y, decorationPaint)
                    y += 16f * density
                }
                holePaint.color = palette.border
                var holeY = 12f * density
                while (holeY < boxHeight - 10f * density) {
                    canvas.drawCircle(12f * density, holeY, 1.5f * density, holePaint)
                    holeY += 12f * density
                }
            }
            ChatBubbleShape.LotusV1 -> {
                decorationPaint.color = palette.border
                decorationPaint.alpha = (255 * 0.56f).roundToInt()
                decorationPaint.style = Paint.Style.STROKE
                decorationPaint.strokeWidth = density
                canvas.save()
                canvas.rotate(
                    31f,
                    (boxWidth - 12f * density),
                    10.5f * density,
                )
                canvas.drawLine(
                    boxWidth - 18f * density,
                    3f * density,
                    boxWidth - 18f * density,
                    18f * density,
                    decorationPaint,
                )
                canvas.restore()
                decorationPaint.alpha = 255
            }
            ChatBubbleShape.CatV1 -> {
                decorationPaint.color = colorMix(palette.border, 0.38f, Color.TRANSPARENT)
                decorationPaint.style = Paint.Style.FILL
                val corner = 2.5f * density
                catBar.set(9f * density, 5f * density, 14f * density, 10f * density)
                catNub.reset()
                catNub.addRoundRect(
                    catBar,
                    floatArrayOf(corner, corner, 0f, 0f, 0f, 0f, corner, corner),
                    Path.Direction.CW,
                )
                canvas.drawPath(catNub, decorationPaint)
                catBar.set(40f * density, 5f * density, 45f * density, 10f * density)
                catNub.reset()
                catNub.addRoundRect(
                    catBar,
                    floatArrayOf(0f, 0f, corner, corner, corner, corner, 0f, 0f),
                    Path.Direction.CW,
                )
                canvas.drawPath(catNub, decorationPaint)
            }
            ChatBubbleShape.FishV1 -> {
                decorationPaint.color = palette.border
                decorationPaint.style = Paint.Style.STROKE
                decorationPaint.strokeWidth = density
                canvas.drawArc(fishEye, -90f, 180f, false, decorationPaint)
            }
            null -> Unit
        }
        canvas.restore()
    }
    private fun drawMotif(canvas: Canvas) {
        if (style.motif == ChatBubbleMotif.None || style.imageUrl.isNotEmpty()) return
        val size = motifSize
        val margin = 7f * density
        val left = if (own) margin else boxWidth - margin - size
        val top = boxHeight - 6f * density - size
        canvas.save()
        canvas.translate(left, top)
        when (style.motif) {
            ChatBubbleMotif.Paw -> drawPaw(canvas)
            ChatBubbleMotif.Leaf -> drawLeaf(canvas)
            ChatBubbleMotif.Bell -> drawBell(canvas)
            ChatBubbleMotif.Fish -> {
                motifPaint.color = palette.ink
                motifPaint.style = Paint.Style.FILL
                canvas.drawPath(motifPath, motifPaint)
            }
            ChatBubbleMotif.None -> Unit
        }
        canvas.restore()
    }

    private fun drawPaw(canvas: Canvas) {
        for ((rect, paint) in pawPaints) {
            canvas.drawOval(rect, paint)
        }
    }

    private fun drawLeaf(canvas: Canvas) {
        val scale = 0.85f
        canvas.save()
        canvas.scale(scale, scale, motifSize / 2f, motifSize / 2f)
        canvas.drawPath(motifPath, leafFillPaint)
        canvas.drawPath(motifPath, leafStrokePaint)
        canvas.restore()
    }

    private fun drawBell(canvas: Canvas) {
        canvas.drawOval(bellRing, bellPaint)
        motifPaint.color = palette.ink
        motifPaint.style = Paint.Style.FILL
        canvas.drawCircle(motifSize * 0.5f, motifSize * 0.87f, motifSize * 1.6f / 15f, motifPaint)
    }

    private fun buildMotif() {
        motifPath.reset()
        val size = motifSize
        when (style.motif) {
            ChatBubbleMotif.Paw -> {
                val dots = arrayOf(
                    0.18f to 0.43f, 0.39f to 0.25f, 0.64f to 0.25f, 0.85f to 0.43f,
                )
                pawPaints.clear()
                for ((x, y) in dots) {
                    val rect = RectF(
                        x * size - 2f / 15f * size,
                        y * size - 2.5f / 15f * size,
                        x * size + 2f / 15f * size,
                        y * size + 2.5f / 15f * size,
                    )
                    pawPaints += rect to ellipsePaint(
                        centerX = rect.centerX(),
                        centerY = rect.centerY(),
                        radiusX = rect.width() / 2f,
                        radiusY = rect.height() / 2f,
                    )
                }
                val pad = RectF(
                    0.52f * size - 4.6f / 15f * size,
                    0.69f * size - 3.4f / 15f * size,
                    0.52f * size + 4.6f / 15f * size,
                    0.69f * size + 3.4f / 15f * size,
                )
                pawPaints += pad to ellipsePaint(
                    centerX = pad.centerX(),
                    centerY = pad.centerY(),
                    radiusX = pad.width() / 2f,
                    radiusY = pad.height() / 2f,
                )
            }
            ChatBubbleMotif.Leaf -> {
                motifPath.addRoundRect(
                    0f,
                    0f,
                    size,
                    size,
                    floatArrayOf(
                        size * 0.67f, size * 0.67f,
                        size * 0.18f, size * 0.18f,
                        size * 0.64f, size * 0.64f,
                        size * 0.18f, size * 0.18f,
                    ),
                    Path.Direction.CW,
                )
                leafFillPaint.shader = angleGradient(
                    width = size,
                    height = size,
                    degrees = 144f,
                    colors = intArrayOf(
                        alphaOf(palette.ink, 0.23f),
                        palette.ink,
                        alphaOf(palette.ink, 0.23f),
                    ),
                    positions = floatArrayOf(0.47f, 0.48f, 0.56f),
                )
                leafStrokePaint.color = palette.ink
                leafStrokePaint.style = Paint.Style.STROKE
                leafStrokePaint.strokeWidth = density
            }
            ChatBubbleMotif.Bell -> {
                bellRing.set(
                    size * 0.5f - size * 5f / 15f,
                    size * 0.44f - size * 5f / 15f,
                    size * 0.5f + size * 5f / 15f,
                    size * 0.44f + size * 5f / 15f,
                )
                bellPaint.shader = ellipseShader(
                    centerX = 0f,
                    centerY = 0f,
                    radiusX = size * 5f / 15f,
                    radiusY = size * 5f / 15f,
                    colors = intArrayOf(
                        alphaOf(palette.ink, 0.46f),
                        alphaOf(palette.ink, 0.46f),
                        palette.ink,
                        palette.ink,
                        Color.TRANSPARENT,
                    ),
                    positions = floatArrayOf(0f, 0.80f, 0.82f, 0.98f, 1f),
                    translateX = size * 0.5f,
                    translateY = size * 0.44f,
                )
            }
            ChatBubbleMotif.Fish -> {
                val points = floatArrayOf(
                    0.03f, 0.48f, 0.15f, 0.29f, 0.38f, 0.18f, 0.55f, 0.22f, 0.71f, 0.42f,
                    0.97f, 0.25f, 0.87f, 0.52f, 0.97f, 0.78f, 0.72f, 0.62f, 0.54f, 0.81f,
                    0.34f, 0.84f, 0.14f, 0.71f,
                )
                var index = 0
                while (index < points.size) {
                    val x = points[index] * size
                    val y = points[index + 1] * size
                    if (index == 0) motifPath.moveTo(x, y) else motifPath.lineTo(x, y)
                    index += 2
                }
                motifPath.close()
            }
            ChatBubbleMotif.None -> Unit
        }
    }

    private fun ellipsePaint(
        centerX: Float,
        centerY: Float,
        radiusX: Float,
        radiusY: Float,
    ): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = ellipseShader(
            centerX = centerX,
            centerY = centerY,
            radiusX = radiusX,
            radiusY = radiusY,
        )
    }

    private fun ellipseShader(
        centerX: Float,
        centerY: Float,
        radiusX: Float,
        radiusY: Float,
        colors: IntArray = intArrayOf(palette.ink, palette.ink, Color.TRANSPARENT),
        positions: FloatArray = floatArrayOf(0f, 0.9f, 1f),
        translateX: Float = centerX,
        translateY: Float = centerY,
    ): Shader {
        val shader = RadialGradient(0f, 0f, radiusX, colors, positions, Shader.TileMode.CLAMP)
        val matrix = Matrix().apply {
            setScale(1f, radiusY / radiusX)
            postTranslate(translateX, translateY)
        }
        shader.setLocalMatrix(matrix)
        return shader
    }

    private fun angleGradient(
        width: Float,
        height: Float,
        degrees: Float,
        colors: IntArray,
        positions: FloatArray,
    ): Shader {
        val radians = Math.toRadians(degrees.toDouble())
        val dx = Math.sin(radians).toFloat()
        val dy = -Math.cos(radians).toFloat()
        val length = kotlin.math.abs(width * dx) + kotlin.math.abs(height * dy)
        val centerX = width / 2f
        val centerY = height / 2f
        return LinearGradient(
            centerX - dx * length / 2f,
            centerY - dy * length / 2f,
            centerX + dx * length / 2f,
            centerY + dy * length / 2f,
            colors,
            positions,
            Shader.TileMode.CLAMP,
        )
    }

    private fun alphaOf(color: Int, fraction: Float): Int = Color.argb(
        (Color.alpha(color) * fraction).roundToInt(),
        Color.red(color),
        Color.green(color),
        Color.blue(color),
    )
    override fun setAlpha(alpha: Int) {
        fillPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fillPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
