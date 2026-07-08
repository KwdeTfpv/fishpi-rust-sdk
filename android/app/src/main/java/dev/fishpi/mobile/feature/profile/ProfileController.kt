package dev.fishpi.mobile.feature.profile

import android.content.Context
import dev.fishpi.mobile.data.ChatFilterConfig
import dev.fishpi.mobile.data.FishPiApiClient
import dev.fishpi.mobile.data.FishPiWebLoginClient
import dev.fishpi.mobile.data.FishPiUser
import dev.fishpi.mobile.data.SavedAccount
import dev.fishpi.mobile.data.SessionStore
import dev.fishpi.mobile.data.UNKNOWN_LIVENESS
import dev.fishpi.mobile.data.UserActivityView
import dev.fishpi.mobile.data.parseWebLoginTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

internal class ProfileController(
    context: Context,
    private val apiKey: String,
    private val sessionUser: FishPiUser,
    initialChatFilters: ChatFilterConfig,
    private val api: FishPiApiClient = FishPiApiClient.shared,
    private val webLoginClient: FishPiWebLoginClient = FishPiWebLoginClient(),
) {
    private val store = SessionStore(context.applicationContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(
        ProfileState.initial(
            currentApiKey = apiKey,
            currentUsername = sessionUser.userName,
            user = sessionUser,
            chatFilters = initialChatFilters,
        ),
    )
    val state: StateFlow<ProfileState> = _state

    private val effects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effect = effects.receiveAsFlow()

    fun dispatch(action: ProfileAction) {
        when (action) {
            is ProfileAction.Initialize -> initialize(action)
            ProfileAction.Refresh -> refresh()
            ProfileAction.OpenSettings -> _state.update { it.copy(settingsOpen = true) }
            ProfileAction.DismissSettings -> _state.update { it.copy(settingsOpen = false) }
            ProfileAction.OpenFilterSettings -> _state.update { it.copy(filterSettingsOpen = true) }
            ProfileAction.DismissFilterSettings -> _state.update { it.copy(filterSettingsOpen = false) }
            ProfileAction.OpenThemeEditor -> _state.update { it.copy(themeEditorOpen = true) }
            ProfileAction.DismissThemeEditor -> _state.update { it.copy(themeEditorOpen = false) }
            ProfileAction.OpenAbout -> _state.update { it.copy(aboutOpen = true) }
            ProfileAction.DismissAbout -> _state.update { it.copy(aboutOpen = false) }
            ProfileAction.OpenContent -> _state.update { it.copy(contentOpen = true) }
            ProfileAction.DismissContent -> _state.update { it.copy(contentOpen = false) }
            ProfileAction.OpenTransfer -> _state.update { it.copy(transferOpen = true) }
            ProfileAction.DismissTransfer -> _state.update { it.copy(transferOpen = false) }
            is ProfileAction.SelectContentTab -> _state.update { it.copy(selectedContentTab = action.value) }
            ProfileAction.LoadMoreArticles -> {
                val page = _state.value.articles.nextPage
                loadUserArticles(page = page, append = true)
            }
            ProfileAction.LoadMoreBreezemoons -> {
                val page = _state.value.breezemoons.nextPage
                loadUserBreezemoons(page = page, append = true)
            }
            ProfileAction.ToggleFollow -> toggleUserFollow()
            is ProfileAction.Transfer -> transfer(action.amount, action.memo, action.username)
            is ProfileAction.OpenArticle -> emit(ProfileEffect.OpenArticle(action.articleId))
            is ProfileAction.OpenPrivateChat -> emit(ProfileEffect.OpenPrivateChat(action.username))
            ProfileAction.OpenNotice -> emit(ProfileEffect.OpenNotice)
            ProfileAction.CheckUpdate -> emit(ProfileEffect.CheckUpdate)
            ProfileAction.Logout -> emit(ProfileEffect.Logout)
            is ProfileAction.SwitchAccount -> emit(ProfileEffect.SwitchAccount(action.account))
            ProfileAction.AddAccount -> emit(ProfileEffect.AddAccount)
            is ProfileAction.SaveChatFilters -> {
                _state.update { it.copy(chatFilters = action.config) }
                emit(ProfileEffect.SaveChatFilters(action.config))
            }
            is ProfileAction.ChangeTheme -> {
                _state.update { it.copy(themeKey = action.key) }
                emit(ProfileEffect.ChangeTheme(action.key))
            }
            is ProfileAction.ImportThemePackage -> emit(ProfileEffect.ImportThemePackage(action.uri))
            is ProfileAction.SaveEditedTheme -> emit(ProfileEffect.SaveEditedTheme(action.raw))
            is ProfileAction.DeleteCustomTheme -> emit(ProfileEffect.DeleteCustomTheme(action.key))
            is ProfileAction.ChangeChatWallpaper -> {
                _state.update { it.copy(chatWallpaperUri = action.uri) }
                emit(ProfileEffect.ChangeChatWallpaper(action.uri))
            }
            is ProfileAction.WebLoginQrScanned -> handleWebLoginQr(action.raw)
            ProfileAction.ConfirmWebLogin -> confirmWebLogin()
            ProfileAction.DismissWebLoginConfirm -> _state.update {
                it.copy(webLoginTargetId = null, isWebLoginAuthorizing = false)
            }
            ProfileAction.CloseProfile -> emit(ProfileEffect.CloseProfile)
        }
    }

    private fun handleWebLoginQr(raw: String) {
        val targetId = parseWebLoginTarget(raw)
        if (targetId.isNullOrBlank()) {
            emit(ProfileEffect.ShowError("请扫描网页版登录二维码"))
            return
        }
        _state.update { it.copy(webLoginTargetId = targetId, isWebLoginAuthorizing = false) }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    withTimeout(8_000L) { webLoginClient.notifyScanned(targetId) }
                }
            }.onFailure { throwable ->
                emit(ProfileEffect.ShowError(throwable.message ?: "网页登录二维码已失效"))
                _state.update {
                    if (it.webLoginTargetId == targetId) {
                        it.copy(webLoginTargetId = null, isWebLoginAuthorizing = false)
                    } else {
                        it
                    }
                }
            }
        }
    }

    private fun confirmWebLogin() {
        val targetId = _state.value.webLoginTargetId ?: return
        if (_state.value.isWebLoginAuthorizing) return
        _state.update { it.copy(isWebLoginAuthorizing = true) }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    withTimeout(10_000L) { webLoginClient.authorize(targetId, apiKey) }
                }
            }.onSuccess {
                _state.update { it.copy(webLoginTargetId = null, isWebLoginAuthorizing = false) }
                emit(ProfileEffect.ShowMessage("已授权网页版登录"))
            }.onFailure { throwable ->
                _state.update { it.copy(isWebLoginAuthorizing = false) }
                emit(ProfileEffect.ShowError(throwable.message ?: "网页登录授权失败"))
            }
        }
    }

    private fun initialize(action: ProfileAction.Initialize) {
        val targetUsername = action.targetUsername?.trim().orEmpty()
        val isSelfProfile = targetUsername.isBlank() || targetUsername.equals(sessionUser.userName, ignoreCase = true)
        val current = _state.value
        val targetChanged = current.targetUsername != targetUsername
        _state.update {
            it.copy(
                targetUsername = targetUsername,
                isSelfProfile = isSelfProfile,
                user = if (targetChanged) {
                    if (isSelfProfile) sessionUser else placeholderProfileUser(targetUsername)
                } else {
                    it.user
                },
                activity = if (isSelfProfile) store.getHomeActivity(apiKey)?.takeIf { cached -> cached.isToday() }?.activity else null,
                savedAccounts = action.savedAccounts,
                chatFilters = action.chatFilters,
                themeOptions = action.themeOptions,
                themeKey = action.themeKey,
                chatWallpaperUri = action.chatWallpaperUri,
                noticeUnread = action.noticeUnread,
                closeOnBack = action.closeOnBack,
                contentOpen = if (targetChanged) false else it.contentOpen,
                articles = if (targetChanged) ProfilePagedState(hasMore = false) else it.articles,
                breezemoons = if (targetChanged) ProfilePagedState(hasMore = false) else it.breezemoons,
            )
        }
        if (
            targetChanged ||
            current.user.userName.isBlank() ||
            (isSelfProfile && current.medals.isEmpty() && !current.isLoadingMedals)
        ) {
            refresh()
        }
    }

    private fun refresh() {
        val current = _state.value
        if (current.isLoading) return
        _state.update { it.copy(isLoading = true, isLoadingMedals = true, error = null) }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    coroutineScope {
                        val latest = _state.value
                        val userDeferred = async {
                            if (latest.isSelfProfile) api.getUser(apiKey) else api.getUserProfile(apiKey, latest.targetUsername)
                        }
                        val activityDeferred = async { if (latest.isSelfProfile) loadSelfActivity() else null }
                        val medalsDeferred = async {
                            api.getUserMedals(apiKey, if (latest.isSelfProfile) sessionUser.userName else latest.targetUsername)
                        }
                        Triple(userDeferred.await(), activityDeferred.await(), medalsDeferred.await())
                    }
                }
            }.onSuccess { (freshUser, freshActivity, freshMedals) ->
                _state.update {
                    it.copy(
                        user = freshUser,
                        activity = freshActivity,
                        medals = freshMedals,
                        isLoading = false,
                        isLoadingMedals = false,
                        isFollowingUser = if (!it.isSelfProfile) freshUser.canFollow.equals("no", ignoreCase = true) else it.isFollowingUser,
                    )
                }
                loadUserArticles()
                loadUserBreezemoons()
            }.onFailure { throwable ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMedals = false,
                        error = throwable.message ?: "刷新用户信息失败",
                    )
                }
            }
        }
    }

    private suspend fun loadSelfActivity(): UserActivityView? {
        val cached = store.getHomeActivity(apiKey)?.takeIf { it.isToday() }
        if (cached?.isFresh() == true || cached?.isAttemptFresh() == true) return cached.activity
        val fresh = runCatching { api.getUserActivity(apiKey) }.getOrNull()
        if (fresh != null) {
            val merged = if (cached?.activity?.livenessRewarded == true && !fresh.livenessRewarded) {
                fresh.copy(livenessRewarded = true)
            } else {
                fresh
            }
            store.saveHomeActivity(apiKey, merged)
            return merged
        }
        val fallback = cached?.activity ?: UserActivityView(
            liveness = UNKNOWN_LIVENESS,
            checkedIn = false,
            livenessRewarded = false,
        )
        store.saveHomeActivityAttempt(apiKey, fallback)
        return fallback
    }

    private fun loadUserArticles(page: Int = 1, append: Boolean = false) {
        val current = _state.value
        if (append && (current.articles.isLoading || current.articles.isLoadingMore || !current.articles.hasMore)) return
        if (!append && current.articles.isLoading) return
        _state.update {
            it.copy(
                articles = it.articles.copy(isLoading = !append, isLoadingMore = append),
                error = null,
            )
        }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.getUserArticles(apiKey, _state.value.user.userName, page) }
            }.onSuccess { result ->
                _state.update {
                    val nextItems = if (append) {
                        (it.articles.items + result.items).distinctBy { article -> article.id }
                    } else {
                        result.items
                    }
                    it.copy(
                        articles = it.articles.copy(
                            items = nextItems,
                            nextPage = result.nextPage,
                            hasMore = result.hasMore,
                            isLoading = false,
                            isLoadingMore = false,
                        ),
                    )
                }
            }.onFailure { throwable ->
                _state.update {
                    it.copy(
                        articles = it.articles.copy(isLoading = false, isLoadingMore = false),
                        error = throwable.message ?: "加载个人帖子失败",
                    )
                }
            }
        }
    }

    private fun loadUserBreezemoons(page: Int = 1, append: Boolean = false) {
        val current = _state.value
        if (append && (current.breezemoons.isLoading || current.breezemoons.isLoadingMore || !current.breezemoons.hasMore)) return
        if (!append && current.breezemoons.isLoading) return
        _state.update {
            it.copy(
                breezemoons = it.breezemoons.copy(isLoading = !append, isLoadingMore = append),
                error = null,
            )
        }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.getUserBreezemoons(apiKey, _state.value.user.userName, page, 20) }
            }.onSuccess { result ->
                _state.update {
                    val next = page + 1
                    val more = result.isNotEmpty()
                    val nextItems = if (append) {
                        (it.breezemoons.items + result).distinctBy { item -> item.id.ifBlank { item.createTime + item.content } }
                    } else {
                        result
                    }
                    it.copy(
                        breezemoons = it.breezemoons.copy(
                            items = nextItems,
                            nextPage = next,
                            hasMore = more,
                            isLoading = false,
                            isLoadingMore = false,
                        ),
                    )
                }
            }.onFailure { throwable ->
                _state.update {
                    it.copy(
                        breezemoons = it.breezemoons.copy(isLoading = false, isLoadingMore = false),
                        error = throwable.message ?: "加载个人清风明月失败",
                    )
                }
            }
        }
    }

    private fun toggleUserFollow() {
        val current = _state.value
        if (current.isSelfProfile || current.user.userId.isBlank() || current.isFollowRunning) return
        _state.update { it.copy(isFollowRunning = true) }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    if (current.isFollowingUser) api.unfollowUser(apiKey, current.user.userId) else api.followUser(apiKey, current.user.userId)
                }
            }.onSuccess {
                _state.update { it.copy(isFollowingUser = !it.isFollowingUser, isFollowRunning = false) }
                emit(ProfileEffect.ShowMessage(if (_state.value.isFollowingUser) "已关注" else "已取消关注"))
            }.onFailure { throwable ->
                _state.update { it.copy(isFollowRunning = false) }
                emit(ProfileEffect.ShowError(throwable.message ?: "操作失败"))
            }
        }
    }

    private fun transfer(amount: Int, memo: String, username: String?) {
        val target = username?.trim().orEmpty().ifBlank { _state.value.user.userName }
        if (target.isBlank()) {
            emit(ProfileEffect.ShowError("请输入转账用户名"))
            return
        }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.transferPoint(apiKey, target, amount, memo) }
            }.onSuccess {
                _state.update { it.copy(transferOpen = false) }
                emit(ProfileEffect.ShowMessage("转账成功"))
            }.onFailure { throwable ->
                emit(ProfileEffect.ShowError(throwable.message ?: "转账失败"))
            }
        }
    }

    private fun emit(effect: ProfileEffect) {
        scope.launch { effects.send(effect) }
    }

    fun close() {
        scope.cancel()
    }
}

private fun placeholderProfileUser(username: String): FishPiUser {
    val name = username.trim()
    return FishPiUser(
        userName = name,
        userNickname = name,
        userAvatarUrl = "",
        role = "",
    )
}
