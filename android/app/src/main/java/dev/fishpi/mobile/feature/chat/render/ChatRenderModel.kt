package dev.fishpi.mobile.feature.chat.render

import dev.fishpi.mobile.data.ChatFilterConfig
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.feature.chat.blocksChatMessage
import dev.fishpi.mobile.shared.message.ChatListItem
import dev.fishpi.mobile.shared.message.ChatMessageRenderHints
import dev.fishpi.mobile.shared.message.RepeatStackInfo
import dev.fishpi.mobile.shared.message.allRenderableImageUrls
import dev.fishpi.mobile.shared.message.messageTimeSeparator
import dev.fishpi.mobile.shared.message.renderSource
import dev.fishpi.mobile.shared.message.toRenderHints
import dev.fishpi.mobile.utils.isDirectVideoUrl

internal data class ChatRenderModel(
    val visibleItems: List<ChatListItem> = emptyList(),
    val itemEndMessageIndexes: List<Int> = emptyList(),
    val visibleMessages: List<ChatRoomMessage> = emptyList(),
    val blockedMessages: List<ChatRoomMessage> = emptyList(),
)

internal class ChatRenderModelReducer {
    private val renderHintCache = LinkedHashMap<String, ChatMessageRenderHints>()
    private val listItemCache = LinkedHashMap<String, ChatListItem>()
    private var model = ChatRenderModel()

    fun recompute(messages: List<ChatRoomMessage>, filters: ChatFilterConfig): ChatRenderModel {
        val blocked = ArrayList<ChatRoomMessage>()
        val visible = ArrayList<ChatRoomMessage>(messages.size)
        messages.forEach { message ->
            if (filters.blocksChatMessage(message)) {
                blocked.add(message)
            } else {
                visible.add(message)
            }
        }
        val (items, itemEndMessageIndexes) = buildVisibleItems(visible)
        renderHintCache.retainRecentRenderHints(visible)
        listItemCache.retainRecentListItems(visible)
        model = ChatRenderModel(
            visibleItems = items,
            itemEndMessageIndexes = itemEndMessageIndexes,
            visibleMessages = visible,
            blockedMessages = blocked,
        )
        return model
    }

    fun onAppended(
        messages: List<ChatRoomMessage>,
        appended: ChatRoomMessage,
        filters: ChatFilterConfig,
    ): ChatRenderModel {
        val consistent = messages.isNotEmpty() &&
            messages.last() === appended &&
            messages.size == model.visibleMessages.size + model.blockedMessages.size + 1
        if (!consistent) {
            return recompute(messages, filters)
        }

        if (filters.blocksChatMessage(appended)) {
            model = model.copy(blockedMessages = model.blockedMessages + appended)
            return model
        }

        val visible = model.visibleMessages
        if (visible.isEmpty()) {
            val item = buildOneItem(listOf(appended), previousMsg = null)
            model = model.copy(
                visibleItems = listOf(item),
                itemEndMessageIndexes = listOf(0),
                visibleMessages = listOf(appended),
            )
            renderHintCache.retainRecentRenderHints(model.visibleMessages)
            listItemCache.retainRecentListItems(model.visibleMessages)
            return model
        }

        val newVisible = visible + appended
        val items = model.visibleItems
        val ends = model.itemEndMessageIndexes
        val lastGroupStart = if (ends.size >= 2) ends[ends.size - 2] + 1 else 0
        val lastGroupFirst = visible[lastGroupStart]
        val merges = shouldStack(appended) &&
            shouldStack(lastGroupFirst) &&
            appended.repeatStackKey() == lastGroupFirst.repeatStackKey()

        if (merges) {
            val groupMessages = newVisible.subList(lastGroupStart, newVisible.size)
            val previousOfGroup = if (lastGroupStart > 0) visible[lastGroupStart - 1] else null
            val rebuilt = buildOneItem(groupMessages, previousOfGroup)
            model = model.copy(
                visibleItems = items.dropLast(1) + rebuilt,
                itemEndMessageIndexes = ends.dropLast(1) + (newVisible.size - 1),
                visibleMessages = newVisible,
            )
        } else {
            val item = buildOneItem(listOf(appended), previousMsg = visible.last())
            model = model.copy(
                visibleItems = items + item,
                itemEndMessageIndexes = ends + (newVisible.size - 1),
                visibleMessages = newVisible,
            )
        }
        renderHintCache.retainRecentRenderHints(newVisible)
        listItemCache.retainRecentListItems(newVisible)
        return model
    }

    fun onReplaced(
        messages: List<ChatRoomMessage>,
        oId: String,
        filters: ChatFilterConfig,
    ): ChatRenderModel {
        if (oId.isBlank()) return recompute(messages, filters)
        val updated = messages.firstOrNull { it.oId == oId } ?: return recompute(messages, filters)

        val blockedIdx = model.blockedMessages.indexOfFirst { it.oId == oId }
        if (blockedIdx >= 0) {
            if (!filters.blocksChatMessage(updated)) {
                return recompute(messages, filters)
            }
            val newBlocked = model.blockedMessages.toMutableList().also { it[blockedIdx] = updated }
            model = model.copy(blockedMessages = newBlocked)
            return model
        }

        val visible = model.visibleMessages
        val vIdx = visible.indexOfFirst { it.oId == oId }
        if (vIdx < 0) return recompute(messages, filters)
        if (filters.blocksChatMessage(updated)) {
            return recompute(messages, filters)
        }

        val newVisible = visible.toMutableList().also { it[vIdx] = updated }
        val ends = model.itemEndMessageIndexes
        val itemIdx = ends.indexOfFirst { it >= vIdx }
        if (itemIdx < 0) return recompute(messages, filters)
        val groupStart = if (itemIdx > 0) ends[itemIdx - 1] + 1 else 0
        val groupEnd = ends[itemIdx]
        val groupMessages = newVisible.subList(groupStart, groupEnd + 1)
        val previousOfGroup = if (groupStart > 0) newVisible[groupStart - 1] else null
        val rebuilt = buildOneItem(groupMessages, previousOfGroup)
        val newItems = model.visibleItems.toMutableList().also { it[itemIdx] = rebuilt }
        model = model.copy(visibleItems = newItems, visibleMessages = newVisible)
        renderHintCache.retainRecentRenderHints(newVisible)
        listItemCache.retainRecentListItems(newVisible)
        return model
    }

    private fun buildVisibleItems(
        visibleMessages: List<ChatRoomMessage>,
    ): Pair<List<ChatListItem>, List<Int>> {
        val groups = buildStackedItems(visibleMessages)
        var msgIndex = 0
        val itemEndMessageIndexes = ArrayList<Int>(groups.size)
        val items = groups.map { group ->
            val previousMsg = if (msgIndex > 0) visibleMessages[msgIndex - 1] else null
            msgIndex += group.messages.size
            itemEndMessageIndexes.add(msgIndex - 1)
            buildOneItem(group.messages, previousMsg)
        }
        return items to itemEndMessageIndexes
    }

    private fun buildOneItem(
        groupMessages: List<ChatRoomMessage>,
        previousMsg: ChatRoomMessage?,
    ): ChatListItem {
        val message = groupMessages.first()
        val hintKey = message.renderHintCacheKey()
        val previousKey = previousMsg?.renderHintCacheKey().orEmpty()
        val itemKey = "$previousKey->$hintKey-${groupMessages.size}"

        val repeatStack = if (groupMessages.size > 1) {
            val participants = groupMessages
                .asSequence()
                .map { it.userName.trim() to it.userAvatarURL }
                .filter { (username, _) -> username.isNotBlank() }
                .toList()
            RepeatStackInfo(
                count = groupMessages.size,
                participantUsernames = participants.map { it.first },
                participantAvatars = participants.map { it.second },
            )
        } else null

        return listItemCache.getOrPut(itemKey) {
            ChatListItem(
                message = message,
                separator = if (repeatStack != null) null
                            else messageTimeSeparator(previousMsg, message),
                renderHints = renderHintCache.getOrPut(hintKey) {
                    message.toRenderHints()
                },
                repeatStack = repeatStack,
            )
        }
    }
}

private data class StackGroup(val messages: List<ChatRoomMessage>)

private fun buildStackedItems(messages: List<ChatRoomMessage>): List<StackGroup> {
    val groups = mutableListOf<StackGroup>()
    var i = 0
    while (i < messages.size) {
        val current = messages[i]
        if (!shouldStack(current)) {
            groups.add(StackGroup(listOf(current)))
            i++
            continue
        }
        val currentKey = current.repeatStackKey()
        var j = i + 1
        while (j < messages.size &&
            shouldStack(messages[j]) &&
            messages[j].repeatStackKey() == currentKey
        ) {
            j++
        }
        groups.add(StackGroup(messages.subList(i, j)))
        i = j
    }
    return groups
}

private fun shouldStack(message: ChatRoomMessage): Boolean {
    return message.type != "system" && message.type != "redPacket" && message.redPacket == null
}

private val StackFieldSeparator = Char(0x1E).toString()
private val StackMediaSeparator = Char(0x1F).toString()

private fun ChatRoomMessage.repeatStackKey(): String {
    val mediaUrls = (allRenderableImageUrls() + linkUrls.filter { it.isDirectVideoUrl() })
        .distinct()
        .joinToString(StackMediaSeparator)
    return listOf(content, mediaUrls).joinToString(StackFieldSeparator)
}

internal fun ChatRoomMessage.renderHintCacheKey(): String {
    val redPacketKey = redPacket?.let { packet ->
        listOf(
            packet.type,
            packet.money.toString(),
            packet.count.toString(),
            packet.got.toString(),
            packet.finished.toString(),
            packet.openable.toString(),
            packet.needGesture.toString(),
            packet.message,
        ).joinToString(":")
    }.orEmpty()
    return listOf(
        stableMessageIdentity(),
        type,
        revoked.toString(),
        userAvatarURL,
        client,
        time,
        renderSource.hashCode().toString(),
        linkUrls.hashCode().toString(),
        imageUrls.hashCode().toString(),
        reactionSummary.hashCode().toString(),
        currentUserReaction,
        redPacketKey,
    ).joinToString("|")
}

private fun ChatRoomMessage.stableMessageIdentity(): String =
    oId.ifBlank { "$time:${displayName}:$content" }

private fun LinkedHashMap<String, ChatMessageRenderHints>.retainRecentRenderHints(
    visibleMessages: List<ChatRoomMessage>,
) {
    val activeKeys = visibleMessages.takeLast(180).mapTo(HashSet()) { it.renderHintCacheKey() }
    entries.removeIf { (key, _) -> key !in activeKeys }
}

private fun LinkedHashMap<String, ChatListItem>.retainRecentListItems(
    visibleMessages: List<ChatRoomMessage>,
) {
    val recentMessages = visibleMessages.takeLast(180)
    val firstRecentIndex = visibleMessages.size - recentMessages.size
    val activeKeys = recentMessages.mapIndexedTo(HashSet()) { index, message ->
        val previousKey = when {
            index > 0 -> recentMessages[index - 1].renderHintCacheKey()
            firstRecentIndex > 0 -> visibleMessages[firstRecentIndex - 1].renderHintCacheKey()
            else -> ""
        }
        "$previousKey->${message.renderHintCacheKey()}"
    }
    entries.removeIf { (key, _) -> key !in activeKeys }
}
