package dev.fishpi.mobile.feature.home

import dev.fishpi.mobile.ui.components.FishPiPillButton
import dev.fishpi.mobile.ui.components.ControlSurface
import dev.fishpi.mobile.ui.components.uiPageBrush

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.ModeComment
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.data.HomeWorkSettings
import dev.fishpi.mobile.feature.home.model.HomeArticleUiModel
import java.util.Calendar
import java.util.Locale

@Composable
internal fun DefaultHomeUi(
    state: HomeState,
    dispatch: (HomeAction) -> Unit,
) {
    HomeSoftDefaultUi(state, dispatch)

    if (state.showWorkSettingsDialog) {
        HomeWorkSettingsDialog(
            draft = state.workSettingsDraft,
            onDismiss = { dispatch(HomeAction.DismissWorkSettings) },
            onStartTimeChange = { dispatch(HomeAction.ChangeWorkStartTime(it)) },
            onEndTimeChange = { dispatch(HomeAction.ChangeWorkEndTime(it)) },
            onWeekendModeChange = { dispatch(HomeAction.ChangeWeekendMode(it)) },
            onToggleCustomRestDay = { dispatch(HomeAction.ToggleCustomRestDay(it)) },
            onSave = { dispatch(HomeAction.SaveWorkSettings) },
        )
    }
}

@Composable
private fun HomeSoftDefaultUi(
    state: HomeState,
    dispatch: (HomeAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(uiPageBrush())
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = FishPiTheme.spacingPage, top = FishPiTheme.spacingControl, end = FishPiTheme.spacingPage, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingSection),
    ) {
        item {
            HomeSoftHeroScene(state = state, dispatch = dispatch)
        }
        item {
            HomeSoftStatusBand(state = state, dispatch = dispatch)
        }
        item {
            HomeSoftPlanBoard(state = state, dispatch = dispatch)
        }
        item {
            HomeSoftCommunityRail(state = state, dispatch = dispatch)
        }
    }
}

@Composable
private fun homeSoftContainerColor(alpha: Float = 0.94f): Color =
    MaterialTheme.colorScheme.surface.copy(alpha = alpha)

@Composable
private fun HomeSoftHeroScene(
    state: HomeState,
    dispatch: (HomeAction) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(118.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth(0.66f)
                .height(72.dp)
                .clip(RoundedCornerShape(topStart = 38.dp, topEnd = 12.dp, bottomEnd = 38.dp, bottomStart = 16.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 12.dp)
                .size(width = 82.dp, height = 34.dp)
                .clip(RoundedCornerShape(topStart = 999.dp, topEnd = 13.dp, bottomEnd = 999.dp, bottomStart = 13.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 4.dp, end = 118.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = homeDayGreeting(),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = state.displayName.ifBlank { "朋友" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.quoteText.ifBlank { "今天也轻一点，先从当前这一步开始。" },
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HomeDatePill(
            onClick = { dispatch(HomeAction.OpenWorkSettings) },
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

@Composable
private fun HomeSoftStatusBand(
    state: HomeState,
    dispatch: (HomeAction) -> Unit,
) {
    val liveness = state.activity?.liveness?.takeIf { it >= 0.0 }
    val progress = ((liveness ?: 0.0).coerceIn(0.0, 100.0) / 100.0).toFloat()
    val hoursLeft = homeHoursUntilOffWork(state.workSettings)
    val rewardValue = when (state.livenessRewarded) {
        true -> "已领"
        false -> if (state.isRewarding) "领取中" else "可领"
        null -> "同步"
    }
    val offWorkValue = when {
        hoursLeft == null -> "休息"
        hoursLeft <= 0f -> "完成"
        else -> String.format(Locale.US, "%.1fh", hoursLeft)
    }
    ControlSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(FishPiTheme.spacingSection),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            HomeSoftLivenessSummary(
                value = liveness?.let { formatLivenessValue(it) } ?: "...",
                hint = if (state.activity?.checkedIn == true) "已签到" else "同步中",
                progress = progress,
                modifier = Modifier.weight(1.25f),
                onClick = { dispatch(HomeAction.OpenLivenessHelp) },
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                HomeSoftStatusPill(
                    title = "奖励",
                    value = rewardValue,
                    hint = "昨日活跃",
                    icon = Icons.Rounded.EmojiEvents,
                    onClick = {
                        if (state.livenessRewarded == false && !state.isRewarding) {
                            dispatch(HomeAction.ClaimYesterdayLivenessReward)
                        }
                    },
                )
                HomeSoftStatusPill(
                    title = "下班",
                    value = offWorkValue,
                    hint = "工作节奏",
                    icon = Icons.Rounded.CalendarMonth,
                    onClick = { dispatch(HomeAction.OpenWorkSettings) },
                )
            }
        }
        }
    }
}

@Composable
private fun HomeSoftLivenessSummary(
    value: String,
    hint: String,
    progress: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            HomeSoftIconDot(icon = Icons.Rounded.LocalFireDepartment)
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "今日活跃",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = hint,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.62f),
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        )
    }
}

@Composable
private fun HomeSoftStatusPill(
    title: String,
    value: String,
    hint: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FishPiTheme.radiusField))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.48f))
            .padding(horizontal = 9.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeSoftIconDot(icon = icon)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(text = title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            Text(text = hint, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.66f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
        Text(text = value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun HomeSoftIconDot(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun HomeSoftPlanBoard(
    state: HomeState,
    dispatch: (HomeAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "常用入口",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        HomePrimaryEntry(
            title = "进入聊天室",
            icon = Icons.AutoMirrored.Rounded.Chat,
            onClick = { dispatch(HomeAction.OpenChat) },
        )
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
        ) {
            HomeSoftMiniAction("清风明月", Icons.Rounded.NightsStay) { dispatch(HomeAction.OpenBreezemoon) }
            HomeSoftMiniAction("扩展集市", Icons.Rounded.Storefront) { dispatch(HomeAction.OpenStore) }
        }
    }
}

@Composable
private fun HomePrimaryEntry(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 22.dp, bottomEnd = 12.dp, bottomStart = 22.dp))
            .background(homeSoftContainerColor(alpha = 0.94f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.13f),
                RoundedCornerShape(topStart = 12.dp, topEnd = 22.dp, bottomEnd = 12.dp, bottomStart = 22.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f), modifier = Modifier.size(19.dp))
        }
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.52f),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun HomeSoftMiniAction(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(topStart = 12.dp, topEnd = 18.dp, bottomEnd = 12.dp, bottomStart = 18.dp)
    Column(
        modifier = modifier
            .width(116.dp)
            .height(56.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
            .border(FishPiTheme.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.56f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun HomeSoftCommunityRail(
    state: HomeState,
    dispatch: (HomeAction) -> Unit,
) {
    val articles = state.recommendedArticles
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FishPiTheme.radiusBox))
            .background(homeSoftContainerColor(alpha = 0.90f))
            .border(FishPiTheme.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(FishPiTheme.radiusBox))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "推荐帖子",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "全部",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(FishPiTheme.radiusField * 0.66f))
                    .clickable { dispatch(HomeAction.OpenArticle) }
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
        when {
            articles.isNotEmpty() -> articles.forEachIndexed { index, article ->
                HomeSoftCommunityArticleRow(
                    article = article,
                    showDivider = index < articles.lastIndex,
                    onClick = { dispatch(HomeAction.OpenArticleDetail(article.id)) },
                )
            }
            state.isLoadingRecommended -> Text(
                text = "正在同步社区内容...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            else -> Text(
                text = state.recommendedError ?: "去看看新的讨论",
                color = if (state.recommendedError == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (articles.isNotEmpty()) {
            HomeSoftRecommendedFooter(state = state, dispatch = dispatch)
        }
    }
}

@Composable
private fun HomeSoftCommunityArticleRow(
    article: HomeArticleUiModel,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FishPiTheme.radiusField * 0.66f))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = article.title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = article.author.ifBlank { "鱼友" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(
                text = "评论 ${article.commentCount}",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
            )
        }
    }
}

@Composable
private fun HomeSoftRecommendedFooter(
    state: HomeState,
    dispatch: (HomeAction) -> Unit,
) {
    when {
        state.recommendedError != null -> Text(
            text = state.recommendedError,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 2.dp),
        )
        state.recommendedHasMore -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = if (state.isLoadingRecommendedMore) "加载中..." else "加载更多",
                color = if (state.isLoadingRecommendedMore) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(FishPiTheme.radiusField * 0.66f))
                    .clickable(enabled = !state.isLoadingRecommendedMore) {
                        dispatch(HomeAction.LoadMoreRecommended)
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
        else -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "暂时看到这里",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun HomeDatePill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(FishPiTheme.radiusBox))
            .background(homeSoftContainerColor(alpha = 0.88f))
            .border(FishPiTheme.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(FishPiTheme.radiusBox))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(Icons.Rounded.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.34f), modifier = Modifier.size(17.dp))
        Text(text = homeDateLabel(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}
