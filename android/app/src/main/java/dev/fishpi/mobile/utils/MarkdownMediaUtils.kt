package dev.fishpi.mobile.utils

internal data class MarkdownMediaToken(
    val start: Int,
    val end: Int,
    val url: String,
    val type: MarkdownMediaType = MarkdownMediaType.Image,
)

internal enum class MarkdownMediaType {
    Image,
    Video,
    FishPiGeneratedBadge,
}

internal val MarkdownImageRegex = Regex("!\\[[^\\]]*]\\(([^)]*)\\)")
internal val MarkdownVideoRegex = Regex("""\[(?:视频|video)]\(([^)\s]+)(?:\s+["'][^)]*["'])?\)""", RegexOption.IGNORE_CASE)
internal val HtmlImageSrcRegex = Regex("<img\\b[^>]*\\bsrc\\s*=\\s*(['\"])(.*?)\\1[^>]*>", RegexOption.IGNORE_CASE)
internal val HtmlImageTagRegex = Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE)
internal val MarkdownLinkRegex = Regex("""!?\[[^\]]*]\(([^)]*)\)""")
internal val HtmlAnchorHrefRegex = Regex(
    """<a\b[^>]*\bhref\s*=\s*(['"])(.*?)\1[^>]*>.*?</a>""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
internal val PlainUrlRegex = Regex("""https?://[^\s<>()\[\]{}\"']+""", RegexOption.IGNORE_CASE)
internal val HtmlTagRegex = Regex("<[^>]+>")

private val OrphanHtmlWrapperRegex = Regex(
    "^(?:</?p\\b[^>]*>|<br\\s*/?>|&nbsp;|\\s)+$",
    RegexOption.IGNORE_CASE,
)
private val ParagraphStartRegex = Regex("^<p\\b[^>]*>\\s*", RegexOption.IGNORE_CASE)
private val ParagraphEndRegex = Regex("\\s*</p>$", RegexOption.IGNORE_CASE)

internal fun String.decodeBasicHtmlEntities(): String =
    replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")

internal fun String.cleanMarkdownUrl(): String =
    decodeBasicHtmlEntities().trim().trim('"', '\'', ' ', '\t', '\n', '\r')

internal fun String.normalizeWebUrl(): String {
    val trimmed = cleanMarkdownUrl()
    return when {
        trimmed.isBlank() -> ""
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        trimmed.startsWith("//") -> "https:$trimmed"
        else -> trimmed
    }
}

internal fun String.isAbsoluteWebUrl(): Boolean =
    startsWith("http://") || startsWith("https://") || startsWith("//")

internal fun String.trimUrlPunctuation(): String =
    trimEnd('.', ',', ';', ')', ']', '}', '!', '?', '"', '\'')

internal fun String.extractMarkdownAndPlainUrls(): List<String> {
    val markdownLinks = MarkdownLinkRegex.findAll(this)
        .map { it.groupValues.getOrNull(1).orEmpty().substringBefore(' ') }
    val plainLinks = PlainUrlRegex.findAll(this)
        .map { it.value.trimUrlPunctuation() }
    return (markdownLinks + plainLinks)
        .map(String::normalizeWebUrl)
        .filter(String::isNotBlank)
        .distinct()
        .toList()
}

internal fun String.extractImageTokens(): List<MarkdownMediaToken> {
    val markdownTokens = MarkdownImageRegex.findAll(this).mapNotNull { match ->
        match.groupValues.getOrNull(1)
            ?.substringBefore(' ')
            ?.cleanMarkdownUrl()
            ?.takeIf(String::isNotBlank)
            ?.let { url -> MarkdownMediaToken(match.range.first, match.range.last + 1, url, url.markdownImageTokenType()) }
    }
    val htmlTokens = HtmlImageSrcRegex.findAll(this).mapNotNull { match ->
        match.groupValues.getOrNull(2)
            ?.cleanMarkdownUrl()
            ?.takeIf(String::isNotBlank)
            ?.let { url -> MarkdownMediaToken(match.range.first, match.range.last + 1, url, url.markdownImageTokenType()) }
    }
    val videoTokens = MarkdownVideoRegex.findAll(this).mapNotNull { match ->
        match.groupValues.getOrNull(1)
            ?.cleanMarkdownUrl()
            ?.takeIf(String::isNotBlank)
            ?.let { url -> MarkdownMediaToken(match.range.first, match.range.last + 1, url, MarkdownMediaType.Video) }
    }
    val badgeTokens = PlainUrlRegex.findAll(this).mapNotNull { match ->
        val url = match.value.trimUrlPunctuation().normalizeWebUrl()
        url.takeIf { it.toFishPiGeneratedBadgeOrNull() != null }
            ?.let { badgeUrl -> MarkdownMediaToken(match.range.first, match.range.last + 1, badgeUrl, MarkdownMediaType.FishPiGeneratedBadge) }
    }
    val plainImageTokens = PlainUrlRegex.findAll(this).mapNotNull { match ->
        val url = match.value.trimUrlPunctuation().normalizeWebUrl()
        url.takeIf { it.isDirectImageUrl() }
            ?.let { imageUrl -> MarkdownMediaToken(match.range.first, match.range.last + 1, imageUrl, MarkdownMediaType.Image) }
    }
    return (markdownTokens + htmlTokens + videoTokens + badgeTokens + plainImageTokens)
        .sortedBy { it.start }
        .fold(mutableListOf()) { acc, token ->
            if (acc.lastOrNull()?.end?.let { token.start < it } != true) acc += token
            acc
        }
}

private fun String.markdownImageTokenType(): MarkdownMediaType =
    if (toFishPiGeneratedBadgeOrNull() != null) {
        MarkdownMediaType.FishPiGeneratedBadge
    } else {
        MarkdownMediaType.Image
    }

internal fun String.cleanImageSplitTextSegment(): String {
    val trimmed = replace(HtmlImageSrcRegex, "")
        .replace(MarkdownImageRegex, "")
        .replace(MarkdownVideoRegex, "")
        .trim()
    if (trimmed.isBlank() || OrphanHtmlWrapperRegex.matches(trimmed)) {
        return ""
    }
    return trimmed
        .replace(ParagraphStartRegex, "")
        .replace(ParagraphEndRegex, "")
        .trim()
        .takeUnless { it.isBlank() || OrphanHtmlWrapperRegex.matches(it) }
        .orEmpty()
}

internal fun String.isDirectImageUrl(): Boolean {
    val clean = normalizeWebUrl().substringBefore('?').substringBefore('#').lowercase()
    return clean.endsWith(".png") ||
        clean.endsWith(".jpg") ||
        clean.endsWith(".jpeg") ||
        clean.endsWith(".gif") ||
        clean.endsWith(".webp") ||
        clean.endsWith(".bmp") ||
        clean.endsWith(".avif") ||
        clean.endsWith(".heic") ||
        clean.endsWith(".heif") ||
        clean.endsWith(".svg")
}

internal fun String.isDirectVideoUrl(): Boolean {
    val clean = normalizeWebUrl().substringBefore('?').substringBefore('#').lowercase()
    return clean.endsWith(".mp4") ||
        clean.endsWith(".m3u8") ||
        clean.endsWith(".webm") ||
        clean.endsWith(".mov")
}
