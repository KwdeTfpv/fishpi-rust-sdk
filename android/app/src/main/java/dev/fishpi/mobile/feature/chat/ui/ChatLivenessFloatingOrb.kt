package dev.fishpi.mobile.feature.chat.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.fishpi.mobile.FishPiTheme
import kotlin.math.roundToInt

private enum class LivenessDockSide {
    None,
    Left,
    Right,
}

private data class LivenessOrbPosition(
    val x: Float,
    val y: Float,
    val dockSide: LivenessDockSide,
)

@Composable
internal fun ChatLivenessFloatingOrb(
    liveness: Double?,
    positionKey: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val orbSizePx = with(density) { 58.dp.toPx() }
        val visibleDockPx = with(density) { 18.dp.toPx() }
        val marginPx = with(density) { 14.dp.toPx() }
        val maxWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(orbSizePx)
        val maxHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(orbSizePx)
        val saved = remember(positionKey, maxWidthPx, maxHeightPx) {
            context.readLivenessOrbPosition(
                positionKey = positionKey,
                defaultX = (maxWidthPx - orbSizePx - marginPx).coerceAtLeast(0f),
                defaultY = marginPx + with(density) { 72.dp.toPx() },
            )
        }
        var x by remember(positionKey, maxWidthPx, maxHeightPx) {
            mutableStateOf(saved.x.coerceIn(-orbSizePx + visibleDockPx, maxWidthPx - visibleDockPx))
        }
        var y by remember(positionKey, maxWidthPx, maxHeightPx) {
            mutableStateOf(saved.y.coerceIn(0f, maxHeightPx - orbSizePx))
        }
        var dockSide by remember(positionKey, maxWidthPx, maxHeightPx) { mutableStateOf(saved.dockSide) }

        val hidden = dockSide != LivenessDockSide.None
        val percent = liveness?.coerceIn(0.0, 100.0)
        Box(
            modifier = Modifier
                .size(58.dp)
                .offsetPx(x, y)
                .clip(CircleShape)
                .background(FishPiTheme.surface.copy(alpha = 0.74f))
                .clickable(enabled = hidden) {
                    dockSide = LivenessDockSide.None
                    x = when {
                        x < 0f -> marginPx
                        x > maxWidthPx - orbSizePx -> (maxWidthPx - orbSizePx - marginPx).coerceAtLeast(0f)
                        else -> x
                    }
                    context.saveLivenessOrbPosition(positionKey, x, y, dockSide)
                }
                .pointerInput(positionKey, maxWidthPx, maxHeightPx) {
                    detectDragGestures(
                        onDragStart = {
                            dockSide = LivenessDockSide.None
                        },
                        onDragEnd = {
                            val leftDistance = x
                            val rightDistance = maxWidthPx - (x + orbSizePx)
                            dockSide = when {
                                leftDistance < -orbSizePx * 0.15f -> LivenessDockSide.Left
                                rightDistance < -orbSizePx * 0.15f -> LivenessDockSide.Right
                                leftDistance < marginPx -> LivenessDockSide.Left
                                rightDistance < marginPx -> LivenessDockSide.Right
                                else -> LivenessDockSide.None
                            }
                            x = when (dockSide) {
                                LivenessDockSide.Left -> -orbSizePx + visibleDockPx
                                LivenessDockSide.Right -> maxWidthPx - visibleDockPx
                                LivenessDockSide.None -> x.coerceIn(0f, maxWidthPx - orbSizePx)
                            }
                            y = y.coerceIn(0f, maxHeightPx - orbSizePx)
                            context.saveLivenessOrbPosition(positionKey, x, y, dockSide)
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        x = (x + dragAmount.x).coerceIn(-orbSizePx + visibleDockPx, maxWidthPx - visibleDockPx)
                        y = (y + dragAmount.y).coerceIn(0f, maxHeightPx - orbSizePx)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            LivenessLevel(
                progress = (percent ?: 0.0) / 100.0,
                accent = FishPiTheme.accent,
                strokeColor = FishPiTheme.accent.copy(alpha = 0.62f),
            )
            Text(
                text = percent?.let(::formatLivenessOrbText) ?: "--",
                color = FishPiTheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                lineHeight = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LivenessLevel(
    progress: Double,
    accent: Color,
    strokeColor: Color,
) {
    Canvas(modifier = Modifier.size(58.dp)) {
        val diameter = size.minDimension
        val radius = diameter / 2f
        val circle = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(0f, 0f, diameter, diameter))
        }
        drawCircle(
            color = accent.copy(alpha = 0.10f),
            radius = radius,
            center = Offset(radius, radius),
        )
        clipPath(circle) {
            val fillProgress = progress.coerceIn(0.0, 1.0).toFloat()
            val fillTop = diameter * (1f - fillProgress)
            drawRect(
                color = accent.copy(alpha = 0.28f),
                topLeft = Offset(0f, fillTop),
                size = androidx.compose.ui.geometry.Size(diameter, diameter - fillTop),
            )
            drawCircle(
                color = accent.copy(alpha = 0.13f),
                radius = radius * 0.72f,
                center = Offset(radius * 0.62f, fillTop + radius * 0.12f),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.22f),
                radius = radius * 0.42f,
                center = Offset(radius * 0.66f, radius * 0.52f),
            )
        }
        drawCircle(
            color = strokeColor,
            radius = radius - 0.75.dp.toPx(),
            center = Offset(radius, radius),
            style = Stroke(width = 1.5.dp.toPx()),
        )
    }
}

private fun formatLivenessOrbText(value: Double): String =
    if (value % 1.0 == 0.0) {
        "${value.toInt()}%"
    } else {
        "%.2f%%".format(java.util.Locale.US, value).trimEnd('0').replace(".%", "%")
    }

private fun Modifier.offsetPx(x: Float, y: Float): Modifier =
    this.then(
        Modifier.offset {
            IntOffset(x.roundToInt(), y.roundToInt())
        },
    )

private fun Context.readLivenessOrbPosition(
    positionKey: String,
    defaultX: Float,
    defaultY: Float,
): LivenessOrbPosition {
    val prefs = getSharedPreferences("fishpi-chat-liveness-orb", Context.MODE_PRIVATE)
    val key = positionKey.hashCode().toString()
    val side = runCatching {
        LivenessDockSide.valueOf(prefs.getString("${key}_side", LivenessDockSide.None.name).orEmpty())
    }.getOrDefault(LivenessDockSide.None)
    return LivenessOrbPosition(
        x = prefs.getFloat("${key}_x", defaultX),
        y = prefs.getFloat("${key}_y", defaultY),
        dockSide = side,
    )
}

private fun Context.saveLivenessOrbPosition(
    positionKey: String,
    x: Float,
    y: Float,
    dockSide: LivenessDockSide,
) {
    val prefs = getSharedPreferences("fishpi-chat-liveness-orb", Context.MODE_PRIVATE)
    val key = positionKey.hashCode().toString()
    prefs.edit()
        .putFloat("${key}_x", x)
        .putFloat("${key}_y", y)
        .putString("${key}_side", dockSide.name)
        .apply()
}
