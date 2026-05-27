package dev.fishpi.mobile.feature.article

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.fishpi.mobile.AppSession
import dev.fishpi.mobile.FishPiNotifier
import dev.fishpi.mobile.ui.overlay.ImagePreviewOverlay
import dev.fishpi.mobile.ui.overlay.LinkPreviewOverlay
import dev.fishpi.mobile.ui.overlay.VideoPlaybackOverlay
import dev.fishpi.mobile.feature.article.model.ArticleOverlayState
import dev.fishpi.mobile.feature.article.publish.ArticlePublishAction
import dev.fishpi.mobile.feature.article.publish.ArticlePublishController
import dev.fishpi.mobile.feature.article.publish.ArticlePublishEffect
import dev.fishpi.mobile.ui.media.rememberChatAttachmentPicker

@Composable
internal fun ArticleRoute(
    session: AppSession,
    active: Boolean,
    jumpArticleId: String? = null,
    jumpRequest: Int = 0,
    onDetailClosed: () -> Unit = {},
    onOpenUserProfile: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val controller = remember(session.apiKey) { ArticleController(apiKey = session.apiKey) }
    val publishController = remember(session.apiKey) { ArticlePublishController(apiKey = session.apiKey) }
    val state by controller.state.collectAsState()
    val publishState by publishController.state.collectAsState()
    var handledJumpRequest by remember { mutableStateOf(-1) }

    val commentAttachmentPicker = rememberChatAttachmentPicker(
        onPickedPath = { path -> controller.dispatch(ArticleAction.UploadCommentImage(path)) },
        onError = { message -> controller.dispatch(ArticleAction.ShowPickerError(message)) },
    )
    val publishContentImagePicker = rememberChatAttachmentPicker(
        onPickedPath = { path -> publishController.dispatch(ArticlePublishAction.UploadContentImage(path)) },
        onError = { message -> publishController.dispatch(ArticlePublishAction.ShowPickerError(message)) },
    )
    val publishRewardImagePicker = rememberChatAttachmentPicker(
        onPickedPath = { path -> publishController.dispatch(ArticlePublishAction.UploadRewardImage(path)) },
        onError = { message -> publishController.dispatch(ArticlePublishAction.ShowPickerError(message)) },
    )

    LaunchedEffect(Unit) {
        controller.dispatch(ArticleAction.RefreshList)
    }

    LaunchedEffect(active, jumpRequest) {
        val id = jumpArticleId
        if (active && !id.isNullOrBlank() && jumpRequest != handledJumpRequest) {
            handledJumpRequest = jumpRequest
            controller.dispatch(ArticleAction.OpenArticleById(id))
        }
    }

    LaunchedEffect(controller) {
        controller.effects.collect { effect ->
            when (effect) {
                ArticleEffect.DetailClosed -> onDetailClosed()
                is ArticleEffect.OpenUserProfile -> onOpenUserProfile(effect.username)
                ArticleEffect.OpenCommentGallery -> commentAttachmentPicker.openGallery()
                ArticleEffect.OpenCommentCamera -> commentAttachmentPicker.openCamera()
                is ArticleEffect.ShareArticle -> shareArticleLink(context, effect.title, effect.articleId)
                is ArticleEffect.ShowMessage -> FishPiNotifier.success(effect.message)
                is ArticleEffect.ShowError -> FishPiNotifier.error(effect.message)
            }
        }
    }

    LaunchedEffect(publishController) {
        publishController.effects.collect { effect ->
            when (effect) {
                ArticlePublishEffect.Closed -> controller.dispatch(ArticleAction.ClosePublish)
                ArticlePublishEffect.OpenContentImagePicker -> publishContentImagePicker.openGallery()
                ArticlePublishEffect.OpenRewardImagePicker -> publishRewardImagePicker.openGallery()
                is ArticlePublishEffect.Published -> controller.dispatch(ArticleAction.PublishCompleted(effect.articleId))
                is ArticlePublishEffect.ShowMessage -> FishPiNotifier.success(effect.message)
                is ArticlePublishEffect.ShowError -> FishPiNotifier.error(effect.message)
            }
        }
    }

    LaunchedEffect(state.publishOpen) {
        if (state.publishOpen) {
            publishController.dispatch(ArticlePublishAction.Initialize)
        }
    }

    DisposableEffect(active, state.selected?.id, state.publishOpen) {
        controller.connectArticleRealtime(active && !state.publishOpen)
        onDispose {
            controller.connectArticleRealtime(false)
        }
    }

    DisposableEffect(controller, publishController) {
        onDispose {
            controller.close()
            publishController.close()
        }
    }

    if (state.publishOpen) {
        DefaultArticlePublishUi(
            state = publishState,
            dispatch = publishController::dispatch,
        )
    } else {
        DefaultArticleUi(
            state = state,
            dispatch = controller::dispatch,
        )
    }

    (state.overlay as? ArticleOverlayState.Image)?.let { (url) ->
        ImagePreviewOverlay(imageUrl = url, onDismiss = { controller.dispatch(ArticleAction.DismissOverlay) })
    }
    (state.overlay as? ArticleOverlayState.Link)?.let { (url) ->
        LinkPreviewOverlay(url = url, apiKey = session.apiKey, onDismiss = { controller.dispatch(ArticleAction.DismissOverlay) })
    }
    (state.overlay as? ArticleOverlayState.Video)?.let { (url) ->
        VideoPlaybackOverlay(url = url, onDismiss = { controller.dispatch(ArticleAction.DismissOverlay) })
    }

    BackHandler(enabled = active && state.overlay != ArticleOverlayState.None) {
        controller.dispatch(ArticleAction.DismissOverlay)
    }

    BackHandler(enabled = active && state.publishOpen) {
        controller.dispatch(ArticleAction.ClosePublish)
    }
}

private fun shareArticleLink(context: Context, title: String, articleId: String) {
    val url = "https://fishpi.cn/article/$articleId"
    val text = buildString {
        if (title.isNotBlank()) {
            append(title)
            append('\n')
        }
        append(url)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title.ifBlank { "摸鱼派帖子" })
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "分享帖子"))
}
