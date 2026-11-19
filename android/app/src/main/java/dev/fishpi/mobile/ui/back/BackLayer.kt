package dev.fishpi.mobile.ui.back

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

internal class BackLayerController {
    internal val layers = mutableStateListOf<BackLayerEntry>()

    val hasEnabled: Boolean
        get() = layers.any { it.enabled }

    internal fun register(entry: BackLayerEntry) {
        if (entry !in layers) layers.add(entry)
    }

    internal fun unregister(entry: BackLayerEntry) {
        layers.remove(entry)
    }

    fun dismissTop(): Boolean {
        val top = layers
            .withIndex()
            .filter { it.value.enabled }
            .maxWithOrNull(
                compareBy<IndexedValue<BackLayerEntry>> { it.index },
            )
            ?.value
            ?: return false
        top.onBack()
        return true
    }
}

internal class BackLayerEntry(
    val key: Any,
    enabled: Boolean,
    initialOnBack: () -> Unit,
) {
    var enabled by mutableStateOf(enabled)
    var onBack by mutableStateOf(initialOnBack)
}

internal val LocalBackLayer = staticCompositionLocalOf<BackLayerController> {
    error("BackLayerController 未提供，请在上层包裹 BackLayerHost")
}

@Composable
internal fun BackLayer(
    key: Any,
    enabled: Boolean,
    onBack: () -> Unit,
) {
    val controller = LocalBackLayer.current
    val entry = remember(controller, key) {
        BackLayerEntry(key = key, enabled = enabled, initialOnBack = onBack)
    }
    DisposableEffect(controller, entry) {
        controller.register(entry)
        onDispose { controller.unregister(entry) }
    }
    SideEffect {
        entry.enabled = enabled
        entry.onBack = onBack
    }
}

@Composable
internal fun BackLayerHost(content: @Composable () -> Unit) {
    val controller = remember { BackLayerController() }
    CompositionLocalProvider(LocalBackLayer provides controller) {
        content()
    }
    BackHandler(enabled = controller.hasEnabled) {
        controller.dismissTop()
    }
}
