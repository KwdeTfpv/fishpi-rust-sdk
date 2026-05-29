package dev.fishpi.mobile.shared.message.native

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import dev.fishpi.mobile.FishPiPalette
import dev.fishpi.mobile.FishPiThemeTokens
import dev.fishpi.mobile.IslandFishPiThemeTokens

internal data class NativeMessageTheme(
    val background: Int = Color.rgb(246, 241, 232),
    val wallpaperColors: List<Int> = listOf(Color.rgb(246, 241, 232)),
    val wallpaperImageUri: String? = null,
    val incomingBubble: Int = Color.WHITE,
    val outgoingBubble: Int = Color.rgb(255, 231, 198),
    val bubbleBorder: Int = Color.argb(44, 132, 120, 106),
    val bubbleText: Int = Color.rgb(36, 32, 28),
    val weakText: Int = Color.rgb(132, 120, 106),
    val authorName: Int = Color.rgb(237, 143, 38),
    val clientText: Int = Color.rgb(132, 120, 106),
    val clientBackground: Int = Color.argb(32, 237, 143, 38),
    val timeText: Int = Color.rgb(132, 120, 106),
    val accent: Int = Color.rgb(237, 143, 38),
    val linkText: Int = Color.rgb(237, 143, 38),
    val quoteText: Int = Color.rgb(132, 120, 106),
    val quoteLine: Int = Color.rgb(132, 120, 106),
    val serviceBackground: Int = Color.argb(130, 237, 143, 38),
    val quoteBackground: Int = Color.rgb(250, 244, 236),
    val radiusSelectorDp: Float = 999f,
    val radiusFieldDp: Float = 18f,
    val radiusBoxDp: Float = 12f,
    val spacingItemDp: Float = 8f,
    val spacingControlDp: Float = 10f,
    val borderWidthDp: Float = 1f,
    val depth: Float = 0.1f,
) {
    companion object {
        fun fromPalette(palette: FishPiPalette): NativeMessageTheme =
            fromTheme(palette, IslandFishPiThemeTokens)

        fun fromTheme(palette: FishPiPalette, tokens: FishPiThemeTokens): NativeMessageTheme {
            val isDark = palette.chatBackground.luminance() < 0.5f
            val incomingBubble = if (isDark) {
                palette.surface.copy(alpha = 0.88f)
            } else {
                palette.surface.copy(alpha = 0.98f)
            }
            val outgoingBubble = if (isDark) {
                palette.surfaceElevated.copy(alpha = 0.92f)
            } else {
                palette.outgoingBubble.copy(alpha = 0.14f).compositeOver(palette.surface)
            }
            val quoteBackground = if (isDark) {
                palette.surfaceElevated.copy(alpha = 0.82f)
            } else {
                palette.outgoingBubble.copy(alpha = 0.09f).compositeOver(palette.surface)
            }
            val bubbleBorder = if (isDark) {
                palette.accent.copy(alpha = 0.18f)
            } else {
                palette.weakText.copy(alpha = 0.10f)
            }
            return NativeMessageTheme(
                background = palette.chatBackground.toArgb(),
                wallpaperColors = palette.wallpaperColors.map { it.toArgb() }.ifEmpty { listOf(palette.chatBackground.toArgb()) },
                wallpaperImageUri = palette.wallpaperImageUri,
                incomingBubble = incomingBubble.toArgb(),
                outgoingBubble = outgoingBubble.toArgb(),
                bubbleBorder = bubbleBorder.toArgb(),
                bubbleText = palette.onSurface.toArgb(),
                weakText = palette.weakText.toArgb(),
                authorName = palette.userName.toArgb(),
                clientText = palette.clientText.toArgb(),
                clientBackground = palette.clientBackground.copy(alpha = 0.72f).toArgb(),
                timeText = palette.timeText.toArgb(),
                accent = palette.accent.toArgb(),
                linkText = palette.linkText.toArgb(),
                quoteText = palette.quoteText.toArgb(),
                quoteLine = palette.quoteLine.toArgb(),
                serviceBackground = palette.accent.copy(alpha = 0.26f).toArgb(),
                quoteBackground = quoteBackground.toArgb(),
                radiusSelectorDp = tokens.radius.selector,
                radiusFieldDp = tokens.radius.field,
                radiusBoxDp = tokens.radius.box,
                spacingItemDp = tokens.spacing.item,
                spacingControlDp = tokens.spacing.control,
                borderWidthDp = tokens.border.width,
                depth = tokens.depth.level,
            )
        }
    }
}

internal fun NativeMessageTheme.wallpaperDrawable(): GradientDrawable =
    GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        wallpaperColors.ifEmpty { listOf(background) }.toIntArray(),
    )

