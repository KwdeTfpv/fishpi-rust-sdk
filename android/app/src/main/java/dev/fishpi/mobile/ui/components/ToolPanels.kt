package dev.fishpi.mobile.ui.components

import dev.fishpi.mobile.ui.components.silentTap

import dev.fishpi.mobile.ui.components.consumeTaps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.fishpi.mobile.ui.components.FishPiIconButton
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.LocalFishPiPalette
import dev.fishpi.mobile.ui.components.silentTap

internal data class ChatToolAction(
    val id: String,
    val label: String,
    val icon: ImageVector = Icons.Rounded.Extension,
    val enabled: Boolean = true,
    val source: String = "builtin",
    val iconTint: Color? = null,
    val iconBackground: Color? = null,
    val onClick: () -> Unit,
)

@Composable
internal fun AppToolGridPanel(
    actions: List<ChatToolAction>,
    modifier: Modifier = Modifier,
    fixedHeight: Dp? = null,
) {
    val rowCount = ((actions.size + 3) / 4).coerceAtLeast(1)
    val visibleRows = rowCount.coerceAtMost(3)
    val panelHeight = fixedHeight ?: (
        FishPiTheme.spacingSection +
            (visibleRows * 68).dp +
            FishPiTheme.spacingItem * (visibleRows - 1).coerceAtLeast(0) / 2f
        )
    val panelShape = RoundedCornerShape(
        topStart = FishPiTheme.radiusBox + 4.dp,
        topEnd = FishPiTheme.radiusBox + 4.dp,
        bottomStart = FishPiTheme.radiusBox * 0.66f,
        bottomEnd = FishPiTheme.radiusBox * 0.66f,
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(panelHeight)
            .clip(panelShape)
            .background(FishPiTheme.surface.copy(alpha = 0.56f))
            .border(
                FishPiTheme.borderWidth,
                FishPiTheme.outline.copy(alpha = 0.08f + FishPiTheme.depth * 0.08f),
                panelShape,
            )
            .padding(horizontal = FishPiTheme.spacingItem, vertical = FishPiTheme.spacingItem / 2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 5.dp)
                .size(width = 34.dp, height = 3.dp)
                .clip(RoundedCornerShape(FishPiTheme.radiusSelector))
                .background(FishPiTheme.outline.copy(alpha = 0.22f)),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(0.dp),
            horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem / 2),
            verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem / 2),
        ) {
            items(actions, key = { it.source + ":" + it.id }) { action ->
                FishPiToolActionCell(action = action)
            }
        }
    }
}

@Composable
private fun FishPiToolActionCell(action: ChatToolAction) {
    val tint = if (action.enabled) action.iconTint ?: LocalFishPiPalette.current.toolDefault else FishPiTheme.weakText.copy(alpha = 0.42f)
    val labelColor = if (action.enabled) FishPiTheme.onSurface else FishPiTheme.weakText.copy(alpha = 0.46f)
    Column(
        modifier = Modifier
            .heightIn(min = 62.dp)
            .clip(RoundedCornerShape(FishPiTheme.radiusField))
            .clickable(enabled = action.enabled, onClick = action.onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem / 4),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(if (action.id.hashCode() % 2 == 0) CircleShape else RoundedCornerShape(FishPiTheme.radiusField)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                tint = tint,
                modifier = Modifier.size(21.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(width = 18.dp, height = 2.dp)
                .clip(RoundedCornerShape(FishPiTheme.radiusSelector))
                .background(
                    if (action.enabled) tint.copy(alpha = 0.32f)
                    else FishPiTheme.weakText.copy(alpha = 0.16f),
                ),
        )
        Text(
            text = action.label,
            color = labelColor,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun AppTaskPanel(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.34f))
            .silentTap(onDismiss)
            .padding(horizontal = FishPiTheme.spacingPage)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .height(maxHeight * 0.86f)
                .clip(RoundedCornerShape(FishPiTheme.radiusBox + 8.dp))
                .background(FishPiTheme.surface)
                .consumeTaps(),
            content = content,
        )
    }
}

@Composable
internal fun AppFullScreenWorkspace(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FishPiTheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .background(FishPiTheme.surface)
                .padding(horizontal = FishPiTheme.spacingPage, vertical = FishPiTheme.spacingControl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem / 4)) {
                Text(
                    text = title,
                    color = FishPiTheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = FishPiTheme.weakText,
                        fontSize = 12.sp,
                    )
                }
            }
            FishPiIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = "关闭$title",
                onClick = onDismiss,
            )
        }
        content()
    }
}




