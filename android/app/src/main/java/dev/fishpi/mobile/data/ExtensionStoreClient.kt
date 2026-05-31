package dev.fishpi.mobile.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class ExtensionStoreSession(
    val accessToken: String,
    val userId: String,
    val username: String,
)

data class ExtensionStoreItem(
    val id: Long,
    val name: String,
    val description: String,
    val type: String,
    val identifier: String,
    val author: String,
    val version: String,
    val status: String,
    val price: String,
    val url: String,
    val code: String,
    val language: String,
    val upgradeFromId: Long?,
    val isPurchased: Boolean,
    val purchaseCount: Int,
)

data class ExtensionStorePage(
    val items: List<ExtensionStoreItem>,
    val total: Int,
)

data class ExtensionStoreComment(
    val id: Long,
    val content: String,
    val createdAt: String,
    val authorId: String,
    val username: String,
    val nickname: String,
    val avatar: String,
    val isAdmin: Boolean,
)

class ExtensionStoreClient private constructor(
    private val baseUrl: String = DefaultBaseUrl,
) {
    companion object {
        val shared: ExtensionStoreClient by lazy { ExtensionStoreClient() }

        const val TypeAppExtension = "app-extension"
        const val TypeAppTheme = "app-theme"
        private const val DefaultBaseUrl = "https://ext.adventext.fun/api"
        private const val MaxStoreContentBytes = 2 * 1024 * 1024
    }

    fun getToken(apiKey: String): ExtensionStoreSession {
        val data = requestJson(
            method = "GET",
            path = "/auth/getToken",
            headers = mapOf("fishpi-key" to apiKey.trim()),
        ).unwrapObject()
        val user = data.optJSONObject("user") ?: JSONObject()
        return ExtensionStoreSession(
            accessToken = data.optString("access_token").ifBlank { error("鱼排扩展集市 Token 为空") },
            userId = user.optString("id"),
            username = user.optString("username"),
        )
    }

    fun getPublishedItems(
        search: String = "",
        type: String? = null,
        page: Int = 1,
        limit: Int = 30,
    ): ExtensionStorePage {
        val params = buildList {
            val keyword = search.trim()
            if (keyword.isNotBlank()) add("search=${keyword.urlEncode()}")
            if (!type.isNullOrBlank()) add("type=${type.urlEncode()}")
            add("page=$page")
            add("limit=$limit")
        }.joinToString("&")
        val data = requestJson("GET", "/items?$params").unwrapData()
        return when (data) {
            is JSONObject -> {
                val items = data.optJSONArray("items")
                    ?.mapObjects { it.toExtensionStoreItem() }
                    .orEmpty()
                    .onlyAppItems()
                ExtensionStorePage(items = items, total = data.optInt("total", items.size))
            }
            is JSONArray -> {
                val items = data.mapObjects { it.toExtensionStoreItem() }.onlyAppItems()
                ExtensionStorePage(items = items, total = items.size)
            }
            else -> ExtensionStorePage(emptyList(), 0)
        }
    }

    fun getMyPurchases(token: String, type: String? = null): List<ExtensionStoreItem> {
        val params = buildList {
            if (!type.isNullOrBlank()) add("type=${type.urlEncode()}")
        }.joinToString("&")
        val path = if (params.isBlank()) "/items/my-purchases" else "/items/my-purchases?$params"
        val data = requestJson(
            method = "GET",
            path = path,
            headers = authHeaders(token),
        ).unwrapData()
        return when (data) {
            is JSONArray -> data.mapObjects { it.toExtensionStoreItem() }.onlyAppItems()
            is JSONObject -> data.optJSONArray("items")?.mapObjects { it.toExtensionStoreItem() }.orEmpty().onlyAppItems()
            else -> emptyList()
        }
    }

    fun getItem(id: Long, token: String? = null): ExtensionStoreItem {
        val data = requestJson(
            method = "GET",
            path = "/items/$id",
            headers = token?.takeIf { it.isNotBlank() }?.let { authHeaders(it) }.orEmpty(),
        ).unwrapData()
        return (data as? JSONObject)?.toExtensionStoreItem()
            ?: error("鱼排扩展集市详情返回为空")
    }

    fun getItemVersions(id: Long, token: String? = null): List<ExtensionStoreItem> {
        val data = requestJson(
            method = "GET",
            path = "/items/$id/versions",
            headers = token?.takeIf { it.isNotBlank() }?.let { authHeaders(it) }.orEmpty(),
        ).unwrapData()
        return when (data) {
            is JSONArray -> data.mapObjects { it.toExtensionStoreItem() }.onlyAppItems()
            is JSONObject -> data.optJSONArray("items")?.mapObjects { it.toExtensionStoreItem() }.orEmpty().onlyAppItems()
            else -> emptyList()
        }
    }

    fun getItemComments(id: Long, token: String? = null): List<ExtensionStoreComment> {
        val data = requestJson(
            method = "GET",
            path = "/items/$id/comments",
            headers = token?.takeIf { it.isNotBlank() }?.let { authHeaders(it) }.orEmpty(),
        ).unwrapData()
        return when (data) {
            is JSONArray -> data.mapComments()
            is JSONObject -> data.optJSONArray("items")?.mapComments().orEmpty()
            else -> emptyList()
        }
    }

    fun postItemComment(token: String, id: Long, content: String): ExtensionStoreComment {
        val data = requestJson(
            method = "POST",
            path = "/items/$id/comments",
            headers = authHeaders(token),
            body = JSONObject().put("content", content),
        ).unwrapData()
        val raw = data as? JSONObject ?: error("鱼排扩展集市评论返回为空")
        return raw.toExtensionStoreComment()
    }

    fun purchaseItem(token: String, id: Long): ExtensionStoreItem {
        val data = requestJson(
            method = "POST",
            path = "/items/$id/purchase",
            headers = authHeaders(token),
        ).unwrapData()
        return (data as? JSONObject)?.toExtensionStoreItem()
            ?: error("鱼排扩展集市购买返回为空")
    }

    fun downloadItemContent(item: ExtensionStoreItem): String {
        if (item.code.isNotBlank()) return item.code
        if (item.url.isNotBlank()) return downloadText(item.url)
        val suffix = if (item.type == TypeAppTheme) "app-theme.json" else "app-extension.js"
        return downloadText("$baseUrl/items/${item.id}/$suffix")
    }

    private fun downloadText(rawUrl: String): String {
        val connection = openConnection(normalizeUrl(rawUrl), "GET")
        return connection.readResponseBytes().toString(Charsets.UTF_8)
    }

    private fun requestJson(
        method: String,
        path: String,
        headers: Map<String, String> = emptyMap(),
        body: JSONObject? = null,
    ): JSONObject {
        val connection = openConnection("$baseUrl$path", method)
        headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
        if (body != null) {
            val bytes = body.toString().toByteArray(Charsets.UTF_8)
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { it.write(bytes) }
        }
        val text = connection.readResponseBytes().toString(Charsets.UTF_8)
        return runCatching { JSONObject(text) }.getOrElse {
            val contentType = connection.getHeaderField("Content-Type").orEmpty()
            error("鱼排扩展集市返回的不是 JSON：$contentType")
        }
    }

    private fun openConnection(url: String, method: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json, text/plain, */*")
            setRequestProperty("User-Agent", "FishPi-Mobile-Android")
        }

    private fun HttpURLConnection.readResponseBytes(limitBytes: Int = MaxStoreContentBytes): ByteArray {
        val status = responseCode
        val stream = if (status in 200..299) inputStream else errorStream
        val bytes = stream?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                require(total <= limitBytes) { "鱼排扩展集市内容过大" }
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        } ?: ByteArray(0)
        if (status !in 200..299) {
            val message = bytes.toString(Charsets.UTF_8).ifBlank { responseMessage ?: "HTTP $status" }
            error(message)
        }
        return bytes
    }

    private fun JSONObject.unwrapObject(): JSONObject {
        val code = optInt("code", 0)
        if (code != 0) error(optString("msg").ifBlank { "鱼排扩展集市请求失败" })
        return optJSONObject("data") ?: JSONObject()
    }

    private fun JSONObject.unwrapData(): Any? {
        val code = optInt("code", 0)
        if (code != 0) error(optString("msg").ifBlank { "鱼排扩展集市请求失败" })
        return opt("data")
    }

    private fun normalizeUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        require(trimmed.isNotBlank()) { "扩展内容地址为空" }
        return when {
            trimmed.startsWith("/") -> {
                val base = URL(baseUrl)
                val port = if (base.port > 0) ":${base.port}" else ""
                "${base.protocol}://${base.host}$port$trimmed"
            }
            else -> trimmed
        }
    }

    private fun authHeaders(token: String): Map<String, String> =
        mapOf("Authorization" to "Bearer ${token.trim()}")
}

private fun JSONObject.toExtensionStoreItem(): ExtensionStoreItem {
    val authorValue = opt("author")
    val authorName = when (authorValue) {
        is JSONObject -> authorValue.optString("username").ifBlank { authorValue.optString("name") }
        null -> ""
        else -> authorValue.toString()
    }
    return ExtensionStoreItem(
        id = optLong("id"),
        name = optString("name"),
        description = optString("description"),
        type = optString("type"),
        identifier = optString("identifier"),
        author = authorName,
        version = optString("version"),
        status = optString("status"),
        price = optString("price"),
        url = optString("url"),
        code = optString("code"),
        language = optString("language"),
        upgradeFromId = optNullableLong("upgradeFromId"),
        isPurchased = optBoolean("isPurchased", false),
        purchaseCount = optInt("purchaseCount", 0),
    )
}

private fun JSONObject.optNullableLong(name: String): Long? =
    if (has(name) && !isNull(name)) optLong(name) else null

private fun List<ExtensionStoreItem>.onlyAppItems(): List<ExtensionStoreItem> =
    filter { it.type == ExtensionStoreClient.TypeAppExtension || it.type == ExtensionStoreClient.TypeAppTheme }

private fun JSONArray.mapObjects(mapper: (JSONObject) -> ExtensionStoreItem): List<ExtensionStoreItem> =
    buildList {
        for (index in 0 until length()) {
            optJSONObject(index)?.let { add(mapper(it)) }
        }
    }

private fun JSONArray.mapComments(): List<ExtensionStoreComment> =
    buildList {
        for (index in 0 until length()) {
            optJSONObject(index)?.let { add(it.toExtensionStoreComment()) }
        }
    }

private fun JSONObject.toExtensionStoreComment(): ExtensionStoreComment {
    val author = optJSONObject("author")
    return ExtensionStoreComment(
        id = optLong("id"),
        content = optString("content"),
        createdAt = optString("createdAt"),
        authorId = optString("authorId").ifBlank { author?.optString("id").orEmpty() },
        username = author?.optString("username").orEmpty(),
        nickname = author?.optString("nickname").orEmpty(),
        avatar = author?.optString("avatar").orEmpty(),
        isAdmin = author?.optBoolean("isAdmin", false) == true,
    )
}

private fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")
