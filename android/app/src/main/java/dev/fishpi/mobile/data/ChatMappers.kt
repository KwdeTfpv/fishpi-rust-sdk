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
        md = optString("md"),
        contentHtml = optString("contentHtml"),
        imageUrls = optJSONArray("imageUrls").toStringList(),
        linkUrls = optJSONArray("linkUrls").toStringList(),
        time = optString("time"),
        client = optString("client"),
        barragerColor = optString("barragerColor"),
        type = optString("type", "msg"),
        revoked = optBoolean("revoked", false),
        reactionSummary = optJSONArray("reactionSummary").toReactionSummaryList(),
        currentUserReaction = optString("currentUserReaction"),
        redPacket = optJSONObject("redPacket")?.toRedPacketPreview(),
        quote = optJSONObject("quote")?.toChatQuotePreview(),
    )
}

private fun JSONObject.toChatQuotePreview(): ChatQuotePreview {
    return ChatQuotePreview(
        text = optString("text"),
        imageUrls = optJSONArray("imageUrls").toStringList(),
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
