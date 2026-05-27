package dev.fishpi.mobile.feature.home

import dev.fishpi.mobile.data.HomeWorkSettings
import dev.fishpi.mobile.data.UserActivityView
import dev.fishpi.mobile.feature.home.model.HomeArticleUiModel
import dev.fishpi.mobile.feature.home.model.HomeWorkSettingsDraft
import dev.fishpi.mobile.feature.home.model.toDraft

internal data class HomeState(
    val displayName: String = "",
    val noticeUnread: Long = 0,
    val quoteText: String = HomeQuoteFallback,
    val activity: UserActivityView? = null,
    val livenessRewarded: Boolean? = null,
    val isRewarding: Boolean = false,
    val workSettings: HomeWorkSettings = HomeWorkSettings(),
    val workSettingsDraft: HomeWorkSettingsDraft = HomeWorkSettings().toDraft(),
    val showWorkSettingsDialog: Boolean = false,
    val recommendedArticles: List<HomeArticleUiModel> = emptyList(),
    val recommendedNextPage: Int = 1,
    val recommendedHasMore: Boolean = false,
    val isLoadingRecommended: Boolean = false,
    val isLoadingRecommendedMore: Boolean = false,
    val recommendedError: String? = null,
    val homeError: String? = null,
)

internal const val HomeLivenessHelpArticleId = "1683775497629"
internal const val HomeQuoteFallback = "风烟俱净，天山共色。从流飘荡，任意东西。"
internal const val HomeActivitySyncIntervalMs = 60 * 1000L
