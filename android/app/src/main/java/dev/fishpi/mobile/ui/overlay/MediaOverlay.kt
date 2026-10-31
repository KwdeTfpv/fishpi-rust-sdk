package dev.fishpi.mobile.ui.overlay

import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun MediaOverlayHost(content: @Composable () -> Unit) {
    val controller = remember { MediaOverlayController() }
    CompositionLocalProvider(LocalMediaOverlay provides controller) {
        content()
        val request = controller.request ?: return@CompositionLocalProvider
        Dialog(
            onDismissRequest = { controller.dismiss() },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            val view = LocalView.current
            val window = (view.parent as? DialogWindowProvider)?.window

            DisposableEffect(window) {
                val dialogWindow = window ?: return@DisposableEffect onDispose {}
                dialogWindow.setDimAmount(0f)
                dialogWindow.setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
                dialogWindow.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
                val insetsController = WindowCompat.getInsetsController(dialogWindow, dialogWindow.decorView)
                val previousLightStatusBars = insetsController.isAppearanceLightStatusBars
                insetsController.isAppearanceLightStatusBars = false
                onDispose {
                    insetsController.isAppearanceLightStatusBars = previousLightStatusBars
                }
            }

            key(request) {
                MediaOverlayScaffold(onDismiss = { controller.dismiss() }) {
                    when (request) {
                        is MediaOverlayRequest.Image -> ImageViewer(
                            request = request.request,
                            onDismiss = { controller.dismiss() },
                        )
                        is MediaOverlayRequest.Video -> VideoPlaybackOverlay(
                            url = request.url,
                            session = request.session,
                            onDismiss = { controller.dismiss() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaOverlayScaffold(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    BackHandler(onBack = onDismiss)
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
