package dev.fishpi.mobile.ui.overlay

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dev.fishpi.mobile.FishPiNotificationHost
import dev.fishpi.mobile.ui.back.BackLayer
import dev.fishpi.mobile.ui.back.BackLayerHost

internal sealed interface MediaOverlayRequest {
    data class Image(val request: ImageViewerRequest) : MediaOverlayRequest
    data class Video(val url: String, val session: PlaybackSession?) : MediaOverlayRequest
}

internal class MediaOverlayController {
    var request by mutableStateOf<MediaOverlayRequest?>(null)
        private set

    fun open(images: List<String>, index: Int) {
        val cleaned = images.filter { it.isNotBlank() }
        if (cleaned.isEmpty()) return
        request = MediaOverlayRequest.Image(
            ImageViewerRequest(
                images = cleaned,
                startIndex = index.coerceIn(0, cleaned.lastIndex),
            ),
        )
    }

    fun open(url: String) {
        if (url.isBlank()) return
        request = MediaOverlayRequest.Image(ImageViewerRequest(listOf(url), 0))
    }

    fun openVideo(session: PlaybackSession, url: String) {
        if (url.isBlank()) return
        request = MediaOverlayRequest.Video(url, session)
    }

    fun openVideo(url: String) {
        if (url.isBlank()) return
        request = MediaOverlayRequest.Video(url, null)
    }

    fun dismiss() {
        request = null
    }
}

internal val LocalMediaOverlay = staticCompositionLocalOf<MediaOverlayController> {
    error("MediaOverlayController 未提供，请在上层包裹 MediaOverlayHost")
}

@Composable
internal fun MediaOverlayHost(content: @Composable () -> Unit) {
    val controller = remember { MediaOverlayController() }
    BackLayerHost {
        CompositionLocalProvider(LocalMediaOverlay provides controller) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
                MediaOverlaySurface(controller)
                FishPiNotificationHost()
            }
        }
    }
}

@Composable
private fun MediaOverlaySurface(controller: MediaOverlayController) {
    val request = controller.request ?: return
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }

    BackLayer(
        key = "media-overlay",
        priority = 1000,
        enabled = true,
        onBack = controller::dismiss,
    )

    DisposableEffect(request) {
        val window = view.context.findActivity()?.window
            ?: return@DisposableEffect onDispose {}
        val insetsController = WindowCompat.getInsetsController(window, view)
        val previousLightStatusBars = insetsController.isAppearanceLightStatusBars
        insetsController.isAppearanceLightStatusBars = false
        onDispose {
            insetsController.isAppearanceLightStatusBars = previousLightStatusBars
        }
    }

    key(request) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) {},
        ) {
            MediaOverlayScaffold {
                when (request) {
                    is MediaOverlayRequest.Image -> ImageViewer(
                        request = request.request,
                        onDismiss = controller::dismiss,
                    )

                    is MediaOverlayRequest.Video -> VideoPlaybackOverlay(
                        url = request.url,
                        session = request.session,
                        onDismiss = controller::dismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaOverlayScaffold(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            content()
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (true) {
        when (current) {
            is Activity -> return current
            is ContextWrapper -> current = current.baseContext
            else -> return null
        }
    }
}
