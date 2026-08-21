package dev.fishpi.mobile.feature.profile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.fishpi.mobile.data.NotificationPrefs
import dev.fishpi.mobile.data.SessionStore
import dev.fishpi.mobile.realtime.RealtimeLifecycleController
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.ui.components.FishPiIconButton
import dev.fishpi.mobile.ui.components.FishPiSettingsRow
import dev.fishpi.mobile.ui.components.FishPiSettingsSection
import dev.fishpi.mobile.ui.components.uiPageBrush

@Composable
internal fun NotificationSettingsScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SessionStore(context.applicationContext) }
    var prefs by remember { mutableStateOf(store.getNotificationPrefs()) }

    fun update(next: NotificationPrefs, keepAliveChanged: Boolean = false) {
        prefs = next
        store.saveNotificationPrefs(next)
        if (keepAliveChanged) {
            RealtimeLifecycleController.onKeepAlivePrefChanged()
        }
    }

    // 授权结果回调：无论是否授权都启动保活（前台服务本身不依赖通知权限，仅通知展示依赖）
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        update(prefs.copy(keepAlive = true), keepAliveChanged = true)
    }

    fun enableKeepAlive() {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            update(prefs.copy(keepAlive = true), keepAliveChanged = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(uiPageBrush()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "通知",
                color = FishPiTheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.weight(1f),
            )
            FishPiIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = "关闭",
                onClick = onDismiss,
                background = FishPiTheme.surfaceContainer,
                iconSizeDp = 18,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                FishPiSettingsSection(title = "后台保活") {
                    ToggleRow(
                        title = "保持后台在线",
                        summary = "常驻通知维持长连接，可在后台接收消息",
                        checked = prefs.keepAlive,
                        onCheckedChange = {
                            if (it) enableKeepAlive()
                            else update(prefs.copy(keepAlive = false), keepAliveChanged = true)
                        },
                    )
                }
            }
            item {
                FishPiSettingsSection(title = "聊天室") {
                    ToggleRow(
                        title = "有人 @我 时通知",
                        summary = "被提到时弹出系统通知",
                        checked = prefs.chatMentions,
                        onCheckedChange = { update(prefs.copy(chatMentions = it)) },
                    )
                    ToggleRow(
                        title = "关键词通知",
                        summary = "仅命中关键词的聊天室消息才通知",
                        checked = prefs.chatKeywordsOnly,
                        onCheckedChange = { update(prefs.copy(chatKeywordsOnly = it)) },
                    )
                    if (prefs.chatKeywordsOnly) {
                        KeywordEditor(
                            keywords = prefs.chatKeywords,
                            onChange = { update(prefs.copy(chatKeywords = it)) },
                        )
                    }
                }
            }
            item {
                FishPiSettingsSection(title = "其他") {
                    ToggleRow(
                        title = "私聊消息",
                        summary = "收到私信时通知",
                        checked = prefs.privateMessages,
                        onCheckedChange = { update(prefs.copy(privateMessages = it)) },
                    )
                    ToggleRow(
                        title = "系统通知",
                        summary = "回复、关注、积分等通知",
                        checked = prefs.notices,
                        onCheckedChange = { update(prefs.copy(notices = it)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    FishPiSettingsRow(
        icon = { Icon(imageVector = Icons.Rounded.Notifications, contentDescription = null, tint = FishPiTheme.accent) },
        title = title,
        summary = summary,
        trailing = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
    )
}

@Composable
private fun KeywordEditor(
    keywords: List<String>,
    onChange: (List<String>) -> Unit,
) {
    var text by remember(keywords) { mutableStateOf(keywords.joinToString("，")) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            val parsed = it.split("，", ",", "\n")
                .map { kw -> kw.trim() }
                .filter { kw -> kw.isNotBlank() }
                .distinct()
            onChange(parsed)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        label = { Text("关键词（逗号分隔，/.../ 为正则）") },
        keyboardActions = KeyboardActions.Default,
        singleLine = false,
    )
}
