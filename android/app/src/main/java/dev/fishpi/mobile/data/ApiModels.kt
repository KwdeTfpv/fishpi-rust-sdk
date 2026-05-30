package dev.fishpi.mobile.data

data class FishPiUser(
    val userId: String = "",
    val userName: String,
    val userNickname: String,
    val userAvatarUrl: String,
    val cardBg: String = "",
    val role: String,
    val userNo: String = "",
    val intro: String = "",
    val city: String = "",
    val url: String = "",
    val points: Long = 0,
    val following: Long = 0,
    val follower: Long = 0,
    val onlineMinutes: Long = 0,
    val canFollow: String = "",
) {
    val displayName: String
        get() = userNickname.ifBlank { userName }

    val canRevokeOthers: Boolean
        get() {
            val normalized = role.trim().lowercase()
            return role.contains("管理员") ||
                role.contains("纪律委员") ||
                normalized.contains("op") ||
                normalized.contains("admin")
        }
}

data class UserActivityView(
    val liveness: Double,
    val checkedIn: Boolean,
    val livenessRewarded: Boolean,
)

data class UserDailyState(
    val checkedIn: Boolean,
    val livenessRewarded: Boolean,
)

data class ChatRoomMessage(
    val oId: String,
    val userName: String,
    val userNickname: String = "",
    val userAvatarURL: String = "",
    val content: String,
    val md: String = "",
    val contentHtml: String = "",
    val imageUrls: List<String> = emptyList(),
    val linkUrls: List<String> = emptyList(),
    val time: String,
    val client: String = "",
    val barragerColor: String = "",
    val type: String = "msg",
    val revoked: Boolean = false,
    val reactionSummary: List<ReactionSummaryItem> = emptyList(),
    val currentUserReaction: String = "",
    val redPacket: RedPacketPreview? = null,
    val music: MusicPreview? = null,
    val quote: ChatQuotePreview? = null,
) {
    val isBarrager: Boolean
        get() = type.equals("barrager", ignoreCase = true)

    val displayName: String
        get() = userNickname.ifBlank { userName }

    val authorLabel: String
        get() {
            val nickname = userNickname.trim()
            val username = userName.trim()
            return when {
                nickname.isBlank() -> username
                username.isBlank() -> nickname
                nickname.equals(username, ignoreCase = true) -> nickname
                else -> "$nickname($username)"
            }
        }

    fun toPluginJson(eventType: String): org.json.JSONObject = org.json.JSONObject().apply {
        put("oId", oId)
        put("userName", userName)
        put("userNickname", userNickname)
        put("userAvatarURL", userAvatarURL)
        put("content", content)
        put("md", md)
        put("contentHtml", contentHtml)
        put("imageUrls", org.json.JSONArray(imageUrls))
        put("linkUrls", org.json.JSONArray(linkUrls))
        put("time", time)
        put("client", client)
        put("barragerColor", barragerColor)
        put("type", eventType)
        put("revoked", revoked)
        put("reactionSummary", org.json.JSONArray(reactionSummary.map {
            org.json.JSONObject().apply {
                put("value", it.value); put("emoji", it.emoji); put("count", it.count); put("selected", it.selected)
            }
        }))
        put("currentUserReaction", currentUserReaction)
        redPacket?.let { packet ->
            put("redPacket", org.json.JSONObject().apply {
                put("type", packet.type)
                put("typeName", packet.typeName)
                put("money", packet.money)
                put("count", packet.count)
                put("got", packet.got)
                put("message", packet.message)
                put("summary", packet.summary)
                put("finished", packet.finished)
                put("openable", packet.openable)
                put("needGesture", packet.needGesture)
                put("receivers", org.json.JSONArray(packet.receivers))
                packet.gesture?.let { put("gesture", it) }
                put("who", org.json.JSONArray(packet.who.map { it.toJson() }))
            })
        }
        music?.let { item ->
            put("music", org.json.JSONObject().apply {
                put("coverURL", item.coverUrl)
                put("source", item.source)
                put("title", item.title)
                put("from", item.from)
            })
        }
        quote?.let { quoteItem ->
            put("quote", org.json.JSONObject().apply {
                put("text", quoteItem.text)
                put("imageUrls", org.json.JSONArray(quoteItem.imageUrls))
            })
        }
    }
}

data class MusicPreview(
    val coverUrl: String = "",
    val source: String = "",
    val title: String = "",
    val from: String = "",
)

data class ChatQuotePreview(
    val text: String,
    val imageUrls: List<String> = emptyList(),
)

data class RedPacketPreview(
    val type: String,
    val typeName: String,
    val money: Long,
    val count: Long,
    val got: Long,
    val message: String,
    val summary: String = "",
    val finished: Boolean,
    val openable: Boolean,
    val needGesture: Boolean,
    val receivers: List<String> = emptyList(),
    val gesture: Int? = null,
    val who: List<RedPacketGot> = emptyList(),
)

data class RedPacketOpenResult(
    val message: String,
    val count: Long,
    val got: Long,
    val gesture: Int? = null,
    val who: List<RedPacketGot>,
    val senderName: String = "",
    val senderAvatar: String = "",
)

data class RedPacketStatusUpdate(
    val messageId: String,
    val count: Long,
    val got: Long,
    val whoGive: String,
)

data class RedPacketGot(
    val userId: String = "",
    val userName: String,
    val avatar: String,
    val userMoney: Long,
    val time: String,
) {
    fun toJson(): org.json.JSONObject = org.json.JSONObject().apply {
        put("userId", userId)
        put("userName", userName)
        put("avatar", avatar)
        put("userMoney", userMoney)
        put("time", time)
    }
}

data class ReactionSummaryItem(
    val value: String,
    val emoji: String,
    val count: Long,
    val selected: Boolean,
)

data class ChatReactionUpdate(
    val messageId: String,
    val summary: List<ReactionSummaryItem>,
    val actorReaction: String,
    val actorUserId: String = "",
    val targetType: String = "",
    val groupType: String = "",
    val dataJson: String = "",
)

data class UploadedChatFile(
    val filename: String,
    val url: String,
    val markdown: String,
    val type: String = "image",
)

data class PrivateChatSession(
    val peer: String,
    val preview: String,
    val time: String,
    val avatar: String,
    val unread: Long,
    val sort: Long = 0,
)

data class PrivateChatNotice(
    val peer: String,
    val preview: String,
    val avatar: String,
    val userId: String = "",
)

data class NoticeUnreadCount(
    val total: Long,
    val reply: Long,
    val point: Long,
    val at: Long,
    val broadcast: Long,
    val system: Long,
    val following: Long,
    val commented: Long,
    val newFollower: Long,
)

data class NoticeItemView(
    val id: String,
    val category: String,
    val author: String?,
    val title: String,
    val content: String,
    val dataType: Int,
    val time: String,
    val read: Boolean,
    val jumpType: String,
    val jumpId: String,
    val mentionUser: String,
)

data class ArticleListResult(
    val items: List<ArticleSummary>,
    val nextPage: Int,
    val hasMore: Boolean,
)

data class ArticleSummary(
    val id: String,
    val title: String,
    val author: String,
    val time: String,
    val tags: String,
    val preview: String,
    val commentCount: Long,
    val goodCount: Long,
    val viewCount: Long,
    val sticky: Boolean,
    val perfect: Boolean,
    val avatar: String,
    val thumbnail: String,
)

data class ArticleDetailView(
    val id: String,
    val title: String,
    val author: String,
    val authorUserName: String,
    val avatar: String,
    val time: String,
    val tags: String,
    val markdown: String,
    val imageUrls: List<String> = emptyList(),
    val linkUrls: List<String> = emptyList(),
    val goodCount: Long,
    val badCount: Long,
    val thankCount: Long,
    val collectCount: Long,
    val watchCount: Long,
    val commentCount: Long,
    val viewCount: Long,
    val following: Boolean,
    val watching: Boolean,
    val thanked: Boolean,
    val rewarded: Boolean,
    val rewardedCount: Long,
    val rewardPoint: Long,
    val rewardContent: String,
    val voteState: Int,
    val commentNextPage: Int,
    val commentHasMore: Boolean,
    val comments: List<ArticleCommentView>,
)

data class ArticleCommentView(
    val id: String,
    val author: String,
    val displayName: String,
    val userName: String,
    val time: String,
    val content: String,
    val imageUrls: List<String> = emptyList(),
    val linkUrls: List<String> = emptyList(),
    val goodCount: Long,
    val badCount: Long,
    val thankCount: Long,
    val voteState: Int,
    val thanked: Boolean,
    val replyId: String,
    val avatar: String,
)

data class EmojiGroupView(
    val id: String,
    val name: String,
    val count: Long,
    val isDefault: Boolean,
    val sort: Long = Long.MAX_VALUE,
)

data class ArticleDraftView(
    val id: String,
    val title: String,
    val summary: String,
    val tags: String,
    val type: Int,
    val columnId: String,
    val columnTitle: String,
    val chapterNo: String,
    val updatedTime: Long,
)

data class ArticleDraftDetailView(
    val id: String,
    val title: String,
    val content: String,
    val thoughtContent: String,
    val tags: String,
    val type: Int,
    val columnId: String,
    val columnTitle: String,
    val chapterNo: String,
    val rewardContent: String,
    val rewardPoint: String,
    val qnaOfferPoint: Int,
    val commentable: Boolean,
    val anonymous: Boolean,
    val notifyFollowers: Boolean,
    val showInList: Int,
    val statement: Int,
    val updatedTime: Long,
)

data class ArticleDraftPayload(
    val draftId: String = "",
    val title: String,
    val content: String,
    val thoughtContent: String = "",
    val tags: String,
    val articleType: Int = 0,
    val columnId: String = "",
    val columnTitle: String = "",
    val chapterNo: String = "",
    val rewardContent: String = "",
    val rewardPoint: String = "",
    val qnaOfferPoint: Int = 0,
    val commentable: Boolean = true,
    val anonymous: Boolean = false,
    val notifyFollowers: Boolean = false,
    val showInList: Int = 1,
    val statement: Int = 0,
)

data class EmojiItemView(
    val id: String,
    val groupId: String,
    val name: String,
    val url: String,
    val sort: Long = Long.MAX_VALUE,
)

data class MedalView(
    val id: String,
    val name: String,
    val text: String,
    val imageUrl: String,
    val backColor: String,
    val fontColor: String,
    val type: String = "",
    val description: String = "",
    val rawAttr: String = "",
)

data class BreezemoonView(
    val id: String,
    val authorName: String,
    val updated: String,
    val created: String,
    val timeAgo: String,
    val content: String,
    val createTime: String,
    val city: String,
    val avatar: String,
)

