package dev.fishpi.mobile.utils

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

internal fun String.toEpochMillisOrNull(): Long? {
    val text = trim()
    if (text.isBlank()) return null
    text.toLongOrNull()?.let { raw ->
        return if (raw > 10_000_000_000L) raw else raw * 1000
    }
    return runCatching { Instant.parse(text).toEpochMilli() }.getOrNull()
        ?: parseLocalDateTime(text, "yyyy-MM-dd HH:mm:ss")
        ?: parseLocalDateTime(text, "yyyy-MM-dd HH:mm")
        ?: parseLocalDateTime(text.replace('T', ' '), "yyyy-MM-dd HH:mm:ss")
}

internal fun Long.toChatTimeLabel(includeSeconds: Boolean = false): String {
    val dateTime = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()
    val today = LocalDateTime.now().toLocalDate()
    val pattern = when {
        dateTime.toLocalDate() == today && includeSeconds -> "HH:mm:ss"
        dateTime.toLocalDate() == today -> "HH:mm"
        includeSeconds -> "MM-dd HH:mm:ss"
        else -> "MM-dd HH:mm"
    }
    return dateTime.format(DateTimeFormatter.ofPattern(pattern))
}

internal fun String.toChatTimeLabelOrNull(includeSeconds: Boolean = false): String? {
    val epochMillis = toEpochMillisOrNull() ?: return trim().takeIf(String::isNotBlank)
    return epochMillis.toChatTimeLabel(includeSeconds = includeSeconds)
}

internal fun Long.toSystemLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

private fun parseLocalDateTime(text: String, pattern: String): Long? {
    return try {
        LocalDateTime
            .parse(text, DateTimeFormatter.ofPattern(pattern))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    } catch (_: DateTimeParseException) {
        null
    }
}
