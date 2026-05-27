package dev.fishpi.mobile.feature.chat.barrage.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.feature.chat.barrage.ChatBarrageComposerState
import dev.fishpi.mobile.ui.components.FishPiIconButton
import dev.fishpi.mobile.ui.components.FishPiPillButton

@Composable
internal fun ChatBarrageComposerDialog(
    state: ChatBarrageComposerState,
    onChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            shape = RoundedCornerShape(18.dp),
            color = FishPiTheme.surface,
            border = BorderStroke(1.dp, FishPiTheme.outline.copy(alpha = 0.16f)),
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "发送弹幕",
                            color = FishPiTheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (state.isLoadingCost) "正在读取弹幕花费..." else state.costLabel,
                            color = FishPiTheme.weakText,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    FishPiIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = "关闭弹幕输入",
                        onClick = onDismiss,
                        enabled = !state.isSending,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = FishPiTheme.surfaceContainer,
                    border = BorderStroke(1.dp, FishPiTheme.outline.copy(alpha = 0.14f)),
                    shadowElevation = 0.dp,
                ) {
                    BasicTextField(
                        value = state.input,
                        onValueChange = onChange,
                        enabled = !state.isSending,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        textStyle = TextStyle(
                            color = FishPiTheme.onSurface,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                        ),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSend() }),
                        decorationBox = { inner ->
                            if (state.input.isBlank()) {
                                Text(
                                    text = "biu~",
                                    color = FishPiTheme.weakText,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            inner()
                        },
                    )
                }
                state.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${state.input.length}/80",
                        color = FishPiTheme.weakText,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f),
                    )
                    FishPiPillButton(
                        text = if (state.isSending) "发送中" else "发弹幕",
                        enabled = !state.isSending && state.input.trim().isNotEmpty(),
                        onClick = onSend,
                        containerColor = FishPiTheme.accent,
                        contentColor = Color.White,
                    )
                }
            }
        }
    }
}
