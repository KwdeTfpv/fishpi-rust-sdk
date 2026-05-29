package dev.fishpi.mobile.feature.profile

import dev.fishpi.mobile.data.ChatFilterConfig
import dev.fishpi.mobile.data.SavedAccount

internal sealed interface ProfileEffect {
    data class ShowMessage(val message: String) : ProfileEffect
    data class ShowError(val message: String) : ProfileEffect
    data class OpenArticle(val articleId: String) : ProfileEffect
    data class OpenPrivateChat(val username: String) : ProfileEffect
    data object OpenNotice : ProfileEffect
    data object CheckUpdate : ProfileEffect
    data object Logout : ProfileEffect
    data class SwitchAccount(val account: SavedAccount) : ProfileEffect
    data object AddAccount : ProfileEffect
    data class SaveChatFilters(val config: ChatFilterConfig) : ProfileEffect
    data class ChangeTheme(val key: String) : ProfileEffect
    data class ImportThemePackage(val uri: String) : ProfileEffect
    data class SaveEditedTheme(val raw: String) : ProfileEffect
    data class DeleteCustomTheme(val key: String) : ProfileEffect
    data class ChangeChatWallpaper(val uri: String) : ProfileEffect
    data object CloseProfile : ProfileEffect
}
