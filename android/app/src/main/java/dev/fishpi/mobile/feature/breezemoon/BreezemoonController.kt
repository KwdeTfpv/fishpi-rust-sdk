package dev.fishpi.mobile.feature.breezemoon

import android.content.Context
import dev.fishpi.mobile.data.AttachmentUpload
import dev.fishpi.mobile.data.AttachmentUploadQueue
import dev.fishpi.mobile.data.ComposerAttachments
import dev.fishpi.mobile.data.FishPiApiClient
import dev.fishpi.mobile.utils.appendDraftBlock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext

internal class BreezemoonController(
    context: Context,
    private val apiKey: String,
    private val api: FishPiApiClient = FishPiApiClient.shared,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(BreezemoonState())
    val state: StateFlow<BreezemoonState> = _state

    private val effects = Channel<BreezemoonEffect>(Channel.BUFFERED)
    val effect = effects.receiveAsFlow()

    private val uploadQueue = AttachmentUploadQueue.shared(context)

    init {
        observeAttachmentUploads()
    }

    fun dispatch(action: BreezemoonAction) {
        when (action) {
            BreezemoonAction.Initialize -> {
                _state.update { it.copy(shouldScrollToBottom = true) }
                load(page = 1, append = false)
            }
            BreezemoonAction.Refresh -> load(page = 1, append = false)
            BreezemoonAction.LoadMore -> {
                val current = _state.value
                if (current.items.isNotEmpty() && current.hasMore && !current.isLoading) {
                    load(page = current.nextPage, append = true)
                }
            }
            BreezemoonAction.Publish -> publish()
            is BreezemoonAction.ChangeInput -> _state.update {
                it.copy(composeInput = action.value, attachments = it.attachments.syncReferences(action.value))
            }
            BreezemoonAction.OpenAttachmentPanel -> _state.update { it.copy(attachmentPanelOpen = true, emojiPanelOpen = false) }
            BreezemoonAction.CloseAttachmentPanel -> _state.update { it.copy(attachmentPanelOpen = false) }
            BreezemoonAction.ToggleAttachmentPanel -> _state.update { it.copy(attachmentPanelOpen = !it.attachmentPanelOpen, emojiPanelOpen = false) }
            BreezemoonAction.RequestGalleryAttachment -> {
                _state.update { it.copy(attachmentPanelOpen = false) }
                emit(BreezemoonEffect.OpenGalleryPicker)
            }
            BreezemoonAction.RequestCameraAttachment -> {
                _state.update { it.copy(attachmentPanelOpen = false) }
                emit(BreezemoonEffect.OpenCameraPicker)
            }
            is BreezemoonAction.PickAttachment -> pickAttachment(action.uri, action.isVideo)
            is BreezemoonAction.RemoveAttachment -> removeAttachment(action.index)
            is BreezemoonAction.RetryAttachment -> retryAttachment(action.index)
            BreezemoonAction.ToggleEmoji -> toggleEmoji()
            BreezemoonAction.CloseEmoji -> _state.update { it.copy(emojiPanelOpen = false) }
            is BreezemoonAction.SelectEmojiGroup -> loadEmojiGroup(action.groupId)
            is BreezemoonAction.PickEmoji -> {
                val markdown = "![${action.name.ifBlank { "表情" }}](${action.url})"
                _state.update { it.copy(composeInput = appendDraftBlock(it.composeInput, markdown)) }
            }
            is BreezemoonAction.OpenUserProfile -> openUserProfile(action.username)
            BreezemoonAction.DismissUserProfile -> clearUserProfile()
            BreezemoonAction.RetryUserProfile -> _state.value.profileUsername?.let(::openUserProfile)
            BreezemoonAction.ConsumeScrollToBottom -> _state.update { it.copy(shouldScrollToBottom = false) }
            is BreezemoonAction.SetError -> _state.update { it.copy(error = action.message) }
        }
    }

    private fun load(page: Int, append: Boolean) {
        val current = _state.value
        if (current.isLoading) return
        _state.update { it.copy(isLoading = true, error = if (append) it.error else null) }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.getBreezemoons(apiKey, page, 20) }
            }.onSuccess { result ->
                val normalized = result.asReversed()
                _state.update {
                    it.copy(
                        items = if (append) (normalized + it.items).distinctBy { item -> item.id } else normalized,
                        nextPage = page + 1,
                        hasMore = result.isNotEmpty(),
                        isLoading = false,
                    )
                }
            }.onFailure { throwable ->
                _state.update { it.copy(isLoading = false, error = throwable.message ?: "加载清风明月失败") }
            }
        }
    }

    private fun pickAttachment(uri: android.net.Uri, isVideo: Boolean) {
        val current = _state.value
        val uploadId = uploadQueue.enqueue(apiKey, uri, isVideo)
        val (nextAttachments, nextInput, _) = current.attachments.append(
            text = current.composeInput,
            cursor = current.composeInput.length,
            uploadId = uploadId,
            previewUri = uri,
            isVideo = isVideo,
        )
        _state.update {
            it.copy(composeInput = nextInput, attachments = nextAttachments, error = null)
        }
    }

    private fun removeAttachment(index: Int) {
        val current = _state.value
        val removed = current.attachments.items.firstOrNull { it.index == index } ?: return
        val (nextAttachments, nextInput) = current.attachments.removeAt(current.composeInput, index)
        uploadQueue.remove(removed.uploadId)
        _state.update { it.copy(composeInput = nextInput, attachments = nextAttachments) }
    }

    private fun retryAttachment(index: Int) {
        val target = _state.value.attachments.items.firstOrNull { it.index == index } ?: return
        uploadQueue.retry(apiKey, target.uploadId)
    }

    private fun observeAttachmentUploads() {
        scope.launch {
            uploadQueue.uploads.collect { uploads ->
                val current = _state.value.attachments
                if (current.isEmpty) return@collect
                val synced = current.syncFrom(uploads)
                if (synced == current) return@collect
                _state.update { it.copy(attachments = synced) }
            }
        }
    }

    private fun publish() {
        val snapshot = _state.value
        val raw = snapshot.composeInput
        val attachments = snapshot.attachments.syncReferences(raw)
        if (snapshot.isSending) return
        if (raw.isBlank() && !attachments.hasSendableAttachment) return
        _state.update { it.copy(isSending = true, error = null) }
        scope.launch {
            uploadQueue.awaitSettled(attachments.pendingUploadIds)
            val settled = attachments.syncFrom(uploadQueue.uploads.value)
            val text = settled.resolve(raw).trim()
            if (text.isBlank()) {
                val reason = settled.items.firstOrNull { it.error != null }?.error
                    ?: "没有可发布的内容"
                _state.update { it.copy(attachments = settled, isSending = false, error = reason) }
                return@launch
            }
            val failed = settled.items.filter {
                it.referenced && it.status == AttachmentUpload.Status.Failed
            }
            if (failed.isNotEmpty()) {
                emit(
                    BreezemoonEffect.ShowError(
                        "图" + failed.joinToString("、") { it.index.toString() } + " 上传失败，已跳过",
                    ),
                )
            }
            runCatching {
                withContext(Dispatchers.IO) { api.sendBreezemoon(apiKey, text) }
            }.onSuccess {
                uploadQueue.remove(settled.allUploadIds)
                _state.update {
                    it.copy(
                        composeInput = "",
                        attachments = ComposerAttachments(),
                        isSending = false,
                        shouldScrollToBottom = true,
                    )
                }
                emit(BreezemoonEffect.ShowMessage("发布成功"))
                load(page = 1, append = false)
            }.onFailure { throwable ->
                _state.update {
                    it.copy(
                        attachments = settled,
                        isSending = false,
                        error = throwable.message ?: "发布清风明月失败",
                    )
                }
            }
        }
    }

    private fun toggleEmoji() {
        val opening = !_state.value.emojiPanelOpen
        _state.update { it.copy(emojiPanelOpen = opening, attachmentPanelOpen = false) }
        if (opening && _state.value.emojiGroups.isEmpty()) {
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
                        isLoadingEmojiPack = firstGroupId.isBlank(),
                    )
                }
                if (firstGroupId.isNotBlank()) loadEmojiGroup(firstGroupId)
            }.onFailure { throwable ->
                _state.update {
                    it.copy(isLoadingEmojiPack = false, emojiPackError = throwable.message ?: "加载表情包分组失败")
                }
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
                _state.update {
                    if (it.selectedEmojiGroupId == groupId) {
                        it.copy(emojiItems = items, isLoadingEmojiPack = false)
                    } else {
                        it
                    }
                }
            }.onFailure { throwable ->
                _state.update {
                    if (it.selectedEmojiGroupId == groupId) {
                        it.copy(isLoadingEmojiPack = false, emojiPackError = throwable.message ?: "加载表情包失败")
                    } else {
                        it
                    }
                }
            }
        }
    }

    private fun openUserProfile(username: String) {
        val target = username.trim()
        val current = _state.value
        if (target.isBlank() || (current.profileUsername == target && current.isLoadingProfile)) return
        _state.update {
            it.copy(
                profileUsername = target,
                profileUser = null,
                profileMedals = emptyList(),
                profileError = null,
                isLoadingProfile = true,
            )
        }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    coroutineScope {
                        val userDeferred = async { api.getUserProfile(apiKey, target) }
                        val medalsDeferred = async { api.getUserMedals(apiKey, target) }
                        userDeferred.await() to medalsDeferred.await()
                    }
                }
            }.onSuccess { (user, medals) ->
                _state.update {
                    if (it.profileUsername == target) {
                        it.copy(profileUser = user, profileMedals = medals, isLoadingProfile = false)
                    } else {
                        it
                    }
                }
            }.onFailure { throwable ->
                _state.update {
                    if (it.profileUsername == target) {
                        it.copy(profileError = throwable.message ?: "加载用户资料失败", isLoadingProfile = false)
                    } else {
                        it
                    }
                }
            }
        }
    }

    private fun clearUserProfile() {
        _state.update {
            it.copy(
                profileUsername = null,
                profileUser = null,
                profileMedals = emptyList(),
                profileError = null,
                isLoadingProfile = false,
            )
        }
    }

    private fun emit(effect: BreezemoonEffect) {
        scope.launch { effects.send(effect) }
    }

    fun close() {
        scope.cancel()
    }
}
