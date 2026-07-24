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

    override fun dispatch(action: ExtensionStoreAction) {
        when (action) {
            ExtensionStoreAction.Initialize -> initialize()
            ExtensionStoreAction.Refresh -> loadStore()
            is ExtensionStoreAction.ChangeFilter -> {
                _state.update { it.copy(selectedFilter = action.filter) }
                loadStore()
            }
            is ExtensionStoreAction.ChangeQuery -> {
                _state.update { it.copy(query = action.query) }
                searchJob?.cancel()
                searchJob = scope.launch {
                    delay(280)
                    loadStore()
                }
            }
            is ExtensionStoreAction.Purchase -> purchaseItem(action.item)
            is ExtensionStoreAction.Upload -> uploadItem(action.request)
            ExtensionStoreAction.LoadDrafts -> loadDrafts()
            is ExtensionStoreAction.OpenDraft -> openDraft(action.item)
            ExtensionStoreAction.ClearEditingDraft -> _state.update { it.copy(editingDraft = null) }
            is ExtensionStoreAction.DeleteDraft -> deleteDraft(action.item)
        }
    }

    fun close() {
        searchJob?.cancel()
    }

    private fun initialize() {
        if (initialized) return
        initialized = true
        if (apiKey.isBlank()) {
            _state.update {
                it.copy(authError = "请先登录后使用鱼排扩展集市", items = emptyList())
            }
            return
        }
        scope.launch {
            _state.update { it.copy(isAuthenticating = true, authError = null) }
            runCatching {
                withContext(Dispatchers.IO) { client.getToken(apiKey) }
            }.onSuccess { session ->
                _state.update { it.copy(session = session, isAuthenticating = false) }
                loadStore(session)
            }.onFailure { throwable ->
                _state.update {
                    it.copy(
                        authError = throwable.message ?: "鱼排扩展集市鉴权失败",
                        isAuthenticating = false,
                        items = emptyList(),
                    )
                }
            }
        }
    }

    private fun loadStore(activeSession: ExtensionStoreSession? = _state.value.session) {
        val requestId = ++loadRequestId
        val snapshot = _state.value
        scope.launch {
            _state.update { it.copy(isLoading = true, loadError = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    val pageTask = async {
                        client.getPublishedItems(
                            search = snapshot.query.trim(),
                            type = snapshot.selectedFilter.type,
                            limit = 12,
                        )
                    }
                    val purchasesTask = async {
                        activeSession?.let { client.getMyPurchases(it.accessToken) }.orEmpty()
                    }
                    pageTask.await() to purchasesTask.await()
                }
            }.onSuccess { (page, purchases) ->
                if (requestId == loadRequestId) {
                    _state.update {
                        it.copy(
                            items = page.items,
                            total = page.total,
                            purchasedItems = purchases,
                            isLoading = false,
                        )
                    }
                }
            }.onFailure { throwable ->
                if (requestId == loadRequestId) {
                    _state.update {
                        it.copy(
                            loadError = throwable.message ?: "鱼排扩展集市加载失败",
                            isLoading = false,
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
            runCatching {
                withContext(Dispatchers.IO) {
                    client.purchaseItem(token, item.id)
                    client.getMyPurchases(token)
                }
            }.onSuccess { purchases ->
                _state.update { it.copy(purchasedItems = purchases, purchasingId = null) }
                emitEffect(ExtensionStoreEffect.ShowMessage("${item.displayName()} 已购买"))
            }.onFailure { throwable ->
                _state.update { it.copy(purchasingId = null) }
                emitEffect(ExtensionStoreEffect.ShowError("购买失败：${throwable.message ?: "未知错误"}"))
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
                    loadDrafts()
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


    private fun loadDrafts() {
        val token = _state.value.session?.accessToken
        if (token.isNullOrBlank()) {
            _state.update { it.copy(draftsError = "请先完成集市鉴权", drafts = emptyList()) }
            return
        }
        scope.launch {
            _state.update { it.copy(isLoadingDrafts = true, draftsError = null) }
            runCatching {
                withContext(Dispatchers.IO) { client.getMyDrafts(token) }
            }.onSuccess { drafts ->
                _state.update { it.copy(drafts = drafts, isLoadingDrafts = false) }
            }.onFailure { throwable ->
                _state.update {
                    it.copy(
                        draftsError = throwable.message ?: "草稿加载失败",
                        isLoadingDrafts = false,
                    )
                }
            }
        }
    }

    private fun openDraft(item: ExtensionStoreItem) {
        if (_state.value.openingDraftId != null) return
        val token = _state.value.session?.accessToken
        scope.launch {
            _state.update { it.copy(openingDraftId = item.id) }
            runCatching {
                // 草稿列表不含正文/标识符，需拉全量详情后再填充编辑表单
                withContext(Dispatchers.IO) { client.getItem(item.id, token) }
            }.onSuccess { full ->
                _state.update { it.copy(openingDraftId = null, editingDraft = full) }
            }.onFailure { throwable ->
                _state.update { it.copy(openingDraftId = null) }
                emitEffect(ExtensionStoreEffect.ShowError("打开草稿失败：${throwable.message ?: "未知错误"}"))
            }
        }
    }

    private fun emitEffect(effect: ExtensionStoreEffect) {
        _effects.tryEmit(effect)
    }
}
