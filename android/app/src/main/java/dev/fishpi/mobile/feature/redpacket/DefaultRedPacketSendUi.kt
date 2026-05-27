package dev.fishpi.mobile.feature.redpacket

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

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

@Composable
private fun RedPacketSendHeader(
    isSending: Boolean,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        RedPacketAccent,
                        RedPacketAccentDark,
                    ),
                )
            )
            .padding(horizontal = 18.dp, vertical = 22.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("发红包", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("积分包裹好啦，选个姿势发出去", color = Color.White.copy(alpha = 0.82f))
        }
        TextButton(
            onClick = onDismiss,
            enabled = !isSending,
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Text("关闭", color = Color.White)
        }
    }
}

private val RedPacketAccent = Color(0xFFE94E2F)
private val RedPacketAccentDark = Color(0xFFC92F22)

private val redPacketTypeOptions = listOf(
    RedPacketTypeRandom to "拼手气",
    RedPacketTypeAverage to "平分",
    RedPacketTypeSpecify to "专属",
    RedPacketTypeHeartbeat to "心跳",
    RedPacketTypeRockPaperScissors to "猜拳",
)
