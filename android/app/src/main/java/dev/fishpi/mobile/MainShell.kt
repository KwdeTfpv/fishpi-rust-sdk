package dev.fishpi.mobile

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import dev.fishpi.mobile.data.ChatFilterConfig
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.data.FishPiApiClient
import dev.fishpi.mobile.data.NoticeRealtimeClient
import dev.fishpi.mobile.data.ReleaseUpdateInfo
import dev.fishpi.mobile.data.SavedAccount
import dev.fishpi.mobile.data.SessionStore
import dev.fishpi.mobile.data.UpdateChecker
import dev.fishpi.mobile.data.UpdateDownloader
import dev.fishpi.mobile.feature.article.ArticleRoute
import dev.fishpi.mobile.feature.breezemoon.BreezemoonRoute
import dev.fishpi.mobile.feature.chat.ChatController
import dev.fishpi.mobile.feature.chat.ChatRealtimeRouteLifecycle
import dev.fishpi.mobile.feature.chat.ChatRoute
import dev.fishpi.mobile.feature.chat.blocksChatMessage
import dev.fishpi.mobile.feature.extensionstore.ExtensionStoreRoute
import dev.fishpi.mobile.shared.message.toRenderHints
import dev.fishpi.mobile.feature.home.HomeRoute
import dev.fishpi.mobile.feature.pluginui.PluginUiRoute
import dev.fishpi.mobile.feature.privatechat.PrivateChatRoute
import dev.fishpi.mobile.feature.profile.ProfileRoute
import dev.fishpi.mobile.ui.animal.AnimalDock
import dev.fishpi.mobile.ui.animal.AnimalDockItem
import dev.fishpi.mobile.ui.animal.AnimalChatGlyph
import dev.fishpi.mobile.ui.animal.AnimalIslandBackground
import dev.fishpi.mobile.ui.animal.AnimalPanel
import dev.fishpi.mobile.ui.animal.AnimalStatusPill
import dev.fishpi.mobile.ui.components.ControlSurface
import dev.fishpi.mobile.ui.components.IconActionButton
import dev.fishpi.mobile.ui.components.statusSuccessColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private enum class FishTab(val title: String) {
    Home("首页"),
    Article("帖子"),
    Chat("聊天"),
    Me("我的"),
}

private enum class HomePane {
    Home,
    Breezemoon,
    Notice,
    Fun,
    Store,
}

private enum class ChatPane {
    Home,
    Room,
}

private val CompactTopBarHeight = 48.dp
private val CompactBottomNavHeight = 54.dp

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun MainShell(
    session: AppSession,
    savedAccounts: List<SavedAccount>,
    themeOptions: List<FishPiThemeOption>,
    themeKey: String,
    onCycleTheme: () -> Unit,
    onThemeChange: (String) -> Unit,
    onImportThemePackage: suspend (String) -> Result<String>,
    onSaveEditedTheme: (String) -> Result<String>,
    onSaveStoreTheme: (String) -> Result<String>,
    isStoreThemeSaved: (String) -> Boolean,
    onDeleteCustomTheme: (String) -> Boolean,
    chatWallpaperUri: String,
    onChatWallpaperChange: (String) -> Unit,
    onSwitchAccount: (SavedAccount) -> Unit,
    onAddAccount: () -> Unit,
    onLogout: () -> Unit,
) {
    val api = remember { FishPiApiClient.shared }
    val noticeRealtime = remember { NoticeRealtimeClient() }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val store = remember { SessionStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val chatController = remember(session.apiKey, session.user.userName) {
        ChatController(
            context = context.applicationContext,
            apiKey = session.apiKey,
            currentUsername = session.user.userName,
        )
    }
    val chatControllerState by chatController.state.collectAsState()
    val chatControllerMessages by chatController.legacyMessages.collectAsState()
    var tab by remember { mutableStateOf(FishTab.Home) }
    var homePane by remember { mutableStateOf(HomePane.Home) }
    var chatPane by remember { mutableStateOf(ChatPane.Home) }
    var privateChatDetailActive by remember { mutableStateOf(false) }
    var noticeOpen by remember { mutableStateOf(false) }
    val tabStateHolder = rememberSaveableStateHolder()
    var noticeUnread by remember { mutableStateOf(0L) }
    var privateUnread by remember { mutableStateOf(0L) }
    var openBlockedRequest by remember { mutableStateOf(0) }
    var chatFilters by remember { mutableStateOf(store.getChatFilters()) }
    var appInForeground by remember { mutableStateOf(true) }
    var lastBackPressedAt by remember { mutableStateOf(0L) }
    var articleJumpId by remember { mutableStateOf<String?>(null) }
    var articleJumpRequest by remember { mutableStateOf(0) }
    var articleReturnTab by remember { mutableStateOf<FishTab?>(null) }
    var profileUsername by remember { mutableStateOf<String?>(null) }
    var profileOverlayUsername by remember { mutableStateOf<String?>(null) }
    var privatePeerJump by remember { mutableStateOf<String?>(null) }
    var privatePeerJumpRequest by remember { mutableStateOf(0) }
    var noticeRefreshInFlight by remember { mutableStateOf(false) }
    var lastNoticeRefreshAt by remember { mutableStateOf(0L) }
    var updateInfo by remember { mutableStateOf<ReleaseUpdateInfo?>(null) }
    var updateDismissed by remember { mutableStateOf(false) }
    var updateDownloadId by remember { mutableStateOf<Long?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var chatRoomCanFollowBottom by remember { mutableStateOf(true) }
    var chatRoomFollowBottomProbe by remember { mutableStateOf<(() -> Boolean)?>(null) }
    val imeBottom = WindowInsets.ime.getBottom(density)
    val imeTargetBottom = WindowInsets.imeAnimationTarget.getBottom(density)
    val bottomNavSuppressed = imeBottom > 0 || imeTargetBottom > 0
    val chatRoomVisible = tab == FishTab.Chat && chatPane == ChatPane.Room
    val chatTabRealtimeEnabled = tab == FishTab.Chat && appInForeground
    val totalMessageUnread = noticeUnread + privateUnread

    fun saveChatFilters(next: ChatFilterConfig) {
        chatFilters = next
        store.saveChatFilters(next)
    }

    fun selectTab(next: FishTab) {
        if (tab == next) {
            if (next == FishTab.Home) {
                homePane = HomePane.Home
            }
            if (next == FishTab.Chat) {
                chatPane = ChatPane.Home
                privateChatDetailActive = false
            }
            return
        }
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        tab = next
        if (next == FishTab.Chat) {
            privateUnread = 0
        }
        if (next != FishTab.Home) {
            homePane = HomePane.Home
        }
        if (next != FishTab.Chat) {
            chatPane = ChatPane.Home
            privateChatDetailActive = false
        }
    }

    fun openArticleTabByJump(articleId: String, returnTo: FishTab? = null) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        articleJumpId = articleId
        articleJumpRequest += 1
        articleReturnTab = returnTo
        tab = FishTab.Article
        homePane = HomePane.Home
        chatPane = ChatPane.Home
        privateChatDetailActive = false
    }

    fun refreshNoticeUnread(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && (noticeRefreshInFlight || now - lastNoticeRefreshAt < 1200L)) {
            return
        }
        noticeRefreshInFlight = true
        lastNoticeRefreshAt = now
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                        api.getNoticeUnreadCount(session.apiKey).total
                    }
                }.onSuccess {
                    noticeUnread = it
                lastNoticeRefreshAt = System.currentTimeMillis()
            }.onFailure {
                // Cool down briefly after failure to avoid immediate retry storms.
                lastNoticeRefreshAt = System.currentTimeMillis()
            }
            noticeRefreshInFlight = false
        }
    }

    fun refreshChatRoomHistory(force: Boolean = false) {
        chatController.refreshHistory(skipIfLoaded = !force)
    }

    fun openChatRoomDetail() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        privateChatDetailActive = false
        tab = FishTab.Chat
        chatPane = ChatPane.Room
        refreshChatRoomHistory(force = true)
    }

    LaunchedEffect(session.apiKey) {
        refreshNoticeUnread(force = true)
    }

    LaunchedEffect(session.apiKey, tab) {
        if (tab == FishTab.Chat) {
            refreshChatRoomHistory()
        }
    }

    fun checkForUpdates(manual: Boolean) {
        if (isCheckingUpdate) {
            if (manual) FishPiNotifier.show("正在检查更新...")
            return
        }
        isCheckingUpdate = true
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { UpdateChecker.checkForUpdate() }
            }.onSuccess { result ->
                val info = result.updateInfo
                if (info != null) {
                    updateInfo = info
                    updateDismissed = false
                    if (manual) FishPiNotifier.success("发现新版本 ${info.tagName}")
                } else if (manual) {
                    val latest = result.latestTagName.ifBlank { BuildConfig.VERSION_NAME }
                    FishPiNotifier.show("当前已是最新版本：$latest")
                }
            }.onFailure {
                if (manual) FishPiNotifier.error("检查更新失败：${it.message ?: "网络异常"}")
            }
            isCheckingUpdate = false
        }
    }

    LaunchedEffect(session.apiKey) {
        updateDismissed = false
        checkForUpdates(manual = false)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> appInForeground = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    appInForeground = false
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(context, updateDownloadId) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent?) {
                if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    return
                }
                val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (completedId <= 0L || completedId != updateDownloadId) {
                    return
                }
                val installed = runCatching {
                    UpdateDownloader.installDownloadedApk(context, completedId)
                }.getOrDefault(false)
                if (!installed) {
                    FishPiNotifier.error("下载完成，但安装启动失败，请手动安装")
                }
                updateDownloadId = null
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    DisposableEffect(session.apiKey, appInForeground) {
        if (appInForeground) {
            noticeRealtime.connect(
                apiKey = session.apiKey,
                onNotice = { refreshNoticeUnread() },
            )
        } else {
            noticeRealtime.disconnect()
        }
        onDispose {
            noticeRealtime.disconnect()
        }
    }

    ChatRealtimeRouteLifecycle(
        chatController = chatController,
        enabled = chatTabRealtimeEnabled,
        chatFilters = chatFilters,
        isRoomVisible = { chatRoomVisible },
        shouldFollowBottom = {
            chatController.state.value.shouldFollowBottom
        },
    )

    LaunchedEffect(chatRoomVisible) {
        if (!chatRoomVisible) {
            chatRoomCanFollowBottom = false
        }
    }

    BackHandler {
        if (noticeOpen) {
            noticeOpen = false
            return@BackHandler
        }
        if (profileOverlayUsername != null) {
            profileOverlayUsername = null
            return@BackHandler
        }
        if (tab == FishTab.Chat && chatPane == ChatPane.Room) {
            chatPane = ChatPane.Home
            return@BackHandler
        }
        if (tab == FishTab.Home && homePane == HomePane.Store) {
            homePane = HomePane.Fun
            return@BackHandler
        }
        if (tab == FishTab.Home && homePane != HomePane.Home) {
            homePane = HomePane.Home
            return@BackHandler
        }
        val now = System.currentTimeMillis()
        if (now - lastBackPressedAt < 1_800L) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressedAt = now
            FishPiNotifier.show("再按一次退出摸鱼派")
        }
    }

    FishPiNotificationHost()

    AnimalIslandBackground {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        val homeDetailVisible = tab == FishTab.Home && homePane == HomePane.Breezemoon
        val bottomNavHiddenByDetail = noticeOpen || profileOverlayUsername != null || chatRoomVisible || homeDetailVisible || (tab == FishTab.Chat && privateChatDetailActive)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(if (tab == FishTab.Chat || tab == FishTab.Home) Modifier else Modifier.statusBarsPadding()),
        ) {
            if (chatRoomVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(3f),
                ) {
                    ChatRoute(
                        chatFilters = chatFilters,
                        openBlockedRequest = openBlockedRequest,
                        active = true,
                        chatController = chatController,
                        themeLabel = themeOptions.firstOrNull { it.key == themeKey }?.label ?: themeKey,
                        noticeUnread = noticeUnread,
                        onCycleTheme = onCycleTheme,
                        onOpenNotice = { noticeOpen = true },
                        onFollowBottomChanged = { chatRoomCanFollowBottom = it },
                        onFollowBottomProbeChanged = { chatRoomFollowBottomProbe = it },
                        onBlockedRequestHandled = { openBlockedRequest = 0 },
                        onOpenUserProfile = { username ->
                            profileOverlayUsername = username
                        },
                        onBack = {
                            chatPane = ChatPane.Home
                        },
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (tab == FishTab.Home) 1f else 0f)
                    .zIndex(if (tab == FishTab.Home) 2f else 0f),
            ) {
                tabStateHolder.SaveableStateProvider(key = FishTab.Home.name) {
                    when (homePane) {
                        HomePane.Home -> {
                            HomeRoute(
                                session = session,
                                noticeUnread = totalMessageUnread,
                                onOpenChat = { openChatRoomDetail() },
                                onOpenArticle = { selectTab(FishTab.Article) },
                                onOpenArticleDetail = { articleId -> openArticleTabByJump(articleId, returnTo = FishTab.Home) },
                                onOpenBreezemoon = { homePane = HomePane.Breezemoon },
                                onOpenFun = { homePane = HomePane.Fun },
                                onOpenProfile = { selectTab(FishTab.Me) },
                            )
                        }
                        HomePane.Breezemoon -> BreezemoonRoute(
                            session = session,
                            active = tab == FishTab.Home && homePane == HomePane.Breezemoon,
                        )
                        HomePane.Notice -> NoticeScreen(
                            session = session,
                            unread = noticeUnread,
                            onUnreadChange = { noticeUnread = it },
                            onDismiss = { homePane = HomePane.Home },
                            onJumpToChatRoom = {
                                openChatRoomDetail()
                            },
                            onJumpToArticle = { articleId ->
                                openArticleTabByJump(articleId)
                            },
                        )
                        HomePane.Fun -> FunApiScreen(
                            onOpenStore = { homePane = HomePane.Store },
                        )
                        HomePane.Store -> ExtensionStoreRoute(
                            apiKey = session.apiKey,
                            onImportTheme = onSaveStoreTheme,
                            isThemeSaved = isStoreThemeSaved,
                        )
                    }
                }
                if (tab != FishTab.Home) {
                    InputBlocker()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (tab == FishTab.Article) 1f else 0f)
                    .zIndex(if (tab == FishTab.Article) 1f else 0f),
            ) {
                tabStateHolder.SaveableStateProvider(key = FishTab.Article.name) {
                    ArticleRoute(
                        session = session,
                        active = tab == FishTab.Article,
                        jumpArticleId = articleJumpId,
                        jumpRequest = articleJumpRequest,
                        onDetailClosed = {
                            articleJumpId = null
                            articleReturnTab?.let { returnTab ->
                                tab = returnTab
                                articleReturnTab = null
                            }
                        },
                        onOpenUserProfile = { username ->
                            profileOverlayUsername = username
                        },
                    )
                }
                if (tab != FishTab.Article) {
                    InputBlocker()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (tab == FishTab.Chat && chatPane == ChatPane.Home) 1f else 0f)
                    .zIndex(if (tab == FishTab.Chat && chatPane == ChatPane.Home) 2f else 0f),
            ) {
                tabStateHolder.SaveableStateProvider(key = "chat-home") {
                    PrivateChatRoute(
                        session = session,
                        realtimeEnabled = appInForeground,
                        active = tab == FishTab.Chat && chatPane == ChatPane.Home,
                        jumpPeer = privatePeerJump,
                        jumpRequest = privatePeerJumpRequest,
                        onDetailActiveChange = { privateChatDetailActive = it },
                        onUnreadChange = { privateUnread = it },
                        listHeader = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = "聊天",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                ChatRoomEntryCard(
                                    messages = chatControllerMessages,
                                    isLoading = chatControllerState.isLoading,
                                    status = chatControllerState.connection.label,
                                    topic = chatControllerState.connection.topic,
                                    onlineCount = chatControllerState.connection.onlineCount.toInt(),
                                    chatFilters = chatFilters,
                                    onClick = {
                                        openChatRoomDetail()
                                    },
                                )
                            }
                        },
                    )
                }
                if (tab != FishTab.Chat || chatPane != ChatPane.Home) {
                    InputBlocker()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (tab == FishTab.Me) 1f else 0f)
                    .zIndex(if (tab == FishTab.Me) 2f else 0f),
            ) {
                tabStateHolder.SaveableStateProvider(key = FishTab.Me.name) {
                    ProfileRoute(
                        session = session,
                        active = tab == FishTab.Me,
                        profileUsername = profileUsername,
                        savedAccounts = savedAccounts,
                        chatFilters = chatFilters,
                        themeOptions = themeOptions,
                        themeKey = themeKey,
                        onSaveChatFilters = { saveChatFilters(it) },
                        onThemeChange = onThemeChange,
                        onImportThemePackage = onImportThemePackage,
                        onSaveEditedTheme = onSaveEditedTheme,
                        onDeleteCustomTheme = onDeleteCustomTheme,
                        chatWallpaperUri = chatWallpaperUri,
                        onChatWallpaperChange = onChatWallpaperChange,
                        onOpenArticle = { articleId ->
                            openArticleTabByJump(articleId, returnTo = FishTab.Me)
                        },
                        onCloseProfile = { profileUsername = null },
                        onOpenPrivateChat = { username ->
                            privatePeerJump = username
                            privatePeerJumpRequest += 1
                            profileUsername = null
                            selectTab(FishTab.Chat)
                            chatPane = ChatPane.Home
                            privateChatDetailActive = true
                        },
                        noticeUnread = totalMessageUnread,
                        onOpenNotice = {
                            noticeOpen = true
                        },
                        onCheckUpdate = { checkForUpdates(manual = true) },
                        onSwitchAccount = onSwitchAccount,
                        onAddAccount = onAddAccount,
                        onLogout = onLogout,
                    )
                }
                if (tab != FishTab.Me) {
                    InputBlocker()
                }
            }
            if (noticeOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(8f),
                ) {
                    NoticeScreen(
                        session = session,
                        unread = noticeUnread,
                        onUnreadChange = { noticeUnread = it },
                        onDismiss = { noticeOpen = false },
                        onJumpToChatRoom = {
                            noticeOpen = false
                            openChatRoomDetail()
                        },
                        onJumpToArticle = { articleId ->
                            noticeOpen = false
                            openArticleTabByJump(articleId, returnTo = tab)
                        },
                    )
                }
            }
        }
        if (!bottomNavHiddenByDetail && !bottomNavSuppressed) {
            NativeBottomNav(
                selected = tab,
                onSelect = ::selectTab,
            )
        }
    }
    }

    profileOverlayUsername?.let { overlayUsername ->
        Dialog(
            onDismissRequest = { profileOverlayUsername = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                ProfileRoute(
                    session = session,
                    active = true,
                    profileUsername = overlayUsername,
                    savedAccounts = savedAccounts,
                    chatFilters = chatFilters,
                    themeOptions = themeOptions,
                    themeKey = themeKey,
                    onSaveChatFilters = { saveChatFilters(it) },
                    onThemeChange = onThemeChange,
                    onImportThemePackage = onImportThemePackage,
                    onSaveEditedTheme = onSaveEditedTheme,
                    onDeleteCustomTheme = onDeleteCustomTheme,
                    chatWallpaperUri = chatWallpaperUri,
                    onChatWallpaperChange = onChatWallpaperChange,
                    onOpenArticle = { articleId ->
                        profileOverlayUsername = null
                        openArticleTabByJump(articleId, returnTo = tab)
                    },
                    onCloseProfile = { profileOverlayUsername = null },
                    closeOnBack = true,
                    onOpenPrivateChat = { username ->
                        privatePeerJump = username
                        privatePeerJumpRequest += 1
                        profileOverlayUsername = null
                        selectTab(FishTab.Chat)
                        chatPane = ChatPane.Home
                        privateChatDetailActive = true
                    },
                    noticeUnread = totalMessageUnread,
                    onOpenNotice = {
                        noticeOpen = true
                    },
                    onCheckUpdate = { checkForUpdates(manual = true) },
                    onSwitchAccount = onSwitchAccount,
                    onAddAccount = onAddAccount,
                    onLogout = onLogout,
                )
            }
        }
    }

    PluginUiRoute()

    val pendingUpdate = updateInfo
    if (pendingUpdate != null && !updateDismissed) {
        AlertDialog(
            onDismissRequest = {
                updateDismissed = true
            },
            title = {
                Text(text = "发现新版本 ${pendingUpdate.tagName}")
            },
            text = {
                Text(
                    text = pendingUpdate.changelog.ifBlank { "此版本暂无更新日志。" },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val downloadId = runCatching {
                            val fileName = UpdateDownloader.buildApkFileName(pendingUpdate.tagName)
                            UpdateDownloader.downloadApk(context, pendingUpdate.apkUrl, fileName)
                        }.getOrNull()
                        if (downloadId == null) {
                            FishPiNotifier.error("启动下载失败，请稍后重试")
                            return@TextButton
                        }
                        updateDownloadId = downloadId
                        updateDismissed = true
                        FishPiNotifier.success("已开始下载，完成后将自动安装")
                    },
                ) {
                    Text("立即更新")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        updateDismissed = true
                    },
                ) {
                    Text("稍后")
                }
            },
        )
    }
}

private fun chatConnectionConnected(status: String): Boolean {
    val text = status.trim()
    return text.contains("已连接") ||
        text.contains("连接已恢复") ||
        text.contains("connected", ignoreCase = true) ||
        text.contains("reconnected", ignoreCase = true)
}

private fun chatConnectionLabel(status: String): String =
    if (chatConnectionConnected(status)) "已连接" else "已断开"

@Composable
private fun ChatRoomEntryCard(
    messages: List<ChatRoomMessage>,
    isLoading: Boolean,
    status: String,
    topic: String,
    onlineCount: Int,
    chatFilters: ChatFilterConfig,
    onClick: () -> Unit,
) {
    val latest = messages.asReversed().firstOrNull { !chatFilters.blocksChatMessage(it) }
    val preview = latest?.let { message ->
        val text = when {
            message.revoked -> "消息已撤回"
            message.redPacket != null -> message.redPacket.summary.ifBlank { message.redPacket.typeName.ifBlank { "红包消息" } }
            else -> message.toRenderHints().plainFallback.ifBlank { message.content }
        }
        "${message.authorLabel}: ${text.ifBlank { "新消息" }}"
    } ?: when {
        isLoading -> "正在加载聊天室消息..."
        status.isNotBlank() -> status
        else -> "暂无聊天室消息"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.58f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            AnimalChatGlyph(
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "聊天室",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusSuccessColor()),
                    )
                    Text(text = "$onlineCount 在线", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            Text(
                text = preview,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun InputBlocker() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            },
    )
}

@Composable
private fun NativeAppTopBar(
    title: String,
    connectionLabel: String,
    connectionConnected: Boolean,
    onReconnectChatRoom: () -> Unit,
    noticeUnread: Long,
    onOpenBlocked: () -> Unit,
    onOpenNotice: () -> Unit,
    themeLabel: String,
    onCycleTheme: () -> Unit,
) {
    ControlSurface(
        modifier = Modifier
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .height(56.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AnimalStatusPill(
                    label = connectionLabel,
                    color = if (connectionConnected) statusSuccessColor() else MaterialTheme.colorScheme.error,
                    leadingDot = true,
                    onClick = onReconnectChatRoom,
                )
            }
            AnimalMiniIconButton(
                icon = Icons.Rounded.Palette,
                contentDescription = "主题：$themeLabel",
                onClick = onCycleTheme,
            )
            AnimalMiniIconButton(
                icon = Icons.Rounded.VisibilityOff,
                contentDescription = "查看已屏蔽消息",
                onClick = onOpenBlocked,
            )
            BadgedBox(
                badge = {
                    if (noticeUnread > 0) {
                        Badge(
                            modifier = Modifier.sizeIn(minWidth = 14.dp, minHeight = 14.dp),
                        ) {
                            Text(
                                text = if (noticeUnread > 99) "99+" else noticeUnread.toString(),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                },
            ) {
                AnimalMiniIconButton(
                    icon = Icons.Rounded.Notifications,
                    contentDescription = "通知",
                    onClick = onOpenNotice,
                )
            }
        }
    }
}

@Composable
private fun AnimalMiniIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconActionButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        size = 36.dp,
        iconSize = 17.dp,
    )
}

@Composable
private fun ChatConnectionBadge(
    label: String,
    connected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (connected) statusSuccessColor() else MaterialTheme.colorScheme.error
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun NativeBottomNav(
    selected: FishTab,
    onSelect: (FishTab) -> Unit,
) {
    val tabs = listOf(
        FishTab.Home to Icons.Rounded.Home,
        FishTab.Article to Icons.Rounded.Newspaper,
        FishTab.Chat to Icons.AutoMirrored.Rounded.Chat,
        FishTab.Me to Icons.Rounded.Person,
    )
    Row(
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .height(54.dp)
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { (tab, icon) ->
            LiquidNavItem(
                title = tab.title,
                icon = icon,
                selected = selected == tab,
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LiquidNavItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = title,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
        Box(
            modifier = Modifier
                .size(width = 18.dp, height = 3.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent),
        )
    }
}

@Composable
private fun AiNavItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) MaterialTheme.colorScheme.surface.copy(alpha = 0.72f) else Color.Transparent)
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
                RoundedCornerShape(7.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = title, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text = title, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

private fun AnimalIslandTokensDockHeight() = 52.dp
