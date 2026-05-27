package dev.fishpi.mobile.data

internal class PrivateChatRealtimeClient : NativeRealtimeClient() {

    fun connect(
        apiKey: String,
        selfUsername: String,
        peer: String,
        onMessage: (ChatRoomMessage) -> Unit,
        onNotice: (PrivateChatNotice) -> Unit = {},
        onRevoke: (String) -> Unit,
        onStatus: (String) -> Unit,
    ) {
        val mappedStatus: (String) -> Unit = { raw ->
            onStatus(raw.toPrivateChatStatus())
        }
        connectInternal(
            connectingStatus = "正在连接私聊...",
            failedStatus = "私聊连接失败",
            onStatus = mappedStatus,
        ) {
            val callback = NativeCallback(mainHandler, onMessage, onNotice, onRevoke, mappedStatus)
            FishPiNative.connectPrivateChat(apiKey, selfUsername, peer, callback)
        }
    }

    fun connectOverview(
        apiKey: String,
        selfUsername: String,
        onNotice: (PrivateChatNotice) -> Unit,
        onStatus: (String) -> Unit,
    ) {
        connect(
            apiKey = apiKey,
            selfUsername = selfUsername,
            peer = "",
            onMessage = {},
            onNotice = onNotice,
            onRevoke = {},
            onStatus = onStatus,
        )
    }

    fun send(content: String): Boolean {
        val handle = currentHandle()
        if (handle == 0L) {
            return false
        }
        FishPiNative.sendPrivateChatMessageOnConnection(handle, content).unwrapApiResult()
        return true
    }

    fun reconnect(): Boolean {
        val handle = currentHandle()
        if (handle == 0L) {
            return false
        }
        return FishPiNative.reconnectPrivateChat(handle)
    }

    override fun disconnectNative(handle: Long) {
        FishPiNative.disconnectPrivateChat(handle)
    }

    private fun String.toPrivateChatStatus(): String {
        val raw = trim()
        return when {
            raw.equals("WebSocket connected", ignoreCase = true) -> "私聊已连接"
            raw.equals("WebSocket disconnected", ignoreCase = true) -> "私聊已断开"
            raw.equals("Disconnected", ignoreCase = true) -> "私聊已断开"
            raw.contains("connected", ignoreCase = true) -> "私聊已连接"
            raw.contains("disconnect", ignoreCase = true) -> "私聊已断开"
            else -> raw
        }
    }

    private class NativeCallback(
        mainHandler: android.os.Handler,
        private val onMessage: (ChatRoomMessage) -> Unit,
        private val onNotice: (PrivateChatNotice) -> Unit,
        private val onRevoke: (String) -> Unit,
        onStatus: (String) -> Unit,
    ) {
        private val dispatcher = RealtimeEventDispatcher(mainHandler, onStatus)

        @Suppress("unused")
        fun onEvent(payload: String) {
            dispatcher.dispatch(payload) { event, type ->
                when (type) {
                    "message" -> {
                        val item = event.optJSONObject("message") ?: return@dispatch
                        post { onMessage(item.toChatRoomMessage()) }
                    }
                    "notice" -> {
                        val item = event.optJSONObject("notice") ?: return@dispatch
                        post { onNotice(item.toPrivateChatNotice()) }
                    }
                    "revoke" -> {
                        val id = event.optString("id")
                        if (id.isNotBlank()) {
                            post { onRevoke(id) }
                        }
                    }
                }
            }
        }
    }
}
