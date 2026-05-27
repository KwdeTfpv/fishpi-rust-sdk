package dev.fishpi.mobile.feature.pluginui

import org.json.JSONArray
import org.json.JSONObject

object PluginUiMapper {
    fun document(pluginId: String, payload: JSONObject): PluginUiDocument {
        val container = when (payload.optString("container", payload.optString("type", "dialog"))) {
            "page" -> PluginUiContainerType.Page
            "sheet" -> PluginUiContainerType.Sheet
            else -> PluginUiContainerType.Dialog
        }
        val id = payload.optString("id").ifBlank { "$pluginId:${container.name.lowercase()}" }
        return PluginUiDocument(
            id = id,
            pluginId = pluginId,
            title = payload.optString("title").ifBlank { "插件" },
            container = container,
            nodes = nodes(payload.optJSONArray("nodes") ?: payload.optJSONArray("children")),
            open = payload.optBoolean("open", true),
            error = payload.optString("error").takeIf { it.isNotBlank() },
        )
    }

    fun nodes(array: JSONArray?): List<PluginUiNode> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let { node(it, "node-$index") }
        }
    }

    private fun node(raw: JSONObject, fallbackId: String): PluginUiNode {
        val type = raw.optString("type", "text")
        val id = raw.optString("id").ifBlank { fallbackId }
        return when (type) {
            "markdown" -> PluginUiNode.Markdown(id, raw.optString("text", raw.optString("content")))
            "image" -> PluginUiNode.Image(id, raw.optString("url"), raw.optString("caption"))
            "divider" -> PluginUiNode.Divider(id)
            "space" -> PluginUiNode.Space(id, raw.optInt("height", 12))
            "json" -> PluginUiNode.Json(id, raw.opt("data")?.toString() ?: raw.optString("text", raw.toString(2)))
            "card" -> PluginUiNode.Card(
                id = id,
                title = raw.optString("title"),
                subtitle = raw.optString("subtitle"),
                actionId = raw.optString("actionId"),
                children = nodes(raw.optJSONArray("children")),
            )
            "section" -> PluginUiNode.Section(id, raw.optString("title"), nodes(raw.optJSONArray("children")))
            "row" -> PluginUiNode.Row(id, nodes(raw.optJSONArray("children")))
            "columns" -> PluginUiNode.Columns(id, nodes(raw.optJSONArray("children")))
            "tabs" -> PluginUiNode.Tabs(id, tabs(raw.optJSONArray("tabs")))
            "input" -> PluginUiNode.Input(
                id = id,
                name = raw.optString("name").ifBlank { id },
                label = raw.optString("label").ifBlank { raw.optString("name").ifBlank { "输入" } },
                value = raw.optString("value"),
                placeholder = raw.optString("placeholder"),
            )
            "textarea" -> PluginUiNode.Input(
                id = id,
                name = raw.optString("name").ifBlank { id },
                label = raw.optString("label").ifBlank { raw.optString("name").ifBlank { "输入" } },
                value = raw.optString("value"),
                placeholder = raw.optString("placeholder"),
                multiline = true,
            )
            "number" -> PluginUiNode.Number(
                id = id,
                name = raw.optString("name").ifBlank { id },
                label = raw.optString("label").ifBlank { raw.optString("name").ifBlank { "数字" } },
                value = raw.optDouble("value", 0.0),
                min = raw.optNullableDouble("min"),
                max = raw.optNullableDouble("max"),
            )
            "switch" -> PluginUiNode.Switch(
                id = id,
                name = raw.optString("name").ifBlank { id },
                label = raw.optString("label").ifBlank { raw.optString("name").ifBlank { "开关" } },
                checked = raw.optBoolean("checked", raw.optBoolean("value", false)),
            )
            "select" -> PluginUiNode.Select(
                id = id,
                name = raw.optString("name").ifBlank { id },
                label = raw.optString("label").ifBlank { raw.optString("name").ifBlank { "选择" } },
                value = raw.optString("value"),
                options = options(raw.optJSONArray("options")),
            )
            "chips" -> PluginUiNode.Chips(
                id = id,
                name = raw.optString("name").ifBlank { id },
                values = strings(raw.optJSONArray("values")),
                options = options(raw.optJSONArray("options")),
            )
            "slider" -> PluginUiNode.Slider(
                id = id,
                name = raw.optString("name").ifBlank { id },
                label = raw.optString("label").ifBlank { raw.optString("name").ifBlank { "滑块" } },
                value = raw.optDouble("value", 0.0).toFloat(),
                min = raw.optDouble("min", 0.0).toFloat(),
                max = raw.optDouble("max", 100.0).toFloat(),
            )
            "loading" -> PluginUiNode.Loading(id, raw.optString("text").ifBlank { "加载中..." })
            "error" -> PluginUiNode.Error(id, raw.optString("text").ifBlank { "加载失败" })
            "empty" -> PluginUiNode.Empty(id, raw.optString("text").ifBlank { "暂无内容" })
            "list" -> PluginUiNode.ListNode(id, listItems(raw.optJSONArray("items")))
            "table" -> PluginUiNode.Table(
                id = id,
                headers = strings(raw.optJSONArray("headers")),
                rows = tableRows(raw.optJSONArray("rows")),
            )
            "stat" -> PluginUiNode.Stat(id, raw.optString("label"), raw.optString("value"), raw.optString("detail"))
            "userCard" -> PluginUiNode.UserCard(
                id = id,
                username = raw.optString("username"),
                displayName = raw.optString("displayName"),
                avatar = raw.optString("avatar"),
                actionId = raw.optString("actionId"),
            )
            "articleCard" -> PluginUiNode.ArticleCard(
                id = id,
                articleId = raw.optString("articleId"),
                title = raw.optString("title"),
                preview = raw.optString("preview"),
                actionId = raw.optString("actionId"),
            )
            "actionBar" -> PluginUiNode.ActionBar(id, buttons(raw.optJSONArray("actions")))
            "button" -> PluginUiNode.Button(
                id = id,
                label = raw.optString("label").ifBlank { raw.optString("text").ifBlank { "按钮" } },
                actionId = raw.optString("actionId").ifBlank { id },
                enabled = raw.optBoolean("enabled", true),
            )
            else -> PluginUiNode.Text(id, raw.optString("text", raw.optString("content", raw.toString())), raw.optString("style", "body"))
        }
    }

    private fun tabs(array: JSONArray?): List<PluginUiTab> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val raw = array.optJSONObject(index) ?: return@mapNotNull null
            PluginUiTab(
                id = raw.optString("id").ifBlank { "tab-$index" },
                label = raw.optString("label").ifBlank { "Tab ${index + 1}" },
                children = nodes(raw.optJSONArray("children")),
            )
        }
    }

    private fun options(array: JSONArray?): List<PluginUiOption> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val item = array.opt(index)
            when (item) {
                is JSONObject -> PluginUiOption(item.optString("value"), item.optString("label").ifBlank { item.optString("value") })
                is String -> PluginUiOption(item, item)
                else -> null
            }
        }
    }

    private fun listItems(array: JSONArray?): List<PluginUiListItem> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val raw = array.optJSONObject(index) ?: return@mapNotNull null
            PluginUiListItem(
                id = raw.optString("id").ifBlank { "item-$index" },
                title = raw.optString("title"),
                subtitle = raw.optString("subtitle"),
                actionId = raw.optString("actionId"),
            )
        }
    }

    private fun buttons(array: JSONArray?): List<PluginUiButton> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val raw = array.optJSONObject(index) ?: return@mapNotNull null
            PluginUiButton(
                id = raw.optString("id").ifBlank { "action-$index" },
                label = raw.optString("label").ifBlank { raw.optString("text").ifBlank { "按钮" } },
                actionId = raw.optString("actionId").ifBlank { raw.optString("id").ifBlank { "action-$index" } },
                enabled = raw.optBoolean("enabled", true),
            )
        }
    }

    private fun strings(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { array.optString(it) }
    }

    private fun tableRows(array: JSONArray?): List<List<String>> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { rowIndex ->
            strings(array.optJSONArray(rowIndex))
        }
    }

    private fun JSONObject.optNullableDouble(name: String): Double? =
        if (has(name) && !isNull(name)) optDouble(name) else null
}
