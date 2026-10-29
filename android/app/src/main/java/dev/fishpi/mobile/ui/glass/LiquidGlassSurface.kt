package dev.fishpi.mobile.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import dev.fishpi.mobile.FishPiTheme

internal val LocalFishPiGlassBackdrop =
    androidx.compose.runtime.staticCompositionLocalOf<Backdrop?> { null }

internal enum class FishPiGlassRole {
    Content,
    Control,
    Floating,
}

private fun softSheenBrush(): Brush = Brush.verticalGradient(
    0f to Color.White.copy(alpha = 0.10f),
    0.34f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.06f),
)

@Composable
internal fun Modifier.fishPiGlassSurface(
    backdrop: Backdrop?,
    shape: Shape,
    containerColor: Color,
    borderColor: Color = Color.Transparent,
    borderWidth: Dp = 0.dp,
    blurRadius: Dp = FishPiTheme.glass.blurRadius.dp,
    refractionHeight: Dp = FishPiTheme.glass.refractionHeight.dp,
    refractionAmount: Dp = FishPiTheme.glass.refractionAmount.dp,
    depthEffect: Boolean = true,
    chromaticAberration: Boolean = FishPiTheme.glass.chromaticAberration,
    shadowAlpha: Float = FishPiTheme.glass.shadowAlpha,
    innerShadowRadius: Dp = FishPiTheme.glass.innerShadowRadius.dp,
    role: FishPiGlassRole = FishPiGlassRole.Control,
    exportedBackdrop: LayerBackdrop? = null,
): Modifier {
    val glassEnabled = FishPiTheme.glassEnabled
    val shouldDrawBackdrop = glassEnabled && backdrop != null && role != FishPiGlassRole.Control
    val glass = if (!shouldDrawBackdrop) {
        Modifier
            .clip(shape)
            .background(containerColor)
            .then(if (glassEnabled) Modifier.background(softSheenBrush()) else Modifier)
    } else {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(blurRadius.toPx())
                lens(
                    refractionHeight = refractionHeight.toPx(),
                    refractionAmount = refractionAmount.toPx(),
                    depthEffect = depthEffect,
                    chromaticAberration = chromaticAberration,
                )
            },
            highlight = { Highlight.Plain },
            shadow = { Shadow(alpha = shadowAlpha) },
            innerShadow = {
                InnerShadow(
                    radius = innerShadowRadius,
                    alpha = shadowAlpha * 0.64f,
                )
            },
            exportedBackdrop = exportedBackdrop,
            onDrawSurface = {
                drawRect(containerColor)
                drawRect(brush = softSheenBrush())
            },
        )
    }
    return then(glass)
        .then(
            if (borderWidth > 0.dp) {
                if (glassEnabled) {
                    Modifier.border(
                        width = borderWidth,
                        brush = Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.24f),
                            0.45f to borderColor,
                            1f to Color.Black.copy(alpha = 0.10f),
                        ),
                        shape = shape,
                    )
                } else {
                    Modifier.border(width = borderWidth, color = borderColor, shape = shape)
                }
            } else {
                Modifier
            },
        )
}
