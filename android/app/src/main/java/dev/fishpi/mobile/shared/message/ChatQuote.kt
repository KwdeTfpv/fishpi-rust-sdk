package dev.fishpi.mobile.shared.message

import dev.fishpi.mobile.data.ChatMediaKind
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.utils.extractImageTokens
import dev.fishpi.mobile.utils.normalizeWebUrl
import dev.fishpi.mobile.utils.toPlainMessageText

internal data class ChatQuote(
    val messageId: String,
    val username: String,
    val content: String,
    val imageUrls: List<String> = emptyList(),
    val targetUrl: String? = null,
    val title: String = "跳转至原消息",
) {
    val isValid: Boolean
        get() = messageId.isNotBlank() && username.isNotBlank() &&
            (content.isNotBlank() || imageUrls.isNotEmpty())

    val preview: String
        get() = when {
            content.isNotBlank() && imageUrls.isNotEmpty() -> "${content.take(100)} [图片]"
            content.isNotBlank() -> content.take(120)
            imageUrls.size > 1 -> "[图片 x${imageUrls.size}]"
            imageUrls.isNotEmpty() -> "[图片]"
            else -> ""
        }

    fun appendTo(
        text: String,
        targetUrl: String? = null,
        title: String? = null,
    ): String {
        if (!isValid) {
            return text
        }
        val url = targetUrl ?: this.targetUrl ?: "https://fishpi.cn/cr#chatroom$messageId"
        val jumpTitle = title ?: this.title
        val jump = if (url.isBlank()) "" else " [↩]($url \"$jumpTitle\")"
        val quoteLines = buildList {
            if (content.isNotBlank()) {
                add(content)
            }
            imageUrls.take(3).forEachIndexed { index, url ->
                add("![图片${if (imageUrls.size > 1) index + 1 else ""}]($url)")
            }
        }.joinToString("\n")
        val quoteMd = quoteLines.replace("\n", "\n> ")
        return text + "\n\n##### 引用 @$username$jump  \n> $quoteMd\n"
    }
}

internal fun ChatRoomMessage.toQuote(): ChatQuote {
    val parts = toQuoteParts()
    return ChatQuote(
        messageId = oId,
        username = userName,
        content = parts.content,
        imageUrls = parts.imageUrls,
    )
}

internal fun ChatRoomMessage.canQuote(revoked: Boolean = false): Boolean {
    val parts = toQuoteParts()
    return !revoked && oId.isNotBlank() && userName.isNotBlank() &&
        (parts.content.isNotBlank() || parts.imageUrls.isNotEmpty())
}

private data class QuoteParts(
    val content: String,
    val imageUrls: List<String>,
)

private fun ChatRoomMessage.toQuoteParts(): QuoteParts {
    val source = renderSource
    val images = allQuoteImageUrls()

    val body = source
        .removeQuoteImages()
        .toQuotePlainText()
    return QuoteParts(
        content = body,
        imageUrls = images,
    )
}

private fun ChatRoomMessage.allQuoteImageUrls(): List<String> {
    return mediaUrls(ChatMediaKind.Image)
        .map(String::normalizeWebUrl)
        .filter(String::isNotBlank)
        .distinct()
}

private fun String.removeQuoteImages(): String {
    val source = this
    val tokens = extractImageTokens()
    if (tokens.isEmpty()) return source
    return buildString {
        var cursor = 0
        tokens.forEach { token ->
            if (token.start > cursor) {
                append(source.substring(cursor, token.start))
            }
            cursor = token.end
        }
        if (cursor < source.length) {
            append(source.substring(cursor))
        }
    }.trim()
}

private fun String.toQuotePlainText(): String {
    return toPlainMessageText()
        .lines()
        .map(String::trim)
        .filter(String::isNotBlank)
        .joinToString("\n")
        .trim()
}

