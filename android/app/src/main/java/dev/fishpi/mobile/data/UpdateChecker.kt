package dev.fishpi.mobile.data

import android.text.Html
import dev.fishpi.mobile.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class ReleaseUpdateInfo(
    val versionCode: Int,
    val changelog: String,
    val tagName: String,
    val apkUrl: String,
)

data class ReleaseUpdateCheckResult(
    val updateInfo: ReleaseUpdateInfo?,
    val latestTagName: String,
)

object UpdateChecker {
    private const val ConnectTimeoutMs = 6000
    private const val ReadTimeoutMs = 8000
    private const val DefaultApkAssetName = "app-release.apk"
    private val SemverRegex = Regex("""^v?(\d+)\.(\d+)\.(\d+)$""")
    private val VersionCodeRegex = Regex("""(?m)^versionCode:\s*(\d+)""")

    fun check(): ReleaseUpdateInfo? {
        return runCatching { checkForUpdate().updateInfo }.getOrNull()
    }

    fun checkForUpdate(): ReleaseUpdateCheckResult {
        val atomRelease = runCatching { requestLatestReleaseByAtom() }.getOrNull()
        if (atomRelease != null) {
            if (compareSemver(atomRelease.tagName, BuildConfig.VERSION_NAME) <= 0) {
                return ReleaseUpdateCheckResult(updateInfo = null, latestTagName = atomRelease.tagName)
            }
            return ReleaseUpdateCheckResult(
                updateInfo = ReleaseUpdateInfo(
                    versionCode = atomRelease.versionCode,
                    changelog = atomRelease.changelog.ifBlank {
                        "发现新版本 ${atomRelease.tagName}，请下载后安装。"
                    },
                    tagName = atomRelease.tagName,
                    apkUrl = buildApkUrl(atomRelease.tagName),
                ),
                latestTagName = atomRelease.tagName,
            )
        }

        val latestTag = requestLatestTagNameByRedirect()
            ?: throw IOException("无法读取最新版本")
        if (compareSemver(latestTag, BuildConfig.VERSION_NAME) <= 0) {
            return ReleaseUpdateCheckResult(updateInfo = null, latestTagName = latestTag)
        }
        return ReleaseUpdateCheckResult(
            updateInfo = ReleaseUpdateInfo(
                versionCode = -1,
                changelog = "发现新版本 $latestTag，请下载后安装。",
                tagName = latestTag,
                apkUrl = buildApkUrl(latestTag),
            ),
            latestTagName = latestTag,
        )
    }

    private fun requestLatestReleaseByAtom(): AtomRelease? {
        val endpoint =
            "https://github.com/${BuildConfig.UPDATE_REPO_OWNER}/${BuildConfig.UPDATE_REPO_NAME}/releases.atom"
        val xml = requestText(endpoint, followRedirects = true)
        val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
            setInput(xml.reader())
        }
        var inEntry = false
        var tagName = ""
        var content = ""
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "entry" -> {
                            inEntry = true
                            tagName = ""
                            content = ""
                        }
                        "link" -> if (inEntry && tagName.isBlank()) {
                            tagName = parser.releaseTagFromHref().orEmpty()
                        }
                        "id" -> if (inEntry && tagName.isBlank()) {
                            tagName = parser.nextText().releaseTagFromUrl().orEmpty()
                        }
                        "content", "summary" -> if (inEntry && content.isBlank()) {
                            content = parser.nextText()
                        }
                    }
                }
                XmlPullParser.END_TAG -> if (parser.name == "entry" && inEntry) {
                    val normalizedTag = tagName.trim()
                    if (normalizedTag.isNotBlank()) {
                        val plainContent = content.toPlainReleaseText()
                        return AtomRelease(
                            tagName = normalizedTag,
                            changelog = plainContent.toReleaseChangelog(),
                            versionCode = VersionCodeRegex.find(plainContent)
                                ?.groupValues
                                ?.getOrNull(1)
                                ?.toIntOrNull()
                                ?: -1,
                        )
                    }
                    inEntry = false
                }
            }
            event = parser.next()
        }
        return null
    }

    private fun requestLatestTagNameByRedirect(): String? {
        val endpoint =
            "https://github.com/${BuildConfig.UPDATE_REPO_OWNER}/${BuildConfig.UPDATE_REPO_NAME}/releases/latest"
        val connection = openConnection(endpoint, followRedirects = false)
        return try {
            val code = connection.responseCode
            if (code !in 300..399) return null
            val location = connection.getHeaderField("Location") ?: return null
            location.substringAfterLast("/releases/tag/", missingDelimiterValue = "")
                .takeIf { it.isNotBlank() }
                ?.substringBefore("?")
        } finally {
            connection.disconnect()
        }
    }

    private fun requestText(endpoint: String, followRedirects: Boolean): String {
        val connection = openConnection(endpoint, followRedirects)
        return try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("请求失败：HTTP $code")
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(endpoint: String, followRedirects: Boolean): HttpURLConnection =
        (URL(endpoint).openConnection() as? HttpURLConnection)
            ?.apply {
                requestMethod = "GET"
                connectTimeout = ConnectTimeoutMs
                readTimeout = ReadTimeoutMs
                setRequestProperty("User-Agent", "fishpi-mobile-update-checker")
                instanceFollowRedirects = followRedirects
            }
            ?: throw IOException("无法创建网络连接")

    private fun buildApkUrl(tagName: String): String =
        "https://github.com/${BuildConfig.UPDATE_REPO_OWNER}/${BuildConfig.UPDATE_REPO_NAME}/releases/download/$tagName/$DefaultApkAssetName"

    private fun compareSemver(left: String, right: String): Int {
        val l = parseSemver(left) ?: return 0
        val r = parseSemver(right) ?: return 0
        return when {
            l.first != r.first -> l.first - r.first
            l.second != r.second -> l.second - r.second
            else -> l.third - r.third
        }
    }

    private fun parseSemver(raw: String): Triple<Int, Int, Int>? {
        val match = SemverRegex.matchEntire(raw.trim()) ?: return null
        val major = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return null
        val minor = match.groupValues.getOrNull(2)?.toIntOrNull() ?: return null
        val patch = match.groupValues.getOrNull(3)?.toIntOrNull() ?: return null
        return Triple(major, minor, patch)
    }

    private data class AtomRelease(
        val tagName: String,
        val changelog: String,
        val versionCode: Int,
    )

    private fun XmlPullParser.releaseTagFromHref(): String? {
        for (index in 0 until attributeCount) {
            val value = getAttributeValue(index)
            val tag = value.releaseTagFromUrl()
            if (!tag.isNullOrBlank()) return tag
        }
        return null
    }

    private fun String.releaseTagFromUrl(): String? =
        substringAfter("/releases/tag/", missingDelimiterValue = "")
            .takeIf { it.isNotBlank() }
            ?.substringBefore("?")
            ?.substringBefore("#")

    private fun String.toPlainReleaseText(): String =
        Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()
            .replace('\u00A0', ' ')
            .lines()
            .map { it.trimEnd() }
            .dropWhile { it.isBlank() }
            .joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()

    private fun String.toReleaseChangelog(): String {
        val lines = lines().dropWhile { it.isBlank() }.toMutableList()
        if (lines.firstOrNull()?.startsWith("versionCode:", ignoreCase = true) == true) {
            lines.removeAt(0)
            while (lines.firstOrNull()?.isBlank() == true) {
                lines.removeAt(0)
            }
            if (lines.firstOrNull()?.trim() == "---") {
                lines.removeAt(0)
            }
        }
        return lines
            .joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}
