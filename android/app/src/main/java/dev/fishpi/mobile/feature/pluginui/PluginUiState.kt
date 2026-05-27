package dev.fishpi.mobile.feature.pluginui

data class PluginUiState(
    val current: PluginUiDocument? = null,
    val backStack: List<PluginUiDocument> = emptyList(),
    val form: PluginFormState = PluginFormState(),
    val lastError: String? = null,
)
