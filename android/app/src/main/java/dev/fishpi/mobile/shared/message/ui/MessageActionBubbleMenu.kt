package dev.fishpi.mobile.shared.message.ui

import dev.fishpi.mobile.*
import dev.fishpi.mobile.shared.message.*
import dev.fishpi.mobile.ui.components.consumeTaps
import dev.fishpi.mobile.ui.components.silentTap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.fishpi.mobile.shared.message.native.MessageActionAnchor
import dev.fishpi.mobile.data.ChatRoomMessage
import kotlin.math.roundToInt

private enum class MessageActionSubmenu {
    Main,
    More,
    Reactions,
}

@Composable
internal fun MessageActionBubbleMenu(
    anchor: MessageActionAnchor,
    rootOffsetInWindow: IntOffset,
    rootSize: IntSize,
    canRevoke: Boolean,
    onDismiss: () -> Unit,
    onCopyContent: () -> Unit,
    onCopyUsername: () -> Unit,
    onCopyImageLinks: () -> Unit,
    onCopyLinks: () -> Unit,
    onMentionUser: (() -> Unit)? = null,
    onQuote: () -> Unit,
    onReaction: (String) -> Unit,
    onRepeat: () -> Unit,
    onRevoke: () -> Unit,
    extraActions: List<MessageActionSpec> = emptyList(),
) {
    var submenu by remember(anchor.message.oId) { mutableStateOf(MessageActionSubmenu.Main) }
    val density = LocalDensity.current
    val palette = LocalFishPiPalette.current
    val menuBackground = palette.surfaceElevated.copy(alpha = 0.98f)
    val menuBorder = palette.outline.copy(alpha = 0.18f)
    val contentColor = palette.onSurface
    val disabledColor = palette.weakText.copy(alpha = 0.44f)
    val selectedBackground = palette.accent.copy(alpha = 0.14f)
    val itemPressedBackground = Color.Transparent
    val mainActionCount = if (canRevoke) 7 else 6
    val moreActionCount = 4 + extraActions.size
    val menuWidth = when (submenu) {
        MessageActionSubmenu.Main -> (mainActionCount * 34 + 14).dp
        MessageActionSubmenu.More -> (moreActionCount * 38 + 14).dp
        MessageActionSubmenu.Reactions -> 218.dp
    }
    val menuHeight = when (submenu) {
        MessageActionSubmenu.Main -> 44.dp
        MessageActionSubmenu.More -> 44.dp
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
    val alignedX = if (anchor.isMine) localRight - menuWidthPx else localLeft.toFloat()
    val maxX = (rootSize.width - menuWidthPx - horizontalPaddingPx).coerceAtLeast(horizontalPaddingPx)
    val x = alignedX.coerceIn(horizontalPaddingPx, maxX).roundToInt()
    val y = (localTop - menuHeightPx - gapPx).coerceAtLeast(topPaddingPx).roundToInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .silentTap(onDismiss),
    ) {
        Box(
            modifier = Modifier
                .padding(start = with(density) { x.toDp() }, top = with(density) { y.toDp() })
                .width(menuWidth),
        ) {
            Column(
                modifier = Modifier
                    .width(menuWidth)
                    .shadow(4.dp, RoundedCornerShape(FishPiTheme.radiusBox), clip = false)
                    .clip(RoundedCornerShape(FishPiTheme.radiusBox))
                    .background(menuBackground)
                    .border(BorderStroke(FishPiTheme.borderWidth, menuBorder), RoundedCornerShape(FishPiTheme.radiusBox))
                    .consumeTaps()
                    .padding(
                        horizontal = FishPiTheme.spacingControl * 0.45f,
                        vertical = FishPiTheme.spacingControl * 0.36f,
                    ),
                verticalArrangement = Arrangement.Center,
            ) {
                when (submenu) {
                    MessageActionSubmenu.Main -> MainActionRow(
                        message = anchor.message,
                        canRevoke = canRevoke,
                        onCopyContent = { onCopyContent(); onDismiss() },
                        onMentionUser = onMentionUser?.let { action -> { action(); onDismiss() } },
                        onQuote = { onQuote(); onDismiss() },
                        onRepeat = { onRepeat(); onDismiss() },
                        onRevoke = { onRevoke(); onDismiss() },
                        itemBackground = itemPressedBackground,
                        contentColor = contentColor,
                        disabledColor = disabledColor,
                        onMore = { submenu = MessageActionSubmenu.More },
                        onReactions = { submenu = MessageActionSubmenu.Reactions },
                    )
                    MessageActionSubmenu.More -> MoreActionRow(
                        message = anchor.message,
                        itemBackground = itemPressedBackground,
                        contentColor = contentColor,
                        disabledColor = disabledColor,
                        onCopyUsername = { onCopyUsername(); onDismiss() },
                        onCopyImageLinks = { onCopyImageLinks(); onDismiss() },
                        onCopyLinks = { onCopyLinks(); onDismiss() },
                        onBack = { submenu = MessageActionSubmenu.Main },
                        extraActions = extraActions.map { action ->
                            action.copy(onClick = { action.onClick(); onDismiss() })
                        },
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
                    .align(if (anchor.isMine) Alignment.BottomEnd else Alignment.BottomStart)
                    .offset(
                        x = if (anchor.isMine) (-20).dp else 20.dp,
                        y = 4.dp,
                    )
                    .size(9.dp)
                    .rotate(45f)
                    .background(menuBackground)
                    .border(FishPiTheme.borderWidth, menuBorder.copy(alpha = 0.72f)),
            )
        }
    }
}

@Composable
private fun MainActionRow(
    message: ChatRoomMessage,
    canRevoke: Boolean,
    onCopyContent: () -> Unit,
    onMentionUser: (() -> Unit)?,
    onQuote: () -> Unit,
    onRepeat: () -> Unit,
    onRevoke: () -> Unit,
    itemBackground: Color,
    contentColor: Color,
    disabledColor: Color,
    onMore: () -> Unit,
    onReactions: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        mainMessageActionSpecs(
            message = message,
            canRevoke = canRevoke,
            onCopyContent = onCopyContent,
            onMentionUser = onMentionUser,
            onQuote = onQuote,
            onRepeat = onRepeat,
            onRevoke = onRevoke,
            onMore = onMore,
            onReactions = onReactions,
        ).forEach { action ->
            MenuAction(
                action = action,
                itemBackground = itemBackground,
                contentColor = contentColor,
                disabledColor = disabledColor,
            )
        }
    }
}

@Composable
private fun MoreActionRow(
    message: ChatRoomMessage,
    itemBackground: Color,
    contentColor: Color,
    disabledColor: Color,
    onCopyUsername: () -> Unit,
    onCopyImageLinks: () -> Unit,
    onCopyLinks: () -> Unit,
    onBack: () -> Unit,
    extraActions: List<MessageActionSpec>,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        moreMessageActionSpecs(
            message = message,
            onCopyUsername = onCopyUsername,
            onCopyImageLinks = onCopyImageLinks,
            onCopyLinks = onCopyLinks,
            onBack = onBack,
        ).forEach { action ->
            MenuAction(
                action = action,
                itemBackground = itemBackground,
                contentColor = contentColor,
                disabledColor = disabledColor,
            )
        }
        extraActions.forEach { action ->
            MenuAction(
                action = action,
                itemBackground = itemBackground,
                contentColor = contentColor,
                disabledColor = disabledColor,
            )
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
            .width(33.dp)
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


