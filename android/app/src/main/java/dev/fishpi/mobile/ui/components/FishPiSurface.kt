package dev.fishpi.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.fishpi.mobile.FishPiTheme

internal enum class FishPiSurfaceKind { Card, Subtle, Panel, Field }

@Composable
internal fun FishPiSurfaceKind.shape(): Shape = when (this) {
    FishPiSurfaceKind.Card -> RoundedCornerShape(FishPiTheme.radiusBox)
    FishPiSurfaceKind.Subtle -> RoundedCornerShape(FishPiTheme.radiusBox)
    FishPiSurfaceKind.Panel -> RoundedCornerShape(FishPiTheme.radiusBox + 8.dp)
    FishPiSurfaceKind.Field -> RoundedCornerShape(FishPiTheme.radiusField)
}

@Composable
internal fun FishPiSurfaceKind.fill(): Color = when (this) {
    FishPiSurfaceKind.Card -> FishPiTheme.surfaceContainer
    FishPiSurfaceKind.Subtle -> FishPiTheme.surfaceContainer.copy(alpha = 0.42f)
    FishPiSurfaceKind.Panel -> FishPiTheme.surface.copy(alpha = 0.86f)
    FishPiSurfaceKind.Field -> FishPiTheme.surface
}

@Composable
internal fun FishPiSurfaceKind.stroke(): Color? = when (this) {
    FishPiSurfaceKind.Card -> FishPiTheme.outline.copy(alpha = 0.16f)
    FishPiSurfaceKind.Subtle -> null
    FishPiSurfaceKind.Panel -> FishPiTheme.outline.copy(alpha = 0.14f + FishPiTheme.depth * 0.08f)
    FishPiSurfaceKind.Field -> FishPiTheme.outline.copy(alpha = 0.12f)
}

@Composable
internal fun Modifier.fishPiSurface(
    kind: FishPiSurfaceKind,
    shape: Shape? = null,
    fill: Color? = null,
    stroked: Boolean = true,
): Modifier {
    val surfaceShape = shape ?: kind.shape()
    val surfaceFill = fill ?: kind.fill()
    val clipped = clip(surfaceShape).background(surfaceFill)
    if (!stroked) return clipped
    val stroke = kind.stroke() ?: return clipped
    val width = FishPiTheme.borderWidth
    if (width <= 0.dp) return clipped
    return clipped.border(width, stroke, surfaceShape)
}
