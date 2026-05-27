package dev.fishpi.mobile.shared.message.ui

import dev.fishpi.mobile.shared.message.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.ui.graphics.vector.ImageVector
import dev.fishpi.mobile.data.ChatRoomMessage

internal data class MessageActionSpec(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean,
    val danger: Boolean = false,
    val onClick: () -> Unit,
)

internal fun mainMessageActionSpecs(
    message: ChatRoomMessage,
    canRevoke: Boolean,
    onCopyContent: () -> Unit,
    onMentionUser: (() -> Unit)?,
    onQuote: () -> Unit,
    onRepeat: () -> Unit,
    onRevoke: () -> Unit,
    onMore: () -> Unit,
    onReactions: () -> Unit,
): List<MessageActionSpec> = buildList {
    add(
        MessageActionSpec(
            label = "复制",
            icon = Icons.Rounded.ContentCopy,
            enabled = message.copyableText().isNotBlank(),
            onClick = onCopyContent,
        ),
    )
    add(
        MessageActionSpec(
            label = "引用",
            icon = Icons.Rounded.FormatQuote,
            enabled = message.canQuote(),
            onClick = onQuote,
        ),
    )
    add(
        MessageActionSpec(
            label = "@TA",
            icon = Icons.Rounded.AlternateEmail,
            enabled = onMentionUser != null && message.userName.isNotBlank(),
            onClick = onMentionUser ?: {},
        ),
    )
    add(
        MessageActionSpec(
            label = "复读",
            icon = Icons.Rounded.Replay,
            enabled = !message.revoked && message.copyableText().isNotBlank(),
            onClick = onRepeat,
        ),
    )
    add(
        MessageActionSpec(
            label = "表情",
            icon = Icons.Rounded.SentimentSatisfied,
            enabled = true,
            onClick = onReactions,
        ),
    )
    if (canRevoke) {
        add(
            MessageActionSpec(
                label = "撤回",
                icon = Icons.Rounded.DeleteOutline,
                enabled = true,
                danger = true,
                onClick = onRevoke,
            ),
        )
    }
    add(
        MessageActionSpec(
            label = "更多",
            icon = Icons.Rounded.MoreHoriz,
            enabled = true,
            onClick = onMore,
        ),
    )
}

internal fun moreMessageActionSpecs(
    message: ChatRoomMessage,
    onCopyUsername: () -> Unit,
    onCopyImageLinks: () -> Unit,
    onCopyLinks: () -> Unit,
    onBack: () -> Unit,
): List<MessageActionSpec> = listOf(
    MessageActionSpec(
        label = "返回",
        icon = Icons.Rounded.MoreHoriz,
        enabled = true,
        onClick = onBack,
    ),
    MessageActionSpec(
        label = "用户名",
        icon = Icons.Rounded.Person,
        enabled = message.userName.isNotBlank() || message.displayName.isNotBlank(),
        onClick = onCopyUsername,
    ),
    MessageActionSpec(
        label = "图片",
        icon = Icons.Rounded.ContentCopy,
        enabled = message.imageUrls.isNotEmpty(),
        onClick = onCopyImageLinks,
    ),
    MessageActionSpec(
        label = "链接",
        icon = Icons.Rounded.ContentCopy,
        enabled = message.linkUrls.isNotEmpty(),
        onClick = onCopyLinks,
    ),
)

internal fun sheetMessageActionSpecs(
    message: ChatRoomMessage,
    canRevoke: Boolean,
    onCopyContent: () -> Unit,
    onCopyUsername: () -> Unit,
    onMentionUser: (() -> Unit)?,
    onQuote: () -> Unit,
    onRepeat: () -> Unit,
    onRevoke: () -> Unit,
): List<MessageActionSpec> = buildList {
    add(
        MessageActionSpec(
            label = "复制用户名",
            icon = Icons.Rounded.Person,
            enabled = message.userName.isNotBlank() || message.displayName.isNotBlank(),
            onClick = onCopyUsername,
        ),
    )
    if (onMentionUser != null) {
        add(
            MessageActionSpec(
                label = "@TA",
                icon = Icons.Rounded.Person,
                enabled = message.userName.isNotBlank(),
                onClick = onMentionUser,
            ),
        )
    }
    addAll(
        listOf(
            MessageActionSpec("引用", Icons.Rounded.FormatQuote, message.canQuote(), onClick = onQuote),
            MessageActionSpec("复读", Icons.Rounded.Replay, !message.revoked && message.copyableText().isNotBlank(), onClick = onRepeat),
            MessageActionSpec("复制", Icons.Rounded.ContentCopy, message.copyableText().isNotBlank(), onClick = onCopyContent),
            MessageActionSpec("撤回", Icons.Rounded.DeleteOutline, canRevoke, danger = true, onClick = onRevoke),
        ),
    )
}

