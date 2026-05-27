package dev.fishpi.mobile.feature.pluginui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.fishpi.mobile.FishPiNotifier

@Composable
internal fun PluginUiRoute(
    controller: PluginUiController = PluginUiController.get(),
) {
    val state by controller.state.collectAsState()

    LaunchedEffect(controller) {
        controller.effects.collect { effect ->
            when (effect) {
                is PluginUiEffect.ShowError -> FishPiNotifier.error(effect.message)
            }
        }
    }

    DefaultPluginUi(
        state = state,
        dispatch = controller::dispatch,
    )
}
