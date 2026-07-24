package dev.fishpi.mobile.feature.home

import java.util.Locale

internal fun formatLivenessValue(value: Double): String {
    val normalized = value.coerceAtLeast(0.0)
    return if (normalized % 1.0 == 0.0) {
        normalized.toLong().toString()
    } else {
        "%.2f".format(Locale.US, normalized).trimEnd('0').trimEnd('.')
    }
}
