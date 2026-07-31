package dev.fishpi.mobile

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.fishpi.mobile.data.ArticleSummary
import dev.fishpi.mobile.data.ChatFilterConfig
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.data.FishPiApiClient
import dev.fishpi.mobile.data.NoticeRealtimeClient
import dev.fishpi.mobile.data.ReleaseUpdateInfo
import dev.fishpi.mobile.data.SavedAccount
import dev.fishpi.mobile.data.SessionStore
import dev.fishpi.mobile.data.UpdateChecker
import dev.fishpi.mobile.data.UpdateDownloader
import dev.fishpi.mobile.auth.VisitorVerificationEvents
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
import dev.fishpi.mobile.ui.animal.AnimalChatGlyph
import dev.fishpi.mobile.ui.animal.AnimalIslandBackground
import dev.fishpi.mobile.ui.components.VisitorVerifyDialog
import dev.fishpi.mobile.ui.motion.FishPiMotion
import dev.fishpi.mobile.ui.components.statusSuccessColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private const val ChatRealtimeGraceMillis = 20_000L

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
    onSaveStoreTheme: (String, Long, String) -> Result<String>,
    storeThemeSaveState: (String, Long, String) -> StoreThemeSaveState,
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

    val navigator = rememberShellNavigator()
    val pagerState = rememberPagerState(
        initialPage = FishTab.Home.ordinal,
        pageCount = { FishTab.entries.size },
    )
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            navigator.selectTab(FishTab.entries[page])
        }
    }
    LaunchedEffect(navigator.selectedTab) {
        val target = navigator.selectedTab.ordinal
        if (pagerState.currentPage != target) {
            pagerState.animateScrollToPage(target)
        }
    }

    var privateChatDetailActive by remember { mutableStateOf(false) }
    var articleDetailActive by remember { mutableStateOf(false) }

    var noticeUnread by remember { mutableStateOf(0L) }
    var privateUnread by remember { mutableStateOf(0L) }
    var openBlockedRequest by remember { mutableStateOf(0) }
    var chatFilters by remember { mutableStateOf(store.getChatFilters()) }
    var appInForeground by remember { mutableStateOf(true) }
    var lastBackPressedAt by remember { mutableStateOf(0L) }
    var privatePeerJump by remember { mutableStateOf<String?>(null) }
    var privatePeerJumpRequest by remember { mutableStateOf(0) }
    var noticeRefreshInFlight by remember { mutableStateOf(false) }
    var lastNoticeRefreshAt by remember { mutableStateOf(0L) }
    var updateInfo by remember { mutableStateOf<ReleaseUpdateInfo?>(null) }
    var updateDismissed by remember { mutableStateOf(false) }
    var updateDownloadId by remember { mutableStateOf<Long?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var showVisitorVerify by remember { mutableStateOf(false) }
    val visitorVerifyVisible by rememberUpdatedState(showVisitorVerify)
    var chatRoomCanFollowBottom by remember { mutableStateOf(true) }
    var chatRoomFollowBottomProbe by remember { mutableStateOf<(() -> Boolean)?>(null) }

    val bottomNavSuppressed = WindowInsets.imeAnimationTarget.getBottom(density) > 0

    val settledTab = FishTab.entries[pagerState.settledPage]
    val onMainLayer = navigator.onMainLayer
    val chatRoomOpen = navigator.overlays.any { it is ShellOverlay.ChatRoom }
    val secondaryBlocking = !onMainLayer || privateChatDetailActive || articleDetailActive
    val bottomNavHidden = secondaryBlocking || bottomNavSuppressed

    var chatRealtimeEnabled by remember { mutableStateOf(chatRoomOpen && appInForeground) }
    LaunchedEffect(chatRoomOpen, appInForeground) {
        when {
            chatRoomOpen && appInForeground -> chatRealtimeEnabled = true
            !appInForeground -> chatRealtimeEnabled = false
            else -> {
                delay(ChatRealtimeGraceMillis)
                chatRealtimeEnabled = false
            }
        }
    }
    val totalMessageUnread = noticeUnread + privateUnread

    fun saveChatFilters(next: ChatFilterConfig) {
        chatFilters = next
        store.saveChatFilters(next)
    }

    fun goToTab(next: FishTab) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        if (next == FishTab.Chat) {
            privateUnread = 0
        }
        navigator.selectTab(next)
    }


    fun openArticleDetail(articleId: String, summary: ArticleSummary? = null) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        navigator.push(ShellOverlay.Article(articleId, summary))
    }

    fun openNotice() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        navigator.push(ShellOverlay.Notice)
    }

    fun openProfileOverlay(username: String) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        navigator.push(ShellOverlay.Profile(username))
    }

    fun openPrivateChat(username: String) {
        privatePeerJump = username
        privatePeerJumpRequest += 1
        navigator.clearOverlays()
        goToTab(FishTab.Chat)
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
                    api.getNoticeUnreadCount(session.apiKey, forceRefresh = force).total
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

    fun refreshAfterVisitorVerification() {
        refreshNoticeUnread(force = true)
        if (chatRoomOpen) {
            refreshChatRoomHistory(force = true)
        }
        FishPiNotifier.success("访客验证已完成")
    }

    fun openChatRoomDetail() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        if (!chatRoomOpen) {
            navigator.push(ShellOverlay.ChatRoom)
        }
        chatController.clearSynthesizedMessages()
        refreshChatRoomHistory(force = true)
    }

    LaunchedEffect(session.apiKey) {
        refreshNoticeUnread(force = true)
    }

    DisposableEffect(session.apiKey) {
        val mainHandler = Handler(Looper.getMainLooper())
        VisitorVerificationEvents.setListener {
            mainHandler.post {
                if (!visitorVerifyVisible) {
                    showVisitorVerify = true
                    FishPiNotifier.show("当前网络需要完成访客验证")
                }
            }
        }
        onDispose {
            VisitorVerificationEvents.setListener(null)
        }
    }

    LaunchedEffect(session.apiKey, settledTab) {
        if (settledTab == FishTab.Chat) {
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
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                    appInForeground = true
                    refreshNoticeUnread(force = true)
                }
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
                onNotice = { refreshNoticeUnread(force = true) },
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
        enabled = chatRealtimeEnabled,
        chatFilters = chatFilters,
        isRoomVisible = { chatRoomOpen },
        shouldFollowBottom = {
            chatController.state.value.shouldFollowBottom
        },
    )

    LaunchedEffect(chatRoomOpen) {
        if (!chatRoomOpen) {
            chatRoomCanFollowBottom = false
        }
    }

    BackHandler {
        if (navigator.back()) {
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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !secondaryBlocking,
                    beyondViewportPageCount = 3, // 4 页全常驻:保滚动位置、realtime、内部详情态不抖。
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    when (FishTab.entries[page]) {
                        FishTab.Home -> HomeRoute(
                            session = session,
                            noticeUnread = totalMessageUnread,
                            onOpenChat = { openChatRoomDetail() },
                            onOpenArticle = { goToTab(FishTab.Article) },
                            onOpenArticleDetail = { articleId -> openArticleDetail(articleId) },
                            onOpenBreezemoon = { navigator.push(ShellOverlay.HomePane(HomeSubPane.Breezemoon)) },
                            onOpenStore = { navigator.push(ShellOverlay.HomePane(HomeSubPane.Store)) },
                            onOpenProfile = { goToTab(FishTab.Me) },
                        )

                        FishTab.Article -> Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                            ArticleRoute(
                                session = session,
                                active = settledTab == FishTab.Article,
                                onDetailActiveChange = { articleDetailActive = it },
                                onOpenUserProfile = { username -> openProfileOverlay(username) },
                            )
                        }

                        FishTab.Chat -> Box(modifier = Modifier.fillMaxSize()) {
                            PrivateChatRoute(
                                session = session,
                                realtimeEnabled = appInForeground,
                                active = settledTab == FishTab.Chat,
                                jumpPeer = privatePeerJump,
                                jumpRequest = privatePeerJumpRequest,
                                onJumpHandled = { privatePeerJump = null },
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
                                        ChatRoomEntryCardHost(
                                            chatController = chatController,
                                            chatFilters = chatFilters,
                                            onClick = { openChatRoomDetail() },
                                        )
                                    }
                                },
                            )
                        }

                        FishTab.Me -> Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                            ProfileRoute(
                                session = session,
                                active = settledTab == FishTab.Me,
                                profileUsername = null,
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
                                onOpenArticle = { articleId, summary ->
                                    openArticleDetail(articleId, summary = summary)
                                },
                                onCloseProfile = { },
                                onOpenPrivateChat = { username -> openPrivateChat(username) },
                                noticeUnread = noticeUnread,
                                onOpenNotice = { openNotice() },
                                onCheckUpdate = { checkForUpdates(manual = true) },
                                onSwitchAccount = onSwitchAccount,
                                onAddAccount = onAddAccount,
                                onLogout = onLogout,
                            )
                        }
                    }
                }

                AnimatedContent(
                    targetState = navigator.topOverlay,
                    transitionSpec = {
                        FishPiMotion.pushTransform(targetState != null)
                            .using(SizeTransform(clip = false))
                    },
                    label = "shellOverlay",
                ) { overlay ->
                    when (overlay) {
                        null -> Box(modifier = Modifier.fillMaxSize())

                        ShellOverlay.ChatRoom -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .pointerInput(Unit) {},
                        ) {
                            ChatRoute(
                                chatFilters = chatFilters,
                                openBlockedRequest = openBlockedRequest,
                                active = true,
                                chatController = chatController,
                                themeLabel = themeOptions.firstOrNull { it.key == themeKey }?.label ?: themeKey,
                                noticeUnread = noticeUnread,
                                onCycleTheme = onCycleTheme,
                                onOpenNotice = { openNotice() },
                                onFollowBottomChanged = { chatRoomCanFollowBottom = it },
                                onFollowBottomProbeChanged = { chatRoomFollowBottomProbe = it },
                                onBlockedRequestHandled = { openBlockedRequest = 0 },
                                onOpenUserProfile = { username -> openProfileOverlay(username) },
                                onBack = { navigator.back() },
                            )
                        }

                        ShellOverlay.Notice -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .pointerInput(Unit) {},
                        ) {
                            NoticeScreen(
                                session = session,
                                unread = noticeUnread,
                                onUnreadChange = { noticeUnread = it },
                                onDismiss = { navigator.back() },
                                onJumpToChatRoom = {
                                    navigator.back()
                                    openChatRoomDetail()
                                },
                                onJumpToArticle = { articleId ->
                                    openArticleDetail(articleId)
                                },
                            )
                        }

                        is ShellOverlay.Profile -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .pointerInput(Unit) {},
                        ) {
                            ProfileRoute(
                                session = session,
                                active = true,
                                profileUsername = overlay.username,
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
                                onOpenArticle = { articleId, summary ->
                                    openArticleDetail(articleId, summary = summary)
                                },
                                onCloseProfile = { navigator.back() },
                                closeOnBack = true,
                                onOpenPrivateChat = { username -> openPrivateChat(username) },
                                noticeUnread = noticeUnread,
                                onOpenNotice = { openNotice() },
                                onCheckUpdate = { checkForUpdates(manual = true) },
                                onSwitchAccount = onSwitchAccount,
                                onAddAccount = onAddAccount,
                                onLogout = onLogout,
                            )
                        }

                        is ShellOverlay.HomePane -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .pointerInput(Unit) {},
                        ) {
                            when (overlay.pane) {
                                HomeSubPane.Breezemoon -> BreezemoonRoute(
                                    session = session,
                                    active = true,
                                )
                                HomeSubPane.Store -> ExtensionStoreRoute(
                                    apiKey = session.apiKey,
                                    onImportTheme = onSaveStoreTheme,
                                    themeSaveState = storeThemeSaveState,
                                )
                            }
                        }

                        is ShellOverlay.Article -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .background(MaterialTheme.colorScheme.background)
                                .pointerInput(Unit) {},
                        ) {
                            ArticleRoute(
                                session = session,
                                active = true,
                                jumpArticleId = overlay.articleId,
                                jumpSummary = overlay.summary,
                                jumpRequest = 1,
                                onDetailClosed = { navigator.back() },
                                onOpenUserProfile = { username -> openProfileOverlay(username) },
                            )
                        }
                    }
                }
            }
            if (!bottomNavHidden) {
                NativeBottomNav(
                    selected = navigator.selectedTab,
                    onSelect = { goToTab(it) },
                )
            }
        }
    }

    PluginUiRoute()

    if (showVisitorVerify) {
        VisitorVerifyDialog(
            apiKey = session.apiKey,
            onVerified = {
                showVisitorVerify = false
                refreshAfterVisitorVerification()
            },
            onDismiss = {
                showVisitorVerify = false
            },
        )
    }

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

@Composable
private fun ChatRoomEntryCardHost(
    chatController: ChatController,
    chatFilters: ChatFilterConfig,
    onClick: () -> Unit,
) {
    val state by chatController.state.collectAsState()
    val messages by chatController.legacyMessages.collectAsState()
    ChatRoomEntryCard(
        messages = messages,
        isLoading = state.isLoading,
        status = state.connection.label,
        onlineCount = state.connection.onlineCount.toInt(),
        chatFilters = chatFilters,
        onClick = onClick,
    )
}

@Composable
private fun ChatRoomEntryCard(
    messages: List<ChatRoomMessage>,
    isLoading: Boolean,
    status: String,
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
            .navigationBarsPadding()
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
