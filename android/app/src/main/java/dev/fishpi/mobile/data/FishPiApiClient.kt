package dev.fishpi.mobile.data

import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class FishPiApiClient private constructor() {
    private data class TimedValue<T>(val value: T, val atMs: Long)

    companion object {
        val shared: FishPiApiClient by lazy { FishPiApiClient() }
        private const val USER_TTL_MS = 30_000L
        private const val NOTICE_TTL_MS = 12_000L
        private const val NOTICE_LIST_TTL_MS = 12_000L
        private const val ARTICLE_LIST_TTL_MS = 20_000L
        private val userCache = ConcurrentHashMap<String, TimedValue<FishPiUser>>()
        private val noticeCache = ConcurrentHashMap<String, TimedValue<NoticeUnreadCount>>()
        private val noticeListCache = ConcurrentHashMap<String, TimedValue<List<NoticeItemView>>>()
        private val articleListCache = ConcurrentHashMap<String, TimedValue<ArticleListResult>>()
    }

    private fun nowMs(): Long = System.currentTimeMillis()

    private fun <T> getFresh(cache: ConcurrentHashMap<String, TimedValue<T>>, key: String, ttlMs: Long): T? {
        val hit = cache[key] ?: return null
        return if (nowMs() - hit.atMs <= ttlMs) hit.value else null
    }

    private inline fun <T> cached(
        cache: ConcurrentHashMap<String, TimedValue<T>>,
        key: String,
        ttlMs: Long,
        op: String,
        crossinline block: () -> T,
    ): T {
        getFresh(cache, key, ttlMs)?.let { cached ->
            return cached
        }
        return traceApi(op) {
            block().also { cache[key] = TimedValue(it, nowMs()) }
        }
    }

    private fun dataObject(response: String): JSONObject =
        response.unwrapApiResult().getJSONObject("data")

    private fun dataArray(response: String) =
        response.unwrapApiResult().dataArray()

    private inline fun <T> apiData(op: String, response: () -> String, map: (JSONObject) -> T): T {
        return traceApi(op) { map(dataObject(response())) }
    }

    private inline fun <T> traceApi(op: String, block: () -> T): T {
        return block()
    }

    fun login(nameOrEmail: String, password: String, mfaCode: String? = null): String {
        return traceApi("login") {
            dataObject(FishPiNative.login(nameOrEmail, password, mfaCode.orEmpty()))
                .optString("apiKey")
                .ifBlank { throw IllegalStateException("登录成功但没有返回 API Key") }
        }
    }

    fun fetchRegisterCaptcha(): String {
        return dataObject(FishPiNative.fetchRegisterCaptcha())
            .optString("imageBase64")
            .ifBlank { throw IllegalStateException("没有获取到图形验证码") }
    }

    fun preRegister(username: String, phone: String, inviteCode: String, captcha: String) {
        FishPiNative.preRegister(username, phone, inviteCode, captcha).unwrapApiResult()
    }

    fun verifyRegisterCode(code: String): String {
        return dataObject(FishPiNative.verifyRegisterCode(code))
            .optString("userId")
            .ifBlank { throw IllegalStateException("没有获取到注册用户 Id") }
    }

    fun registerAccount(userId: String, password: String, role: String, inviteCode: String) {
        FishPiNative.registerAccount(userId, password, role, inviteCode).unwrapApiResult()
    }

    fun resetRegisterSession() {
        FishPiNative.resetRegisterSession()
    }

    fun getUser(apiKey: String): FishPiUser {
        return cached(userCache, apiKey, USER_TTL_MS, "getUser") {
            dataObject(FishPiNative.getUser(apiKey)).toFishPiUser()
        }
    }

    fun getUserProfile(apiKey: String, username: String): FishPiUser {
        return dataObject(FishPiNative.getUserProfile(apiKey, username)).toFishPiUser()
    }

    fun getUserPoints(apiKey: String, username: String): Long {
        return dataObject(FishPiNative.getUserPoints(apiKey, username)).optLong("point")
    }

    fun followUser(apiKey: String, userId: String) {
        FishPiNative.followUser(apiKey, userId).unwrapApiResult()
    }

    fun unfollowUser(apiKey: String, userId: String) {
        FishPiNative.unfollowUser(apiKey, userId).unwrapApiResult()
    }

    fun transferPoint(apiKey: String, username: String, amount: Int, memo: String) {
        FishPiNative.transferPoint(apiKey, username, amount, memo).unwrapApiResult()
    }

    fun getUserActivity(apiKey: String): UserActivityView {
        return dataObject(FishPiNative.getUserActivity(apiKey)).let { data ->
            UserActivityView(
                liveness = data.optDouble("liveness"),
                checkedIn = data.optBoolean("checkedIn"),
                livenessRewarded = data.optBoolean("livenessRewarded"),
            )
        }
    }

    fun getUserDailyState(apiKey: String): UserDailyState {
        return dataObject(FishPiNative.getUserDailyState(apiKey)).let { data ->
            UserDailyState(
                checkedIn = data.optBoolean("checkedIn"),
                livenessRewarded = data.optBoolean("livenessRewarded"),
            )
        }
    }

    fun rewardLiveness(apiKey: String): Long {
        return dataObject(FishPiNative.rewardLiveness(apiKey)).optLong("sum")
    }

    fun getChatRoomHistory(apiKey: String, page: Int = 1, selfUsername: String): List<ChatRoomMessage> {
        return traceApi("getChatRoomHistory(page=$page)") {
            dataArray(FishPiNative.getChatRoomHistory(apiKey, page, selfUsername))
                .mapObjects { it.toChatRoomMessage() }
        }
    }

    fun getPrivateChatSessions(apiKey: String, selfUsername: String): List<PrivateChatSession> {
        return traceApi("getPrivateChatSessions") {
            dataArray(FishPiNative.getPrivateChatSessions(apiKey, selfUsername))
                .mapObjects { it.toPrivateChatSession() }
        }
    }

    fun getPrivateChatHistory(
        apiKey: String,
        peer: String,
        page: Int = 1,
        selfUsername: String,
    ): List<ChatRoomMessage> {
        return traceApi("getPrivateChatHistory(page=$page)") {
            dataArray(FishPiNative.getPrivateChatHistory(apiKey, peer, page, selfUsername))
                .mapObjects { it.toChatRoomMessage() }
        }
    }

    fun sendPrivateChatMessage(apiKey: String, peer: String, content: String) {
        FishPiNative.sendPrivateChatMessage(apiKey, peer, content).unwrapApiResult()
    }

    fun revokePrivateChatMessage(apiKey: String, messageId: String) {
        FishPiNative.revokePrivateChatMessage(apiKey, messageId).unwrapApiResult()
    }

    fun markPrivateChatRead(apiKey: String, peer: String) {
        FishPiNative.markPrivateChatRead(apiKey, peer).unwrapApiResult()
    }

    fun sendChatRoomMessage(apiKey: String, content: String) {
        FishPiNative.sendChatRoomMessage(apiKey, content).unwrapApiResult()
    }

    fun setChatRoomDiscuss(apiKey: String, discuss: String) {
        FishPiNative.setChatRoomDiscuss(apiKey, discuss).unwrapApiResult()
    }

    fun sendChatRoomBarrager(apiKey: String, content: String, color: String = "#ffffff") {
        FishPiNative.sendChatRoomBarrager(apiKey, content, color).unwrapApiResult()
    }

    fun getChatRoomBarragerCost(apiKey: String): String {
        val data = dataObject(FishPiNative.getChatRoomBarragerCost(apiKey))
        val cost = data.optLong("cost", 0)
        val unit = data.optString("unit", "积分")
        return if (cost > 0) "$cost$unit" else data.optString("label", "发送弹幕会消耗积分")
    }

    fun revokeChatRoomMessage(apiKey: String, messageId: String) {
        FishPiNative.revokeChatRoomMessage(apiKey, messageId).unwrapApiResult()
    }

    fun reactChatRoomMessage(apiKey: String, messageId: String, value: String): ChatReactionUpdate {
        return dataObject(FishPiNative.reactChatRoomMessage(apiKey, messageId, value)).let { data ->
            ChatReactionUpdate(
                messageId = data.optString("targetId"),
                summary = data.optJSONArray("summary").toReactionSummaryList(),
                actorReaction = data.optString("currentUserReaction"),
            )
        }
    }

    fun openRedPacket(apiKey: String, messageId: String, gesture: Int? = null): RedPacketOpenResult {
        val data = dataObject(FishPiNative.openRedPacket(apiKey, messageId, gesture ?: -1))
        val info = data.optJSONObject("info") ?: JSONObject()
        return RedPacketOpenResult(
            message = info.optString("message"),
            count = info.optLong("count"),
            got = info.optLong("got"),
            gesture = if (info.isNull("gesture")) null else info.optInt("gesture"),
            who = data.optJSONArray("who").toRedPacketGotList(),
            senderName = info.optString("userName"),
            senderAvatar = info.optString("userAvatarURL"),
        )
    }

    fun sendRedPacket(
        apiKey: String,
        type: String,
        money: Int,
        count: Int,
        message: String,
        receivers: String = "",
        gesture: Int? = null,
    ) {
        FishPiNative.sendRedPacket(apiKey, type, money, count, message, receivers, gesture ?: -1).unwrapApiResult()
    }

    fun uploadChatFile(apiKey: String, filePath: String): UploadedChatFile {
        val raw = FishPiNative.uploadChatFile(apiKey, filePath)
        return dataObject(raw).let { data ->
            UploadedChatFile(
                filename = data.optString("filename"),
                url = data.optString("url"),
                markdown = data.optString("markdown"),
                type = data.optString("type", "image"),
            )
        }
    }

    fun searchAtUsers(query: String): List<AtUserCandidate> {
        return dataArray(FishPiNative.searchAtUsers(query)).mapObjects { obj ->
            AtUserCandidate(
                userName = obj.optString("userName"),
                avatarUrl = obj.optString("userAvatarURL"),
            )
        }.filter { it.userName.isNotBlank() }
    }

    fun getNoticeUnreadCount(apiKey: String, forceRefresh: Boolean = false): NoticeUnreadCount {
        val load: () -> NoticeUnreadCount = {
            val data = dataObject(FishPiNative.getNoticeUnreadCount(apiKey))
            NoticeUnreadCount(
                total = data.optLong("total"),
                reply = data.optLong("reply"),
                point = data.optLong("point"),
                at = data.optLong("at"),
                broadcast = data.optLong("broadcast"),
                system = data.optLong("system"),
                following = data.optLong("following"),
                commented = data.optLong("commented"),
                newFollower = data.optLong("newFollower"),
            )
        }
        if (forceRefresh) {
            return traceApi("getNoticeUnreadCount") {
                load().also { noticeCache[apiKey] = TimedValue(it, nowMs()) }
            }
        }
        return cached(noticeCache, apiKey, NOTICE_TTL_MS, "getNoticeUnreadCount", load)
    }

    fun getNotices(apiKey: String): List<NoticeItemView> {
        return cached(noticeListCache, apiKey, NOTICE_LIST_TTL_MS, "getNotices") {
            val raw = FishPiNative.getNotices(apiKey)
            val root = raw.unwrapApiResult()
            root.dataArray().mapObjects { item -> item.toNoticeItemView() }
        }
    }

    fun markAllNoticesRead(apiKey: String) {
        FishPiNative.markAllNoticesRead(apiKey).unwrapApiResult()
        noticeListCache[apiKey]?.let { cached ->
            noticeListCache[apiKey] = TimedValue(cached.value.map { it.copy(read = true) }, nowMs())
        }
        noticeCache[apiKey] = TimedValue(
            NoticeUnreadCount(
                total = 0,
                reply = 0,
                point = 0,
                at = 0,
                broadcast = 0,
                system = 0,
                following = 0,
                commented = 0,
                newFollower = 0,
            ),
            nowMs(),
        )
    }

    fun getArticles(apiKey: String, filter: String, tag: String, page: Int = 1): ArticleListResult {
        val normalizedTag = tag.trim()
        val cacheKey = "$apiKey|$filter|$normalizedTag|$page"
        if (page == 1) {
            getFresh(articleListCache, cacheKey, ARTICLE_LIST_TTL_MS)?.let {
                return it
            }
        }
        return traceApi("getArticles(filter=$filter,page=$page)") {
            val data = dataObject(FishPiNative.getArticles(apiKey, filter, tag, page))
            ArticleListResult(
                items = data.optJSONArray("items").mapObjects { it.toArticleSummary() },
                nextPage = data.optInt("nextPage", page + 1),
                hasMore = data.optBoolean("hasMore", false),
            ).also { result ->
                if (page == 1) {
                    articleListCache[cacheKey] = TimedValue(result, nowMs())
                }
            }
        }
    }

    fun getUserArticles(apiKey: String, username: String, page: Int = 1): ArticleListResult {
        val data = dataObject(FishPiNative.getUserArticles(apiKey, username, page))
        return ArticleListResult(
            items = data.optJSONArray("items").mapObjects { it.toArticleSummary() },
            nextPage = data.optInt("nextPage", page + 1),
            hasMore = data.optBoolean("hasMore", false),
        )
    }

    fun getArticleDetail(apiKey: String, articleId: String, page: Int = 1): ArticleDetailView {
        return apiData(
            op = "getArticleDetail(id=$articleId,page=$page)",
            response = { FishPiNative.getArticleDetail(apiKey, articleId, page) },
        ) { data ->
            data.toArticleDetailView()
        }
    }

    fun getArticleHeat(apiKey: String, articleId: String): Long {
        return dataObject(FishPiNative.getArticleHeat(apiKey, articleId)).optLong("articleHeat")
    }

    fun getArticleDrafts(apiKey: String): List<ArticleDraftView> {
        return dataArray(FishPiNative.getArticleDrafts(apiKey))
            .mapObjects { it.toArticleDraftView() }
    }

    fun getArticleDraftDetail(apiKey: String, draftId: String): ArticleDraftDetailView {
        return dataObject(FishPiNative.getArticleDraftDetail(apiKey, draftId)).toArticleDraftDetailView()
    }

    fun saveArticleDraft(apiKey: String, payload: ArticleDraftPayload): ArticleDraftView {
        return dataObject(
            FishPiNative.saveArticleDraft(
                apiKey = apiKey,
                draftId = payload.draftId,
                title = payload.title,
                content = payload.content,
                thoughtContent = payload.thoughtContent,
                tags = payload.tags,
                articleType = payload.articleType,
                columnId = payload.columnId,
                columnTitle = payload.columnTitle,
                chapterNo = payload.chapterNo,
                rewardContent = payload.rewardContent,
                rewardPoint = payload.rewardPoint,
                qnaOfferPoint = payload.qnaOfferPoint,
                commentable = payload.commentable,
                anonymous = payload.anonymous,
                notifyFollowers = payload.notifyFollowers,
                showInList = payload.showInList,
                statement = payload.statement,
            ),
        ).toArticleDraftView()
    }

    fun deleteArticleDraft(apiKey: String, draftId: String) {
        FishPiNative.deleteArticleDraft(apiKey, draftId).unwrapApiResult()
    }

    fun publishArticle(apiKey: String, payload: ArticleDraftPayload, isGoodArticle: Boolean = false): String {
        return dataObject(
            FishPiNative.publishArticle(
                apiKey = apiKey,
                title = payload.title,
                content = payload.content,
                tags = payload.tags,
                rewardContent = payload.rewardContent,
                rewardPoint = payload.rewardPoint,
                qnaOfferPoint = payload.qnaOfferPoint,
                commentable = payload.commentable,
                anonymous = payload.anonymous,
                notifyFollowers = payload.notifyFollowers,
                showInList = payload.showInList,
                isGoodArticle = isGoodArticle,
            ),
        ).optString("articleId")
    }

    fun getEmojiGroups(apiKey: String): List<EmojiGroupView> {
        return dataArray(FishPiNative.getEmojiGroups(apiKey))
            .mapObjects { it.toEmojiGroupView() }
            .sortedWith(compareBy<EmojiGroupView> { it.sort }.thenBy { it.name })
    }

    fun getEmojiGroupItems(apiKey: String, groupId: String): List<EmojiItemView> {
        return dataArray(FishPiNative.getEmojiGroupItems(apiKey, groupId))
            .mapObjects { it.toEmojiItemView() }
            .sortedWith(compareBy<EmojiItemView> { it.sort }.thenBy { it.name })
    }

    fun uploadEmojiUrl(apiKey: String, url: String) {
        FishPiNative.uploadEmojiUrl(apiKey, url).unwrapApiResult()
    }

    fun createEmojiGroup(apiKey: String, name: String, sort: Long = 0L) {
        FishPiNative.createEmojiGroup(apiKey, name, sort).unwrapApiResult()
    }

    fun updateEmojiGroup(apiKey: String, groupId: String, name: String, sort: Long = 0L) {
        FishPiNative.updateEmojiGroup(apiKey, groupId, name, sort).unwrapApiResult()
    }

    fun deleteEmojiGroup(apiKey: String, groupId: String) {
        FishPiNative.deleteEmojiGroup(apiKey, groupId).unwrapApiResult()
    }

    fun addEmojiToGroup(apiKey: String, groupId: String, emojiId: String, sort: Long = 0L, name: String = "") {
        FishPiNative.addEmojiToGroup(apiKey, groupId, emojiId, sort, name).unwrapApiResult()
    }

    fun addEmojiUrlToGroup(apiKey: String, groupId: String, url: String, sort: Long = 0L, name: String = "") {
        FishPiNative.addEmojiUrlToGroup(apiKey, groupId, url, sort, name).unwrapApiResult()
    }

    fun removeEmojiFromGroup(apiKey: String, groupId: String, emojiId: String) {
        FishPiNative.removeEmojiFromGroup(apiKey, groupId, emojiId).unwrapApiResult()
    }

    fun updateEmojiItem(apiKey: String, oId: String, groupId: String, name: String, sort: Long = 0L) {
        FishPiNative.updateEmojiItem(apiKey, oId, groupId, name, sort).unwrapApiResult()
    }

    fun sendArticleComment(apiKey: String, articleId: String, content: String, replyId: String = "") {
        FishPiNative.sendArticleComment(apiKey, articleId, content, replyId).unwrapApiResult()
    }

    fun voteArticle(apiKey: String, articleId: String, like: Boolean) {
        FishPiNative.voteArticle(apiKey, articleId, like).unwrapApiResult()
    }

    fun thankArticle(apiKey: String, articleId: String) {
        FishPiNative.thankArticle(apiKey, articleId).unwrapApiResult()
    }

    fun voteComment(apiKey: String, commentId: String, like: Boolean) {
        FishPiNative.voteComment(apiKey, commentId, like).unwrapApiResult()
    }

    fun thankComment(apiKey: String, commentId: String) {
        FishPiNative.thankComment(apiKey, commentId).unwrapApiResult()
    }

    fun rewardArticle(apiKey: String, articleId: String) {
        FishPiNative.rewardArticle(apiKey, articleId).unwrapApiResult()
    }

    fun followArticle(apiKey: String, articleId: String) {
        FishPiNative.followArticle(apiKey, articleId).unwrapApiResult()
    }

    fun unfollowArticle(apiKey: String, articleId: String) {
        FishPiNative.unfollowArticle(apiKey, articleId).unwrapApiResult()
    }

    fun watchArticle(apiKey: String, articleId: String) {
        FishPiNative.watchArticle(apiKey, articleId).unwrapApiResult()
    }

    fun getUserMedals(apiKey: String, userName: String): List<MedalView> {
        return traceApi("getUserMedals(user=$userName)") {
            dataArray(FishPiNative.getUserMedals(apiKey, userName))
                .mapObjects { it.toMedalView() }
        }
    }

    fun getProfessionMe(apiKey: String): JSONObject =
        dataObject(FishPiNative.getProfessionMe(apiKey))

    fun getProfessionDetail(apiKey: String, professionId: String, dark: Boolean = false): JSONObject =
        dataObject(FishPiNative.getProfessionDetail(apiKey, professionId, dark))

    fun getProfessionRecords(
        apiKey: String,
        professionId: String,
        beforeOccurredAt: Long = 0L,
        beforeEffectId: String = "",
        limit: Int = 30,
    ): JSONObject =
        dataObject(FishPiNative.getProfessionRecords(apiKey, professionId, beforeOccurredAt, beforeEffectId, limit))

    fun setPrimaryProfession(apiKey: String, professionId: String): JSONObject =
        dataObject(FishPiNative.setPrimaryProfession(apiKey, professionId))

    fun skipProfessionPrompt(apiKey: String) {
        FishPiNative.skipProfessionPrompt(apiKey).unwrapApiResult()
    }

    fun updateProfessionPrivacy(
        apiKey: String,
        preset: String,
        moduleVisibilityJson: String = "",
    ): JSONObject =
        dataObject(
            FishPiNative.updateProfessionPrivacy(
                apiKey = apiKey,
                preset = preset,
                moduleVisibilityJson = moduleVisibilityJson,
            ),
        )

    fun getUserProfession(
        apiKey: String,
        userName: String,
        position: String = "homeProfile",
        dark: Boolean = false,
    ): JSONObject =
        dataObject(FishPiNative.getUserProfession(apiKey, userName, position, dark))

    fun getProfessionRanking(apiKey: String, professionId: String = "", dark: Boolean = false): JSONObject =
        dataObject(FishPiNative.getProfessionRanking(apiKey, professionId, dark))

    fun getBreezemoons(apiKey: String, page: Int = 1, size: Int = 20): List<BreezemoonView> {
        return dataArray(FishPiNative.getBreezemoons(apiKey, page, size)).toBreezemoons()
    }

    fun getUserBreezemoons(
        apiKey: String,
        userName: String,
        page: Int = 1,
        size: Int = 20,
    ): List<BreezemoonView> {
        return dataArray(FishPiNative.getUserBreezemoons(apiKey, userName, page, size)).toBreezemoons()
    }

    fun sendBreezemoon(apiKey: String, content: String) {
        FishPiNative.sendBreezemoon(apiKey, content).unwrapApiResult()
    }
}

