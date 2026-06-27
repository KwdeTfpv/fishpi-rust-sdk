package dev.fishpi.mobile.data

object FishPiNative {
    init {
        System.loadLibrary("fishpi_sdk")
    }

    external fun login(nameOrEmail: String, password: String, mfaCode: String): String

    external fun getUser(apiKey: String): String

    external fun getUserProfile(apiKey: String, username: String): String
    external fun getUserPoints(apiKey: String, username: String): String
    external fun getUserActivity(apiKey: String): String
    external fun getUserDailyState(apiKey: String): String
    external fun followUser(apiKey: String, userId: String): String
    external fun unfollowUser(apiKey: String, userId: String): String
    external fun transferPoint(apiKey: String, username: String, amount: Int, memo: String): String

    external fun rewardLiveness(apiKey: String): String

    external fun getChatRoomHistory(apiKey: String, page: Int, selfUsername: String): String

    external fun getPrivateChatSessions(apiKey: String, selfUsername: String): String

    external fun getPrivateChatHistory(apiKey: String, peer: String, page: Int, selfUsername: String): String

    external fun sendPrivateChatMessage(apiKey: String, peer: String, content: String): String

    external fun sendPrivateChatMessageOnConnection(handle: Long, content: String): String

    external fun revokePrivateChatMessage(apiKey: String, messageId: String): String

    external fun markPrivateChatRead(apiKey: String, peer: String): String

    external fun connectPrivateChat(apiKey: String, selfUsername: String, peer: String, callback: Any): Long

    external fun reconnectPrivateChat(handle: Long): Boolean

    external fun disconnectPrivateChat(handle: Long)

    external fun setChatRoomClientType(client: String, version: String)

    external fun sendChatRoomMessage(apiKey: String, content: String): String

    external fun setChatRoomDiscuss(apiKey: String, discuss: String): String

    external fun sendChatRoomBarrager(apiKey: String, content: String, color: String): String

    external fun getChatRoomBarragerCost(apiKey: String): String

    external fun sendChatRoomMessageWithClientType(
        apiKey: String,
        content: String,
        client: String,
        version: String,
    ): String

    external fun revokeChatRoomMessage(apiKey: String, messageId: String): String

    external fun reactChatRoomMessage(apiKey: String, messageId: String, value: String): String

    external fun openRedPacket(apiKey: String, messageId: String, gesture: Int): String

    external fun sendRedPacket(
        apiKey: String,
        type: String,
        money: Int,
        count: Int,
        message: String,
        receivers: String,
        gesture: Int,
    ): String

    external fun uploadChatFile(apiKey: String, filePath: String): String

    external fun searchAtUsers(query: String): String

    external fun getNoticeUnreadCount(apiKey: String): String

    external fun getNotices(apiKey: String): String

    external fun markAllNoticesRead(apiKey: String): String

    external fun connectNotice(apiKey: String, callback: Any): Long

    external fun disconnectNotice(handle: Long)

    external fun getArticles(apiKey: String, filter: String, tag: String, page: Int): String

    external fun getUserArticles(apiKey: String, username: String, page: Int): String

    external fun getArticleDetail(apiKey: String, articleId: String, page: Int): String

    external fun getArticleHeat(apiKey: String, articleId: String): String

    external fun connectArticle(apiKey: String, articleId: String, articleType: Int, callback: Any): Long

    external fun disconnectArticle(handle: Long)

    external fun getArticleDrafts(apiKey: String): String

    external fun getArticleDraftDetail(apiKey: String, draftId: String): String

    external fun saveArticleDraft(
        apiKey: String,
        draftId: String,
        title: String,
        content: String,
        thoughtContent: String,
        tags: String,
        articleType: Int,
        columnId: String,
        columnTitle: String,
        chapterNo: String,
        rewardContent: String,
        rewardPoint: String,
        qnaOfferPoint: Int,
        commentable: Boolean,
        anonymous: Boolean,
        notifyFollowers: Boolean,
        showInList: Int,
        statement: Int,
    ): String

    external fun deleteArticleDraft(apiKey: String, draftId: String): String

    external fun publishArticle(
        apiKey: String,
        title: String,
        content: String,
        tags: String,
        rewardContent: String,
        rewardPoint: String,
        qnaOfferPoint: Int,
        commentable: Boolean,
        anonymous: Boolean,
        notifyFollowers: Boolean,
        showInList: Int,
        isGoodArticle: Boolean,
    ): String

    external fun getEmojiGroups(apiKey: String): String

    external fun getEmojiGroupItems(apiKey: String, groupId: String): String

    external fun sendArticleComment(apiKey: String, articleId: String, content: String, replyId: String): String

    external fun voteArticle(apiKey: String, articleId: String, like: Boolean): String

    external fun thankArticle(apiKey: String, articleId: String): String

    external fun voteComment(apiKey: String, commentId: String, like: Boolean): String

    external fun thankComment(apiKey: String, commentId: String): String

    external fun rewardArticle(apiKey: String, articleId: String): String

    external fun followArticle(apiKey: String, articleId: String): String
    external fun unfollowArticle(apiKey: String, articleId: String): String

    external fun watchArticle(apiKey: String, articleId: String): String
    external fun getUserMedals(apiKey: String, userName: String): String
    external fun getBreezemoons(apiKey: String, page: Int, size: Int): String
    external fun getUserBreezemoons(apiKey: String, userName: String, page: Int, size: Int): String
    external fun sendBreezemoon(apiKey: String, content: String): String

    external fun connectChatRoom(apiKey: String, selfUsername: String, callback: Any): Long

    external fun pauseChatRoomEvents(handle: Long)

    external fun resumeChatRoomEvents(handle: Long)

    external fun reconnectChatRoom(handle: Long): Boolean

    external fun disconnectChatRoom(handle: Long)
}
