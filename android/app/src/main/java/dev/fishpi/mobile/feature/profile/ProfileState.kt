package dev.fishpi.mobile.feature.profile

import dev.fishpi.mobile.FishPiThemeOption
import dev.fishpi.mobile.data.ArticleSummary
import dev.fishpi.mobile.data.BreezemoonView
import dev.fishpi.mobile.data.ChatFilterConfig
import dev.fishpi.mobile.data.FishPiUser
import dev.fishpi.mobile.data.MedalView
import dev.fishpi.mobile.data.SavedAccount
import dev.fishpi.mobile.data.UserActivityView

internal data class ProfilePagedState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val nextPage: Int = 1,
    val hasMore: Boolean = false,
)

internal data class ProfileState(
    val currentApiKey: String = "",
    val currentUsername: String = "",
    val targetUsername: String = "",
    val isSelfProfile: Boolean = true,
    val user: FishPiUser,
    val activity: UserActivityView? = null,
    val medals: List<MedalView> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMedals: Boolean = false,
    val error: String? = null,
    val articles: ProfilePagedState<ArticleSummary> = ProfilePagedState(hasMore = false),
    val breezemoons: ProfilePagedState<BreezemoonView> = ProfilePagedState(hasMore = false),
    val selectedContentTab: String = "article",
    val settingsOpen: Boolean = false,
    val filterSettingsOpen: Boolean = false,
    val themeEditorOpen: Boolean = false,
    val aboutOpen: Boolean = false,
    val contentOpen: Boolean = false,
    val transferOpen: Boolean = false,
    val isFollowingUser: Boolean = false,
    val isFollowRunning: Boolean = false,
    val savedAccounts: List<SavedAccount> = emptyList(),
    val chatFilters: ChatFilterConfig,
    val themeOptions: List<FishPiThemeOption> = emptyList(),
    val themeKey: String = "",
    val chatWallpaperUri: String = "",
    val noticeUnread: Long = 0L,
    val closeOnBack: Boolean = false,
    val webLoginTargetId: String? = null,
    val isWebLoginAuthorizing: Boolean = false,
) {
    companion object {
        fun initial(
            currentApiKey: String,
            currentUsername: String,
            user: FishPiUser,
            chatFilters: ChatFilterConfig,
        ): ProfileState = ProfileState(
            currentApiKey = currentApiKey,
            currentUsername = currentUsername,
            user = user,
            chatFilters = chatFilters,
        )
    }
}
