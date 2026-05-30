package dev.fishpi.mobile.plugin

import android.util.Log
import android.webkit.JavascriptInterface
import org.json.JSONObject

internal class PluginBridge(
    val pluginId: String,
    private val evalJs: (String) -> Unit,
    private val dispatch: (pluginId: String, action: String, args: JSONObject) -> JSONObject,
) {
    @JavascriptInterface
    fun call(action: String, argsJson: String): String {
        return runCatching {
            val args = if (argsJson.isBlank()) JSONObject() else JSONObject(argsJson)
            dispatch(pluginId, action, args).toString()
        }.getOrElse { e ->
            JSONObject().put("ok", false)
                .put("error", e.message ?: "call failed").toString()
        }
    }

    @JavascriptInterface
    fun log(message: String) {
        Log.d("FishPiPlugin", "[$pluginId] $message")
    }

    @JavascriptInterface
    fun getStorage(key: String, defaultValue: String): String {
        val prefs = PluginManager.storageFor(pluginId)
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    @JavascriptInterface
    fun setStorage(key: String, value: String) {
        val prefs = PluginManager.storageFor(pluginId)
        prefs.edit().putString(key, value).apply()
    }

    fun emit(event: String, dataJson: String) {
        val escaped = dataJson.replace("\\", "\\\\").replace("'", "\\'")
        evalJs("_emit('$event','$escaped')")
    }
}
