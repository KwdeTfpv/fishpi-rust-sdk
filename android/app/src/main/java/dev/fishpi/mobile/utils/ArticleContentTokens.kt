package dev.fishpi.mobile.utils

import dev.fishpi.mobile.shared.message.previewableContentLinkUrls

internal sealed interface ArticleContentToken {
    data class Text(val markdown: String) : ArticleContentToken
    data class Image(val url: String) : ArticleContentToken
    data class Video(val url: String, val label: String) : ArticleContentToken
    data class LinkPreview(val url: String) : ArticleContentToken
}

private data class ArticleRangeToken(
    val start: Int,
    val end: Int,
    val token: ArticleContentToken,
    val consumesSource: Boolean,
)

private val HtmlImageEmojiClassRegex =
    Regex("""\bclass\s*=\s*(['"])[^'"]*\bemoji\b[^'"]*\1""", RegexOption.IGNORE_CASE)

private val HtmlTableTagRegex = Regex("<(?:table|tr|td|tbody)\\b", RegexOption.IGNORE_CASE)

private fun List<MarkdownBlock>.structuralRanges(): List<IntRange> =
    filter { it.isStructural }.map { it.start until it.end }

private fun String.htmlSplittableRanges(blocks: List<MarkdownBlock>): List<IntRange> =
    blocks
        .filter { it.kind == MarkdownBlockKind.Html }
        .filterNot { block -> HtmlTableTagRegex.containsMatchIn(substring(block.start, block.end)) }
        .map { it.start until it.end }

internal fun String.articleContentTokens(
    linkUrls: List<String>,
    imageUrls: List<String>,
): List<ArticleContentToken> {
    if (isBlank()) return emptyList()
    val blocks = markdownBlocks()
    val protectedRanges = blocks.structuralRanges()
    val splittableHtmlRanges = htmlSplittableRanges(blocks)
    val imageTokens = articleImageRangeTokens(protectedRanges, splittableHtmlRanges)
    val videoTokens = articleVideoRangeTokens(protectedRanges)
    val renderedImages = imageTokens.mapNotNull { (it.token as? ArticleContentToken.Image)?.url }
    val previewUrls = previewableContentLinkUrls(
        content = this,
        linkUrls = linkUrls,
        imageUrls = imageUrls,
        renderableImageUrls = renderedImages,
    ).filterNot { it.isDirectVideoUrl() }
    val previewTokens = articlePreviewRangeTokens(previewUrls, protectedRanges)
    val rangeTokens = (imageTokens + videoTokens + previewTokens)
        .sortedWith(compareBy<ArticleRangeToken> { it.start }.thenBy { if (it.consumesSource) 0 else 1 })
    val output = mutableListOf<ArticleContentToken>()
    var cursor = 0
    rangeTokens.forEach { rangeToken ->
        if (rangeToken.start < cursor) return@forEach
        val before = substring(cursor, rangeToken.start).cleanImageSplitTextSegment()
        if (before.isNotBlank()) output += ArticleContentToken.Text(before)
        output += rangeToken.token
        cursor = if (rangeToken.consumesSource) rangeToken.end else rangeToken.start
    }
    val tail = substring(cursor).cleanImageSplitTextSegment()
    if (tail.isNotBlank()) output += ArticleContentToken.Text(tail)
    return output
}

internal fun String.unrenderedHtmlImageUrls(): List<String> {
    if (isBlank()) return emptyList()
    val blocks = markdownBlocks()
    val protectedRanges = blocks.structuralRanges()
    val splittableHtmlRanges = htmlSplittableRanges(blocks)
    return extractImageTokens()
        .filter { token -> substring(token.start, token.end).startsWith("<img", ignoreCase = true) }
        .filterNot { token -> HtmlImageEmojiClassRegex.containsMatchIn(substring(token.start, token.end)) }
        .filter { token -> token.start in protectedRanges && token.start !in splittableHtmlRanges }
        .map { token -> token.url.normalizeWebUrl() }
        .filter(String::isNotBlank)
        .distinct()
        .toList()
}

private fun String.articleImageRangeTokens(
    protectedRanges: List<IntRange>,
    splittableHtmlRanges: List<IntRange>,
): List<ArticleRangeToken> {
    return extractImageTokens()
        .mapNotNull { token ->
            val tag = substring(token.start, token.end)
            if (tag.startsWith("<img", ignoreCase = true)) {
                if (HtmlImageEmojiClassRegex.containsMatchIn(tag)) return@mapNotNull null
                if (token.start in protectedRanges && token.start !in splittableHtmlRanges) return@mapNotNull null
            } else if (token.start in protectedRanges) {
                return@mapNotNull null
            }
            if (token.type == MarkdownMediaType.Video) {
                return@mapNotNull ArticleRangeToken(
                    start = token.start,
                    end = token.end,
                    token = ArticleContentToken.Video(token.url.normalizeWebUrl(), "视频"),
                    consumesSource = true,
                )
            }
            ArticleRangeToken(
                start = token.start,
                end = token.end,
                token = ArticleContentToken.Image(token.url.normalizeWebUrl()),
                consumesSource = true,
            )
        }
        .sortedBy { it.start }
        .distinctBy { it.start to it.end }
        .toList()
}

private fun String.articleVideoRangeTokens(protectedRanges: List<IntRange>): List<ArticleRangeToken> {
    return MarkdownLinkRegex.findAll(this)
        .mapNotNull { match ->
            if (match.range.first in protectedRanges) return@mapNotNull null
            val label = match.value.substringAfter('[').substringBefore(']').trim()
            val url = match.groupValues.getOrNull(1).orEmpty().substringBefore(' ').normalizeWebUrl()
            if (!url.isDirectVideoUrl()) return@mapNotNull null
            ArticleRangeToken(
                start = match.range.first,
                end = match.range.last + 1,
                token = ArticleContentToken.Video(url, label.ifBlank { "视频" }),
                consumesSource = true,
            )
        }
        .sortedBy { it.start }
        .distinctBy { it.start to it.end }
        .toList()
}

private fun String.articlePreviewRangeTokens(
    previewUrls: List<String>,
    protectedRanges: List<IntRange>,
): List<ArticleRangeToken> {
    if (previewUrls.isEmpty()) return emptyList()
    val remaining = previewUrls.toMutableSet()
    val tokens = mutableListOf<ArticleRangeToken>()
    fun add(url: String, index: Int) {
        if (index < 0 || index in protectedRanges || !remaining.remove(url)) return
        tokens += ArticleRangeToken(index, index, ArticleContentToken.LinkPreview(url), false)
    }
    val htmlAnchorRanges = mutableListOf<IntRange>()
    HtmlAnchorHrefRegex.findAll(this).forEach { match ->
        val url = match.groupValues.getOrNull(2).orEmpty().normalizeWebUrl()
        htmlAnchorRanges += match.range
        if (url in remaining) add(url, match.range.last + 1)
    }
    MarkdownLinkRegex.findAll(this).forEach { match ->
        val url = match.groupValues.getOrNull(1).orEmpty().substringBefore(' ').normalizeWebUrl()
        if (url in remaining) add(url, match.range.last + 1)
    }
    PlainUrlRegex.findAll(this).forEach { match ->
        if (htmlAnchorRanges.any { match.range.first in it }) return@forEach
        val url = match.value.trimUrlPunctuation().normalizeWebUrl()
        if (url in remaining) add(url, match.range.last + 1)
    }
    return tokens.sortedBy { it.start }
}

private operator fun List<IntRange>.contains(index: Int): Boolean = any { index in it }
