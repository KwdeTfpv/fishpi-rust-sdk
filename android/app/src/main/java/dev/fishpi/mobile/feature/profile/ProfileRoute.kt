package dev.fishpi.mobile.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.fishpi.mobile.AppSession
import dev.fishpi.mobile.FishPiThemeOption
import dev.fishpi.mobile.FishPiNotifier
import dev.fishpi.mobile.data.ChatFilterConfig
import dev.fishpi.mobile.data.SavedAccount

@Composable
internal fun ProfileRoute(
    session: AppSession,
    active: Boolean,
    profileUsername: String? = null,
    savedAccounts: List<SavedAccount>,
    chatFilters: ChatFilterConfig,
    themeOptions: List<FishPiThemeOption>,
    themeKey: String,
    onSaveChatFilters: (ChatFilterConfig) -> Unit,
    onThemeChange: (String) -> Unit,
    onImportTheme: (String) -> Result<String>,
    onSaveEditedTheme: (String) -> Result<String>,
    onDeleteCustomTheme: (String) -> Boolean,
    chatWallpaperUri: String,
    onChatWallpaperChange: (String) -> Unit,
    onOpenArticle: (String) -> Unit,
    onCloseProfile: () -> Unit = {},
    closeOnBack: Boolean = false,
    onOpenPrivateChat: (String) -> Unit = {},
    noticeUnread: Long,
    onOpenNotice: () -> Unit,
    onCheckUpdate: () -> Unit,
    onSwitchAccount: (SavedAccount) -> Unit,
    onAddAccount: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val controller = remember(session.apiKey, session.user.userName) {
        ProfileController(
            context = context.applicationContext,
            apiKey = session.apiKey,
            sessionUser = session.user,
            initialChatFilters = chatFilters,
        )
    }
    val state by controller.state.collectAsState()

    LaunchedEffect(
        profileUsername,
        savedAccounts,
        chatFilters,
        themeOptions,
        themeKey,
        chatWallpaperUri,
        noticeUnread,
        closeOnBack,
    ) {
        controller.dispatch(
            ProfileAction.Initialize(
                targetUsername = profileUsername,
                savedAccounts = savedAccounts,
                chatFilters = chatFilters,
                themeOptions = themeOptions,
                themeKey = themeKey,
                chatWallpaperUri = chatWallpaperUri,
                noticeUnread = noticeUnread,
                closeOnBack = closeOnBack,
            ),
        )
    }

    LaunchedEffect(controller) {
        controller.effect.collect { effect ->
            when (effect) {
                is ProfileEffect.ShowMessage -> FishPiNotifier.success(effect.message)
                is ProfileEffect.ShowError -> FishPiNotifier.error(effect.message)
                is ProfileEffect.OpenArticle -> onOpenArticle(effect.articleId)
                is ProfileEffect.OpenPrivateChat -> onOpenPrivateChat(effect.username)
                ProfileEffect.OpenNotice -> onOpenNotice()
                ProfileEffect.CheckUpdate -> onCheckUpdate()
                ProfileEffect.Logout -> onLogout()
                is ProfileEffect.SwitchAccount -> onSwitchAccount(effect.account)
                ProfileEffect.AddAccount -> onAddAccount()
                is ProfileEffect.SaveChatFilters -> onSaveChatFilters(effect.config)
                is ProfileEffect.ChangeTheme -> onThemeChange(effect.key)
                is ProfileEffect.ImportTheme -> onImportTheme(effect.raw).fold(
                    onSuccess = { FishPiNotifier.success("已导入主题：$it") },
                    onFailure = { FishPiNotifier.error("主题导入失败：${it.message ?: "格式不正确"}") },
                )
                is ProfileEffect.SaveEditedTheme -> onSaveEditedTheme(effect.raw).fold(
                    onSuccess = {
                        FishPiNotifier.success("已保存主题：$it")
                        controller.dispatch(ProfileAction.DismissThemeEditor)
                    },
                    onFailure = { FishPiNotifier.error("主题保存失败：${it.message ?: "格式不正确"}") },
                )
                is ProfileEffect.DeleteCustomTheme -> {
                    if (onDeleteCustomTheme(effect.key)) FishPiNotifier.success("已删除自定义主题") else FishPiNotifier.error("删除失败")
                }
                is ProfileEffect.ChangeChatWallpaper -> onChatWallpaperChange(effect.uri)
                ProfileEffect.CloseProfile -> onCloseProfile()
            }
        }
    }

    DisposableEffect(controller) {
        onDispose { controller.close() }
    }

    DefaultProfileUi(
        state = state,
        active = active,
        dispatch = controller::dispatch,
    )
}
