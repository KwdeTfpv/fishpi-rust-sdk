package dev.fishpi.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import dev.fishpi.mobile.LocalFishPiThemeTokens
import dev.fishpi.mobile.ui.motion.fishClickable

internal object UiLayerTokens {
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
    shape: RoundedCornerShape = RoundedCornerShape(FishPiTheme.radiusField),
    contentPadding: PaddingValues = PaddingValues(
        horizontal = FishPiTheme.spacingControl,
        vertical = FishPiTheme.spacingControl * 0.75f,
    ),
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
        border = BorderStroke(FishPiTheme.borderWidth, palette.accent.copy(alpha = 0.18f + FishPiTheme.depth * 0.12f)),
    ) {
        Box(Modifier.padding(contentPadding), content = content)
    }
}

@Composable
internal fun ContentCardSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(FishPiTheme.spacingSection),
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = LocalFishPiPalette.current
    val shape = RoundedCornerShape(FishPiTheme.radiusBox)
    val color = palette.surface.copy(alpha = 0.78f)
    val border = BorderStroke(FishPiTheme.borderWidth, palette.outline.copy(alpha = 0.12f + FishPiTheme.depth * 0.10f))
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
    val themeColors = LocalFishPiThemeTokens.current.colors
    val shape = RoundedCornerShape(FishPiTheme.radiusSelector)
    val bg = when {
        selected -> palette.accent
        enabled -> palette.surface.copy(alpha = 0.92f)
        else -> palette.surfaceContainer.copy(alpha = 0.48f)
    }
    val fg = when {
        selected -> themeColors.primaryContent
        enabled -> palette.onSurface
        else -> palette.weakText.copy(alpha = 0.62f)
    }
    Row(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(
                FishPiTheme.borderWidth,
                if (selected) fg.copy(alpha = 0.28f) else palette.outline.copy(alpha = 0.18f),
                shape,
            )
            .fishClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = FishPiTheme.spacingControl, vertical = FishPiTheme.spacingControl * 0.45f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem * 0.75f),
    ) {
        if (leadingDot) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (selected) fg.copy(alpha = 0.82f) else palette.accent.copy(alpha = 0.68f)),
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
    val shape = RoundedCornerShape(FishPiTheme.radiusField)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(if (selected) palette.accent.copy(alpha = 0.12f) else palette.surface.copy(alpha = 0.72f))
            .border(
                FishPiTheme.borderWidth,
                if (selected) palette.accent.copy(alpha = 0.26f) else palette.outline.copy(alpha = 0.14f),
                shape,
            )
            .fishClickable(enabled = enabled, rippleBounded = false, onClick = onClick),
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
        shape = RoundedCornerShape(FishPiTheme.radiusField),
        color = palette.surface.copy(alpha = 0.96f),
        contentColor = if (enabled) palette.accent else palette.weakText,
        border = BorderStroke(FishPiTheme.borderWidth, palette.accent.copy(alpha = 0.18f + FishPiTheme.depth * 0.10f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = FishPiTheme.spacingControl,
                vertical = FishPiTheme.spacingControl * 0.64f,
            ),
            color = if (enabled) palette.accent else palette.weakText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
internal fun statusSuccessColor(): Color =
    FishPiTheme.success

@Composable
internal fun statusWarningColor(): Color =
    FishPiTheme.warning
