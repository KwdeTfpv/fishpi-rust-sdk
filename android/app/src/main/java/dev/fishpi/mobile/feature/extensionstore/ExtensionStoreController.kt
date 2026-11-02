package dev.fishpi.mobile.feature.extensionstore

import dev.fishpi.mobile.core.ui.UiController
import dev.fishpi.mobile.data.ExtensionStoreClient
import dev.fishpi.mobile.data.ExtensionStoreItem
import dev.fishpi.mobile.data.ExtensionStoreSession
import dev.fishpi.mobile.data.ExtensionStoreUploadRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ExtensionStoreController(
    private val apiKey: String,
    private val client: ExtensionStoreClient = ExtensionStoreClient.shared,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : UiController<ExtensionStoreState, ExtensionStoreAction> {
    private val _state = MutableStateFlow(ExtensionStoreState())
    override val state: StateFlow<ExtensionStoreState> = _state

    private val _effects = MutableSharedFlow<ExtensionStoreEffect>(extraBufferCapacity = 16)
    val effects: SharedFlow<ExtensionStoreEffect> = _effects.asSharedFlow()

    private var initialized = false
    private var searchJob: Job? = null
    private var loadRequestId = 0
    private var draftsRequestId = 0
    private var draftOpenRequestId = 0

    override fun dispatch(action: ExtensionStoreAction) {
        when (action) {
            ExtensionStoreAction.Initialize -> initialize()
            ExtensionStoreAction.Refresh -> {
                if (_state.value.session == null) initialize(force = true) else loadStore(reset = true)
            }
            ExtensionStoreAction.RetryAuth -> initialize(force = true)
            ExtensionStoreAction.LoadMore -> loadStore(reset = false)
            is ExtensionStoreAction.ChangeFilter -> {
                _state.update { it.copy(selectedFilter = action.filter) }
                loadStore(reset = true)
            }
            is ExtensionStoreAction.ChangeQuery -> {
                _state.update { it.copy(query = action.query) }
                searchJob?.cancel()
                searchJob = scope.launch {
                    delay(280)
                    loadStore(reset = true)
                }
            }
            is ExtensionStoreAction.Purchase -> purchaseItem(action.item)
            is ExtensionStoreAction.Upload -> uploadItem(action.request)
            ExtensionStoreAction.LoadDrafts -> loadDrafts(reset = true)
            ExtensionStoreAction.LoadMoreDrafts -> loadDrafts(reset = false)
            is ExtensionStoreAction.OpenDraft -> openDraft(action.item)
            ExtensionStoreAction.ClearEditingDraft -> _state.update { it.copy(editingDraft = null) }
            is ExtensionStoreAction.DeleteDraft -> deleteDraft(action.item)
        }
    }

    fun close() {
        searchJob?.cancel()
        scope.cancel()
    }

    private fun initialize(force: Boolean = false) {
        if (!force && initialized) return
        if (_state.value.isAuthenticating) return
        initialized = true
        if (apiKey.isBlank()) {
            _state.update {
                it.copy(
                    authError = "请先登录后使用鱼排扩展集市",
                    items = emptyList(),
                    total = 0,
                    page = 0,
                    hasMore = false,
                )
            }
            return
        }
        scope.launch {
            _state.update { it.copy(isAuthenticating = true, authError = null) }
            runCatching {
                withContext(Dispatchers.IO) { client.getToken(apiKey) }
            }.onSuccess { session ->
                _state.update { it.copy(session = session, isAuthenticating = false) }
                loadStore(session, reset = true)
            }.onFailure { throwable ->
                initialized = false
                _state.update {
                    it.copy(
                        authError = throwable.message ?: "鱼排扩展集市鉴权失败",
                        isAuthenticating = false,
                        items = emptyList(),
                        total = 0,
                        page = 0,
                        hasMore = false,
                    )
                }
            }
        }
    }

    private fun loadStore(
        activeSession: ExtensionStoreSession? = _state.value.session,
        reset: Boolean = true,
    ) {
        if (!reset && (_state.value.isLoading || _state.value.isLoadingMore || !_state.value.hasMore)) return
        val requestId = ++loadRequestId
        val snapshot = _state.value
        val nextPage = if (reset) 1 else snapshot.page + 1
        scope.launch {
            _state.update {
                it.copy(
                    isLoading = reset,
                    isLoadingMore = !reset,
                    loadError = null,
                )
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    val pageTask = async {
                        client.getPublishedItems(
                            search = snapshot.query.trim(),
                            type = snapshot.selectedFilter.type,
                            page = nextPage,
                            limit = 12,
                        )
                    }
                    val purchasesTask = if (reset) {
                        async { activeSession?.let { client.getMyPurchases(it.accessToken) }.orEmpty() }
                    } else {
                        null
                    }
                    pageTask.await() to (purchasesTask?.await() ?: snapshot.purchasedItems)
                }
            }.onSuccess { (page, purchases) ->
                if (requestId == loadRequestId) {
                    _state.update {
                        val merged = if (reset) {
                            page.items
                        } else {
                            (it.items + page.items).distinctBy { item -> item.id }
                        }
                        it.copy(
                            items = merged,
                            total = page.total,
                            page = nextPage,
                            hasMore = page.items.isNotEmpty() && merged.size < page.total,
                            purchasedItems = purchases,
                            isLoading = false,
                            isLoadingMore = false,
                        )
                    }
                }
            }.onFailure { throwable ->
                if (requestId == loadRequestId) {
                    if (!reset || snapshot.items.isNotEmpty()) {
                        emitEffect(
                            ExtensionStoreEffect.ShowError(
                                if (reset) {
                                    "刷新失败：${throwable.message ?: "未知错误"}"
                                } else {
                                    "加载更多失败：${throwable.message ?: "未知错误"}"
                                },
                            ),
                        )
                    }
                    _state.update {
                        it.copy(
                            loadError = if (reset) throwable.message ?: "鱼排扩展集市加载失败" else it.loadError,
                            isLoading = false,
                            isLoadingMore = false,
                        )
                    }
                }
            }
        }
    }

    private fun purchaseItem(item: ExtensionStoreItem) {
        val token = _state.value.session?.accessToken
        if (token.isNullOrBlank()) {
            emitEffect(ExtensionStoreEffect.ShowError("请先完成集市鉴权"))
            return
        }
        if (_state.value.purchasingId != null) return
        scope.launch {
            _state.update { it.copy(purchasingId = item.id) }
            val purchased = runCatching {
                withContext(Dispatchers.IO) { client.purchaseItem(token, item.id) }
            }
            purchased.onFailure { throwable ->
                _state.update { it.copy(purchasingId = null) }
                emitEffect(ExtensionStoreEffect.ShowError("购买失败：${throwable.message ?: "未知错误"}"))
                return@launch
            }
            val purchasedItem = purchased.getOrThrow().copy(isPurchased = true)
            _state.update {
                it.copy(
                    purchasedItems = (it.purchasedItems.filterNot { current -> current.id == purchasedItem.id } + purchasedItem)
                        .sortedByDescending { current -> current.id },
                    purchasingId = null,
                )
            }
            emitEffect(ExtensionStoreEffect.ShowMessage("${item.displayName()} 已购买"))
            runCatching {
                withContext(Dispatchers.IO) { client.getMyPurchases(token) }
            }.onSuccess { purchases ->
                _state.update { it.copy(purchasedItems = purchases) }
            }.onFailure { throwable ->
                emitEffect(ExtensionStoreEffect.ShowError("购买成功，但购买列表刷新失败：${throwable.message ?: "未知错误"}"))
            }
        }
    }

    private fun uploadItem(request: ExtensionStoreUploadRequest) {
        val token = _state.value.session?.accessToken
        if (token.isNullOrBlank()) {
            emitEffect(ExtensionStoreEffect.ShowError("请先完成集市鉴权"))
            return
        }
        if (_state.value.isUploading) return
        val draftId = request.draftId
        scope.launch {
            _state.update { it.copy(isUploading = true) }
            runCatching {
                withContext(Dispatchers.IO) {
                    when {
                        // 编辑已有草稿并提交审核：先保存最新改动到草稿，再走发布草稿接口
                        draftId != null && !request.isDraft -> {
                            client.updateDraft(token, draftId, request.copy(isDraft = true))
                            client.publishDraft(token, draftId)
                        }
                        // 编辑已有草稿并存草稿：更新原草稿，不再新建副本
                        draftId != null -> client.updateDraft(token, draftId, request)
                        // 新建作品：发布或首次存草稿
                        else -> client.uploadItem(token, request)
                    }
                }
            }.onSuccess {
                _state.update {
                    it.copy(
                        isUploading = false,
                        uploadSuccessCount = it.uploadSuccessCount + 1,
                    )
                }
                emitEffect(ExtensionStoreEffect.UploadFinished)
                emitEffect(ExtensionStoreEffect.ShowMessage(uploadSuccessMessage(request)))
                loadStore()
                if (_state.value.drafts.isNotEmpty() || _state.value.isLoadingDrafts) {
                    loadDrafts(reset = true)
                }
            }.onFailure { throwable ->
                _state.update { it.copy(isUploading = false) }
                emitEffect(
                    ExtensionStoreEffect.ShowError(
                        "${uploadActionLabel(request)}失败：${throwable.message ?: "未知错误"}",
                    ),
                )
            }
        }
    }

    private fun uploadActionLabel(request: ExtensionStoreUploadRequest): String =
        if (request.isDraft) "保存草稿" else "发布"

    private fun uploadSuccessMessage(request: ExtensionStoreUploadRequest): String =
        if (request.isDraft) "草稿已保存" else "发布成功，作品已进入审核流程"

    private fun deleteDraft(item: ExtensionStoreItem) {
        val token = _state.value.session?.accessToken
        if (token.isNullOrBlank()) {
            emitEffect(ExtensionStoreEffect.ShowError("请先完成集市鉴权"))
            return
        }
        if (_state.value.deletingDraftId != null) return
        scope.launch {
            _state.update { it.copy(deletingDraftId = item.id) }
            runCatching {
                withContext(Dispatchers.IO) { client.deleteItem(token, item.id) }
            }.onSuccess {
                _state.update {
                    it.copy(
                        deletingDraftId = null,
                        drafts = it.drafts.filterNot { draft -> draft.id == item.id },
                    )
                }
                emitEffect(ExtensionStoreEffect.ShowMessage("${item.displayName()} 已删除"))
            }.onFailure { throwable ->
                _state.update { it.copy(deletingDraftId = null) }
                emitEffect(ExtensionStoreEffect.ShowError("删除草稿失败：${throwable.message ?: "未知错误"}"))
            }
        }
    }


    private fun loadDrafts(reset: Boolean) {
        if (!reset && (_state.value.isLoadingDrafts || _state.value.isLoadingMoreDrafts || !_state.value.draftsHasMore)) return
        val token = _state.value.session?.accessToken
        if (token.isNullOrBlank()) {
            _state.update { it.copy(draftsError = "请先完成集市鉴权", drafts = emptyList()) }
            return
        }
        val requestId = ++draftsRequestId
        val snapshot = _state.value
        val nextPage = if (reset) 1 else snapshot.draftsPage + 1
        scope.launch {
            _state.update {
                it.copy(
                    isLoadingDrafts = reset,
                    isLoadingMoreDrafts = !reset,
                    draftsError = null,
                )
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    client.getMyDrafts(token, page = nextPage, limit = 12)
                }
            }.onSuccess { drafts ->
                if (requestId == draftsRequestId) {
                    _state.update {
                        val merged = if (reset) {
                            drafts
                        } else {
                            (it.drafts + drafts).distinctBy { draft -> draft.id }
                        }
                        it.copy(
                            drafts = merged,
                            draftsPage = nextPage,
                            draftsHasMore = drafts.size >= 12,
                            isLoadingDrafts = false,
                            isLoadingMoreDrafts = false,
                        )
                    }
                }
            }.onFailure { throwable ->
                if (requestId == draftsRequestId) {
                    if (!reset || snapshot.drafts.isNotEmpty()) {
                        emitEffect(
                            ExtensionStoreEffect.ShowError(
                                if (reset) {
                                    "刷新草稿失败：${throwable.message ?: "未知错误"}"
                                } else {
                                    "加载更多草稿失败：${throwable.message ?: "未知错误"}"
                                },
                            ),
                        )
                    }
                    _state.update {
                        it.copy(
                            draftsError = if (reset) throwable.message ?: "草稿加载失败" else it.draftsError,
                            isLoadingDrafts = false,
                            isLoadingMoreDrafts = false,
                        )
                    }
                }
            }
        }
    }

    private fun openDraft(item: ExtensionStoreItem) {
        if (_state.value.openingDraftId != null) return
        val token = _state.value.session?.accessToken
        val requestId = ++draftOpenRequestId
        scope.launch {
            _state.update { it.copy(openingDraftId = item.id) }
            runCatching {
                // 草稿列表不含正文/标识符，需拉全量详情后再填充编辑表单
                withContext(Dispatchers.IO) { client.getItem(item.id, token) }
            }.onSuccess { full ->
                if (requestId == draftOpenRequestId) {
                    _state.update { it.copy(openingDraftId = null, editingDraft = full) }
                }
            }.onFailure { throwable ->
                if (requestId == draftOpenRequestId) {
                    _state.update { it.copy(openingDraftId = null) }
                    emitEffect(ExtensionStoreEffect.ShowError("打开草稿失败：${throwable.message ?: "未知错误"}"))
                }
            }
        }
    }

    private fun emitEffect(effect: ExtensionStoreEffect) {
        _effects.tryEmit(effect)
    }
}
