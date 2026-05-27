package dev.fishpi.mobile.utils

import coil3.Image

internal fun adaptiveImageBoxSize(
    imageWidth: Int,
    imageHeight: Int,
    maxWidth: Int,
    maxHeight: Int,
    minHeight: Int,
    fallbackWidth: Int,
    fallbackHeight: Int,
): Pair<Int, Int> {
    if (imageWidth <= 0 || imageHeight <= 0) {
        return fallbackWidth to fallbackHeight
    }
    val ratio = imageWidth.toFloat() / imageHeight.toFloat()
    var width = maxWidth
    var height = (width / ratio).toInt().coerceAtLeast(1)
    if (height > maxHeight) {
        height = maxHeight
        width = (height * ratio).toInt().coerceAtLeast(1).coerceAtMost(maxWidth)
    }
    if (height < minHeight) {
        height = minHeight
        width = (height * ratio).toInt().coerceAtLeast(1).coerceAtMost(maxWidth)
    }
    return width to height
}

internal fun Image.adaptiveComposeImageRatio(fallback: Float): Float {
    if (width <= 0 || height <= 0) return fallback
    return (width.toFloat() / height.toFloat()).coerceIn(0.18f, 8f)
}
