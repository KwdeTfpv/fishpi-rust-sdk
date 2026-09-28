package dev.fishpi.mobile.utils

/** Markdown 顶层块类型，用于判断一个块能否被拆分、以及能否从中提取媒体卡片。 */
internal enum class MarkdownBlockKind {
    Paragraph,
    Heading,
    ThematicBreak,
    List,
    BlockQuote,
    FencedCode,
    IndentedCode,
    Table,
    Html,
}

/** 原文中的一个顶层块，start 含、end 不含。 */
internal data class MarkdownBlock(
    val kind: MarkdownBlockKind,
    val start: Int,
    val end: Int,
) {
    /** 结构块整块渲染，内部不做媒体卡片提取，也不允许从中间切开。 */
    val isStructural: Boolean
        get() = when (kind) {
            MarkdownBlockKind.List,
            MarkdownBlockKind.BlockQuote,
            MarkdownBlockKind.FencedCode,
            MarkdownBlockKind.IndentedCode,
            MarkdownBlockKind.Table,
            MarkdownBlockKind.Html,
            -> true
            else -> false
        }
}

private data class MdLine(val text: String, val start: Int, val end: Int)

private val ListMarkerRegex = Regex("""^ {0,3}([-*+]|\d{1,9}[.)])(\s|$)""")
private val FenceRegex = Regex("""^ {0,3}(`{3,}|~{3,})""")
private val HeadingRegex = Regex("""^ {0,3}#{1,6}(\s|$)""")
private val ThematicBreakRegex = Regex("""^ {0,3}([-*_])[ \t]*(\1[ \t]*){2,}$""")
private val TableDelimiterRegex = Regex("""^ {0,3}\|?[ \t]*:?-+:?[ \t]*(\|[ \t]*:?-+:?[ \t]*)*\|?[ \t]*$""")

/** 把原文切成顶层块序列。空行作为块间分隔，不归属任何块。 */
internal fun String.markdownBlocks(): List<MarkdownBlock> {
    if (isEmpty()) return emptyList()
    val lines = splitMarkdownLines()
    val blocks = mutableListOf<MarkdownBlock>()
    var i = 0
    while (i < lines.size) {
        if (lines[i].text.isBlank()) {
            i++
            continue
        }
        val first = lines[i]
        val text = first.text
        val kind: MarkdownBlockKind
        val last: Int
        when {
            FenceRegex.containsMatchIn(text) -> {
                kind = MarkdownBlockKind.FencedCode
                last = consumeFence(lines, i)
            }
            HeadingRegex.containsMatchIn(text) -> {
                kind = MarkdownBlockKind.Heading
                last = i
            }
            ThematicBreakRegex.matches(text) -> {
                kind = MarkdownBlockKind.ThematicBreak
                last = i
            }
            ListMarkerRegex.containsMatchIn(text) -> {
                kind = MarkdownBlockKind.List
                last = consumeList(lines, i)
            }
            text.trimStart().startsWith(">") && indentWidth(text) < 4 -> {
                kind = MarkdownBlockKind.BlockQuote
                last = consumeBlockQuote(lines, i)
            }
            isTableStart(lines, i) -> {
                kind = MarkdownBlockKind.Table
                last = consumeTable(lines, i)
            }
            indentWidth(text) >= 4 -> {
                kind = MarkdownBlockKind.IndentedCode
                last = consumeIndentedCode(lines, i)
            }
            text.trimStart().startsWith("<") && indentWidth(text) < 4 -> {
                kind = MarkdownBlockKind.Html
                last = consumeUntilBlank(lines, i)
            }
            else -> {
                kind = MarkdownBlockKind.Paragraph
                last = consumeParagraph(lines, i)
            }
        }
        blocks += MarkdownBlock(kind, first.start, lines[last].end)
        i = last + 1
    }
    return blocks
}

/** 结构块的字符区间，用作媒体提取的保护区。 */
internal fun String.structuralMarkdownRanges(): List<IntRange> =
    markdownBlocks().filter { it.isStructural }.map { it.start until it.end }

private fun String.splitMarkdownLines(): List<MdLine> {
    val lines = mutableListOf<MdLine>()
    var lineStart = 0
    var i = 0
    while (i < length) {
        if (this[i] == '\n') {
            // 行文本去掉尾部 \r 供正则匹配，偏移量仍指向原文（含 \r），块边界保持正确。
            lines += MdLine(substring(lineStart, i).removeSuffix("\r"), lineStart, i)
            lineStart = i + 1
        }
        i++
    }
    lines += MdLine(substring(lineStart, length).removeSuffix("\r"), lineStart, length)
    return lines
}

private fun indentWidth(line: String): Int {
    var count = 0
    for (c in line) {
        when (c) {
            ' ' -> count += 1
            '\t' -> count += 4
            else -> return count
        }
    }
    return count
}

private fun consumeFence(lines: List<MdLine>, start: Int): Int {
    val open = FenceRegex.find(lines[start].text)!!.groupValues[1]
    val markerChar = open[0]
    val markerLen = open.length
    var j = start + 1
    while (j < lines.size) {
        val close = FenceRegex.find(lines[j].text)
        if (close != null) {
            val marker = close.groupValues[1]
            val rest = lines[j].text.trimStart().drop(marker.length)
            if (marker[0] == markerChar && marker.length >= markerLen && rest.isBlank()) {
                return j
            }
        }
        j++
    }
    return lines.size - 1
}

private fun consumeParagraph(lines: List<MdLine>, start: Int): Int {
    var j = start
    while (j + 1 < lines.size) {
        val next = lines[j + 1].text
        if (next.isBlank() || startsNewBlock(lines, j + 1)) break
        j++
    }
    return j
}

private fun consumeList(lines: List<MdLine>, start: Int): Int {
    var j = start
    while (j + 1 < lines.size) {
        val next = lines[j + 1].text
        if (next.isBlank()) {
            var k = j + 2
            while (k < lines.size && lines[k].text.isBlank()) k++
            if (k >= lines.size) break
            val kt = lines[k].text
            val continues = indentWidth(kt) >= 1 || ListMarkerRegex.containsMatchIn(kt)
            if (!continues) break
            j++
        } else {
            val continues = indentWidth(next) >= 1 || ListMarkerRegex.containsMatchIn(next)
            if (!continues && startsStrongBlock(lines, j + 1)) break
            j++
        }
    }
    return j
}

private fun consumeBlockQuote(lines: List<MdLine>, start: Int): Int {
    var j = start
    while (j + 1 < lines.size) {
        val next = lines[j + 1].text
        if (next.isBlank()) {
            var k = j + 2
            while (k < lines.size && lines[k].text.isBlank()) k++
            if (k < lines.size && lines[k].text.trimStart().startsWith(">")) j++ else break
        } else if (next.trimStart().startsWith(">")) {
            j++
        } else {
            if (startsStrongBlock(lines, j + 1)) break
            j++
        }
    }
    return j
}

private fun consumeIndentedCode(lines: List<MdLine>, start: Int): Int {
    var j = start
    while (j + 1 < lines.size) {
        val next = lines[j + 1].text
        if (next.isBlank()) {
            var k = j + 2
            while (k < lines.size && lines[k].text.isBlank()) k++
            if (k < lines.size && indentWidth(lines[k].text) >= 4) j++ else break
        } else if (indentWidth(next) >= 4) {
            j++
        } else {
            break
        }
    }
    return j
}

private fun consumeTable(lines: List<MdLine>, start: Int): Int {
    var j = start + 1
    while (j + 1 < lines.size) {
        val next = lines[j + 1].text
        if (next.isBlank() || !next.contains('|')) break
        j++
    }
    return j
}

private fun consumeUntilBlank(lines: List<MdLine>, start: Int): Int {
    var j = start
    while (j + 1 < lines.size && !lines[j + 1].text.isBlank()) j++
    return j
}

private fun isTableStart(lines: List<MdLine>, idx: Int): Boolean {
    val text = lines[idx].text
    if (indentWidth(text) >= 4 || !text.contains('|')) return false
    val next = lines.getOrNull(idx + 1)?.text ?: return false
    return next.contains('-') && TableDelimiterRegex.matches(next)
}

private fun startsNewBlock(lines: List<MdLine>, idx: Int): Boolean {
    val text = lines[idx].text
    return FenceRegex.containsMatchIn(text) ||
        HeadingRegex.containsMatchIn(text) ||
        ThematicBreakRegex.matches(text) ||
        ListMarkerRegex.containsMatchIn(text) ||
        (text.trimStart().startsWith(">") && indentWidth(text) < 4) ||
        isTableStart(lines, idx)
}

/** 缩进为 0 的强分隔块，用于在懒续行场景下终止列表/引用，避免把后续段落无限吞并。 */
private fun startsStrongBlock(lines: List<MdLine>, idx: Int): Boolean {
    val text = lines[idx].text
    if (indentWidth(text) >= 1) return false
    return FenceRegex.containsMatchIn(text) ||
        HeadingRegex.containsMatchIn(text) ||
        ThematicBreakRegex.matches(text) ||
        text.trimStart().startsWith(">") ||
        isTableStart(lines, idx)
}
