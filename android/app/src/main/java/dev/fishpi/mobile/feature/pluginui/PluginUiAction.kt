package dev.fishpi.mobile.feature.pluginui

import org.json.JSONObject

sealed interface PluginUiAction {
    data class Open(val pluginId: String, val payload: JSONObject) : PluginUiAction
    data class Update(val pluginId: String, val payload: JSONObject) : PluginUiAction
    data class Close(val pluginId: String? = null) : PluginUiAction
    data class Clear(val pluginId: String) : PluginUiAction

    data class StreamPush(
        val pluginId: String,
        val streamId: String,
        val delta: String,
        val replace: Boolean = false,
    ) : PluginUiAction

    data class StreamEnd(
        val pluginId: String,
        val streamId: String,
        val finalText: String? = null,
    ) : PluginUiAction

    data class TriggerAction(val actionId: String, val nodeId: String = "") : PluginUiAction
    data class ChangeText(val name: String, val value: String) : PluginUiAction
    data class ChangeNumber(val name: String, val value: Double) : PluginUiAction
    data class ChangeBool(val name: String, val value: Boolean) : PluginUiAction
    data class ChangeStrings(val name: String, val value: List<String>) : PluginUiAction
    data object ClearError : PluginUiAction
}
