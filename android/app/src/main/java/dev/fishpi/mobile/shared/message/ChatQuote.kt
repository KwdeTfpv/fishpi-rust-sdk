package dev.fishpi.mobile.shared.message

import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.utils.HtmlImageSrcRegex
import dev.fishpi.mobile.utils.HtmlImageTagRegex
import dev.fishpi.mobile.utils.HtmlTagRegex
import dev.fishpi.mobile.utils.MarkdownImageRegex
import dev.fishpi.mobile.utils.cleanMarkdownUrl
import dev.fishpi.mobile.utils.decodeBasicHtmlEntities

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

internal fun ChatRoomMessage.canQuote(): Boolean {
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
    return buildList {
        addAll(imageUrls)
        listOf(md, content).forEach { source ->
            MarkdownImageRegex.findAll(source).forEach { match ->
                add(match.groupValues.getOrNull(1).orEmpty())
            }
            HtmlImageSrcRegex.findAll(source).forEach { match ->
                add(match.groupValues.getOrNull(2).orEmpty())
            }
        }
    }
        .map(String::cleanMarkdownUrl)
        .filter(String::isNotBlank)
        .distinct()
}

private fun String.removeQuoteImages(): String {
    return replace(MarkdownImageRegex, "")
        .replace(HtmlImageTagRegex, "")
        .trim()
}

private fun String.toQuotePlainText(): String {
    return replace(SimpleParagraphBoundaryRegex, "\n")
        .replace(ParagraphTagRegex, "")
        .replace(BreakTagRegex, "\n")
        .replace(HtmlTagRegex, "")
        .decodeBasicHtmlEntities()
        .lines()
        .map(String::trim)
        .filter(String::isNotBlank)
        .joinToString("\n")
        .trim()
}

private val SimpleParagraphBoundaryRegex = Regex("</p\\s*>\\s*<p\\b[^>]*>", RegexOption.IGNORE_CASE)
private val ParagraphTagRegex = Regex("</?p\\b[^>]*>", RegexOption.IGNORE_CASE)
private val BreakTagRegex = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)

