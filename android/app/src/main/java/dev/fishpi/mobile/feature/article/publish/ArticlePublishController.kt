package dev.fishpi.mobile.feature.article.publish

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.fishpi.mobile.core.ui.UiController
import dev.fishpi.mobile.data.ArticleDraftPayload
import dev.fishpi.mobile.data.FishPiApiClient
import dev.fishpi.mobile.utils.appendDraftBlock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ArticlePublishController(
    private val apiKey: String,
    private val api: FishPiApiClient = FishPiApiClient.shared,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : UiController<ArticlePublishState, ArticlePublishAction> {
    private val _state = MutableStateFlow(ArticlePublishState())
    override val state: StateFlow<ArticlePublishState> = _state

    private val _effects = MutableSharedFlow<ArticlePublishEffect>(extraBufferCapacity = 16)
    val effects: SharedFlow<ArticlePublishEffect> = _effects.asSharedFlow()

    private var initialized = false

    override fun dispatch(action: ArticlePublishAction) {
        when (action) {
            ArticlePublishAction.Initialize -> initialize()
            ArticlePublishAction.Close -> emitEffect(ArticlePublishEffect.Closed)
            is ArticlePublishAction.ChangeTitle -> _state.update { it.copy(title = action.value.take(255), titleError = null) }
            is ArticlePublishAction.ChangeContent -> _state.update { it.copy(content = action.value.take(1_024_000), contentError = null) }
            is ArticlePublishAction.ChangeTags -> _state.update { it.copy(tags = action.value.copy(text = action.value.text.take(255)), tagsError = null) }
            is ArticlePublishAction.ChangeRewardContent -> _state.update { it.copy(rewardContent = action.value.take(102_400)) }
            is ArticlePublishAction.ChangeRewardPoint -> _state.update { it.copy(rewardPoint = action.value.filter(Char::isDigit).take(6)) }
            is ArticlePublishAction.ChangeQnaOfferPoint -> _state.update { it.copy(qnaOfferPoint = action.value.filter(Char::isDigit).take(6)) }
            is ArticlePublishAction.ChangeCommentable -> _state.update { it.copy(commentable = action.value) }
            is ArticlePublishAction.ChangeAnonymous -> _state.update { it.copy(anonymous = action.value) }
            is ArticlePublishAction.ChangeNotifyFollowers -> _state.update { it.copy(notifyFollowers = action.value) }
            is ArticlePublishAction.ChangeShowInList -> _state.update { it.copy(showInList = action.value) }
            is ArticlePublishAction.ChangeOriginalStatement -> _state.update { it.copy(originalStatement = action.value) }
            ArticlePublishAction.LoadDrafts -> loadDrafts(openDialog = false)
            ArticlePublishAction.OpenDrafts -> loadDrafts(openDialog = true)
            ArticlePublishAction.DismissDrafts -> _state.update { it.copy(showDrafts = false) }
            is ArticlePublishAction.OpenDraft -> loadDraftDetail(action.draftId)
            is ArticlePublishAction.DeleteDraft -> deleteDraft(action.draftId)
            ArticlePublishAction.SaveDraft -> saveDraft()
            ArticlePublishAction.RequestPublish -> requestPublish()
            ArticlePublishAction.ConfirmGoodArticlePublish -> publish(isGoodArticle = true)
            ArticlePublishAction.ConfirmNormalPublish -> publish(isGoodArticle = false)
            ArticlePublishAction.DismissGoodArticleConfirm -> _state.update { it.copy(pendingPublishPayload = null) }
            ArticlePublishAction.PickContentImage -> {
                _state.update { it.copy(imageInsertTarget = ArticlePublishEditorTarget.Content) }
                emitEffect(ArticlePublishEffect.OpenContentImagePicker)
            }
            ArticlePublishAction.PickRewardImage -> {
                _state.update { it.copy(imageInsertTarget = ArticlePublishEditorTarget.Reward) }
                emitEffect(ArticlePublishEffect.OpenRewardImagePicker)
            }
            is ArticlePublishAction.UploadContentImage -> uploadAndInsertImage(action.path, ArticlePublishEditorTarget.Content)
            is ArticlePublishAction.UploadRewardImage -> uploadAndInsertImage(action.path, ArticlePublishEditorTarget.Reward)
            is ArticlePublishAction.ShowPickerError -> setError(action.message)
            is ArticlePublishAction.InsertContentBlock -> _state.update { it.copy(content = appendDraftBlock(it.content, action.text)) }
            is ArticlePublishAction.InsertRewardBlock -> _state.update { it.copy(rewardContent = appendDraftBlock(it.rewardContent, action.text)) }
            is ArticlePublishAction.AppendTag -> _state.update { it.copy(tags = articleTagsFieldValue(appendArticleTag(it.tags.text, action.tag)), tagsError = null) }
            is ArticlePublishAction.ShowPreview -> _state.update { it.copy(previewTarget = action.target, showPreview = true) }
            ArticlePublishAction.DismissPreview -> _state.update { it.copy(showPreview = false) }
            ArticlePublishAction.ClearError -> _state.update { it.copy(error = null, titleError = null, contentError = null, tagsError = null) }
        }
    }

    fun close() {
        scope.cancel()
    }

    private fun initialize() {
        if (initialized) return
        initialized = true
        loadDrafts(openDialog = false)
    }

    private fun payload(): ArticleDraftPayload {
        val state = _state.value
        return ArticleDraftPayload(
            draftId = state.draftId,
            title = state.title.trim(),
            content = state.content,
            tags = normalizedArticleTags(state.tags.text),
            rewardContent = state.rewardContent,
            rewardPoint = state.rewardPoint.trim(),
            qnaOfferPoint = state.qnaOfferPoint.trim().toIntOrNull() ?: 0,
            commentable = state.commentable,
            anonymous = state.anonymous,
            notifyFollowers = state.notifyFollowers,
            showInList = if (state.showInList) 1 else 0,
            statement = if (state.originalStatement) 1 else 0,
        )
    }

    private fun validatePayload(): ArticleDraftPayload? {
        val next = payload()
        val tagCount = next.tags.split(",").count { it.isNotBlank() }
        return when {
            next.title.isBlank() -> {
                _state.update { it.copy(titleError = "标题不能为空", contentError = null, tagsError = null) }
                null
            }
            next.content.isBlank() -> {
                _state.update { it.copy(titleError = null, contentError = "正文不能为空", tagsError = null) }
                null
            }
            tagCount > 4 -> {
                _state.update { it.copy(titleError = null, contentError = null, tagsError = "最多填写 4 个标签") }
                null
            }
            else -> {
                _state.update { it.copy(titleError = null, contentError = null, tagsError = null, error = null) }
                next
            }
        }
    }

    private fun loadDrafts(openDialog: Boolean) {
        if (_state.value.loadingDrafts) return
        _state.update { it.copy(loadingDrafts = true, error = null) }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.getArticleDrafts(apiKey) }
            }.onSuccess { drafts ->
                _state.update { it.copy(drafts = drafts, showDrafts = openDialog, loadingDrafts = false) }
            }.onFailure {
                setError(it.message ?: "加载草稿失败")
                _state.update { state -> state.copy(loadingDrafts = false) }
            }
        }
    }

    private fun loadDraftDetail(id: String) {
        _state.update { it.copy(showDrafts = false, submitting = true, error = null) }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.getArticleDraftDetail(apiKey, id) }
            }.onSuccess { draft ->
                _state.update {
                    it.copy(
                        draftId = draft.id,
                        title = draft.title,
                        content = draft.content,
                        tags = articleTagsFieldValue(draft.tags),
                        rewardContent = draft.rewardContent,
                        rewardPoint = draft.rewardPoint,
                        qnaOfferPoint = if (draft.qnaOfferPoint > 0) draft.qnaOfferPoint.toString() else "",
                        commentable = draft.commentable,
                        anonymous = draft.anonymous,
                        notifyFollowers = draft.notifyFollowers,
                        showInList = draft.showInList != 0,
                        originalStatement = draft.statement != 0,
                        titleError = null,
                        contentError = null,
                        tagsError = null,
                        submitting = false,
                    )
                }
            }.onFailure {
                setError(it.message ?: "加载草稿详情失败")
                _state.update { state -> state.copy(submitting = false) }
            }
        }
    }

    private fun saveDraft() {
        val next = validatePayload() ?: return
        _state.update { it.copy(submitting = true, error = null) }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.saveArticleDraft(apiKey, next) }
            }.onSuccess { draft ->
                _state.update { it.copy(draftId = draft.id, submitting = false) }
                emitEffect(ArticlePublishEffect.ShowMessage("草稿已保存"))
                loadDrafts(openDialog = false)
            }.onFailure {
                setError(it.message ?: "保存草稿失败")
                _state.update { state -> state.copy(submitting = false) }
            }
        }
    }

    private fun deleteDraft(id: String) {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.deleteArticleDraft(apiKey, id) }
            }.onSuccess {
                _state.update {
                    it.copy(
                        drafts = it.drafts.filterNot { draft -> draft.id == id },
                        draftId = if (it.draftId == id) "" else it.draftId,
                    )
                }
                emitEffect(ArticlePublishEffect.ShowMessage("草稿已删除"))
            }.onFailure {
                emitEffect(ArticlePublishEffect.ShowError(it.message ?: "删除草稿失败"))
            }
        }
    }

    private fun requestPublish() {
        val next = validatePayload() ?: return
        _state.update { it.copy(pendingPublishPayload = next) }
    }

    private fun publish(isGoodArticle: Boolean) {
        val next = _state.value.pendingPublishPayload ?: return
        _state.update { it.copy(pendingPublishPayload = null, submitting = true, error = null) }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.publishArticle(apiKey, next, isGoodArticle = isGoodArticle) }
            }.onSuccess { articleId ->
                emitEffect(ArticlePublishEffect.ShowMessage("发布成功"))
                emitEffect(ArticlePublishEffect.Published(articleId))
                _state.update { it.copy(submitting = false) }
            }.onFailure {
                setError(it.message ?: "发布帖子失败")
                _state.update { state -> state.copy(submitting = false) }
            }
        }
    }

    private fun uploadAndInsertImage(path: String, target: ArticlePublishEditorTarget) {
        if (_state.value.uploadingImage) return
        _state.update { it.copy(uploadingImage = true, error = null) }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.uploadChatFile(apiKey, path) }
            }.onSuccess { uploaded ->
                _state.update {
                    if (target == ArticlePublishEditorTarget.Reward) {
                        it.copy(rewardContent = appendDraftBlock(it.rewardContent, uploaded.markdown), uploadingImage = false)
                    } else {
                        it.copy(content = appendDraftBlock(it.content, uploaded.markdown), uploadingImage = false)
                    }
                }
                emitEffect(ArticlePublishEffect.ShowMessage(if (target == ArticlePublishEditorTarget.Reward) "图片已插入打赏区" else "图片已插入正文"))
            }.onFailure {
                setError(it.message ?: "上传图片失败")
                _state.update { state -> state.copy(uploadingImage = false) }
            }
        }
    }

    private fun setError(message: String) {
        _state.update { it.copy(error = message) }
        emitEffect(ArticlePublishEffect.ShowError(message))
    }

    private fun emitEffect(effect: ArticlePublishEffect) {
        _effects.tryEmit(effect)
    }
}

internal fun normalizedArticleTags(raw: String): String =
    raw.split(",", "，", " ", "#")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(",")

internal fun articleTagsFieldValue(text: String): TextFieldValue =
    TextFieldValue(text, selection = TextRange(text.length))

internal fun appendArticleTag(raw: String, tag: String): String {
    val current = normalizedArticleTags(raw)
    val next = (current.split(",").filter { it.isNotBlank() } + tag.trim())
        .filter { it.isNotBlank() }
        .distinct()
        .take(4)
    return next.joinToString(",")
}
