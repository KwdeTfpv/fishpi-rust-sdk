package dev.fishpi.mobile.feature.profile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import dev.fishpi.mobile.realtime.RealtimeNotifier
import dev.fishpi.mobile.FishPiNotifier
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.ui.components.FishPiIconButton
import dev.fishpi.mobile.ui.components.FishPiSettingsRow
import dev.fishpi.mobile.ui.components.FishPiSettingsSection
import dev.fishpi.mobile.ui.components.uiPageBrush
import dev.fishpi.mobile.ui.bottomBarContentPadding

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
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 8.dp,
                bottom = bottomBarContentPadding(),
            ),
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
                    FishPiSettingsRow(
                        icon = { Icon(imageVector = Icons.Rounded.Notifications, contentDescription = null, tint = FishPiTheme.accent) },
                        title = "忽略电池优化",
                        summary = "允许后台常驻，避免系统休眠时收不到消息",
                        onClick = { context.requestIgnoreBatteryOptimizations() },
                    )
                }
            }
            item {
                FishPiSettingsSection(title = "系统通知设置") {
                    FishPiSettingsRow(
                        icon = { Icon(imageVector = Icons.Rounded.Notifications, contentDescription = null, tint = FishPiTheme.accent) },
                        title = "打开系统消息通知设置",
                        summary = "检查消息通知的声音、震动、横幅或悬浮通知",
                        onClick = {
                            RealtimeNotifier.ensureChannels(context)
                            context.openMessageNotificationSettings()
                        },
                    )
                    FishPiSettingsRow(
                        icon = { Icon(imageVector = Icons.Rounded.Notifications, contentDescription = null, tint = FishPiTheme.accent) },
                        title = "打开后台保活通知设置",
                        summary = "检查常驻通知是否被设成高优先级或非静默",
                        onClick = {
                            RealtimeNotifier.ensureChannels(context)
                            context.openKeepAliveNotificationSettings()
                        },
                    )
                    FishPiSettingsRow(
                        icon = { Icon(imageVector = Icons.Rounded.Notifications, contentDescription = null, tint = FishPiTheme.accent) },
                        title = "修复通知渠道",
                        summary = "重建消息和保活渠道，恢复默认提醒方式",
                        onClick = {
                            RealtimeNotifier.resetChannels(context)
                            RealtimeLifecycleController.onKeepAlivePrefChanged()
                            FishPiNotifier.success("已修复通知渠道")
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

private fun Context.openMessageNotificationSettings() {
    openNotificationChannelSettings(RealtimeNotifier.CHANNEL_MESSAGES)
}

private fun Context.isIgnoringBatteryOptimizations(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    val power = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
    return power.isIgnoringBatteryOptimizations(packageName)
}

private fun Context.requestIgnoreBatteryOptimizations() {
    val appContext = applicationContext
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isIgnoringBatteryOptimizations()) {
        val prompt = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { appContext.startActivity(prompt) }.isSuccess) {
            return
        }
    }
    runCatching {
        appContext.startActivity(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.onFailure {
        appContext.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private fun Context.openKeepAliveNotificationSettings() {
    openNotificationChannelSettings(RealtimeNotifier.CHANNEL_KEEPALIVE)
}

private fun Context.openNotificationChannelSettings(channelId: String) {
    val appContext = applicationContext
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    runCatching {
        appContext.startActivity(intent)
    }.onFailure {
        appContext.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
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
