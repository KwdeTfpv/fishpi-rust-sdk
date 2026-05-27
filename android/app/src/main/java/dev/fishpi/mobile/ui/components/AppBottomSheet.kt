package dev.fishpi.mobile.ui.components

import dev.fishpi.mobile.ui.components.silentTap

import dev.fishpi.mobile.ui.components.consumeTaps

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.fishpi.mobile.FishPiErrorRed
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.ui.components.silentTap

@Composable
internal fun AppBottomSheet(
    onDismiss: () -> Unit,
    withNavigationBarPadding: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.18f))
            .silentTap(onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .then(if (withNavigationBarPadding) Modifier.navigationBarsPadding() else Modifier)
                .shadow(10.dp, RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp), clip = false)
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            FishPiTheme.surface.copy(alpha = 0.96f),
                            FishPiTheme.surfaceContainer.copy(alpha = 0.92f),
                        ),
                    ),
                )
                .border(1.dp, FishPiTheme.outline.copy(alpha = 0.16f), RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .consumeTaps()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = {
                AppSheetHandle()
                content()
            },
        )
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
                .clip(RoundedCornerShape(999.dp))
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
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
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
            .clip(RoundedCornerShape(12.dp))
                .background(FishPiTheme.surface.copy(alpha = 0.74f))
                .border(1.dp, FishPiTheme.outline.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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




