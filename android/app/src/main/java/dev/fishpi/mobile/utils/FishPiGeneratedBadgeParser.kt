package dev.fishpi.mobile.utils

import dev.fishpi.mobile.data.MedalView

internal data class FishPiGeneratedBadge(
    val text: String,
    val imageUrl: String,
    val backColor: Int,
    val fontColor: Int,
)

internal fun String.toFishPiGeneratedBadgeOrNull(): FishPiGeneratedBadge? {
    return runCatching {
        val uri = java.net.URI(normalizeWebUrl())
        val host = uri.host.orEmpty().lowercase()
        val path = uri.path.orEmpty().trimEnd('/').lowercase()
        val scheme = uri.scheme.orEmpty().lowercase()
        val isGenUrl = (scheme == "http" || scheme == "https") &&
            (host == "fishpi.cn" || host.endsWith(".fishpi.cn")) &&
            path == "/gen"
        if (!isGenUrl) return@runCatching null
        val params = uri.rawQuery.orEmpty()
            .split('&')
            .mapNotNull { part ->
                val key = part.substringBefore('=', "").takeIf(String::isNotBlank) ?: return@mapNotNull null
                val value = part.substringAfter('=', "")
                decodeBadgeUrlParam(key) to decodeBadgeUrlParam(value)
            }
            .toMap()
        val text = params["txt"].orEmpty().trim().ifBlank { params["name"].orEmpty().trim() }
        val imageUrl = params["url"].orEmpty().trim()
        if (text.isBlank() || imageUrl.isBlank()) return@runCatching null
        FishPiGeneratedBadge(
            text = text,
            imageUrl = imageUrl,
            backColor = parseBadgeColor(params["backcolor"], 0xFFF8FAFC.toInt()),
            fontColor = parseBadgeColor(params["fontcolor"], 0xFF9CA3AF.toInt()),
        )
    }.getOrNull()
}

internal fun MedalView.toFishPiGeneratedBadgeOrNull(): FishPiGeneratedBadge? {
    val text = name.ifBlank { text }.trim()
    if (text.isBlank() || imageUrl.isBlank()) return null
    return FishPiGeneratedBadge(
        text = text,
        imageUrl = imageUrl,
        backColor = parseBadgeColor(backColor, 0xFFF8FAFC.toInt()),
        fontColor = parseBadgeColor(fontColor, 0xFF9CA3AF.toInt()),
    )
}

private fun decodeBadgeUrlParam(value: String): String =
    java.net.URLDecoder.decode(value, Charsets.UTF_8.name())

private fun parseBadgeColor(raw: String?, fallback: Int): Int {
    val hex = raw.orEmpty()
        .substringBefore(',')
        .trim()
        .trimStart('#')
    if (hex.length !in setOf(6, 8)) return fallback
    return runCatching {
        val value = hex.toLong(16)
        if (hex.length == 6) {
            (0xFF000000 or value).toInt()
        } else {
            value.toInt()
        }
    }.getOrDefault(fallback)
}
