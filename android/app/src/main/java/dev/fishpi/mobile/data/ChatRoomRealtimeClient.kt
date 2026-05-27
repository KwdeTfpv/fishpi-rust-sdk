package dev.fishpi.mobile.data

internal class ChatRoomRealtimeClient : NativeRealtimeClient() {

    fun connect(
        apiKey: String,
        selfUsername: String,
        onMessage: (ChatRoomMessage) -> Unit,
        onRevoke: (String) -> Unit,
        onReaction: (ChatReactionUpdate) -> Unit,
        onRedPacketStatus: (RedPacketStatusUpdate) -> Unit,
        onOnline: (Int, String, List<ChatOnlineUser>) -> Unit,
        onDiscussChanged: (String) -> Unit,
        onNodeChanged: (String) -> Unit,
        onStatus: (String) -> Unit,
    ) {
        connectInternal(
            connectingStatus = "正在连接聊天室...",
            failedStatus = "聊天室连接失败",
            onStatus = onStatus,
        ) {
            val callback = NativeCallback(
                mainHandler = mainHandler,
                onMessage = onMessage,
                onRevoke = onRevoke,
                onReaction = onReaction,
                onRedPacketStatus = onRedPacketStatus,
                onOnline = onOnline,
                onDiscussChanged = onDiscussChanged,
                onNodeChanged = onNodeChanged,
                onStatus = onStatus,
            )
            FishPiNative.connectChatRoom(apiKey, selfUsername, callback)
        }
    }

    override fun disconnectNative(handle: Long) {
        FishPiNative.disconnectChatRoom(handle)
    }

    fun pauseEvents() {
        val handle = currentHandle()
        if (handle != 0L) {
            Thread {
                FishPiNative.pauseChatRoomEvents(handle)
            }.start()
        }
    }

    fun resumeEvents() {
        val handle = currentHandle()
        if (handle != 0L) {
            Thread {
                FishPiNative.resumeChatRoomEvents(handle)
            }.start()
        }
    }

    fun reconnect(onStatus: (String) -> Unit) {
        val handle = currentHandle()
        if (handle == 0L) {
            onStatus("聊天室未连接")
            return
        }
        onStatus("聊天室正在重连...")
        Thread {
            val ok = FishPiNative.reconnectChatRoom(handle)
            mainHandler.post {
                onStatus(if (ok) "聊天室连接已恢复" else "聊天室重连失败")
            }
        }.start()
    }

    fun hasActiveHandle(): Boolean = currentHandle() != 0L

    private class NativeCallback(
        mainHandler: android.os.Handler,
        private val onMessage: (ChatRoomMessage) -> Unit,
        private val onRevoke: (String) -> Unit,
        private val onReaction: (ChatReactionUpdate) -> Unit,
        private val onRedPacketStatus: (RedPacketStatusUpdate) -> Unit,
        private val onOnline: (Int, String, List<ChatOnlineUser>) -> Unit,
        private val onDiscussChanged: (String) -> Unit,
        private val onNodeChanged: (String) -> Unit,
        private val onStatus: (String) -> Unit,
    ) {
        private val dispatcher = RealtimeEventDispatcher(mainHandler, onStatus)

        @Suppress("unused")
        fun onEvent(payload: String) {
            dispatcher.dispatch(payload) { event, type ->
                when (type) {
                    "message" -> {
                        val item = event.optJSONObject("message") ?: return@dispatch
                        val message = item.toChatRoomMessage()
                        post { onMessage(message) }
                    }
                    "online" -> {
                        val count = event.optInt("onlineCount", 0)
                        val topic = event.optString("discussing")
                        val users = event.optJSONArray("users").toOnlineUserList()
                        post { onOnline(count, topic, users) }
                    }
                    "discussChanged" -> {
                        val topic = event.optString("topic")
                        post { onDiscussChanged(topic) }
                    }
                    "node" -> {
                        val name = event.optString("name")
                        if (name.isNotBlank()) {
                            post { onNodeChanged(name) }
                        }
                    }
                    "revoke" -> {
                        val id = event.optString("id")
                        post {
                            if (id.isNotBlank()) {
                                onRevoke(id)
                            }
                        }
                    }
                    "redPacketStatus" -> {
                        val id = event.optRealtimeMessageId()
                        if (id.isNotBlank()) {
                            val update = RedPacketStatusUpdate(
                                messageId = id,
                                count = event.optLong("count"),
                                got = event.optLong("got"),
                                whoGive = event.optString("whoGive"),
                            )
                            post { onRedPacketStatus(update) }
                        }
                    }
                    "chatReaction" -> {
                        val id = event.optRealtimeMessageId()
                        if (id.isNotBlank()) {
                            val update = ChatReactionUpdate(
                                messageId = id,
                                summary = event.optJSONArray("reactionSummary").toReactionSummaryList(),
                                actorReaction = event.optString("actorReaction"),
                                actorUserId = event.optString("actorUserId"),
                                targetType = event.optString("targetType"),
                                groupType = event.optString("groupType"),
                                dataJson = event.opt("data")?.toString().orEmpty(),
                            )
                            post { onReaction(update) }
                        }
                    }
                    "custom" -> {
                        val message = event.optString("message")
                        if (message.isNotBlank()) {
                            val systemMessage = ChatRoomMessage(
                                oId = "custom:${System.currentTimeMillis()}:$message",
                                userName = "",
                                userNickname = "",
                                content = message,
                                time = "",
                                client = "system",
                                type = "system",
                            )
                            post { onMessage(systemMessage) }
                        }
                    }
                }
            }
        }
    }
}

private fun org.json.JSONArray?.toOnlineUserList(): List<ChatOnlineUser> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = opt(index)
            when (item) {
                is org.json.JSONObject -> {
                    val username = item.optString("userName").trim()
                    if (username.isNotBlank()) {
                        add(ChatOnlineUser(userName = username, avatarUrl = item.optString("avatar").trim()))
                    }
                }
                else -> {
                    val username = optString(index).trim()
                    if (username.isNotBlank()) {
                        add(ChatOnlineUser(userName = username))
                    }
                }
            }
        }
    }
}
