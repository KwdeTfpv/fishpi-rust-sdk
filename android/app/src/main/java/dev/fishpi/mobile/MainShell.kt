package dev.fishpi.mobile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import java.util.Locale
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import dev.fishpi.mobile.data.ArticleSummary
import dev.fishpi.mobile.data.ChatFilterConfig
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.data.FishPiApiClient
import dev.fishpi.mobile.data.PrivateChatNotice
import dev.fishpi.mobile.realtime.PendingRealtimeNavigation
import dev.fishpi.mobile.realtime.RealtimeConnectionManager
import dev.fishpi.mobile.realtime.RealtimeLifecycleController
import dev.fishpi.mobile.realtime.RealtimeNavTarget
import dev.fishpi.mobile.realtime.RealtimeSession
import dev.fishpi.mobile.data.ReleaseUpdateInfo
import dev.fishpi.mobile.data.SavedAccount
import dev.fishpi.mobile.data.SessionStore
import dev.fishpi.mobile.data.UpdateChecker
import dev.fishpi.mobile.data.UpdateDownloader
import dev.fishpi.mobile.data.UpdateInstaller
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
import dev.fishpi.mobile.ui.FloatingBottomBarClearance
import dev.fishpi.mobile.ui.LocalBottomBarClearance
import dev.fishpi.mobile.ui.animal.AnimalIslandBackground
import dev.fishpi.mobile.ui.components.VisitorVerifyDialog
import dev.fishpi.mobile.ui.components.uiPageBrush
import dev.fishpi.mobile.ui.motion.FishPiMotion
import dev.fishpi.mobile.ui.components.statusSuccessColor
import dev.fishpi.mobile.ui.glass.LiquidDragAnimation
import dev.fishpi.mobile.ui.glass.LiquidInteractiveHighlight
import dev.fishpi.mobile.ui.glass.FishPiGlassRole
import dev.fishpi.mobile.ui.glass.LocalFishPiGlassBackdrop
import dev.fishpi.mobile.ui.glass.fishPiGlassSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private const val UpdateCheckIntervalMs = 24L * 60L * 60L * 1000L

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
    glassEffectsEnabled: Boolean,
    onGlassEffectsEnabledChange: (Boolean) -> Unit,
    onSwitchAccount: (SavedAccount) -> Unit,
    onAddAccount: () -> Unit,
    onLogout: () -> Unit,
) {
    val api = remember { FishPiApiClient.shared }
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

    DisposableEffect(chatController) {
        onDispose { chatController.close() }
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
    var pendingPrivateNotice by remember { mutableStateOf<PrivateChatNotice?>(null) }
    var pendingPrivateNoticeVersion by remember { mutableStateOf(0) }
    var noticeRefreshInFlight by remember { mutableStateOf(false) }
    var lastNoticeRefreshAt by remember { mutableStateOf(0L) }
    var updateInfo by remember { mutableStateOf<ReleaseUpdateInfo?>(null) }
    var updateDismissed by remember { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var updateDownloadedBytes by remember { mutableStateOf(0L) }
    var updateTotalBytes by remember { mutableStateOf(-1L) }
    var updateDownloadError by remember { mutableStateOf<String?>(null) }
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

    val totalMessageUnread = noticeUnread + privateUnread

    fun saveChatFilters(next: ChatFilterConfig) {
        chatFilters = next
        store.saveChatFilters(next)
    }

    fun goToTab(next: FishTab) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
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

    fun openChatRoomDetail(targetOId: String? = null) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        if (!chatRoomOpen) {
            navigator.push(ShellOverlay.ChatRoom)
        }
        chatController.clearSynthesizedMessages()
        val locate = targetOId?.takeIf { it.isNotBlank() }
        if (locate != null) {
            chatController.refreshHistory(
                skipIfLoaded = false,
                onSuccess = { chatController.locateMessage(locate) },
            )
        } else {
            refreshChatRoomHistory(force = true)
        }
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
        if (!manual) {
            val lastCheckedAt = store.getUpdateCheckAt()
            if (lastCheckedAt > 0 && System.currentTimeMillis() - lastCheckedAt < UpdateCheckIntervalMs) {
                return
            }
        }
        isCheckingUpdate = true
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { UpdateChecker.checkForUpdate(store.getUpdateChannel()) }
            }.onSuccess { result ->
                val info = result.updateInfo
                val checkedAt = System.currentTimeMillis()
                store.saveUpdateCheckAt(checkedAt)
                if (info != null) {
                    store.saveUpdateCheckResult("发现新版本 ${info.tagName}")
                    updateInfo = info
                    updateDismissed = false
                    if (manual) FishPiNotifier.success("发现新版本 ${info.tagName}")
                } else {
                    val latest = result.latestTagName.ifBlank { BuildConfig.VERSION_NAME }
                    store.saveUpdateCheckResult("已是最新版本：$latest")
                    if (manual) FishPiNotifier.show("当前已是最新版本：$latest")
                }
            }.onFailure {
                store.saveUpdateCheckAt(System.currentTimeMillis())
                store.saveUpdateCheckResult("检查失败：${it.message ?: "网络异常"}")
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

    LaunchedEffect(appInForeground, chatController) {
        chatController.setPollingActive(appInForeground)
    }

    // 登录态交给进程级连接管理器；连接与前台服务由 RealtimeLifecycleController 托管
    DisposableEffect(session.apiKey, session.user.userName) {
        RealtimeLifecycleController.onSession(
            RealtimeSession(session.apiKey, session.user.userName),
        )
        onDispose { }
    }

    // 消费来自系统通知点击的待处理跳转
    LaunchedEffect(Unit) {
        PendingRealtimeNavigation.target.collect { target ->
            if (target == null) return@collect
            when (target) {
                is RealtimeNavTarget.ChatRoom -> openChatRoomDetail(target.messageId)
                is RealtimeNavTarget.Article -> openArticleDetail(target.articleId)
                is RealtimeNavTarget.PrivateChat -> openPrivateChat(target.peer)
                RealtimeNavTarget.Notice -> openNotice()
            }
            PendingRealtimeNavigation.consume()
        }
    }

    LaunchedEffect(session.apiKey) {
        RealtimeConnectionManager.noticeEvents.collect {
            refreshNoticeUnread(force = true)
        }
    }

    LaunchedEffect(session.apiKey) {
        RealtimeConnectionManager.privateNotices.collect {
            privateUnread += 1
            pendingPrivateNotice = it
            pendingPrivateNoticeVersion += 1
        }
    }

    // 通知分发器需知道聊天室是否可见，避免在聊天室内重复弹通知
    LaunchedEffect(chatRoomOpen, appInForeground) {
        (context.applicationContext as? FishPiApplication)?.notificationDispatcher?.let {
            it.chatRoomVisible = chatRoomOpen
            it.appInForeground = appInForeground
        }
    }

    // 始终订阅聊天室事件用于 UI 渲染；连接生命周期与 UI 解耦
    ChatRealtimeRouteLifecycle(
        chatController = chatController,
        enabled = true,
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

    val shellBackdropBrush = uiPageBrush()
    val shellBackdrop = rememberLayerBackdrop {
        drawRect(brush = shellBackdropBrush)
        drawContent()
    }
    val bottomBarBackdropBrush = uiPageBrush()
    val bottomBarBackdrop = rememberLayerBackdrop {
        drawRect(brush = bottomBarBackdropBrush)
        drawContent()
    }

    FishPiOverlayWindow {
        CompositionLocalProvider(
            LocalFishPiGlassBackdrop provides shellBackdrop,
            LocalBottomBarClearance provides if (bottomNavHidden) 0.dp else FloatingBottomBarClearance,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AnimalIslandBackground(
                    modifier = Modifier.layerBackdrop(shellBackdrop),
                ) {}
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(bottomBarBackdrop),
                ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
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
                                externalNotice = pendingPrivateNotice,
                                externalNoticeVersion = pendingPrivateNoticeVersion,
                                onNoticeHandled = { pendingPrivateNotice = null },
                                onJumpHandled = { privatePeerJump = null },
                                onOpenUserProfile = { username -> openProfileOverlay(username) },
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
                                glassEffectsEnabled = glassEffectsEnabled,
                                onGlassEffectsEnabledChange = onGlassEffectsEnabledChange,
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
                    targetState = navigator.topOverlay to navigator.overlays.size,
                    transitionSpec = {
                        val entering = targetState.second > initialState.second
                        FishPiMotion.pushTransform(entering)
                            .using(SizeTransform(clip = false))
                    },
                    contentKey = { it.first },
                    label = "shellOverlay",
                ) { (overlay, _) ->
                    when (overlay) {
                        null -> Box(modifier = Modifier.fillMaxSize())

                        ShellOverlay.ChatRoom -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(uiPageBrush())
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
                                .background(uiPageBrush())
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
                                .background(uiPageBrush())
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
                                glassEffectsEnabled = glassEffectsEnabled,
                                onGlassEffectsEnabledChange = onGlassEffectsEnabledChange,
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
                                .background(uiPageBrush())
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
                                .background(uiPageBrush())
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
                }
            if (!bottomNavHidden) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    NativeBottomNav(
                        selected = navigator.selectedTab,
                        onSelect = { goToTab(it) },
                        backdrop = bottomBarBackdrop,
                    )
                }
            }
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
                if (!isDownloadingUpdate) updateDismissed = true
            },
            containerColor = FishPiTheme.surface,
            tonalElevation = 0.dp,
            title = {
                Text(text = "发现新版本 ${pendingUpdate.tagName}")
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (isDownloadingUpdate) {
                        Text(
                            text = "正在下载 ${formatBytes(updateDownloadedBytes)}" +
                                if (updateTotalBytes > 0) " / ${formatBytes(updateTotalBytes)}" else "",
                        )
                        if (updateTotalBytes > 0) {
                            LinearProgressIndicator(
                                progress = {
                                    (updateDownloadedBytes.toFloat() / updateTotalBytes.toFloat()).coerceIn(0f, 1f)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                            )
                        }
                    }
                    updateDownloadError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    Text(
                        text = pendingUpdate.changelog.ifBlank { "此版本暂无更新日志。" },
                        modifier = Modifier.padding(top = if (isDownloadingUpdate || updateDownloadError != null) 8.dp else 0.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isDownloadingUpdate,
                    onClick = {
                        if (isDownloadingUpdate) return@TextButton
                        if (!context.packageManager.canRequestPackageInstalls()) {
                            store.saveUpdateDownloadAt(System.currentTimeMillis())
                            store.saveUpdateDownloadResult("未允许安装未知应用")
                            FishPiNotifier.error("请先允许安装未知应用")
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                        Uri.parse("package:${context.packageName}"),
                                    ),
                                )
                            }
                            return@TextButton
                        }
                        isDownloadingUpdate = true
                        updateDownloadError = null
                        updateDownloadedBytes = 0L
                        updateTotalBytes = -1L
                        scope.launch {
                            val downloaded = runCatching {
                                withContext(Dispatchers.IO) {
                                    UpdateDownloader.downloadApk(
                                        context = context,
                                        urls = pendingUpdate.urls,
                                        expectedSha256 = pendingUpdate.sha256,
                                        onProgress = { downloadedBytes, totalBytes ->
                                            scope.launch(Dispatchers.Main.immediate) {
                                                updateDownloadedBytes = downloadedBytes
                                                updateTotalBytes = totalBytes
                                            }
                                        },
                                    )
                                }
                            }
                            downloaded.onSuccess { apk ->
                                updateDismissed = true
                                store.saveUpdateDownloadAt(System.currentTimeMillis())
                                val started = runCatching {
                                    withContext(Dispatchers.IO) { UpdateInstaller.install(context, apk) }
                                }.getOrDefault(false)
                                if (!started) {
                                    store.saveUpdateDownloadResult("安装启动失败")
                                    FishPiNotifier.error("安装启动失败，请手动安装")
                                } else {
                                    store.saveUpdateDownloadResult("已拉起安装器")
                                }
                            }.onFailure {
                                updateDownloadError = it.message ?: "下载失败"
                                store.saveUpdateDownloadAt(System.currentTimeMillis())
                                store.saveUpdateDownloadResult("下载失败：${it.message ?: "未知错误"}")
                                FishPiNotifier.error(it.message ?: "下载失败")
                            }
                            isDownloadingUpdate = false
                        }
                    },
                ) {
                    Text(
                        when {
                            isDownloadingUpdate -> "下载中..."
                            updateDownloadError != null -> "重试"
                            else -> "立即更新"
                        },
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDownloadingUpdate,
                    onClick = {
                        updateDismissed = true
                    },
                ) {
                    Text(if (isDownloadingUpdate) "下载中" else "稍后")
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
            .fishPiGlassSurface(
                backdrop = LocalFishPiGlassBackdrop.current,
                shape = RoundedCornerShape(16.dp),
                containerColor = FishPiTheme.surface.copy(alpha = FishPiTheme.glass.surfaceAlpha),
                borderColor = FishPiTheme.outline.copy(alpha = 0.12f),
                borderWidth = 1.dp,
                role = FishPiGlassRole.Content,
            )
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
    backdrop: LayerBackdrop,
) {
    val tabs = listOf(
        FishTab.Home to Icons.Rounded.Home,
        FishTab.Article to Icons.Rounded.Newspaper,
        FishTab.Chat to Icons.AutoMirrored.Rounded.Chat,
        FishTab.Me to Icons.Rounded.Person,
    )
    val selectedIndex = tabs.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    Box(
        modifier = Modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        LiquidBottomNavTrack(
            selectedTabIndex = { selectedIndex },
            onTabSelected = { index -> onSelect(tabs[index].first) },
            tabsCount = tabs.size,
            backdrop = backdrop,
            modifier = Modifier
                .height(52.dp)
                .fillMaxWidth(),
        ) { scale ->
            tabs.forEachIndexed { index, (tab, icon) ->
                LiquidNavItem(
                    title = tab.title,
                    icon = icon,
                    selected = selectedIndex == index,
                    onClick = { onSelect(tab) },
                    scale = scale,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LiquidBottomNavTrack(
    selectedTabIndex: () -> Int,
    onTabSelected: (Int) -> Unit,
    tabsCount: Int,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.(scale: () -> Float) -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart,
    ) {
        val density = LocalDensity.current
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8.dp.toPx()) / tabsCount
        }
        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) {
                    4.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var currentIndex by remember(selectedTabIndex) {
            mutableIntStateOf(selectedTabIndex())
        }
        val dragAnimation = remember(animationScope, tabsCount, tabWidth, isLtr) {
            LiquidDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTabIndex().toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 1.055f,
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    currentIndex = targetIndex
                    animateToValue(targetIndex.toFloat())
                    animationScope.launch {
                        offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                    }
                },
                onDrag = { _, dragAmount ->
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat()),
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                },
            )
        }
        LaunchedEffect(selectedTabIndex) {
            snapshotFlow { selectedTabIndex() }
                .collectLatest { index ->
                    currentIndex = index
                    dragAnimation.animateToValue(index.toFloat())
                }
        }
        LaunchedEffect(dragAnimation) {
            snapshotFlow { currentIndex }
                .drop(1)
                .collectLatest { index ->
                    dragAnimation.animateToValue(index.toFloat())
                    onTabSelected(index)
                }
        }
        val interactiveHighlight = remember(animationScope, isLtr, tabWidth) {
            LiquidInteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    Offset(
                        x = if (isLtr) {
                            (dragAnimation.value + 0.5f) * tabWidth + panelOffset
                        } else {
                            size.width - (dragAnimation.value + 0.5f) * tabWidth + panelOffset
                        },
                        y = size.height / 2f,
                    )
                },
            )
        }
        val bottomBarSurfaceColor = if (!FishPiTheme.glassEnabled) {
            FishPiTheme.surface
        } else if (FishPiTheme.background.luminance() < 0.5f) {
            Color.Black.copy(alpha = FishPiTheme.glass.scrimAlpha)
        } else {
            Color.White.copy(alpha = FishPiTheme.glass.scrimAlpha)
        }
        val bottomBarBlurRadius = FishPiTheme.glass.blurRadius.dp
        val bottomBarRefractionHeight = FishPiTheme.glass.refractionHeight.dp
        val bottomBarRefractionAmount = FishPiTheme.glass.refractionAmount.dp
        val selectedTabTint = MaterialTheme.colorScheme.primary
        val selectedTabSurface = if (FishPiTheme.background.luminance() < 0.5f) {
            Color.White.copy(alpha = 0.16f)
        } else {
            Color.White.copy(alpha = 0.34f)
        }
        val selectedTabTintAlpha = if (FishPiTheme.background.luminance() < 0.5f) 0.24f else 0.18f
        val selectedTabBorder = if (FishPiTheme.background.luminance() < 0.5f) {
            Color.White.copy(alpha = 0.22f)
        } else {
            selectedTabTint.copy(alpha = 0.22f)
        }

        Box(
            modifier = Modifier
                .graphicsLayer { translationX = panelOffset }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(bottomBarBlurRadius.toPx())
                        lens(
                            bottomBarRefractionHeight.toPx(),
                            bottomBarRefractionAmount.toPx(),
                            chromaticAberration = false,
                        )
                    },
                    layerBlock = {
                        val progress = dragAnimation.pressProgress
                        val scale = lerp(1f, 1.01f, progress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = {
                        drawRect(bottomBarSurfaceColor)
                    },
                )
                .then(interactiveHighlight.modifier)
                .height(52.dp)
                .fillMaxWidth(),
        )

        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .graphicsLayer {
                    translationX = if (isLtr) {
                        dragAnimation.value * tabWidth + panelOffset
                    } else {
                        size.width - (dragAnimation.value + 1f) * tabWidth + panelOffset
                    }
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(bottomBarBlurRadius.toPx())
                        lens(
                            (bottomBarRefractionHeight * 0.92f).toPx(),
                            (bottomBarRefractionAmount * 0.82f).toPx(),
                            chromaticAberration = false,
                        )
                    },
                    layerBlock = {
                        val progress = dragAnimation.pressProgress
                        val scale = lerp(1f, 1.025f, progress)
                        scaleX = scale
                        scaleY = lerp(1f, 0.985f, progress)
                    },
                    onDrawSurface = {
                        drawRect(selectedTabTint, blendMode = BlendMode.Hue)
                        drawRect(selectedTabTint.copy(alpha = selectedTabTintAlpha))
                        drawRect(selectedTabSurface)
                    },
                )
                .border(FishPiTheme.borderWidth, selectedTabBorder, Capsule())
                .then(interactiveHighlight.gestureModifier)
                .then(dragAnimation.modifier)
                .graphicsLayer {
                    scaleX = dragAnimation.scaleX
                    scaleY = dragAnimation.scaleY
                    val velocity = dragAnimation.velocity / 18f
                    scaleX /= 1f - (velocity * 0.18f).fastCoerceIn(-0.035f, 0.035f)
                    scaleY *= 1f - (velocity * 0.08f).fastCoerceIn(-0.025f, 0.025f)
                }
                .height(44.dp)
                .fillMaxWidth(1f / tabsCount),
        )

        Row(
            modifier = Modifier
                .graphicsLayer { translationX = panelOffset }
                .then(interactiveHighlight.modifier)
                .height(52.dp)
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content { 1f }
        }
    }
}

@Composable
private fun LiquidNavItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    scale: () -> Float,
    modifier: Modifier = Modifier,
    ) {
    val selectedContentColor = MaterialTheme.colorScheme.onSurface
    val unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
    val contentColor = if (selected) selectedContentColor else unselectedContentColor
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(Capsule())
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .graphicsLayer {
                val targetScale = scale()
            scaleX = targetScale
            scaleY = targetScale
            }
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = title,
            color = contentColor,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 2.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.72f) else Color.Transparent),
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    return String.format(Locale.US, "%.1f MB", kb / 1024.0)
}
