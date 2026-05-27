package dev.fishpi.mobile.data

import org.json.JSONArray
import org.json.JSONObject

internal inline fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
    if (this == null) {
        return emptyList()
    }
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(transform(item))
        }
    }
}

internal fun JSONObject.dataArray(): JSONArray? =
    optJSONArray("data")

internal fun JSONObject.toFishPiUser(): FishPiUser {
    return FishPiUser(
        userId = optString("userId"),
        userName = optString("userName"),
        userNickname = optString("userNickname"),
        userAvatarUrl = optString("userAvatarURL"),
        cardBg = optString("cardBg"),
        role = optString("role"),
        userNo = optString("userNo").ifBlank { optLong("userNo").takeIf { it > 0 }?.toString().orEmpty() },
        intro = optString("intro"),
        city = optString("city"),
        url = optString("url"),
        points = optLong("points"),
        following = optLong("following"),
        follower = optLong("follower"),
        onlineMinutes = optLong("onlineMinutes"),
        canFollow = optString("canFollow"),
    )
}

internal fun JSONObject.toPrivateChatSession(): PrivateChatSession {
    return PrivateChatSession(
        peer = optString("peer"),
        preview = optString("preview"),
        time = optString("time"),
        avatar = optString("avatar"),
        unread = optLong("unread"),
        sort = optLong("sort"),
    )
}

internal fun JSONObject.toPrivateChatNotice(): PrivateChatNotice {
    return PrivateChatNotice(
        peer = optString("peer"),
        preview = optString("preview"),
        avatar = optString("avatar"),
        userId = optString("userId"),
    )
}

internal fun JSONObject.toEmojiGroupView(): EmojiGroupView {
    return EmojiGroupView(
        id = optString("id"),
        name = optString("name"),
        count = optLong("count"),
        isDefault = optBoolean("isDefault", false),
        sort = optLong("sort", Long.MAX_VALUE),
    )
}

internal fun JSONObject.toEmojiItemView(): EmojiItemView {
    return EmojiItemView(
        id = optString("id"),
        groupId = optString("groupId"),
        name = optString("name"),
        url = optString("url"),
        sort = optLong("sort", Long.MAX_VALUE),
    )
}
