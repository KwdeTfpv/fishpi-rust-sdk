package dev.fishpi.mobile.feature.profile

import dev.fishpi.mobile.FishPiThemeOption
import dev.fishpi.mobile.data.ChatFilterConfig
import dev.fishpi.mobile.data.SavedAccount

internal sealed interface ProfileAction {
    data class Initialize(
        val targetUsername: String?,
        val savedAccounts: List<SavedAccount>,
        val chatFilters: ChatFilterConfig,
        val themeOptions: List<FishPiThemeOption>,
        val themeKey: String,
        val chatWallpaperUri: String,
        val noticeUnread: Long,
        val closeOnBack: Boolean,
    ) : ProfileAction

    data object Refresh : ProfileAction
    data object OpenSettings : ProfileAction
    data object DismissSettings : ProfileAction
    data object OpenFilterSettings : ProfileAction
    data object DismissFilterSettings : ProfileAction
    data object OpenThemeEditor : ProfileAction
    data object DismissThemeEditor : ProfileAction
    data object OpenAbout : ProfileAction
    data object DismissAbout : ProfileAction
    data object OpenContent : ProfileAction
    data object DismissContent : ProfileAction
    data object OpenTransfer : ProfileAction
    data object DismissTransfer : ProfileAction
    data class SelectContentTab(val value: String) : ProfileAction
    data object LoadMoreArticles : ProfileAction
    data object LoadMoreBreezemoons : ProfileAction
    data object ToggleFollow : ProfileAction
    data class Transfer(val amount: Int, val memo: String) : ProfileAction
    data class OpenArticle(val articleId: String) : ProfileAction
    data class OpenPrivateChat(val username: String) : ProfileAction
    data object OpenNotice : ProfileAction
    data object CheckUpdate : ProfileAction
    data object Logout : ProfileAction
    data class SwitchAccount(val account: SavedAccount) : ProfileAction
    data object AddAccount : ProfileAction
    data class SaveChatFilters(val config: ChatFilterConfig) : ProfileAction
    data class ChangeTheme(val key: String) : ProfileAction
    data class ImportThemePackage(val uri: String) : ProfileAction
    data class SaveEditedTheme(val raw: String) : ProfileAction
    data class DeleteCustomTheme(val key: String) : ProfileAction
    data class ChangeChatWallpaper(val uri: String) : ProfileAction
    data object CloseProfile : ProfileAction
}
