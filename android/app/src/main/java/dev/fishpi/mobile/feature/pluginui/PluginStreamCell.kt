package dev.fishpi.mobile.feature.pluginui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class PluginStreamCell {
    var text: String by mutableStateOf("")
        private set

    var done: Boolean by mutableStateOf(false)
        private set

    fun append(delta: String) {
        if (delta.isEmpty()) return
        text += delta
    }

    fun set(value: String) {
        text = value
    }

    fun finish(finalText: String?) {
        if (finalText != null) text = finalText
        done = true
    }
}
