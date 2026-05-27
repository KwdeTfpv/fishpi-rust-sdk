package dev.fishpi.mobile.core.navigation

sealed interface AppNavigationAction {
    data object Back : AppNavigationAction
    data object Dismiss : AppNavigationAction
    data class OpenArticle(val articleId: String) : AppNavigationAction
    data class OpenUserProfile(val username: String) : AppNavigationAction
    data class OpenPrivateChat(val username: String) : AppNavigationAction
    data class OpenExternalUrl(val url: String) : AppNavigationAction
}
