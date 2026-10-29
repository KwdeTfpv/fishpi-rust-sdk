package dev.fishpi.mobile.shared.message.ui

import dev.fishpi.mobile.*
import dev.fishpi.mobile.shared.message.*
import dev.fishpi.mobile.ui.components.consumeTaps
import dev.fishpi.mobile.ui.components.silentTap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.fishpi.mobile.shared.message.native.MessageActionAnchor
import dev.fishpi.mobile.ui.blur.FishPiBlurOverlay
import dev.fishpi.mobile.ui.glass.FishPiGlassRole
import dev.fishpi.mobile.ui.glass.LocalFishPiGlassBackdrop
import dev.fishpi.mobile.ui.glass.fishPiGlassSurface
import kotlin.math.roundToInt

private enum class MessageActionSubmenu {
    Main,
    Reactions,
}

private const val ACTIONS_PER_ROW = 6

@Composable
internal fun MessageActionBubbleMenu(
    anchor: MessageActionAnchor,
    rootOffsetInWindow: IntOffset,
    rootSize: IntSize,
    canRevoke: Boolean,
    onDismiss: () -> Unit,
    onCopyContent: () -> Unit,
    onCopyUsername: () -> Unit,
    onMentionUser: (() -> Unit)? = null,
    onQuote: () -> Unit,
    onReaction: (String) -> Unit,
    onRepeat: () -> Unit,
    onRevoke: () -> Unit,
    onCopyImage: (() -> Unit)? = null,
    onCopyImageLink: (() -> Unit)? = null,
    onFavoriteEmoji: (() -> Unit)? = null,
    extraActions: List<MessageActionSpec> = emptyList(),
) {
    var submenu by remember(anchor.message.oId) { mutableStateOf(MessageActionSubmenu.Main) }
    val density = LocalDensity.current
    val palette = LocalFishPiPalette.current
    val menuBackground = palette.surface.copy(alpha = FishPiTheme.glass.floatingAlpha)
    val menuBorder = palette.outline.copy(alpha = 0.18f)
    val contentColor = palette.onSurface
    val disabledColor = palette.weakText.copy(alpha = 0.44f)
    val selectedBackground = palette.accent.copy(alpha = 0.14f)
    val itemPressedBackground = Color.Transparent
    val actions = bubbleMessageActionSpecs(
        message = anchor.message,
        canRevoke = canRevoke,
        onCopyContent = { onCopyContent(); onDismiss() },
        onCopyUsername = { onCopyUsername(); onDismiss() },
        onMentionUser = onMentionUser?.let { action -> { action(); onDismiss() } },
        onQuote = { onQuote(); onDismiss() },
        onRepeat = { onRepeat(); onDismiss() },
        onRevoke = { onRevoke(); onDismiss() },
        onReactions = { submenu = MessageActionSubmenu.Reactions },
        onCopyImage = onCopyImage?.let { action -> { action(); onDismiss() } },
        onCopyImageLink = onCopyImageLink?.let { action -> { action(); onDismiss() } },
        onFavoriteEmoji = onFavoriteEmoji?.let { action -> { action(); onDismiss() } },
        extraActions = extraActions.map { action ->
            action.copy(onClick = { action.onClick(); onDismiss() })
        },
    )
    val rowCount = (actions.size + ACTIONS_PER_ROW - 1) / ACTIONS_PER_ROW
    val columnCount = minOf(actions.size, ACTIONS_PER_ROW)
    val menuWidth = when (submenu) {
        MessageActionSubmenu.Main -> (columnCount * 38 + 14).dp
        MessageActionSubmenu.Reactions -> 218.dp
    }
    val menuHeight = when (submenu) {
        MessageActionSubmenu.Main -> (rowCount * 38 + 8).dp
        MessageActionSubmenu.Reactions -> 44.dp
    }
    val horizontalPadding = 8.dp
    val topPadding = 56.dp
    val gap = 7.dp
    val menuWidthPx = with(density) { menuWidth.toPx() }
    val menuHeightPx = with(density) { menuHeight.toPx() }
    val horizontalPaddingPx = with(density) { horizontalPadding.toPx() }
    val topPaddingPx = with(density) { topPadding.toPx() }
    val gapPx = with(density) { gap.toPx() }
    val localLeft = anchor.rectInWindow.left - rootOffsetInWindow.x
    val localRight = anchor.rectInWindow.right - rootOffsetInWindow.x
    val localTop = anchor.rectInWindow.top - rootOffsetInWindow.y
    val localBottom = anchor.rectInWindow.bottom - rootOffsetInWindow.y
    val menuShape = RoundedCornerShape(FishPiTheme.radiusBox)
    val alignedX = if (anchor.isMine) localRight - menuWidthPx else localLeft.toFloat()
    val maxX = (rootSize.width - menuWidthPx - horizontalPaddingPx).coerceAtLeast(horizontalPaddingPx)
    val x = alignedX.coerceIn(horizontalPaddingPx, maxX).roundToInt()
    val above = localTop - menuHeightPx - gapPx
    val placeBelow = above < topPaddingPx
    val y = if (placeBelow) {
        val below = localBottom + gapPx
        val maxY = (rootSize.height - menuHeightPx - horizontalPaddingPx).coerceAtLeast(topPaddingPx)
        below.coerceIn(topPaddingPx, maxY).roundToInt()
    } else {
        above.roundToInt()
    }

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
            overlayColor = Color.Black.copy(alpha = 0.08f),
            blurRadius = 18.dp,
        ) {
            Box(
                modifier = Modifier
                    .padding(start = with(density) { x.toDp() }, top = with(density) { y.toDp() })
                    .width(menuWidth),
            ) {
                Column(
                    modifier = Modifier
                        .width(menuWidth)
                        .shadow(4.dp, menuShape, clip = false)
                        .clip(menuShape)
                        .fishPiGlassSurface(
                            backdrop = LocalFishPiGlassBackdrop.current,
                            shape = menuShape,
                            containerColor = menuBackground,
                            borderColor = menuBorder,
                            borderWidth = FishPiTheme.borderWidth,
                            role = FishPiGlassRole.Floating,
                        )
                        .consumeTaps()
                        .padding(
                            horizontal = FishPiTheme.spacingControl * 0.45f,
                            vertical = FishPiTheme.spacingControl * 0.36f,
                        ),
                    verticalArrangement = Arrangement.Center,
                ) {
                    when (submenu) {
                        MessageActionSubmenu.Main -> ActionGrid(
                            actions = actions,
                            itemBackground = itemPressedBackground,
                            contentColor = contentColor,
                            disabledColor = disabledColor,
                        )
                        MessageActionSubmenu.Reactions -> ReactionActionRow(
                            contentColor = contentColor,
                            selectedBackground = selectedBackground,
                            itemBackground = itemPressedBackground,
                            selected = anchor.message.currentUserReaction,
                            onReaction = { value -> onReaction(value); onDismiss() },
                            onBack = { submenu = MessageActionSubmenu.Main },
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(
                            when {
                                placeBelow && anchor.isMine -> Alignment.TopEnd
                                placeBelow -> Alignment.TopStart
                                anchor.isMine -> Alignment.BottomEnd
                                else -> Alignment.BottomStart
                            },
                        )
                        .offset(
                            x = if (anchor.isMine) (-20).dp else 20.dp,
                            y = if (placeBelow) (-4).dp else 4.dp,
                        )
                        .size(9.dp)
                        .rotate(45f)
                        .background(menuBackground),
                )
            }
        }
    }
}

@Composable
private fun ActionGrid(
    actions: List<MessageActionSpec>,
    itemBackground: Color,
    contentColor: Color,
    disabledColor: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        actions.chunked(ACTIONS_PER_ROW).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                row.forEach { action ->
                    MenuAction(
                        action = action,
                        itemBackground = itemBackground,
                        contentColor = contentColor,
                        disabledColor = disabledColor,
                    )
                }
                repeat(ACTIONS_PER_ROW - row.size) {
                    Box(modifier = Modifier.width(37.dp))
                }
            }
        }
    }
}

@Composable
private fun ReactionActionRow(
    contentColor: Color,
    selectedBackground: Color,
    itemBackground: Color,
    selected: String,
    onReaction: (String) -> Unit,
    onBack: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "返回",
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(FishPiTheme.radiusField * 0.66f))
                .background(itemBackground)
                .clickable(onClick = onBack)
                .padding(
                    horizontal = FishPiTheme.spacingControl * 0.64f,
                    vertical = FishPiTheme.spacingControl * 0.55f,
                ),
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            QuickReactionOptions.forEach { option ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(FishPiTheme.radiusField * 0.66f))
                        .background(if (selected == option.value) selectedBackground else Color.Transparent)
                        .clickable { onReaction(option.value) }
                        .padding(FishPiTheme.spacingControl * 0.55f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = option.emoji)
                }
            }
        }
    }
}

@Composable
private fun MenuAction(
    action: MessageActionSpec,
    itemBackground: Color,
    contentColor: Color,
    disabledColor: Color,
) {
    val color = when {
        !action.enabled -> disabledColor
        action.danger -> FishPiErrorRed
        else -> contentColor
    }
    Column(
        modifier = Modifier
            .width(37.dp)
            .clip(RoundedCornerShape(FishPiTheme.radiusField * 0.66f))
            .background(if (action.enabled) itemBackground else Color.Transparent)
            .clickable(enabled = action.enabled, onClick = action.onClick)
            .padding(
                horizontal = FishPiTheme.spacingControl * 0.18f,
                vertical = FishPiTheme.spacingControl * 0.27f,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Icon(imageVector = action.icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(text = action.label, color = color, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, maxLines = 1)
    }
}


