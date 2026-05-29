package dev.fishpi.mobile.shared.message.ui

import dev.fishpi.mobile.*
import dev.fishpi.mobile.shared.message.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.ui.components.AppBottomSheet
import dev.fishpi.mobile.ui.components.AppSheetActionRow
import dev.fishpi.mobile.ui.components.AppSheetTitle
import androidx.compose.material3.Text

@Composable
internal fun MessageActionSheet(
    message: ChatRoomMessage,
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
    showReactions: Boolean = true,
) {
    AppBottomSheet(
        onDismiss = onDismiss,
        withNavigationBarPadding = false,
    ) {
            AppSheetTitle(message.displayName.ifBlank { "消息操作" })
            if (showReactions) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickReactionOptions.forEach { option ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(FishPiTheme.radiusField))
                                .background(
                                    if (message.currentUserReaction == option.value) {
                                        FishPiTheme.accent.copy(alpha = 0.12f)
                                    } else {
                                        FishPiTheme.surfaceContainer
                                    },
                                )
                                .clickable {
                                    onReaction(option.value)
                                    onDismiss()
                                }
                                .padding(
                                    horizontal = FishPiTheme.spacingControl,
                                    vertical = FishPiTheme.spacingControl * 0.72f,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = option.emoji)
                        }
                    }
                }
            }
            sheetMessageActionSpecs(
                message = message,
                canRevoke = canRevoke,
                onCopyContent = onCopyContent,
                onCopyUsername = onCopyUsername,
                onMentionUser = onMentionUser,
                onQuote = onQuote,
                onRepeat = onRepeat,
                onRevoke = onRevoke,
            ).forEach { action ->
                AppSheetActionRow(
                    text = action.label,
                    icon = action.icon,
                    enabled = action.enabled,
                    danger = action.danger,
                    onClick = {
                        action.onClick()
                        onDismiss()
                    },
                )
            }
    }
}




