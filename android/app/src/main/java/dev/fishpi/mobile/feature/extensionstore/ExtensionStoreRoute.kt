package dev.fishpi.mobile.feature.extensionstore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.fishpi.mobile.FishPiNotifier

@Composable
internal fun ExtensionStoreRoute(
    apiKey: String,
    onImportTheme: (String) -> Result<String>,
    isThemeSaved: (String) -> Boolean,
) {
    val controller = remember(apiKey) {
        ExtensionStoreController(apiKey = apiKey)
    }
    val state by controller.state.collectAsState()

    LaunchedEffect(controller) {
        controller.dispatch(ExtensionStoreAction.Initialize)
    }

    LaunchedEffect(controller) {
        controller.effects.collect { effect ->
            when (effect) {
                is ExtensionStoreEffect.ShowMessage -> FishPiNotifier.success(effect.message)
                is ExtensionStoreEffect.ShowError -> FishPiNotifier.error(effect.message)
                ExtensionStoreEffect.UploadFinished -> Unit
            }
        }
    }

    DisposableEffect(controller) {
        onDispose { controller.close() }
    }

    DefaultExtensionStoreUi(
        state = state,
        dispatch = controller::dispatch,
        onImportTheme = onImportTheme,
        isThemeSaved = isThemeSaved,
    )
}
