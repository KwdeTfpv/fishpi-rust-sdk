package dev.fishpi.mobile.feature.emoji

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import dev.fishpi.mobile.FishPiErrorRed
import dev.fishpi.mobile.FishPiNotifier
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.data.EmojiItemView
import dev.fishpi.mobile.data.FishPiApiClient
import dev.fishpi.mobile.rememberFishPiImageLoader
import dev.fishpi.mobile.ui.components.FishPiScreenHeader
import dev.fishpi.mobile.ui.components.IconActionButton
import dev.fishpi.mobile.ui.components.UiLayerScaffold
import dev.fishpi.mobile.ui.theme.FishPiType

@Composable
internal fun EmojiManagerScreen(
    apiKey: String,
    onDismiss: () -> Unit,
    api: FishPiApiClient = FishPiApiClient.shared,
) {
    val scope = rememberCoroutineScope()
    val state = remember(apiKey) { EmojiManagerState(api, apiKey) }
    var groupEditor by remember { mutableStateOf<GroupEditorTarget?>(null) }
    var itemEditor by remember { mutableStateOf<EmojiItemView?>(null) }
    var movingItem by remember { mutableStateOf<EmojiItemView?>(null) }
    var pendingDeleteGroup by remember { mutableStateOf(false) }
    var addingEmoji by remember { mutableStateOf(false) }

    LaunchedEffect(apiKey) { state.loadGroups(scope) }
    LaunchedEffect(state.message) {
        state.message?.let {
            FishPiNotifier.success(it)
            state.consumeMessage()
        }
    }
    BackHandler(onBack = onDismiss)

    UiLayerScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = FishPiTheme.spacingPage),
            verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem),
        ) {
            FishPiScreenHeader(
                title = "表情包",
                subtitle = state.selectedGroup?.name?.takeIf { it.isNotBlank() },
                onNavigationClick = onDismiss,
                actions = {
                    IconActionButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = "新建分组",
                        enabled = !state.busy,
                        onClick = { groupEditor = GroupEditorTarget.Create },
                    )
                },
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem * 0.64f)) {
                items(state.groups, key = { it.id }) { group ->
                    GroupChip(
                        name = group.name.ifBlank { "表情" },
                        selected = group.id == state.selectedGroupId,
                        onClick = { state.selectGroup(scope, group.id) },
                    )
                }
            }

            if (state.selectedGroupId.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem * 0.5f)) {
                    ActionChip(
                        label = "添加表情",
                        enabled = !state.busy,
                        onClick = { addingEmoji = true },
                    )
                    if (state.canEditSelectedGroup) {
                        ActionChip(
                            label = "重命名分组",
                            enabled = !state.busy,
                            onClick = {
                                groupEditor = GroupEditorTarget.Rename(
                                    state.selectedGroupId,
                                    state.selectedGroup?.name.orEmpty(),
                                )
                            },
                        )
                        ActionChip(
                            label = "删除分组",
                            enabled = !state.busy,
                            danger = true,
                            onClick = { pendingDeleteGroup = true },
                        )
                    }
                }
            }

            state.error?.let { reason ->
                Text(text = reason, style = FishPiType.caption, color = FishPiErrorRed)
            }

            when {
                state.loadingGroups || state.loadingItems -> CenterHint(loading = true, text = "正在加载...")
                state.groups.isEmpty() -> CenterHint(text = "还没有表情包分组")
                state.items.isEmpty() -> CenterHint(text = "这个分组还没有表情")
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem * 0.75f),
                    verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem * 0.75f),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.items, key = { it.id }) { item ->
                        EmojiCell(
                            item = item,
                            enabled = !state.busy,
                            onRename = { itemEditor = item },
                            onMove = { movingItem = item },
                            onRemove = { state.removeEmoji(scope, item) },
                        )
                    }
                }
            }
        }
    }

    groupEditor?.let { target ->
        TextInputDialog(
            title = if (target is GroupEditorTarget.Create) "新建分组" else "重命名分组",
            initial = (target as? GroupEditorTarget.Rename)?.name.orEmpty(),
            placeholder = "分组名称",
            confirmLabel = if (target is GroupEditorTarget.Create) "创建" else "保存",
            onDismiss = { groupEditor = null },
            onConfirm = { value ->
                when (target) {
                    GroupEditorTarget.Create -> state.createGroup(scope, value)
                    is GroupEditorTarget.Rename -> state.renameGroup(scope, target.groupId, value)
                }
                groupEditor = null
            },
        )
    }

    if (addingEmoji) {
        TextInputDialog(
            title = "添加表情",
            initial = "",
            placeholder = "图片链接",
            confirmLabel = "添加",
            onDismiss = { addingEmoji = false },
            onConfirm = { value ->
                state.addEmojiUrl(scope, state.selectedGroupId, value)
                addingEmoji = false
            },
        )
    }

    itemEditor?.let { item ->
        TextInputDialog(
            title = "重命名表情",
            initial = item.name,
            placeholder = "表情名称",
            confirmLabel = "保存",
            onDismiss = { itemEditor = null },
            onConfirm = { value ->
                state.renameEmoji(scope, item, value)
                itemEditor = null
            },
        )
    }

    movingItem?.let { item ->
        val targets = state.groups.filter { it.id != item.groupId }
        AlertDialog(
            onDismissRequest = { movingItem = null },
            containerColor = FishPiTheme.surface.copy(alpha = FishPiTheme.glass.floatingAlpha),
            tonalElevation = 0.dp,
            title = { Text("移动到分组", style = FishPiType.heading) },
            text = {
                if (targets.isEmpty()) {
                    Text("没有其他分组", style = FishPiType.secondary, color = FishPiTheme.weakText)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem * 0.5f)) {
                        targets.forEach { group ->
                            Text(
                                text = group.name.ifBlank { "表情" },
                                style = FishPiType.body,
                                color = FishPiTheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(FishPiTheme.radiusField))
                                    .clickable {
                                        state.moveEmoji(scope, item, group.id)
                                        movingItem = null
                                    }
                                    .padding(
                                        horizontal = FishPiTheme.spacingControl,
                                        vertical = FishPiTheme.spacingControl * 0.75f,
                                    ),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { movingItem = null }) { Text("取消") }
            },
        )
    }

    if (pendingDeleteGroup) {
        AlertDialog(
            onDismissRequest = { pendingDeleteGroup = false },
            containerColor = FishPiTheme.surface.copy(alpha = FishPiTheme.glass.floatingAlpha),
            tonalElevation = 0.dp,
            title = { Text("删除分组", style = FishPiType.heading) },
            text = {
                Text(
                    "删除「${state.selectedGroup?.name.orEmpty()}」后，组内表情将不再归入该分组。",
                    style = FishPiType.secondary,
                    color = FishPiTheme.weakText,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.deleteGroup(scope, state.selectedGroupId)
                        pendingDeleteGroup = false
                    },
                ) {
                    Text("删除", color = FishPiErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteGroup = false }) { Text("取消") }
            },
        )
    }
}

private sealed interface GroupEditorTarget {
    data object Create : GroupEditorTarget
    data class Rename(val groupId: String, val name: String) : GroupEditorTarget
}

@Composable
private fun EmojiCell(
    item: EmojiItemView,
    enabled: Boolean,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onRemove: () -> Unit,
) {
    var actionsOpen by remember(item.id) { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(FishPiTheme.radiusBox))
            .background(FishPiTheme.surfaceContainer.copy(alpha = 0.42f))
            .clickable(enabled = enabled) { actionsOpen = !actionsOpen },
    ) {
        SubcomposeAsyncImage(
            model = item.url,
            imageLoader = rememberFishPiImageLoader(),
            contentDescription = item.name.ifBlank { "表情" },
            contentScale = ContentScale.Crop,
            loading = {},
            error = { Text("表情", style = FishPiType.caption, color = FishPiTheme.weakText) },
            modifier = Modifier.fillMaxSize(),
        )
        if (actionsOpen) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FishPiTheme.surface.copy(alpha = 0.82f)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    CellAction(Icons.Rounded.Edit, "重命名") { actionsOpen = false; onRename() }
                    CellAction(Icons.AutoMirrored.Rounded.DriveFileMove, "移动") { actionsOpen = false; onMove() }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    CellAction(Icons.Rounded.DeleteOutline, "移除", danger = true) {
                        actionsOpen = false
                        onRemove()
                    }
                    CellAction(Icons.Rounded.Close, "取消") { actionsOpen = false }
                }
            }
        }
    }
}

@Composable
private fun CellAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(FishPiTheme.radiusField * 0.66f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (danger) FishPiErrorRed else FishPiTheme.onSurface,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun GroupChip(name: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(FishPiTheme.radiusField * 0.66f))
            .background(
                if (selected) FishPiTheme.accent.copy(alpha = 0.14f)
                else FishPiTheme.surfaceContainer.copy(alpha = 0.42f),
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = FishPiTheme.spacingItem,
                vertical = FishPiTheme.spacingItem * 0.38f,
            ),
    ) {
        Text(
            text = name,
            style = FishPiType.caption,
            color = if (selected) FishPiTheme.accent else FishPiTheme.weakText,
            maxLines = 1,
        )
    }
}

@Composable
private fun ActionChip(
    label: String,
    enabled: Boolean,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(FishPiTheme.radiusField * 0.66f))
            .background(FishPiTheme.surfaceContainer.copy(alpha = 0.42f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = FishPiTheme.spacingItem * 0.75f,
                vertical = FishPiTheme.spacingItem * 0.32f,
            ),
    ) {
        Text(
            text = label,
            style = FishPiType.label,
            color = when {
                !enabled -> FishPiTheme.weakText.copy(alpha = 0.44f)
                danger -> FishPiErrorRed
                else -> FishPiTheme.onSurface
            },
        )
    }
}

@Composable
private fun CenterHint(text: String, loading: Boolean = false) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem * 0.5f),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = FishPiTheme.accent,
                )
            }
            Text(text = text, style = FishPiType.secondary, color = FishPiTheme.weakText)
        }
    }
}

@Composable
private fun TextInputDialog(
    title: String,
    initial: String,
    placeholder: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FishPiTheme.surface.copy(alpha = FishPiTheme.glass.floatingAlpha),
        tonalElevation = 0.dp,
        title = { Text(title, style = FishPiType.heading) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(FishPiTheme.radiusField))
                    .background(FishPiTheme.surfaceContainer.copy(alpha = 0.42f))
                    .padding(
                        horizontal = FishPiTheme.spacingControl,
                        vertical = FishPiTheme.spacingControl * 0.75f,
                    ),
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    textStyle = TextStyle(color = FishPiTheme.onSurface),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (value.isEmpty()) {
                            Text(placeholder, style = FishPiType.body, color = FishPiTheme.weakText)
                        }
                        inner()
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = value.isNotBlank(),
                onClick = { onConfirm(value) },
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
