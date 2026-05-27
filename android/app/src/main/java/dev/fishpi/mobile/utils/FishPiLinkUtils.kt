package dev.fishpi.mobile.utils

import java.net.URI
import java.net.URLDecoder

private val FishPiUsernameRegex = Regex("""^[A-Za-z0-9_-]+$""")
internal val FishPiMentionRegex = Regex("""(?<![A-Za-z0-9_@.-])@([A-Za-z0-9_-]+)""")

internal fun String.isValidFishPiUsername(): Boolean =
    FishPiUsernameRegex.matches(trim())

internal fun String.toFishPiMemberNameOrNull(allowRelative: Boolean = false): String? {
    return runCatching {
        val source = trim()
        val uri = URI(
            if (allowRelative && source.startsWith("/")) {
                "https://fishpi.cn$source"
            } else {
                source
            },
        )
        val scheme = uri.scheme.orEmpty().lowercase()
        if (scheme != "http" && scheme != "https") return@runCatching null
        if (!uri.host.orEmpty().isFishPiHost()) return@runCatching null
        val segments = uri.path.orEmpty()
            .trim('/')
            .takeIf(String::isNotBlank)
            ?.split('/')
            ?.filter(String::isNotBlank)
            .orEmpty()
        if (segments.size != 2 || segments.firstOrNull()?.lowercase() != "member") return@runCatching null
        segments.getOrNull(1)
            ?.let { URLDecoder.decode(it, "UTF-8") }
            ?.takeIf(String::isValidFishPiUsername)
    }.getOrNull()
}

internal fun String.isFishPiHost(): Boolean {
    val host = trim().lowercase()
    return host == "fishpi.cn" || host.endsWith(".fishpi.cn")
}
