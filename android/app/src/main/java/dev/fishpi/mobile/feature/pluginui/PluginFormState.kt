package dev.fishpi.mobile.feature.pluginui

data class PluginFormState(
    val values: Map<String, PluginFormValue> = emptyMap(),
) {
    fun put(name: String, value: PluginFormValue): PluginFormState =
        copy(values = values + (name to value))
}

sealed interface PluginFormValue {
    data class Text(val value: String) : PluginFormValue
    data class Number(val value: Double) : PluginFormValue
    data class Bool(val value: Boolean) : PluginFormValue
    data class Strings(val value: List<String>) : PluginFormValue
}
