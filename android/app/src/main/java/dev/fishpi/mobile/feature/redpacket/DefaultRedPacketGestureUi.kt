package dev.fishpi.mobile.feature.redpacket

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun DefaultRedPacketGestureUi(
    selectedGesture: Int?,
    dispatch: (RedPacketAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { dispatch(RedPacketAction.Dismiss) },
        title = { Text("猜拳领取红包") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("选择你的出拳")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RedPacketGestureOptions.forEach { option ->
                        FilterChip(
                            selected = selectedGesture == option.value,
                            onClick = { dispatch(RedPacketAction.GesturePicked(option.value)) },
                            label = { Text("${option.emoji} ${option.label}") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = { dispatch(RedPacketAction.Dismiss) },
                modifier = Modifier.padding(end = 4.dp),
            ) { Text("取消") }
        },
    )
}
