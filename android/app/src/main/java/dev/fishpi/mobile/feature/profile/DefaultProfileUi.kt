package dev.fishpi.mobile.feature.profile

import dev.fishpi.mobile.ui.components.*
import dev.fishpi.mobile.ui.animal.AnimalAppTile
import dev.fishpi.mobile.ui.animal.AnimalIconButton
import dev.fishpi.mobile.ui.animal.AnimalPanel
import dev.fishpi.mobile.ui.animal.AnimalStatusPill

import androidx.activity.compose.BackHandler
import dev.fishpi.mobile.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.text.Html
import android.text.method.LinkMovementMethod
import android.webkit.MimeTypeMap
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.SwitchAccount
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import dev.fishpi.mobile.feature.chat.ChatFilterSettingsOverlay
import dev.fishpi.mobile.ui.components.AppBottomSheet
import dev.fishpi.mobile.ui.components.AppSheetTitle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.TextView
import dev.fishpi.mobile.data.FishPiUser
import dev.fishpi.mobile.data.ChatFilterConfig
import dev.fishpi.mobile.data.ArticleSummary
import dev.fishpi.mobile.data.SavedAccount
import dev.fishpi.mobile.data.UserActivityView
import dev.fishpi.mobile.data.MedalView
import dev.fishpi.mobile.data.BreezemoonView
import dev.fishpi.mobile.utils.EditableThemePalette
import dev.fishpi.mobile.utils.ThemeColorSections
import dev.fishpi.mobile.utils.copyUriToSingleFile
import dev.fishpi.mobile.utils.isValidThemeHex
import dev.fishpi.mobile.utils.themeHexFromRgb
import dev.fishpi.mobile.utils.toThemeColor
import dev.fishpi.mobile.utils.toThemeRgb
import androidx.compose.material3.Text
import java.io.File
import java.net.URLEncoder

@Composable
private fun profileAccentColor(): Color = FishPiTheme.accent

@Composable
private fun profileAccentSoft(): Color = FishPiTheme.accent.copy(alpha = 0.10f)

@Composable
internal fun DefaultProfileUi(
    state: ProfileState,
    active: Boolean,
    dispatch: (ProfileAction) -> Unit,
) {
    val profileListState = rememberLazyListState()
    val user = state.user
    val articlePage = state.articles
    val breezePage = state.breezemoons
    val isOverlayOpen = state.settingsOpen || state.filterSettingsOpen || state.themeEditorOpen || state.aboutOpen

    if (!isOverlayOpen) {
        if (state.contentOpen) {
            LazyColumn(
                state = profileListState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(uiPageBrush()),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "articles_header", contentType = "articles_header") {
                    ProfileArticleSectionHeader(
                        articleCount = articlePage.items.size,
                        breezeCount = breezePage.items.size,
                        selectedTab = state.selectedContentTab,
                        isLoading = if (state.selectedContentTab == "article") articlePage.isLoading else breezePage.isLoading,
                        onSelectTab = { dispatch(ProfileAction.SelectContentTab(it)) },
                    )
                }
                if (state.selectedContentTab == "article") {
                    items(
                        items = articlePage.items,
                        key = { it.id },
                        contentType = { "article_row" },
                    ) { article ->
                        ProfileArticleRow(article = article, onClick = { dispatch(ProfileAction.OpenArticle(article.id)) })
                    }
                    if (articlePage.hasMore) {
                        item(key = "articles_more", contentType = "articles_more") {
                            ProfileArticleLoadMore(
                                isLoading = articlePage.isLoadingMore,
                                onLoadMore = { dispatch(ProfileAction.LoadMoreArticles) },
                            )
                        }
                    }
                } else {
                    items(
                        items = breezePage.items,
                        key = { it.id.ifBlank { it.createTime + it.content } },
                        contentType = { "breezemoon_row" },
                    ) { breeze ->
                        ProfileBreezemoonRow(item = breeze)
                    }
                    if (breezePage.hasMore) {
                        item(key = "breeze_more", contentType = "breeze_more") {
                            ProfileArticleLoadMore(
                                isLoading = breezePage.isLoadingMore,
                                onLoadMore = { dispatch(ProfileAction.LoadMoreBreezemoons) },
                            )
                        }
                    }
                }
                state.error?.let {
                    item(key = "error", contentType = "error") {
                        Text(text = it, color = FishPiErrorRed, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(uiPageBrush()),
                contentPadding = PaddingValues(bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "overview", contentType = "overview") {
                    ProfileOverviewPage(
                        user = user,
                        medals = state.medals,
                        isLoadingMedals = state.isLoadingMedals,
                        apiKey = state.currentApiKey,
                        noticeUnread = state.noticeUnread,
                        onOpenPosts = { dispatch(ProfileAction.OpenContent) },
                        onOpenNotice = { dispatch(ProfileAction.OpenNotice) },
                        onRefresh = { dispatch(ProfileAction.CheckUpdate) },
                        onOpenAbout = { dispatch(ProfileAction.OpenAbout) },
                        onOpenSettings = { dispatch(ProfileAction.OpenSettings) },
                        isSelfProfile = state.isSelfProfile,
                        isFollowingUser = state.isFollowingUser,
                        isFollowRunning = state.isFollowRunning,
                        onFollow = { dispatch(ProfileAction.ToggleFollow) },
                        onPrivateChat = { dispatch(ProfileAction.OpenPrivateChat(user.userName)) },
                        onTransfer = { dispatch(ProfileAction.OpenTransfer) },
                        onLogout = { dispatch(ProfileAction.Logout) },
                    )
                }
                if (!state.isSelfProfile) {
                    item(key = "other_articles_header", contentType = "articles_header") {
                        ProfileArticleSectionHeader(
                            articleCount = articlePage.items.size,
                            breezeCount = breezePage.items.size,
                            selectedTab = state.selectedContentTab,
                            isLoading = if (state.selectedContentTab == "article") articlePage.isLoading else breezePage.isLoading,
                            onSelectTab = { dispatch(ProfileAction.SelectContentTab(it)) },
                        )
                    }
                    if (state.selectedContentTab == "article") {
                        items(
                            items = articlePage.items,
                            key = { it.id },
                            contentType = { "article_row" },
                        ) { article ->
                            ProfileArticleRow(article = article, onClick = { dispatch(ProfileAction.OpenArticle(article.id)) })
                        }
                        if (articlePage.hasMore) {
                            item(key = "other_articles_more", contentType = "articles_more") {
                                ProfileArticleLoadMore(
                                    isLoading = articlePage.isLoadingMore,
                                    onLoadMore = { dispatch(ProfileAction.LoadMoreArticles) },
                                )
                            }
                        }
                    } else {
                        items(
                            items = breezePage.items,
                            key = { it.id.ifBlank { it.createTime + it.content } },
                            contentType = { "breezemoon_row" },
                        ) { breeze ->
                            ProfileBreezemoonRow(item = breeze)
                        }
                        if (breezePage.hasMore) {
                            item(key = "other_breeze_more", contentType = "breeze_more") {
                                ProfileArticleLoadMore(
                                    isLoading = breezePage.isLoadingMore,
                                    onLoadMore = { dispatch(ProfileAction.LoadMoreBreezemoons) },
                                )
                            }
                        }
                    }
                }
                state.error?.let {
                    item(key = "error", contentType = "error") {
                        Text(text = it, color = FishPiErrorRed, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                }
            }
        }
    }

    if (state.settingsOpen) {
        ProfileSettingsOverlay(
            user = user,
            currentApiKey = state.currentApiKey,
            chatFilters = state.chatFilters,
            savedAccounts = state.savedAccounts,
            themeOptions = state.themeOptions,
            themeKey = state.themeKey,
            chatWallpaperUri = state.chatWallpaperUri,
            onThemeChange = { dispatch(ProfileAction.ChangeTheme(it)) },
            onImportTheme = {
                dispatch(ProfileAction.ImportTheme(it))
                Result.success("已提交")
            },
            onEditTheme = { dispatch(ProfileAction.OpenThemeEditor) },
            onDeleteCustomTheme = {
                dispatch(ProfileAction.DeleteCustomTheme(it))
                true
            },
            onChatWallpaperChange = { dispatch(ProfileAction.ChangeChatWallpaper(it)) },
            onOpenFilters = { dispatch(ProfileAction.OpenFilterSettings) },
            onSwitchAccount = { dispatch(ProfileAction.SwitchAccount(it)) },
            onAddAccount = { dispatch(ProfileAction.AddAccount) },
            onLogout = { dispatch(ProfileAction.Logout) },
            onDismiss = { dispatch(ProfileAction.DismissSettings) },
        )
    }

    if (state.filterSettingsOpen) {
        ChatFilterSettingsOverlay(
            config = state.chatFilters,
            onSave = { dispatch(ProfileAction.SaveChatFilters(it)) },
            onDismiss = { dispatch(ProfileAction.DismissFilterSettings) },
        )
    }

    if (state.themeEditorOpen) {
        ThemeEditorOverlay(
            option = state.themeOptions.firstOrNull { it.key == state.themeKey } ?: state.themeOptions.first(),
            onSave = {
                dispatch(ProfileAction.SaveEditedTheme(it))
                Result.success("已提交")
            },
            onDismiss = { dispatch(ProfileAction.DismissThemeEditor) },
        )
    }
    if (state.aboutOpen) {
        AboutAcknowledgementsWorkspace(onDismiss = { dispatch(ProfileAction.DismissAbout) })
    }
    if (state.transferOpen) {
        ProfileTransferDialog(
            user = user,
            onDismiss = { dispatch(ProfileAction.DismissTransfer) },
            onTransfer = { amount, memo -> dispatch(ProfileAction.Transfer(amount, memo)) },
        )
    }

    BackHandler(enabled = active && (state.closeOnBack || !state.isSelfProfile) && !isOverlayOpen) {
        dispatch(ProfileAction.CloseProfile)
    }
    BackHandler(enabled = active && state.contentOpen && !isOverlayOpen) {
        dispatch(ProfileAction.DismissContent)
    }
    BackHandler(enabled = active && state.settingsOpen && !state.filterSettingsOpen && !state.themeEditorOpen) {
        dispatch(ProfileAction.DismissSettings)
    }
    BackHandler(enabled = active && state.filterSettingsOpen) {
        dispatch(ProfileAction.DismissFilterSettings)
    }
    BackHandler(enabled = active && state.themeEditorOpen) {
        dispatch(ProfileAction.DismissThemeEditor)
    }
    BackHandler(enabled = active && state.aboutOpen) {
        dispatch(ProfileAction.DismissAbout)
    }
}

@Composable
private fun ProfileSettingsOverlay(
    user: FishPiUser,
    currentApiKey: String,
    chatFilters: ChatFilterConfig,
    savedAccounts: List<SavedAccount>,
    themeOptions: List<FishPiThemeOption>,
    themeKey: String,
    chatWallpaperUri: String,
    onThemeChange: (String) -> Unit,
    onImportTheme: (String) -> Result<String>,
    onEditTheme: () -> Unit,
    onDeleteCustomTheme: (String) -> Boolean,
    onChatWallpaperChange: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onSwitchAccount: (SavedAccount) -> Unit,
    onAddAccount: () -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FishPiTheme.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "设置",
                        color = FishPiTheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.weight(1f),
                    )
                    FishPiIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = "关闭设置",
                        onClick = onDismiss,
                        background = FishPiTheme.surfaceContainer,
                        iconSizeDp = 18,
                    )
                }
            }
            item {
                ThemeSettingsSection(
                    options = themeOptions,
                    selectedKey = themeKey,
                    onSelect = onThemeChange,
                    onImportTheme = onImportTheme,
                    onEditTheme = onEditTheme,
                    onDeleteCustomTheme = onDeleteCustomTheme,
                    chatWallpaperUri = chatWallpaperUri,
                    onChatWallpaperChange = onChatWallpaperChange,
                )
            }
            item {
                ProfileActionSection(
                    chatFilters = chatFilters,
                    savedAccounts = savedAccounts,
                    onOpenFilters = onOpenFilters,
                    onLogout = onLogout,
                )
            }
            item {
                AccountSwitchCard(
                    currentApiKey = currentApiKey,
                    accounts = savedAccounts,
                    onSwitchAccount = onSwitchAccount,
                    onAddAccount = onAddAccount,
                )
            }
        }
    }
}

private fun ChatFilterConfig.profileRuleCount(): Int =
    blockedUsers.size + blockedKeywords.size + blockedPrefixKeywords.size + blockedRegex.size

@Composable
private fun ThemeSettingsSection(
    options: List<FishPiThemeOption>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    onImportTheme: (String) -> Result<String>,
    onEditTheme: () -> Unit,
    onDeleteCustomTheme: (String) -> Boolean,
    chatWallpaperUri: String,
    onChatWallpaperChange: (String) -> Unit,
) {
    val context = LocalContext.current
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val result = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().use { it.readText() }
            }.orEmpty()
        }.mapCatching { raw ->
            onImportTheme(raw).getOrThrow()
        }
        result.fold(
            onSuccess = { FishPiNotifier.success("已导入主题：$it") },
            onFailure = { FishPiNotifier.error("主题导入失败：${it.message ?: "格式不正确"}") },
        )
    }
    val wallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.copyUriToSingleFile(uri, "chat_wallpapers", "wallpaper")
        }.onSuccess { file ->
            onChatWallpaperChange(Uri.fromFile(file).toString())
            FishPiNotifier.success("已设置聊天背景图片")
        }.onFailure { err ->
            FishPiNotifier.error("设置聊天背景失败：${err.message ?: "无法读取图片"}")
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FishPiTheme.surfaceContainer)
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = "主题",
            color = FishPiTheme.weakText,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
        options.forEach { option ->
            val palette = option.palette
            val isCustom = option.builtinPreset == null && option.rawJson != null
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { onSelect(option.key) })
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(palette.wallpaperColors)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(palette.accent),
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = option.label,
                        color = FishPiTheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = option.description,
                        color = FishPiTheme.weakText,
                    )
                }
                if (option.key == selectedKey) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "当前主题",
                        tint = profileAccentColor(),
                    )
                }
                if (isCustom) {
                    IconButton(
                        onClick = {
                            val removed = onDeleteCustomTheme(option.key)
                            if (removed) {
                                FishPiNotifier.success("已删除自定义主题")
                            } else {
                                FishPiNotifier.error("删除失败")
                            }
                        },
                        modifier = Modifier.size(30.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "删除自定义主题",
                            tint = FishPiErrorRed,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
        ThemeActionRow(
            title = "编辑当前主题",
            summary = "调整字体、弱文字、背景、消息框和强调色",
            onClick = onEditTheme,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) }
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(profileAccentSoft()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = profileAccentColor(),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "导入主题文件", color = FishPiTheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text(text = "支持 JSON 主题文件，导入后自动应用", color = FishPiTheme.weakText)
            }
            Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null, tint = FishPiTheme.weakText.copy(alpha = 0.5f))
        }
        ThemeActionRow(
            title = if (chatWallpaperUri.isBlank()) "选择聊天背景图片" else "更换聊天背景图片",
            summary = if (chatWallpaperUri.isBlank()) "从相册选择图片作为聊天壁纸" else "已设置自定义背景图片",
            onClick = {
                wallpaperLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        )
        if (chatWallpaperUri.isNotBlank()) {
            ThemeActionRow(
                title = "清除聊天背景图片",
                summary = "恢复当前主题的渐变背景",
                danger = true,
                onClick = { onChatWallpaperChange("") },
            )
        }
    }
}

@Composable
private fun ThemeEditorOverlay(
    option: FishPiThemeOption,
    onSave: (String) -> Result<String>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var name by remember(option.key) { mutableStateOf(option.label) }
    var editablePalette by remember(option.key) { mutableStateOf(EditableThemePalette.from(option.palette)) }
    var error by remember { mutableStateOf<String?>(null) }

    fun applyPalette(palette: FishPiPalette) {
        editablePalette = EditableThemePalette.from(palette)
        error = null
    }

    fun normalizedPalette(reportError: Boolean = false): FishPiPalette? {
        if (!editablePalette.isValid()) {
            if (reportError) {
                error = "颜色需要使用 #RRGGBB 格式"
            }
            return null
        }
        if (reportError) {
            error = null
        }
        return editablePalette.toPalette(option.palette)
    }

    val previewPalette by remember(
        editablePalette,
        option.palette,
    ) {
        derivedStateOf {
            normalizedPalette(reportError = false) ?: option.palette
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FishPiTheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "编辑主题",
                    color = FishPiTheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f),
                )
                FishPiIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "关闭主题编辑",
                    onClick = onDismiss,
                    background = FishPiTheme.surfaceContainer,
                    iconSizeDp = 18,
                )
            }
            ThemePreviewCard(
                palette = previewPalette,
                title = name.ifBlank { "应用内主题" },
            )
            TextField(
                value = name,
                onValueChange = { name = it },
                label = "主题名称",
                placeholder = "例如：我的蓝色主题",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            PresetChip("重置为夜间模式", onClick = { applyPalette(option.palette) })
            ThemeColorSections.forEach { section ->
                Text(
                    text = section.label,
                    color = FishPiTheme.weakText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                section.colors.forEach { spec ->
                    ThemeColorField(spec.label, editablePalette[spec.key]) {
                        editablePalette = editablePalette.with(spec.key, it)
                    }
                }
            }
            error?.let { Text(text = it, color = FishPiErrorRed) }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(profileAccentSoft())
                    .clickable {
                        val palette = normalizedPalette(reportError = true) ?: return@clickable
                        val raw = buildEditableThemeJson(
                            label = name.ifBlank { "应用内主题" },
                            description = "应用内编辑主题",
                            palette = palette,
                        )
                        val result = onSave(raw)
                        result.fold(
                            onSuccess = { FishPiNotifier.success("已保存主题：$it") },
                            onFailure = { FishPiNotifier.error("保存失败：${it.message ?: "颜色格式不正确"}") },
                        )
                        if (result.isSuccess) {
                            onDismiss()
                        }
                    }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "保存并应用主题",
                    color = profileAccentColor(),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(FishPiTheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        color = FishPiTheme.onSurface,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
    )
}

@Composable
private fun ThemePreviewCard(
    palette: FishPiPalette,
    title: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(palette.wallpaperColors))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "鱼友", color = palette.userName, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.clientBackground)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(text = "Android", color = palette.clientText)
            }
            Text(text = title, color = palette.onSurface, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(palette.incomingBubble)
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Text(text = "别人发来的消息", color = palette.onSurface)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(palette.quoteBackground)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = 28.dp)
                        .clip(CircleShape)
                        .background(palette.quoteLine),
                )
                Text(text = "引用 @fishpi 和链接", color = palette.quoteText)
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.End)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.outgoingBubble)
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Text(text = "自己发送的消息", color = palette.onSurface)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.surface)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(text = "surface 页面卡片底色", color = palette.onSurface)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.surfaceContainer)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(text = "surfaceContainer 输入框/控件底色", color = palette.onSurface)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "链接 / @用户", color = palette.linkText, fontWeight = FontWeight.SemiBold)
            Text(text = "弱文字说明", color = palette.weakText)
        }
        Text(text = "消息时间 12:34", color = palette.timeText)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeToolDot("相册", palette.toolGallery)
            ThemeToolDot("拍照", palette.toolCamera)
            ThemeToolDot("红包", palette.toolRedPacket)
        }
    }
}

@Composable
private fun ThemeToolDot(label: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(text = label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ThemeColorField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    var pickerOpen by remember(label) { mutableStateOf(false) }
    var red by remember(label) { mutableStateOf(255f) }
    var green by remember(label) { mutableStateOf(255f) }
    var blue by remember(label) { mutableStateOf(255f) }
    var hexInput by remember(label) { mutableStateOf("") }

    fun resetPickerFromCurrentValue() {
        val (r, g, b) = value.toThemeRgb()
        red = r.toFloat()
        green = g.toFloat()
        blue = b.toFloat()
        hexInput = value
    }

    val currentColor = themeHexFromRgb(red.toInt(), green.toInt(), blue.toInt())
    val isValidHex = hexInput.isValidThemeHex()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (value.isValidThemeHex()) value.toThemeColor() else FishPiTheme.surfaceContainer)
                .then(
                    if (value.isValidThemeHex()) {
                        Modifier.clickable {
                            resetPickerFromCurrentValue()
                            pickerOpen = true
                        }
                    } else Modifier
                ),
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = "#FFFFFF",
            singleLine = true,
            isError = value.isNotBlank() && !value.isValidThemeHex(),
            modifier = Modifier.weight(1f),
        )
    }

    if (pickerOpen) {
        AppBottomSheet(onDismiss = { pickerOpen = false }) {
            AppSheetTitle(label)
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(currentColor.toThemeColor()),
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextField(
                value = hexInput,
                onValueChange = { input ->
                    hexInput = input
                    if (input.isValidThemeHex()) {
                        val (r, g, b) = input.toThemeRgb()
                        red = r.toFloat(); green = g.toFloat(); blue = b.toFloat()
                    }
                },
                label = "十六进制颜色",
                placeholder = "#FF8800",
                singleLine = true,
                isError = hexInput.isNotBlank() && !isValidHex,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            SliderRow(label = "R ${red.toInt()}", value = red, color = currentColor.toThemeColor()) {
                red = it; hexInput = themeHexFromRgb(it.toInt(), green.toInt(), blue.toInt())
            }
            SliderRow(label = "G ${green.toInt()}", value = green, color = currentColor.toThemeColor()) {
                green = it; hexInput = themeHexFromRgb(red.toInt(), it.toInt(), blue.toInt())
            }
            SliderRow(label = "B ${blue.toInt()}", value = blue, color = currentColor.toThemeColor()) {
                blue = it; hexInput = themeHexFromRgb(red.toInt(), green.toInt(), it.toInt())
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(FishPiTheme.surfaceContainer)
                        .clickable { pickerOpen = false }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(text = "取消", color = FishPiTheme.weakText, fontWeight = FontWeight.Medium)
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(profileAccentSoft())
                        .clickable {
                            onValueChange(currentColor)
                            pickerOpen = false
                        }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(text = "应用", color = profileAccentColor(), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, color: Color, onValueChange: (Float) -> Unit) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = 0f..255f,
        modifier = Modifier.fillMaxWidth(),
        colors = SliderDefaults.colors(
            thumbColor = color,
            activeTrackColor = color,
        ),
    )
}

@Composable
private fun ThemeActionRow(
    title: String,
    summary: String,
    icon: ImageVector = Icons.Rounded.Add,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (danger) FishPiErrorRed.copy(alpha = 0.12f) else profileAccentSoft()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (danger) Icons.AutoMirrored.Rounded.Logout else icon,
                contentDescription = null,
                tint = if (danger) FishPiErrorRed else profileAccentColor(),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = if (danger) FishPiErrorRed else FishPiTheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = summary, color = FishPiTheme.weakText)
        }
        Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null, tint = FishPiTheme.onSurface.copy(alpha = 0.42f))
    }
}

@Composable
private fun AccountSwitchCard(
    currentApiKey: String,
    accounts: List<SavedAccount>,
    onSwitchAccount: (SavedAccount) -> Unit,
    onAddAccount: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FishPiTheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "账号",
            color = FishPiTheme.weakText,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )
        if (accounts.isEmpty()) {
            Text(text = "当前还没有保存其他账号", color = FishPiTheme.weakText)
        } else {
            accounts.forEach { account ->
                AccountRow(
                    account = account,
                    selected = account.apiKey == currentApiKey,
                    onClick = { onSwitchAccount(account) },
                )
            }
        }
        Row(
            modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(profileAccentSoft())
            .clickable(onClick = onAddAccount)
            .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = null, tint = profileAccentColor())
            Text(text = "添加账号", color = profileAccentColor(), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ProfileActionSection(
    chatFilters: ChatFilterConfig,
    savedAccounts: List<SavedAccount>,
    onOpenFilters: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FishPiTheme.surfaceContainer)
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = "设置与管理",
            color = FishPiTheme.weakText,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
        ProfileActionRow(
            icon = { Icon(imageVector = Icons.Rounded.Block, contentDescription = null, tint = profileAccentColor()) },
            title = "聊天室过滤",
            summary = "${chatFilters.profileRuleCount()} 条规则",
            onClick = onOpenFilters,
        )
        ProfileActionRow(
            icon = { Icon(imageVector = Icons.Rounded.SwitchAccount, contentDescription = null, tint = profileAccentColor()) },
            title = "账号",
            summary = if (savedAccounts.isEmpty()) "添加或切换账号" else "${savedAccounts.size} 个已保存账号",
            onClick = {},
            enabled = false,
        )
        ProfileActionRow(
            icon = { Icon(imageVector = Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = FishPiErrorRed) },
            title = "退出登录",
            summary = "清除本机保存的 API Key",
            onClick = onLogout,
            danger = true,
        )
    }
}

@Composable
private fun ProfileActionRow(
    icon: @Composable () -> Unit,
    title: String,
    summary: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (danger) FishPiErrorRed.copy(alpha = 0.12f) else FishPiTheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = title,
                color = if (danger) FishPiErrorRed else FishPiTheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = summary, color = FishPiTheme.weakText)
        }
        if (enabled) {
            Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null, tint = FishPiTheme.weakText.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun AccountRow(
    account: SavedAccount,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) profileAccentSoft() else FishPiTheme.surface)
            .clickable(enabled = !selected, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FishPiAvatar(
            avatarUrl = account.avatarUrl,
            displayName = account.displayName.ifBlank { account.userName },
            contentDescription = "账号头像",
            size = 34.dp,
            fallback = FishPiAvatarFallback.Icon,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = account.displayName.ifBlank { account.userName.ifBlank { "已保存账号" } },
                color = if (selected) profileAccentColor() else FishPiTheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            if (account.userName.isNotBlank()) {
                Text(
                    text = "@${account.userName}",
                    color = FishPiTheme.onSurface.copy(alpha = 0.56f),
                )
            }
        }
        if (selected) {
            Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = "当前账号", tint = profileAccentColor())
        }
    }
}

@Composable
private fun ProfileHero(
    user: FishPiUser,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        FishPiTheme.surface,
                        FishPiTheme.surfaceElevated,
                    ),
                ),
            )
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FishPiAvatar(
                avatarUrl = user.userAvatarUrl,
                displayName = user.displayName,
                contentDescription = "头像",
                size = 64.dp,
                fallback = FishPiAvatarFallback.Icon,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = user.displayName,
                        fontWeight = FontWeight.Bold,
                        color = FishPiTheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (user.role.isNotBlank()) {
                        FishPiRoleBadge(role = user.role)
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProfileMetaChip(text = "@${user.userName}")
                }
                if (user.userNo.isNotBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ProfileMetaChip(text = "摸鱼派第 ${user.userNo} 号成员")
                    }
                }
            }
            FishPiIconButton(
                icon = Icons.Rounded.Info,
                contentDescription = "打开关于与鸣谢",
                onClick = onOpenAbout,
                background = FishPiTheme.surface.copy(alpha = 0.82f),
                sizeDp = 34,
                iconSizeDp = 18,
            )
            FishPiIconButton(
                icon = Icons.Rounded.Refresh,
                contentDescription = "刷新我的信息",
                onClick = onRefresh,
                enabled = !isLoading,
                tint = profileAccentColor(),
                background = profileAccentSoft(),
                sizeDp = 34,
                iconSizeDp = 18,
            )
            FishPiIconButton(
                icon = Icons.Rounded.Settings,
                contentDescription = "打开我的设置",
                onClick = onOpenSettings,
                background = FishPiTheme.surface.copy(alpha = 0.82f),
                sizeDp = 34,
                iconSizeDp = 18,
            )
        }
    }
}

@Composable
private fun ProfileOverviewPage(
    user: FishPiUser,
    medals: List<MedalView>,
    isLoadingMedals: Boolean,
    apiKey: String,
    noticeUnread: Long,
    onOpenPosts: () -> Unit,
    onOpenNotice: () -> Unit,
    onRefresh: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSettings: () -> Unit,
    isSelfProfile: Boolean,
    isFollowingUser: Boolean,
    isFollowRunning: Boolean,
    onFollow: () -> Unit,
    onPrivateChat: () -> Unit,
    onTransfer: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.82f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.28f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.0f),
                        ),
                    ),
                ),
        ) {
            if (user.cardBg.isNotBlank()) {
                coil3.compose.SubcomposeAsyncImage(
                    model = user.cardBg,
                    imageLoader = rememberFishPiImageLoader(),
                    contentDescription = "个人主页背景",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.36f),
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val overlapPx = (30f * density).toInt()
                    layout(placeable.width, placeable.height - overlapPx) {
                        placeable.place(0, -overlapPx)
                    }
                }
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ProfileOverviewCard(
                user = user,
                medals = medals,
                isLoadingMedals = isLoadingMedals,
                apiKey = apiKey,
                isSelfProfile = isSelfProfile,
                isFollowingUser = isFollowingUser,
                isFollowRunning = isFollowRunning,
                onFollow = onFollow,
                onPrivateChat = onPrivateChat,
                onTransfer = onTransfer,
                onOpenSettings = onOpenSettings,
            )
            if (isSelfProfile) {
                ProfileActionGrid(
                    noticeUnread = noticeUnread,
                    onOpenPosts = onOpenPosts,
                    onOpenNotice = onOpenNotice,
                    onRefresh = onRefresh,
                    onOpenAbout = onOpenAbout,
                )
                ControlSurface {
                    Text(
                        text = "退出登录",
                        color = FishPiErrorRed,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(onClick = onLogout)
                            .padding(vertical = 9.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileOverviewCard(
    user: FishPiUser,
    medals: List<MedalView>,
    isLoadingMedals: Boolean,
    apiKey: String,
    isSelfProfile: Boolean,
    isFollowingUser: Boolean,
    isFollowRunning: Boolean,
    onFollow: () -> Unit,
    onPrivateChat: () -> Unit,
    onTransfer: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ContentCardSurface(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
        ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            FishPiAvatar(
                avatarUrl = user.userAvatarUrl,
                displayName = user.displayName,
                contentDescription = "头像",
                size = 82.dp,
                fallback = FishPiAvatarFallback.Icon,
                modifier = Modifier.align(Alignment.TopStart),
            )
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isSelfProfile) {
                    AnimalIconButton(
                        icon = Icons.Rounded.Settings,
                        contentDescription = "设置",
                        onClick = onOpenSettings,
                    )
                } else {
                    ProfileHeaderAction(
                        label = if (isFollowingUser) "已关注" else "关注",
                        icon = Icons.Rounded.PersonAdd,
                        enabled = !isFollowRunning && user.userId.isNotBlank(),
                        onClick = onFollow,
                    )
                    ProfileHeaderAction(icon = Icons.Rounded.ChatBubbleOutline, onClick = onPrivateChat)
                    ProfileHeaderAction(icon = Icons.Rounded.AttachMoney, onClick = onTransfer)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = user.displayName,
                    color = FishPiTheme.onSurface,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (user.role.isNotBlank()) {
                    FishPiRoleBadge(role = user.role)
                }
            }
            Text(text = "@${user.userName}", color = FishPiTheme.weakText, fontSize = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (user.userNo.isNotBlank()) {
                    ProfileMetaChip(text = "摸鱼派第 ${user.userNo} 号成员")
                }
                if (user.city.isNotBlank()) {
                    ProfileMetaChip(text = user.city)
                }
                if (user.url.isNotBlank()) {
                    ProfileMetaChip(text = "个人主页")
                }
            }
        }
        Text(
            text = user.intro.ifBlank { "这个人很神秘，暂时还没有签名。" },
            color = FishPiTheme.onSurface.copy(alpha = 0.68f),
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (medals.isNotEmpty() || isLoadingMedals) {
            ProfileBadgeWall(user = user, medals = medals, isLoading = isLoadingMedals, apiKey = apiKey)
        }
        ProfileStatsStrip(user = user)
    }
    }
}
}

@Composable
private fun ProfileActionGrid(
    noticeUnread: Long,
    onOpenPosts: () -> Unit,
    onOpenNotice: () -> Unit,
    onRefresh: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    ContentCardSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
    ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        ProfileActionItem(Icons.AutoMirrored.Rounded.Article, "我的帖子", onOpenPosts)
        ProfileActionItem(Icons.Rounded.Notifications, "消息通知", onOpenNotice, badge = noticeUnread)
        ProfileActionItem(Icons.Rounded.Refresh, "检查更新", onRefresh)
        ProfileActionItem(Icons.Rounded.Info, "关于APP", onOpenAbout)
    }
    }
}

@Composable
private fun ProfileHeaderAction(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
) {
    ControlSurface(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = if (label == null) 14.dp else 16.dp, vertical = 10.dp),
    ) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) FishPiTheme.onSurface.copy(alpha = 0.72f) else FishPiTheme.weakText,
            modifier = Modifier.size(21.dp),
        )
        label?.let {
            Text(
                text = it,
                color = if (enabled) FishPiTheme.onSurface.copy(alpha = 0.72f) else FishPiTheme.weakText,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
    }
}

@Composable
private fun ProfileTransferDialog(
    user: FishPiUser,
    onDismiss: () -> Unit,
    onTransfer: (Int, String) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    val amount = amountText.toIntOrNull() ?: 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("转账给 ${user.displayName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter(Char::isDigit).take(8) },
                    label = { Text("积分数量") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it.take(80) },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                enabled = amount > 0,
                onClick = { onTransfer(amount, memo) },
            ) {
                Text("确认转账")
            }
        },
        dismissButton = {
            FishPiPillButton(text = "取消", onClick = onDismiss, compact = true)
        },
    )
}

@Composable
private fun ProfileActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    badge: Long = 0,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(profileAccentSoft()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = label, tint = profileAccentColor(), modifier = Modifier.size(24.dp))
            }
            if (badge > 0) {
                Text(
                    text = if (badge > 99) "99+" else badge.toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .sizeIn(minWidth = 14.dp, minHeight = 14.dp)
                        .clip(CircleShape)
                        .background(FishPiErrorRed)
                        .padding(horizontal = 4.dp, vertical = 0.dp),
                )
            }
        }
        Text(
            text = label,
            color = FishPiTheme.onSurface,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProfileMetaChip(text: String) {
    Text(
        text = text,
        color = FishPiTheme.weakText,
        fontSize = 11.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(FishPiTheme.surfaceContainer.copy(alpha = 0.70f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

private fun formatProfileNumber(value: Long): String = value.toString()

@Composable
private fun ProfileStatsStrip(user: FishPiUser) {
    ControlSurface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(vertical = 9.dp),
    ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileStatColumn(value = formatProfileNumber(user.points), label = "积分", modifier = Modifier.weight(1f))
        ProfileStatDivider()
        ProfileStatColumn(value = formatProfileNumber(user.following), label = "关注", modifier = Modifier.weight(1f))
        ProfileStatDivider()
        ProfileStatColumn(value = formatProfileNumber(user.follower), label = "粉丝", modifier = Modifier.weight(1f))
        ProfileStatDivider()
        ProfileStatColumn(value = formatProfileNumber(user.onlineMinutes), label = "总在线/分钟", modifier = Modifier.weight(1f))
    }
    }
}

@Composable
private fun ProfileStatColumn(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = value,
            color = FishPiTheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            color = FishPiTheme.weakText,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProfileStatDivider() {
    Box(
        modifier = Modifier
            .size(width = 1.dp, height = 30.dp)
            .background(FishPiTheme.surfaceContainer.copy(alpha = 0.92f)),
    )
}

@Composable
private fun ProfileBadgeWall(
    user: FishPiUser,
    medals: List<MedalView>,
    isLoading: Boolean,
    apiKey: String,
) {
    var expanded by remember(user.userName, medals.size) { mutableStateOf(false) }
    val visibleMedals = if (expanded) medals else medals.take(6)
    ContentCardSurface(
        modifier = Modifier
            .fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 13.dp),
    ) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "勋章墙",
                color = FishPiTheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (medals.size > 6) {
                Text(
                    text = if (expanded) "收起" else "展开",
                    color = profileAccentColor(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        when {
            isLoading -> Text(text = "正在加载勋章...", color = FishPiTheme.weakText)
            medals.isEmpty() -> Unit
            else -> {
                visibleMedals.chunked(3).forEach { rowMedals ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        repeat(3) { index ->
                            val medal = rowMedals.getOrNull(index)
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (medal != null) {
                                    FishPiMedalBadge(
                                        medal = medal,
                                        apiKey = apiKey,
                                        fillWidth = true,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

private fun parseColor(raw: String, fallback: Color): Color {
    val hex = raw.trim().removePrefix("#")
    if (hex.isEmpty()) return fallback
    return runCatching {
        Color(android.graphics.Color.parseColor("#$hex"))
    }.getOrDefault(fallback)
}

@Composable
private fun ProfileArticleSectionHeader(
    articleCount: Int,
    breezeCount: Int,
    selectedTab: String,
    isLoading: Boolean,
    onSelectTab: (String) -> Unit,
) {
    ControlSurface(
        modifier = Modifier
            .fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ProfileContentTabs(
            articleCount = articleCount,
            breezeCount = breezeCount,
            selectedTab = selectedTab,
            onSelectTab = onSelectTab,
        )
        if ((selectedTab == "article" && articleCount == 0) || (selectedTab == "breeze" && breezeCount == 0)) {
            Text(
                text = if (isLoading) {
                    if (selectedTab == "article") "正在加载个人帖子..." else "正在加载个人清风明月..."
                } else {
                    if (selectedTab == "article") "还没有发布帖子" else "还没有发布清风明月"
                },
                color = FishPiTheme.weakText,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            )
        }
    }
    }
}

@Composable
private fun ProfileArticleLoadMore(
    isLoading: Boolean,
    onLoadMore: () -> Unit,
) {
    Text(
        text = if (isLoading) "加载中..." else "加载更多",
        color = profileAccentColor(),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = !isLoading, onClick = onLoadMore)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

@Composable
private fun ProfileContentTabs(
    articleCount: Int,
    breezeCount: Int,
    selectedTab: String,
    onSelectTab: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileContentTab(
            text = "帖子",
            count = articleCount,
            selected = selectedTab == "article",
            onClick = { onSelectTab("article") },
        )
        ProfileContentTab(
            text = "清风明月",
            count = breezeCount,
            selected = selectedTab == "breeze",
            onClick = { onSelectTab("breeze") },
        )
    }
}

@Composable
private fun ProfileContentTab(text: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                color = if (selected) profileAccentColor() else FishPiTheme.weakText,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = count.toString(),
                color = if (selected) profileAccentColor() else FishPiTheme.weakText,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) profileAccentSoft() else FishPiTheme.surfaceContainer)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(width = 34.dp, height = 3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) profileAccentColor() else androidx.compose.ui.graphics.Color.Transparent),
        )
    }
}

@Composable
private fun ProfileArticleRow(article: ArticleSummary, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = article.title.ifBlank { "[无标题]" },
            color = FishPiTheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = article.time,
                color = FishPiTheme.weakText,
                modifier = Modifier.weight(1f),
            )
            ProfileArticleMetric(
                icon = Icons.Rounded.Visibility,
                value = article.viewCount,
                contentDescription = "浏览数",
            )
            ProfileArticleMetric(
                icon = Icons.Rounded.ChatBubbleOutline,
                value = article.commentCount,
                contentDescription = "评论数",
            )
        }
    }
}

@Composable
private fun ProfileBreezemoonRow(item: BreezemoonView) {
    val textColor = FishPiTheme.onSurface.toArgb()
    val accent = if (isSystemInDarkTheme()) Color(0xFFD4B96A) else Color(0xFFC9A44F)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(IntrinsicSize.Max)
                .clip(RoundedCornerShape(1.dp))
                .background(accent.copy(alpha = 0.45f)),
        )
        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = item.authorName.ifBlank { "鱼友" },
                    color = FishPiTheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Text(
                    text = listOf(item.timeAgo, item.city.takeIf { it.isNotBlank() }).filterNotNull().joinToString(" · "),
                    color = FishPiTheme.weakText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    TextView(context).apply {
                        textSize = 15f
                        setLineSpacing(0f, 1.2f)
                        movementMethod = LinkMovementMethod.getInstance()
                    }
                },
                update = { view ->
                    view.setTextColor(textColor)
                    view.text = Html.fromHtml(item.content, Html.FROM_HTML_MODE_LEGACY)
                },
            )
        }
    }
}

@Composable
private fun ProfileArticleMetric(
    icon: ImageVector,
    value: Long,
    contentDescription: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(start = 12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = FishPiTheme.weakText,
            modifier = Modifier.size(16.dp),
        )
        Text(text = value.toString(), color = FishPiTheme.weakText)
    }
}

@Composable
private fun ProfileInfoCard(user: FishPiUser) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FishPiTheme.surface.copy(alpha = 0.80f))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "资料", color = FishPiTheme.weakText, fontWeight = FontWeight.SemiBold)
        if (user.intro.isNotBlank()) {
            Text(text = user.intro)
        }
        if (user.city.isNotBlank()) {
            Text(text = "城市：${user.city}", color = FishPiTheme.onSurface.copy(alpha = 0.66f))
        }
        if (user.url.isNotBlank()) {
            Text(text = "主页：${user.url}", color = FishPiTheme.onSurface.copy(alpha = 0.66f))
        }
        if (user.onlineMinutes > 0) {
            Text(
                text = "在线时长：${formatOnlineMinutes(user.onlineMinutes)}",
                color = FishPiTheme.onSurface.copy(alpha = 0.66f),
            )
        }
    }
}

private fun formatOnlineMinutes(minutes: Long): String {
    val days = minutes / 1440
    val hours = (minutes % 1440) / 60
    val mins = minutes % 60
    return buildString {
        if (days > 0) append("${days}天")
        if (hours > 0) append("${hours}小时")
        if (mins > 0 || isEmpty()) append("${mins}分钟")
    }
}

private fun formatProfileLivenessValue(value: Double): String {
    val normalized = value.coerceAtLeast(0.0)
    return if (normalized % 1.0 == 0.0) {
        normalized.toLong().toString()
    } else {
        "%.2f".format(java.util.Locale.US, normalized).trimEnd('0').trimEnd('.')
    }
}




