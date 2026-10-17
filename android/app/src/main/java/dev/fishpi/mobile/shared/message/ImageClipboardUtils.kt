package dev.fishpi.mobile.shared.message

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File
import java.net.URL

internal fun Context.copyImageToClipboard(imageUrl: String): Uri {
    val uri = cacheImageForClipboard(imageUrl)
    val mimeType = contentResolver.getType(uri) ?: "image/*"
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newUri(contentResolver, "图片", uri))
    grantUriPermission("android", uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return uri
}

private fun Context.cacheImageForClipboard(imageUrl: String): Uri {
    val extension = imageExtensionOf(imageUrl, this)
    val dir = File(cacheDir, "shared-images").apply { mkdirs() }
    val file = File(dir, "fishpi-${System.currentTimeMillis()}.$extension")
    openImageStream(imageUrl).use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    }
    return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
}

private fun Context.openImageStream(source: String): java.io.InputStream {
    val scheme = source.substringBefore(':').lowercase()
    return if (scheme == "content" || scheme == "file") {
        requireNotNull(contentResolver.openInputStream(source.toUri())) { "无法读取图片" }
    } else {
        URL(source).openStream()
    }
}

private fun imageExtensionOf(source: String, context: Context): String {
    val scheme = source.substringBefore(':').lowercase()
    if (scheme == "content" || scheme == "file") {
        val mime = context.contentResolver.getType(source.toUri()).orEmpty().lowercase()
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
