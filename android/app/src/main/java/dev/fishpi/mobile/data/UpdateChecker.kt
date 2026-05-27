package dev.fishpi.mobile.data

import dev.fishpi.mobile.BuildConfig
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

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
    private val VersionCodeRegex = Regex("""(?m)^versionCode:\s*(\d+)\s*$""")
    private val SemverRegex = Regex("""^v?(\d+)\.(\d+)\.(\d+)$""")

    fun check(): ReleaseUpdateInfo? {
        return runCatching { checkForUpdate().updateInfo }.getOrNull()
    }

    fun checkForUpdate(): ReleaseUpdateCheckResult {
        val endpoint =
            "https://api.github.com/repos/${BuildConfig.UPDATE_REPO_OWNER}/${BuildConfig.UPDATE_REPO_NAME}/releases/latest"
        val response = requestText(endpoint)
        val json = JSONObject(response)
        val body = json.optString("body")
        val remoteVersionCode = VersionCodeRegex.find(body)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
        val remoteTag = json.optString("tag_name")
        val hasNewerCode = remoteVersionCode?.let { it > BuildConfig.VERSION_CODE } ?: false
        val hasNewerSemver = compareSemver(remoteTag, BuildConfig.VERSION_NAME) > 0
        if (!hasNewerCode && !hasNewerSemver) {
            return ReleaseUpdateCheckResult(updateInfo = null, latestTagName = remoteTag)
        }

        val assets = json.optJSONArray("assets") ?: throw IOException("发现新版本，但发布页没有附件")
        var apkUrl = ""
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            val name = asset.optString("name")
            if (name.endsWith(".apk", ignoreCase = true)) {
                apkUrl = asset.optString("browser_download_url")
                break
            }
        }
        if (apkUrl.isBlank()) {
            throw IOException("发现新版本，但没有可下载的 APK")
        }

        return ReleaseUpdateCheckResult(
            updateInfo = ReleaseUpdateInfo(
                versionCode = remoteVersionCode ?: -1,
                changelog = body.replaceFirst(VersionCodeRegex, "").trim(),
                tagName = remoteTag,
                apkUrl = apkUrl,
            ),
            latestTagName = remoteTag,
        )
    }

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

    private fun requestText(url: String): String {
        val connection = URL(url).openConnection() as? HttpURLConnection
            ?: throw IOException("无法创建网络连接")
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = ConnectTimeoutMs
            connection.readTimeout = ReadTimeoutMs
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            connection.setRequestProperty("User-Agent", "fishpi-mobile-update-checker")
            connection.instanceFollowRedirects = true

            if (connection.responseCode !in 200..299) {
                throw IOException("HTTP ${connection.responseCode}")
            }
            BufferedInputStream(connection.inputStream).bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
