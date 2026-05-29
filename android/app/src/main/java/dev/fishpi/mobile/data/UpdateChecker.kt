package dev.fishpi.mobile.data

import dev.fishpi.mobile.BuildConfig
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
    private const val DefaultApkAssetName = "app-release.apk"
    private val SemverRegex = Regex("""^v?(\d+)\.(\d+)\.(\d+)$""")

    fun check(): ReleaseUpdateInfo? {
        return runCatching { checkForUpdate().updateInfo }.getOrNull()
    }

    fun checkForUpdate(): ReleaseUpdateCheckResult {
        val latestTag = requestLatestTagNameByRedirect()
            ?: throw IOException("无法读取最新版本")
        if (compareSemver(latestTag, BuildConfig.VERSION_NAME) <= 0) {
            return ReleaseUpdateCheckResult(updateInfo = null, latestTagName = latestTag)
        }
        val apkUrl =
            "https://github.com/${BuildConfig.UPDATE_REPO_OWNER}/${BuildConfig.UPDATE_REPO_NAME}/releases/download/$latestTag/$DefaultApkAssetName"
        return ReleaseUpdateCheckResult(
            updateInfo = ReleaseUpdateInfo(
                versionCode = -1,
                changelog = "发现新版本 $latestTag，请下载后安装。",
                tagName = latestTag,
                apkUrl = apkUrl,
            ),
            latestTagName = latestTag,
        )
    }

    private fun requestLatestTagNameByRedirect(): String? {
        val endpoint =
            "https://github.com/${BuildConfig.UPDATE_REPO_OWNER}/${BuildConfig.UPDATE_REPO_NAME}/releases/latest"
        val connection = URL(endpoint).openConnection() as? HttpURLConnection
            ?: throw IOException("无法创建网络连接")
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = ConnectTimeoutMs
            connection.readTimeout = ReadTimeoutMs
            connection.setRequestProperty("User-Agent", "fishpi-mobile-update-checker")
            connection.instanceFollowRedirects = false

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
}
