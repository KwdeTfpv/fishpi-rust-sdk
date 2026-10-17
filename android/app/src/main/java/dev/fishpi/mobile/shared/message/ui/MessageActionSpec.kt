package dev.fishpi.mobile.shared.message.ui

import dev.fishpi.mobile.shared.message.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.ui.graphics.vector.ImageVector
import dev.fishpi.mobile.data.ChatRoomMessage

internal data class MessageActionSpec(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean,
    val danger: Boolean = false,
    val onClick: () -> Unit,
)


internal fun bubbleMessageActionSpecs(
    message: ChatRoomMessage,
    canRevoke: Boolean,
    onCopyContent: () -> Unit,
    onCopyUsername: () -> Unit,
    onMentionUser: (() -> Unit)?,
    onQuote: () -> Unit,
    onRepeat: () -> Unit,
    onRevoke: () -> Unit,
    onReactions: () -> Unit,
    onCopyImage: (() -> Unit)? = null,
    onCopyImageLink: (() -> Unit)? = null,
    onFavoriteEmoji: (() -> Unit)? = null,
    extraActions: List<MessageActionSpec> = emptyList(),
): List<MessageActionSpec> = buildList {
    val hasImage = message.allRenderableImageUrls().isNotEmpty()
    val hasContent = message.copyableText().isNotBlank() || hasImage
    add(
        MessageActionSpec(
            label = "复制",
            icon = Icons.Rounded.ContentCopy,
            enabled = hasContent,
            onClick = onCopyContent,
        ),
    )
    if (onCopyImage != null && hasImage) {
        add(
            MessageActionSpec(
                label = "复制图片",
                icon = Icons.Rounded.Image,
                enabled = true,
                onClick = onCopyImage,
            ),
        )
    }
    if (onCopyImageLink != null && hasImage) {
        add(
            MessageActionSpec(
                label = "复制链接",
                icon = Icons.Rounded.Link,
                enabled = true,
                onClick = onCopyImageLink,
            ),
        )
    }
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
            enabled = !message.revoked && hasContent,
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
    if (onFavoriteEmoji != null && hasImage) {
        add(
            MessageActionSpec(
                label = "收藏",
                icon = Icons.Rounded.StarOutline,
                enabled = true,
                onClick = onFavoriteEmoji,
            ),
        )
    }
    add(
        MessageActionSpec(
            label = "用户名",
            icon = Icons.Rounded.Person,
            enabled = message.userName.isNotBlank() || message.displayName.isNotBlank(),
            onClick = onCopyUsername,
        ),
    )
    addAll(extraActions)
}

internal fun sheetMessageActionSpecs(
    message: ChatRoomMessage,
    canRevoke: Boolean,
    onCopyContent: () -> Unit,
    onCopyUsername: () -> Unit,
    onMentionUser: (() -> Unit)?,
    onQuote: () -> Unit,
    onRepeat: () -> Unit,
    onRevoke: () -> Unit,
    onCopyImage: (() -> Unit)? = null,
    onCopyImageLink: (() -> Unit)? = null,
    onFavoriteEmoji: (() -> Unit)? = null,
): List<MessageActionSpec> = buildList {
    val hasImage = message.allRenderableImageUrls().isNotEmpty()
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
            MessageActionSpec("复读", Icons.Rounded.Replay, !message.revoked && (message.copyableText().isNotBlank() || hasImage), onClick = onRepeat),
            MessageActionSpec("复制", Icons.Rounded.ContentCopy, message.copyableText().isNotBlank() || hasImage, onClick = onCopyContent),
            MessageActionSpec("撤回", Icons.Rounded.DeleteOutline, canRevoke, danger = true, onClick = onRevoke),
        ),
    )
    if (onCopyImage != null && hasImage) {
        add(
            MessageActionSpec(
                label = "复制图片",
                icon = Icons.Rounded.Image,
                enabled = true,
                onClick = onCopyImage,
            ),
        )
    }
    if (onCopyImageLink != null && hasImage) {
        add(
            MessageActionSpec(
                label = "复制图片链接",
                icon = Icons.Rounded.Link,
                enabled = true,
                onClick = onCopyImageLink,
            ),
        )
    }
    if (onFavoriteEmoji != null && hasImage) {
        add(
            MessageActionSpec(
                label = "收藏",
                icon = Icons.Rounded.StarOutline,
                enabled = true,
                onClick = onFavoriteEmoji,
            ),
        )
    }
}

