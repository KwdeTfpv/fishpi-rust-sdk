package dev.fishpi.mobile.feature.pluginui

import dev.fishpi.mobile.core.ui.UiEffect

sealed interface PluginUiEffect : UiEffect {
    data class ShowError(val message: String) : PluginUiEffect
}
