package dev.fishpi.mobile.feature.home

import dev.fishpi.mobile.ui.components.FishPiPillButton
import dev.fishpi.mobile.FishPiTheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.ModeComment
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import dev.fishpi.mobile.data.HomeWorkSettings
import dev.fishpi.mobile.data.UserActivityView
import dev.fishpi.mobile.feature.home.model.HomeArticleUiModel
import dev.fishpi.mobile.feature.home.model.HomeWorkSettingsDraft
import dev.fishpi.mobile.feature.home.model.homeTimeMinutes
import dev.fishpi.mobile.feature.home.model.isHomeTimeText
import dev.fishpi.mobile.feature.home.model.toHomeTimeParts
import dev.fishpi.mobile.rememberFishPiImageLoader
import java.util.Calendar
import java.util.Locale



@Composable
internal fun HomeActivityCard(
    activity: UserActivityView?,
    onOpenLivenessHelp: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val liveness = activity?.liveness?.takeIf { it >= 0.0 }
    val livenessValue = liveness ?: 0.0
    val progress = (livenessValue.coerceIn(0.0, 100.0) / 100f).toFloat()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FishPiTheme.radiusBox + 8.dp))
            .background(homeIslandSurfaceColor())
            .border(FishPiTheme.borderWidth, homeIslandBorderColor(), RoundedCornerShape(FishPiTheme.radiusBox + 8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = colors.primary)
            Text(
                text = "活跃度",
                color = colors.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 6.dp),
            )
            if (activity?.checkedIn == true) {
                Text(
                    text = "今日已签到",
                    color = colors.primary,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clip(CircleShape)
                        .background(colors.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = liveness?.let { "${formatLivenessValue(it)} / 100" } ?: "... / 100",
                color = colors.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = colors.primary,
            trackColor = colors.surfaceContainerHighest,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                Icons.AutoMirrored.Rounded.HelpOutline,
                contentDescription = "查看活跃度说明",
                tint = colors.onSurfaceVariant,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenLivenessHelp)
                    .padding(5.dp),
            )
            Text(
                text = homeLivenessHint(liveness),
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

internal fun homeLivenessHint(liveness: Double?): String {
    val value = liveness ?: return "活跃度接口 10 分钟内只能刷新一次，稍后会自动更新；签到和昨日奖励状态会单独同步。"
    return when {
        value <= 0.0 -> "今天还没怎么活跃哦，可以先去聊天室聊几句，或者看看帖子、点个赞热热身。"
        value < 15.0 -> "现在活跃度还比较低，聊天室发言、查看帖子、评论和点赞都能慢慢涨起来。"
        value < 45.0 -> "今天已经开始活跃啦，继续评论几篇帖子，感谢或打赏喜欢的内容，会更快接近奖励。"
        value < 70.0 -> "活跃度不错了！如果有认真内容想分享，发一篇有营养的帖子会提升很多。"
        value < 100.0 -> "离满活跃不远啦，再互动一会儿，说不定今天就能冲到 100%。"
        else -> "今天活跃度已经满啦！记得明天回来领取昨日活跃奖励，今天可以安心摸鱼了。"
    }
}


internal fun formatLivenessValue(value: Double): String {
    val normalized = value.coerceAtLeast(0.0)
    return if (normalized % 1.0 == 0.0) {
        normalized.toLong().toString()
    } else {
        "%.2f".format(Locale.US, normalized).trimEnd('0').trimEnd('.')
    }
}


@Composable
internal fun HomeRewardCard(
    livenessRewarded: Boolean,
    isRewarding: Boolean,
    onClaim: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FishPiTheme.radiusBox + 8.dp))
            .background(homeRewardGradient())
            .border(FishPiTheme.borderWidth, homeIslandBorderColor(), RoundedCornerShape(FishPiTheme.radiusBox + 8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = colors.onSecondaryContainer)
            Text(text = "昨日活跃奖励", color = colors.onSecondaryContainer, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Text(
            text = if (livenessRewarded) "昨日活跃奖励已领取，明天再来看看。" else "恭喜您昨日活跃度达标，可领取奖励！",
            color = colors.onSecondaryContainer,
            fontWeight = FontWeight.SemiBold,
        )
        if (!livenessRewarded) {
            FishPiPillButton(
                text = if (isRewarding) "领取中..." else "领取奖励",
                onClick = onClaim,
                enabled = !isRewarding,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}



