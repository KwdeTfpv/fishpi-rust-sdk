package dev.fishpi.mobile.feature.redpacket

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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

@Composable
private fun RedPacketResultHeader(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        RedPacketAccent,
                        RedPacketAccentDark,
                    ),
                ),
            )
            .padding(horizontal = 18.dp, vertical = 22.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("红包详情", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("领取记录", color = Color.White.copy(alpha = 0.82f))
        }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Text("关闭", color = Color.White)
        }
    }
}

private val RedPacketAccent = Color(0xFFE94E2F)
private val RedPacketAccentDark = Color(0xFFC92F22)
