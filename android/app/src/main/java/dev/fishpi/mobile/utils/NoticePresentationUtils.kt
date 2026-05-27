package dev.fishpi.mobile.utils

import android.text.Html
import dev.fishpi.mobile.data.NoticeItemView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

internal enum class NoticeDestination {
    Article,
    ChatRoom,
    None,
}

internal enum class NoticePresentationCategory {
    All,
    Reply,
    AtMe,
    Points,
    System,
    Follow,
}

private object NoticeDataType {
    const val ARTICLE = 0
    const val COMMENT = 1
    const val AT = 2
    const val COMMENTED = 3
    const val FOLLOWING_USER = 4
    const val POINT_CHARGE = 5
    const val POINT_TRANSFER = 6
    const val POINT_ARTICLE_REWARD = 7
    const val POINT_COMMENT_THANK = 8
    const val BROADCAST = 9
    const val POINT_EXCHANGE = 10
    const val ABUSE_POINT_DEDUCT = 11
    const val POINT_ARTICLE_THANK = 12
    const val REPLY = 13
    const val INVITECODE_USED = 14
    const val SYS_ANNOUNCE_ARTICLE = 15
    const val SYS_ANNOUNCE_NEW_USER = 16
    const val NEW_FOLLOWER = 17
    const val INVITATION_LINK_USED = 18
    const val SYS_ANNOUNCE_ROLE_CHANGED = 19
    const val FOLLOWING_ARTICLE_UPDATE = 20
    const val FOLLOWING_ARTICLE_COMMENT = 21
    const val POINT_PERFECT_ARTICLE = 22
    const val ARTICLE_NEW_FOLLOWER = 23
    const val ARTICLE_NEW_WATCHER = 24
    const val COMMENT_VOTE_UP = 25
    const val COMMENT_VOTE_DOWN = 26
    const val ARTICLE_VOTE_UP = 27
    const val ARTICLE_VOTE_DOWN = 28
    const val POINT_COMMENT_ACCEPT = 33
    const val POINT_REPORT_HANDLED = 36
    const val CHATROOM_AT = 38
    const val RED_PACKET = 39
}

private val ArticleLinkRegex =
    Regex("""<a[^>]*href="[^"]*/article/[^"]*"[^>]*>([^<]+)</a>""")

internal fun NoticeItemView.noticeDisplayTitle(): String {
    val articleTitle = extractArticleTitle()
    if (articleTitle != null) return articleTitle
    return title.htmlToPlainText()
        .ifBlank { noticeCategoryLabel().ifBlank { "通知" } }
}

internal fun NoticeItemView.noticeSummaryText(): String {
    val stripped = content.htmlToPlainText()
    val articleTitle = extractArticleTitle()
    return if (articleTitle != null) {
        stripped
            .replace(articleTitle, "")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    } else {
        stripped
    }
}

internal fun NoticeItemView.noticePrimaryDestination(): NoticeDestination {
    return when (dataType) {
        NoticeDataType.AT,
        NoticeDataType.CHATROOM_AT,
        -> NoticeDestination.ChatRoom
        NoticeDataType.ARTICLE,
        NoticeDataType.COMMENT,
        NoticeDataType.COMMENTED,
        NoticeDataType.REPLY,
        NoticeDataType.FOLLOWING_ARTICLE_UPDATE,
        NoticeDataType.FOLLOWING_ARTICLE_COMMENT,
        NoticeDataType.ARTICLE_NEW_FOLLOWER,
        NoticeDataType.ARTICLE_NEW_WATCHER,
        NoticeDataType.SYS_ANNOUNCE_ARTICLE,
        NoticeDataType.ARTICLE_VOTE_UP,
        NoticeDataType.ARTICLE_VOTE_DOWN,
        -> NoticeDestination.Article
        else -> when (jumpType) {
            "chatroom" -> NoticeDestination.ChatRoom
            "article" -> NoticeDestination.Article
            else -> NoticeDestination.None
        }
    }
}

internal fun NoticeItemView.noticePresentationCategory(): NoticePresentationCategory {
    return when (dataType) {
        NoticeDataType.COMMENTED,
        NoticeDataType.REPLY,
        -> NoticePresentationCategory.Reply
        NoticeDataType.AT,
        NoticeDataType.CHATROOM_AT,
        -> NoticePresentationCategory.AtMe
        NoticeDataType.POINT_CHARGE,
        NoticeDataType.POINT_TRANSFER,
        NoticeDataType.POINT_ARTICLE_REWARD,
        NoticeDataType.POINT_COMMENT_THANK,
        NoticeDataType.POINT_EXCHANGE,
        NoticeDataType.ABUSE_POINT_DEDUCT,
        NoticeDataType.POINT_ARTICLE_THANK,
        NoticeDataType.POINT_PERFECT_ARTICLE,
        NoticeDataType.POINT_COMMENT_ACCEPT,
        NoticeDataType.POINT_REPORT_HANDLED,
        -> NoticePresentationCategory.Points
        NoticeDataType.SYS_ANNOUNCE_ARTICLE,
        NoticeDataType.SYS_ANNOUNCE_NEW_USER,
        NoticeDataType.SYS_ANNOUNCE_ROLE_CHANGED,
        NoticeDataType.BROADCAST,
        -> NoticePresentationCategory.System
        NoticeDataType.FOLLOWING_USER,
        NoticeDataType.NEW_FOLLOWER,
        NoticeDataType.FOLLOWING_ARTICLE_UPDATE,
        NoticeDataType.FOLLOWING_ARTICLE_COMMENT,
        NoticeDataType.ARTICLE_NEW_FOLLOWER,
        NoticeDataType.ARTICLE_NEW_WATCHER,
        -> NoticePresentationCategory.Follow
        else -> NoticePresentationCategory.All
    }
}

internal fun NoticeItemView.noticeCategoryLabel(): String {
    return when (dataType) {
        NoticeDataType.POINT_CHARGE,
        NoticeDataType.POINT_TRANSFER,
        NoticeDataType.POINT_ARTICLE_REWARD,
        NoticeDataType.POINT_COMMENT_THANK,
        NoticeDataType.POINT_EXCHANGE,
        NoticeDataType.ABUSE_POINT_DEDUCT,
        NoticeDataType.POINT_ARTICLE_THANK,
        NoticeDataType.POINT_PERFECT_ARTICLE,
        NoticeDataType.POINT_COMMENT_ACCEPT,
        NoticeDataType.POINT_REPORT_HANDLED,
        -> "积分"
        NoticeDataType.SYS_ANNOUNCE_ARTICLE,
        NoticeDataType.SYS_ANNOUNCE_NEW_USER,
        NoticeDataType.SYS_ANNOUNCE_ROLE_CHANGED,
        NoticeDataType.BROADCAST,
        -> "系统"
        NoticeDataType.COMMENTED,
        NoticeDataType.REPLY,
        -> "帖子回复"
        NoticeDataType.AT,
        NoticeDataType.CHATROOM_AT,
        -> "@我"
        NoticeDataType.FOLLOWING_USER,
        NoticeDataType.NEW_FOLLOWER,
        NoticeDataType.FOLLOWING_ARTICLE_UPDATE,
        NoticeDataType.FOLLOWING_ARTICLE_COMMENT,
        NoticeDataType.ARTICLE_NEW_FOLLOWER,
        NoticeDataType.ARTICLE_NEW_WATCHER,
        -> "关注"
        NoticeDataType.ARTICLE -> "文章"
        NoticeDataType.COMMENT -> "评论"
        NoticeDataType.INVITECODE_USED -> "邀请码"
        NoticeDataType.INVITATION_LINK_USED -> "邀请链接"
        NoticeDataType.COMMENT_VOTE_UP,
        NoticeDataType.COMMENT_VOTE_DOWN,
        -> "评论"
        NoticeDataType.ARTICLE_VOTE_UP,
        NoticeDataType.ARTICLE_VOTE_DOWN,
        -> "帖子点赞"
        NoticeDataType.RED_PACKET -> "红包"
        else -> category.ifBlank { "通知" }
    }
}

internal fun NoticeItemView.noticeTimeLabel(): String {
    val normalized = time.trim()
    val shanghai = TimeZone.getTimeZone("Asia/Shanghai")
    val date = runCatching {
        val pattern = if (normalized.contains(" CST ")) {
            "EEE MMM dd HH:mm:ss 'CST' yyyy"
        } else {
            "EEE MMM dd HH:mm:ss z yyyy"
        }
        SimpleDateFormat(pattern, Locale.US).apply {
            timeZone = shanghai
        }.parse(normalized)
    }.getOrNull() ?: return ""

    val cal = Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val todayStart = cal.timeInMillis
    val yesterdayStart = todayStart - 86_400_000L
    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    return when {
        date.time >= todayStart -> "今天 ${timeFmt.format(date)}"
        date.time >= yesterdayStart -> "昨天 ${timeFmt.format(date)}"
        else -> SimpleDateFormat("M月d日 HH:mm", Locale.getDefault()).format(date)
    }
}

private fun NoticeItemView.extractArticleTitle(): String? {
    return ArticleLinkRegex.findAll(content)
        .lastOrNull()
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
}

private fun String.htmlToPlainText(): String =
    Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace(Regex("\\s+"), " ")
        .trim()
