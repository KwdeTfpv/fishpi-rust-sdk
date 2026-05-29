package dev.fishpi.mobile.feature.redpacket

import androidx.compose.runtime.Composable

@Composable
internal fun DefaultRedPacketResultUi(
    state: RedPacketResultUiModel,
    dispatch: (RedPacketAction) -> Unit,
) {
    RedPacketResultPage(
        message = state.message,
        count = state.count,
        got = state.got,
        gesture = state.gesture,
        who = state.who.map { it.toLegacyRedPacketGot() },
        senderName = state.senderName,
        senderAvatar = state.senderAvatar,
        packetMessage = state.packetMessage,
        selfUsername = state.selfUsername,
        finished = state.finished,
        onDismiss = { dispatch(RedPacketAction.DismissResult) },
    )
}
