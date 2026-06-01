package dev.fishpi.mobile

import dev.fishpi.mobile.ui.components.LoadingScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import dev.fishpi.mobile.data.FishPiApiClient
import dev.fishpi.mobile.data.SavedAccount
import dev.fishpi.mobile.data.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FishPiApp() {
    val context = LocalContext.current
    val store = remember { SessionStore(context.applicationContext) }
    val fallbackTheme = FishPiThemePreset.DeepBlueNeon
    val builtinOptions = remember { builtinThemeOptions() }
    var importedThemes by remember {
        mutableStateOf(store.getImportedThemeJsons().mapNotNull { raw ->
            runCatching { parseCustomFishPiTheme(raw) }.getOrNull()
        })
    }
    var chatWallpaperUri by remember { mutableStateOf(store.getChatWallpaperUri()) }
    val themeOptions = remember(builtinOptions, importedThemes) {
        buildThemeOptions(builtinOptions, importedThemes)
    }
    var themeKey by remember {
        val savedThemeKey = store.getThemePresetKey()
        mutableStateOf(
            when {
                savedThemeKey.isBlank() -> fallbackTheme.key
                themeOptions.none { it.key == savedThemeKey } -> fallbackTheme.key
                else -> savedThemeKey
            },
        )
    }
    val activeTheme = themeOptions.firstOrNull { it.key == themeKey }
        ?: themeOptions.firstOrNull { it.key == fallbackTheme.key }
        ?: builtinOptions.first()
    fun applyThemeKey(nextKey: String) {
        themeKey = nextKey
        store.saveThemePresetKey(nextKey)
    }
    fun importTheme(rawJson: String): Result<String> =
        runCatching {
            val custom = parseCustomFishPiTheme(rawJson)
            val nextThemes = (importedThemes.filterNot { it.key == custom.key } + custom)
            importedThemes = nextThemes
            store.saveImportedThemeJsons(nextThemes.map { it.rawJson })
            applyThemeKey(custom.key)
            custom.label
        }
    fun saveThemeOnly(rawJson: String): Result<String> =
        runCatching {
            val custom = parseCustomFishPiTheme(rawJson)
            val nextThemes = (importedThemes.filterNot { it.key == custom.key } + custom)
            importedThemes = nextThemes
            store.saveImportedThemeJsons(nextThemes.map { it.rawJson })
            custom.label
        }
    fun storeThemeSaveState(identifier: String, itemId: Long, rawJson: String): StoreThemeSaveState =
        runCatching {
            val storeRaw = buildStoreThemeJson(rawJson, identifier, itemId)
            val custom = parseCustomFishPiTheme(storeRaw)
            val existing = importedThemes.firstOrNull { it.key == custom.key }
            when {
                existing?.rawJson == storeRaw -> StoreThemeSaveState.SavedSameContent
                existing != null -> StoreThemeSaveState.SavedDifferentContent
                importedThemes.any { it.rawJson == rawJson } -> StoreThemeSaveState.SavedDifferentContent
                else -> StoreThemeSaveState.NotSaved
            }
        }.getOrDefault(StoreThemeSaveState.NotSaved)

    fun saveStoreTheme(identifier: String, itemId: Long, rawJson: String): Result<String> =
        runCatching {
            val storeRaw = buildStoreThemeJson(rawJson, identifier, itemId)
            val custom = parseCustomFishPiTheme(storeRaw)
            val nextThemes = (importedThemes.filterNot { it.key == custom.key || it.rawJson == rawJson } + custom)
            importedThemes = nextThemes
            store.saveImportedThemeJsons(nextThemes.map { it.rawJson })
            custom.label
        }

    suspend fun importThemePackage(uri: String): Result<String> =
        runCatching {
            val custom = withContext(Dispatchers.IO) {
                importFishPiThemePackage(context.applicationContext, uri)
            }
            val nextThemes = (importedThemes.filterNot { it.key == custom.key } + custom)
            importedThemes = nextThemes
            store.saveImportedThemeJsons(nextThemes.map { it.rawJson })
            applyThemeKey(custom.key)
            custom.label
        }
    fun deleteCustomTheme(key: String): Boolean {
        val existing = importedThemes.firstOrNull { it.key == key } ?: return false
        val nextThemes = importedThemes.filterNot { it.key == existing.key }
        deleteFishPiThemePackageFiles(context.applicationContext, existing.key)
        importedThemes = nextThemes
        store.saveImportedThemeJsons(nextThemes.map { it.rawJson })
        if (themeKey == existing.key) {
            applyThemeKey(fallbackTheme.key)
        }
        return true
    }
    val activeWallpaperUri = chatWallpaperUri.ifBlank { activeTheme.palette.wallpaperImageUri.orEmpty() }
    val palette = activeTheme.tokens.toPalette(activeWallpaperUri.ifBlank { null })
    fun applyChatWallpaper(uri: String) {
        chatWallpaperUri = uri
        store.saveChatWallpaperUri(uri)
    }
    LaunchedEffect(themeOptions, themeKey, fallbackTheme) {
        val resolvedKey = themeOptions.firstOrNull { it.key == themeKey }?.key
            ?: themeOptions.firstOrNull { it.key == fallbackTheme.key }?.key
            ?: builtinOptions.first().key
        if (resolvedKey != themeKey) {
            applyThemeKey(resolvedKey)
        }
    }
    FishPiM3BridgedTheme(palette = palette, tokens = activeTheme.tokens, uiStyle = activeTheme.uiStyle) {
        val api = remember { FishPiApiClient.shared }
        val scope = rememberCoroutineScope()
        var session by remember { mutableStateOf<AppSession?>(null) }
        var savedAccounts by remember { mutableStateOf(store.getAccounts()) }
        var isBooting by remember { mutableStateOf(true) }
        var bootError by remember { mutableStateOf<String?>(null) }

        fun refreshSavedAccounts() {
            savedAccounts = store.getAccounts()
        }

        fun switchAccount(account: SavedAccount) {
            if (account.apiKey == session?.apiKey) return
            isBooting = true
            bootError = null
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        val user = api.getUser(account.apiKey)
                        AppSession(account.apiKey, user)
                    }
                }.onSuccess {
                    store.saveAccount(it.apiKey, it.user)
                    refreshSavedAccounts()
                    session = it
                }.onFailure {
                    bootError = it.message ?: "切换账号失败"
                }
                isBooting = false
            }
        }

        LaunchedEffect(Unit) {
            val savedApiKey = store.getApiKey()
            if (savedApiKey.isBlank()) {
                isBooting = false
                return@LaunchedEffect
            }

            runCatching {
                withContext(Dispatchers.IO) {
                    AppSession(savedApiKey, api.getUser(savedApiKey))
                }
            }.onSuccess {
                session = it
                store.saveAccount(it.apiKey, it.user)
                refreshSavedAccounts()
            }.onFailure {
                bootError = it.message ?: "Token 验证失败"
            }
            isBooting = false
        }

        when {
            isBooting -> LoadingScreen("正在登录")
            session == null -> LoginScreen(
                initialError = bootError,
                savedAccounts = savedAccounts,
                onSwitchAccount = { account -> switchAccount(account) },
                onAuthenticated = {
                    store.saveAccount(it.apiKey, it.user)
                    refreshSavedAccounts()
                    session = it
                    bootError = null
                },
            )
            else -> CompositionLocalProvider(LocalAppSession provides session!!) {
                MainShell(
                    session = session!!,
                    savedAccounts = savedAccounts,
                    themeOptions = themeOptions,
                    themeKey = themeKey,
                    onCycleTheme = {
                        val currentIndex = themeOptions.indexOfFirst { it.key == themeKey }.coerceAtLeast(0)
                        val next = themeOptions[(currentIndex + 1) % themeOptions.size]
                        applyThemeKey(next.key)
                    },
                    onThemeChange = { applyThemeKey(it) },
                    onImportThemePackage = { importThemePackage(it) },
                    onSaveEditedTheme = { importTheme(it) },
                    onSaveStoreTheme = { identifier, itemId, raw -> saveStoreTheme(identifier, itemId, raw) },
                    storeThemeSaveState = { identifier, itemId, raw -> storeThemeSaveState(identifier, itemId, raw) },
                    onDeleteCustomTheme = { deleteCustomTheme(it) },
                    chatWallpaperUri = chatWallpaperUri,
                    onChatWallpaperChange = { applyChatWallpaper(it) },
                    onSwitchAccount = { account -> switchAccount(account) },
                    onAddAccount = {
                        session = null
                        bootError = null
                    },
                    onLogout = {
                        store.clear(session!!.apiKey)
                        refreshSavedAccounts()
                        session = null
                    },
                )
            }
        }
        }
    }


