package dev.fishpi.mobile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.fishpi.mobile.data.FishPiApiClient
import dev.fishpi.mobile.data.NoticeItemView
import dev.fishpi.mobile.data.NoticeUnreadCount
import dev.fishpi.mobile.utils.NoticeDestination
import dev.fishpi.mobile.utils.NoticePresentationCategory
import dev.fishpi.mobile.utils.noticeCategoryLabel
import dev.fishpi.mobile.utils.noticeDisplayTitle
import dev.fishpi.mobile.utils.noticePresentationCategory
import dev.fishpi.mobile.utils.noticePrimaryDestination
import dev.fishpi.mobile.utils.noticeSummaryText
import dev.fishpi.mobile.utils.noticeTimeLabel
import dev.fishpi.mobile.ui.components.ActionChipButton
import dev.fishpi.mobile.ui.components.ContentCardSurface
import dev.fishpi.mobile.ui.components.ControlSurface
import dev.fishpi.mobile.ui.components.FishPiBrandLoadingLogo
import dev.fishpi.mobile.ui.components.IconActionButton
import dev.fishpi.mobile.ui.components.LoadingScreen
import dev.fishpi.mobile.ui.components.PlainBackButton
import dev.fishpi.mobile.ui.components.UiLayerScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class NoticeCategory(
    val label: String,
    val icon: ImageVector,
    val presentation: NoticePresentationCategory,
) {
    All("全部", Icons.Rounded.Notifications, NoticePresentationCategory.All),
    Reply("回复", Icons.Rounded.ChatBubble, NoticePresentationCategory.Reply),
    AtMe("@我", Icons.Rounded.AlternateEmail, NoticePresentationCategory.AtMe),
    Points("积分", Icons.Rounded.Star, NoticePresentationCategory.Points),
    System("系统", Icons.Rounded.Campaign, NoticePresentationCategory.System),
    Follow("关注", Icons.Rounded.Person, NoticePresentationCategory.Follow),
}

private data class NoticeUiItem(
    val raw: NoticeItemView,
    val displayTitle: String,
    val summary: String,
    val categoryLabel: String,
    val timeText: String,
)

@Composable
internal fun NoticeScreen(
    session: AppSession,
    unread: Long,
    onUnreadChange: (Long) -> Unit,
    onDismiss: () -> Unit,
    onJumpToChatRoom: (String?) -> Unit,
    onJumpToArticle: (String) -> Unit,
) {
    val api = remember { FishPiApiClient.shared }
    val scope = rememberCoroutineScope()
    var notices by remember { mutableStateOf<List<NoticeItemView>>(emptyList()) }
    var count by remember { mutableStateOf<NoticeUnreadCount?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("通知中心") }
    var selectedCategory by remember { mutableStateOf(NoticeCategory.All) }

    fun refresh() {
        val hasLoaded = notices.isNotEmpty()
        if (!hasLoaded) {
            isLoading = true
        }
        error = null
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val nextNotices = api.getNotices(session.apiKey)
                    withContext(Dispatchers.Main) {
                        notices = nextNotices
                        status = "已加载 ${nextNotices.size} 条通知，正在同步未读数..."
                    }
                    val nextCount = api.getNoticeUnreadCount(session.apiKey)
                    nextCount to nextNotices
                }
            }.onSuccess { (nextCount, nextNotices) ->
                count = nextCount
                onUnreadChange(nextCount.total)
                status = "已同步 ${nextNotices.size} 条通知"
            }.onFailure {
                error = it.message ?: "加载通知失败"
            }
            isLoading = false
        }
    }

    LaunchedEffect(session.apiKey) {
        refresh()
    }

    val filteredNotices = remember(notices, selectedCategory) {
        if (selectedCategory == NoticeCategory.All) notices
        else notices.filter { it.noticePresentationCategory() == selectedCategory.presentation }
    }
    val uiNotices = remember(filteredNotices) {
        filteredNotices.map { item ->
            NoticeUiItem(
                raw = item,
                displayTitle = item.noticeDisplayTitle(),
                summary = item.noticeSummaryText(),
                categoryLabel = item.noticeCategoryLabel(),
                timeText = item.noticeTimeLabel(),
            )
        }
    }

    val categoryUnreadCounts = remember(notices) {
        NoticeCategory.entries.associateWith { cat ->
            if (cat == NoticeCategory.All) notices.count { !it.read }.toLong()
            else notices.count { !it.read && it.noticePresentationCategory() == cat.presentation }.toLong()
        }
    }

    UiLayerScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            ControlSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
            PlainBackButton(onClick = onDismiss, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "通知",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Text(
                    text = "未读 ${count?.total ?: unread} · $status",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconActionButton(
                icon = Icons.Rounded.Refresh,
                contentDescription = "刷新通知",
                onClick = { refresh() },
            )
            ActionChipButton(
                text = "已读",
                selected = true,
                onClick = {
                        scope.launch {
                            runCatching {
                                withContext(Dispatchers.IO) { api.markAllNoticesRead(session.apiKey) }
                            }.onSuccess {
                                onUnreadChange(0)
                                count = count?.copy(total = 0)
                                notices = notices.map { it.copy(read = true) }
                                status = "已全部标记为已读"
                            }.onFailure {
                                error = it.message ?: "全部已读失败"
                            }
                        }
                },
            )
        }
        }

        NoticeCategoryTabs(
            selected = selectedCategory,
            unreadCounts = categoryUnreadCounts,
            onSelect = { selectedCategory = it },
        )

        when {
            isLoading && notices.isEmpty() -> LoadingScreen("加载通知...")
            error != null && notices.isEmpty() -> NoticeStateMessage(error ?: "加载通知失败", "请稍后刷新重试", isError = true)
            notices.isEmpty() -> NoticeStateMessage("暂无通知", "新的回复、积分和系统消息会显示在这里")
            filteredNotices.isEmpty() -> NoticeStateMessage("该分类暂无通知", "切换到全部可以查看其它通知")
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    error?.let { message ->
                        item(key = "notice-inline-error") {
                            NoticeInlineError(message)
                        }
                    }
                    itemsIndexed(
                        uiNotices,
                        key = { _, it -> it.raw.id.ifBlank { it.raw.time + it.raw.title } },
                    ) { _, item ->
                        NoticeCard(
                            item = item,
                            onJumpToChatRoom = onJumpToChatRoom,
                            onJumpToArticle = onJumpToArticle,
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun NoticeCategoryTabs(
    selected: NoticeCategory,
    unreadCounts: Map<NoticeCategory, Long>,
    onSelect: (NoticeCategory) -> Unit,
) {
    ControlSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            NoticeCategory.entries.forEach { category ->
                val isSelected = category == selected
                val unread = unreadCounts[category] ?: 0L

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface,
                        )
                        .clickable { onSelect(category) }
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = category.label,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        if (unread > 0 && !isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .offset(x = 2.dp, y = (-1).dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                                    .align(Alignment.TopEnd),
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = category.label,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun NoticeStateMessage(
    title: String,
    subtitle: String,
    isError: Boolean = false,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        ContentCardSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 22.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FishPiBrandLoadingLogo()
                Text(
                    text = title,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun NoticeInlineError(message: String) {
    ContentCardSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun NoticeCard(
    item: NoticeUiItem,
    onJumpToChatRoom: (String?) -> Unit,
    onJumpToArticle: (String) -> Unit,
) {
    val raw = item.raw

    ContentCardSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.displayTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (!raw.read) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error),
                    )
                }
            }
            if (item.summary.isNotBlank()) {
                Text(
                    text = item.summary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.categoryLabel,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                )
                if (!raw.author.isNullOrBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = raw.author,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.weight(1f))
                val destination = raw.noticePrimaryDestination()
                when (destination) {
                    NoticeDestination.ChatRoom -> NoticeActionChip(
                        icon = Icons.Rounded.ChatBubble,
                        text = "去聊天室",
                        onClick = { onJumpToChatRoom(raw.jumpId.ifBlank { null }) },
                    )
                    NoticeDestination.Article -> {
                        if (raw.jumpId.isNotBlank()) {
                            NoticeActionChip(
                                icon = Icons.AutoMirrored.Rounded.Article,
                                text = "帖子 #${raw.jumpId}",
                                onClick = { onJumpToArticle(raw.jumpId) },
                            )
                        }
                    }
                    NoticeDestination.None -> Unit
                }
                if (destination != NoticeDestination.None) {
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = item.timeText,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun NoticeActionChip(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

