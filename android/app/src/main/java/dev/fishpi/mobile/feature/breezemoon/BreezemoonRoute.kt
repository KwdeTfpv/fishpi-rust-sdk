package dev.fishpi.mobile.feature.breezemoon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.fishpi.mobile.AppSession
import dev.fishpi.mobile.FishPiNotifier
import dev.fishpi.mobile.ui.media.rememberChatAttachmentPicker

@Composable
internal fun BreezemoonRoute(
    session: AppSession,
    active: Boolean,
) {
    val context = LocalContext.current
    val controller = remember(session.apiKey) {
        BreezemoonController(context = context, apiKey = session.apiKey)
    }
    val state by controller.state.collectAsState()
    val attachmentPicker = rememberChatAttachmentPicker(
        onPicked = { uri, isVideo -> controller.dispatch(BreezemoonAction.PickAttachment(uri, isVideo)) },
        onError = { controller.dispatch(BreezemoonAction.SetError(it)) },
    )

    LaunchedEffect(session.apiKey) {
        controller.dispatch(BreezemoonAction.Initialize)
    }

    LaunchedEffect(controller) {
        controller.effect.collect { effect ->
            when (effect) {
                is BreezemoonEffect.ShowMessage -> FishPiNotifier.success(effect.message)
                is BreezemoonEffect.ShowError -> FishPiNotifier.error(effect.message)
                BreezemoonEffect.OpenGalleryPicker -> attachmentPicker.openGallery()
                BreezemoonEffect.OpenCameraPicker -> attachmentPicker.openCamera()
            }
        }
    }

    DisposableEffect(controller) {
        onDispose { controller.close() }
    }

    DefaultBreezemoonUi(
        state = state,
        active = active,
        dispatch = controller::dispatch,
    )
}
