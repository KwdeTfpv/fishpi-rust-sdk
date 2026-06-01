package dev.fishpi.mobile.feature.extensionstore

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.fishpi.mobile.FishPiNotifier
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.StoreThemeSaveState
import dev.fishpi.mobile.rememberFishPiImageLoader
import dev.fishpi.mobile.data.ExtensionStoreClient
import dev.fishpi.mobile.data.ExtensionStoreComment
import dev.fishpi.mobile.data.ExtensionStoreItem
import dev.fishpi.mobile.data.ExtensionStoreSession
import dev.fishpi.mobile.data.ExtensionStoreUploadRequest
import dev.fishpi.mobile.plugin.PluginManager
import dev.fishpi.mobile.ui.components.PlainBackButton
import coil3.compose.SubcomposeAsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal enum class StoreFilter(val title: String, val type: String?) {
    All("全部", null),
    Plugins("插件", ExtensionStoreClient.TypeAppExtension),
    Themes("主题", ExtensionStoreClient.TypeAppTheme),
}

private fun StoreFilter.emptyMessage(total: Int): String =
    if (total > 0) {
        "当前筛选下没有可展示内容"
    } else {
        when (this) {
            StoreFilter.All -> "当前没有可展示的 APP 扩展或主题"
            StoreFilter.Plugins -> "当前没有可展示的 APP 扩展"
            StoreFilter.Themes -> "当前没有可展示的 APP 主题"
        }
    }

private val PluginHeaderRegex = Regex("""//\s*==FishPiPlugin==[\s\S]*?//\s*==/FishPiPlugin==""")
private val StoreIdentifierRegex = Regex("""^[A-Za-z0-9_.-]{3,100}$""")

private enum class StoreItemAction {
    Purchase,
    Install,
    Update,
    Installed,
}

@Composable
internal fun DefaultExtensionStoreUi(
    state: ExtensionStoreState,
    dispatch: (ExtensionStoreAction) -> Unit,
    onImportTheme: (String, Long, String) -> Result<String>,
    themeSaveState: (String, Long, String) -> StoreThemeSaveState,
) {
    val scope = rememberCoroutineScope()
    val pluginManager = remember { PluginManager.get() }
    var installingId by remember { mutableStateOf<Long?>(null) }
    var contentById by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var detailById by remember { mutableStateOf<Map<Long, ExtensionStoreItem>>(emptyMap()) }
    var versionsById by remember { mutableStateOf<Map<Long, List<ExtensionStoreItem>>>(emptyMap()) }
    var commentsById by remember { mutableStateOf<Map<Long, List<ExtensionStoreComment>>>(emptyMap()) }
    var detailItem by remember { mutableStateOf<ExtensionStoreItem?>(null) }
    var purchaseConfirmItem by remember { mutableStateOf<ExtensionStoreItem?>(null) }
    var uploadOpen by remember { mutableStateOf(false) }
    var uploadSubmitted by remember { mutableStateOf(false) }
    var uploadBaseline by remember { mutableStateOf(state.uploadSuccessCount) }

    fun actionStateFor(item: ExtensionStoreItem, versions: List<ExtensionStoreItem> = emptyList()): StoreItemAction {
        val ownership = item.ownershipIn(state.purchasedItems, versions)
        if (item.type == ExtensionStoreClient.TypeAppTheme) {
            val themeContent = contentById[item.id]?.takeIf { it.isNotBlank() } ?: item.code
            val currentThemeState = if (themeContent.isNotBlank()) {
                themeSaveState(item.identifier, item.id, themeContent)
            } else {
                StoreThemeSaveState.NotSaved
            }
            return item.themeActionState(ownership, currentThemeState)
        }
        return item.actionState(
            ownership = ownership,
            currentInstalled = item.isCurrentInstalled(
                pluginManager = pluginManager,
                loadedContent = contentById[item.id],
            ),
        )
    }

    fun installItem(item: ExtensionStoreItem) {
        if (installingId != null) return
        scope.launch {
            installingId = item.id
            var themeSaveStateBefore: StoreThemeSaveState? = null
            runCatching {
                val content = withContext(Dispatchers.IO) {
                    ExtensionStoreClient.shared.downloadItemContent(item)
                }
                contentById = contentById + (item.id to content)
                themeSaveStateBefore = if (item.type == ExtensionStoreClient.TypeAppTheme) {
                    themeSaveState(item.identifier, item.id, content)
                } else {
                    null
                }
                if (item.type == ExtensionStoreClient.TypeAppTheme) {
                    if (themeSaveStateBefore != StoreThemeSaveState.SavedSameContent) {
                        onImportTheme(item.identifier, item.id, content).getOrThrow()
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        pluginManager.installPluginFromSource(content, item.preferredStoreName(), enable = false)
                    }
                }
            }.onSuccess {
                FishPiNotifier.success(
                    if (item.type == ExtensionStoreClient.TypeAppTheme) {
                        if (themeSaveStateBefore == StoreThemeSaveState.SavedDifferentContent) {
                            "${item.displayName()} 已更新"
                        } else {
                            "${item.displayName()} 已加入主题列表"
                        }
                    } else {
                        "${item.displayName()} 已安装"
                    },
                )
            }.onFailure { throwable ->
                FishPiNotifier.error("安装失败：${throwable.message ?: "未知错误"}")
            }
            installingId = null
        }
    }


    if (uploadOpen) {
        BackHandler {
            if (!state.isUploading) uploadOpen = false
        }
        StoreUploadPage(
            initialType = state.selectedFilter.type ?: ExtensionStoreClient.TypeAppExtension,
            uploading = state.isUploading,
            onBack = {
                if (!state.isUploading) uploadOpen = false
            },
            onSubmit = {
                uploadSubmitted = true
                uploadBaseline = state.uploadSuccessCount
                dispatch(ExtensionStoreAction.Upload(it))
            },
        )
        LaunchedEffect(state.uploadSuccessCount, uploadSubmitted, uploadBaseline) {
            if (uploadSubmitted && state.uploadSuccessCount > uploadBaseline) {
                uploadOpen = false
                uploadSubmitted = false
            }
        }
        return
    }

    val activeDetailItem = detailItem
    if (activeDetailItem != null) {
        val item = activeDetailItem
        val fullItem = detailById[item.id] ?: item
        val versions = versionsById[item.id].orEmpty()
        BackHandler {
            detailItem = null
        }
        StoreItemDetailPage(
            item = fullItem,
            token = state.session?.accessToken,
            versions = versions,
            comments = commentsById[item.id].orEmpty(),
            running = installingId == fullItem.id,
            purchasing = state.purchasingId == fullItem.id,
            enabled = installingId == null && state.purchasingId == null,
            actionState = actionStateFor(fullItem, versions),
            onDismiss = { detailItem = null },
            onDetailLoaded = { loadedItem, loadedVersions, loadedComments ->
                detailById = detailById + (item.id to loadedItem)
                versionsById = versionsById + (item.id to loadedVersions)
                commentsById = commentsById + (item.id to loadedComments)
                if (loadedItem.code.isNotBlank()) {
                    contentById = contentById + (loadedItem.id to loadedItem.code)
                }
            },
            onPreviewLoaded = { content -> contentById = contentById + (item.id to content) },
            onInstall = { installItem(fullItem) },
            onPurchase = { purchaseConfirmItem = fullItem },
        )
        purchaseConfirmItem?.let { pending ->
            PurchaseConfirmDialog(
                item = pending,
                purchasing = state.purchasingId == pending.id,
                onDismiss = {
                    if (state.purchasingId == null) purchaseConfirmItem = null
                },
                onConfirm = {
                    purchaseConfirmItem = null
                    dispatch(ExtensionStoreAction.Purchase(pending))
                },
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StoreTopBar(
            session = state.session,
            authError = state.authError,
            isAuthenticating = state.isAuthenticating,
            canUpload = state.session != null && !state.isUploading,
            onUpload = { uploadOpen = true },
        )
        StoreControls(
            query = state.query,
            onQueryChange = { dispatch(ExtensionStoreAction.ChangeQuery(it)) },
            selectedFilter = state.selectedFilter,
            onFilterChange = { dispatch(ExtensionStoreAction.ChangeFilter(it)) },
            canRefresh = state.session != null && !state.isLoading,
            onRefresh = { dispatch(ExtensionStoreAction.Refresh) },
        )
        when {
            state.isAuthenticating -> StoreStateRow("正在进行鱼排扩展集市鉴权", busy = true)
            state.authError != null -> StoreStateRow(state.authError)
            state.isLoading -> StoreStateRow("正在加载鱼排扩展集市", busy = true)
            state.loadError != null -> StoreStateRow(state.loadError)
            state.items.isEmpty() -> StoreStateRow(state.selectedFilter.emptyMessage(state.total))
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.items, key = { "${it.type}-${it.id}" }) { item ->
                    val actionState = actionStateFor(item)
                    val owned = actionState != StoreItemAction.Purchase
                    StoreItemRow(
                        item = item,
                        running = installingId == item.id,
                        purchasing = state.purchasingId == item.id,
                        enabled = installingId == null && state.purchasingId == null,
                        actionState = actionState,
                        owned = owned,
                        onOpenDetail = { detailItem = item },
                        onInstall = { installItem(item) },
                        onPurchase = { purchaseConfirmItem = item },
                    )
                }
            }
        }
    }

    purchaseConfirmItem?.let { item ->
        PurchaseConfirmDialog(
            item = item,
            purchasing = state.purchasingId == item.id,
            onDismiss = {
                if (state.purchasingId == null) purchaseConfirmItem = null
            },
            onConfirm = {
                purchaseConfirmItem = null
                dispatch(ExtensionStoreAction.Purchase(item))
            },
        )
    }

}

@Composable
private fun StoreTopBar(
    session: ExtensionStoreSession?,
    authError: String?,
    isAuthenticating: Boolean,
    canUpload: Boolean,
    onUpload: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Storefront,
                contentDescription = "鱼排扩展集市",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("鱼排扩展集市", color = FishPiTheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(
                text = when {
                    isAuthenticating -> "正在连接鱼排扩展集市"
                    authError != null -> authError
                    session != null -> "发现并获取鱼排的最新扩展与主题"
                    else -> "使用当前账号访问鱼排扩展集市"
                },
                color = if (authError == null) FishPiTheme.weakText else MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(
            onClick = onUpload,
            enabled = canUpload,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) {
            Icon(Icons.Rounded.UploadFile, contentDescription = "发布扩展或主题", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("发布", fontSize = 12.sp)
        }
    }
}

@Composable
private fun StoreControls(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: StoreFilter,
    onFilterChange: (StoreFilter) -> Unit,
    canRefresh: Boolean,
    onRefresh: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "搜索") },
            placeholder = { Text("搜索扩展、主题或作者") },
            shape = RoundedCornerShape(FishPiTheme.radiusField),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StoreFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter.title, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.54f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedFilter == filter,
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                        selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                    ),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = onRefresh,
                enabled = canRefresh,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = "刷新扩展集市", modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("刷新", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun StoreStateRow(text: String, busy: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FishPiTheme.radiusBox))
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f))
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Text(text = text, color = FishPiTheme.weakText)
    }
}

@Composable
private fun PurchaseConfirmDialog(
    item: ExtensionStoreItem,
    purchasing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认购买") },
        text = {
            Text(
                text = "确定要购买「${item.displayName()}」吗？本次购买需要 ${item.priceLabel()}，购买后可在集市中安装或加入到 APP。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !purchasing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                ),
            ) {
                Text(if (purchasing) "购买中" else "确认购买")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !purchasing) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun StoreUploadPage(
    initialType: String,
    uploading: Boolean,
    onBack: () -> Unit,
    onSubmit: (ExtensionStoreUploadRequest) -> Unit,
) {
    var type by remember(initialType) {
        mutableStateOf(
            initialType.takeIf { it == ExtensionStoreClient.TypeAppTheme }
                ?: ExtensionStoreClient.TypeAppExtension,
        )
    }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var identifier by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("0") }
    var code by remember {
        mutableStateOf(
            if (type == ExtensionStoreClient.TypeAppTheme) defaultAppThemeCode() else defaultAppExtensionCode(),
        )
    }
    var error by remember { mutableStateOf<String?>(null) }

    fun switchType(nextType: String) {
        if (type == nextType) return
        val oldDefault = if (type == ExtensionStoreClient.TypeAppTheme) defaultAppThemeCode() else defaultAppExtensionCode()
        type = nextType
        if (code.isBlank() || code == oldDefault) {
            code = if (nextType == ExtensionStoreClient.TypeAppTheme) defaultAppThemeCode() else defaultAppExtensionCode()
        }
    }

    fun submit(isDraft: Boolean) {
        val cleanName = name.trim()
        val cleanIdentifier = identifier.trim()
        val cleanPrice = price.trim().ifBlank { "0" }
        val cleanCode = code.trim()
        error = validateUploadForm(
            name = cleanName,
            identifier = cleanIdentifier,
            price = cleanPrice,
            type = type,
            code = cleanCode,
        )
        if (error != null) return
        onSubmit(
            ExtensionStoreUploadRequest(
                name = cleanName,
                description = description.trim(),
                identifier = cleanIdentifier,
                price = cleanPrice,
                type = type,
                code = cleanCode,
                language = if (type == ExtensionStoreClient.TypeAppTheme) "json" else "javascript",
                isDraft = isDraft,
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PlainBackButton(onClick = onBack)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("发布 APP 作品", color = FishPiTheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    if (type == ExtensionStoreClient.TypeAppTheme) "提交 APP 主题配置，审核通过后上架" else "提交 APP 插件脚本，审核通过后上架",
                    color = FishPiTheme.weakText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StoreUploadTypeChip(
                title = "APP 扩展",
                selected = type == ExtensionStoreClient.TypeAppExtension,
                onClick = { switchType(ExtensionStoreClient.TypeAppExtension) },
            )
            StoreUploadTypeChip(
                title = "APP 主题",
                selected = type == ExtensionStoreClient.TypeAppTheme,
                onClick = { switchType(ExtensionStoreClient.TypeAppTheme) },
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CompactUploadField(
                    label = "名称",
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                CompactUploadField(
                    label = "价格",
                    value = price,
                    onValueChange = { next -> price = next.filter { it.isDigit() }.take(7) },
                    modifier = Modifier.width(96.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            CompactUploadField(
                label = "标识符",
                value = identifier,
                onValueChange = { identifier = it },
                placeholder = "me.name.tool",
                singleLine = true,
            )
            CompactUploadField(
                label = "简介",
                value = description,
                onValueChange = { description = it },
                placeholder = "一句话说明这个作品的用途",
                minLines = 2,
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (type == ExtensionStoreClient.TypeAppTheme) "主题 JSON" else "插件 JS",
                    color = FishPiTheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                Text(
                    if (type == ExtensionStoreClient.TypeAppTheme) "合法 JSON 配置" else "需要 FishPiPlugin 头部",
                    color = FishPiTheme.weakText,
                    fontSize = 11.sp,
                )
            }
            CompactCodeField(
                value = code,
                onValueChange = { code = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { submit(true) }, enabled = !uploading, modifier = Modifier.weight(1f)) {
                Text(if (uploading) "处理中" else "存草稿")
            }
            Button(
                onClick = { submit(false) },
                enabled = !uploading,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                ),
            ) {
                Text(if (uploading) "提交中" else "提交审核")
            }
        }
    }
}

@Composable
private fun CompactUploadField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = false,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = FishPiTheme.weakText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = keyboardOptions,
            textStyle = TextStyle(
                color = FishPiTheme.onSurface,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(FishPiTheme.radiusField))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
                .border(
                    FishPiTheme.borderWidth,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                    RoundedCornerShape(FishPiTheme.radiusField),
                )
                .padding(horizontal = 11.dp, vertical = 9.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isBlank() && placeholder.isNotBlank()) {
                        Text(placeholder, color = FishPiTheme.weakText.copy(alpha = 0.62f), fontSize = 13.sp)
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun CompactCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            color = FishPiTheme.onSurface,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        ),
        modifier = modifier
            .clip(RoundedCornerShape(FishPiTheme.radiusBox))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.66f))
            .border(
                FishPiTheme.borderWidth,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                RoundedCornerShape(FishPiTheme.radiusBox),
            )
            .padding(11.dp)
            .verticalScroll(verticalScroll)
            .horizontalScroll(horizontalScroll),
    )
}

@Composable
private fun StoreUploadTypeChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(title, fontSize = 12.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.54f),
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
        ),
    )
}

@Composable
private fun StoreItemRow(
    item: ExtensionStoreItem,
    running: Boolean,
    purchasing: Boolean,
    enabled: Boolean,
    actionState: StoreItemAction,
    owned: Boolean,
    onOpenDetail: () -> Unit,
    onInstall: () -> Unit,
    onPurchase: () -> Unit,
) {
    val isTheme = item.type == ExtensionStoreClient.TypeAppTheme
    Surface(
        shape = RoundedCornerShape(FishPiTheme.radiusBox),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
        border = BorderStroke(FishPiTheme.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClickLabel = "查看${item.displayName()}详情",
                    role = Role.Button,
                    onClick = onOpenDetail,
                )
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StoreTypeIcon(
                icon = if (isTheme) Icons.Rounded.Palette else Icons.Rounded.Extension,
                contentDescription = if (isTheme) "APP 主题" else "APP 插件",
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = item.displayName(),
                        color = FishPiTheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (owned) {
                        StoreOwnedChip()
                    }
                }
                Text(
                    text = item.description.ifBlank { item.identifier.ifBlank { "暂无说明" } },
                    color = FishPiTheme.weakText,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                StoreItemSubline(item = item)
            }
            Box(
                modifier = Modifier.width(66.dp),
                contentAlignment = Alignment.Center,
            ) {
                Button(
                    onClick = when (actionState) {
                        StoreItemAction.Purchase -> onPurchase
                        StoreItemAction.Install -> onInstall
                        StoreItemAction.Update -> onInstall
                        StoreItemAction.Installed -> onInstall
                    },
                    enabled = enabled && actionState != StoreItemAction.Installed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    ),
                ) {
                    Text(
                        when {
                            running -> if (item.type == ExtensionStoreClient.TypeAppTheme) "加入中" else "安装中"
                            purchasing -> "购买中"
                            actionState == StoreItemAction.Installed -> if (item.type == ExtensionStoreClient.TypeAppTheme) "已加入" else "已安装"
                            actionState == StoreItemAction.Update -> "更新"
                            actionState == StoreItemAction.Install -> if (item.type == ExtensionStoreClient.TypeAppTheme) "加入" else "安装"
                            else -> "购买"
                        },
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun StoreOwnedChip() {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        Text(
            text = "已拥有",
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun StoreItemSubline(item: ExtensionStoreItem) {
    val author = item.author.ifBlank { "未知作者" }
    val version = item.version.ifBlank { "1" }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "@$author",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(
            text = "v$version",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
            fontSize = 11.sp,
            maxLines = 1,
        )
        Text(
            text = item.priceLabel(),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StoreItemDetailPage(
    item: ExtensionStoreItem,
    token: String?,
    versions: List<ExtensionStoreItem>,
    comments: List<ExtensionStoreComment>,
    running: Boolean,
    purchasing: Boolean,
    enabled: Boolean,
    actionState: StoreItemAction,
    onDismiss: () -> Unit,
    onDetailLoaded: (ExtensionStoreItem, List<ExtensionStoreItem>, List<ExtensionStoreComment>) -> Unit,
    onPreviewLoaded: (String) -> Unit,
    onInstall: () -> Unit,
    onPurchase: () -> Unit,
) {
    var preview by remember(item.id, item.type) { mutableStateOf(item.code) }
    var previewLoading by remember(item.id, item.type) { mutableStateOf(item.code.isBlank()) }
    var previewError by remember(item.id, item.type) { mutableStateOf<String?>(null) }
    var detailLoading by remember(item.id) { mutableStateOf(versions.isEmpty() && comments.isEmpty()) }
    var compareVersionId by remember(item.id) { mutableStateOf<Long?>(null) }
    var compareVersionContent by remember(item.id) { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var compareVersionLoadingId by remember(item.id) { mutableStateOf<Long?>(null) }
    var compareVersionError by remember(item.id) { mutableStateOf<String?>(null) }
    var commentDraft by remember(item.id) { mutableStateOf("") }
    var commentPosting by remember(item.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val compareVersion = versions.firstOrNull { it.id == compareVersionId && it.id != item.id }
    val pagerState = rememberPagerState(pageCount = { 2 })

    fun selectCompareVersion(version: ExtensionStoreItem) {
        if (version.id == item.id || compareVersionId == version.id) {
            compareVersionId = null
            compareVersionError = null
            return
        }
        compareVersionId = version.id
        compareVersionError = null
        if (version.code.isNotBlank() || compareVersionContent.containsKey(version.id)) return
        scope.launch {
            compareVersionLoadingId = version.id
            runCatching {
                withContext(Dispatchers.IO) { ExtensionStoreClient.shared.downloadItemContent(version) }
            }.onSuccess { content ->
                compareVersionContent = compareVersionContent + (version.id to content)
            }.onFailure { throwable ->
                compareVersionError = throwable.message ?: "版本内容读取失败"
            }
            if (compareVersionLoadingId == version.id) {
                compareVersionLoadingId = null
            }
        }
    }

    fun submitComment() {
        val activeToken = token
        val content = commentDraft.trim()
        if (activeToken.isNullOrBlank()) {
            FishPiNotifier.error("请先完成集市鉴权")
            return
        }
        if (content.isBlank() || commentPosting) return
        scope.launch {
            commentPosting = true
            runCatching {
                withContext(Dispatchers.IO) {
                    ExtensionStoreClient.shared.postItemComment(activeToken, item.id, content)
                    ExtensionStoreClient.shared.getItemComments(item.id, activeToken)
                }
            }.onSuccess { refreshed ->
                commentDraft = ""
                onDetailLoaded(item, versions, refreshed)
                FishPiNotifier.success("评论已发送")
            }.onFailure { throwable ->
                FishPiNotifier.error("评论发送失败：${throwable.message ?: "未知错误"}")
            }
            commentPosting = false
        }
    }

    LaunchedEffect(item.id, item.type) {
        previewLoading = true
        previewError = null
        val detailResult = runCatching {
            withContext(Dispatchers.IO) {
                ExtensionStoreClient.shared.getItem(item.id, token)
            }
        }
        val loadedVersions = runCatching {
            withContext(Dispatchers.IO) { ExtensionStoreClient.shared.getItemVersions(item.id, token) }
        }.getOrDefault(emptyList())
        val loadedComments = runCatching {
            withContext(Dispatchers.IO) { ExtensionStoreClient.shared.getItemComments(item.id, token) }
        }.getOrDefault(emptyList())
        val detail = detailResult.getOrNull() ?: item
        onDetailLoaded(detail, loadedVersions, loadedComments)
        detailResult.onSuccess {
            if (detail.code.isNotBlank()) {
                preview = detail.code
                onPreviewLoaded(detail.code)
            } else if (preview.isBlank()) {
                val content = withContext(Dispatchers.IO) { ExtensionStoreClient.shared.downloadItemContent(detail) }
                preview = content
                onPreviewLoaded(content)
            }
        }.onFailure {
            if (preview.isBlank()) {
                previewError = it.message ?: "内容预览加载失败"
            }
        }
        previewLoading = false
        detailLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StoreDetailPageHeader(item = item, onBack = onDismiss)
        StoreDetailTabs(
            selected = pagerState.currentPage,
            onSelect = { page -> scope.launch { pagerState.animateScrollToPage(page) } },
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            if (page == 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 1.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StoreDetailHeader(item = item)
                StoreDetailMetaStrip(item = item)
                StoreVersionStrip(
                    currentId = item.id,
                    versions = versions,
                    loading = detailLoading,
                    selectedVersionId = compareVersionId,
                    onSelectVersion = ::selectCompareVersion,
                )
                if (item.type == ExtensionStoreClient.TypeAppTheme) {
                    StoreThemePreview(content = preview)
                }
                StoreCodePreview(
                    modifier = Modifier.weight(1f),
                    title = if (item.type == ExtensionStoreClient.TypeAppTheme) "主题配置 JSON" else "插件 JavaScript",
                    content = preview,
                    loading = previewLoading,
                    error = previewError,
                    isJson = item.type == ExtensionStoreClient.TypeAppTheme,
                )
                StoreVersionDiffPreview(
                    current = preview,
                    previous = compareVersion,
                    previousContent = compareVersion?.let { compareVersionContent[it.id] },
                    loading = compareVersion?.id == compareVersionLoadingId,
                    error = compareVersionError,
                    isJson = item.type == ExtensionStoreClient.TypeAppTheme,
                )
            }
            } else {
                StoreCommentsSection(
                    modifier = Modifier.fillMaxSize(),
                    comments = comments,
                    loading = detailLoading,
                    draft = commentDraft,
                    posting = commentPosting,
                    onDraftChange = { commentDraft = it },
                    onSubmit = ::submitComment,
                    expanded = true,
                )
            }
        }
        StoreDetailActions(
            item = item,
            running = running,
            purchasing = purchasing,
            enabled = enabled && !purchasing,
            actionState = actionState,
            onDismiss = onDismiss,
            onInstall = onInstall,
            onPurchase = onPurchase,
        )
    }
}

@Composable
private fun StoreDetailPageHeader(
    item: ExtensionStoreItem,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PlainBackButton(onClick = onBack)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.displayName(),
                color = FishPiTheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (item.type == ExtensionStoreClient.TypeAppTheme) "APP 主题" else "APP 插件",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
        Text(
            text = item.priceLabel(),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun StoreDetailTabs(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf("详情", "评论").forEachIndexed { index, title ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clickable(
                        onClickLabel = "切换到$title",
                        role = Role.Button,
                    ) { onSelect(index) },
                shape = RoundedCornerShape(999.dp),
                color = if (selected == index) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.48f)
                },
                contentColor = if (selected == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = if (selected == index) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun StoreDetailHeader(item: ExtensionStoreItem) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "简介",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = item.description.ifBlank { "暂无说明" },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StoreDetailMetaStrip(item: ExtensionStoreItem) {
    val scroll = rememberScrollState()
    val values = listOfNotNull(
        item.author.ifBlank { null }?.let { "@$it" },
        item.language.ifBlank { if (item.type == ExtensionStoreClient.TypeAppTheme) "json" else "javascript" },
        item.identifier.ifBlank { null },
    )
    if (values.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        values.forEachIndexed { index, value ->
            Text(
                text = value,
                color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
            )
            if (index != values.lastIndex) {
                Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.54f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun StoreVersionStrip(
    currentId: Long,
    versions: List<ExtensionStoreItem>,
    loading: Boolean,
    selectedVersionId: Long?,
    onSelectVersion: (ExtensionStoreItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "版本",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = when {
                    loading -> "读取中"
                    versions.isEmpty() -> "暂无版本"
                    else -> "共 ${versions.size} 个"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
        if (versions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                versions.sortedByDescending { it.version.toIntOrNull() ?: 0 }.forEach { version ->
                    StoreVersionChip(
                        text = "v${version.version.ifBlank { "1" }}",
                        selected = version.id == currentId,
                        comparing = selectedVersionId == version.id,
                        onClick = { onSelectVersion(version) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StoreVersionChip(
    text: String,
    selected: Boolean,
    comparing: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = if (selected) {
            Modifier
        } else {
            Modifier.clickable(
                onClickLabel = "对比$text",
                role = Role.Button,
                onClick = onClick,
            )
        },
        shape = RoundedCornerShape(999.dp),
        color = when {
            comparing -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
            selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.58f)
        },
        contentColor = when {
            comparing -> MaterialTheme.colorScheme.secondary
            selected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Text(
            text = when {
                selected -> "$text 当前"
                comparing -> "$text 对比"
                else -> text
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = if (selected || comparing) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StoreVersionDiffPreview(
    current: String,
    previous: ExtensionStoreItem?,
    previousContent: String?,
    loading: Boolean,
    error: String?,
    isJson: Boolean,
) {
    if (previous == null) return
    val oldCode = previousContent ?: previous.code
    val diffLines = remember(current, oldCode, isJson) {
        buildStoreDiff(
            oldText = if (isJson) oldCode.prettyJsonOrRaw() else oldCode.trim(),
            newText = if (isJson) current.prettyJsonOrRaw() else current.trim(),
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "版本差异",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "v${previous.version.ifBlank { "1" }} -> 当前",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
                .clip(RoundedCornerShape(FishPiTheme.radiusField))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.60f))
                .padding(10.dp),
        ) {
            if (loading || error != null || oldCode.isBlank()) {
                val message = when {
                    loading -> "正在读取旧版本内容"
                    error != null -> error
                    oldCode.isBlank() -> "旧版本暂无可对比内容"
                    else -> ""
                }
                Text(
                    message,
                    color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            } else if (diffLines.isEmpty()) {
                Text(
                    "两个版本内容一致",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    diffLines.take(120).forEach { line ->
                        Text(
                            text = line.text,
                            color = when (line.kind) {
                                StoreDiffKind.Added -> MaterialTheme.colorScheme.primary
                                StoreDiffKind.Removed -> MaterialTheme.colorScheme.error
                                StoreDiffKind.Same -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreCommentsSection(
    modifier: Modifier = Modifier,
    comments: List<ExtensionStoreComment>,
    loading: Boolean,
    draft: String,
    posting: Boolean,
    onDraftChange: (String) -> Unit,
    onSubmit: () -> Unit,
    expanded: Boolean = false,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "评论",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = when {
                    loading -> "读取中"
                    comments.isEmpty() -> "暂无评论"
                    else -> "${comments.size} 条"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
        when {
            comments.isNotEmpty() -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (expanded) Modifier.weight(1f) else Modifier.heightIn(max = 220.dp)),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(comments, key = { it.id }) { comment ->
                    StoreCommentRow(comment)
                }
            }
            !loading -> Text(
                text = "暂无评论",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                enabled = !posting,
                placeholder = { Text("写评论") },
                shape = RoundedCornerShape(FishPiTheme.radiusField),
            )
            Button(
                onClick = onSubmit,
                enabled = draft.isNotBlank() && !posting,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                ),
            ) {
                Text(if (posting) "发送中" else "发送", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun StoreCommentRow(comment: ExtensionStoreComment) {
    val imageLoader = rememberFishPiImageLoader()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            if (comment.avatar.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = comment.avatar,
                    imageLoader = imageLoader,
                    contentDescription = comment.displayName(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        StoreCommentAvatarFallback(comment)
                    },
                    error = {
                        StoreCommentAvatarFallback(comment)
                    },
                )
            } else {
                StoreCommentAvatarFallback(comment)
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = comment.displayName(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (comment.isAdmin) {
                    Text(
                        text = "管理员",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = comment.username.takeIf { it.isNotBlank() }?.let { "@$it" } ?: comment.authorId,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (comment.createdAt.isNotBlank()) {
                    Text(
                        text = "· ${comment.createdAt.toStoreDateLabel()}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        fontSize = 10.sp,
                        maxLines = 1,
                    )
                }
            }
            Text(
                text = comment.content.ifBlank { "无内容" },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StoreCommentAvatarFallback(comment: ExtensionStoreComment) {
    Text(
        text = comment.displayName().firstOrNull()?.toString() ?: "?",
        color = MaterialTheme.colorScheme.primary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun StoreDetailActions(
    item: ExtensionStoreItem,
    running: Boolean,
    purchasing: Boolean,
    enabled: Boolean,
    actionState: StoreItemAction,
    onDismiss: () -> Unit,
    onInstall: () -> Unit,
    onPurchase: () -> Unit,
) {
    val isTheme = item.type == ExtensionStoreClient.TypeAppTheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
            Text("关闭")
        }
        Button(
            onClick = when (actionState) {
                StoreItemAction.Purchase -> onPurchase
                StoreItemAction.Install -> onInstall
                StoreItemAction.Update -> onInstall
                StoreItemAction.Installed -> onInstall
            },
            enabled = enabled && actionState != StoreItemAction.Installed,
            modifier = Modifier.weight(1.6f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            ),
        ) {
            Icon(
                imageVector = if (actionState == StoreItemAction.Purchase) Icons.Rounded.Payments else Icons.Rounded.CloudDownload,
                contentDescription = if (actionState == StoreItemAction.Purchase) "购买" else "下载并安装",
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                when {
                    running -> "安装中"
                    purchasing -> "购买中"
                    actionState == StoreItemAction.Installed -> if (isTheme) "已加入" else "已安装"
                    actionState == StoreItemAction.Update -> "更新"
                    actionState == StoreItemAction.Install -> if (isTheme) "加入" else "安装"
                    else -> "购买 ${item.priceLabel()}"
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StoreCodePreview(
    modifier: Modifier = Modifier,
    title: String,
    content: String,
    loading: Boolean,
    error: String?,
    isJson: Boolean,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val display = remember(content, isJson) {
        if (isJson) content.prettyJsonOrRaw() else content.trim()
    }
    val highlighted = remember(display, isJson) { display.highlightStoreCode(isJson = isJson) }
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                Text(
                    text = if (isJson) "加入后可在主题列表中选择" else "安装后可在插件管理中启用",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
            }
            TextButton(
                onClick = {
                    scope.launch {
                        clipboard.setClipEntry(
                            ClipEntry(ClipData.newPlainText("预览代码", display)),
                        )
                        FishPiNotifier.success("已复制预览代码")
                    }
                },
                enabled = display.isNotBlank() && !loading && error == null,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = "复制预览代码", modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("复制", fontSize = 12.sp)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when {
                loading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("正在加载预览", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                error != null -> Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                display.isBlank() -> Text("暂无可预览内容", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                else -> SelectionContainer {
                    Text(
                        text = highlighted,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(verticalScroll)
                            .horizontalScroll(horizontalScroll),
                    )
                }
            }
        }
    }
}

@Composable
private fun StoreThemePreview(content: String) {
    val theme = remember(content) { content.parseThemeJson() }
    if (theme == null) return

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (theme.colors.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 1.dp),
            ) {
                items(theme.colors, key = { it.first }) { (name, color) ->
                    StoreThemeSwatch(name = name, color = color)
                }
            }
        }
    }
}

@Composable
private fun StoreThemeSwatch(name: String, color: Color) {
    Column(
        modifier = Modifier.width(50.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color),
        )
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StoreThemeChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StoreDetailRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.width(42.dp),
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StoreTypeIcon(icon: ImageVector, contentDescription: String) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.70f),
            modifier = Modifier.size(18.dp),
        )
    }
}

internal fun ExtensionStoreItem.displayName(): String =
    name.ifBlank { identifier }.ifBlank { "未命名扩展" }

private fun ExtensionStoreComment.displayName(): String =
    nickname.ifBlank { username }.ifBlank { authorId }.ifBlank { "匿名用户" }

private fun String.toStoreDateLabel(): String =
    take(10)

private fun ExtensionStoreItem.metaLine(): String =
    listOf(
        if (type == ExtensionStoreClient.TypeAppTheme) "主题" else "插件",
        author.ifBlank { null },
        version.ifBlank { null }?.let { "v$it" },
        priceLabel(),
    ).filterNotNull().joinToString(" · ")

private fun ExtensionStoreItem.pricePoints(): Int =
    price.trim().toIntOrNull() ?: 0

private fun ExtensionStoreItem.isFree(): Boolean =
    pricePoints() <= 0

private fun ExtensionStoreItem.priceLabel(): String =
    pricePoints().let { if (it <= 0) "免费" else "$it 积分" }

private fun validateUploadForm(
    name: String,
    identifier: String,
    price: String,
    type: String,
    code: String,
): String? {
    if (name.isBlank()) return "名称不能为空"
    if (!StoreIdentifierRegex.matches(identifier)) return "标识符至少 3 位，只能包含字母、数字、点、下划线或连字符"
    if (price.toIntOrNull() == null) return "价格必须是非负整数"
    if (code.isBlank()) return "请输入代码或配置内容"
    return when (type) {
        ExtensionStoreClient.TypeAppExtension -> {
            if (!PluginHeaderRegex.containsMatchIn(code)) {
                "APP 扩展必须包含 // ==FishPiPlugin== 到 // ==/FishPiPlugin== 头部声明"
            } else {
                null
            }
        }
        ExtensionStoreClient.TypeAppTheme -> {
            runCatching { JSONObject(code) }
                .fold(
                    onSuccess = { null },
                    onFailure = { "APP 主题必须是合法 JSON：${it.message ?: "解析失败"}" },
                )
        }
        else -> "只能发布 APP 扩展或 APP 主题"
    }
}

private fun defaultAppExtensionCode(): String =
    """
    // ==FishPiPlugin==
    // @name         我的插件
    // @author       你的名字
    // @version      1.0.0
    // @scenes       chatRoom
    // ==/FishPiPlugin==

    ui.notify('Hello in FishPi APP Extension!');
    log('plugin loaded');
    """.trimIndent()

private fun defaultAppThemeCode(): String =
    """
    {
      "schema": 1,
      "previewTemplate": "fishpi-mobile-v1",
      "name": "我的主题",
      "description": "适合 FishPi APP 的主题",
      "colorScheme": "light",
      "colors": {
        "base-100": "#F3F8FF",
        "base-200": "#FFFFFF",
        "base-300": "#E9F2FF",
        "base-content": "#08233F",
        "primary": "#08233F",
        "primary-content": "#FFFFFF",
        "secondary": "#0B5C93",
        "secondary-content": "#FFFFFF",
        "accent": "#7CFF52",
        "accent-content": "#08233F",
        "neutral": "#B7C4D3",
        "neutral-content": "#5D7188",
        "info": "#0B5C93",
        "success": "#42D94D",
        "warning": "#EAB308",
        "error": "#E53935"
      },
      "radius": {
        "radius-selector": 40,
        "radius-field": 18,
        "radius-box": 12
      },
      "spacing": {
        "page": 14,
        "section": 12,
        "item": 8,
        "control": 10
      },
      "border": {
        "border": 1,
        "opacity": 0.2
      },
      "depth": {
        "depth": 0.12
      },
      "wallpaper": {
        "image": "assets/wallpaper.png"
      }
    }
    """.trimIndent()

private data class StoreOwnership(
    val current: Boolean,
    val previous: Boolean,
)

private fun ExtensionStoreItem.actionState(ownership: StoreOwnership, currentInstalled: Boolean): StoreItemAction =
    when {
        currentInstalled -> StoreItemAction.Installed
        ownership.previous && !ownership.current -> StoreItemAction.Update
        ownership.current -> StoreItemAction.Install
        else -> StoreItemAction.Purchase
    }

private fun ExtensionStoreItem.themeActionState(
    ownership: StoreOwnership,
    saveState: StoreThemeSaveState,
): StoreItemAction =
    when {
        saveState == StoreThemeSaveState.SavedSameContent -> StoreItemAction.Installed
        saveState == StoreThemeSaveState.SavedDifferentContent -> StoreItemAction.Update
        ownership.previous && !ownership.current -> StoreItemAction.Update
        ownership.current -> StoreItemAction.Install
        else -> StoreItemAction.Purchase
    }

private fun ExtensionStoreItem.ownershipIn(
    purchases: List<ExtensionStoreItem>,
    versions: List<ExtensionStoreItem>,
): StoreOwnership {
    if (isPurchased) return StoreOwnership(current = true, previous = false)
    val purchasedIds = purchases.map { it.id }.toSet()
    val relatedIds = if (versions.isNotEmpty()) {
        versions.map { it.id }.toSet()
    } else {
        listOfNotNull(id, upgradeFromId).toSet()
    }
    val current = id in purchasedIds
    val previous = relatedIds.any { it != id && it in purchasedIds } ||
        upgradeFromId?.let { it in purchasedIds } == true
    return StoreOwnership(current = current, previous = previous)
}

private fun ExtensionStoreItem.isCurrentInstalled(
    pluginManager: PluginManager,
    loadedContent: String?,
): Boolean =
    if (type == ExtensionStoreClient.TypeAppTheme) {
        false
    } else {
        val pluginContent = loadedContent?.takeIf { it.isNotBlank() } ?: code
        pluginContent.isNotBlank() && pluginManager.storePluginMatchesSource(preferredStoreName(), pluginContent)
    }

private fun ExtensionStoreItem.preferredStoreName(): String =
    identifier.ifBlank { name }.ifBlank { "store-plugin-$id" }

private fun String.prettyJsonOrRaw(): String =
    runCatching {
        val trimmed = trim()
        when {
            trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
            trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
            else -> trimmed
        }
    }.getOrDefault(this)

private fun String.highlightStoreCode(isJson: Boolean): AnnotatedString {
    val keywordStyle = SpanStyle(color = Color(0xFF2563EB), fontWeight = FontWeight.SemiBold)
    val stringStyle = SpanStyle(color = Color(0xFF0F766E))
    val numberStyle = SpanStyle(color = Color(0xFFB45309))
    val commentStyle = SpanStyle(color = Color(0xFF6B7280))
    val literalStyle = SpanStyle(color = Color(0xFF7C3AED), fontWeight = FontWeight.SemiBold)
    val punctuationStyle = SpanStyle(color = Color(0xFF64748B))
    val builder = AnnotatedString.Builder()
    lines().forEachIndexed { lineIndex, line ->
        if (lineIndex > 0) builder.append('\n')
        if (isJson) {
            builder.appendHighlightedJsonLine(line, stringStyle, numberStyle, literalStyle, punctuationStyle)
        } else {
            builder.appendHighlightedJavaScriptLine(line, keywordStyle, stringStyle, numberStyle, commentStyle, literalStyle, punctuationStyle)
        }
    }
    return builder.toAnnotatedString()
}

private fun AnnotatedString.Builder.appendHighlightedJsonLine(
    line: String,
    stringStyle: SpanStyle,
    numberStyle: SpanStyle,
    literalStyle: SpanStyle,
    punctuationStyle: SpanStyle,
) {
    var index = 0
    while (index < line.length) {
        val char = line[index]
        when {
            char == '"' -> {
                val end = line.findStringEnd(index)
                withStyle(stringStyle) { append(line.substring(index, end)) }
                index = end
            }
            char.isDigit() || char == '-' -> {
                val end = line.scanWhile(index) { it.isDigit() || it == '-' || it == '.' }
                withStyle(numberStyle) { append(line.substring(index, end)) }
                index = end
            }
            line.startsWith("true", index) || line.startsWith("false", index) || line.startsWith("null", index) -> {
                val end = line.scanWhile(index) { it.isLetter() }
                withStyle(literalStyle) { append(line.substring(index, end)) }
                index = end
            }
            char in "{}[]:," -> {
                withStyle(punctuationStyle) { append(char) }
                index += 1
            }
            else -> {
                append(char)
                index += 1
            }
        }
    }
}

private fun AnnotatedString.Builder.appendHighlightedJavaScriptLine(
    line: String,
    keywordStyle: SpanStyle,
    stringStyle: SpanStyle,
    numberStyle: SpanStyle,
    commentStyle: SpanStyle,
    literalStyle: SpanStyle,
    punctuationStyle: SpanStyle,
) {
    val commentStart = line.indexOf("//")
    val codePart = if (commentStart >= 0) line.substring(0, commentStart) else line
    var index = 0
    while (index < codePart.length) {
        val char = codePart[index]
        when {
            char == '"' || char == '\'' || char == '`' -> {
                val end = codePart.findStringEnd(index)
                withStyle(stringStyle) { append(codePart.substring(index, end)) }
                index = end
            }
            char.isDigit() -> {
                val end = codePart.scanWhile(index) { it.isDigit() || it == '.' }
                withStyle(numberStyle) { append(codePart.substring(index, end)) }
                index = end
            }
            Character.isJavaIdentifierStart(char) -> {
                val end = codePart.scanWhile(index) { Character.isJavaIdentifierPart(it) }
                val token = codePart.substring(index, end)
                val style = when {
                    token in StoreJsKeywords -> keywordStyle
                    token in StoreJsLiterals -> literalStyle
                    else -> null
                }
                if (style != null) withStyle(style) { append(token) } else append(token)
                index = end
            }
            char in "{}[]().,;:+-*/%=!<>?" -> {
                withStyle(punctuationStyle) { append(char) }
                index += 1
            }
            else -> {
                append(char)
                index += 1
            }
        }
    }
    if (commentStart >= 0) {
        withStyle(commentStyle) { append(line.substring(commentStart)) }
    }
}

private val StoreJsKeywords = setOf(
    "async", "await", "break", "case", "catch", "class", "const", "continue", "default",
    "else", "export", "for", "from", "function", "if", "import", "let", "new", "return",
    "switch", "throw", "try", "var", "while",
)

private val StoreJsLiterals = setOf("true", "false", "null", "undefined", "this")

private fun String.findStringEnd(start: Int): Int {
    val quote = this[start]
    var index = start + 1
    var escaping = false
    while (index < length) {
        val char = this[index]
        if (escaping) {
            escaping = false
        } else if (char == '\\') {
            escaping = true
        } else if (char == quote) {
            return index + 1
        }
        index += 1
    }
    return length
}

private inline fun String.scanWhile(start: Int, predicate: (Char) -> Boolean): Int {
    var index = start
    while (index < length && predicate(this[index])) index += 1
    return index
}

private enum class StoreDiffKind {
    Same,
    Added,
    Removed,
}

private data class StoreDiffLine(
    val kind: StoreDiffKind,
    val text: String,
)

private fun buildStoreDiff(oldText: String, newText: String): List<StoreDiffLine> {
    val oldLines = oldText.lines()
    val newLines = newText.lines()
    if (oldLines == newLines) return emptyList()
    val table = Array(oldLines.size + 1) { IntArray(newLines.size + 1) }
    for (oldIndex in oldLines.indices.reversed()) {
        for (newIndex in newLines.indices.reversed()) {
            table[oldIndex][newIndex] = if (oldLines[oldIndex] == newLines[newIndex]) {
                table[oldIndex + 1][newIndex + 1] + 1
            } else {
                maxOf(table[oldIndex + 1][newIndex], table[oldIndex][newIndex + 1])
            }
        }
    }
    val result = mutableListOf<StoreDiffLine>()
    var oldIndex = 0
    var newIndex = 0
    while (oldIndex < oldLines.size && newIndex < newLines.size) {
        when {
            oldLines[oldIndex] == newLines[newIndex] -> {
                if (result.lastOrNull()?.kind != StoreDiffKind.Same) {
                    result += StoreDiffLine(StoreDiffKind.Same, "  ${oldLines[oldIndex]}")
                }
                oldIndex += 1
                newIndex += 1
            }
            table[oldIndex + 1][newIndex] >= table[oldIndex][newIndex + 1] -> {
                result += StoreDiffLine(StoreDiffKind.Removed, "- ${oldLines[oldIndex]}")
                oldIndex += 1
            }
            else -> {
                result += StoreDiffLine(StoreDiffKind.Added, "+ ${newLines[newIndex]}")
                newIndex += 1
            }
        }
    }
    while (oldIndex < oldLines.size) {
        result += StoreDiffLine(StoreDiffKind.Removed, "- ${oldLines[oldIndex]}")
        oldIndex += 1
    }
    while (newIndex < newLines.size) {
        result += StoreDiffLine(StoreDiffKind.Added, "+ ${newLines[newIndex]}")
        newIndex += 1
    }
    return result
}

private data class StoreThemePreviewData(
    val schema: Int,
    val previewTemplate: String,
    val colorScheme: String,
    val description: String,
    val colors: List<Pair<String, Color>>,
)

private fun String.parseThemeJson(): StoreThemePreviewData? =
    runCatching {
        val json = JSONObject(trim())
        val colorsJson = json.optJSONObject("colors") ?: JSONObject()
        val priority = listOf(
            "base-100",
            "base-200",
            "base-300",
            "base-content",
            "primary",
            "primary-content",
            "secondary",
            "secondary-content",
            "accent",
            "accent-content",
            "neutral",
            "neutral-content",
            "info",
            "success",
            "warning",
            "error",
        )
        val colorKeys = buildList {
            addAll(priority.filter { colorsJson.has(it) })
            val keys = colorsJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key !in this) add(key)
            }
        }
        StoreThemePreviewData(
            schema = json.optInt("schema", 1),
            previewTemplate = json.optString("previewTemplate"),
            colorScheme = json.optString("colorScheme"),
            description = json.optString("description"),
            colors = colorKeys.mapNotNull { key ->
                colorsJson.optString(key).takeIf { it.isNotBlank() }?.parseComposeColor()?.let { key to it }
            },
        )
    }.getOrNull()

private fun String.parseComposeColor(): Color? =
    runCatching { Color(android.graphics.Color.parseColor(this)) }.getOrNull()
