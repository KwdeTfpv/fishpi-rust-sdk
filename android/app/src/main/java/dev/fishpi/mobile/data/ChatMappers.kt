package dev.fishpi.mobile.data

import org.json.JSONArray
import org.json.JSONObject

internal fun JSONObject.toChatRoomMessage(): ChatRoomMessage {
    return ChatRoomMessage(
        oId = optString("oId"),
        userName = optString("userName"),
        userNickname = optString("userNickname"),
        userAvatarURL = optString("userAvatarURL"),
        content = optString("content"),
        contentKind = ChatContentKind.fromWire(optString("contentKind", "markdown")),
        media = optJSONArray("media").toChatMediaList(),
        time = optString("time"),
        client = optString("client"),
        barragerColor = optString("barragerColor"),
        reactionSummary = optJSONArray("reactionSummary").toReactionSummaryList(),
        currentUserReaction = optString("currentUserReaction"),
        redPacket = optJSONObject("redPacket")?.toRedPacketPreview(),
        music = optJSONObject("music")?.toMusicPreview(),
    )
}

internal fun JSONArray?.toChatMediaList(): List<ChatMedia> {
    if (this == null) {
        return emptyList()
    }
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val kind = item.optString("kind").trim()
            val url = item.optString("url").trim()
            if (kind.isNotEmpty() && url.isNotEmpty()) {
                add(ChatMedia(kind = kind, url = url))
            }
        }
    }
}

private fun JSONObject.toMusicPreview(): MusicPreview {
    return MusicPreview(
        coverUrl = optString("coverURL"),
        source = optString("source"),
        title = optString("title"),
        from = optString("from"),
    )
}

private fun JSONObject.toRedPacketPreview(): RedPacketPreview {
    return RedPacketPreview(
        type = optString("type"),
        typeName = optString("typeName", "红包"),
        money = optLong("money"),
        count = optLong("count"),
        got = optLong("got"),
        message = optString("message"),
        summary = optString("summary"),
        finished = optBoolean("finished", false),
        openable = optBoolean("openable", false),
        needGesture = optBoolean("needGesture", false),
        receivers = optJSONArray("receivers").toStringList(),
        gesture = if (isNull("gesture")) null else optInt("gesture"),
        who = optJSONArray("who").toRedPacketGotList(),
    )
}

internal fun JSONArray?.toStringList(): List<String> {
    if (this == null) {
        return emptyList()
    }
    return buildList {
        for (index in 0 until length()) {
            optString(index).takeIf { it.isNotBlank() }?.let(::add)
        }
    }
}

internal fun JSONArray?.toReactionSummaryList(): List<ReactionSummaryItem> {
    return mapObjects { item ->
        ReactionSummaryItem(
            value = item.optString("value"),
            emoji = item.optString("emoji"),
            count = item.optLong("count"),
            selected = item.optBoolean("selected"),
        )
    }
}

internal fun JSONArray?.toRedPacketGotList(): List<RedPacketGot> {
    return mapObjects { item ->
        RedPacketGot(
            userId = item.optString("userId"),
            userName = item.optString("userName"),
            avatar = item.optString("avatar"),
            userMoney = item.optLong("userMoney"),
            time = item.optString("time"),
        )
    }
}
