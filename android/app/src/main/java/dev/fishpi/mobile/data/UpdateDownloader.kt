package dev.fishpi.mobile.data

import android.content.Context
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

    fun downloadApk(
        context: Context,
        urls: List<String>,
        expectedSha256: String,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ): File {
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        directory.listFiles()?.forEach { it.delete() }
        val target = File(directory, "fishpi-update.apk")
        var lastError: Throwable? = null
        for (url in urls.map { it.trim() }.filter { it.isNotEmpty() }) {
            try {
                downloadTo(url, target, expectedSha256, onProgress)
                return target
            } catch (error: Throwable) {
                lastError = error
                target.delete()
            }
        }
        throw IllegalStateException(lastError?.message ?: "没有可用的下载地址", lastError)
    }

    private fun downloadTo(
        url: String,
        target: File,
        expectedSha256: String,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "FishPi-Mobile-Android")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("下载失败：HTTP ${response.code}")
            val body = response.body ?: error("下载失败：响应为空")
            val totalBytes = body.contentLength().takeIf { it > 0 } ?: -1L
            val digest = MessageDigest.getInstance("SHA-256")
            var downloaded = 0L
            var lastReported = 0L
            onProgress(0L, totalBytes)
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        downloaded += read
                        if (downloaded > MaxDownloadBytes) error("安装包过大")
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                        if (downloaded - lastReported >= 512L * 1024L) {
                            onProgress(downloaded, totalBytes)
                            lastReported = downloaded
                        }
                    }
                }
            }
            onProgress(downloaded, totalBytes)
            if (expectedSha256.isNotBlank()) {
                val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xFF) }
                if (!actual.equals(expectedSha256.trim(), ignoreCase = true)) {
                    error("安装包校验失败")
                }
            }
        }
    }
}
