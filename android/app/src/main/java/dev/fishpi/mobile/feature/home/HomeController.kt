package dev.fishpi.mobile.feature.home

import android.content.Context
import dev.fishpi.mobile.core.ui.UiController
import dev.fishpi.mobile.data.FishPiApiClient
import dev.fishpi.mobile.data.HomeWorkSettings
import dev.fishpi.mobile.data.SessionStore
import dev.fishpi.mobile.data.UNKNOWN_LIVENESS
import dev.fishpi.mobile.data.UserActivityView
import dev.fishpi.mobile.feature.home.mapper.toHomeArticleUiModel
import dev.fishpi.mobile.feature.home.model.toDraft
import java.net.HttpURLConnection
import java.net.URL
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
import org.json.JSONObject

internal class HomeController(
    context: Context,
    private val apiKey: String,
    displayName: String,
    noticeUnread: Long,
    private val api: FishPiApiClient = FishPiApiClient.shared,
    private val store: SessionStore = SessionStore(context.applicationContext),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : UiController<HomeState, HomeAction> {
    private val _state = MutableStateFlow(
        HomeState(
            displayName = displayName,
            noticeUnread = noticeUnread,
            activity = store.getHomeActivity(apiKey)?.takeIf { it.isToday() }?.activity,
            livenessRewarded = store.getHomeActivity(apiKey)?.takeIf { it.isToday() }?.activity?.livenessRewarded,
            workSettings = store.getHomeWorkSettings(),
            workSettingsDraft = store.getHomeWorkSettings().toDraft(),
        ),
    )
    override val state: StateFlow<HomeState> = _state

    private val _effects = MutableSharedFlow<HomeEffect>(extraBufferCapacity = 16)
    val effects: SharedFlow<HomeEffect> = _effects.asSharedFlow()

    private var initialized = false
    private var activitySyncJob: Job? = null

    override fun dispatch(action: HomeAction) {
        when (action) {
            HomeAction.Initialize -> initialize()
            HomeAction.RefreshActivity -> syncHomeActivity()
            HomeAction.ClaimYesterdayLivenessReward -> claimLivenessReward()
            is HomeAction.LoadRecommended -> loadRecommendedArticles(page = 1, append = false)
            HomeAction.LoadMoreRecommended -> loadRecommendedArticles(_state.value.recommendedNextPage, append = true)
            HomeAction.LoadQuote -> loadQuote()
            is HomeAction.UpdateNoticeUnread -> _state.update { it.copy(noticeUnread = action.value) }
            HomeAction.OpenWorkSettings -> _state.update { it.copy(showWorkSettingsDialog = true, workSettingsDraft = it.workSettings.toDraft()) }
            HomeAction.DismissWorkSettings -> _state.update { it.copy(showWorkSettingsDialog = false, workSettingsDraft = it.workSettings.toDraft()) }
            is HomeAction.ChangeWorkStartTime -> updateDraft { it.copy(startTime = action.value) }
            is HomeAction.ChangeWorkEndTime -> updateDraft { it.copy(endTime = action.value) }
            is HomeAction.ChangeWeekendMode -> updateDraft {
                val mode = action.value.takeIf {
                    it in setOf(
                        HomeWorkSettings.WEEKEND_DOUBLE,
                        HomeWorkSettings.WEEKEND_SINGLE,
                        HomeWorkSettings.WEEKEND_CUSTOM,
                    )
                } ?: HomeWorkSettings.WEEKEND_DOUBLE
                it.copy(weekendMode = mode)
            }
            is HomeAction.ToggleCustomRestDay -> updateDraft {
                val day = action.day.coerceIn(1, 7)
                val nextDays = if (day in it.customRestDays) {
                    (it.customRestDays - day).ifEmpty { setOf(7) }
                } else {
                    it.customRestDays + day
                }
                it.copy(customRestDays = nextDays)
            }
            HomeAction.SaveWorkSettings -> saveWorkSettings()
            HomeAction.OpenChat -> emitEffect(HomeEffect.NavigateToChat)
            HomeAction.OpenArticle -> emitEffect(HomeEffect.NavigateToArticle)
            is HomeAction.OpenArticleDetail -> emitEffect(HomeEffect.NavigateToArticleDetail(action.articleId))
            HomeAction.OpenBreezemoon -> emitEffect(HomeEffect.NavigateToBreezemoon)
            HomeAction.OpenStore -> emitEffect(HomeEffect.NavigateToStore)
            HomeAction.OpenProfile -> emitEffect(HomeEffect.NavigateToProfile)
            HomeAction.OpenLivenessHelp -> emitEffect(HomeEffect.NavigateToArticleDetail(HomeLivenessHelpArticleId))
            HomeAction.ClearError -> _state.update { it.copy(homeError = null, recommendedError = null) }
        }
    }

    fun close() {
        activitySyncJob?.cancel()
    }

    private fun initialize() {
        if (initialized) return
        initialized = true
        loadQuote()
        loadRecommendedArticles(page = 1, append = false)
        syncHomeActivity()
        activitySyncJob = scope.launch {
            while (true) {
                delay(HomeActivitySyncIntervalMs)
                syncHomeActivity()
            }
        }
    }

    private fun syncHomeActivity() {
        scope.launch {
            val cachedActivity = store.getHomeActivity(apiKey)?.takeIf { it.isToday() }
            _state.update {
                it.copy(
                    activity = cachedActivity?.activity ?: it.activity,
                    livenessRewarded = cachedActivity?.activity?.livenessRewarded ?: it.livenessRewarded,
                )
            }

            val dailyState = runCatching {
                withContext(Dispatchers.IO) { api.getUserDailyState(apiKey) }
            }.getOrNull()

            if (dailyState != null) {
                val snapshot = _state.value
                val rewarded = dailyState.livenessRewarded ||
                    snapshot.activity?.livenessRewarded == true ||
                    cachedActivity?.activity?.livenessRewarded == true
                val correctedActivity = (snapshot.activity ?: UserActivityView(
                    UNKNOWN_LIVENESS,
                    checkedIn = false,
                    livenessRewarded = false,
                )).copy(
                    checkedIn = dailyState.checkedIn,
                    livenessRewarded = rewarded,
                )
                _state.update {
                    it.copy(
                        activity = correctedActivity,
                        livenessRewarded = rewarded,
                    )
                }
                if (cachedActivity != null) {
                    store.saveHomeActivity(
                        apiKey = apiKey,
                        activity = correctedActivity,
                        fetchedAt = cachedActivity.fetchedAt,
                        attemptedAt = cachedActivity.attemptedAt,
                    )
                }
            }

            if (cachedActivity?.isFresh() == true || cachedActivity?.isAttemptFresh() == true) {
                return@launch
            }

            val nextActivityResult = runCatching {
                withContext(Dispatchers.IO) { api.getUserActivity(apiKey) }
            }
            val nextActivity = nextActivityResult.getOrNull()
            if (nextActivity != null) {
                val snapshot = _state.value
                val mergedActivity = dailyState?.let {
                    val rewarded = it.livenessRewarded ||
                        snapshot.activity?.livenessRewarded == true ||
                        cachedActivity?.activity?.livenessRewarded == true
                    nextActivity.copy(
                        checkedIn = it.checkedIn,
                        livenessRewarded = rewarded,
                    )
                } ?: if (snapshot.activity?.livenessRewarded == true || cachedActivity?.activity?.livenessRewarded == true) {
                    nextActivity.copy(livenessRewarded = true)
                } else {
                    nextActivity
                }
                _state.update {
                    it.copy(
                        activity = mergedActivity,
                        livenessRewarded = mergedActivity.livenessRewarded,
                    )
                }
                store.saveHomeActivity(apiKey, mergedActivity)
            } else {
                store.saveHomeActivityAttempt(apiKey, _state.value.activity)
            }
        }
    }

    private fun claimLivenessReward() {
        val snapshot = _state.value
        if (snapshot.isRewarding || snapshot.activity?.livenessRewarded == true || snapshot.livenessRewarded == true) return
        _state.update { it.copy(isRewarding = true) }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.rewardLiveness(apiKey) }
            }.onSuccess { reward ->
                val nextActivity = _state.value.activity?.copy(livenessRewarded = true)
                    ?: UserActivityView(UNKNOWN_LIVENESS, checkedIn = false, livenessRewarded = true)
                _state.update {
                    it.copy(
                        activity = nextActivity,
                        livenessRewarded = true,
                        isRewarding = false,
                    )
                }
                store.saveHomeActivity(apiKey, nextActivity)
                emitEffect(
                    HomeEffect.ShowMessage(
                        if (reward > 0) "已领取 $reward 积分" else "昨日活跃奖励已领取",
                    ),
                )
            }.onFailure {
                _state.update { state -> state.copy(isRewarding = false, homeError = it.message ?: "领取失败") }
                emitEffect(HomeEffect.ShowError(it.message ?: "领取失败"))
            }
        }
    }

    private fun loadRecommendedArticles(page: Int, append: Boolean) {
        val snapshot = _state.value
        if (snapshot.isLoadingRecommended || snapshot.isLoadingRecommendedMore) return
        if (append && !snapshot.recommendedHasMore) return
        _state.update {
            it.copy(
                isLoadingRecommended = !append,
                isLoadingRecommendedMore = append,
                recommendedError = null,
            )
        }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.getArticles(apiKey, "hot", "", page) }
            }.onSuccess { result ->
                val mapped = result.items.map { it.toHomeArticleUiModel() }
                _state.update {
                    it.copy(
                        recommendedArticles = if (append) {
                            (it.recommendedArticles + mapped).distinctBy { article -> article.id }
                        } else {
                            mapped
                        },
                        recommendedNextPage = result.nextPage,
                        recommendedHasMore = result.hasMore,
                        isLoadingRecommended = false,
                        isLoadingRecommendedMore = false,
                    )
                }
            }.onFailure {
                _state.update { state ->
                    state.copy(
                        recommendedError = it.message ?: "加载推荐帖子失败",
                        isLoadingRecommended = false,
                        isLoadingRecommendedMore = false,
                    )
                }
            }
        }
    }

    private fun loadQuote() {
        scope.launch {
            val quote = fetchHomeQuote().getOrDefault(HomeQuoteFallback)
            _state.update { it.copy(quoteText = quote) }
        }
    }

    private fun saveWorkSettings() {
        val draft = _state.value.workSettingsDraft
        if (!draft.isValid) return
        val settings = draft.toSettings()
        store.saveHomeWorkSettings(settings)
        _state.update {
            it.copy(
                workSettings = settings,
                workSettingsDraft = settings.toDraft(),
                showWorkSettingsDialog = false,
            )
        }
    }

    private fun updateDraft(transform: (dev.fishpi.mobile.feature.home.model.HomeWorkSettingsDraft) -> dev.fishpi.mobile.feature.home.model.HomeWorkSettingsDraft) {
        _state.update { it.copy(workSettingsDraft = transform(it.workSettingsDraft)) }
    }

    private fun emitEffect(effect: HomeEffect) {
        _effects.tryEmit(effect)
    }

    private suspend fun fetchHomeQuote(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL("https://api.mu-jie.cc/stray-birds/range?type=json").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
            }
            try {
                val text = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                JSONObject(text).optString("cn").trim().ifBlank { HomeQuoteFallback }
            } finally {
                connection.disconnect()
            }
        }
    }
}
