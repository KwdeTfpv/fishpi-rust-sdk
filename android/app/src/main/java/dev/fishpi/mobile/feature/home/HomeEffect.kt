package dev.fishpi.mobile.feature.home

import dev.fishpi.mobile.core.ui.UiEffect

internal sealed interface HomeEffect : UiEffect {
    data object NavigateToChat : HomeEffect
    data object NavigateToArticle : HomeEffect
    data class NavigateToArticleDetail(val articleId: String, val returnToHome: Boolean = true) : HomeEffect
    data object NavigateToBreezemoon : HomeEffect
    data object NavigateToStore : HomeEffect
    data object NavigateToProfile : HomeEffect
    data class ShowMessage(val message: String) : HomeEffect
    data class ShowError(val message: String) : HomeEffect
}
