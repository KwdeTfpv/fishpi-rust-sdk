package dev.fishpi.mobile.feature.privatechat

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.fishpi.mobile.AppSession
import dev.fishpi.mobile.FishPiNotifier
import dev.fishpi.mobile.data.PrivateChatNotice

@Composable
internal fun PrivateChatRoute(
    session: AppSession,
    realtimeEnabled: Boolean,
    active: Boolean,
    jumpPeer: String? = null,
    jumpRequest: Int = 0,
    externalNotice: PrivateChatNotice? = null,
    externalNoticeVersion: Int = 0,
    onDetailActiveChange: (Boolean) -> Unit = {},
    onUnreadChange: (Long) -> Unit = {},
    onNoticeHandled: () -> Unit = {},
    listHeader: @Composable (ColumnScope.() -> Unit)? = null,
) {
    val controller = remember(session.apiKey, session.user.userName) {
        PrivateChatController(
            apiKey = session.apiKey,
            selfUsername = session.user.userName,
        )
    }
    val state by controller.state.collectAsState()

    LaunchedEffect(Unit) {
        controller.dispatch(PrivateChatAction.RefreshSessions)
    }

    LaunchedEffect(jumpRequest, jumpPeer, active) {
        val peer = jumpPeer?.trim().orEmpty()
        if (active && peer.isNotBlank()) {
            controller.dispatch(PrivateChatAction.OpenPeer(peer))
        }
    }

    LaunchedEffect(externalNoticeVersion, externalNotice) {
        externalNotice?.let {
            controller.dispatch(PrivateChatAction.ApplyNotice(it, notify = false))
            onNoticeHandled()
        }
    }

    LaunchedEffect(controller) {
        controller.effects.collect { effect ->
            when (effect) {
                is PrivateChatEffect.DetailActiveChanged -> onDetailActiveChange(effect.active)
                is PrivateChatEffect.NotifyPrivateMessage -> {
                    val preview = effect.notice.preview.ifBlank { "收到一条私聊消息" }
                    FishPiNotifier.show("${effect.notice.peer}: $preview", avatarUrl = effect.notice.avatar)
                }
                is PrivateChatEffect.ShowError -> FishPiNotifier.error(effect.message)
                is PrivateChatEffect.TotalUnreadChanged -> onUnreadChange(effect.unread)
            }
        }
    }

    DisposableEffect(controller, realtimeEnabled) {
        controller.connectOverview(realtimeEnabled)
        onDispose {
            controller.connectOverview(false)
        }
    }

    DisposableEffect(controller, realtimeEnabled, state.conversation.selectedPeer) {
        controller.connectConversation(realtimeEnabled)
        onDispose {
            controller.connectConversation(false)
        }
    }

    ProvidePrivateChatUiEnvironment(
        environment = PrivateChatUiEnvironment(
            session = session,
            active = active,
            listHeader = listHeader,
        ),
    ) {
        DefaultPrivateChatUi(
            state = state,
            dispatch = controller::dispatch,
        )
    }
}
