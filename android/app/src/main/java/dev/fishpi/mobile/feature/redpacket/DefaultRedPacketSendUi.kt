package dev.fishpi.mobile.feature.redpacket

import androidx.compose.runtime.Composable

@Composable
internal fun DefaultRedPacketSendUi(
    form: RedPacketFormState,
    dispatch: (RedPacketAction) -> Unit,
) {
    RedPacketSendDialog(
        type = form.type,
        money = form.money,
        count = form.count,
        message = form.message,
        receivers = form.receivers,
        gesture = form.gesture,
        balance = form.balance,
        isSending = form.isSending,
        onTypeChange = { dispatch(RedPacketAction.TypeChanged(it)) },
        onMoneyChange = { dispatch(RedPacketAction.MoneyChanged(it)) },
        onCountChange = { dispatch(RedPacketAction.CountChanged(it)) },
        onMessageChange = { dispatch(RedPacketAction.MessageChanged(it)) },
        onReceiversChange = { dispatch(RedPacketAction.ReceiversChanged(it)) },
        onGestureChange = { dispatch(RedPacketAction.GesturePicked(it)) },
        onDismiss = { dispatch(RedPacketAction.Dismiss) },
        onSend = { dispatch(RedPacketAction.SendClicked) },
    )
}
