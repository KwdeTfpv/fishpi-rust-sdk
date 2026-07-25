package dev.fishpi.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.feature.chat.model.ChatMentionCandidateUiModel

/**
 * @ 提及候选人弹窗：竖向列表，每行圆头像 + 用户名。
 */
@Composable
internal fun MentionCandidatePopup(
    candidates: List<ChatMentionCandidateUiModel>,
    onPick: (ChatMentionCandidateUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (candidates.isEmpty()) {
        return
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FishPiTheme.radiusField),
        color = FishPiTheme.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(FishPiTheme.borderWidth, FishPiTheme.outline),
        shadowElevation = 0.dp,
    ) {
        Column {
            candidates.forEach { candidate ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(candidate) }
                        .padding(horizontal = FishPiTheme.spacingControl, vertical = FishPiTheme.spacingItem),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem),
                ) {
                    FishPiAvatar(
                        avatarUrl = candidate.avatarUrl,
                        displayName = candidate.displayName,
                        contentDescription = candidate.username,
                        size = 32.dp,
                    )
                    Text(
                        text = "@${candidate.displayName}",
                        color = FishPiTheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
