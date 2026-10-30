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
import androidx.compose.runtime.remember
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
import dev.fishpi.mobile.FishPiPalette
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.LocalFishPiPalette
import dev.fishpi.mobile.LocalFishPiThemeTokens
import dev.fishpi.mobile.ui.glass.FishPiGlassRole
import dev.fishpi.mobile.ui.glass.LocalFishPiGlassBackdrop
import dev.fishpi.mobile.ui.glass.fishPiGlassSurface
import dev.fishpi.mobile.ui.motion.fishClickable

internal object UiLayerTokens {
    const val ZPage = 0f
    const val ZControl = 10f
    const val ZFloating = 30f
    const val ZPanel = 50f
    const val ZDialog = 80f
    const val ZPreview = 100f
}

private fun pageBrushOf(palette: FishPiPalette): Brush {
    if (palette.wallpaperColors.size >= 4) {
        return Brush.linearGradient(
            colors = palette.wallpaperColors,
        )
    }
    return Brush.verticalGradient(
        listOf(
            palette.background,
            palette.chatBackground,
            palette.surface,
        ),
    )
}

@Composable
internal fun uiPageBrush(): Brush = pageBrushOf(LocalFishPiPalette.current)

@Composable
internal fun rememberUiPageBrush(): Brush {
    val palette = LocalFishPiPalette.current
    return remember(palette) { pageBrushOf(palette) }
}

@Composable
internal fun UiLayerScaffold(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(rememberUiPageBrush()),
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
    val backdrop = LocalFishPiGlassBackdrop.current
    Box(
        modifier = modifier
            .fishPiGlassSurface(
                backdrop = backdrop,
                shape = shape,
                containerColor = palette.surface.copy(alpha = FishPiTheme.glass.surfaceAlpha),
                borderColor = palette.accent.copy(alpha = 0.18f + FishPiTheme.depth * 0.12f),
                borderWidth = FishPiTheme.borderWidth,
                role = FishPiGlassRole.Control,
            ),
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
    val clickableModifier = onClick?.let { Modifier.fishClickable(onClick = it) } ?: Modifier
    Box(
        modifier = modifier
            .fishPiGlassSurface(
                backdrop = LocalFishPiGlassBackdrop.current,
                shape = shape,
                containerColor = palette.surface.copy(alpha = FishPiTheme.glass.elevatedAlpha),
                borderColor = palette.outline.copy(
                    alpha = 0.12f + FishPiTheme.depth * 0.10f,
                ),
                borderWidth = FishPiTheme.borderWidth,
                role = FishPiGlassRole.Content,
            )
            .then(clickableModifier),
    ) {
        Box(Modifier.padding(contentPadding), content = content)
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
        selected -> palette.accent.copy(alpha = 0.82f)
        enabled -> palette.surface.copy(alpha = FishPiTheme.glass.surfaceAlpha)
        else -> palette.surfaceContainer.copy(alpha = 0.42f)
    }
    val fg = when {
        selected -> themeColors.primaryContent
        enabled -> palette.onSurface
        else -> palette.weakText.copy(alpha = 0.62f)
    }
    Row(
        modifier = modifier
            .clip(shape)
            .fishPiGlassSurface(
                backdrop = LocalFishPiGlassBackdrop.current,
                shape = shape,
                containerColor = bg,
                borderColor = if (selected) fg.copy(alpha = 0.28f) else palette.outline.copy(alpha = 0.18f),
                borderWidth = FishPiTheme.borderWidth,
                role = FishPiGlassRole.Control,
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
            .fishPiGlassSurface(
                backdrop = LocalFishPiGlassBackdrop.current,
                shape = shape,
                containerColor = if (selected) palette.accent.copy(alpha = 0.16f) else palette.surface.copy(alpha = FishPiTheme.glass.surfaceAlpha),
                borderColor = if (selected) palette.accent.copy(alpha = 0.26f) else palette.outline.copy(alpha = 0.14f),
                borderWidth = FishPiTheme.borderWidth,
                role = FishPiGlassRole.Control,
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
    val shape = RoundedCornerShape(FishPiTheme.radiusField)
    Box(
        modifier = modifier
            .fishPiGlassSurface(
                backdrop = LocalFishPiGlassBackdrop.current,
                shape = shape,
                containerColor = palette.surface.copy(alpha = FishPiTheme.glass.elevatedAlpha),
                borderColor = palette.outline.copy(
                    alpha = 0.18f + FishPiTheme.depth * 0.10f,
                ),
                borderWidth = FishPiTheme.borderWidth,
                role = FishPiGlassRole.Floating,
            )
            .fishClickable(enabled = enabled, onClick = onClick),
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
