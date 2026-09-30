package dev.fishpi.mobile.utils

internal fun attachmentPlaceholder(index: Int): String = "[图$index]"

private val AttachmentPlaceholderRegex = Regex("""\[图(\d+)]""")

internal fun insertAttachmentPlaceholder(
    current: String,
    index: Int,
    cursor: Int,
): Pair<String, Int> {
    val placeholder = attachmentPlaceholder(index)
    val at = cursor.coerceIn(0, current.length)
    val needsLeadingSpace = at > 0 && !current[at - 1].isWhitespace()
    val needsTrailingSpace = at < current.length && !current[at].isWhitespace()
    val insertion = buildString {
        if (needsLeadingSpace) append(' ')
        append(placeholder)
        if (needsTrailingSpace) append(' ')
    }
    val next = current.substring(0, at) + insertion + current.substring(at)
    return next to (at + insertion.length)
}

internal fun removeAttachmentPlaceholder(current: String, index: Int): String {
    val withoutTarget = AttachmentPlaceholderRegex.replace(current) { match ->
        if (match.groupValues[1].toIntOrNull() == index) "" else match.value
    }
    val renumbered = AttachmentPlaceholderRegex.replace(withoutTarget) { match ->
        val number = match.groupValues[1].toIntOrNull() ?: return@replace match.value
        if (number > index) attachmentPlaceholder(number - 1) else match.value
    }
    return renumbered.collapseInsertedGaps()
}

internal fun attachmentPlaceholderIndexes(current: String): List<Int> =
    AttachmentPlaceholderRegex.findAll(current)
        .mapNotNull { it.groupValues[1].toIntOrNull() }
        .distinct()
        .toList()

internal fun resolveAttachmentPlaceholders(
    current: String,
    markdownByIndex: Map<Int, String>,
): String {
    val resolved = AttachmentPlaceholderRegex.replace(current) { match ->
        val number = match.groupValues[1].toIntOrNull() ?: return@replace match.value
        markdownByIndex[number]?.let { markdown -> "\n$markdown\n" }.orEmpty()
    }
    return resolved
        .lines()
        .map(String::trim)
        .filter(String::isNotBlank)
        .joinToString("\n")
}

private fun String.collapseInsertedGaps(): String =
    replace(Regex("""[ \t]{2,}"""), " ")
        .lines()
        .joinToString("\n") { it.trimEnd() }
        .trim()
