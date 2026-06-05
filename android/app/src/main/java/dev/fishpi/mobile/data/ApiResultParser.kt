package dev.fishpi.mobile.data

import dev.fishpi.mobile.auth.VisitorVerificationEvents
import dev.fishpi.mobile.auth.isVisitorVerificationRequired
import org.json.JSONObject
import java.util.Locale

internal fun String.unwrapApiResult(): JSONObject {
    val json = JSONObject(this)
    if (!json.optBoolean("ok", false)) {
        val rawError = json.optString("error", "Rust core call failed")
        val error = rawError.toUserFriendlyError()
        if (error.isVisitorVerificationRequired()) {
            VisitorVerificationEvents.notifyRequired()
        }
        throw IllegalStateException(error)
    }
    return json
}

private fun String.toUserFriendlyError(): String {
    val normalized = lowercase(Locale.ROOT)
    if (normalized.contains("response decoding")) {
        return "服务响应解析失败，请稍后重试"
    }
    if (normalized.contains("request error")) {
        return "网络请求失败，请检查网络后重试"
    }
    return this
}
