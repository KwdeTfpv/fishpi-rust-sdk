package dev.fishpi.mobile.feature.chat.barrage.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.rememberFishPiImageLoader
import dev.fishpi.mobile.feature.chat.barrage.ChatBarrageUiModel
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun ChatBarrageOverlay(
    barrages: List<ChatBarrageUiModel>,
    onFinished: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.padding(top = 14.dp, bottom = 72.dp),
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val availableHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(120f)
        val laneHeightPx = with(density) { 40.dp.toPx() }
        val laneCount = (availableHeightPx / laneHeightPx).toInt().coerceIn(3, 7)
        barrages.forEachIndexed { index, item ->
            key(item.id) {
                val seed = abs("${item.id}:${item.createdAtMs}".hashCode())
                ChatBarrageFlyingItem(
                    item = item,
                    lane = (seed + index) % laneCount,
                    seed = seed,
                    widthPx = widthPx,
                    onFinished = onFinished,
                )
            }
        }
    }
}

@Composable
private fun ChatBarrageFlyingItem(
    item: ChatBarrageUiModel,
    lane: Int,
    seed: Int,
    widthPx: Float,
    onFinished: (String) -> Unit,
) {
    val startOffset = remember(item.id, widthPx) { widthPx + 48f + (seed % 96) }
    val targetOffset = remember(item.id, widthPx) { -widthPx * (0.82f + (seed % 24) / 100f) }
    val durationMs = remember(item.id) { 6200 + seed % 1700 }
    val offsetX = remember(item.id) { Animatable(startOffset) }
    var initialized by remember(item.id, widthPx) { mutableStateOf(false) }
    var isHolding by remember(item.id) { mutableStateOf(false) }
    var holdRequest by remember(item.id) { mutableIntStateOf(0) }

    LaunchedEffect(item.id, widthPx, isHolding) {
        if (!initialized) {
            offsetX.snapTo(startOffset)
            initialized = true
        }
        if (isHolding) {
            return@LaunchedEffect
        }
        val fullDistance = (startOffset - targetOffset).coerceAtLeast(1f)
        val remainingDistance = (offsetX.value - targetOffset).coerceAtLeast(0f)
        val remainingDuration = (durationMs * (remainingDistance / fullDistance))
            .roundToInt()
            .coerceAtLeast(240)
        offsetX.animateTo(
            targetValue = targetOffset,
            animationSpec = tween(durationMillis = remainingDuration, easing = LinearEasing),
        )
        onFinished(item.id)
    }
    LaunchedEffect(holdRequest) {
        if (holdRequest == 0) return@LaunchedEffect
        isHolding = true
        kotlinx.coroutines.delay(2600)
        isHolding = false
    }
    val laneJitter = ((seed / 7) % 13 - 6).dp
    val laneTop = (lane * 40).dp + laneJitter
    val laneTopPx = with(LocalDensity.current) { laneTop.roundToPx() }
    val accent = remember(item.color) { parseBarrageColor(item.color) }
    Surface(
        modifier = Modifier
            .offset { IntOffset(offsetX.value.roundToInt(), laneTopPx) }
            .widthIn(max = 260.dp)
            .clickable { holdRequest += 1 },
        shape = RoundedCornerShape(FishPiTheme.radiusField),
        color = FishPiTheme.surface.copy(alpha = 0.78f),
        border = androidx.compose.foundation.BorderStroke(FishPiTheme.borderWidth, accent.copy(alpha = 0.36f)),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 5.dp, top = 5.dp, end = 10.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            BarrageAvatar(item = item, accent = accent)
            Text(
                text = item.content,
                color = FishPiTheme.onSurface,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BarrageAvatar(
    item: ChatBarrageUiModel,
    accent: Color,
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        if (item.avatarUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model = item.avatarUrl,
                imageLoader = rememberFishPiImageLoader(),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                loading = { BarrageAvatarFallback(item, accent) },
                error = { BarrageAvatarFallback(item, accent) },
            )
        } else {
            BarrageAvatarFallback(item, accent)
        }
    }
}

@Composable
private fun BarrageAvatarFallback(
    item: ChatBarrageUiModel,
    accent: Color,
) {
    Text(
        text = item.author.trim().take(1).ifBlank { "鱼" },
        color = accent,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
    )
}

private fun parseBarrageColor(raw: String): Color =
    parseRgbaColor(raw)
        ?: runCatching { Color(android.graphics.Color.parseColor(raw.ifBlank { "#FFFFFF" })) }
            .getOrDefault(Color.White)

private val RgbaColorRegex =
    Regex("""rgba\((\d+),\s*(\d+),\s*(\d+),\s*([0-9.]+)\)""", RegexOption.IGNORE_CASE)

private fun parseRgbaColor(raw: String): Color? {
    val match = RgbaColorRegex.matchEntire(raw.trim())
        ?: return null
    val r = match.groupValues[1].toIntOrNull()?.coerceIn(0, 255) ?: return null
    val g = match.groupValues[2].toIntOrNull()?.coerceIn(0, 255) ?: return null
    val b = match.groupValues[3].toIntOrNull()?.coerceIn(0, 255) ?: return null
    val a = match.groupValues[4].toFloatOrNull()?.coerceIn(0f, 1f) ?: return null
    return Color(r, g, b, (a * 255).roundToInt().coerceIn(0, 255))
}
