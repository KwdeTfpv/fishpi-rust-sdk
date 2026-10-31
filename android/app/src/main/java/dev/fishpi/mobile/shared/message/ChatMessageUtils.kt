package dev.fishpi.mobile.shared.message

import dev.fishpi.mobile.AppSession
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import dev.fishpi.mobile.data.ChatContentKind
import dev.fishpi.mobile.data.ChatMediaKind
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.utils.toChatTimeLabel
import dev.fishpi.mobile.utils.toEpochMillisOrNull
import dev.fishpi.mobile.utils.toSystemLocalDate

internal val ChatRoomMessage.renderSource: String
    get() = content

internal fun ChatRoomMessage.copyableText(): String {
    val text = content.trim()

    return buildList {
        if (text.isNotBlank()) {
            add(text)
        }

        mediaUrls(ChatMediaKind.Image)
            .filter(String::isNotBlank)
            .distinct()
            .takeIf { it.isNotEmpty() }
            ?.joinToString("\n")
            ?.let(::add)
        mediaUrls(ChatMediaKind.Link)
            .filter(String::isNotBlank)
            .distinct()
            .takeIf { it.isNotEmpty() }
            ?.joinToString("\n")
            ?.let(::add)
    }.joinToString("\n\n").takeIf { it.isNotBlank() } ?: run {
        allRenderableImageUrls().takeIf { it.isNotEmpty() }?.joinToString("\n").orEmpty()
    }
}

internal fun Context.copyToClipboard(label: String, text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

internal fun ChatRoomMessage.repeatableDraftContent(includeImageFallback: Boolean = true): String {
    val text = content.trim()
    if (text.isNotBlank() || !includeImageFallback) {
        return text
    }
    return mediaUrls(ChatMediaKind.Image).joinToString("\n") { url -> "![图片]($url)" }
}

internal fun ChatRoomMessage.canBeRevokedBy(
    session: AppSession,
    revoked: Boolean = false,
    allowRevokeOthers: Boolean = session.user.canRevokeOthers,
    excludeRedPacket: Boolean = true,
): Boolean {
    if (revoked || oId.isBlank() || (excludeRedPacket && contentKind == ChatContentKind.RedPacket)) {
        return false
    }
    return userName.equals(session.user.userName, ignoreCase = true) || allowRevokeOthers
}

internal fun messageTimeSeparator(previous: ChatRoomMessage?, current: ChatRoomMessage): String? {
    if (current.time.isBlank()) {
        return null
    }
    val currentTime = current.time.toEpochMillisOrNull() ?: return null
    val previousTime = previous?.time?.toEpochMillisOrNull()
    if (previousTime == null || currentTime - previousTime >= 10 * 60 * 1000) {
        return currentTime.toChatTimeLabel()
    }
    val currentDate = currentTime.toSystemLocalDate()
    val previousDate = previousTime.toSystemLocalDate()
    return if (currentDate != previousDate) currentTime.toChatTimeLabel() else null
}
