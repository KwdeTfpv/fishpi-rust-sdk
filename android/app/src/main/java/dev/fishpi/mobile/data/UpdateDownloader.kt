package dev.fishpi.mobile.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

object UpdateDownloader {
    private const val MaxDownloadBytes = 200L * 1024L * 1024L

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun downloadApk(context: Context, urls: List<String>, expectedSha256: String): File {
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        directory.listFiles()?.forEach { it.delete() }
        val target = File(directory, "fishpi-update.apk")
        var lastError: Throwable? = null
        for (url in urls.map { it.trim() }.filter { it.isNotEmpty() }) {
            try {
                downloadTo(url, target, expectedSha256)
                return target
            } catch (error: Throwable) {
                lastError = error
                target.delete()
            }
        }
        throw IllegalStateException(lastError?.message ?: "没有可用的下载地址", lastError)
    }

    fun installDownloadedApk(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists()) return false
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
        return true
    }

    private fun downloadTo(url: String, target: File, expectedSha256: String) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "FishPi-Mobile-Android")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("下载失败：HTTP ${response.code}")
            val body = response.body ?: error("下载失败：响应为空")
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MaxDownloadBytes) error("安装包过大")
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                    }
                }
            }
            if (expectedSha256.isNotBlank()) {
                val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xFF) }
                if (!actual.equals(expectedSha256.trim(), ignoreCase = true)) {
                    error("安装包校验失败")
                }
            }
        }
    }
}
