package dev.fishpi.mobile.data

import org.json.JSONObject

enum class V3FailureKind {
    Unauthorized,
    Forbidden,
    NotFound,
    Conflict,
    Invalid,
    RateLimited,
    Captcha,
    MfaRequired,
    Server,
    Transport,
    Protocol,
    ;

    companion object {
        fun from(value: String?): V3FailureKind = when (value) {
            "unauthorized" -> Unauthorized
            "forbidden" -> Forbidden
            "notFound" -> NotFound
            "conflict" -> Conflict
            "invalid" -> Invalid
            "rateLimited" -> RateLimited
            "captcha" -> Captcha
            "mfaRequired" -> MfaRequired
            "server" -> Server
            "transport" -> Transport
            else -> Protocol
        }
    }
}

class V3RemoteException(
    val kind: V3FailureKind,
    val status: Int?,
    val code: String?,
    val retryAfterSeconds: Long?,
    override val message: String,
) : IllegalStateException(message) {

    val requiresReauth: Boolean
        get() = kind == V3FailureKind.Unauthorized

    val isRejection: Boolean
        get() = kind == V3FailureKind.Forbidden ||
            kind == V3FailureKind.NotFound ||
            kind == V3FailureKind.Invalid ||
            kind == V3FailureKind.Conflict

    val isUnknownResult: Boolean
        get() = kind == V3FailureKind.Transport ||
            kind == V3FailureKind.Server ||
            kind == V3FailureKind.RateLimited ||
            kind == V3FailureKind.Protocol
}

data class V3SessionSnapshot(
    val ownerUserId: String,
    val accessToken: String,
    val expiresAt: Long,
    val refreshExpiresIn: Long,
    val scope: String,
    val refreshCookiesJson: String,
    val generation: Long,
)

data class V3Identity(
    val userId: String,
    val userName: String,
    val userNickname: String,
)

data class V3SessionCredentials(
    val accessToken: String,
    val expiresIn: Long,
    val refreshExpiresIn: Long,
    val scope: String,
    val refreshCookiesJson: String,
)

data class V3CaptchaOptions(
    val provider: String,
    val captchaId: String,
)

internal fun JSONObject.toV3SessionCredentials(): V3SessionCredentials {
    val accessToken = optString("accessToken")
    if (accessToken.isBlank()) {
        throw V3RemoteException(
            V3FailureKind.Protocol,
            null,
            null,
            null,
            "服务端未返回 V3 访问令牌",
        )
    }
    return V3SessionCredentials(
        accessToken = accessToken,
        expiresIn = optLong("expiresIn", 0L),
        refreshExpiresIn = optLong("refreshExpiresIn", 0L),
        scope = optString("scope"),
        refreshCookiesJson = optJSONArray("refreshCookies")?.toString().orEmpty(),
    )
}

internal fun String.parseV3Envelope(): JSONObject {
    val json = JSONObject(this)
    if (!json.optBoolean("ok", false)) {
        val failure = json.optJSONObject("v3")
        val status = failure?.optInt("status", -1)?.takeIf { it > 0 }
        val retryAfter = failure?.optLong("retryAfter", -1L)?.takeIf { it >= 0 }
        throw V3RemoteException(
            kind = V3FailureKind.from(failure?.optString("kind")),
            status = status,
            code = failure?.optString("code")?.takeIf { it.isNotBlank() },
            retryAfterSeconds = retryAfter,
            message = json.optString("error").ifBlank { "V3 请求失败" },
        )
    }
    return json
}

internal fun JSONObject.v3Data(): JSONObject = optJSONObject("data") ?: JSONObject()
