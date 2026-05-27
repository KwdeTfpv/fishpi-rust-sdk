package dev.fishpi.mobile.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.fishpi.mobile.AppSession
import dev.fishpi.mobile.FishPiNotifier

@Composable
internal fun HomeRoute(
    session: AppSession,
    noticeUnread: Long,
    onOpenChat: () -> Unit,
    onOpenArticle: () -> Unit,
    onOpenArticleDetail: (String) -> Unit,
    onOpenBreezemoon: () -> Unit,
    onOpenFun: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val context = LocalContext.current
    val controller = remember(session.apiKey, session.user.displayName) {
        HomeController(
            context = context.applicationContext,
            apiKey = session.apiKey,
            displayName = session.user.displayName,
            noticeUnread = noticeUnread,
        )
    }
    val state by controller.state.collectAsState()

    LaunchedEffect(controller) {
        controller.dispatch(HomeAction.Initialize)
    }

    LaunchedEffect(noticeUnread) {
        controller.dispatch(HomeAction.UpdateNoticeUnread(noticeUnread))
    }

    LaunchedEffect(controller) {
        controller.effects.collect { effect ->
            when (effect) {
                HomeEffect.NavigateToChat -> onOpenChat()
                HomeEffect.NavigateToArticle -> onOpenArticle()
                is HomeEffect.NavigateToArticleDetail -> onOpenArticleDetail(effect.articleId)
                HomeEffect.NavigateToBreezemoon -> onOpenBreezemoon()
                HomeEffect.NavigateToFun -> onOpenFun()
                HomeEffect.NavigateToProfile -> onOpenProfile()
                is HomeEffect.ShowMessage -> FishPiNotifier.success(effect.message)
                is HomeEffect.ShowError -> FishPiNotifier.error(effect.message)
            }
        }
    }

    DisposableEffect(controller) {
        onDispose {
            controller.close()
        }
    }

    DefaultHomeUi(
        state = state,
        dispatch = controller::dispatch,
    )
}
