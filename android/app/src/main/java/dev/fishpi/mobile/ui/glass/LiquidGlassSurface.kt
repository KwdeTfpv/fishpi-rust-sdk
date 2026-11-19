package dev.fishpi.mobile.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
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

internal val LocalGlassInMotion = staticCompositionLocalOf<(() -> Boolean)?> { null }

internal enum class FishPiGlassRole {
    Content,
    Control,
    Floating,
}

private val SoftSheenBrush: Brush = Brush.verticalGradient(
    0f to Color.White.copy(alpha = 0.10f),
    0.34f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.06f),
)

private fun glassBorderBrush(borderColor: Color): Brush = Brush.verticalGradient(
    0f to Color.White.copy(alpha = 0.24f),
    0.45f to borderColor,
    1f to Color.Black.copy(alpha = 0.10f),
)

internal enum class FishPiSurfaceTone { Base, Elevated, Floating, Sheet }

@Composable
internal fun FishPiSurfaceTone.containerColor(): Color = when (this) {
    FishPiSurfaceTone.Base -> FishPiTheme.surface.copy(alpha = FishPiTheme.glass.surfaceAlpha)
    FishPiSurfaceTone.Elevated -> FishPiTheme.surface.copy(alpha = FishPiTheme.glass.elevatedAlpha)
    FishPiSurfaceTone.Floating -> FishPiTheme.surface.copy(alpha = FishPiTheme.glass.floatingAlpha)
    FishPiSurfaceTone.Sheet -> FishPiTheme.surface.copy(alpha = 0.82f)
}

@Composable
internal fun FishPiSurfaceTone.borderColor(): Color = when (this) {
    FishPiSurfaceTone.Base -> FishPiTheme.outline.copy(alpha = 0.12f + FishPiTheme.depth * 0.08f)
    FishPiSurfaceTone.Elevated -> FishPiTheme.outline.copy(alpha = 0.14f + FishPiTheme.depth * 0.08f)
    FishPiSurfaceTone.Floating -> FishPiTheme.outline.copy(alpha = 0.18f + FishPiTheme.depth * 0.10f)
    FishPiSurfaceTone.Sheet -> FishPiTheme.outline.copy(alpha = 0.18f)
}

@Composable
internal fun Modifier.fishPiGlassSurface(
    backdrop: Backdrop?,
    shape: Shape,
    tone: FishPiSurfaceTone? = null,
    containerColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp? = null,
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
    val fillColor = containerColor ?: tone?.containerColor() ?: Color.Transparent
    val strokeColor = borderColor ?: tone?.borderColor() ?: Color.Transparent
    val strokeWidth = borderWidth ?: if (tone != null) FishPiTheme.borderWidth else 0.dp
    val glassEnabled = FishPiTheme.glassEnabled
    val motionProbe = LocalGlassInMotion.current
    val inMotion by remember(motionProbe) {
        derivedStateOf { motionProbe?.invoke() == true }
    }
    val shouldDrawBackdrop = glassEnabled && backdrop != null && role != FishPiGlassRole.Control
    val glass = remember(
        glassEnabled,
        shouldDrawBackdrop,
        inMotion,
        backdrop,
        shape,
        fillColor,
        strokeWidth,
        blurRadius,
        refractionHeight,
        refractionAmount,
        depthEffect,
        chromaticAberration,
        shadowAlpha,
        innerShadowRadius,
        exportedBackdrop,
    ) {
        if (!shouldDrawBackdrop) {
            Modifier
                .clip(shape)
                .background(fillColor)
                .then(if (glassEnabled) Modifier.background(SoftSheenBrush) else Modifier)
        } else if (inMotion) {
            Modifier.drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                },
                highlight = { Highlight.Plain },
                shadow = { Shadow(alpha = 0f) },
                innerShadow = {
                    InnerShadow(radius = 0.dp, alpha = 0f)
                },
                exportedBackdrop = exportedBackdrop,
                onDrawSurface = {
                    drawRect(fillColor)
                    drawRect(brush = SoftSheenBrush)
                },
            )
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
                    drawRect(fillColor)
                    drawRect(brush = SoftSheenBrush)
                },
            )
        }
    }
    val border = remember(strokeWidth, strokeColor, glassEnabled, shape) {
        if (strokeWidth > 0.dp) {
            if (glassEnabled) {
                Modifier.border(
                    width = strokeWidth,
                    brush = glassBorderBrush(strokeColor),
                    shape = shape,
                )
            } else {
                Modifier.border(width = strokeWidth, color = strokeColor, shape = shape)
            }
        } else {
            Modifier
        }
    }
    return then(glass).then(border)
}
