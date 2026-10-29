package dev.fishpi.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.fishpi.mobile.FishPiErrorRed
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.ui.blur.FishPiBlurOverlay
import dev.fishpi.mobile.ui.glass.FishPiGlassRole
import dev.fishpi.mobile.ui.glass.LocalFishPiGlassBackdrop
import dev.fishpi.mobile.ui.glass.fishPiGlassSurface

@Composable
internal fun AppBottomSheet(
    onDismiss: () -> Unit,
    withNavigationBarPadding: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        FishPiBlurOverlay(
            modifier = Modifier
                .fillMaxSize()
                .silentTap(onDismiss),
            overlayColor = Color.Black.copy(alpha = 0.14f),
            blurRadius = 24.dp,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                val sheetShape = RoundedCornerShape(
                    topStart = FishPiTheme.radiusBox + 16.dp,
                    topEnd = FishPiTheme.radiusBox + 16.dp,
                )
                val backdrop = LocalFishPiGlassBackdrop.current
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .then(if (withNavigationBarPadding) Modifier.navigationBarsPadding() else Modifier)
                        .fishPiGlassSurface(
                            backdrop = backdrop,
                            shape = sheetShape,
                            containerColor = FishPiTheme.surface.copy(alpha = FishPiTheme.glass.floatingAlpha),
                            borderColor = FishPiTheme.outline.copy(alpha = 0.18f + FishPiTheme.depth * 0.10f),
                            borderWidth = FishPiTheme.borderWidth,
                            depthEffect = true,
                            role = FishPiGlassRole.Floating,
                        )
                        .consumeTaps()
                        .padding(horizontal = FishPiTheme.spacingPage, vertical = FishPiTheme.spacingSection),
                    verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem),
                    content = {
                        AppSheetHandle()
                        content()
                    },
                )
            }
        }
    }
}

@Composable
private fun AppSheetHandle() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.14f)
                .clip(RoundedCornerShape(FishPiTheme.radiusSelector))
                .background(FishPiTheme.outline.copy(alpha = 0.42f))
                .padding(vertical = 2.dp),
        )
    }
}

@Composable
internal fun AppSheetTitle(text: String) {
    Text(
        text = text,
        color = FishPiTheme.onSurface,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = FishPiTheme.spacingItem / 2, vertical = FishPiTheme.spacingItem / 2),
    )
}

@Composable
internal fun AppSheetActionRow(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val activeColor = if (danger) FishPiErrorRed else FishPiTheme.onSurface
    val contentColor = if (enabled) activeColor else FishPiTheme.weakText.copy(alpha = 0.46f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FishPiTheme.radiusField))
                .background(FishPiTheme.surface.copy(alpha = 0.74f))
                .border(FishPiTheme.borderWidth, FishPiTheme.outline.copy(alpha = 0.12f + FishPiTheme.depth * 0.08f), RoundedCornerShape(FishPiTheme.radiusField))
                .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = FishPiTheme.spacingControl + 2.dp, vertical = FishPiTheme.spacingControl + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (danger) FishPiErrorRed.copy(alpha = 0.12f) else FishPiTheme.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (danger) FishPiErrorRed else FishPiTheme.accent,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(text = text, color = contentColor, fontWeight = FontWeight.SemiBold)
    }
}




