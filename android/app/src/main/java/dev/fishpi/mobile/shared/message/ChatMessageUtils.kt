package dev.fishpi.mobile.shared.message

import dev.fishpi.mobile.AppSession
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.utils.toChatTimeLabel
import dev.fishpi.mobile.utils.toEpochMillisOrNull
import dev.fishpi.mobile.utils.toSystemLocalDate

internal fun ChatRoomMessage.copyableText(): String {
    val text = content.trim()
    val mdText = md.trim()
    val quoteText = quote?.text?.trim().orEmpty()
    val quoteImages = quote?.imageUrls
        ?.filter(String::isNotBlank)
        ?.distinct()
        .orEmpty()

    return buildList {
        when {
            text.isNotBlank() -> add(text)
            mdText.isNotBlank() -> add(mdText)
        }

        if (quoteText.isNotBlank()) {
            val duplicatedInBody = text.contains(quoteText) || mdText.contains(quoteText)
            if (!duplicatedInBody) {
                add("引用消息：$quoteText")
            }
        }

        if (quoteImages.isNotEmpty()) {
            val quoteImageText = quoteImages.joinToString("\n")
            val duplicatedInBody = text.contains(quoteImageText) || mdText.contains(quoteImageText)
            if (!duplicatedInBody) {
                add("引用图片：\n$quoteImageText")
            }
        }

        imageUrls
            .filter(String::isNotBlank)
            .distinct()
            .takeIf { it.isNotEmpty() }
            ?.joinToString("\n")
            ?.let(::add)
        linkUrls
            .filter(String::isNotBlank)
            .distinct()
            .takeIf { it.isNotEmpty() }
            ?.joinToString("\n")
            ?.let(::add)
    }.joinToString("\n\n")
}

internal fun Context.copyToClipboard(label: String, text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

internal fun ChatRoomMessage.repeatableDraftContent(includeImageFallback: Boolean = false): String {
    val mdText = md.trim()
    if (mdText.isNotBlank()) {
        return mdText
    }
    val text = content.trim()
    if (text.isNotBlank() || !includeImageFallback) {
        return text
    }
    return imageUrls.joinToString("\n") { url -> "![图片]($url)" }
}

internal fun List<ChatRoomMessage>.markMessageRevoked(messageId: String): List<ChatRoomMessage> {
    if (messageId.isBlank()) {
        return this
    }
    return map { message ->
        if (message.oId == messageId) {
            message.asRevoked()
        } else {
            message
        }
    }
}

internal fun ChatRoomMessage.canBeRevokedBy(
    session: AppSession,
    allowRevokeOthers: Boolean = session.user.canRevokeOthers,
    excludeRedPacket: Boolean = true,
): Boolean {
    if (revoked || oId.isBlank() || (excludeRedPacket && type == "redPacket")) {
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

private fun ChatRoomMessage.asRevoked(): ChatRoomMessage =
    copy(
        content = "[该消息已撤回]",
        contentHtml = "",
        imageUrls = emptyList(),
        linkUrls = emptyList(),
        revoked = true,
    )

