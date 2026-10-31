package dev.fishpi.mobile.shared.message

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dev.fishpi.mobile.ui.media.cleanupSharedImageCache
import dev.fishpi.mobile.ui.media.imageExtensionOf
import dev.fishpi.mobile.ui.media.openImageStream
import java.io.File

internal fun Context.copyImageToClipboard(imageUrl: String): Uri {
    val uri = cacheImageForClipboard(imageUrl)
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newUri(contentResolver, "图片", uri))
    grantUriPermission("android", uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return uri
}

private fun Context.cacheImageForClipboard(imageUrl: String): Uri {
    cleanupSharedImageCache()
    val extension = imageExtensionOf(imageUrl)
    val dir = File(cacheDir, "shared-images").apply { mkdirs() }
    val file = File(dir, "fishpi-${System.currentTimeMillis()}.$extension")
    openImageStream(imageUrl).use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    }
    return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
}