package dev.fishpi.mobile.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileInputStream
import java.util.Locale

internal const val CHAT_UPLOAD_MAX_BYTES: Long = 20L * 1024L * 1024L

internal fun Long.toReadableMediaSize(): String {
    val mb = this / 1024.0 / 1024.0
    return String.format(Locale.US, "%.1fMB", mb)
}

internal fun Context.uriContentSize(uri: Uri): Long? {
    return runCatching {
        contentResolver
            .query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0 && cursor.moveToFirst()) cursor.getLong(index).takeIf { it >= 0 } else null
            }
    }.getOrNull()
}

internal fun Context.uriContentExtension(uri: Uri, defaultExtension: String): String {
    return runCatching {
        contentResolver.getType(uri)
            ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            ?.trim()
            ?.trimStart('.')
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()
        ?: uriDisplayNameExtension(uri)
        ?: defaultExtension
}

internal fun Context.uriDisplayName(uri: Uri): String? {
    return runCatching {
        contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
    }.getOrNull()
        ?: uri.lastPathSegment
}

private fun Context.uriDisplayNameExtension(uri: Uri): String? {
    return uriDisplayName(uri)
        ?.substringAfterLast('.', missingDelimiterValue = "")
        ?.trim()
        ?.takeIf { it.isNotBlank() && it.length <= 8 }
}

private fun Context.copyUriToFile(uri: Uri, file: File) {
    val sourceName = uriDisplayName(uri).orEmpty()
    runCatching {
        copyUriToFileWithInputStream(uri, file)
    }.recoverCatching {
        copyUriToFileWithDescriptor(uri, file)
    }.getOrElse {
        file.delete()
        val suffix = sourceName.takeIf { it.isNotBlank() }?.let { "：$it" }.orEmpty()
        val reason = it.message?.takeIf { message -> message.isNotBlank() }?.let { message -> "（$message）" }.orEmpty()
        error("无法读取选择的媒体$suffix$reason")
    }
}

private fun Context.copyUriToFileWithInputStream(uri: Uri, file: File) {
    contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "无法读取选择的媒体" }
        file.outputStream().use { output -> input.copyTo(output) }
    }
}

private fun Context.copyUriToFileWithDescriptor(uri: Uri, file: File) {
    contentResolver.openFileDescriptor(uri, "r").use { descriptor ->
        requireNotNull(descriptor) { "无法读取选择的媒体" }
        FileInputStream(descriptor.fileDescriptor).use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
    }
}

internal fun File.chatUploadSizeError(): String? {
    val size = length()
    if (size <= CHAT_UPLOAD_MAX_BYTES) return null
    return "文件过大，当前 ${size.toReadableMediaSize()}，最大 ${CHAT_UPLOAD_MAX_BYTES.toReadableMediaSize()}"
}

internal fun Context.copyUriToCacheFile(
    uri: Uri,
    directoryName: String,
    prefix: String,
    defaultExtension: String = "jpg",
): File {
    val extension = uriContentExtension(uri, defaultExtension)
    return createCacheFile(directoryName, prefix, extension).also { file ->
        copyUriToFile(uri, file)
    }
}

internal fun Context.createCacheFile(
    directoryName: String,
    prefix: String,
    extension: String = "jpg",
): File {
    val dir = File(cacheDir, directoryName).apply { mkdirs() }
    val safeExt = extension.trim().trimStart('.').ifBlank { "jpg" }
    return File.createTempFile("fishpi-$prefix-", ".$safeExt", dir)
}

internal fun Context.copyUriToSingleFile(
    uri: Uri,
    directoryName: String,
    fileStem: String,
    defaultExtension: String = "jpg",
): File {
    val extension = uriContentExtension(uri, defaultExtension)
    val dir = File(filesDir, directoryName).apply { mkdirs() }
    dir.listFiles()?.forEach { file ->
        if (file.isFile) file.delete()
    }
    return File(dir, "$fileStem.$extension").also { file ->
        copyUriToFile(uri, file)
    }
}
