package dev.fishpi.mobile.data

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val WEB_LOGIN_CHANNEL_URL = "wss://fishpi.cn:443/login-channel"

internal fun parseWebLoginTarget(raw: String): String? {
    val text = raw.trim()
    val marker = "web:"
    if (!text.startsWith(marker, ignoreCase = true)) return null
    return text.substring(marker.length).trim().takeIf { it.isNotBlank() }
}

internal fun buildWebLoginScanMessage(targetId: String): String =
    JSONObject()
        .put("type", 3)
        .put("targetId", targetId)
        .toString()

internal fun buildWebLoginAuthorizeMessage(targetId: String, apiKey: String): String =
    JSONObject()
        .put("type", 2)
        .put("targetId", targetId)
        .put("apiKey", apiKey)
        .toString()

internal class FishPiWebLoginClient(
    private val client: OkHttpClient = OkHttpClient(),
) {
    suspend fun notifyScanned(targetId: String) {
        sendAndAwaitSuccess(buildWebLoginScanMessage(targetId))
    }

    suspend fun authorize(targetId: String, apiKey: String) {
        sendAndAwaitSuccess(buildWebLoginAuthorizeMessage(targetId, apiKey))
    }

    private suspend fun sendAndAwaitSuccess(message: String): Unit =
        suspendCancellableCoroutine { continuation ->
            val finished = AtomicBoolean(false)
            var socket: WebSocket? = null

            fun finishFailure(error: Throwable) {
                if (finished.compareAndSet(false, true)) {
                    socket?.close(1000, null)
                    continuation.resumeWithException(error)
                }
            }

            fun finishSuccess() {
                if (finished.compareAndSet(false, true)) {
                    socket?.close(1000, null)
                    continuation.resume(Unit)
                }
            }

            val request = Request.Builder()
                .url(WEB_LOGIN_CHANNEL_URL)
                .build()

            socket = client.newWebSocket(
                request,
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        webSocket.send(message)
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        runCatching { JSONObject(text) }
                            .onSuccess { json ->
                                if (json.optInt("code", 1) == 0) {
                                    finishSuccess()
                                } else {
                                    finishFailure(IllegalStateException(json.optString("msg", "授权失败")))
                                }
                            }
                            .onFailure {
                                finishFailure(IllegalStateException("网页登录服务返回异常"))
                            }
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        finishFailure(t)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        if (!finished.get()) finishFailure(IllegalStateException("网页登录连接已关闭"))
                    }
                },
            )

            continuation.invokeOnCancellation {
                if (finished.compareAndSet(false, true)) {
                    socket?.close(1000, null)
                }
            }
        }
}
