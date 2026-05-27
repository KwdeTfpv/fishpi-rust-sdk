package dev.fishpi.mobile.feature.pluginui

enum class PluginUiContainerType {
    Dialog,
    Page,
    Sheet,
}

data class PluginUiDocument(
    val id: String,
    val pluginId: String,
    val title: String,
    val container: PluginUiContainerType,
    val nodes: List<PluginUiNode> = emptyList(),
    val open: Boolean = false,
    val error: String? = null,
)

sealed interface PluginUiNode {
    val id: String

    data class Text(
        override val id: String,
        val text: String,
        val style: String = "body",
    ) : PluginUiNode

    data class Markdown(override val id: String, val text: String) : PluginUiNode
    data class Image(override val id: String, val url: String, val caption: String = "") : PluginUiNode
    data class Divider(override val id: String) : PluginUiNode
    data class Space(override val id: String, val height: Int = 12) : PluginUiNode
    data class Json(override val id: String, val json: String) : PluginUiNode
    data class Loading(override val id: String, val text: String = "加载中...") : PluginUiNode
    data class Error(override val id: String, val text: String) : PluginUiNode
    data class Empty(override val id: String, val text: String = "暂无内容") : PluginUiNode

    data class Card(
        override val id: String,
        val title: String = "",
        val subtitle: String = "",
        val actionId: String = "",
        val children: List<PluginUiNode> = emptyList(),
    ) : PluginUiNode

    data class Section(
        override val id: String,
        val title: String = "",
        val children: List<PluginUiNode> = emptyList(),
    ) : PluginUiNode

    data class Row(override val id: String, val children: List<PluginUiNode> = emptyList()) : PluginUiNode
    data class Columns(override val id: String, val children: List<PluginUiNode> = emptyList()) : PluginUiNode

    data class Tabs(
        override val id: String,
        val tabs: List<PluginUiTab> = emptyList(),
    ) : PluginUiNode

    data class Input(
        override val id: String,
        val name: String,
        val label: String,
        val value: String = "",
        val placeholder: String = "",
        val multiline: Boolean = false,
    ) : PluginUiNode

    data class Number(
        override val id: String,
        val name: String,
        val label: String,
        val value: Double = 0.0,
        val min: Double? = null,
        val max: Double? = null,
    ) : PluginUiNode

    data class Switch(
        override val id: String,
        val name: String,
        val label: String,
        val checked: Boolean = false,
    ) : PluginUiNode

    data class Select(
        override val id: String,
        val name: String,
        val label: String,
        val value: String = "",
        val options: List<PluginUiOption> = emptyList(),
    ) : PluginUiNode

    data class Chips(
        override val id: String,
        val name: String,
        val values: List<String> = emptyList(),
        val options: List<PluginUiOption> = emptyList(),
    ) : PluginUiNode

    data class Slider(
        override val id: String,
        val name: String,
        val label: String,
        val value: Float = 0f,
        val min: Float = 0f,
        val max: Float = 100f,
    ) : PluginUiNode

    data class ListNode(
        override val id: String,
        val items: List<PluginUiListItem> = emptyList(),
    ) : PluginUiNode

    data class Table(
        override val id: String,
        val headers: List<String> = emptyList(),
        val rows: List<List<String>> = emptyList(),
    ) : PluginUiNode

    data class Stat(
        override val id: String,
        val label: String,
        val value: String,
        val detail: String = "",
    ) : PluginUiNode

    data class UserCard(
        override val id: String,
        val username: String,
        val displayName: String = "",
        val avatar: String = "",
        val actionId: String = "",
    ) : PluginUiNode

    data class ArticleCard(
        override val id: String,
        val articleId: String,
        val title: String,
        val preview: String = "",
        val actionId: String = "",
    ) : PluginUiNode

    data class ActionBar(
        override val id: String,
        val actions: List<PluginUiButton> = emptyList(),
    ) : PluginUiNode

    data class Button(
        override val id: String,
        val label: String,
        val actionId: String,
        val enabled: Boolean = true,
    ) : PluginUiNode
}

data class PluginUiTab(
    val id: String,
    val label: String,
    val children: List<PluginUiNode> = emptyList(),
)

data class PluginUiOption(val value: String, val label: String)

data class PluginUiListItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val actionId: String = "",
)

data class PluginUiButton(
    val id: String,
    val label: String,
    val actionId: String,
    val enabled: Boolean = true,
)
