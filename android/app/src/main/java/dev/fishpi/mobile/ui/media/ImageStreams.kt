package dev.fishpi.mobile.ui.media

import android.content.Context
import androidx.core.net.toUri
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

internal fun Context.openImageStream(source: String): InputStream {
    val scheme = source.substringBefore(':').lowercase()
    if (scheme == "content" || scheme == "file") {
        return requireNotNull(contentResolver.openInputStream(source.toUri())) { "无法读取图片" }
    }
    val connection = URL(source).openConnection() as? HttpURLConnection
        ?: throw IOException("不支持的图片地址")
    connection.connectTimeout = 10_000
    connection.readTimeout = 20_000
    connection.instanceFollowRedirects = true
    connection.setRequestProperty("Accept", "image/*")
    connection.connect()
    val responseCode = connection.responseCode
    if (responseCode !in 200..299) {
        connection.disconnect()
        throw IOException("图片下载失败：HTTP $responseCode")
    }
    val contentType = connection.contentType.orEmpty()
    if (contentType.isNotBlank() && !contentType.startsWith("image/", ignoreCase = true)) {
        connection.disconnect()
        throw IOException("图片地址返回了非图片内容")
    }
    val stream = try {
        connection.inputStream
    } catch (error: Throwable) {
        connection.disconnect()
        throw error
    }
    return object : FilterInputStream(stream) {
        override fun close() {
            try {
                super.close()
            } finally {
                connection.disconnect()
            }
        }
    }
}

internal fun Context.imageExtensionOf(source: String): String {
    val scheme = source.substringBefore(':').lowercase()
    if (scheme == "content" || scheme == "file") {
        val mime = contentResolver.getType(source.toUri()).orEmpty().lowercase()
        return when (mime) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
    }
    return source.substringBefore('?')
        .substringAfterLast('.', "jpg")
        .lowercase()
        .takeIf { it in setOf("jpg", "jpeg", "png", "webp", "gif") }
        ?: "jpg"
}

internal fun Context.cleanupSharedImageCache(
    maxAgeMillis: Long = 7L * 24L * 60L * 60L * 1000L,
    maxFiles: Int = 50,
) {
    val directory = File(cacheDir, "shared-images")
    val files = directory.listFiles()?.sortedByDescending { it.lastModified() } ?: return
    val now = System.currentTimeMillis()
    files.forEachIndexed { index, file ->
        if (index >= maxFiles || now - file.lastModified() > maxAgeMillis) {
            file.delete()
        }
    }
}
