package dev.fishpi.mobile.realtime

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow

// 通知点击后要跳转到的目标
sealed interface RealtimeNavTarget {
    data class ChatRoom(val messageId: String? = null) : RealtimeNavTarget
    data class PrivateChat(val peer: String) : RealtimeNavTarget
    data class Article(val articleId: String) : RealtimeNavTarget
    data object Notice : RealtimeNavTarget

    companion object {
        const val EXTRA_NAV_TYPE = "fishpi.nav.type"
        const val EXTRA_NAV_ARG = "fishpi.nav.arg"

        fun fromIntent(intent: Intent?): RealtimeNavTarget? {
            val type = intent?.getStringExtra(EXTRA_NAV_TYPE) ?: return null
            val arg = intent.getStringExtra(EXTRA_NAV_ARG).orEmpty()
            return when (type) {
                "chatroom" -> ChatRoom(arg.ifBlank { null })
                "private" -> if (arg.isBlank()) null else PrivateChat(arg)
                "article" -> if (arg.isBlank()) null else Article(arg)
                "notice" -> Notice
                else -> null
            }
        }

        fun clear(intent: Intent?) {
            intent?.removeExtra(EXTRA_NAV_TYPE)
            intent?.removeExtra(EXTRA_NAV_ARG)
        }
    }

    fun writeTo(intent: Intent) {
        val (type, arg) = when (this) {
            is ChatRoom -> "chatroom" to messageId.orEmpty()
            is PrivateChat -> "private" to peer
            is Article -> "article" to articleId
            Notice -> "notice" to ""
        }
        intent.putExtra(EXTRA_NAV_TYPE, type)
        intent.putExtra(EXTRA_NAV_ARG, arg)
    }
}

// 进程级待处理导航：MainActivity 解析通知 intent 后写入，MainShell 消费后清空
object PendingRealtimeNavigation {
    val target = MutableStateFlow<RealtimeNavTarget?>(null)

    fun submit(next: RealtimeNavTarget) {
        target.value = next
    }

    fun consume() {
        target.value = null
    }
}
