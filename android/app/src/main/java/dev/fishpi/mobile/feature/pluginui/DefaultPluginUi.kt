package dev.fishpi.mobile.feature.pluginui

import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.SubcomposeAsyncImage
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.chatui.ChatMarkdownRenderCache
import dev.fishpi.mobile.chatui.MarkwonContentRenderer
import dev.fishpi.mobile.chatui.MarkwonContentStyle
import dev.fishpi.mobile.rememberFishPiImageLoader
import dev.fishpi.mobile.ui.components.ActionChipButton
import dev.fishpi.mobile.ui.components.ContentCardSurface
import dev.fishpi.mobile.ui.components.ControlSurface
import dev.fishpi.mobile.ui.components.IconActionButton
import dev.fishpi.mobile.ui.components.PlainBackButton
import dev.fishpi.mobile.ui.components.UiLayerScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun DefaultPluginUi(
    state: PluginUiState,
    dispatch: (PluginUiAction) -> Unit,
) {
    val document = state.current?.takeIf { it.open } ?: return
    when (document.container) {
        PluginUiContainerType.Dialog,
        PluginUiContainerType.Sheet -> Dialog(
            onDismissRequest = { dispatch(PluginUiAction.Close(document.pluginId)) },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            PluginUiSurface(document = document, state = state, dispatch = dispatch)
        }
        PluginUiContainerType.Page -> {
            Dialog(
                onDismissRequest = { dispatch(PluginUiAction.Close(document.pluginId)) },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
            ) {
                PluginUiSurface(document = document, state = state, dispatch = dispatch)
            }
            BackHandler {
                dispatch(PluginUiAction.Close(document.pluginId))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PluginUiSurface(
    document: PluginUiDocument,
    state: PluginUiState,
    dispatch: (PluginUiAction) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ControlSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(
                        horizontal = FishPiTheme.spacingPage,
                        vertical = FishPiTheme.spacingItem,
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (document.container == PluginUiContainerType.Page) {
                        PlainBackButton(
                            onClick = { dispatch(PluginUiAction.Close(document.pluginId)) },
                            contentDescription = "关闭插件页面",
                        )
                    } else {
                        IconActionButton(
                            icon = Icons.Rounded.Close,
                            contentDescription = "关闭插件页面",
                            onClick = { dispatch(PluginUiAction.Close(document.pluginId)) },
                        )
                    }
                    Text(
                        text = document.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
    ) { innerPadding ->
        UiLayerScaffold {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(
                        horizontal = FishPiTheme.spacingPage,
                        vertical = FishPiTheme.spacingSection,
                    ),
                verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem),
            ) {
                document.error?.let {
                    item { PluginErrorText(it) }
                }
                if (document.nodes.isEmpty()) {
                    item { PluginEmptyText("暂无插件内容") }
                }
                items(document.nodes, key = { it.id }) { node ->
                    PluginUiNodeView(node = node, state = state, dispatch = dispatch)
                }
            }
        }
    }
}

@Composable
private fun PluginUiNodeView(
    node: PluginUiNode,
    state: PluginUiState,
    dispatch: (PluginUiAction) -> Unit,
) {
    when (node) {
        is PluginUiNode.Text -> Text(
            text = node.text,
            color = FishPiTheme.onSurface,
            style = if (node.style == "title") MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (node.style == "title") FontWeight.SemiBold else FontWeight.Normal,
        )
        is PluginUiNode.Markdown -> PluginMarkdown(node.text, node.id)
        is PluginUiNode.Image -> PluginImage(node)
        is PluginUiNode.Divider -> HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.7f))
        is PluginUiNode.Space -> Spacer(modifier = Modifier.height(node.height.dp))
        is PluginUiNode.Json -> PluginJson(node.json)
        is PluginUiNode.Loading -> Text(node.text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        is PluginUiNode.Error -> PluginErrorText(node.text)
        is PluginUiNode.Empty -> PluginEmptyText(node.text)
        is PluginUiNode.Card -> PluginCard(node, state, dispatch)
        is PluginUiNode.Section -> PluginSection(node, state, dispatch)
        is PluginUiNode.Row -> PluginRow(node, state, dispatch)
        is PluginUiNode.Columns -> PluginRow(PluginUiNode.Row(node.id, node.children), state, dispatch)
        is PluginUiNode.Tabs -> PluginTabs(node, state, dispatch)
        is PluginUiNode.Input -> PluginInput(node, state, dispatch)
        is PluginUiNode.Number -> PluginNumber(node, state, dispatch)
        is PluginUiNode.Switch -> PluginSwitch(node, state, dispatch)
        is PluginUiNode.Select -> PluginSelect(node, state, dispatch)
        is PluginUiNode.Chips -> PluginChips(node, state, dispatch)
        is PluginUiNode.Slider -> PluginSlider(node, state, dispatch)
        is PluginUiNode.ListNode -> PluginList(node, dispatch)
        is PluginUiNode.Table -> PluginTable(node)
        is PluginUiNode.Stat -> PluginStat(node)
        is PluginUiNode.UserCard -> PluginUserCard(node, dispatch)
        is PluginUiNode.ArticleCard -> PluginArticleCard(node, dispatch)
        is PluginUiNode.ActionBar -> PluginActionBar(node, dispatch)
        is PluginUiNode.Button -> ActionChipButton(
            text = node.label,
            onClick = { dispatch(PluginUiAction.TriggerAction(node.actionId, node.id)) },
            enabled = node.enabled,
            selected = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PluginMarkdown(text: String, key: String) {
    val context = LocalContext.current
    val cache = remember { ChatMarkdownRenderCache(maxEntries = 40, maxChars = 120_000) }
    val scope = remember { kotlinx.coroutines.CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    val style = MarkwonContentStyle(
        textColor = FishPiTheme.onSurface.toArgb(),
        weakTextColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb(),
        accentColor = MaterialTheme.colorScheme.primary.toArgb(),
        codeBackgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh.toArgb(),
        textSizeSp = 15f,
        lineSpacingMultiplier = 1.24f,
    )
    val renderer = remember(context, style, cache, scope) {
        MarkwonContentRenderer(context, style, cache, scope, onLinkClick = {}, onMentionClick = {})
    }
    DisposableEffect(scope, cache) {
        onDispose {
            scope.cancel()
            cache.clear()
        }
    }
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { TextView(it) },
        update = { view -> renderer.renderInto(view, "plugin-md-$key-${text.hashCode()}", text) },
        onRelease = { view -> renderer.clear(view) },
    )
}

@Composable
private fun PluginImage(node: PluginUiNode.Image) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SubcomposeAsyncImage(
            model = node.url,
            imageLoader = rememberFishPiImageLoader(),
            contentDescription = node.caption.ifBlank { "插件图片" },
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp, max = 320.dp)
                .clip(RoundedCornerShape(FishPiTheme.radiusBox))
                .background(MaterialTheme.colorScheme.surfaceContainer),
        )
        if (node.caption.isNotBlank()) {
            Text(node.caption, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable private fun PluginJson(json: String) {
    ContentCardSurface(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(FishPiTheme.spacingControl)) {
        Text(
            text = json,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable private fun PluginErrorText(text: String) {
    Text(text, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth())
}

@Composable private fun PluginEmptyText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = FishPiTheme.spacingSection * 2f),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun PluginCard(node: PluginUiNode.Card, state: PluginUiState, dispatch: (PluginUiAction) -> Unit) {
    ContentCardSurface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (node.actionId.isNotBlank()) Modifier.clickable { dispatch(PluginUiAction.TriggerAction(node.actionId, node.id)) } else Modifier),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(FishPiTheme.spacingSection),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (node.title.isNotBlank()) Text(node.title, fontWeight = FontWeight.SemiBold, color = FishPiTheme.onSurface)
            if (node.subtitle.isNotBlank()) Text(node.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            node.children.forEach { PluginUiNodeView(it, state, dispatch) }
        }
    }
}

@Composable private fun PluginSection(node: PluginUiNode.Section, state: PluginUiState, dispatch: (PluginUiAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (node.title.isNotBlank()) Text(node.title, fontWeight = FontWeight.SemiBold, color = FishPiTheme.onSurface)
        node.children.forEach { PluginUiNodeView(it, state, dispatch) }
    }
}

@Composable private fun PluginRow(node: PluginUiNode.Row, state: PluginUiState, dispatch: (PluginUiAction) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        node.children.forEach { child ->
            Box(modifier = Modifier.weight(1f)) { PluginUiNodeView(child, state, dispatch) }
        }
    }
}

@Composable private fun PluginTabs(node: PluginUiNode.Tabs, state: PluginUiState, dispatch: (PluginUiAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            node.tabs.forEach { tab -> FilterChip(selected = false, onClick = {}, label = { Text(tab.label) }) }
        }
        node.tabs.firstOrNull()?.children?.forEach { PluginUiNodeView(it, state, dispatch) }
    }
}

@Composable private fun PluginInput(node: PluginUiNode.Input, state: PluginUiState, dispatch: (PluginUiAction) -> Unit) {
    val value = (state.form.values[node.name] as? PluginFormValue.Text)?.value ?: node.value
    OutlinedTextField(
        value = value,
        onValueChange = { dispatch(PluginUiAction.ChangeText(node.name, it)) },
        label = { Text(node.label) },
        placeholder = { Text(node.placeholder) },
        minLines = if (node.multiline) 4 else 1,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable private fun PluginNumber(node: PluginUiNode.Number, state: PluginUiState, dispatch: (PluginUiAction) -> Unit) {
    val value = ((state.form.values[node.name] as? PluginFormValue.Number)?.value ?: node.value).toString().removeSuffix(".0")
    OutlinedTextField(
        value = value,
        onValueChange = { dispatch(PluginUiAction.ChangeNumber(node.name, it.toDoubleOrNull() ?: 0.0)) },
        label = { Text(node.label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable private fun PluginSwitch(node: PluginUiNode.Switch, state: PluginUiState, dispatch: (PluginUiAction) -> Unit) {
    val checked = (state.form.values[node.name] as? PluginFormValue.Bool)?.value ?: node.checked
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(node.label, modifier = Modifier.weight(1f), color = FishPiTheme.onSurface)
        Switch(checked = checked, onCheckedChange = { dispatch(PluginUiAction.ChangeBool(node.name, it)) })
    }
}

@Composable private fun PluginSelect(node: PluginUiNode.Select, state: PluginUiState, dispatch: (PluginUiAction) -> Unit) {
    val value = (state.form.values[node.name] as? PluginFormValue.Text)?.value ?: node.value
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(node.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            node.options.forEach { option ->
                FilterChip(
                    selected = value == option.value,
                    onClick = { dispatch(PluginUiAction.ChangeText(node.name, option.value)) },
                    label = { Text(option.label) },
                )
            }
        }
    }
}

@Composable private fun PluginChips(node: PluginUiNode.Chips, state: PluginUiState, dispatch: (PluginUiAction) -> Unit) {
    val values = (state.form.values[node.name] as? PluginFormValue.Strings)?.value ?: node.values
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        node.options.forEach { option ->
            FilterChip(
                selected = option.value in values,
                onClick = {
                    val next = if (option.value in values) values - option.value else values + option.value
                    dispatch(PluginUiAction.ChangeStrings(node.name, next))
                },
                label = { Text(option.label) },
            )
        }
    }
}

@Composable private fun PluginSlider(node: PluginUiNode.Slider, state: PluginUiState, dispatch: (PluginUiAction) -> Unit) {
    val value = ((state.form.values[node.name] as? PluginFormValue.Number)?.value?.toFloat() ?: node.value).coerceIn(node.min, node.max)
    Column {
        Text("${node.label}: ${value.toInt()}", color = FishPiTheme.onSurface)
        Slider(value = value, onValueChange = { dispatch(PluginUiAction.ChangeNumber(node.name, it.toDouble())) }, valueRange = node.min..node.max)
    }
}

@Composable private fun PluginList(node: PluginUiNode.ListNode, dispatch: (PluginUiAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        node.items.forEach { item ->
            ContentCardSurface(
                modifier = Modifier.fillMaxWidth(),
                onClick = if (item.actionId.isNotBlank()) ({ dispatch(PluginUiAction.TriggerAction(item.actionId, item.id)) }) else null,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(FishPiTheme.spacingSection),
            ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.title, color = FishPiTheme.onSurface, fontWeight = FontWeight.Medium)
                    if (item.subtitle.isNotBlank()) Text(item.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
            }
        }
    }
}

@Composable private fun PluginTable(node: PluginUiNode.Table) {
    Column(Modifier.horizontalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (node.headers.isNotEmpty()) Text(node.headers.joinToString("    "), fontWeight = FontWeight.SemiBold)
        node.rows.forEach { Text(it.joinToString("    "), color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable private fun PluginStat(node: PluginUiNode.Stat) {
    ContentCardSurface(contentPadding = androidx.compose.foundation.layout.PaddingValues(FishPiTheme.spacingSection)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(node.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(node.value, color = FishPiTheme.onSurface, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (node.detail.isNotBlank()) Text(node.detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun PluginUserCard(node: PluginUiNode.UserCard, dispatch: (PluginUiAction) -> Unit) {
    ContentCardSurface(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (node.actionId.isNotBlank()) ({ dispatch(PluginUiAction.TriggerAction(node.actionId, node.id)) }) else null,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(FishPiTheme.spacingSection),
    ) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            if (node.avatar.isNotBlank()) {
                SubcomposeAsyncImage(model = node.avatar, imageLoader = rememberFishPiImageLoader(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else Icon(Icons.Rounded.Person, contentDescription = null)
        }
        Column {
            Text(node.displayName.ifBlank { node.username }, fontWeight = FontWeight.SemiBold)
            Text(node.username, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
    }
}

@Composable private fun PluginArticleCard(node: PluginUiNode.ArticleCard, dispatch: (PluginUiAction) -> Unit) {
    ContentCardSurface(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (node.actionId.isNotBlank()) ({ dispatch(PluginUiAction.TriggerAction(node.actionId, node.id)) }) else null,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(FishPiTheme.spacingSection),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.AutoMirrored.Rounded.Article, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(node.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (node.preview.isNotBlank()) Text(node.preview, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable private fun PluginActionBar(node: PluginUiNode.ActionBar, dispatch: (PluginUiAction) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        node.actions.forEach { action ->
            ActionChipButton(
                text = action.label,
                onClick = { dispatch(PluginUiAction.TriggerAction(action.actionId, action.id)) },
                enabled = action.enabled,
                modifier = Modifier.weight(1f).widthIn(min = 0.dp),
            )
        }
    }
}

