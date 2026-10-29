package dev.fishpi.mobile.data

import android.os.Handler

internal class NoticeRealtimeClient : NativeRealtimeClient() {

    fun connect(
        apiKey: String,
        onNotice: () -> Unit,
        onStatus: (String) -> Unit = {},
    ) {
        connectInternal(
            connectingStatus = "正在连接通知...",
            failedStatus = "通知连接失败",
            onStatus = onStatus,
        ) {
            val callback = NativeCallback(mainHandler, onNotice, onStatus)
            FishPiNative.connectNotice(apiKey, callback)
        }
    }

    override fun disconnectNative(handle: Long) {
        FishPiNative.disconnectNotice(handle)
    }

    private class NativeCallback(
        mainHandler: Handler,
        private val onNotice: () -> Unit,
        onStatus: (String) -> Unit,
    ) {
        private val dispatcher = RealtimeEventDispatcher(mainHandler, onStatus)

        @Suppress("unused")
        fun onEvent(payload: String) {
            dispatcher.dispatch(payload) { _, type ->
                if (type == "notice") {
                    post { onNotice() }
                }
            }
        }
    }
}
