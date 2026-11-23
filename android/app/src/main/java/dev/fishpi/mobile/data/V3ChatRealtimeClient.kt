package dev.fishpi.mobile.data

import android.os.Handler
import android.os.Looper
import org.json.JSONObject

internal class V3ChatRealtimeClient : NativeRealtimeClient() {

    fun connect(
        accessToken: String,
        onAppearance: (messageId: String, revision: String?, appearance: ChatAppearanceSnapshot?) -> Unit,
        onDeleted: (String) -> Unit,
        onSyncRequired: () -> Unit,
        onAuthExpired: () -> Unit,
        onStatus: (String) -> Unit,
    ) {
        connectInternal(
            connectingStatus = "正在连接 V3 外观订阅...",
            failedStatus = "V3 外观订阅连接失败",
            onStatus = onStatus,
        ) {
            val callback = NativeCallback(
                Handler(Looper.getMainLooper()),
                onAppearance,
                onDeleted,
                onSyncRequired,
                onAuthExpired,
            )
            FishPiNative.connectV3ChatRealtime(accessToken, callback)
        }
    }

    override fun disconnectNative(handle: Long) {
        FishPiNative.disconnectV3ChatRealtime(handle)
    }

    private class NativeCallback(
        private val mainHandler: Handler,
        private val onAppearance: (String, String?, ChatAppearanceSnapshot?) -> Unit,
        private val onDeleted: (String) -> Unit,
        private val onSyncRequired: () -> Unit,
        private val onAuthExpired: () -> Unit,
    ) {
        @Suppress("unused")
        fun onEvent(payload: String) {
            val event = JSONObject(payload)
            when (event.optString("event")) {
                "status" -> mainHandler.post { }
                "appearance" -> {
                    val messageId = event.optString("messageId")
                    if (messageId.isBlank()) return
                    val revision = if (event.has("revision") && !event.isNull("revision")) {
                        event.optString("revision").takeIf { it.isNotBlank() }
                    } else {
                        null
                    }
                    val appearance = event.optJSONObject("appearance")?.toChatAppearanceSnapshot()
                    mainHandler.post { onAppearance(messageId, revision, appearance) }
                }
                "appearanceDeleted" -> {
                    val messageId = event.optString("messageId")
                    if (messageId.isBlank()) return
                    mainHandler.post { onDeleted(messageId) }
                }
                "syncRequired" -> mainHandler.post { onSyncRequired() }
                "authExpired" -> mainHandler.post { onAuthExpired() }
            }
        }
    }
}
