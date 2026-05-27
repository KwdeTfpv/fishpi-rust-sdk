package dev.fishpi.mobile.data

import android.os.Handler
import org.json.JSONObject

internal class NoticeRealtimeClient : NativeRealtimeClient() {

    fun connect(
        apiKey: String,
        onNotice: () -> Unit,
    ) {
        connectInternal {
            val callback = NativeCallback(mainHandler, onNotice)
            FishPiNative.connectNotice(apiKey, callback)
        }
    }

    override fun disconnectNative(handle: Long) {
        FishPiNative.disconnectNotice(handle)
    }

    private class NativeCallback(
        private val mainHandler: Handler,
        private val onNotice: () -> Unit,
    ) {
        @Suppress("unused")
        fun onEvent(payload: String) {
            val event = runCatching { JSONObject(payload) }.getOrNull() ?: return
            if (event.optString("event") == "notice") {
                mainHandler.post { onNotice() }
            }
        }
    }
}
