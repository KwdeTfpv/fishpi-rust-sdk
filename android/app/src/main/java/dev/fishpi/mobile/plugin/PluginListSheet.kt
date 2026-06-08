package dev.fishpi.mobile.plugin

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.fishpi.mobile.FishPiNotifier
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.ui.components.FishPiIconButton
import dev.fishpi.mobile.ui.components.consumeTaps
import dev.fishpi.mobile.ui.components.silentTap
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginListSheet(onDismiss: () -> Unit) {
    val pm = remember { PluginManager.get() }
    var plugins by remember { mutableStateOf(pm.pluginInfos()) }
    var detailPlugin by remember { mutableStateOf<PluginInfo?>(null) }
    var settingsPlugin by remember { mutableStateOf<PluginInfo?>(null) }
    var editorPlugin by remember { mutableStateOf<PluginInfo?>(null) }
    var deleteConfirm by remember { mutableStateOf<PluginInfo?>(null) }
    var safetyConfirmPlugin by remember { mutableStateOf<PluginInfo?>(null) }
    val installLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            pm.installPluginFromUri(uri)
        }.onSuccess { fileName ->
            plugins = pm.pluginInfos()
            FishPiNotifier.success("已导入: $fileName，启用前需要确认安全风险")
        }.onFailure { e ->
            FishPiNotifier.error("安装失败: ${e.message ?: "未知错误"}")
        }
    }

    // Delete confirmation
    deleteConfirm?.let { plugin ->
        AlertDialog(
            onDismissRequest = { deleteConfirm = null },
            title = { Text("卸载插件") },
            text = { Text("确定要卸载「${plugin.name}」？") },
            confirmButton = {
                TextButton(onClick = {
                    pm.uninstallPlugin(plugin.fileName)
                    plugins = pm.pluginInfos()
                    deleteConfirm = null
                    FishPiNotifier.success("「${plugin.name}」已卸载")
                }) { Text("卸载", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirm = null }) { Text("取消") }
            },
        )
    }

    safetyConfirmPlugin?.let { plugin ->
        AlertDialog(
            onDismissRequest = { safetyConfirmPlugin = null },
            title = { Text("加载本地插件？") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("「${plugin.name}」不是从扩展集市安装，或文件内容已变化。")
                    Text(
                        "插件可以读取当前登录 API Key，并调用 App 暴露的能力。请确认来源可信后再启用。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pm.approvePluginForCurrentContent(plugin.fileName)
                    pm.togglePlugin(plugin.fileName, enable = true)
                    plugins = pm.pluginInfos()
                    safetyConfirmPlugin = null
                    FishPiNotifier.success("「${plugin.name}」已启用")
                }) { Text("确认启用") }
            },
            dismissButton = {
                TextButton(onClick = { safetyConfirmPlugin = null }) { Text("取消") }
            },
        )
    }

    // Detail dialog
    detailPlugin?.let { plugin ->
        AlertDialog(
            onDismissRequest = { detailPlugin = null },
            title = { Text(plugin.name) },
            text = {
                val state = pm.getState(plugin.fileName)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("v${plugin.version} · ${plugin.author.ifBlank { "未知" }} · ${plugin.source.label()}", fontSize = 12.sp)
                    Text("状态: ${state.status}", fontSize = 12.sp,
                        color = if (state.status == PluginStatus.Error) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.errors.isNotEmpty()) {
                        Text("错误日志:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        state.errors.takeLast(5).forEach { err ->
                            Text(err, fontSize = 12.sp, color = MaterialTheme.colorScheme.error,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (state.recentCalls.isNotEmpty()) {
                        Text("最近调用:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        state.recentCalls.takeLast(5).forEach { call ->
                            val symbol = if (call.ok) "✓" else "✗"
                            Text("$symbol ${call.method} ${call.durationMs}ms",
                                fontSize = 12.sp,
                                color = if (call.ok) MaterialTheme.colorScheme.onSurfaceVariant
                                        else MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { detailPlugin = null }) { Text("关闭") } },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = FishPiTheme.surface,
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        val errorCount = plugins.count { pm.getState(it.fileName).status == PluginStatus.Error }
                        Text("插件管理", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = FishPiTheme.onSurface)
                        Text(
                            text = "${plugins.size} 个插件" + if (errorCount > 0) " · $errorCount 个异常" else "",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                        )
                    }
                    TextButton(
                        onClick = { installLauncher.launch(arrayOf("text/*", "application/javascript")) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Icon(Icons.Rounded.FileOpen, "安装", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("安装")
                    }
                    FishPiIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = "关闭插件管理",
                        onClick = onDismiss,
                        sizeDp = 34,
                        iconSizeDp = 18,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(top = 10.dp, bottom = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(pm.pluginDirectoryPath(), color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp, modifier = Modifier.weight(1f))
                }

                if (plugins.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("暂无插件", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        Text("将 .js 文件放入应用插件目录\n或点击下方按钮选择文件安装",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 12.sp)
                        TextButton(
                            onClick = { installLauncher.launch(arrayOf("text/*", "application/javascript")) },
                        ) {
                            Icon(Icons.Rounded.FileOpen, "安装", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("选择 .js 文件", fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 10.dp),
                    ) {
                        items(plugins, key = { it.fileName }) { plugin ->
                            val state = pm.getState(plugin.fileName)
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                modifier = Modifier.clickable { detailPlugin = plugin },
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(plugin.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                                if (!plugin.enabled) Text(" (已禁用)", fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.error)
                                                else if (state.status == PluginStatus.Error) Text(" (异常)", fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.error)
                                            }
                                            Text("v${plugin.version} · ${plugin.author.ifBlank { "未知" }} · ${plugin.source.label()}",
                                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            state.lastError?.let { err ->
                                                Text(err, fontSize = 12.sp, color = MaterialTheme.colorScheme.error,
                                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        Switch(checked = plugin.enabled, onCheckedChange = { enable ->
                                            if (enable && pm.requiresSafetyConfirmation(plugin.fileName)) {
                                                safetyConfirmPlugin = plugin
                                            } else {
                                                pm.togglePlugin(plugin.fileName, enable)
                                                plugins = pm.pluginInfos()
                                            }
                                        })
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        IconButton(onClick = { editorPlugin = plugin },
                                            modifier = Modifier.size(48.dp)) {
                                            Icon(Icons.Rounded.Edit, "编辑",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                                modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(onClick = { settingsPlugin = plugin },
                                            modifier = Modifier.size(48.dp)) {
                                            Icon(Icons.Rounded.Settings, "设置",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(onClick = { deleteConfirm = plugin },
                                            modifier = Modifier.size(48.dp)) {
                                            Icon(Icons.Rounded.Delete, "卸载",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                                modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        settingsPlugin?.let { plugin ->
            PluginSettingsPanel(
                plugin = plugin,
                manager = pm,
                onDismiss = { settingsPlugin = null },
                onMessage = { text -> FishPiNotifier.success(text) },
            )
        }

        editorPlugin?.let { plugin ->
            PluginSourceEditorScreen(
                plugin = plugin,
                manager = pm,
                onDismiss = { editorPlugin = null },
                onSaved = { plugins = pm.pluginInfos() },
            )
        }
    }
}

private fun JSONArray.jsonValues(): List<String> {
    return buildList {
        for (i in 0 until length()) {
            val value = opt(i)
            add(value?.toString() ?: "")
        }
    }
}

private fun PluginSource.label(): String = when (this) {
    PluginSource.Store -> "扩展集市"
    PluginSource.Local -> "本地导入"
    PluginSource.Unknown -> "未知来源"
}

@Composable
private fun PluginSettingsPanel(
    plugin: PluginInfo,
    manager: PluginManager,
    onDismiss: () -> Unit,
    onMessage: (String) -> Unit,
) {
    var entries by remember(plugin.fileName) {
        mutableStateOf(manager.getPluginStorage(plugin.fileName).toList().sortedBy { it.first })
    }
    var deleteKeyConfirm by remember(plugin.fileName) { mutableStateOf<String?>(null) }
    var addExpanded by remember(plugin.fileName) { mutableStateOf(false) }
    var newKey by remember(plugin.fileName) { mutableStateOf("") }
    var newValue by remember(plugin.fileName) { mutableStateOf("") }

    fun reload() {
        entries = manager.getPluginStorage(plugin.fileName).toList().sortedBy { it.first }
    }

    BackHandler(onBack = onDismiss)

    deleteKeyConfirm?.let { key ->
        AlertDialog(
            onDismissRequest = { deleteKeyConfirm = null },
            title = { Text("删除设置项") },
            text = { Text("确定删除「$key」？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        manager.removePluginStorage(plugin.fileName, key)
                        reload()
                        deleteKeyConfirm = null
                        onMessage("已删除 $key")
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteKeyConfirm = null }) { Text("取消") }
            },
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28f))
            .silentTap(onDismiss)
            .padding(horizontal = 14.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .heightIn(max = maxHeight * 0.82f)
                .clip(RoundedCornerShape(18.dp))
                .background(FishPiTheme.surface)
                .consumeTaps(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = plugin.name,
                        color = FishPiTheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "插件设置",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
                FishPiIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "关闭插件设置",
                    onClick = onDismiss,
                    sizeDp = 34,
                    iconSizeDp = 18,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (entries.isEmpty()) {
                    item {
                        Text(
                            text = "暂无设置项",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                        )
                    }
                }
                items(entries, key = { it.first }) { (key, value) ->
                    PluginSettingRow(
                        plugin = plugin,
                        manager = manager,
                        entryKey = key,
                        storedValue = value,
                        onDelete = { deleteKeyConfirm = key },
                        onSaved = {
                            reload()
                            onMessage("已保存 $key")
                        },
                    )
                }
                item {
                    PluginAddSettingSection(
                        expanded = addExpanded,
                        newKey = newKey,
                        newValue = newValue,
                        onExpandedChange = { addExpanded = it },
                        onKeyChange = { newKey = it },
                        onValueChange = { newValue = it },
                        onAdd = {
                            manager.setPluginStorage(plugin.fileName, newKey.trim(), newValue)
                            newKey = ""
                            newValue = ""
                            addExpanded = false
                            reload()
                            onMessage("已添加设置项")
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PluginSettingRow(
    plugin: PluginInfo,
    manager: PluginManager,
    entryKey: String,
    storedValue: String,
    onDelete: () -> Unit,
    onSaved: () -> Unit,
) {
    val parsed = remember(entryKey, storedValue) { parsePluginSettingValue(storedValue) }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = entryKey,
                        color = FishPiTheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = pluginSettingTypeName(parsed),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "删除设置项",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.52f),
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
            when (parsed) {
                is JSONArray -> PluginArraySettingEditor(
                    plugin = plugin,
                    manager = manager,
                    entryKey = entryKey,
                    initialItems = parsed.jsonValues(),
                    onSaved = onSaved,
                )
                is Number -> PluginNumberSettingEditor(
                    plugin = plugin,
                    manager = manager,
                    entryKey = entryKey,
                    initialValue = parsed,
                    onSaved = onSaved,
                )
                else -> PluginStringSettingEditor(
                    plugin = plugin,
                    manager = manager,
                    entryKey = entryKey,
                    initialValue = parsed.toString(),
                    onSaved = onSaved,
                )
            }
        }
    }
}

@Composable
private fun PluginStringSettingEditor(
    plugin: PluginInfo,
    manager: PluginManager,
    entryKey: String,
    initialValue: String,
    onSaved: () -> Unit,
) {
    var text by remember(entryKey, initialValue) { mutableStateOf(initialValue) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CompactPluginTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = "文本",
            modifier = Modifier.weight(1f),
        )
        CompactSaveButton(
            enabled = text != initialValue,
            onClick = {
                manager.setPluginStorage(plugin.fileName, entryKey, JSONObject.quote(text))
                onSaved()
            },
        )
    }
}

@Composable
private fun PluginNumberSettingEditor(
    plugin: PluginInfo,
    manager: PluginManager,
    entryKey: String,
    initialValue: Number,
    onSaved: () -> Unit,
) {
    var text by remember(entryKey, initialValue) { mutableStateOf(initialValue.toDouble().toCompactNumberString()) }
    var error by remember(entryKey) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CompactPluginTextField(
                value = text,
                onValueChange = {
                    text = it
                    error = false
                },
                placeholder = "数字",
                modifier = Modifier.weight(1f),
            )
            CompactSaveButton(
                enabled = text != initialValue.toDouble().toCompactNumberString(),
                onClick = {
                    val value = text.toDoubleOrNull()
                    if (value == null) {
                        error = true
                        return@CompactSaveButton
                    }
                    manager.setPluginStorage(plugin.fileName, entryKey, value.toCompactNumberString())
                    onSaved()
                },
            )
        }
        if (error) {
            Text("请输入有效数字", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
        }
    }
}

@Composable
private fun PluginArraySettingEditor(
    plugin: PluginInfo,
    manager: PluginManager,
    entryKey: String,
    initialItems: List<String>,
    onSaved: () -> Unit,
) {
    var items by remember(entryKey, initialItems) { mutableStateOf(initialItems) }
    var addItem by remember(entryKey) { mutableStateOf("") }

    fun save(next: List<String>) {
        items = next
        manager.setPluginStorage(plugin.fileName, entryKey, JSONArray(next).toString())
        onSaved()
    }

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        if (items.isEmpty()) {
            Text("空数组", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items.forEachIndexed { index, item ->
                    PluginArrayToken(
                        text = item,
                        onRemove = { save(items.toMutableList().also { it.removeAt(index) }) },
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CompactPluginTextField(
                value = addItem,
                onValueChange = { addItem = it },
                placeholder = "新增项",
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = {
                    val item = addItem.trim()
                    if (item.isNotEmpty()) {
                        save(items + item)
                        addItem = ""
                    }
                },
                enabled = addItem.isNotBlank(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp),
            ) {
                Text("添加", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PluginArrayToken(text: String, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f),
    ) {
        Row(
            modifier = Modifier.padding(start = 9.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = text,
                color = FishPiTheme.onSurface,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp),
            )
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "移除",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onRemove)
                    .padding(2.dp),
            )
        }
    }
}

@Composable
private fun PluginAddSettingSection(
    expanded: Boolean,
    newKey: String,
    newValue: String,
    onExpandedChange: (Boolean) -> Unit,
    onKeyChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (expanded) MaterialTheme.colorScheme.surfaceContainerLow else Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!expanded) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "新增设置项",
                    color = FishPiTheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (expanded) "收起" else "展开",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                )
            }
            if (expanded) {
                CompactPluginTextField(
                    value = newKey,
                    onValueChange = onKeyChange,
                    placeholder = "键名",
                    modifier = Modifier.fillMaxWidth(),
                )
                CompactPluginTextField(
                    value = newValue,
                    onValueChange = onValueChange,
                    placeholder = "值(JSON 格式)",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 2,
                )
                Button(
                    onClick = onAdd,
                    enabled = newKey.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    contentPadding = PaddingValues(vertical = 0.dp),
                ) {
                    Text("添加", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun CompactPluginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.heightIn(min = if (singleLine) 38.dp else 64.dp),
        singleLine = singleLine,
        minLines = minLines,
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
        placeholder = {
            Text(placeholder, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f))
        },
        shape = RoundedCornerShape(10.dp),
    )
}

@Composable
private fun CompactSaveButton(enabled: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        modifier = Modifier.height(36.dp),
    ) {
        Text("保存", fontSize = 12.sp)
    }
}

private fun parsePluginSettingValue(value: String): Any {
    return try {
        JSONObject("{\"v\":$value}").get("v")
    } catch (_: Exception) {
        value
    }
}

private fun pluginSettingTypeName(value: Any): String = when (value) {
    is JSONArray -> "数组"
    is Number -> "数字"
    else -> "文本"
}

private fun Double.toCompactNumberString(): String =
    if (this == toLong().toDouble()) toLong().toString() else toString()
