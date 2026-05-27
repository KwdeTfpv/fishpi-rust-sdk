package dev.fishpi.mobile.data

import org.json.JSONObject

internal fun JSONObject.toNoticeItemView(): NoticeItemView {
    val author = if (has("author")) optString("author").takeIf { it.isNotBlank() } else null
    return NoticeItemView(
        id = optString("id"),
        category = optString("category"),
        author = author,
        title = optString("title"),
        content = optString("content"),
        dataType = optInt("dataType", -1),
        time = optString("time"),
        read = optBoolean("read", false),
        jumpType = optString("jumpType"),
        jumpId = optString("jumpId"),
        mentionUser = optString("mentionUser"),
    )
}
