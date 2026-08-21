package dev.fishpi.mobile.realtime

import android.content.Context
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.data.NotificationPrefs
import dev.fishpi.mobile.data.PrivateChatNotice
import dev.fishpi.mobile.data.SessionStore
import dev.fishpi.mobile.utils.toPlainMessageText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

// 订阅实时事件，按用户偏好决定是否弹系统通知；不触碰 UI
class RealtimeNotificationDispatcher(
    private val appContext: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val store = SessionStore(appContext)

    @Volatile
    var chatRoomVisible: Boolean = false

    @Volatile
    var appInForeground: Boolean = false

    private var nextId = 2000

    fun start() {
        RealtimeConnectionManager.chatEvents
            .onEach { event ->
                if (event is ChatRoomEvent.Message) onChatMessage(event.message)
            }
            .launchIn(scope)

        RealtimeConnectionManager.privateNotices
            .onEach { onPrivateNotice(it) }
            .launchIn(scope)

        RealtimeConnectionManager.noticeEvents
            .onEach { onNoticeEvent() }
            .launchIn(scope)
    }

    private fun onChatMessage(message: ChatRoomMessage) {
        // 聊天室可见时不打扰
        if (chatRoomVisible && appInForeground) {
            return
        }
        if (message.type == "system" || message.userName.isBlank()) {
            return
        }
        val prefs = store.getNotificationPrefs()
        val self = RealtimeConnectionManager.currentSession()?.username.orEmpty()
        if (message.userName.equals(self, ignoreCase = true)) {
            return
        }

        val raw = message.content
        val rawWithoutQuote = raw.substringBefore("##### 引用").ifBlank { raw }
        val quotedSelf = self.isNotBlank() && raw.contains("引用 @$self")
        val plain = rawWithoutQuote.toNotificationText()
        val mentioned = self.isNotBlank() && (
            raw.contains("/member/$self", ignoreCase = true) ||
                plain.contains("@$self", ignoreCase = true)
            )
        val hitKeyword = if (prefs.chatKeywordsOnly) {
            prefs.matchedChatKeyword(plain) ?: prefs.matchedChatKeyword(rawWithoutQuote)
        } else {
            null
        }

        val shouldNotify = when {
            quotedSelf || mentioned -> prefs.chatMentions
            prefs.chatKeywordsOnly -> hitKeyword != null
            else -> false
        }
        if (!shouldNotify) {
            return
        }

        val sender = message.userNickname.ifBlank { message.userName }
        RealtimeNotifier.notify(
            context = appContext,
            id = notificationId(),
            title = when {
                quotedSelf -> "$sender 引用你的消息"
                mentioned -> "$sender @你"
                else -> sender
            },
            body = plain.take(120).ifBlank { "[消息]" },
            target = RealtimeNavTarget.ChatRoom(message.oId.ifBlank { null }),
        )
    }

    private fun onPrivateNotice(notice: PrivateChatNotice) {
        if (appInForeground) {
            return
        }
        val prefs = store.getNotificationPrefs()
        if (!prefs.privateMessages) {
            return
        }
        RealtimeNotifier.notify(
            context = appContext,
            id = privateNotificationId(notice.peer),
            title = notice.peer,
            body = notice.preview.toNotificationText().take(120).ifBlank { "[新消息]" },
            target = RealtimeNavTarget.PrivateChat(notice.peer),
        )
    }

    private fun onNoticeEvent() {
        if (appInForeground) {
            return
        }
        val prefs = store.getNotificationPrefs()
        if (!prefs.notices) {
            return
        }
        RealtimeNotifier.notify(
            context = appContext,
            id = 4001,
            title = "摸鱼派",
            body = "你有一条新的系统通知",
            target = RealtimeNavTarget.Notice,
        )
    }

    private fun notificationId(): Int {
        nextId += 1
        if (nextId > 2999) nextId = 2000
        return nextId
    }

    private fun privateNotificationId(peer: String): Int =
        3000 + (peer.hashCode() and 0x3FF)

    private fun String.toNotificationText(): String {
        val withoutQuotes = lineSequence()
            .filterNot { it.trimStart().startsWith(">") }
            .joinToString("\n")
        return withoutQuotes
            .toPlainMessageText()
            .replace(MarkdownLinkRegex) { it.groupValues.getOrNull(1).orEmpty() }
            .replace(PlainUrlRegex, "[链接]")
            .replace(WhitespaceRegex, " ")
            .trim()
    }

    private companion object {
        val MarkdownLinkRegex = Regex("""\[([^\]]*)]\([^)]*\)""")
        val PlainUrlRegex = Regex("""https?://[^\s<>()\[\]{}"']+""", RegexOption.IGNORE_CASE)
        val WhitespaceRegex = Regex("\\s+")
    }
}
