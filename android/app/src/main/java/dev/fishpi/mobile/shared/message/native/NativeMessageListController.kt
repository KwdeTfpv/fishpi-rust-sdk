package dev.fishpi.mobile.shared.message.native

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.fishpi.mobile.shared.message.ChatListItem

internal class NativeMessageListController {
    internal var recyclerView: RecyclerView? = null
    internal var adapter: NativeMessageAdapter? = null
    private var pendingScrollToBottom = false
    private var scrollRetryCount = 0
    private var lastNearTop: Boolean? = null
    private var lastNearBottom: Boolean? = null
    private var lastVisibleRange: VisibleRange? = null
    private var pendingPrependAnchor: PrependAnchor? = null
    private var restoredPrependAnchor = false

    internal val hasPendingScrollToBottom: Boolean
        get() = pendingScrollToBottom

    fun requestScrollToBottom() {
        pendingScrollToBottom = true
        scrollRetryCount = 0
        flushScrollToBottom()
    }

    fun onItemsChanged() {
        if (pendingScrollToBottom) {
            flushScrollToBottom()
        }
    }

    fun isNearBottom(): Boolean {
        val view = recyclerView ?: return true
        val count = adapter?.itemCount ?: view.adapter?.itemCount ?: 0
        if (count <= 0) {
            return true
        }
        val layout = view.layoutManager as? LinearLayoutManager
        val lastVisible = layout?.findLastVisibleItemPosition() ?: RecyclerView.NO_POSITION
        if (lastVisible != RecyclerView.NO_POSITION && lastVisible >= count - 2) {
            return true
        }
        if (!view.canScrollVertically(1)) {
            return true
        }
        val remaining = view.computeVerticalScrollRange() -
            view.computeVerticalScrollOffset() -
            view.computeVerticalScrollExtent()
        return remaining <= 96.dp(view.context)
    }

    internal fun consumeNearTopChanged(value: Boolean): Boolean {
        if (lastNearTop == value) return false
        lastNearTop = value
        return true
    }

    internal fun consumeNearBottomChanged(value: Boolean): Boolean {
        if (lastNearBottom == value) return false
        lastNearBottom = value
        return true
    }

    internal fun consumeVisibleRangeChanged(first: Int, last: Int, count: Int): Boolean {
        val next = VisibleRange(first, last, count)
        if (lastVisibleRange == next) return false
        lastVisibleRange = next
        return true
    }

    fun capturePrependAnchor() {
        val view = recyclerView ?: return
        val items = adapter?.currentItems.orEmpty()
        if (items.isEmpty()) return
        val layout = view.layoutManager as? LinearLayoutManager ?: return
        val first = layout.findFirstVisibleItemPosition()
        if (first == RecyclerView.NO_POSITION) return
        val child = layout.findViewByPosition(first) ?: return
        val item = items.getOrNull(first) ?: return
        pendingPrependAnchor = PrependAnchor(
            key = item.prependAnchorKey(),
            offset = child.top - view.paddingTop,
        )
        restoredPrependAnchor = false
    }

    internal fun restorePrependAnchorIfNeeded(): Boolean {
        val anchor = pendingPrependAnchor ?: return false
        val view = recyclerView ?: return false
        val layout = view.layoutManager as? LinearLayoutManager ?: return false
        val items = adapter?.currentItems.orEmpty()
        val index = items.indexOfFirst { it.prependAnchorKey() == anchor.key }
        if (index < 0) return false
        view.stopScroll()
        layout.scrollToPositionWithOffset(index, anchor.offset)
        pendingPrependAnchor = null
        restoredPrependAnchor = true
        return true
    }

    private fun flushScrollToBottom(): Boolean {
        val view = recyclerView ?: return false
        val count = adapter?.itemCount ?: view.adapter?.itemCount ?: 0
        if (count <= 0) {
            return false
        }
        view.post {
            val latestCount = adapter?.itemCount ?: view.adapter?.itemCount ?: 0
            if (latestCount > 0) {
                view.scrollToPosition(latestCount - 1)
                view.postOnAnimation {
                    val settledCount = adapter?.itemCount ?: view.adapter?.itemCount ?: 0
                    if (settledCount > 0) {
                        view.scrollToPosition(settledCount - 1)
                        val layout = view.layoutManager as? LinearLayoutManager
                        val last = layout?.findLastVisibleItemPosition() ?: -1
                        if (last >= settledCount - 2 || scrollRetryCount >= 2) {
                            pendingScrollToBottom = false
                            scrollRetryCount = 0
                        } else {
                            scrollRetryCount += 1
                            view.postOnAnimation { flushScrollToBottom() }
                        }
                    }
                }
            }
        }
        return true
    }

    fun scrollToMessage(messageId: String) {
        val index = adapter?.currentItems?.indexOfFirst { it.message.oId == messageId } ?: -1
        if (index >= 0) {
            recyclerView?.post { recyclerView?.smoothScrollToPosition(index) }
        }
    }

    fun keepPositionAfterPrepend(insertedCount: Int) {
        if (insertedCount > 0) {
            recyclerView?.postOnAnimation {
                if (restoredPrependAnchor) {
                    restoredPrependAnchor = false
                    return@postOnAnimation
                }
                if (restorePrependAnchorIfNeeded()) {
                    return@postOnAnimation
                }
                val layout = recyclerView?.layoutManager as? LinearLayoutManager
                if (layout != null) {
                    layout.scrollToPositionWithOffset(insertedCount, 0)
                } else {
                    recyclerView?.scrollToPosition(insertedCount)
                }
            }
        }
    }
}

private fun Int.dp(context: android.content.Context): Int =
    (this * context.resources.displayMetrics.density).toInt()

private data class VisibleRange(
    val first: Int,
    val last: Int,
    val count: Int,
)

private data class PrependAnchor(
    val key: String,
    val offset: Int,
)

private fun ChatListItem.prependAnchorKey(): String =
    message.oId.ifBlank { "${message.time}:${message.userName}:${message.content.hashCode()}" }
