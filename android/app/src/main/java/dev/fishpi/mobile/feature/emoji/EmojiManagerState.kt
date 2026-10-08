package dev.fishpi.mobile.feature.emoji

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.fishpi.mobile.data.EmojiGroupView
import dev.fishpi.mobile.data.EmojiItemView
import dev.fishpi.mobile.data.FishPiApiClient
import dev.fishpi.mobile.utils.launchIoCatching
import kotlinx.coroutines.CoroutineScope


@Stable
internal class EmojiManagerState(
    private val api: FishPiApiClient,
    private val apiKey: String,
) {
    var groups by mutableStateOf<List<EmojiGroupView>>(emptyList())
        private set
    var selectedGroupId by mutableStateOf("")
        private set
    var loadingGroups by mutableStateOf(false)
        private set
    var loadingItems by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    private val itemCache = mutableMapOf<String, List<EmojiItemView>>()
    var items by mutableStateOf<List<EmojiItemView>>(emptyList())
        private set

    val selectedGroup: EmojiGroupView?
        get() = groups.firstOrNull { it.id == selectedGroupId }

    val canEditSelectedGroup: Boolean
        get() = selectedGroup?.let { !it.isDefault } == true

    fun consumeMessage() {
        message = null
    }

    fun clearError() {
        error = null
    }

    fun loadGroups(scope: CoroutineScope, preserveSelection: Boolean = false) {
        if (loadingGroups) return
        loadingGroups = true
        error = null
        val previous = selectedGroupId
        scope.launchIoCatching(
            block = { api.getEmojiGroups(apiKey) },
            onSuccess = { loaded ->
                groups = loaded
                val target = previous.takeIf { preserveSelection && loaded.any { g -> g.id == it } }
                    ?: loaded.firstOrNull()?.id.orEmpty()
                loadingGroups = false
                if (target.isNotBlank()) {
                    selectGroup(scope, target)
                } else {
                    items = emptyList()
                }
            },
            onFailure = {
                loadingGroups = false
                error = it.message ?: "加载表情包分组失败"
            },
        )
    }

    fun selectGroup(scope: CoroutineScope, groupId: String, forceReload: Boolean = false) {
        if (groupId.isBlank()) return
        selectedGroupId = groupId
        error = null
        val cached = itemCache[groupId]
        if (cached != null && !forceReload) {
            items = cached
            loadingItems = false
            return
        }
        items = emptyList()
        loadingItems = true
        scope.launchIoCatching(
            block = { api.getEmojiGroupItems(apiKey, groupId) },
            onSuccess = { loaded ->
                itemCache[groupId] = loaded
                if (selectedGroupId == groupId) {
                    items = loaded
                    loadingItems = false
                }
            },
            onFailure = {
                if (selectedGroupId == groupId) {
                    loadingItems = false
                    error = it.message ?: "加载表情包失败"
                }
            },
        )
    }

    fun createGroup(scope: CoroutineScope, name: String) {
        val label = name.trim()
        if (label.isBlank() || busy) return
        runWrite(scope, "已创建分组「$label」", failure = "创建分组失败") {
            api.createEmojiGroup(apiKey, label, nextGroupSort())
        }
    }

    fun renameGroup(scope: CoroutineScope, groupId: String, name: String) {
        val label = name.trim()
        val target = groups.firstOrNull { it.id == groupId } ?: return
        if (label.isBlank() || busy || target.isDefault) return
        runWrite(scope, "已重命名为「$label」", failure = "重命名失败") {
            api.updateEmojiGroup(apiKey, groupId, label, target.sortOrZero())
        }
    }

    fun deleteGroup(scope: CoroutineScope, groupId: String) {
        val target = groups.firstOrNull { it.id == groupId } ?: return
        if (busy || target.isDefault) return
        itemCache.remove(groupId)
        runWrite(scope, "已删除分组", failure = "删除分组失败", preserveSelection = false) {
            api.deleteEmojiGroup(apiKey, groupId)
        }
    }

    fun addEmojiUrl(scope: CoroutineScope, groupId: String, url: String, name: String = "") {
        val link = url.trim()
        if (link.isBlank() || busy) return
        val target = groups.firstOrNull { it.id == groupId } ?: return
        itemCache.remove(groupId)
        runWrite(scope, "已添加表情", failure = "添加表情失败") {
            // 「全部」分组走 upload 接口，自定义分组走 add-url。
            if (target.isDefault) {
                api.uploadEmojiUrl(apiKey, link)
            } else {
                api.addEmojiUrlToGroup(apiKey, groupId, link, nextItemSort(groupId), name.trim())
            }
        }
    }

    fun removeEmoji(scope: CoroutineScope, item: EmojiItemView) {
        if (busy) return
        val emojiId = item.emojiId.ifBlank { item.id }
        if (emojiId.isBlank()) return
        val groupId = item.resolveGroupId()
        itemCache.remove(groupId)
        runWrite(scope, "已移除表情", failure = "移除表情失败") {
            api.removeEmojiFromGroup(apiKey, groupId, emojiId)
        }
    }

    fun renameEmoji(scope: CoroutineScope, item: EmojiItemView, name: String) {
        val label = name.trim()
        if (busy) return
        val groupId = item.resolveGroupId()
        itemCache.remove(groupId)
        runWrite(scope, "已重命名", failure = "重命名失败") {
            api.updateEmojiItem(apiKey, item.id, groupId, label, item.sortOrZero())
        }
    }

    fun moveEmoji(scope: CoroutineScope, item: EmojiItemView, targetGroupId: String) {
        val sourceGroupId = item.resolveGroupId()
        if (busy || targetGroupId.isBlank() || targetGroupId == sourceGroupId) return
        itemCache.remove(sourceGroupId)
        itemCache.remove(targetGroupId)
        val targetName = groups.firstOrNull { it.id == targetGroupId }?.name.orEmpty()
        runWrite(scope, "已移动到「$targetName」", failure = "移动表情失败") {
            api.updateEmojiItem(apiKey, item.id, targetGroupId, item.name, item.sortOrZero())
        }
    }

    private fun runWrite(
        scope: CoroutineScope,
        success: String,
        failure: String,
        preserveSelection: Boolean = true,
        block: suspend () -> Unit,
    ) {
        busy = true
        error = null
        scope.launchIoCatching(
            block = block,
            onSuccess = {
                busy = false
                message = success
                loadGroups(scope, preserveSelection = preserveSelection)
            },
            onFailure = {
                busy = false
                error = it.message ?: failure
            },
        )
    }

    // 服务端 group/emojis 不保证回传 groupId，退回到当前选中的分组。
    private fun EmojiItemView.resolveGroupId(): String =
        groupId.ifBlank { selectedGroupId }

    private fun nextGroupSort(): Long =
        (groups.mapNotNull { it.sort.takeIf { s -> s != Long.MAX_VALUE } }.maxOrNull() ?: 0L) + 1L

    private fun nextItemSort(groupId: String): Long =
        ((itemCache[groupId] ?: items)
            .mapNotNull { it.sort.takeIf { s -> s != Long.MAX_VALUE } }
            .maxOrNull() ?: 0L) + 1L
}

private fun EmojiGroupView.sortOrZero(): Long = if (sort == Long.MAX_VALUE) 0L else sort

private fun EmojiItemView.sortOrZero(): Long = if (sort == Long.MAX_VALUE) 0L else sort
