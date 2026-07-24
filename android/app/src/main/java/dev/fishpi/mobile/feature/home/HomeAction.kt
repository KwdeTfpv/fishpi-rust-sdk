package dev.fishpi.mobile.feature.home

internal sealed interface HomeAction {
    data object Initialize : HomeAction
    data object RefreshActivity : HomeAction
    data object ClaimYesterdayLivenessReward : HomeAction
    data class LoadRecommended(val refresh: Boolean) : HomeAction
    data object LoadMoreRecommended : HomeAction
    data object LoadQuote : HomeAction
    data class UpdateNoticeUnread(val value: Long) : HomeAction
    data object OpenWorkSettings : HomeAction
    data object DismissWorkSettings : HomeAction
    data class ChangeWorkStartTime(val value: String) : HomeAction
    data class ChangeWorkEndTime(val value: String) : HomeAction
    data class ChangeWeekendMode(val value: String) : HomeAction
    data class ToggleCustomRestDay(val day: Int) : HomeAction
    data object SaveWorkSettings : HomeAction
    data object OpenChat : HomeAction
    data object OpenArticle : HomeAction
    data class OpenArticleDetail(val articleId: String) : HomeAction
    data object OpenBreezemoon : HomeAction
    data object OpenStore : HomeAction
    data object OpenProfile : HomeAction
    data object OpenLivenessHelp : HomeAction
    data object ClearError : HomeAction
}
