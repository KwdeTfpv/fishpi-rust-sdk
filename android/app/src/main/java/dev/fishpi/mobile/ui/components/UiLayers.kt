package dev.fishpi.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.LocalFishPiPalette

internal object UiLayerTokens {
    val PagePadding = 16.dp
    val SectionGap = 12.dp
    val InlineGap = 8.dp
    val ControlRadius = 14.dp
    val CardRadius = 12.dp
    val ChipRadius = 999.dp
    val ControlPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp)
    val CardPadding = PaddingValues(14.dp)

    const val ZPage = 0f
    const val ZControl = 10f
    const val ZFloating = 30f
    const val ZPanel = 50f
    const val ZDialog = 80f
    const val ZPreview = 100f
}

@Composable
internal fun uiPageBrush(): Brush {
    val palette = LocalFishPiPalette.current
    return Brush.verticalGradient(
        listOf(
            palette.background,
            palette.chatBackground,
            palette.surface,
        ),
    )
}

@Composable
internal fun UiLayerScaffold(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(uiPageBrush()),
        content = content,
    )
}

@Composable
internal fun ControlSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(UiLayerTokens.ControlRadius),
    contentPadding: PaddingValues = UiLayerTokens.ControlPadding,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = LocalFishPiPalette.current
    Surface(
        modifier = modifier,
        shape = shape,
        color = palette.surface.copy(alpha = 0.88f),
        contentColor = palette.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.18f)),
    ) {
        Box(Modifier.padding(contentPadding), content = content)
    }
}

@Composable
internal fun ContentCardSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = UiLayerTokens.CardPadding,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = LocalFishPiPalette.current
    val shape = RoundedCornerShape(UiLayerTokens.CardRadius)
    val color = palette.surface.copy(alpha = 0.78f)
    val border = BorderStroke(1.dp, palette.outline.copy(alpha = 0.12f))
    if (onClick == null) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = color,
            contentColor = palette.onSurface,
            border = border,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Box(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = color,
            contentColor = palette.onSurface,
            border = border,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            onClick = onClick,
        ) {
            Box(Modifier.padding(contentPadding), content = content)
        }
    }
}

@Composable
internal fun ActionChipButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    leadingDot: Boolean = false,
) {
    val palette = LocalFishPiPalette.current
    val shape = RoundedCornerShape(UiLayerTokens.ChipRadius)
    val bg = when {
        selected -> palette.accent
        enabled -> palette.surface.copy(alpha = 0.92f)
        else -> palette.surfaceContainer.copy(alpha = 0.48f)
    }
    val fg = when {
        selected -> Color.White
        enabled -> palette.onSurface
        else -> palette.weakText.copy(alpha = 0.62f)
    }
    Row(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(
                1.dp,
                if (selected) palette.toolDefault.copy(alpha = 0.42f) else palette.outline.copy(alpha = 0.18f),
                shape,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leadingDot) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (selected) palette.toolDefault else palette.accent.copy(alpha = 0.68f)),
            )
        }
        Text(text = text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun IconActionButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    size: Dp = 38.dp,
    iconSize: Dp = 19.dp,
) {
    val palette = LocalFishPiPalette.current
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(if (selected) palette.accent.copy(alpha = 0.12f) else palette.surface.copy(alpha = 0.72f))
            .border(
                1.dp,
                if (selected) palette.accent.copy(alpha = 0.26f) else palette.outline.copy(alpha = 0.14f),
                shape,
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = when {
                !enabled -> palette.weakText.copy(alpha = 0.42f)
                selected -> palette.accent
                else -> palette.weakText
            },
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
internal fun FloatingNoticePill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = LocalFishPiPalette.current
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = palette.surface.copy(alpha = 0.96f),
        contentColor = if (enabled) palette.accent else palette.weakText,
        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.18f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            color = if (enabled) palette.accent else palette.weakText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
internal fun statusSuccessColor(): Color =
    LocalFishPiPalette.current.toolDefault

@Composable
internal fun statusWarningColor(): Color =
    MaterialTheme.colorScheme.error
