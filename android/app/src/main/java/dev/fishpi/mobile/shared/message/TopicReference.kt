package dev.fishpi.mobile.shared.message

internal fun formatTopicReferenceMarkdown(topic: String): String {
    val text = topic
        .trim()
        .removePrefix("*`")
        .removeSuffix("`*")
        .trim()
        .trim('#')
        .trim()
    return if (text.isBlank()) "" else "*`# $text #`*"
}
