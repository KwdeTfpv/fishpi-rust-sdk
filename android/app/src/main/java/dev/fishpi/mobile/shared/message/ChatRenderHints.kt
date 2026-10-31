package dev.fishpi.mobile.shared.message

import dev.fishpi.mobile.data.ChatMediaKind
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.utils.MarkdownMediaToken
import dev.fishpi.mobile.utils.MarkdownMediaType
import dev.fishpi.mobile.utils.cleanImageSplitTextSegment
import dev.fishpi.mobile.utils.extractImageTokens
import dev.fishpi.mobile.utils.toPlainMessageText
import dev.fishpi.mobile.utils.isDirectImageUrl
import dev.fishpi.mobile.utils.normalizeWebUrl
import dev.fishpi.mobile.utils.toChatTimeLabelOrNull

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

internal fun ChatRoomMessage.toRenderHints(
    revoked: Boolean = false,
    avatarModel: Any? = null,
): ChatMessageRenderHints {
    val markdownContent = renderSource.trim().withMediaImages(media)
    val imageTokens = markdownContent.extractImageTokens()
    val inlineVideoLinks = imageTokens
        .filter { it.type == MarkdownMediaType.Video }
        .map { it.url.normalizeWebUrl() }
    val videoLinks = previewVideoUrls().filterNot(inlineVideoLinks::contains)
    val plainFallback = markdownContent.toPlainMessageText()
    return ChatMessageRenderHints(
        previewLinks = previewLinkUrls().filterNot(videoLinks::contains),
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

private fun String.withMediaImages(media: List<dev.fishpi.mobile.data.ChatMedia>): String {
    val existing = extractImageTokens()
        .filter { it.type == MarkdownMediaType.Image }
        .map { it.url.normalizeWebUrl() }
        .toSet()
    val missing = media
        .asSequence()
        .filter { it.kind == ChatMediaKind.Image }
        .map { it.url.normalizeWebUrl() }
        .filter { it.isNotBlank() && it !in existing }
        .distinct()
        .toList()
    if (missing.isEmpty()) return this
    return buildString {
        append(trim())
        if (isNotBlank()) append("\n\n")
        missing.forEach { url -> append("![图片](").append(url).append(")\n") }
    }.trim()
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

private fun ChatRoomMessage.previewLinkUrls(): List<String> {
    val images = mediaUrls(ChatMediaKind.Image)
        .map(String::normalizeWebUrl)
        .toSet()
    return mediaUrls(ChatMediaKind.Link)
        .map(String::normalizeWebUrl)
        .filter(String::isNotBlank)
        .distinct()
        .filterNot(images::contains)
        .filterNot { it.isDirectImageUrl() }
        .filterNot(::isMentionProfileUrl)
        .filterNot(::isFishPiInternalJumpUrl)
}

private fun ChatRoomMessage.previewVideoUrls(): List<String> {
    return media
        .filter { it.kind == ChatMediaKind.Video }
        .map { it.url.normalizeWebUrl() }
        .filter(String::isNotBlank)
        .distinct()
        .take(2)
        .toList()
}

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
    return media
        .filter { it.kind == ChatMediaKind.Image }
        .map { it.url.normalizeWebUrl() }
        .filter { it.isNotBlank() }
        .distinct()
        .toList()
}

private fun String.toMessageTimeLabelOrNull(): String? {
    return toChatTimeLabelOrNull(includeSeconds = true)
}

