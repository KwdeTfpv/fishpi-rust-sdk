package dev.fishpi.mobile.data

import android.text.Html
import dev.fishpi.mobile.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class ReleaseUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val changelog: String,
    val tagName: String,
    val apkUrl: String,
    val urls: List<String> = listOf(apkUrl),
    val sha256: String = "",
    val sizeBytes: Long = 0L,
    val channel: String = UpdateChannel.Stable.key,
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
        return runCatching { checkForUpdate(UpdateChannel.Stable).updateInfo }.getOrNull()
    }

    fun checkForUpdate(channel: UpdateChannel = UpdateChannel.Stable): ReleaseUpdateCheckResult {
        val manifest = runCatching { fetchManifest(channel) }.getOrNull()
        if (manifest != null) {
            return if (compareSemver(manifest.versionName, BuildConfig.VERSION_NAME) <= 0) {
                ReleaseUpdateCheckResult(updateInfo = null, latestTagName = manifest.versionName)
            } else {
                ReleaseUpdateCheckResult(updateInfo = manifest, latestTagName = manifest.versionName)
            }
        }
        return checkForUpdateFromAtom(channel)
    }

    private fun fetchManifest(channel: UpdateChannel): ReleaseUpdateInfo? {
        val rcTag = if (channel == UpdateChannel.Rc) {
            requestAtomReleases().firstOrNull { it.tagName.contains("-rc.") }?.tagName ?: return null
        } else {
            null
        }
        val url = if (rcTag == null) {
            releasesUrl("latest/download/update.json")
        } else {
            releasesUrl("download/$rcTag/update.json")
        }
        val payload = runCatching { JSONObject(requestText(url, followRedirects = true)) }.getOrNull()
            ?: return null
        return manifestToInfo(payload, channel)
    }

    private fun manifestToInfo(payload: JSONObject, channel: UpdateChannel): ReleaseUpdateInfo? {
        val versionName = payload.optString("versionName").trim()
        val versionCode = payload.optInt("versionCode", -1)
        if (versionName.isBlank() || versionCode < 0) return null
        val tagName = "v$versionName"
        val urls = payload.optJSONArray("urls").toStringList()
        val apkUrl = urls.firstOrNull() ?: buildApkUrl(tagName)
        return ReleaseUpdateInfo(
            versionCode = versionCode,
            versionName = versionName,
            changelog = payload.optString("changelog").trim().ifBlank {
                "发现新版本 $tagName，请下载后安装。"
            },
            tagName = tagName,
            apkUrl = apkUrl,
            urls = if (urls.isEmpty()) listOf(apkUrl) else urls,
            sha256 = payload.optString("sha256").trim(),
            sizeBytes = payload.optLong("sizeBytes", 0L),
            channel = payload.optString("channel").trim().ifBlank { channel.key },
        )
    }

    private fun checkForUpdateFromAtom(channel: UpdateChannel): ReleaseUpdateCheckResult {
        val releases = runCatching { requestAtomReleases() }.getOrDefault(emptyList())
        val release = when (channel) {
            UpdateChannel.Rc -> releases.firstOrNull { it.tagName.contains("-rc.") }
            UpdateChannel.Stable -> releases.firstOrNull { !it.tagName.contains("-rc.") }
        }
        if (release != null) {
            if (compareSemver(release.tagName, BuildConfig.VERSION_NAME) <= 0) {
                return ReleaseUpdateCheckResult(updateInfo = null, latestTagName = release.tagName)
            }
            return ReleaseUpdateCheckResult(
                updateInfo = atomReleaseToInfo(release),
                latestTagName = release.tagName,
            )
        }
        if (channel == UpdateChannel.Rc) {
            return ReleaseUpdateCheckResult(updateInfo = null, latestTagName = "")
        }

        val latestTag = requestLatestTagNameByRedirect()
            ?: throw IOException("无法读取最新版本")
        if (compareSemver(latestTag, BuildConfig.VERSION_NAME) <= 0) {
            return ReleaseUpdateCheckResult(updateInfo = null, latestTagName = latestTag)
        }
        val apkUrl = buildApkUrl(latestTag)
        return ReleaseUpdateCheckResult(
            updateInfo = ReleaseUpdateInfo(
                versionCode = -1,
                versionName = latestTag.removePrefix("v"),
                changelog = "发现新版本 $latestTag，请下载后安装。",
                tagName = latestTag,
                apkUrl = apkUrl,
                urls = listOf(apkUrl),
            ),
            latestTagName = latestTag,
        )
    }

    private fun atomReleaseToInfo(release: AtomRelease): ReleaseUpdateInfo {
        val versionName = release.tagName.removePrefix("v")
        val apkUrl = buildApkUrl(release.tagName)
        return ReleaseUpdateInfo(
            versionCode = release.versionCode,
            versionName = versionName,
            changelog = release.changelog.ifBlank {
                "发现新版本 ${release.tagName}，请下载后安装。"
            },
            tagName = release.tagName,
            apkUrl = apkUrl,
            urls = listOf(apkUrl),
            channel = if (release.tagName.contains("-rc.")) UpdateChannel.Rc.key else UpdateChannel.Stable.key,
        )
    }

    private fun requestAtomReleases(): List<AtomRelease> {
        val endpoint = releasesUrl("atom")
        val xml = requestText(endpoint, followRedirects = true)
        val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
            setInput(xml.reader())
        }
        val releases = mutableListOf<AtomRelease>()
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
                        releases += AtomRelease(
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
        return releases
    }

    private fun requestLatestTagNameByRedirect(): String? {
        val endpoint = releasesUrl("latest")
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

    private fun releasesUrl(path: String): String =
        "https://github.com/${BuildConfig.UPDATE_REPO_OWNER}/${BuildConfig.UPDATE_REPO_NAME}/releases/$path"

    private fun buildApkUrl(tagName: String): String =
        releasesUrl("download/$tagName/$DefaultApkAssetName")

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
