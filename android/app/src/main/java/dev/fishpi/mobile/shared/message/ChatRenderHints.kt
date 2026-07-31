package dev.fishpi.mobile.shared.message

import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.utils.HtmlAnchorHrefRegex
import dev.fishpi.mobile.utils.HtmlImageSrcRegex
import dev.fishpi.mobile.utils.MarkdownImageRegex
import dev.fishpi.mobile.utils.MarkdownVideoRegex
import dev.fishpi.mobile.utils.MarkdownMediaToken
import dev.fishpi.mobile.utils.MarkdownMediaType
import dev.fishpi.mobile.utils.cleanImageSplitTextSegment
import dev.fishpi.mobile.utils.extractImageTokens
import dev.fishpi.mobile.utils.toPlainMessageText
import dev.fishpi.mobile.utils.extractMarkdownAndPlainUrls
import dev.fishpi.mobile.utils.isDirectImageUrl
import dev.fishpi.mobile.utils.isDirectVideoUrl
import dev.fishpi.mobile.utils.normalizeWebUrl
import dev.fishpi.mobile.utils.toChatTimeLabelOrNull
import dev.fishpi.mobile.utils.trimUrlPunctuation

internal data class ChatMessageRenderHints(
    val previewLinks: List<String> = emptyList(),
    val videoLinks: List<String> = emptyList(),
    val clientLabel: String = "",
    val timeLabel: String = "",
    val markdownContent: String = "",
    val plainFallback: String = "",
    val avatarModel: Any? = null,
    val imageTokens: List<MarkdownMediaToken> = emptyList(),
    val isMediaOnly: Boolean = false,
)

internal fun ChatRoomMessage.toRenderHints(avatarModel: Any? = null): ChatMessageRenderHints {
    val markdownContent = renderSource.trim()
    val inlineVideoLinks = markdownContent.markdownVideoUrls()
    val videoLinks = previewVideoUrls().filterNot(inlineVideoLinks::contains)
    val plainFallback = markdownContent.toPlainMessageText()
    val imageTokens = markdownContent.extractImageTokens()
    return ChatMessageRenderHints(
        previewLinks = previewLinkUrls(markdownContent).filterNot(videoLinks::contains),
        videoLinks = videoLinks,
        clientLabel = client.cleanClientType(),
        timeLabel = time.toMessageTimeLabelOrNull().orEmpty(),
        markdownContent = markdownContent,
        plainFallback = plainFallback,
        avatarModel = avatarModel,
        imageTokens = imageTokens,
        isMediaOnly = computeIsMediaOnly(
            revoked = revoked,
            hasRedPacket = redPacket != null,
            markdownContent = markdownContent,
            plainFallback = plainFallback,
            hasVideoLinks = videoLinks.isNotEmpty(),
            imageTokens = imageTokens,
        ),
    )
}

private fun computeIsMediaOnly(
    revoked: Boolean,
    hasRedPacket: Boolean,
    markdownContent: String,
    plainFallback: String,
    hasVideoLinks: Boolean,
    imageTokens: List<MarkdownMediaToken>,
): Boolean {
    if (revoked || hasRedPacket) return false
    val markdown = markdownContent.trim()
    val text = plainFallback.trim()
    val hasRenderableMedia = imageTokens.any {
        it.type == MarkdownMediaType.Image || it.type == MarkdownMediaType.Video
    } || hasVideoLinks
    if (!hasRenderableMedia) return false
    val stripped = buildString {
        var cursor = 0
        imageTokens.forEach { token ->
            if (token.start > cursor) {
                append(markdown.substring(cursor, token.start.coerceAtMost(markdown.length)))
            }
            cursor = token.end.coerceAtMost(markdown.length)
        }
        if (cursor < markdown.length) {
            append(markdown.substring(cursor))
        }
    }.cleanImageSplitTextSegment().trim()
    val hasBodyText = if (markdown.isNotBlank()) {
        stripped.isNotBlank()
    } else {
        text.isNotBlank()
    }
    return !hasBodyText
}

private val HtmlBlockQuoteRegex = Regex("<blockquote\\b[^>]*>(.*?)</blockquote>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

internal fun previewableContentLinkUrls(
    content: String,
    linkUrls: List<String>,
    imageUrls: List<String>,
    renderableImageUrls: List<String>,
): List<String> {
    val quotedLinks = content.quotedLinkUrls()
    val sourceLinks = content.extractLinkUrls()
    val renderableImages = renderableImageUrls
        .map(String::normalizeWebUrl)
        .toSet()
    val knownImages = imageUrls
        .map(String::normalizeWebUrl)
        .toSet()
    return (linkUrls + sourceLinks)
        .map(String::normalizeWebUrl)
        .filter(String::isNotBlank)
        .distinct()
        .filterNot { url -> knownImages.contains(url) || renderableImages.contains(url) }
        .filterNot { it.isDirectImageUrl() }
        .filterNot { url -> quotedLinks.contains(url) }
        .filterNot(::isMentionProfileUrl)
        .filterNot(::isFishPiInternalJumpUrl)
}

private fun ChatRoomMessage.previewLinkUrls(markdownContent: String): List<String> {
    return previewableContentLinkUrls(
        content = markdownContent,
        linkUrls = linkUrls,
        imageUrls = imageUrls,
        renderableImageUrls = allRenderableImageUrls(),
    )
}

private fun String.quotedLinkUrls(): Set<String> {
    val markdownQuoteText = lineSequence()
        .filter { it.trimStart().startsWith(">") }
        .joinToString("\n") { it.trimStart().removePrefix(">").trimStart() }
    val htmlQuoteText = HtmlBlockQuoteRegex.findAll(this)
        .joinToString("\n") { it.groupValues.getOrNull(1).orEmpty() }
    return (markdownQuoteText + "\n" + htmlQuoteText)
        .extractMarkdownAndPlainUrls()
        .toSet()
}

private fun String.extractLinkUrls(): List<String> {
    val htmlLinks = HtmlAnchorHrefRegex.findAll(this)
        .map { it.groupValues.getOrNull(2).orEmpty() }
    return (htmlLinks + extractMarkdownAndPlainUrls().asSequence())
        .map(String::normalizeWebUrl)
        .filter(String::isNotBlank)
        .distinct()
        .toList()
}

private fun ChatRoomMessage.previewVideoUrls(): List<String> {
    val fromLinks = linkUrls.filter { it.isDirectVideoUrl() }
    val fromMarkdownVideoLinks = renderSource.markdownVideoUrls()
    val fromContent = VideoUrlRegex.findAll(renderSource)
        .map { it.value.trimUrlPunctuation() }
        .map(String::normalizeWebUrl)
        .filter { it.isDirectVideoUrl() }
    return (fromLinks + fromMarkdownVideoLinks + fromContent)
        .distinct()
        .take(2)
        .toList()
}

private fun String.markdownVideoUrls(): List<String> {
    return MarkdownVideoRegex.findAll(this)
        .map { it.groupValues.getOrNull(1).orEmpty() }
        .map(String::normalizeWebUrl)
        .filter(String::isNotBlank)
        .distinct()
        .toList()
}

private val VideoUrlRegex = Regex("""https?://[^\s<>"']+\.(?:mp4|m3u8|webm|mov)(?:\?[^\s<>"']*)?""", RegexOption.IGNORE_CASE)

private fun isMentionProfileUrl(url: String): Boolean {
    return runCatching {
        val uri = java.net.URI(url)
        val host = uri.host.orEmpty().lowercase()
        val path = uri.path.orEmpty().trimEnd('/').lowercase()
        val isFishPiHost = host == "fishpi.cn" || host.endsWith(".fishpi.cn")
        isFishPiHost && (path == "/member" || path.startsWith("/member/"))
    }.getOrDefault(false)
}

private fun isFishPiInternalJumpUrl(url: String): Boolean {
    return runCatching {
        val uri = java.net.URI(url)
        val host = uri.host.orEmpty().lowercase()
        val path = uri.path.orEmpty().trimEnd('/').lowercase()
        val fragment = uri.fragment.orEmpty().lowercase()
        val isFishPiHost = host == "fishpi.cn" || host.endsWith(".fishpi.cn")
        isFishPiHost && (
            (path == "/cr" && fragment.startsWith("chatroom")) ||
                (path == "/chat" && fragment.isNotBlank())
            )
    }.getOrDefault(false)
}

private fun String.cleanClientType(): String {
    return trim()
        .removePrefix("client:")
        .removePrefix("Client:")
        .replace('_', ' ')
}

internal fun ChatRoomMessage.allRenderableImageUrls(): List<String> {
    val markdownOrText = renderSource
    val fromArray = imageUrls.asSequence()
    val fromMarkdown = MarkdownImageRegex.findAll(markdownOrText)
        .map { it.groupValues.getOrNull(1).orEmpty() }
    val fromHtml = if (md.isBlank()) {
        HtmlImageSrcRegex.findAll(content).map { it.groupValues.getOrNull(2).orEmpty() }
    } else {
        emptySequence()
    }
    return (fromArray + fromMarkdown + fromHtml)
        .map { it.normalizeWebUrl() }
        .filter { it.isNotBlank() }
        .distinct()
        .toList()
}

private fun String.toMessageTimeLabelOrNull(): String? {
    return toChatTimeLabelOrNull(includeSeconds = true)
}

