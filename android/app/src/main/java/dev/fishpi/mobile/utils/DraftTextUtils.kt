package dev.fishpi.mobile.utils

internal fun appendDraftBlock(current: String, addition: String): String {
    val text = addition.trim()
    if (text.isBlank()) return current
    return if (current.isBlank()) text else current.trimEnd() + "\n" + text
}

internal fun removeDraftBlock(current: String, target: String): String =
    current
        .replace(target, "")
        .lines()
        .map(String::trim)
        .filter(String::isNotBlank)
        .joinToString("\n")

internal fun appendMentionDraft(current: String, username: String): String {
    val user = username.trim()
    if (user.isBlank()) return current
    val mention = "@$user "
    return if (current.isBlank()) mention else current.trimEnd() + " " + mention
}
