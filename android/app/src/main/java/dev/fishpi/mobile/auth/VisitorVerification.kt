package dev.fishpi.mobile.auth

import android.net.Uri
import dev.fishpi.mobile.data.SavedAccount
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicReference

internal const val VisitorVerifyUrl = "https://fishpi.cn/test"
private const val VisitorVerifyLoginUrl = "https://fishpi.cn/loginWebInApiKey"

internal sealed interface VisitorRetryAction {
    data class SavedApiKey(val apiKey: String) : VisitorRetryAction
    data class SavedAccountLogin(val account: SavedAccount) : VisitorRetryAction
}

internal fun String?.isVisitorVerificationRequired(): Boolean =
    this?.contains("访客验证") == true

internal object VisitorVerificationEvents {
    private val listener = AtomicReference<(() -> Unit)?>(null)

    fun setListener(next: (() -> Unit)?) {
        listener.set(next)
    }

    fun notifyRequired() {
        listener.get()?.invoke()
    }
}

internal fun visitorVerificationStartUrl(apiKey: String?): String {
    val key = apiKey?.trim().orEmpty()
    if (key.isBlank()) {
        return VisitorVerifyUrl
    }

    return Uri.parse(VisitorVerifyLoginUrl)
        .buildUpon()
        .appendQueryParameter("apiKey", key)
        .appendQueryParameter("r", "/test")
        .build()
        .toString()
}

internal fun isVisitorVerificationSuccessResponse(url: String, body: String): Boolean {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    if (uri.scheme !in setOf("http", "https")) return false
    if (uri.host != "fishpi.cn") return false
    if (uri.path.orEmpty() != "/validateCaptcha") return false
    return runCatching { JSONObject(body).optInt("code", -1) == 0 }.getOrDefault(false)
}

internal fun isVisitorVerificationChallengeUrl(url: String): Boolean =
    fishPiPath(url) == "/test"

internal fun isVisitorVerificationBypassedUrl(url: String): Boolean {
    val path = fishPiPath(url) ?: return false
    return path != "/test" &&
        path != "/validateCaptcha" &&
        path != "/loginWebInApiKey"
}

private fun fishPiPath(url: String): String? {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
    if (uri.scheme !in setOf("http", "https")) return null
    if (uri.host != "fishpi.cn") return null
    return uri.path.orEmpty().ifBlank { "/" }
}
