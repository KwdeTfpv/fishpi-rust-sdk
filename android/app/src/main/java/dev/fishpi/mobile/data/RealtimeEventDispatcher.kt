package dev.fishpi.mobile.data

import android.os.Handler
import org.json.JSONObject

internal class RealtimeEventDispatcher(
    private val mainHandler: Handler,
    private val onStatus: (String) -> Unit,
) {
    fun dispatch(payload: String, block: RealtimeEventDispatcher.(JSONObject, String) -> Unit) {
        val event = runCatching { JSONObject(payload) }.getOrNull() ?: return
        val type = event.optString("event")
        if (type == "status" || type == "error") {
            postStatus(event.optString("message"))
            return
        }
        block(event, type)
    }

    fun post(block: () -> Unit) {
        mainHandler.post(block)
    }

    fun postStatus(message: String) {
        if (message.isNotBlank()) {
            post { onStatus(message) }
        }
    }
}

internal fun JSONObject.optRealtimeMessageId(): String =
    optString("oId").ifBlank {
        optString("id").ifBlank { optString("messageId") }
    }
