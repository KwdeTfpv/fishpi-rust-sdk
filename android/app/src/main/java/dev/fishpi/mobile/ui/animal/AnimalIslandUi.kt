package dev.fishpi.mobile.ui.animal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.LocalFishPiPalette

internal object AnimalIslandTokens {
    val PagePadding = 14.dp
    val SectionGap = 10.dp
    val CardRadius = 12.dp
    val SmallRadius = 10.dp
    val ControlRadius = 12.dp
    val CardPadding = 12.dp
    val CompactPadding = 8.dp
    val DockHeight = 56.dp
}

@Composable
private fun themedRadius(base: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp =
    base

@Composable
private fun themedBorderColor(baseAlpha: Float = 0.16f): Color =
    MaterialTheme.colorScheme.outline.copy(
        alpha = baseAlpha.coerceAtMost(0.16f),
    )

@Composable
internal fun AnimalIslandBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = LocalFishPiPalette.current
    val colors = if (palette.wallpaperColors.size >= 4) {
        palette.wallpaperColors
    } else {
        listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            MaterialTheme.colorScheme.background,
        )
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors,
                ),
            ),
    ) {
        content()
    }
}

@Composable
internal fun AnimalIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val shape = RoundedCornerShape(themedRadius(AnimalIslandTokens.ControlRadius))
    val background = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
    }
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, themedBorderColor(0.12f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(19.dp))
    }
}

@Composable
internal fun AnimalChatGlyph(
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(themedRadius(12.dp)))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(themedRadius(12.dp)))
            .padding(6.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(width = 21.dp, height = 17.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(accent.copy(alpha = 0.82f)),
        ) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color.White.copy(alpha = 0.86f)),
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(width = 18.dp, height = 15.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                .border(1.dp, accent.copy(alpha = 0.24f), RoundedCornerShape(6.dp)),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(width = 8.dp, height = 3.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(accent.copy(alpha = 0.72f)),
            )
        }
    }
}

@Composable
internal fun AnimalStatusPill(
    label: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    leadingDot: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (leadingDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(color),
            )
        }
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
