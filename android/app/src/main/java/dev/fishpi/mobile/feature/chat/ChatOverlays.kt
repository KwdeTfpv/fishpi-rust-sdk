package dev.fishpi.mobile.feature.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.fishpi.mobile.FishPiErrorRed
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.LocalAppSession
import dev.fishpi.mobile.LocalFishPiPalette
import dev.fishpi.mobile.data.ChatFilterConfig
import dev.fishpi.mobile.data.ChatOnlineUser
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.data.EmojiGroupView
import dev.fishpi.mobile.data.FishPiUser
import dev.fishpi.mobile.data.MedalView
import dev.fishpi.mobile.feature.chat.render.blockedChatItems
import dev.fishpi.mobile.shared.message.native.MessageActionAnchor
import dev.fishpi.mobile.shared.message.native.NativeMessageList
import dev.fishpi.mobile.ui.bottomBarContentPadding
import dev.fishpi.mobile.ui.components.FishPiAvatar
import dev.fishpi.mobile.ui.components.FishPiFullScreenDialog
import dev.fishpi.mobile.ui.components.FishPiIconButton
import dev.fishpi.mobile.ui.components.FishPiPillButton
import dev.fishpi.mobile.ui.components.FishPiSurfaceKind
import dev.fishpi.mobile.ui.components.FishPiUserSummaryCard
import dev.fishpi.mobile.ui.components.ErrorState
import dev.fishpi.mobile.ui.components.LoadingScreen
import dev.fishpi.mobile.ui.components.TextField
import dev.fishpi.mobile.ui.components.fishPiSurface
import dev.fishpi.mobile.ui.components.rememberUiPageBrush
import dev.fishpi.mobile.ui.glass.FishPiGlassRole
import dev.fishpi.mobile.ui.glass.LocalFishPiGlassBackdrop
import dev.fishpi.mobile.ui.glass.fishPiGlassSurface
import dev.fishpi.mobile.ui.theme.FishPiType

private fun List<String>.withUniqueRule(rule: String, exactCase: Boolean = false): List<String> {
    val value = rule.trim()
    if (value.isBlank()) {
        return this
    }
    val exists = if (exactCase) {
        any { it == value }
    } else {
        any { it.equals(value, ignoreCase = true) }
    }
    return if (exists) this else this + value
}

@Composable
internal fun ChatFilterSettingsOverlay(
    config: ChatFilterConfig,
    onSave: (ChatFilterConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    var userInput by remember { mutableStateOf("") }
    var keywordInput by remember { mutableStateOf("") }
    var prefixInput by remember { mutableStateOf("") }
    var regexInput by remember { mutableStateOf("") }
    var regexError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(rememberUiPageBrush()),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = FishPiTheme.spacingPage,
                end = FishPiTheme.spacingPage,
                top = FishPiTheme.spacingPage,
                bottom = bottomBarContentPadding(extra = FishPiTheme.spacingPage),
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "聊天室过滤",
                        color = FishPiTheme.accent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    FishPiIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = "关闭过滤设置",
                        onClick = onDismiss,
                    )
                }
            }
            item {
                ChatAvatarSettingRow(
                    showAvatars = config.showAvatars,
                    onToggle = { onSave(config.copy(showAvatars = !config.showAvatars)) },
                )
            }
            item {
                ChatSendOnEnterSettingRow(
                    sendOnEnter = config.sendOnEnter,
                    onToggle = { onSave(config.copy(sendOnEnter = !config.sendOnEnter)) },
                )
            }
            item {
                FilterRuleEditor(
                    title = "屏蔽用户",
                    hint = "输入用户名",
                    value = userInput,
                    rules = config.blockedUsers,
                    onValueChange = { userInput = it },
                    onAdd = {
                        val next = config.copy(blockedUsers = config.blockedUsers.withUniqueRule(userInput))
                        userInput = ""
                        onSave(next)
                    },
                    onRemove = { rule ->
                        onSave(config.copy(blockedUsers = config.blockedUsers.filterNot { it == rule }))
                    },
                )
            }
            item {
                FilterRuleEditor(
                    title = "关键词屏蔽",
                    hint = "输入关键词",
                    value = keywordInput,
                    rules = config.blockedKeywords,
                    onValueChange = { keywordInput = it },
                    onAdd = {
                        val next = config.copy(blockedKeywords = config.blockedKeywords.withUniqueRule(keywordInput))
                        keywordInput = ""
                        onSave(next)
                    },
                    onRemove = { rule ->
                        onSave(config.copy(blockedKeywords = config.blockedKeywords.filterNot { it == rule }))
                    },
                )
            }
            item {
                FilterRuleEditor(
                    title = "开头关键词屏蔽",
                    hint = "输入开头关键词",
                    value = prefixInput,
                    rules = config.blockedPrefixKeywords,
                    onValueChange = { prefixInput = it },
                    onAdd = {
                        val next = config.copy(
                            blockedPrefixKeywords = config.blockedPrefixKeywords.withUniqueRule(prefixInput),
                        )
                        prefixInput = ""
                        onSave(next)
                    },
                    onRemove = { rule ->
                        onSave(config.copy(blockedPrefixKeywords = config.blockedPrefixKeywords.filterNot { it == rule }))
                    },
                )
            }
            item {
                FilterRuleEditor(
                    title = "正则屏蔽",
                    hint = "输入正则，如 (?i)广告|推广",
                    value = regexInput,
                    rules = config.blockedRegex,
                    onValueChange = {
                        regexInput = it
                        regexError = null
                    },
                    onAdd = {
                        val raw = regexInput.trim()
                        val compiled = runCatching { Regex(raw) }
                        if (raw.isBlank()) {
                            return@FilterRuleEditor
                        }
                        if (compiled.isFailure) {
                            regexError = "正则无效：${compiled.exceptionOrNull()?.message.orEmpty()}"
                            return@FilterRuleEditor
                        }
                        val next = config.copy(blockedRegex = config.blockedRegex.withUniqueRule(raw, exactCase = true))
                        regexInput = ""
                        regexError = null
                        onSave(next)
                    },
                    onRemove = { rule ->
                        onSave(config.copy(blockedRegex = config.blockedRegex.filterNot { it == rule }))
                    },
                )
                regexError?.let {
                    Text(text = it, color = FishPiErrorRed, modifier = Modifier.padding(top = 6.dp))
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(FishPiTheme.radiusBox))
                        .background(FishPiErrorRed.copy(alpha = 0.08f))
                        .clickable {
                            onSave(ChatFilterConfig(
                                blockedUsers = emptyList(),
                                blockedKeywords = emptyList(),
                                blockedPrefixKeywords = emptyList(),
                                blockedRegex = emptyList(),
                                showAvatars = config.showAvatars,
                                sendOnEnter = config.sendOnEnter,
                            ))
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = FishPiErrorRed,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "清空全部过滤规则",
                        color = FishPiErrorRed,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fishPiSurface(FishPiSurfaceKind.Card, stroked = false)
                        .clickable {
                            onSave(ChatFilterConfig(
                                showAvatars = config.showAvatars,
                                sendOnEnter = config.sendOnEnter,
                            ))
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = FishPiTheme.weakText,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "恢复默认过滤规则",
                        color = FishPiTheme.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatSendOnEnterSettingRow(
    sendOnEnter: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fishPiSurface(FishPiSurfaceKind.Card, stroked = false)
            .clickable(onClick = onToggle)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "回车发送", color = FishPiTheme.accent, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (sendOnEnter) "键盘发送键或实体回车会直接发送" else "回车保留为换行，只点发送按钮发送",
                color = FishPiTheme.onSurface.copy(alpha = 0.58f),
            )
        }
        CapsuleSwitch(checked = sendOnEnter)
    }
}

@Composable
private fun ChatAvatarSettingRow(
    showAvatars: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fishPiSurface(FishPiSurfaceKind.Card, stroked = false)
            .clickable(onClick = onToggle)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "显示聊天室头像", color = FishPiTheme.accent, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (showAvatars) "消息左侧显示发送者头像" else "隐藏头像，保留紧凑消息布局",
                color = FishPiTheme.onSurface.copy(alpha = 0.58f),
            )
        }
        CapsuleSwitch(checked = showAvatars)
    }
}

@Composable
private fun CapsuleSwitch(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 58.dp, height = 32.dp)
            .clip(RoundedCornerShape(FishPiTheme.radiusSelector))
            .background(if (checked) FishPiTheme.accent else FishPiTheme.surface),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(24.dp)
                .clip(RoundedCornerShape(FishPiTheme.radiusSelector))
                .background(rememberUiPageBrush()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (checked) "开" else "关",
                color = if (checked) FishPiTheme.accent else FishPiTheme.onSurface.copy(alpha = 0.58f),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun FilterRuleEditor(
    title: String,
    hint: String,
    value: String,
    rules: List<String>,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fishPiSurface(FishPiSurfaceKind.Card, stroked = false)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = title, color = FishPiTheme.accent, fontWeight = FontWeight.SemiBold, fontSize = FishPiType.Size.heading)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                label = hint,
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(FishPiTheme.radiusBox))
                    .background(if (value.isNotBlank()) FishPiTheme.accent.copy(alpha = 0.12f) else FishPiTheme.surface)
                    .clickable(enabled = value.isNotBlank()) { onAdd() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "添加",
                    tint = if (value.isNotBlank()) FishPiTheme.accent else FishPiTheme.weakText,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        if (rules.isEmpty()) {
            Text(text = "暂无规则", color = FishPiTheme.weakText, fontSize = FishPiType.Size.secondary)
        } else {
            rules.forEach { rule ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fishPiSurface(FishPiSurfaceKind.Field, stroked = false)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = rule, modifier = Modifier.weight(1f), color = FishPiTheme.onSurface)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { onRemove(rule) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "删除",
                            tint = FishPiErrorRed,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun BlockedMessagesOverlay(
    messages: List<ChatRoomMessage>,
    selfUsername: String,
    showAvatars: Boolean,
    onImageClick: (images: List<String>, index: Int) -> Unit,
    onLinkClick: (String) -> Unit,
    onAvatarClick: (String) -> Unit,
    onAvatarLongPress: (String) -> Unit,
    onRedPacketClick: (ChatRoomMessage) -> Unit,
    onReactionClick: (ChatRoomMessage, String) -> Unit,
    onLongPress: (MessageActionAnchor) -> Unit,
    onDismiss: () -> Unit,
) {
    val items = remember(messages) { blockedChatItems(messages) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(rememberUiPageBrush())
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FishPiTheme.surface)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "已屏蔽消息",
                    color = FishPiTheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = FishPiType.Size.title,
                )
                Text(
                    text = if (messages.isEmpty()) "暂无被屏蔽消息" else "${messages.size} 条消息被当前规则隐藏",
                    color = FishPiTheme.weakText,
                    fontSize = FishPiType.Size.caption,
                )
            }
            FishPiIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = "关闭已屏蔽消息",
                onClick = onDismiss,
            )
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "暂无被屏蔽消息", color = FishPiTheme.weakText)
            }
        } else {
            NativeMessageList(
                items = items,
                selfUsername = selfUsername,
                showAvatars = showAvatars,
                scrollToBottomRequest = 0,
                allowScrollToBottom = false,
                redPacketJumpTargetId = null,
                drawBackground = false,
                onLoadMore = {},
                onNearBottomChanged = {},
                onImageClick = onImageClick,
                onLinkClick = onLinkClick,
                onLongPress = onLongPress,
                onAvatarClick = onAvatarClick,
                onAvatarLongPress = onAvatarLongPress,
                onRedPacketClick = onRedPacketClick,
                onReactionClick = onReactionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
}

@Composable
internal fun ChatUserProfileOverlay(
    username: String,
    user: FishPiUser?,
    medals: List<MedalView>,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(rememberUiPageBrush()),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "用户资料",
                    color = FishPiTheme.accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                FishPiIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "关闭用户资料",
                    onClick = onDismiss,
                )
            }

            when {
                isLoading -> LoadingScreen("加载 @$username 的资料...")
                error != null -> ErrorState(message = error, onRetry = onRetry)
                user == null -> ErrorState(message = "没有读取到 @$username 的资料", onRetry = onRetry)
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        FishPiUserSummaryCard(
                            user = user,
                            medals = medals,
                            apiKey = LocalAppSession.current.apiKey,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatOnlineUsersPage(
    onlineCount: Int,
    users: List<ChatOnlineUser>,
    onDismiss: () -> Unit,
    onOpenUser: (String) -> Unit,
) {
    FishPiFullScreenDialog(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(rememberUiPageBrush())
                .navigationBarsPadding(),
        ) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FishPiTheme.surface,
                    titleContentColor = FishPiTheme.onSurface,
                    actionIconContentColor = FishPiTheme.onSurface,
                ),
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "在线用户",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text(
                            text = "$onlineCount 人在线",
                            color = FishPiTheme.weakText,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭")
                    }
                },
            )
            if (users.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "正在等待在线列表同步",
                        color = FishPiTheme.weakText,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    itemsIndexed(users) { _, user ->
                        ChatOnlineUserRow(
                            user = user,
                            onClick = { onOpenUser(user.userName) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatOnlineUserRow(
    user: ChatOnlineUser,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FishPiTheme.radiusBox))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ChatOnlineAvatar(user = user)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = user.userName,
                color = FishPiTheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "在线",
                color = FishPiTheme.weakText,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ChatOnlineAvatar(user: ChatOnlineUser) {
    FishPiAvatar(
        avatarUrl = user.avatarUrl,
        displayName = user.userName,
        contentDescription = "${user.userName}头像",
        size = 40.dp,
        fallbackBackground = FishPiTheme.surfaceContainer,
        fallbackContentColor = FishPiTheme.weakText,
    )
}

@Composable
internal fun ChatTopicEditorDialog(
    value: String,
    currentTopic: String,
    onChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val trimmed = value.trim()
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .fishPiGlassSurface(
                    backdrop = LocalFishPiGlassBackdrop.current,
                    shape = RoundedCornerShape(FishPiTheme.radiusField),
                    containerColor = FishPiTheme.surface.copy(alpha = 0.82f),
                    borderColor = FishPiTheme.outline.copy(alpha = 0.18f),
                    borderWidth = FishPiTheme.borderWidth,
                    role = FishPiGlassRole.Floating,
                ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "更改话题",
                            color = FishPiTheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "修改话题需要16积分，将自动从账户中扣除；\n最大长度64字符，不合法字符将被自动过滤",
                            color = FishPiTheme.weakText,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    FishPiIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = "关闭话题输入",
                        onClick = onDismiss,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(FishPiTheme.radiusBox + 4.dp),
                    color = FishPiTheme.surfaceContainer,
                    border = BorderStroke(FishPiTheme.borderWidth, FishPiTheme.outline.copy(alpha = 0.14f)),
                    shadowElevation = 0.dp,
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = onChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        textStyle = TextStyle(
                            color = FishPiTheme.onSurface,
                            fontSize = FishPiType.Size.heading,
                            lineHeight = 20.sp,
                        ),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (trimmed.isNotBlank()) onConfirm()
                        }),
                        decorationBox = { inner ->
                            if (value.isBlank()) {
                                Text(
                                    text = "输入新的聊天室话题",
                                    color = FishPiTheme.weakText,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            inner()
                        },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${value.length}/64",
                        color = FishPiTheme.weakText,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f),
                    )
                    FishPiPillButton(
                        text = "保存话题",
                        enabled = trimmed.isNotBlank(),
                        onClick = onConfirm,
                        containerColor = FishPiTheme.accent,
                        contentColor = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
internal fun FavoriteEmojiSelectorSheet(
    url: String,
    groups: List<EmojiGroupView>,
    isLoading: Boolean,
    onGroupSelected: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var emojiName by remember { mutableStateOf("") }
    val palette = LocalFishPiPalette.current

    FishPiFullScreenDialog(
        onDismissRequest = onDismiss,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fishPiGlassSurface(
                    backdrop = LocalFishPiGlassBackdrop.current,
                    shape = RoundedCornerShape(FishPiTheme.radiusBox),
                    containerColor = FishPiTheme.surface.copy(alpha = 0.78f),
                    borderColor = FishPiTheme.outline.copy(alpha = 0.18f),
                    borderWidth = FishPiTheme.borderWidth,
                    role = FishPiGlassRole.Floating,
                ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 标题
                Text(
                    text = "收藏表情",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                // 表情名称输入框
                Text(
                    text = "表情名称",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                BasicTextField(
                    value = emojiName,
                    onValueChange = { emojiName = it.take(32) },
                    enabled = !isLoading,
                    textStyle = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(palette.surface)
                        .border(1.dp, palette.outline.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                        .defaultMinSize(minHeight = 32.dp),
                    decorationBox = { innerTextField ->
                        if (emojiName.isEmpty()) {
                            Text(
                                text = "输入表情名称...",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.outline.copy(alpha = 0.5f),
                            )
                        }
                        innerTextField()
                    },
                )

                // 分组标题
                Text(
                    text = "选择分组",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.onSurface,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )

                // 分组列表
                if (groups.isEmpty()) {
                    Text(
                        text = "暂无表情分组",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.weakText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        groups.forEach { group ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(
                                        enabled = !isLoading,
                                        onClick = {
                                            val finalName = emojiName.trim().takeIf { it.isNotBlank() } ?: group.name
                                            onGroupSelected(group.id, finalName)
                                            onDismiss()
                                        },
                                    ),
                                color = palette.surfaceContainer,
                            ) {
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isLoading) palette.weakText else palette.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                )
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDismiss),
                    color = palette.surfaceContainer,
                ) {
                    Text(
                        text = "取消",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
