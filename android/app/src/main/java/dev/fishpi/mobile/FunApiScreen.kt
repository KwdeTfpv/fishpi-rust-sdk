package dev.fishpi.mobile

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Composable
internal fun FunApiScreen(
    onOpenStore: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember { FunApiStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var customEntries by remember { mutableStateOf(store.getCustomEntries()) }
    var selectedEntry by remember { mutableStateOf<FunApiEntry?>(null) }
    var editingEntry by remember { mutableStateOf<FunApiEntry?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    fun saveCustomEntries(next: List<FunApiEntry>) {
        customEntries = next
        store.saveCustomEntries(next)
    }

    if (selectedEntry != null) {
        FunApiResultDialog(
            entry = selectedEntry!!,
            onDismiss = { selectedEntry = null },
        )
    }

    if (showEditor) {
        FunApiEditorDialog(
            initial = editingEntry,
            onDismiss = {
                showEditor = false
                editingEntry = null
            },
            onSave = { saved ->
                val next = customEntries
                    .filterNot { it.id == saved.id }
                    .plus(saved)
                saveCustomEntries(next)
                showEditor = false
                editingEntry = null
            },
        )
    }

    val entries = remember(customEntries) { listOf(FishPiExtensionMarketEntry) + FunApiDefaults + customEntries }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            )
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "工具",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .size(width = 42.dp, height = 4.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            IconButton(
                onClick = {
                    editingEntry = null
                    showEditor = true
                },
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "新增工具接口")
            }
        }
        Spacer(modifier = Modifier.height(36.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items((entries.size + 1) / 2) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    repeat(2) { columnIndex ->
                        val entry = entries.getOrNull(rowIndex * 2 + columnIndex)
                        if (entry == null) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            FunApiMenuCard(
                                entry = entry,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (entry.id == FishPiExtensionMarketEntry.id) {
                                        onOpenStore()
                                    } else if (entry.url.isBlank()) {
                                        FishPiNotifier.show("${entry.title} 还没有配置接口")
                                    } else {
                                        selectedEntry = entry
                                    }
                                },
                                onEdit = if (entry.custom) {
                                    {
                                        editingEntry = entry
                                        showEditor = true
                                    }
                                } else {
                                    null
                                },
                                onDelete = if (entry.custom) {
                                    {
                                        scope.launch {
                                            saveCustomEntries(customEntries.filterNot { it.id == entry.id })
                                        }
                                    }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    text = "声明：公开接口工具的内容来自第三方 API，仅供娱乐和学习交流使用；扩展集市内容由对应作者发布，请按需安装与使用。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 25.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 18.dp),
                )
            }
        }
    }
}

@Composable
private fun FunApiMenuCard(
    entry: FunApiEntry,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(92.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(entry.color.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = entry.icon,
                        contentDescription = null,
                        tint = entry.color,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = entry.summary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (onEdit != null || onDelete != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp),
                ) {
                    onEdit?.let {
                        IconButton(onClick = it, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.Edit, contentDescription = "编辑${entry.title}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    onDelete?.let {
                        IconButton(onClick = it, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.Delete, contentDescription = "删除${entry.title}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FunApiResultDialog(
    entry: FunApiEntry,
    onDismiss: () -> Unit,
) {
    var requestSeed by remember { mutableStateOf(System.currentTimeMillis()) }
    var responseText by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(entry, requestSeed) {
        if (entry.responseType != FunApiResponseType.Image) {
            loading = true
            error = ""
            responseText = ""
            val result = FunApiHttpClient.execute(entry)
            result
                .onSuccess { responseText = entry.responseType.format(it) }
                .onFailure { error = it.message.orEmpty().ifBlank { "请求失败" } }
            loading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${entry.method.name} / ${entry.responseType.label}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                IconButton(onClick = { requestSeed = System.currentTimeMillis() }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "刷新${entry.title}")
                }
            }
        },
        text = {
            when {
                entry.responseType == FunApiResponseType.Image && entry.method == FunApiMethod.Get -> {
                    SubcomposeAsyncImage(
                        model = entry.url.withCacheBust(requestSeed),
                        imageLoader = rememberFishPiImageLoader(),
                        contentDescription = entry.title,
                        contentScale = ContentScale.Fit,
                        loading = { FunApiImagePlaceholder(text = "图片加载中...", showProgress = true) },
                        error = { FunApiImagePlaceholder(text = "图片加载失败，点右上角刷新重试", showProgress = false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp, max = 560.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    )
                }
                entry.responseType == FunApiResponseType.Image -> {
                    FunApiTextResult("图片响应暂时只支持 GET。")
                }
                loading -> FunApiTextResult("加载中...")
                error.isNotBlank() -> FunApiTextResult(error)
                else -> FunApiTextResult(responseText)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}

@Composable
private fun FunApiImagePlaceholder(
    text: String,
    showProgress: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.5.dp,
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun FunApiTextResult(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(14.dp),
        )
    }
}

@Composable
private fun FunApiEditorDialog(
    initial: FunApiEntry?,
    onDismiss: () -> Unit,
    onSave: (FunApiEntry) -> Unit,
) {
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var summary by remember(initial) { mutableStateOf(initial?.summary.orEmpty()) }
    var url by remember(initial) { mutableStateOf(initial?.url.orEmpty()) }
    var body by remember(initial) { mutableStateOf(initial?.body.orEmpty()) }
    var method by remember(initial) { mutableStateOf(initial?.method ?: FunApiMethod.Get) }
    var responseType by remember(initial) { mutableStateOf(initial?.responseType ?: FunApiResponseType.Image) }
    var colorIndex by remember(initial) {
        mutableStateOf(
            FunApiPalette.indexOfFirst { it == initial?.color }.takeIf { it >= 0 } ?: 0,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新增工具接口" else "编辑工具接口") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = summary,
                        onValueChange = { summary = it },
                        label = { Text("描述") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    FunApiChipRow(
                        label = "请求",
                        values = FunApiMethod.entries,
                        selected = method,
                        title = { it.name },
                        onSelect = { method = it },
                    )
                }
                item {
                    FunApiChipRow(
                        label = "响应",
                        values = FunApiResponseType.entries,
                        selected = responseType,
                        title = { it.label },
                        onSelect = { responseType = it },
                    )
                }
                if (method == FunApiMethod.Post) {
                    item {
                        OutlinedTextField(
                            value = body,
                            onValueChange = { body = it },
                            label = { Text("POST body") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                item {
                    Text("颜色", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                        FunApiPalette.forEachIndexed { index, color ->
                            FilterChip(
                                selected = colorIndex == index,
                                onClick = { colorIndex = index },
                                label = { Text(" ") },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(RoundedCornerShape(99.dp))
                                            .background(color),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.trim().isNotBlank() && url.trim().startsWith("http", ignoreCase = true),
                onClick = {
                    onSave(
                        FunApiEntry(
                            id = initial?.id ?: "custom-${System.currentTimeMillis()}",
                            title = title.trim(),
                            summary = summary.trim().ifBlank { "自定义工具接口" },
                            url = url.trim(),
                            method = method,
                            responseType = responseType,
                            body = body,
                            color = FunApiPalette[colorIndex],
                            icon = responseType.defaultIcon(),
                            custom = true,
                        ),
                    )
                },
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun <T> FunApiChipRow(
    label: String,
    values: List<T>,
    selected: T,
    title: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { value ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(title(value)) },
                )
            }
        }
    }
}

private data class FunApiEntry(
    val id: String,
    val title: String,
    val summary: String,
    val url: String,
    val method: FunApiMethod,
    val responseType: FunApiResponseType,
    val body: String = "",
    val color: Color,
    val icon: ImageVector,
    val custom: Boolean = false,
)

private enum class FunApiMethod {
    Get,
    Post,
}

private enum class FunApiResponseType(val label: String) {
    Image("图片"),
    Text("文本"),
    Json("JSON");

    fun format(raw: String): String = when (this) {
        Image -> raw
        Text -> raw
        Json -> raw.prettyJson()
    }
}

private class FunApiStore(context: Context) {
    private val prefs = context.getSharedPreferences("fishpi-fun-api", Context.MODE_PRIVATE)

    fun getCustomEntries(): List<FunApiEntry> {
        val raw = prefs.getString(KEY_ENTRIES, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val responseType = item.optString("responseType").toResponseType()
                    add(
                        FunApiEntry(
                            id = item.optString("id").ifBlank { "custom-$index" },
                            title = item.optString("title"),
                            summary = item.optString("summary").ifBlank { "自定义工具接口" },
                            url = item.optString("url"),
                            method = item.optString("method").toMethod(),
                            responseType = responseType,
                            body = item.optString("body"),
                            color = Color(item.optLong("color", FunApiPalette.first().value.toLong()).toULong()),
                            icon = responseType.defaultIcon(),
                            custom = true,
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
            .filter { it.title.isNotBlank() && it.url.isNotBlank() }
    }

    fun saveCustomEntries(entries: List<FunApiEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("title", entry.title)
                    .put("summary", entry.summary)
                    .put("url", entry.url)
                    .put("method", entry.method.name)
                    .put("responseType", entry.responseType.name)
                    .put("body", entry.body)
                    .put("color", entry.color.value.toLong()),
            )
        }
        prefs.edit().putString(KEY_ENTRIES, array.toString()).apply()
    }

    private companion object {
        const val KEY_ENTRIES = "entries"
    }
}

private object FunApiHttpClient {
    suspend fun execute(entry: FunApiEntry): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(entry.url).openConnection() as HttpURLConnection).apply {
                requestMethod = entry.method.name.uppercase(Locale.ROOT)
                connectTimeout = 10_000
                readTimeout = 15_000
                doInput = true
                if (entry.method == FunApiMethod.Post) {
                    doOutput = true
                    entry.body.toByteArray(Charsets.UTF_8).let { bytes ->
                        outputStream.use { it.write(bytes) }
                    }
                }
            }
            try {
                val stream = if (connection.responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream ?: connection.inputStream
                }
                val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                if (connection.responseCode !in 200..299) {
                    error("HTTP ${connection.responseCode}: $text")
                }
                text
            } finally {
                connection.disconnect()
            }
        }
    }
}

private val FunApiPalette = listOf(
    Color(0xFFFF5F82),
    Color(0xFF524BF0),
    Color(0xFF5B8E7D),
    Color(0xFF2FD36E),
    Color(0xFF00A7C7),
    Color(0xFF8E5CF7),
)

private val FunApiDefaults = listOf(
    FunApiEntry(
        id = "moyu",
        title = "摸鱼日报",
        summary = "今日摸鱼周报",
        url = "https://api.52vmy.cn/api/wl/moyu",
        method = FunApiMethod.Get,
        responseType = FunApiResponseType.Image,
        color = FunApiPalette[3],
        icon = Icons.Rounded.CalendarMonth,
    ),
)

private val FishPiExtensionMarketEntry = FunApiEntry(
    id = "fishpi-extension-market",
    title = "鱼排扩展集市",
    summary = "插件、主题和应用增强",
    url = "",
    method = FunApiMethod.Get,
    responseType = FunApiResponseType.Text,
    color = Color(0xFF2563EB),
    icon = Icons.Rounded.Storefront,
)

private fun FunApiResponseType.defaultIcon(): ImageVector = when (this) {
    FunApiResponseType.Image -> Icons.Rounded.Image
    FunApiResponseType.Text -> Icons.AutoMirrored.Rounded.MenuBook
    FunApiResponseType.Json -> Icons.Rounded.PhotoLibrary
}

private fun String.toMethod(): FunApiMethod =
    FunApiMethod.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: FunApiMethod.Get

private fun String.toResponseType(): FunApiResponseType =
    FunApiResponseType.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: FunApiResponseType.Image

private fun String.prettyJson(): String =
    runCatching {
        val trimmed = trim()
        when {
            trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
            trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
            else -> trimmed
        }
    }.getOrElse { this }

private fun String.withCacheBust(seed: Long): String {
    val separator = if (contains("?")) "&" else "?"
    return "$this${separator}t=$seed"
}
