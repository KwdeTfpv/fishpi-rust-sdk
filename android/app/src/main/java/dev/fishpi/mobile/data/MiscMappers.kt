package dev.fishpi.mobile.data

import org.json.JSONArray
import org.json.JSONObject

internal fun JSONObject.toMedalView(): MedalView {
    fun firstText(vararg keys: String): String {
        keys.forEach { key ->
            val value = optString(key).trim()
            if (value.isNotBlank()) {
                return value
            }
        }
        return ""
    }

    val rawAttr = optString("medal_attr").trim()

    fun parseAttrMap(): Map<String, String> {
        val raw = rawAttr
        if (raw.isBlank()) return emptyMap()
        return raw.split("&")
            .mapNotNull { part ->
                val idx = part.indexOf("=")
                if (idx <= 0) return@mapNotNull null
                val key = part.substring(0, idx).trim().lowercase()
                val value = part.substring(idx + 1).trim()
                if (key.isBlank() || value.isBlank()) return@mapNotNull null
                key to value
            }.toMap()
    }
    val attr = parseAttrMap()

    return MedalView(
        id = firstText("medal_id", "medalId", "id", "oId"),
        name = firstText("medal_name", "medalName", "name", "title"),
        text = firstText("txt", "data"),
        imageUrl = attr["url"].orEmpty(),
        backColor = attr["backcolor"].orEmpty(),
        fontColor = attr["fontcolor"].orEmpty(),
        type = firstText("medal_type", "medalType", "type"),
        description = firstText("medal_description", "medalDescription", "description"),
        rawAttr = rawAttr,
    )
}

internal fun JSONArray?.toBreezemoons(): List<BreezemoonView> {
    return mapObjects { item ->
        BreezemoonView(
            id = item.optString("id"),
            authorName = item.optString("authorName"),
            updated = item.optString("updated"),
            created = item.optString("created"),
            timeAgo = item.optString("timeAgo"),
            content = item.optString("content"),
            createTime = item.optString("createTime"),
            city = item.optString("city"),
            avatar = item.optString("avatar"),
        )
    }
}
