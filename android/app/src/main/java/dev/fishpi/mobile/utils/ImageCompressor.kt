package dev.fishpi.mobile.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

internal const val UPLOAD_MAX_LONG_EDGE = 2048

internal const val UPLOAD_JPEG_QUALITY = 85

internal fun isRecompressableImageMime(mimeType: String?): Boolean {
    val normalized = mimeType?.trim()?.lowercase().orEmpty()
    return when (normalized) {
        "image/jpeg", "image/jpg", "image/png", "image/heic", "image/heif", "image/bmp" -> true
        else -> false
    }
}

internal fun Context.compressImageForUpload(
    uri: Uri,
    directoryName: String,
    prefix: String,
): File? {
    var dest: File? = null
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = computeSampleSize(bounds.outWidth, bounds.outHeight, UPLOAD_MAX_LONG_EDGE)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = contentResolver.openInputStream(uri)
            ?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
            ?: return null

        val oriented = decoded.applyRotation(readExifRotation(uri))
        val scaled = oriented.scaleToLongEdge(UPLOAD_MAX_LONG_EDGE)
        val keepAlpha = scaled.hasAlpha() && scaled.hasTranslucentPixel()

        val target = createCacheFile(directoryName, prefix, if (keepAlpha) "png" else "jpg")
        dest = target
        target.outputStream().use { output ->
            if (keepAlpha) {
                scaled.compress(Bitmap.CompressFormat.PNG, 100, output)
            } else {
                scaled.compress(Bitmap.CompressFormat.JPEG, UPLOAD_JPEG_QUALITY, output)
            }
        }

        if (scaled !== oriented) scaled.recycle()
        if (oriented !== decoded) oriented.recycle()
        decoded.recycle()

        target.takeIf { it.length() > 0 }
    }.getOrElse {
        dest?.delete()
        null
    }
}

private fun computeSampleSize(width: Int, height: Int, maxEdge: Int): Int {
    var sample = 1
    val longEdge = max(width, height)
    while (longEdge / (sample * 2) >= maxEdge) {
        sample *= 2
    }
    return sample
}

private fun Bitmap.scaleToLongEdge(maxEdge: Int): Bitmap {
    val longEdge = max(width, height)
    if (longEdge <= maxEdge) return this
    val ratio = maxEdge.toFloat() / longEdge
    val targetWidth = (width * ratio).roundToInt().coerceAtLeast(1)
    val targetHeight = (height * ratio).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}

private fun Bitmap.applyRotation(degrees: Int): Bitmap {
    if (degrees == 0) return this
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

private fun Bitmap.hasTranslucentPixel(): Boolean {
    val stepX = (width / SAMPLE_GRID).coerceAtLeast(1)
    val stepY = (height / SAMPLE_GRID).coerceAtLeast(1)
    var y = 0
    while (y < height) {
        var x = 0
        while (x < width) {
            if (getPixel(x, y).ushr(24) != 0xFF) return true
            x += stepX
        }
        y += stepY
    }
    return false
}

private const val SAMPLE_GRID = 64

private fun Context.readExifRotation(uri: Uri): Int {
    return runCatching {
        contentResolver.openInputStream(uri)?.use { input ->
            when (
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    }.getOrDefault(0)
}
