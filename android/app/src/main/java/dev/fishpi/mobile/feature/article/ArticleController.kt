package dev.fishpi.mobile.feature.article

import android.content.Context
import dev.fishpi.mobile.core.ui.UiController
import dev.fishpi.mobile.data.ArticleDetailView
import dev.fishpi.mobile.data.ArticleRealtimeClient
import dev.fishpi.mobile.data.ArticleSummary
import dev.fishpi.mobile.data.EmojiItemView
import dev.fishpi.mobile.data.AttachmentUpload
import dev.fishpi.mobile.data.AttachmentUploadQueue
import dev.fishpi.mobile.data.ComposerAttachments
import dev.fishpi.mobile.data.FishPiApiClient
import dev.fishpi.mobile.data.toPluginJson
import dev.fishpi.mobile.plugin.PluginManager
import dev.fishpi.mobile.plugin.PluginToolbarEntry
import dev.fishpi.mobile.feature.article.mapper.toArticleSummaryUiModel
import dev.fishpi.mobile.feature.article.mapper.toSummary
import dev.fishpi.mobile.feature.article.model.ArticleOverlayState
import dev.fishpi.mobile.feature.article.model.CommentSort
import dev.fishpi.mobile.feature.article.model.RecentArticleFilters
import dev.fishpi.mobile.feature.article.model.TaggedArticleFilters
import dev.fishpi.mobile.feature.article.model.articleFiltersForTag
import dev.fishpi.mobile.utils.appendDraftBlock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ArticleController(
    context: Context,
    private val apiKey: String,
    currentUsername: String = "",
    private val api: FishPiApiClient = FishPiApiClient.shared,
    private val realtime: ArticleRealtimeClient = ArticleRealtimeClient(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : UiController<ArticleState, ArticleAction> {
    private val _state = MutableStateFlow(ArticleState())
    override val state: StateFlow<ArticleState> = _state

    private val _effects = MutableSharedFlow<ArticleEffect>(extraBufferCapacity = 16)
    val effects: SharedFlow<ArticleEffect> = _effects.asSharedFlow()

    private val requestedPages = mutableSetOf<Int>()
    private var commentGeneration = 0
    private var heatJob: Job? = null
    private val uploadQueue = AttachmentUploadQueue.shared(context)

    init {
        observeCommentUploads()
    }

    private val pluginManager = PluginManager.init(context).also {
        it.apiKey = apiKey
        if (currentUsername.isNotBlank()) it.userName = currentUsername
    }

    val pluginToolbarEntries: List<PluginToolbarEntry>
        get() = pluginManager.toolbarEntries

    private val sceneToken = pluginManager.acquireSceneToken()

    fun setPluginScene(scene: String) {
        pluginManager.setSceneFor(sceneToken, scene)
    }

    fun releasePluginScene() {
        pluginManager.releaseSceneToken(sceneToken)
    }

    fun emitPluginToolbarAction(entry: PluginToolbarEntry, actionId: String) {
        val context = _state.value.detail?.toPluginJson()
        pluginManager.emitToolbarAction(entry.pluginId, entry.id, actionId, context)
    }

    override fun dispatch(action: ArticleAction) {
        when (action) {
            ArticleAction.RefreshList -> loadArticles(page = 1, append = false)
            ArticleAction.LoadMoreList -> loadArticles(_state.value.nextPage, append = true)
            is ArticleAction.ChangeFilter -> changeFilter(action.filter)
            is ArticleAction.ChangeTagInput -> _state.update { it.copy(tagInput = action.value) }
            ArticleAction.ApplyTag -> applyTag()
            ArticleAction.ClearTag -> clearTag()
            is ArticleAction.OpenArticle -> openArticle(action.article)
            is ArticleAction.OpenArticleById -> openArticleById(action.articleId)
            ArticleAction.CloseDetail -> closeDetail()
            ArticleAction.LoadMoreComments -> loadMoreComments()
            is ArticleAction.ChangeCommentSort -> changeCommentSort(action.sort)
            is ArticleAction.ChangeCommentInput -> _state.update { it.copy(commentInput = action.value) }
            ArticleAction.SendComment -> sendComment()
            ArticleAction.PickCommentImage -> emitEffect(ArticleEffect.OpenCommentGallery)
            ArticleAction.CaptureCommentImage -> emitEffect(ArticleEffect.OpenCommentCamera)
            is ArticleAction.PickCommentImageUri -> pickCommentImage(action.uri, action.isVideo)
            is ArticleAction.RemoveCommentImage -> removeCommentImage(action.index)
            is ArticleAction.RetryCommentImage -> retryCommentImage(action.index)
            is ArticleAction.ShowPickerError -> setError(action.message)
            is ArticleAction.ReplyToComment -> replyToComment(action.comment)
            ArticleAction.CancelReply -> _state.update { it.copy(replyToCommentId = "", replyTarget = null) }
            ArticleAction.ToggleEmoji -> toggleEmoji()
            ArticleAction.DismissEmoji -> _state.update { it.copy(emojiPanelOpen = false) }
            is ArticleAction.PickEmojiGroup -> loadEmojiGroup(action.groupId)
            is ArticleAction.PickEmoji -> pickEmoji(action.item)
            ArticleAction.VoteUp -> runArticleAction("点赞") { articleId -> api.voteArticle(apiKey, articleId, true) }
            ArticleAction.VoteDown -> runArticleAction("点踩") { articleId -> api.voteArticle(apiKey, articleId, false) }
            ArticleAction.Thank -> runArticleAction("感谢") { articleId -> api.thankArticle(apiKey, articleId) }
            is ArticleAction.VoteCommentUp -> voteComment(action.comment, like = true)
            is ArticleAction.VoteCommentDown -> voteComment(action.comment, like = false)
            is ArticleAction.ThankComment -> thankComment(action.comment)
            ArticleAction.ToggleFollow -> toggleFollow()
            ArticleAction.Watch -> runArticleAction("关注") { articleId -> api.watchArticle(apiKey, articleId) }
            ArticleAction.RequestRewardArticle -> _state.update { it.copy(rewardConfirmOpen = true) }
            ArticleAction.ConfirmRewardArticle -> rewardArticle()
            ArticleAction.DismissRewardConfirm -> _state.update { it.copy(rewardConfirmOpen = false) }
            is ArticleAction.ShowLinkPreview -> _state.update { it.copy(overlay = ArticleOverlayState.Link(action.url)) }
            is ArticleAction.ShowVideoPreview -> _state.update { it.copy(overlay = ArticleOverlayState.Video(action.url)) }
            ArticleAction.DismissOverlay -> _state.update { it.copy(overlay = ArticleOverlayState.None) }
            ArticleAction.ShareArticle -> shareSelectedArticle()
            is ArticleAction.OpenUserProfile -> emitEffect(ArticleEffect.OpenUserProfile(action.username))
            ArticleAction.OpenPublish -> _state.update { it.copy(publishOpen = true) }
            ArticleAction.ClosePublish -> _state.update { it.copy(publishOpen = false) }
            is ArticleAction.PublishCompleted -> onPublishCompleted(action.articleId)
            ArticleAction.ClearError -> _state.update { it.copy(error = null) }
        }
    }

    fun close() {
        disconnectRealtime()
        heatJob?.cancel()
    }

    fun connectArticleRealtime(enabled: Boolean) {
        val articleId = _state.value.selected?.id.orEmpty()
        if (!enabled || articleId.isBlank() || _state.value.publishOpen) {
            disconnectRealtime()
            return
        }
        realtime.connect(
            apiKey = apiKey,
            articleId = articleId,
            articleType = 0,
            onHeatDelta = { eventArticleId, delta ->
                if (_state.value.selected?.id == eventArticleId) {
                    _state.update { state ->
                        state.copy(articleHeat = ((state.articleHeat ?: 0L) + delta).coerceAtLeast(0L))
                    }
                }
            },
        )
        heatJob?.cancel()
        heatJob = scope.launch {
            while (true) {
                refreshArticleHeat(articleId)
                delay(30_000)
            }
        }
    }

    private fun disconnectRealtime() {
        realtime.disconnect()
        heatJob?.cancel()
        heatJob = null
    }

    private fun changeFilter(filter: dev.fishpi.mobile.feature.article.model.ArticleFilterUiModel) {
        _state.update { it.copy(filter = filter) }
        loadArticles(page = 1, append = false)
    }

    private fun applyTag() {
        val nextTag = _state.value.tagInput.trim()
        _state.update {
            val filters = articleFiltersForTag(nextTag)
            it.copy(
                appliedTag = nextTag,
                filter = if (nextTag.isNotBlank() && it.filter.key == "long") TaggedArticleFilters.first() else it.filter,
                availableFilters = filters,
            )
        }
        loadArticles(page = 1, append = false)
    }

    private fun clearTag() {
        _state.update {
            it.copy(
                tagInput = "",
                appliedTag = "",
                filter = if (it.filter.key == "perfect") RecentArticleFilters.first() else it.filter,
                availableFilters = RecentArticleFilters,
            )
        }
        loadArticles(page = 1, append = false)
    }

    private fun loadArticles(page: Int, append: Boolean) {
        val snapshot = _state.value
        if (append) {
            if (snapshot.isLoading || snapshot.isLoadingMore || !snapshot.hasMore || page in requestedPages) return
            requestedPages += page
            _state.update { it.copy(isLoadingMore = true, error = null) }
        } else {
            if (snapshot.isLoading) return
            requestedPages.clear()
            _state.update {
                it.copy(
                    isLoading = true,
                    isLoadingMore = false,
                    rawArticles = emptyList(),
                    articles = emptyList(),
                    selected = null,
                    detail = null,
                    articleHeat = null,
                    error = null,
                )
            }
        }
        val requestFilterKey = _state.value.filter.key
        val requestTag = _state.value.appliedTag
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.getArticles(apiKey, requestFilterKey, requestTag, page) }
            }.onSuccess { result ->
                if (_state.value.filter.key == requestFilterKey && _state.value.appliedTag == requestTag) {
                    _state.update { state ->
                        val raw = if (append) {
                            (state.rawArticles + result.items).distinctBy { it.id }
                        } else {
                            result.items
                        }
                        state.copy(
                            rawArticles = raw,
                            articles = raw.map { it.toArticleSummaryUiModel() },
                            nextPage = result.nextPage,
                            hasMore = result.hasMore && (!append || raw.size > state.rawArticles.size),
                            isLoading = false,
                            isLoadingMore = false,
                        )
                    }
                }
            }.onFailure {
                setError(it.message ?: "加载帖子失败")
                _state.update { state -> state.copy(isLoading = false, isLoadingMore = false) }
            }
            if (append) requestedPages -= page
        }
    }

    private fun openArticle(article: ArticleSummary) {
        commentGeneration += 1
        disconnectRealtime()
        _state.update {
            it.copy(
                selected = article,
                detail = null,
                articleHeat = null,
                commentInput = "",
                replyToCommentId = "",
                replyTarget = null,
                isLoadingDetail = true,
                error = null,
            )
        }
        loadDetail(article.id, replaceSummary = false)
    }

    private fun openArticleById(articleId: String) {
        val id = articleId.trim()
        if (id.isBlank()) return
        val placeholder = ArticleSummary(
            id = id,
            title = "帖子 #$id",
            author = "",
            time = "",
            tags = "",
            preview = "",
            commentCount = 0,
            goodCount = 0,
            viewCount = 0,
            sticky = false,
            perfect = false,
            avatar = "",
            thumbnail = "",
        )
        openArticle(placeholder)
        loadDetail(id, replaceSummary = true)
    }

    private fun closeDetail() {
        commentGeneration += 1
        disconnectRealtime()
        _state.update {
            it.copy(
                selected = null,
                detail = null,
                articleHeat = null,
                commentInput = "",
                replyToCommentId = "",
                replyTarget = null,
                replyFocusSignal = 0,
                dismissKeyboardSignal = 0,
                emojiPanelOpen = false,
                overlay = ArticleOverlayState.None,
            )
        }
        emitEffect(ArticleEffect.DetailClosed)
    }

    private fun loadDetail(articleId: String, replaceSummary: Boolean = false) {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.getArticleDetail(apiKey, articleId) }
            }.onSuccess { loaded ->
                if (_state.value.selected?.id != articleId) return@onSuccess
                val needsLastPage =
                    _state.value.commentSort == CommentSort.Earliest && loaded.commentPageCount > 1
                _state.update {
                    it.copy(
                        detail = if (needsLastPage) loaded.copy(comments = emptyList()) else loaded,
                        selected = if (replaceSummary) loaded.toSummary() else it.selected,
                        isLoadingDetail = needsLastPage,
                    )
                }
                refreshArticleHeat(articleId)
                if (needsLastPage) {
                    loadCommentPage(articleId, loaded.commentPageCount, replace = true)
                }
            }.onFailure {
                if (_state.value.selected?.id == articleId) {
                    setError(it.message ?: "加载帖子详情失败")
                    _state.update { state -> state.copy(isLoadingDetail = false) }
                }
            }
        }
    }

    private fun changeCommentSort(sort: CommentSort) {
        if (_state.value.commentSort == sort) return
        commentGeneration += 1
        val detail = _state.value.detail
        val targetPage = when (sort) {
            CommentSort.Earliest -> detail?.commentPageCount ?: 1
            CommentSort.Latest -> 1
        }
        val needsReload = detail != null &&
            detail.commentPageCount > 1 &&
            detail.commentPage != targetPage
        _state.update {
            it.copy(
                commentSort = sort,
                detail = if (needsReload) detail.copy(comments = emptyList()) else it.detail,
                isLoadingDetail = needsReload || it.isLoadingDetail,
            )
        }
        if (needsReload) {
            loadCommentPage(detail.id, targetPage, replace = true)
        }
    }

    private fun loadCommentPage(articleId: String, page: Int, replace: Boolean) {
        val generation = commentGeneration
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.getArticleDetail(apiKey, articleId, page) }
            }.onSuccess { loaded ->
                if (generation != commentGeneration) return@onSuccess
                val current = _state.value.detail
                if (current?.id != articleId) return@onSuccess
                val merged = if (replace) {
                    loaded.comments
                } else {
                    mergeComments(current.comments, loaded.comments)
                }
                _state.update {
                    it.copy(
                        detail = current.copy(
                            comments = merged,
                            commentPage = loaded.commentPage,
                            commentPageCount = loaded.commentPageCount,
                        ),
                        isLoadingDetail = false,
                    )
                }
            }.onFailure {
                if (generation != commentGeneration) return@onFailure
                if (_state.value.detail?.id == articleId) setError(it.message ?: "加载评论失败")
                _state.update { it.copy(isLoadingDetail = false) }
            }
        }
    }

    private fun mergeComments(
        loadedComments: List<dev.fishpi.mobile.data.ArticleCommentView>,
        incoming: List<dev.fishpi.mobile.data.ArticleCommentView>,
    ): List<dev.fishpi.mobile.data.ArticleCommentView> {
        return (loadedComments + incoming).distinctBy { it.id }
    }

    private fun reloadDetail(articleId: String) {
        commentGeneration += 1
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.getArticleDetail(apiKey, articleId) }
            }.onSuccess { loaded ->
                if (_state.value.selected?.id != articleId) return@onSuccess
                val needsLastPage =
                    _state.value.commentSort == CommentSort.Earliest && loaded.commentPageCount > 1
                _state.update { it.copy(detail = loaded, isLoadingDetail = needsLastPage) }
                if (needsLastPage) {
                    loadCommentPage(articleId, loaded.commentPageCount, replace = true)
                }
            }.onFailure {
                if (_state.value.selected?.id == articleId) {
                    setError(it.message ?: "刷新帖子详情失败")
                    _state.update { state -> state.copy(isLoadingDetail = false) }
                }
            }
        }
    }

    private fun loadMoreComments() {
        val current = _state.value.detail ?: return
        if (_state.value.isLoadingDetail) return
        val nextPage = when (_state.value.commentSort) {
            CommentSort.Earliest -> current.commentPage - 1
            CommentSort.Latest -> current.commentPage + 1
        }
        if (nextPage < 1 || nextPage > current.commentPageCount) return
        _state.update { it.copy(isLoadingDetail = true) }
        loadCommentPage(current.id, nextPage, replace = false)
    }

    private fun sendComment() {
        val current = _state.value.detail ?: return
        val raw = _state.value.commentInput
        val attachments = _state.value.commentAttachments.syncReferences(raw)
        if (_state.value.isSendingComment) return
        if (raw.isBlank() && !attachments.hasSendableAttachment) return
        val replyId = _state.value.replyToCommentId
        _state.update { it.copy(isSendingComment = true) }
        scope.launch {
            uploadQueue.awaitSettled(attachments.pendingUploadIds)
            val settled = attachments.syncFrom(uploadQueue.uploads.value)
            val text = settled.resolve(raw).trim()
            if (text.isBlank()) {
                val reason = settled.items.firstOrNull { it.error != null }?.error
                    ?: "没有可发送的内容"
                _state.update { it.copy(commentAttachments = settled, isSendingComment = false) }
                setError(reason)
                return@launch
            }
            val failed = settled.items.filter {
                it.referenced && it.status == AttachmentUpload.Status.Failed
            }
            if (failed.isNotEmpty()) {
                emitEffect(
                    ArticleEffect.ShowError(
                        "图" + failed.joinToString("、") { it.index.toString() } + " 上传失败，已跳过",
                    ),
                )
            }
            runCatching {
                withContext(Dispatchers.IO) { api.sendArticleComment(apiKey, current.id, text, replyId) }
            }.onSuccess {
                uploadQueue.remove(settled.allUploadIds)
                if (_state.value.selected?.id == current.id) {
                    _state.update {
                        it.copy(
                            commentInput = "",
                            commentAttachments = ComposerAttachments(),
                            replyToCommentId = "",
                            replyTarget = null,
                            emojiPanelOpen = false,
                            dismissKeyboardSignal = it.dismissKeyboardSignal + 1,
                            isSendingComment = false,
                        )
                    }
                    emitEffect(ArticleEffect.ShowMessage("评论发送成功"))
                    reloadDetail(current.id)
                }
            }.onFailure {
                if (_state.value.selected?.id == current.id) setError(it.message ?: "发送评论失败")
                _state.update { it.copy(isSendingComment = false) }
            }
        }
    }

    private fun pickCommentImage(uri: android.net.Uri, isVideo: Boolean) {
        val current = _state.value
        val uploadId = uploadQueue.enqueue(apiKey, uri, isVideo)
        val (nextAttachments, nextInput, _) = current.commentAttachments.append(
            text = current.commentInput,
            cursor = current.commentInput.length,
            uploadId = uploadId,
            previewUri = uri,
            isVideo = isVideo,
        )
        _state.update { it.copy(commentInput = nextInput, commentAttachments = nextAttachments) }
    }

    private fun removeCommentImage(index: Int) {
        val current = _state.value
        val removed = current.commentAttachments.items.firstOrNull { it.index == index } ?: return
        val (nextAttachments, nextInput) = current.commentAttachments.removeAt(current.commentInput, index)
        uploadQueue.remove(removed.uploadId)
        _state.update { it.copy(commentInput = nextInput, commentAttachments = nextAttachments) }
    }

    private fun retryCommentImage(index: Int) {
        val target = _state.value.commentAttachments.items.firstOrNull { it.index == index } ?: return
        uploadQueue.retry(apiKey, target.uploadId)
    }

    private fun observeCommentUploads() {
        scope.launch {
            uploadQueue.uploads.collect { uploads ->
                val current = _state.value.commentAttachments
                if (current.isEmpty) return@collect
                val synced = current.syncFrom(uploads)
                if (synced == current) return@collect
                _state.update { it.copy(commentAttachments = synced) }
            }
        }
    }

    private fun replyToComment(comment: dev.fishpi.mobile.data.ArticleCommentView) {
        _state.update {
            it.copy(
                replyToCommentId = comment.id,
                replyTarget = comment.displayLabel(),
                replyFocusSignal = it.replyFocusSignal + 1,
            )
        }
    }

    private fun toggleEmoji() {
        val targetOpen = !_state.value.emojiPanelOpen
        _state.update { it.copy(emojiPanelOpen = targetOpen) }
        if (targetOpen && _state.value.emojiGroups.isEmpty()) {
            loadEmojiGroups()
        }
    }

    private fun loadEmojiGroups() {
        _state.update { it.copy(isLoadingEmojiPack = true, emojiPackError = null) }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.getEmojiGroups(apiKey) }
            }.onSuccess { groups ->
                val firstGroupId = groups.firstOrNull()?.id.orEmpty()
                _state.update {
                    it.copy(
                        emojiGroups = groups,
                        selectedEmojiGroupId = firstGroupId,
                        isLoadingEmojiPack = firstGroupId.isNotBlank(),
                    )
                }
                if (firstGroupId.isNotBlank()) loadEmojiGroup(firstGroupId)
            }.onFailure { err ->
                _state.update { it.copy(isLoadingEmojiPack = false, emojiPackError = err.message ?: "加载表情包分组失败") }
            }
        }
    }

    private fun loadEmojiGroup(groupId: String) {
        if (groupId.isBlank()) return
        _state.update { it.copy(selectedEmojiGroupId = groupId, isLoadingEmojiPack = true, emojiPackError = null) }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.getEmojiGroupItems(apiKey, groupId) }
            }.onSuccess { items ->
                if (_state.value.selectedEmojiGroupId == groupId) {
                    _state.update { it.copy(emojiItems = items, isLoadingEmojiPack = false) }
                }
            }.onFailure { err ->
                if (_state.value.selectedEmojiGroupId == groupId) {
                    _state.update { it.copy(isLoadingEmojiPack = false, emojiPackError = err.message ?: "加载表情包失败") }
                }
            }
        }
    }

    private fun pickEmoji(item: EmojiItemView) {
        _state.update {
            it.copy(commentInput = appendDraftBlock(it.commentInput, "![${item.name.ifBlank { "表情" }}](${item.url})"))
        }
    }

    private fun toggleFollow() {
        val following = _state.value.detail?.following == true
        if (following) {
            runArticleAction("取消收藏") { articleId -> api.unfollowArticle(apiKey, articleId) }
        } else {
            runArticleAction("收藏") { articleId -> api.followArticle(apiKey, articleId) }
        }
    }

    private fun voteComment(comment: dev.fishpi.mobile.data.ArticleCommentView, like: Boolean) {
        val commentId = comment.id.trim()
        if (commentId.isBlank() || _state.value.isArticleActionRunning) return
        _state.update { it.copy(isArticleActionRunning = true, error = null) }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.voteComment(apiKey, commentId, like) }
            }.onSuccess {
                updateCommentVote(commentId, like)
                emitEffect(ArticleEffect.ShowMessage(if (like) "评论点赞成功" else "评论点踩成功"))
            }.onFailure {
                setError(it.message ?: "评论点赞失败")
                emitEffect(ArticleEffect.ShowError(it.message ?: "评论点赞失败"))
            }
            _state.update { it.copy(isArticleActionRunning = false) }
        }
    }

    private fun thankComment(comment: dev.fishpi.mobile.data.ArticleCommentView) {
        val commentId = comment.id.trim()
        if (commentId.isBlank() || comment.thanked || _state.value.isArticleActionRunning) return
        _state.update { it.copy(isArticleActionRunning = true, error = null) }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.thankComment(apiKey, commentId) }
            }.onSuccess {
                updateCommentThank(commentId)
                emitEffect(ArticleEffect.ShowMessage("评论感谢成功"))
            }.onFailure {
                setError(it.message ?: "评论感谢失败")
                emitEffect(ArticleEffect.ShowError(it.message ?: "评论感谢失败"))
            }
            _state.update { it.copy(isArticleActionRunning = false) }
        }
    }

    private fun updateCommentVote(commentId: String, like: Boolean) {
        _state.update { state ->
            val detail = state.detail ?: return@update state
            state.copy(
                detail = detail.copy(
                    comments = detail.comments.map { comment ->
                        if (comment.id != commentId) return@map comment
                        val nextVote = if (like) 1 else -1
                        val oldVote = comment.voteState
                        val resolvedVote = if (oldVote == nextVote) 0 else nextVote
                        comment.copy(
                            voteState = resolvedVote,
                            goodCount = (comment.goodCount + voteDelta(oldVote, resolvedVote, 1)).coerceAtLeast(0),
                            badCount = (comment.badCount + voteDelta(oldVote, resolvedVote, -1)).coerceAtLeast(0),
                        )
                    },
                ),
            )
        }
    }

    private fun updateCommentThank(commentId: String) {
        _state.update { state ->
            val detail = state.detail ?: return@update state
            state.copy(
                detail = detail.copy(
                    comments = detail.comments.map { comment ->
                        if (comment.id == commentId && !comment.thanked) {
                            comment.copy(thanked = true, thankCount = comment.thankCount + 1)
                        } else {
                            comment
                        }
                    },
                ),
            )
        }
    }

    private fun voteDelta(oldVote: Int, newVote: Int, targetVote: Int): Long =
        (if (newVote == targetVote) 1L else 0L) - (if (oldVote == targetVote) 1L else 0L)

    private fun rewardArticle() {
        val current = _state.value.detail ?: return
        if (_state.value.isRewarding || _state.value.isArticleActionRunning) return
        _state.update { it.copy(rewardConfirmOpen = false, isRewarding = true) }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.rewardArticle(apiKey, current.id) }
            }.onSuccess {
                emitEffect(ArticleEffect.ShowMessage("打赏成功"))
                reloadDetail(current.id)
            }.onFailure {
                emitEffect(ArticleEffect.ShowError(it.message ?: "打赏失败"))
            }
            _state.update { it.copy(isRewarding = false) }
        }
    }

    private fun runArticleAction(label: String, action: suspend (String) -> Unit) {
        val current = _state.value.detail ?: return
        if (_state.value.isArticleActionRunning) return
        _state.update { it.copy(isArticleActionRunning = true, error = null) }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { action(current.id) }
            }.onSuccess {
                emitEffect(ArticleEffect.ShowMessage("${label}成功"))
                reloadDetail(current.id)
            }.onFailure {
                setError(it.message ?: "${label}失败")
                emitEffect(ArticleEffect.ShowError(it.message ?: "${label}失败"))
            }
            _state.update { it.copy(isArticleActionRunning = false) }
        }
    }

    private fun refreshArticleHeat(articleId: String) {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.getArticleHeat(apiKey, articleId) }
            }.onSuccess { heat ->
                if (_state.value.selected?.id == articleId) {
                    _state.update { it.copy(articleHeat = heat) }
                }
            }.onFailure {
                if (_state.value.selected?.id == articleId && _state.value.articleHeat == null) {
                    _state.update { it.copy(articleHeat = 0) }
                }
            }
        }
    }

    private fun shareSelectedArticle() {
        val summary = _state.value.selected ?: return
        emitEffect(ArticleEffect.ShareArticle(summary.title, summary.id))
    }

    private fun onPublishCompleted(articleId: String) {
        _state.update { it.copy(publishOpen = false) }
        loadArticles(page = 1, append = false)
        if (articleId.isNotBlank()) openArticleById(articleId)
    }

    private fun setError(message: String) {
        _state.update { it.copy(error = message) }
    }

    private fun emitEffect(effect: ArticleEffect) {
        _effects.tryEmit(effect)
    }

    private fun dev.fishpi.mobile.data.ArticleCommentView.displayLabel(): String =
        displayName.ifBlank { author.ifBlank { userName } }
}
