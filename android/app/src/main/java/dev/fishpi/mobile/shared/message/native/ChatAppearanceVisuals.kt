package dev.fishpi.mobile.shared.message.native

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.drawable.Drawable
import coil3.asDrawable
import dev.fishpi.mobile.R
import dev.fishpi.mobile.data.ChatAppearanceSnapshot
import kotlin.math.max
import kotlin.math.roundToInt

/** CSS color-mix(in srgb, color fraction, base)，按预乘 alpha 合成。 */
internal fun colorMix(color: Int, fraction: Float, base: Int): Int {
    val p = fraction.coerceIn(0f, 1f)
    val alphaA = Color.alpha(color) / 255f * p
    val alphaB = Color.alpha(base) / 255f * (1f - p)
    val outAlpha = alphaA + alphaB
    if (outAlpha <= 0f) return Color.TRANSPARENT
    val r = ((Color.red(color) * alphaA + Color.red(base) * alphaB) / outAlpha).roundToInt()
    val g = ((Color.green(color) * alphaA + Color.green(base) * alphaB) / outAlpha).roundToInt()
    val b = ((Color.blue(color) * alphaA + Color.blue(base) * alphaB) / outAlpha).roundToInt()
    return Color.argb((outAlpha * 255f).roundToInt(), r.coerceAtMost(255), g.coerceAtMost(255), b.coerceAtMost(255))
}

internal enum class ChatBadgeStyle { Box, Logo, Glyph, Flower }

internal data class ChatBadgeVisual(
    val style: ChatBadgeStyle,
    val sizePx: Float,
    val anchorRightPx: Float,
    val anchorBottomPx: Float,
    val borderPx: Float = 0f,
    val radiusPx: Float = 0f,
    val borderColor: Int = Color.TRANSPARENT,
    val fillColor: Int = Color.TRANSPARENT,
    val logo: Bitmap? = null,
    val glyph: String? = null,
    val glyphColor: Int = Color.TRANSPARENT,
    val ringColor: Int = Color.TRANSPARENT,
    val ringPx: Float = 0f,
)

internal class ChatBubbleVisual(
    val background: Drawable,
    val paddingLeft: Int,
    val paddingTop: Int,
    val paddingRight: Int,
    val paddingBottom: Int,
    val reserveLeft: Int,
    val reserveRight: Int,
    val reserveBottom: Int,
    val textColor: Int,
    val badge: ChatBadgeVisual?,
    val badgeOnLeft: Boolean,
    val minWidth: Int = 0,
    val sticker: ChatBubbleSticker? = null,
    val sakura: Boolean = false,
)

internal data class ChatBubbleSticker(
    val url: String,
    val sizePx: Int,
    val marginStartPx: Int,
    val marginBottomPx: Int,
    val onStart: Boolean,
)

internal class ChatAvatarFrameVisual(
    val background: Drawable,
    val borderPx: Float,
    val paddingPx: Float,
    val badge: ChatBadgeVisual?,
    val sakura: Boolean = false,
    val innerBackground: Drawable? = null,
)

/** 头像框在给定内头像尺寸下的环宽与外框尺寸；装饰外溢不占布局。 */
internal data class ChatAvatarFrameGeometry(
    val ringPx: Int,
    val frameSizePx: Int,
)

internal fun ChatAvatarFrameVisual.geometry(innerSizePx: Int): ChatAvatarFrameGeometry {
    val ring = (paddingPx + borderPx).roundToInt()
    return ChatAvatarFrameGeometry(
        ringPx = ring,
        frameSizePx = innerSizePx + ring * 2,
    )
}

internal class ChatBubbleBoxDrawable(
    private val topColor: Int,
    private val bottomColor: Int,
    private val angleDegrees: Float,
    private val borderColor: Int,
    private val borderWidthPx: Float,
    private val cornerRadiiPx: FloatArray,
    private val shadowOffsetPx: Float,
    private val shadowColor: Int,
    private val rightReservePx: Float,
    private val bottomReservePx: Float,
    private val radialCenterX: Float = -1f,
    private val radialCenterY: Float = -1f,
    private val radialColor: Int = Color.TRANSPARENT,
    private val radialStop: Float = 1f,
    private val insetLineColor: Int = Color.TRANSPARENT,
    private val insetLinePx: Float = 0f,
) : Drawable() {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val radialPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val insetPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = shadowColor }
    private val box = RectF()
    private val path = Path()
    private val shadowPath = Path()

    override fun onBoundsChange(bounds: Rect) {
        box.set(
            bounds.left + borderWidthPx / 2f,
            bounds.top + borderWidthPx / 2f,
            bounds.right - rightReservePx - borderWidthPx / 2f,
            bounds.bottom - bottomReservePx - borderWidthPx / 2f,
        )
        path.reset()
        path.addRoundRect(box, cornerRadiiPx, Path.Direction.CW)
        shadowPath.reset()
        shadowPath.addRoundRect(
            RectF(box.left, box.top + shadowOffsetPx, box.right, box.bottom + shadowOffsetPx),
            cornerRadiiPx,
            Path.Direction.CW,
        )
        val radians = Math.toRadians(angleDegrees.toDouble())
        val dx = Math.sin(radians).toFloat()
        val dy = -Math.cos(radians).toFloat()
        val length = kotlin.math.abs(box.width() * dx) + kotlin.math.abs(box.height() * dy)
        val cx = box.centerX()
        val cy = box.centerY()
        fillPaint.shader = LinearGradient(
            cx - dx * length / 2f,
            cy - dy * length / 2f,
            cx + dx * length / 2f,
            cy + dy * length / 2f,
            intArrayOf(topColor, bottomColor),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        borderPaint.color = borderColor
        borderPaint.strokeWidth = borderWidthPx
        if (radialColor != Color.TRANSPARENT && radialCenterX >= 0f && box.width() > 0f && box.height() > 0f) {
            val rCx = box.left + box.width() * radialCenterX
            val rCy = box.top + box.height() * radialCenterY
            val rx = max(rCx - box.left, box.right - rCx)
            val ry = max(rCy - box.top, box.bottom - rCy)
            val shader = RadialGradient(
                0f,
                0f,
                1f,
                intArrayOf(radialColor, Color.TRANSPARENT),
                floatArrayOf(0f, radialStop),
                Shader.TileMode.CLAMP,
            )
            val matrix = Matrix()
            matrix.setScale(1f / rx, 1f / ry)
            matrix.postTranslate(-rCx / rx, -rCy / ry)
            shader.setLocalMatrix(matrix)
            radialPaint.shader = shader
        } else {
            radialPaint.shader = null
        }
        insetPaint.color = insetLineColor
    }

    override fun draw(canvas: Canvas) {
        if (shadowColor != Color.TRANSPARENT && shadowOffsetPx > 0f) canvas.drawPath(shadowPath, shadowPaint)
        canvas.drawPath(path, fillPaint)
        if (radialPaint.shader != null) canvas.drawRect(box, radialPaint)
        if (insetLinePx > 0f && insetLineColor != Color.TRANSPARENT) {
            val save = canvas.save()
            canvas.clipPath(path)
            canvas.drawRect(box.left, box.bottom - insetLinePx, box.right, box.bottom, insetPaint)
            canvas.restoreToCount(save)
        }
        if (borderWidthPx > 0f) canvas.drawPath(path, borderPaint)
    }

    override fun setAlpha(alpha: Int) {
        fillPaint.alpha = alpha
        radialPaint.alpha = alpha
        borderPaint.alpha = alpha
        insetPaint.alpha = alpha
        shadowPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        fillPaint.colorFilter = colorFilter
        radialPaint.colorFilter = colorFilter
        borderPaint.colorFilter = colorFilter
        insetPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
}

internal class ChatAvatarFrameDrawable(
    private val gradientColors: IntArray,
    private val gradientPositions: FloatArray,
    private val angleDegrees: Float,
    private val conic: Boolean = false,
    private val borderColor: Int = Color.TRANSPARENT,
    private val borderWidthPx: Float = 0f,
    private val cornerRadiusPx: Float,
    private val ringColor: Int = Color.TRANSPARENT,
    private val ringPx: Float = 0f,
    private val outlineColor: Int = Color.TRANSPARENT,
    private val outlineStrokePx: Float = 0f,
    private val outlineOffsetPx: Float = 0f,
    private val innerDashColor: Int = Color.TRANSPARENT,
    private val innerDashStrokePx: Float = 0f,
    private val innerDashInsetPx: Float = 0f,
    private val softShadowColor: Int = Color.TRANSPARENT,
    private val softShadowOffsetPx: Float = 0f,
    private val softShadowBlurPx: Float = 0f,
    private val glowColor: Int = Color.TRANSPARENT,
    private val glowBlurPx: Float = 0f,
) : Drawable() {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val box = RectF()
    private val path = Path()

    override fun onBoundsChange(bounds: Rect) {
        val halfBorder = borderWidthPx / 2f
        box.set(
            bounds.left + halfBorder,
            bounds.top + halfBorder,
            bounds.right - halfBorder,
            bounds.bottom - halfBorder,
        )
        path.reset()
        path.addRoundRect(box, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)
        if (conic) {
            fillPaint.shader = SweepGradient(
                box.centerX(),
                box.centerY(),
                gradientColors,
                gradientPositions,
            )
        } else {
            val radians = Math.toRadians(angleDegrees.toDouble())
            val dx = Math.sin(radians).toFloat()
            val dy = -Math.cos(radians).toFloat()
            val length = kotlin.math.abs(box.width() * dx) + kotlin.math.abs(box.height() * dy)
            val cx = box.centerX()
            val cy = box.centerY()
            fillPaint.shader = LinearGradient(
                cx - dx * length / 2f,
                cy - dy * length / 2f,
                cx + dx * length / 2f,
                cy + dy * length / 2f,
                gradientColors,
                gradientPositions,
                Shader.TileMode.CLAMP,
            )
        }
        borderPaint.color = borderColor
        borderPaint.strokeWidth = borderWidthPx
        shadowPaint.color = softShadowColor
        shadowPaint.maskFilter = if (softShadowBlurPx > 0f) {
            BlurMaskFilter(softShadowBlurPx / 2f, BlurMaskFilter.Blur.NORMAL)
        } else {
            null
        }
        glowPaint.color = glowColor
        glowPaint.maskFilter = if (glowBlurPx > 0f) {
            BlurMaskFilter(glowBlurPx / 2f, BlurMaskFilter.Blur.NORMAL)
        } else {
            null
        }
        ringPaint.color = ringColor
        outlinePaint.color = outlineColor
        outlinePaint.strokeWidth = outlineStrokePx
        if (outlineStrokePx > 0f) {
            outlinePaint.pathEffect = DashPathEffect(
                floatArrayOf(outlineStrokePx * 3f, outlineStrokePx * 2f),
                0f,
            )
        }
        innerPaint.color = innerDashColor
        innerPaint.strokeWidth = innerDashStrokePx
        if (innerDashStrokePx > 0f) {
            innerPaint.pathEffect = DashPathEffect(
                floatArrayOf(innerDashStrokePx * 3f, innerDashStrokePx * 2f),
                0f,
            )
        }
    }

    override fun draw(canvas: Canvas) {
        if (glowColor != Color.TRANSPARENT) {
            canvas.drawRoundRect(box, cornerRadiusPx, cornerRadiusPx, glowPaint)
        }
        if (softShadowColor != Color.TRANSPARENT) {
            val shadow = RectF(box).apply {
                inset(-softShadowBlurPx / 2f, -softShadowBlurPx / 2f)
                offset(0f, softShadowOffsetPx)
            }
            canvas.drawRoundRect(
                shadow,
                cornerRadiusPx + softShadowBlurPx / 2f,
                cornerRadiusPx + softShadowBlurPx / 2f,
                shadowPaint,
            )
        }
        if (ringPx > 0f && ringColor != Color.TRANSPARENT) {
            val ring = RectF(box).apply { inset(-ringPx, -ringPx) }
            canvas.drawRoundRect(ring, cornerRadiusPx + ringPx, cornerRadiusPx + ringPx, ringPaint)
        }
        canvas.drawPath(path, fillPaint)
        if (borderWidthPx > 0f) canvas.drawPath(path, borderPaint)
        if (outlineColor != Color.TRANSPARENT && outlineStrokePx > 0f) {
            val expandBy = outlineOffsetPx + outlineStrokePx / 2f
            val outline = RectF(box).apply { inset(-expandBy, -expandBy) }
            val outlineRadius = cornerRadiusPx + outlineOffsetPx
            canvas.drawRoundRect(
                outline,
                outlineRadius,
                outlineRadius,
                outlinePaint,
            )
        }
        if (innerDashColor != Color.TRANSPARENT && innerDashStrokePx > 0f) {
            val radius = box.width() / 2f + innerDashInsetPx - innerDashStrokePx / 2f
            canvas.drawCircle(box.centerX(), box.centerY(), radius, innerPaint)
        }
    }

    override fun setAlpha(alpha: Int) {
        fillPaint.alpha = alpha
        borderPaint.alpha = alpha
        shadowPaint.alpha = alpha
        glowPaint.alpha = alpha
        ringPaint.alpha = alpha
        outlinePaint.alpha = alpha
        innerPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        fillPaint.colorFilter = colorFilter
        borderPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
}

internal object ChatAppearanceRenderer {
    private const val PAIPAI_ORANGE = "paipai-orange"
    private const val FISHPOND_BLUE = "fishpond-blue"
    private const val CANDY = "candy"
    private const val SEA_SODA = "sea-soda"
    private const val SUNSET_LETTER = "sunset-letter"
    private const val STAR_ROUTE = "star-route"
    private const val GOLD = "gold"
    private const val PAIPAI_STAMP = "paipai-stamp"
    private const val STAR_RING = "star-ring"
    private const val TIDE = "tide"
    private const val NIGHT_SAKURA = ChatAppearanceSupport.NightSakura
    private const val SAKURA_LETTER = ChatAppearanceSupport.SakuraLetter
    private const val SAKURA_MOON = ChatAppearanceSupport.SakuraMoon

    private data class WebTokens(val card: Int, val text: Int, val border: Int)

    private fun webTokens(dark: Boolean) = if (dark) {
        WebTokens(
            card = 0xFF282A2A.toInt(),
            text = 0xFFCBC5BA.toInt(),
            border = Color.argb(20, 255, 255, 255),
        )
    } else {
        WebTokens(
            card = 0xFFFFFFFF.toInt(),
            text = 0xFF2C2E33.toInt(),
            border = 0xFFE6E8EC.toInt(),
        )
    }

    private var logo: Bitmap? = null
    private var logoLoaded = false

    fun supportsBubble(id: String?): Boolean = ChatAppearanceSupport.supportsBubble(id)

    fun supportsFrame(id: String?): Boolean = ChatAppearanceSupport.supportsFrame(id)

    fun bubble(
        snapshot: ChatAppearanceSnapshot?,
        dark: Boolean,
        isMine: Boolean,
        resources: Resources,
        density: Float,
    ): ChatBubbleVisual? {
        if (snapshot == null || snapshot.version != 1) return null
        val tokens = webTokens(dark)
        val styleJson = snapshot.bubbleStyleJson?.takeIf { it.isNotBlank() }
            ?: ChatBubbleStylePresets.json(snapshot.bubbleId)
        ChatBubbleStyle.decode(styleJson)?.let { style ->
            return managedBubble(style, tokens, dark, isMine, density)
        }
        return when (snapshot.bubbleId) {
            PAIPAI_ORANGE -> paipaiOrangeBubble(tokens, isMine, resources, density)
            FISHPOND_BLUE -> fishpondBlueBubble(tokens, isMine, resources, density)
            CANDY -> candyBubble(tokens, density)
            SEA_SODA -> tintedBubble(0xFF8FCFDF.toInt(), "◦", tokens, isMine, density)
            SUNSET_LETTER -> tintedBubble(0xFFE9B684.toInt(), null, tokens, isMine, density)
            STAR_ROUTE -> tintedBubble(0xFFA4ADD8.toInt(), "✧", tokens, isMine, density)
            SAKURA_LETTER -> return ChatSakura.bubble(ChatSakuraStyle.Letter, isMine, dark, density)
            SAKURA_MOON -> return ChatSakura.bubble(ChatSakuraStyle.Moon, isMine, dark, density)
            NIGHT_SAKURA -> return ChatSakura.bubble(ChatSakuraStyle.Letter, isMine, dark, density)
            else -> null
        }
    }

    fun avatarFrame(
        snapshot: ChatAppearanceSnapshot?,
        dark: Boolean,
        resources: Resources,
        density: Float,
        isMine: Boolean = false,
    ): ChatAvatarFrameVisual? {
        if (snapshot == null || snapshot.version != 1) return null
        val tokens = webTokens(dark)
        return when (snapshot.frameId) {
            PAIPAI_ORANGE -> paipaiOrangeFrame(tokens, resources, density)
            FISHPOND_BLUE -> fishpondBlueFrame(tokens, resources, density)
            GOLD -> goldFrame(density)
            PAIPAI_STAMP -> paipaiStampFrame(tokens, resources, density)
            STAR_RING -> starRingFrame(tokens, density)
            TIDE -> tideFrame(tokens, density)
            SAKURA_LETTER -> return ChatSakura.frame(ChatSakuraStyle.Letter, dark, density, isMine)
            SAKURA_MOON -> return ChatSakura.frame(ChatSakuraStyle.Moon, dark, density, isMine)
            NIGHT_SAKURA -> return ChatSakura.frame(ChatSakuraStyle.Letter, dark, density, isMine)
            else -> null
        }
    }

    private fun dp(value: Int, density: Float): Int = (value * density).roundToInt()

    private fun cornerRadii(
        smallDp: Float,
        largeDp: Float,
        smallAtTopLeft: Boolean,
        density: Float,
    ): FloatArray {
        val small = smallDp * density
        val large = largeDp * density
        return if (smallAtTopLeft) {
            floatArrayOf(small, small, large, large, large, large, large, large)
        } else {
            floatArrayOf(large, large, small, small, large, large, large, large)
        }
    }

    private fun logoBadge(
        sizeDp: Float,
        radiusDp: Float,
        borderColor: Int,
        ringColor: Int = Color.TRANSPARENT,
        ringDp: Float = 0f,
        density: Float,
        resources: Resources,
    ): ChatBadgeVisual = ChatBadgeVisual(
        style = ChatBadgeStyle.Box,
        sizePx = sizeDp * density,
        anchorRightPx = 5f * density,
        anchorBottomPx = 5f * density,
        borderPx = 2f * density,
        radiusPx = radiusDp * density,
        borderColor = borderColor,
        fillColor = 0xFFF4D963.toInt(),
        logo = logo(resources),
        ringColor = ringColor,
        ringPx = ringDp * density,
    )

    private val managedSourcePattern = Regex(
        "^https://file\\.fishpi\\.cn/[^?#\\s\"<>\\\\]+\\.(?:png|jpe?g|gif|webp|bmp|avif|heic|tiff?)$",
        RegexOption.IGNORE_CASE,
    )

    internal fun managedStickerSource(url: String, shape: ChatBubbleShape?): String {
        if (shape == null) return url
        if (!managedSourcePattern.matches(url)) return url
        return "$url?imageView2/1/w/68/h/68"
    }

    private fun managedBubble(
        style: ChatBubbleStyle,
        tokens: WebTokens,
        dark: Boolean,
        isMine: Boolean,
        density: Float,
    ): ChatBubbleVisual {
        val shape = style.shape
        val palette = style.palette(dark)
        val layout = managedBubbleLayout(
            shape = shape,
            own = isMine,
            hasMotif = style.motif != ChatBubbleMotif.None,
            hasImage = style.imageUrl.isNotEmpty(),
        )
        val sticker = style.imageUrl.takeIf { it.isNotEmpty() }?.let { raw ->
            ChatBubbleSticker(
                url = managedStickerSource(raw, shape),
                sizePx = dp(layout.stickerSizeDp, density),
                marginStartPx = dp(layout.stickerMarginDp, density),
                marginBottomPx = dp(layout.stickerBottomDp, density),
                onStart = layout.stickerOnStart,
            )
        }
        return ChatBubbleVisual(
            background = ChatManagedBubbleDrawable(style, dark, isMine, density),
            paddingLeft = dp(layout.paddingLeftDp, density),
            paddingTop = dp(layout.paddingTopDp, density),
            paddingRight = dp(layout.paddingRightDp, density),
            paddingBottom = dp(layout.paddingBottomDp, density),
            reserveLeft = 0,
            reserveRight = 0,
            reserveBottom = 0,
            textColor = palette.text,
            badge = null,
            badgeOnLeft = isMine,
            minWidth = dp(layout.minWidthDp, density),
            sticker = sticker,
        )
    }

    private fun paipaiOrangeBubble(
        tokens: WebTokens,
        isMine: Boolean,
        resources: Resources,
        density: Float,
    ): ChatBubbleVisual {
        val tint = 0xFFEF9829.toInt()
        val startMix = if (isMine) 0.19f else 0.08f
        val endMix = if (isMine) 0.30f else 0.16f
        val background = ChatBubbleBoxDrawable(
            topColor = colorMix(tint, startMix, tokens.card),
            bottomColor = colorMix(tint, endMix, tokens.card),
            angleDegrees = 145f,
            borderColor = colorMix(0xFFEC9222.toInt(), 0.38f, tokens.border),
            borderWidthPx = density,
            cornerRadiiPx = cornerRadii(5f, 17f, !isMine, density),
            shadowOffsetPx = 2f * density,
            shadowColor = colorMix(0xFFA65D15.toInt(), 0.14f, Color.TRANSPARENT),
            rightReservePx = if (isMine) 0f else 5f * density,
            bottomReservePx = 5f * density,
        )
        return ChatBubbleVisual(
            background = background,
            paddingLeft = dp(if (isMine) 34 else 14, density),
            paddingTop = dp(11, density),
            paddingRight = dp(if (isMine) 14 else 34, density),
            paddingBottom = dp(11, density),
            reserveLeft = if (isMine) dp(5, density) else 0,
            reserveRight = if (isMine) 0 else dp(5, density),
            reserveBottom = dp(5, density),
            textColor = tokens.text,
            badge = logoBadge(24f, 8f, tokens.card, density = density, resources = resources),
            badgeOnLeft = isMine,
        )
    }

    private fun fishpondBlueBubble(
        tokens: WebTokens,
        isMine: Boolean,
        resources: Resources,
        density: Float,
    ): ChatBubbleVisual {
        val base = colorMix(0xFF69C0D8.toInt(), if (isMine) 0.18f else 0.07f, tokens.card)
        val overflow = dp(8, density)
        val background = ChatBubbleBoxDrawable(
            topColor = base,
            bottomColor = base,
            angleDegrees = 90f,
            borderColor = colorMix(0xFF53A7C2.toInt(), 0.40f, tokens.border),
            borderWidthPx = density,
            cornerRadiiPx = cornerRadii(5f, 17f, !isMine, density),
            shadowOffsetPx = 0f,
            shadowColor = Color.TRANSPARENT,
            rightReservePx = if (isMine) 0f else overflow.toFloat(),
            bottomReservePx = overflow.toFloat(),
            radialCenterX = if (isMine) 0f else 1f,
            radialCenterY = 1f,
            radialColor = colorMix(0xFF6AC6DB.toInt(), if (isMine) 0.28f else 0.22f, Color.TRANSPARENT),
            radialStop = 0.70f,
            insetLineColor = colorMix(0xFF5EB6CD.toInt(), 0.15f, Color.TRANSPARENT),
            insetLinePx = 3f * density,
        )
        return ChatBubbleVisual(
            background = background,
            paddingLeft = dp(if (isMine) 34 else 14, density),
            paddingTop = dp(11, density),
            paddingRight = dp(if (isMine) 14 else 34, density),
            paddingBottom = dp(13, density),
            reserveLeft = if (isMine) overflow else 0,
            reserveRight = if (isMine) 0 else overflow,
            reserveBottom = overflow,
            textColor = tokens.text,
            badge = logoBadge(
                sizeDp = 24f,
                radiusDp = 12f,
                borderColor = colorMix(0xFF75C9DF.toInt(), 0.60f, tokens.card),
                ringColor = colorMix(0xFF75C9DF.toInt(), 0.12f, Color.TRANSPARENT),
                ringDp = 3f,
                density = density,
                resources = resources,
            ),
            badgeOnLeft = isMine,
        )
    }

    private fun candyBubble(tokens: WebTokens, density: Float): ChatBubbleVisual {
        val background = ChatBubbleBoxDrawable(
            topColor = 0xFFFFF3F8.toInt(),
            bottomColor = 0xFFFFD6E7.toInt(),
            angleDegrees = 180f,
            borderColor = colorMix(tokens.text, 0.05f, Color.TRANSPARENT),
            borderWidthPx = density,
            cornerRadiiPx = FloatArray(8) { 18f * density },
            shadowOffsetPx = 6f * density,
            shadowColor = 0xFFF3A0BF.toInt(),
            rightReservePx = 0f,
            bottomReservePx = 6f * density,
        )
        return ChatBubbleVisual(
            background = background,
            paddingLeft = dp(14, density),
            paddingTop = dp(10, density),
            paddingRight = dp(14, density),
            paddingBottom = dp(10, density),
            reserveLeft = 0,
            reserveRight = 0,
            reserveBottom = dp(6, density),
            textColor = 0xFF5B2742.toInt(),
            badge = null,
            badgeOnLeft = false,
        )
    }

    private fun tintedBubble(
        tint: Int,
        glyph: String?,
        tokens: WebTokens,
        isMine: Boolean,
        density: Float,
    ): ChatBubbleVisual {
        val background = ChatBubbleBoxDrawable(
            topColor = colorMix(tint, 0.22f, tokens.card),
            bottomColor = tokens.card,
            angleDegrees = 135f,
            borderColor = colorMix(tint, 0.75f, tokens.border),
            borderWidthPx = density,
            cornerRadiiPx = cornerRadii(5f, 16f, !isMine, density),
            shadowOffsetPx = 0f,
            shadowColor = Color.TRANSPARENT,
            rightReservePx = if (isMine || glyph == null) 0f else 5f * density,
            bottomReservePx = if (glyph == null) 0f else 9f * density,
            insetLineColor = colorMix(tint, 0.24f, Color.TRANSPARENT),
            insetLinePx = 2f * density,
        )
        return ChatBubbleVisual(
            background = background,
            paddingLeft = dp(16, density),
            paddingTop = dp(10, density),
            paddingRight = dp(16, density),
            paddingBottom = dp(10, density),
            reserveLeft = if (!isMine || glyph == null) 0 else dp(5, density),
            reserveRight = if (isMine || glyph == null) 0 else dp(5, density),
            reserveBottom = if (glyph == null) 0 else dp(9, density),
            textColor = tokens.text,
            badge = glyph?.let {
                ChatBadgeVisual(
                    style = ChatBadgeStyle.Glyph,
                    sizePx = 22f * density,
                    anchorRightPx = 5f * density,
                    anchorBottomPx = 9f * density,
                    glyph = it,
                    glyphColor = tint,
                )
            },
            badgeOnLeft = isMine,
        )
    }

    private fun paipaiOrangeFrame(
        tokens: WebTokens,
        resources: Resources,
        density: Float,
    ): ChatAvatarFrameVisual {
        val background = ChatAvatarFrameDrawable(
            gradientColors = intArrayOf(
                0xFFFFF4CE.toInt(),
                0xFFEF9829.toInt(),
                0xFFC77713.toInt(),
            ),
            gradientPositions = floatArrayOf(0f, 0.62f, 1f),
            angleDegrees = 145f,
            borderColor = colorMix(0xFFD98318.toInt(), 0.65f, tokens.border),
            borderWidthPx = density,
            cornerRadiusPx = 14f * density,
            softShadowColor = colorMix(0xFF975B12.toInt(), 0.12f, Color.TRANSPARENT),
            softShadowOffsetPx = 2f * density,
            softShadowBlurPx = 5f * density,
        )
        return ChatAvatarFrameVisual(
            background = background,
            borderPx = density,
            paddingPx = 3f * density,
            badge = logoBadge(17f, 6f, tokens.card, density = density, resources = resources),
        )
    }

    private fun fishpondBlueFrame(
        tokens: WebTokens,
        resources: Resources,
        density: Float,
    ): ChatAvatarFrameVisual {
        val background = ChatAvatarFrameDrawable(
            gradientColors = intArrayOf(
                0xFFD5F6FF.toInt(),
                0xFF72C4DC.toInt(),
                0xFF4394B4.toInt(),
            ),
            gradientPositions = floatArrayOf(0f, 0.55f, 1f),
            angleDegrees = 155f,
            borderColor = colorMix(0xFF4A9FB9.toInt(), 0.55f, tokens.border),
            borderWidthPx = density,
            cornerRadiusPx = 15f * density,
            ringColor = colorMix(0xFF71BFD6.toInt(), 0.14f, Color.TRANSPARENT),
            ringPx = 3f * density,
        )
        return ChatAvatarFrameVisual(
            background = background,
            borderPx = density,
            paddingPx = 3f * density,
            badge = logoBadge(
                sizeDp = 17f,
                radiusDp = 8.5f,
                borderColor = tokens.card,
                ringColor = colorMix(0xFF71BFD6.toInt(), 0.35f, Color.TRANSPARENT),
                ringDp = 2f,
                density = density,
                resources = resources,
            ),
        )
    }

    private fun goldFrame(density: Float): ChatAvatarFrameVisual {
        val background = ChatAvatarFrameDrawable(
            gradientColors = intArrayOf(
                0xFF8A5A12.toInt(),
                0xFFF6E27A.toInt(),
                0xFFFFF8D0.toInt(),
                0xFFD4A017.toInt(),
                0xFF8A5A12.toInt(),
            ),
            gradientPositions = floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f),
            angleDegrees = 0f,
            conic = true,
            cornerRadiusPx = 8f * density,
            ringColor = 0xFFF6C85F.toInt(),
            ringPx = density,
            glowColor = Color.argb(115, 246, 200, 95),
            glowBlurPx = 12f * density,
            innerDashColor = Color.argb(179, 246, 200, 95),
            innerDashStrokePx = density,
            innerDashInsetPx = 4f * density,
        )
        return ChatAvatarFrameVisual(
            background = background,
            borderPx = 0f,
            paddingPx = 3f * density,
            badge = null,
        )
    }

    private fun paipaiStampFrame(
        tokens: WebTokens,
        resources: Resources,
        density: Float,
    ): ChatAvatarFrameVisual {
        val tint = 0xFFD9B47D.toInt()
        val background = ChatAvatarFrameDrawable(
            gradientColors = intArrayOf(colorMix(tint, 0.22f, tokens.card), tokens.card),
            gradientPositions = floatArrayOf(0f, 1f),
            angleDegrees = 135f,
            borderColor = colorMix(tint, 0.75f, tokens.border),
            borderWidthPx = density,
            cornerRadiusPx = 12f * density,
            ringColor = colorMix(tint, 0.30f, Color.TRANSPARENT),
            ringPx = 2f * density,
            outlineColor = tint,
            outlineStrokePx = density,
            outlineOffsetPx = 2f * density,
        )
        return ChatAvatarFrameVisual(
            background = background,
            borderPx = density,
            paddingPx = 3f * density,
            badge = ChatBadgeVisual(
                style = ChatBadgeStyle.Logo,
                sizePx = 17f * density,
                anchorRightPx = 5f * density,
                anchorBottomPx = 5f * density,
                logo = logo(resources),
            ),
        )
    }

    private fun starRingFrame(tokens: WebTokens, density: Float): ChatAvatarFrameVisual {
        val tint = 0xFFB8A9DB.toInt()
        val background = ChatAvatarFrameDrawable(
            gradientColors = intArrayOf(colorMix(tint, 0.22f, tokens.card), tokens.card),
            gradientPositions = floatArrayOf(0f, 1f),
            angleDegrees = 135f,
            borderColor = colorMix(tint, 0.75f, tokens.border),
            borderWidthPx = density,
            cornerRadiusPx = 12f * density,
            ringColor = colorMix(tint, 0.30f, Color.TRANSPARENT),
            ringPx = 2f * density,
        )
        return ChatAvatarFrameVisual(
            background = background,
            borderPx = density,
            paddingPx = 3f * density,
            badge = ChatBadgeVisual(
                style = ChatBadgeStyle.Glyph,
                sizePx = 22f * density,
                anchorRightPx = 5f * density,
                anchorBottomPx = 9f * density,
                glyph = "✦",
                glyphColor = tint,
            ),
        )
    }

    private fun tideFrame(tokens: WebTokens, density: Float): ChatAvatarFrameVisual {
        val tint = 0xFF75BACE.toInt()
        val background = ChatAvatarFrameDrawable(
            gradientColors = intArrayOf(colorMix(tint, 0.22f, tokens.card), tokens.card),
            gradientPositions = floatArrayOf(0f, 1f),
            angleDegrees = 135f,
            borderColor = colorMix(tint, 0.75f, tokens.border),
            borderWidthPx = density,
            cornerRadiusPx = 12f * density,
            ringColor = colorMix(tint, 0.30f, Color.TRANSPARENT),
            ringPx = 2f * density,
        )
        return ChatAvatarFrameVisual(
            background = background,
            borderPx = density,
            paddingPx = 3f * density,
            badge = null,
        )
    }

    /** 按 CSS ::after 定位绘制徽记；glyph 无字形时跳过绘制。 */
    fun drawBadge(
        canvas: Canvas,
        badge: ChatBadgeVisual,
        boxLeft: Float,
        boxTop: Float,
        boxRight: Float,
        boxBottom: Float,
        onLeft: Boolean,
        density: Float,
    ) {
        val left = if (onLeft) {
            boxLeft - badge.anchorRightPx
        } else {
            boxRight + badge.anchorRightPx - badge.sizePx
        }
        val top = boxBottom + badge.anchorBottomPx - badge.sizePx
        val right = left + badge.sizePx
        val bottom = top + badge.sizePx
        if (badge.ringPx > 0f && badge.ringColor != Color.TRANSPARENT) {
            val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = badge.ringColor }
            val ring = RectF(left, top, right, bottom).apply { inset(-badge.ringPx, -badge.ringPx) }
            canvas.drawRoundRect(
                ring,
                badge.radiusPx + badge.ringPx,
                badge.radiusPx + badge.ringPx,
                ringPaint,
            )
        }
        when (badge.style) {
            ChatBadgeStyle.Glyph -> {
                val glyph = badge.glyph ?: return
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = badge.sizePx
                    color = badge.glyphColor
                    textAlign = Paint.Align.CENTER
                }
                if (!paint.hasGlyph(glyph)) return
                val metrics = paint.fontMetrics
                val centerY = top + badge.sizePx / 2f
                canvas.drawText(
                    glyph,
                    left + badge.sizePx / 2f,
                    centerY - (metrics.ascent + metrics.descent) / 2f,
                    paint,
                )
            }
            ChatBadgeStyle.Flower -> Unit
            ChatBadgeStyle.Logo -> {
                val image = badge.logo ?: return
                drawContain(canvas, image, RectF(left, top, right, bottom))
            }
            ChatBadgeStyle.Box -> {
                val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = badge.fillColor }
                canvas.drawRoundRect(left, top, right, bottom, badge.radiusPx, badge.radiusPx, fill)
                val image = badge.logo
                if (image != null) {
                    val inner = RectF(
                        left + badge.borderPx,
                        top + badge.borderPx,
                        right - badge.borderPx,
                        bottom - badge.borderPx,
                    )
                    drawContain(canvas, image, inner)
                }
                if (badge.borderPx > 0f && badge.borderColor != Color.TRANSPARENT) {
                    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = badge.borderColor
                        style = Paint.Style.STROKE
                        strokeWidth = badge.borderPx
                    }
                    canvas.drawRoundRect(
                        left + badge.borderPx / 2f,
                        top + badge.borderPx / 2f,
                        right - badge.borderPx / 2f,
                        bottom - badge.borderPx / 2f,
                        badge.radiusPx - badge.borderPx / 2f,
                        badge.radiusPx - badge.borderPx / 2f,
                        stroke,
                    )
                }
            }
        }
    }

    private fun drawContain(canvas: Canvas, image: Bitmap, target: RectF) {
        val scale = minOf(target.width() / image.width, target.height() / image.height)
        val width = image.width * scale
        val height = image.height * scale
        val rect = RectF(
            target.centerX() - width / 2f,
            target.centerY() - height / 2f,
            target.centerX() + width / 2f,
            target.centerY() + height / 2f,
        )
        canvas.drawBitmap(image, null, rect, Paint(Paint.FILTER_BITMAP_FLAG))
    }

    private fun logo(resources: Resources): Bitmap? {
        if (!logoLoaded) loadLogo(resources)
        return logo
    }

    fun loadLogo(resources: Resources) {
        if (logoLoaded) return
        logoLoaded = true
        logo = runCatching {
            BitmapFactory.decodeResource(resources, R.drawable.chat_appearance_paipai_logo)
        }.getOrNull()
    }

    internal fun brandLogo(resources: Resources): Bitmap? = logo(resources)
}

internal fun ChatBubbleVisual.paintBadge(canvas: Canvas, density: Float) {
    val badge = badge ?: return
    ChatAppearanceRenderer.drawBadge(
        canvas = canvas,
        badge = badge,
        boxLeft = reserveLeft.toFloat(),
        boxTop = 0f,
        boxRight = canvas.width - reserveRight.toFloat(),
        boxBottom = canvas.height - reserveBottom.toFloat(),
        onLeft = badgeOnLeft,
        density = density,
    )
}

internal class ChatAppearancePreviewView(context: android.content.Context) : android.widget.FrameLayout(context) {
    init {
        setWillNotDraw(false)
    }

    private var bubbleId = "classic"
    private var frameId = "none"
    private var bubbleStyleJson: String? = null
    private var dark = false
    private var classicFill = 0xFFE8F5E4.toInt()
    private var classicText = 0xFF2C2E33.toInt()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var sticker: android.widget.ImageView? = null
    private var stickerUrl: String? = null
    private var avatarUrl: String? = null
    private var avatarDrawable: Drawable? = null
    private var avatarRequestKey: String? = null
    private var imageLoader: coil3.ImageLoader? = null
    private val avatarClip = Path()
    private var visualCacheKey: String? = null
    private var cachedBubbleVisual: ChatBubbleVisual? = null
    private var cachedFrameVisual: ChatAvatarFrameVisual? = null

    private fun visuals(): Pair<ChatBubbleVisual?, ChatAvatarFrameVisual?> {
        val key = "$bubbleId|$frameId|$bubbleStyleJson|$dark"
        if (key == visualCacheKey) return Pair(cachedBubbleVisual, cachedFrameVisual)
        visualCacheKey = key
        val density = resources.displayMetrics.density
        val snapshot = ChatAppearanceSnapshot(
            bubbleId = bubbleId,
            frameId = frameId,
            bubbleStyleJson = bubbleStyleJson,
        )
        cachedBubbleVisual = ChatAppearanceRenderer.bubble(snapshot, dark, false, resources, density)
        cachedFrameVisual = ChatAppearanceRenderer.avatarFrame(snapshot, dark, resources, density)
        return Pair(cachedBubbleVisual, cachedFrameVisual)
    }

    private fun sakuraDrawables(): List<SakuraDuoDrawable> {
        val (bubble, frame) = visuals()
        return listOfNotNull(
            bubble?.background as? SakuraDuoDrawable,
            frame?.background as? SakuraDuoDrawable,
        )
    }

    private fun drawAnimatedBadge(
        canvas: Canvas,
        badge: ChatBadgeVisual,
        boxLeft: Float,
        boxTop: Float,
        boxRight: Float,
        boxBottom: Float,
        onLeft: Boolean,
        sakuraVisual: Boolean,
        density: Float,
    ) {
        val bloom = 1f
        val centerX = if (onLeft) boxLeft + badge.anchorRightPx + badge.sizePx / 2f
                      else boxRight - badge.anchorRightPx - badge.sizePx / 2f
        val centerY = boxBottom - badge.anchorBottomPx - badge.sizePx / 2f
        canvas.save()
        canvas.scale(bloom, bloom, centerX, centerY)
        ChatAppearanceRenderer.drawBadge(
            canvas = canvas,
            badge = badge,
            boxLeft = boxLeft,
            boxTop = boxTop,
            boxRight = boxRight,
            boxBottom = boxBottom,
            onLeft = onLeft,
            density = density,
        )
        canvas.restore()
    }

    fun bind(
        bubbleId: String,
        frameId: String,
        dark: Boolean,
        classicFill: Int,
        classicText: Int,
        bubbleStyleJson: String? = null,
        imageLoader: coil3.ImageLoader? = null,
        avatarUrl: String? = null,
    ) {
        this.bubbleId = bubbleId
        this.frameId = frameId
        this.bubbleStyleJson = bubbleStyleJson
        this.dark = dark
        this.classicFill = classicFill
        this.classicText = classicText
        this.imageLoader = imageLoader
        this.avatarUrl = avatarUrl
        refreshAvatarDrawable()
        requestLayout()
        invalidate()
    }

    /** 加载用户头像并驱动动图播放，成功前回退品牌占位图。 */
    private fun refreshAvatarDrawable() {
        val url = avatarUrl?.trim().orEmpty()
        if (url.isBlank()) {
            (avatarDrawable as? android.graphics.drawable.Animatable)?.stop()
            avatarDrawable?.callback = null
            avatarDrawable?.setVisible(false, false)
            avatarDrawable = null
            avatarRequestKey = null
            invalidate()
            return
        }
        if (avatarRequestKey == url) return
        avatarRequestKey = url
        val loader = imageLoader ?: return
        val request = coil3.request.ImageRequest.Builder(context)
            .data(url)
            .size(coil3.size.Size(256, 256))
            .precision(coil3.size.Precision.INEXACT)
            .target(
                onSuccess = { image ->
                    val drawable = image.asDrawable(context.resources)
                    (avatarDrawable as? android.graphics.drawable.Animatable)?.stop()
                    avatarDrawable?.callback = null
                    avatarDrawable?.setVisible(false, false)
                    drawable.callback = this
                    avatarDrawable = drawable
                    invalidate()
                },
            )
            .build()
        loader.enqueue(request)
    }

    override fun verifyDrawable(who: Drawable): Boolean =
        super.verifyDrawable(who) || who === avatarDrawable

    private fun placeSticker(
        badge: ChatBubbleSticker?,
        boxLeft: Float,
        boxRight: Float,
        boxBottom: Float,
        scale: Float = 1f,
    ) {
        val view = sticker ?: android.widget.ImageView(context).apply {
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            addView(this, LayoutParams(0, 0))
            sticker = this
        }
        if (badge == null) {
            view.visibility = GONE
            stickerUrl = null
            return
        }
        view.visibility = VISIBLE
        val sizePx = (badge.sizePx * scale).roundToInt().coerceAtLeast(1)
        val marginStartPx = badge.marginStartPx * scale
        val left = if (badge.onStart) boxLeft + marginStartPx else boxRight - marginStartPx - sizePx
        val bottom = boxBottom - badge.marginBottomPx * scale
        val top = bottom - sizePx
        view.layout(left.roundToInt(), top.roundToInt(), (left + sizePx).roundToInt(), bottom.roundToInt())
        val requestKey = "${badge.url}#$sizePx"
        if (stickerUrl == requestKey) return
        stickerUrl = requestKey
        view.setImageDrawable(null)
        val loader = imageLoader ?: return
        val request = coil3.request.ImageRequest.Builder(context)
            .data(badge.url)
            .size(coil3.size.Size(sizePx, sizePx))
            .precision(coil3.size.Precision.INEXACT)
            .target(
                onSuccess = { image ->
                    if (sticker === view && stickerUrl == requestKey) {
                        view.setImageDrawable(image.asDrawable(context.resources))
                    }
                },
            )
            .build()
        loader.enqueue(request)
    }

    override fun onDetachedFromWindow() {
        (avatarDrawable as? android.graphics.drawable.Animatable)?.stop()
        avatarDrawable?.setVisible(false, false)
        sakuraDrawables().forEach { it.playing = false }
        super.onDetachedFromWindow()
    }

private class PreviewLayout(
    val frameLeft: Float,
    val frameTop: Float,
    val outerPx: Float,
    val avatarRect: RectF,
    val boxLeft: Float,
    val boxTop: Float,
    val boxRight: Float,
    val boxBottom: Float,
    val boxScale: Float = 1f,
)

/** 头像列靠左，气泡盒紧随其后，整体垂直居中。 */
private fun previewLayout(bubble: ChatBubbleVisual?, frame: ChatAvatarFrameVisual?): PreviewLayout {
    val density = resources.displayMetrics.density
    val innerPx = minOf(36f * density, height - 12f * density).coerceAtLeast(16f * density)
    val geometry = frame?.geometry(innerPx.roundToInt())
    val ringPx = geometry?.ringPx?.toFloat() ?: 0f
    val outerPx = (geometry?.frameSizePx ?: innerPx.roundToInt()).toFloat()
    val frameLeft = 4f * density
    val frameTop = (height - outerPx) / 2f
    val avatarRect = if (frame != null) {
        RectF(
            frameLeft + ringPx,
            frameTop + ringPx,
            frameLeft + ringPx + innerPx,
            frameTop + ringPx + innerPx,
        )
    } else {
        val avatarLeft = 4f * density
        val avatarTop = (height - innerPx) / 2f
        RectF(avatarLeft, avatarTop, avatarLeft + innerPx, avatarTop + innerPx)
    }
    val avatarRight = if (frame != null) frameLeft + outerPx else avatarRect.right
    val boxLeft = avatarRight + 10f * density
    textPaint.textSize = 14f * density
    val textWidth = textPaint.measureText("新消息预览")
    val naturalWidth = if (bubble != null) {
        maxOf(
            bubble.minWidth.toFloat(),
            textWidth + bubble.paddingLeft + bubble.paddingRight + 20f * density,
        )
    } else {
        textWidth + 28f * density
    }
    val textHeight = textPaint.fontMetrics.let { it.descent - it.ascent }
    val naturalHeight = if (bubble != null) {
        textHeight + bubble.paddingTop + bubble.paddingBottom + bubble.reserveBottom
    } else {
        textHeight + 22f * density
    }
    val available = (width - 12f * density) - boxLeft
    val boxScale = if (available > 24f * density) (available / naturalWidth).coerceAtMost(1f) else 1f
    val boxHeight = naturalHeight * boxScale
    val boxTop = ((height - boxHeight) / 2f).coerceAtLeast(2f * density)
    val boxRight = boxLeft + naturalWidth * boxScale
    return PreviewLayout(frameLeft, frameTop, outerPx, avatarRect, boxLeft, boxTop, boxRight, boxTop + boxHeight, boxScale)
}

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val (bubble, frame) = visuals()
        if (bubble == null) {
            placeSticker(null, 0f, 0f, 0f)
            return
        }
        val layout = previewLayout(bubble, frame)
        placeSticker(
            badge = bubble.sticker,
            boxLeft = layout.boxLeft + bubble.reserveLeft * layout.boxScale,
            boxRight = layout.boxRight - bubble.reserveRight * layout.boxScale,
            boxBottom = layout.boxBottom - bubble.reserveBottom * layout.boxScale,
            scale = layout.boxScale,
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        setMeasuredDimension(
            resolveSize((280f * density).toInt(), widthMeasureSpec),
            resolveSize((92f * density).toInt(), heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val (bubble, frame) = visuals()
        val layout = previewLayout(bubble, frame)
        if (frame != null) {
            canvas.save()
            canvas.translate(layout.frameLeft, layout.frameTop)
            frame.background.setBounds(0, 0, layout.outerPx.roundToInt(), layout.outerPx.roundToInt())
            frame.background.draw(canvas)
            canvas.restore()
        }
        val avatarRect = layout.avatarRect
        val inner = frame?.innerBackground
        avatarClip.reset()
        if (inner is SakuraInnerDrawable) {
            inner.writeContour(avatarRect.width(), avatarRect.height(), avatarClip)
            avatarClip.offset(avatarRect.left, avatarRect.top)
        } else {
            avatarClip.addRoundRect(avatarRect, 8f * density, 8f * density, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(avatarClip)
        if (inner != null) {
            canvas.save()
            canvas.translate(avatarRect.left, avatarRect.top)
            inner.setBounds(0, 0, avatarRect.width().roundToInt(), avatarRect.height().roundToInt())
            inner.draw(canvas)
            canvas.restore()
        } else {
            fillPaint.color = classicFill
            canvas.drawRect(avatarRect, fillPaint)
        }
        val avatarArt = avatarDrawable
            ?: ChatAppearanceRenderer.brandLogo(resources)?.let { android.graphics.drawable.BitmapDrawable(resources, it) }
        avatarArt?.let { art ->
            val artWidth = art.intrinsicWidth.takeIf { it > 0 } ?: avatarRect.width().roundToInt()
            val artHeight = art.intrinsicHeight.takeIf { it > 0 } ?: avatarRect.height().roundToInt()
            val artScale = maxOf(
                avatarRect.width() / artWidth.toFloat(),
                avatarRect.height() / artHeight.toFloat(),
            )
            val scaledWidth = (artWidth * artScale).roundToInt()
            val scaledHeight = (artHeight * artScale).roundToInt()
            val artLeft = avatarRect.left + (avatarRect.width() - scaledWidth) / 2f
            val artTop = avatarRect.top + (avatarRect.height() - scaledHeight) / 2f
            canvas.save()
            canvas.clipRect(artLeft, artTop, artLeft + scaledWidth, artTop + scaledHeight)
            art.setBounds(
                artLeft.roundToInt(),
                artTop.roundToInt(),
                (artLeft + scaledWidth).roundToInt(),
                (artTop + scaledHeight).roundToInt(),
            )
            (art as? android.graphics.drawable.Animatable)?.start()
            art.setVisible(true, false)
            art.draw(canvas)
            canvas.restore()
        }
        canvas.restore()
        if (frame != null) {
            frame.badge?.let {
                drawAnimatedBadge(
                    canvas = canvas,
                    badge = it,
                    boxLeft = layout.frameLeft,
                    boxTop = layout.frameTop,
                    boxRight = layout.frameLeft + layout.outerPx,
                    boxBottom = layout.frameTop + layout.outerPx,
                    onLeft = false,
                    sakuraVisual = frame.sakura,
                    density = density,
                )
            }
        }
        val laneSave = canvas.save()
        canvas.clipRect(
            layout.boxLeft - 6f * density,
            -8f * density,
            width.toFloat(),
            height + 8f * density,
        )
        if (bubble != null) {
            val boxSave = canvas.save()
            canvas.translate(layout.boxLeft, layout.boxTop)
            canvas.scale(layout.boxScale, layout.boxScale)
            bubble.background.setBounds(
                0,
                0,
                ((layout.boxRight - layout.boxLeft) / layout.boxScale).roundToInt(),
                ((layout.boxBottom - layout.boxTop) / layout.boxScale).roundToInt(),
            )
            bubble.background.draw(canvas)
            canvas.restoreToCount(boxSave)
            textPaint.textSize = 14f * density * layout.boxScale
            textPaint.color = bubble.textColor
            canvas.drawText(
                "新消息预览",
                layout.boxLeft + (bubble.reserveLeft + bubble.paddingLeft) * layout.boxScale,
                layout.boxTop + bubble.paddingTop * layout.boxScale - textPaint.fontMetrics.ascent,
                textPaint,
            )
            bubble.badge?.let {
                drawAnimatedBadge(
                    canvas = canvas,
                    badge = it,
                    boxLeft = layout.boxLeft + bubble.reserveLeft,
                    boxTop = layout.boxTop,
                    boxRight = layout.boxRight - bubble.reserveRight,
                    boxBottom = layout.boxBottom - bubble.reserveBottom,
                    onLeft = bubble.badgeOnLeft,
                    sakuraVisual = bubble.sakura,
                    density = density,
                )
            }
        } else {
            val boxSave = canvas.save()
            canvas.translate(layout.boxLeft, layout.boxTop)
            canvas.scale(layout.boxScale, layout.boxScale)
            fillPaint.color = classicFill
            canvas.drawRoundRect(
                RectF(
                    0f,
                    0f,
                    (layout.boxRight - layout.boxLeft) / layout.boxScale,
                    (layout.boxBottom - layout.boxTop) / layout.boxScale,
                ),
                14f * density,
                14f * density,
                fillPaint,
            )
            canvas.restoreToCount(boxSave)
            textPaint.textSize = 14f * density * layout.boxScale
            textPaint.color = classicText
            canvas.drawText(
                "新消息预览",
                layout.boxLeft + 14f * density * layout.boxScale,
                layout.boxTop + 11f * density * layout.boxScale - textPaint.fontMetrics.ascent,
                textPaint,
            )
        }
        canvas.restoreToCount(laneSave)
    }
}
