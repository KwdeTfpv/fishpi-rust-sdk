package dev.fishpi.mobile.auth

import android.net.Uri
import dev.fishpi.mobile.data.SavedAccount

internal const val VisitorVerifyUrl = "https://fishpi.cn/test"

internal sealed interface VisitorRetryAction {
    data class SavedApiKey(val apiKey: String) : VisitorRetryAction
    data class SavedAccountLogin(val account: SavedAccount) : VisitorRetryAction
}

internal fun String?.isVisitorVerificationRequired(): Boolean =
    this?.contains("访客验证") == true

internal fun isVisitorVerificationCompletedUrl(url: String): Boolean {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    if (uri.scheme !in setOf("http", "https")) return false
    if (uri.host != "fishpi.cn") return false
    val path = uri.path.orEmpty().ifBlank { "/" }
    return path != "/test" && path != "/validateCaptcha"
}

