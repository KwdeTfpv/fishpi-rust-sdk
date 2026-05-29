package dev.fishpi.mobile.ui.components

import dev.fishpi.mobile.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import dev.fishpi.mobile.data.EmojiGroupView
import dev.fishpi.mobile.data.EmojiItemView
import dev.fishpi.mobile.data.FishPiApiClient
import dev.fishpi.mobile.utils.launchIoCatching
import kotlinx.coroutines.CoroutineScope

internal class EmojiPanelState {
    var open by mutableStateOf(false)
    var groups by mutableStateOf<List<EmojiGroupView>>(emptyList())
    var items by mutableStateOf<List<EmojiItemView>>(emptyList())
    var selectedGroupId by mutableStateOf("")
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun close() {
        open = false
    }

    fun toggle(onOpening: () -> Unit = {}): Boolean {
        val targetOpen = !open
        if (targetOpen) {
            onOpening()
        }
        open = targetOpen
        return targetOpen
    }

    private fun beginGroupLoading(groupId: String): Boolean {
        if (groupId.isBlank()) return false
        selectedGroupId = groupId
        loading = true
        error = null
        return true
    }

    private fun completeGroupLoading(groupId: String, nextItems: List<EmojiItemView>) {
        if (selectedGroupId == groupId) {
            items = nextItems
            loading = false
        }
    }

    private fun failGroupLoading(groupId: String, reason: String) {
        if (selectedGroupId == groupId) {
            error = reason
            loading = false
        }
    }

    private fun beginGroupsLoading() {
        loading = true
        error = null
    }

    private fun completeGroupsLoading(nextGroups: List<EmojiGroupView>): String? {
        groups = nextGroups
        return nextGroups.firstOrNull()?.id.also { firstGroupId ->
            if (firstGroupId.isNullOrBlank()) {
                loading = false
            }
        }
    }

    private fun failGroupsLoading(reason: String) {
        error = reason
        loading = false
    }

    fun loadGroup(
        scope: CoroutineScope,
        api: FishPiApiClient,
        apiKey: String,
        groupId: String,
    ) {
        if (!beginGroupLoading(groupId)) return
        scope.launchIoCatching(
            block = { api.getEmojiGroupItems(apiKey, groupId) },
            onSuccess = { completeGroupLoading(groupId, it) },
            onFailure = { failGroupLoading(groupId, it.message ?: "加载表情包失败") },
        )
    }

    fun toggleAndLoad(
        scope: CoroutineScope,
        api: FishPiApiClient,
        apiKey: String,
        onOpening: () -> Unit = {},
    ) {
        val targetOpen = toggle(onOpening)
        if (!targetOpen || groups.isNotEmpty()) return
        beginGroupsLoading()
        scope.launchIoCatching(
            block = { api.getEmojiGroups(apiKey) },
            onSuccess = { loadedGroups ->
                completeGroupsLoading(loadedGroups)?.let { firstGroupId ->
                    loadGroup(scope, api, apiKey, firstGroupId)
                }
            },
            onFailure = { failGroupsLoading(it.message ?: "加载表情包分组失败") },
        )
    }
}

@Composable
internal fun rememberEmojiPanelState(vararg keys: Any?): EmojiPanelState =
    remember(*keys) { EmojiPanelState() }

@Composable
internal fun EmojiPackPanel(
    groups: List<EmojiGroupView>,
    emojiItems: List<EmojiItemView>,
    selectedGroupId: String,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onPickGroup: (String) -> Unit,
    onPickEmoji: (EmojiItemView) -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 206.dp,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FishPiTheme.radiusBox))
            .background(FishPiTheme.surface.copy(alpha = 0.96f))
            .padding(
                horizontal = FishPiTheme.spacingItem,
                vertical = FishPiTheme.spacingItem * 0.75f,
            ),
        verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem * 0.75f),
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem * 0.64f)) {
            items(groups, key = { it.id }) { group ->
                EmojiGroupChip(
                    group = group,
                    selected = group.id == selectedGroupId,
                    onClick = { onPickGroup(group.id) },
                )
            }
        }
        when {
            isLoading -> Text(text = "正在加载表情包...", color = FishPiTheme.weakText)
            error != null -> Text(text = error, color = FishPiErrorRed)
            groups.isEmpty() -> Text(text = "暂无表情包分组", color = FishPiTheme.weakText)
            emojiItems.isEmpty() -> Text(text = "这个分组还没有表情", color = FishPiTheme.weakText)
            else -> {
                val rows = remember(emojiItems) { emojiItems.asReversed().chunked(5) }
                LazyColumn(
                    modifier = Modifier.heightIn(max = maxHeight),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(rows) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { item ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(FishPiTheme.radiusBox))
                                        .background(FishPiTheme.surfaceContainer)
                                        .clickable { onPickEmoji(item) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    SubcomposeAsyncImage(
                                        model = item.url,
                                        imageLoader = rememberFishPiImageLoader(),
                                        contentDescription = item.name.ifBlank { "表情" },
                                        contentScale = ContentScale.Crop,
                                        loading = { Text(text = "鱼", color = FishPiTheme.weakText) },
                                        error = {
                                            Text(text = "表情", color = FishPiTheme.weakText, fontSize = 12.sp)
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                            repeat(5 - row.size) {
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmojiGroupChip(
    group: EmojiGroupView,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(FishPiTheme.radiusField * 0.66f))
            .background(
                if (selected) FishPiTheme.accent.copy(alpha = 0.14f)
                else FishPiTheme.surfaceContainer,
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = FishPiTheme.spacingItem,
                vertical = FishPiTheme.spacingItem * 0.38f,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = group.name.ifBlank { "表情" },
            color = if (selected) FishPiTheme.accent else FishPiTheme.weakText,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}


