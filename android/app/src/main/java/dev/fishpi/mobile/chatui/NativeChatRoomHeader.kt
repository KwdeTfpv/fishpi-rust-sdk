package dev.fishpi.mobile.chatui

import dev.fishpi.mobile.ui.components.fishPiChatWallpaper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.fishpi.mobile.FishPiTheme
import androidx.compose.material3.Text

@Composable
internal fun NativeChatRoomHeader(
    topic: String,
    onlineCount: Int,
    onQuoteTopic: () -> Unit,
    onRefresh: () -> Unit,
    onOpenPlugins: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val normalizedTopic = topic.trim()
    val hasTopic = normalizedTopic.isNotBlank() && normalizedTopic != "暂无"
    val topicText = if (hasTopic) "当前话题：#$normalizedTopic#" else "暂无话题"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .shadow(4.dp, RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(FishPiTheme.surface.copy(alpha = 0.94f))
            .border(1.dp, FishPiTheme.outline.copy(alpha = 0.34f), RoundedCornerShape(22.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(FishPiTheme.surfaceContainer.copy(alpha = 0.72f))
                .clickable(enabled = hasTopic, onClick = onQuoteTopic)
                .padding(horizontal = 12.dp, vertical = 7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = topicText,
                    color = if (hasTopic) FishPiTheme.onSurface else FishPiTheme.weakText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF32B56A)),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$onlineCount 在线",
                    color = FishPiTheme.weakText,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(36.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(FishPiTheme.surfaceContainer.copy(alpha = 0.78f))
                .clickable(onClick = onRefresh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "刷新聊天室",
                tint = FishPiTheme.weakText,
                modifier = Modifier.size(16.dp),
            )
        }

        Box(
            modifier = Modifier
                .padding(start = 6.dp)
                .size(36.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(FishPiTheme.surfaceContainer.copy(alpha = 0.78f))
                .clickable(onClick = onOpenPlugins),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Extension,
                contentDescription = "打开插件",
                tint = FishPiTheme.weakText,
                modifier = Modifier.size(16.dp),
            )
        }

    }
}

