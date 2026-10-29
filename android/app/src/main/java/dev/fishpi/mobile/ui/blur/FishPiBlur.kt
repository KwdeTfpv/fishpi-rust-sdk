package dev.fishpi.mobile.ui.blur

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import dev.fishpi.mobile.ui.glass.LocalFishPiGlassBackdrop

private val SquareShape = RoundedCornerShape(0.dp)

@Composable
internal fun FishPiBlurBackdrop(
    modifier: Modifier = Modifier,
    overlayColor: Color = Color.Black.copy(alpha = 0.20f),
    blurRadius: Dp = 22.dp,
) {
    val backdrop = LocalFishPiGlassBackdrop.current
    if (backdrop == null) {
        Box(modifier = modifier.background(overlayColor))
        return
    }

    Box(
        modifier = modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { SquareShape },
            effects = { blur(blurRadius.toPx()) },
            onDrawSurface = { drawRect(overlayColor) },
        ),
    )
}

@Composable
internal fun FishPiBlurOverlay(
    modifier: Modifier = Modifier,
    overlayColor: Color = Color.Black.copy(alpha = 0.20f),
    blurRadius: Dp = 22.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        FishPiBlurBackdrop(
            modifier = Modifier.fillMaxSize(),
            overlayColor = overlayColor,
            blurRadius = blurRadius,
        )
        content()
    }
}
