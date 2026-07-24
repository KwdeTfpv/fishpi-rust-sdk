package dev.fishpi.mobile.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

data class SavedAccount(
    val apiKey: String,
    val userName: String,
    val displayName: String,
    val avatarUrl: String,
)

data class ChatFilterConfig(
    val blockedUsers: List<String> = DefaultBlockedUsers,
    val blockedKeywords: List<String> = emptyList(),
    val blockedPrefixKeywords: List<String> = DefaultBlockedPrefixKeywords,
    val blockedRegex: List<String> = emptyList(),
    val showAvatars: Boolean = true,
    val sendOnEnter: Boolean = true,
) {
    private val compiledBlockedRegex: List<Regex> by lazy {
        blockedRegex.mapNotNull { source ->
            val pattern = source.trim()
            if (pattern.isBlank()) {
                null
            } else {
                runCatching { Regex(pattern) }.getOrNull()
            }
        }
    }

    fun matchesBlockedRegex(content: String): Boolean =

        compiledBlockedRegex.any { regex ->
            runCatching { regex.containsMatchIn(content) }.getOrDefault(false)
        }

    companion object {
        val DefaultBlockedUsers = listOf("xiaoIce", "sevenSummer", "b", "xds")
        val DefaultBlockedPrefixKeywords = listOf("冰冰", "鸽", "~", "小斗士")
    }
}

data class HomeWorkSettings(
    val startTime: String = "09:00",
    val endTime: String = "18:00",
    val weekendMode: String = WEEKEND_DOUBLE,
    val customRestDays: Set<Int> = setOf(7),
) {
    fun restDays(): Set<Int> = when (weekendMode) {
        WEEKEND_SINGLE -> setOf(7)
        WEEKEND_CUSTOM -> customRestDays.ifEmpty { setOf(7) }
        else -> setOf(6, 7)
    }

    companion object {
        const val WEEKEND_DOUBLE = "double"
        const val WEEKEND_SINGLE = "single"
        const val WEEKEND_CUSTOM = "custom"
    }
}

data class CachedUserActivity(
    val activity: UserActivityView,
    val date: String,
    val fetchedAt: Long,
    val attemptedAt: Long = fetchedAt,
) {
    fun isToday(): Boolean = date == todayKey()

    fun isFresh(now: Long = System.currentTimeMillis()): Boolean =
        isToday() && now - fetchedAt < ACTIVITY_REFRESH_INTERVAL_MS

    fun isAttemptFresh(now: Long = System.currentTimeMillis()): Boolean =
        isToday() && now - attemptedAt < ACTIVITY_REFRESH_INTERVAL_MS
}

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("fishpi-session", Context.MODE_PRIVATE)

    fun getApiKey(): String =
        prefs.getString(KEY_API_KEY, "").orEmpty()

    fun saveApiKey(apiKey: String) {
        prefs.edit().putString(KEY_API_KEY, apiKey.trim()).apply()
    }

    fun saveAccount(apiKey: String, user: FishPiUser) {
        val token = apiKey.trim()
        if (token.isBlank()) return
        val normalizedUserName = user.userName.normalizedKey()

        val accounts = getAccounts()
            .filterNot { account ->
                account.apiKey == token ||
                    (normalizedUserName.isNotBlank() &&
                        account.userName.normalizedKey() == normalizedUserName)
            }
            .toMutableList()
            .apply {
                add(
                    0,
                    SavedAccount(
                        apiKey = token,
                        userName = user.userName,
                        displayName = user.displayName,
                        avatarUrl = user.userAvatarUrl,
                    ),
                )
            }

        prefs.edit()
            .putString(KEY_API_KEY, token)
            .putString(KEY_ACCOUNTS, accounts.toJson())
            .apply()
    }

    fun getAccounts(): List<SavedAccount> {
        val raw = prefs.getString(KEY_ACCOUNTS, "").orEmpty()
        val accounts = runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val token = item.optString("apiKey").trim()
                    if (token.isBlank()) continue
                    add(
                        SavedAccount(
                            apiKey = token,
                            userName = item.optString("userName"),
                            displayName = item.optString("displayName").ifBlank { item.optString("userName") },
                            avatarUrl = item.optString("avatarUrl"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
            .dedupAccounts()

        val currentApiKey = getApiKey()
        return if (accounts.none { it.apiKey == currentApiKey } && currentApiKey.isNotBlank()) {
            accounts + SavedAccount(
                apiKey = currentApiKey,
                userName = "",
                displayName = "当前账号",
                avatarUrl = "",
            )
        } else {
            accounts
        }
    }

    fun clear(currentApiKey: String? = null) {
        val editor = prefs.edit().remove(KEY_API_KEY)
        val token = currentApiKey?.trim().orEmpty()
        if (token.isNotBlank()) {
            val remain = getAccounts().filterNot { it.apiKey == token }
            editor.putString(KEY_ACCOUNTS, remain.toJson())
        }
        editor.apply()
    }

    fun getChatFilters(): ChatFilterConfig {
        val raw = prefs.getString(KEY_CHAT_FILTERS, "").orEmpty()
        if (raw.isBlank()) {
            return ChatFilterConfig()
        }
        return runCatching {
            val json = JSONObject(raw)
            ChatFilterConfig(
                blockedUsers = json.optJSONArray("blocked_users").toUniqueStringList(ChatFilterConfig.DefaultBlockedUsers),
                blockedKeywords = json.optJSONArray("blocked_keywords").toUniqueStringList(emptyList()),
                blockedPrefixKeywords = json.optJSONArray("blocked_prefix_keywords")
                    .toUniqueStringList(ChatFilterConfig.DefaultBlockedPrefixKeywords),
                blockedRegex = json.optJSONArray("blocked_regex").toUniqueStringList(emptyList()),
                showAvatars = json.optBoolean("show_avatars", true),
                sendOnEnter = json.optBoolean("send_on_enter", true),
            )
        }.getOrDefault(ChatFilterConfig())
    }

    fun saveChatFilters(config: ChatFilterConfig) {
        prefs.edit()
            .putString(
                KEY_CHAT_FILTERS,
                JSONObject()
                    .put("blocked_users", config.blockedUsers.toJsonArray())
                    .put("blocked_keywords", config.blockedKeywords.toJsonArray())
                    .put("blocked_prefix_keywords", config.blockedPrefixKeywords.toJsonArray())
                    .put("blocked_regex", config.blockedRegex.toJsonArray())
                    .put("show_avatars", config.showAvatars)
                    .put("send_on_enter", config.sendOnEnter)
                    .toString(),
            )
            .apply()
    }

    fun getThemePresetKey(): String =
        prefs.getString(KEY_THEME_PRESET, "").orEmpty()

    fun saveThemePresetKey(key: String) {
        prefs.edit().putString(KEY_THEME_PRESET, key).apply()
    }

    fun getImportedThemeJsons(): List<String> {
        val raw = prefs.getString(KEY_IMPORTED_THEMES, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val text = array.optString(index).trim()
                    if (text.isNotBlank()) add(text)
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveImportedThemeJsons(themes: List<String>) {
        val array = JSONArray()
        themes.forEach { raw -> array.put(raw) }
        prefs.edit().putString(KEY_IMPORTED_THEMES, array.toString()).apply()
    }

    fun getChatWallpaperUri(): String =
        prefs.getString(KEY_CHAT_WALLPAPER_URI, "").orEmpty()

    fun saveChatWallpaperUri(uri: String) {
        prefs.edit().putString(KEY_CHAT_WALLPAPER_URI, uri).apply()
    }

    fun getHomeWorkSettings(): HomeWorkSettings {
        val raw = prefs.getString(KEY_HOME_WORK_SETTINGS, "").orEmpty()
        if (raw.isBlank()) {
            return HomeWorkSettings()
        }
        return runCatching {
            val json = JSONObject(raw)
            HomeWorkSettings(
                startTime = json.optString("start_time", "09:00").takeIf { it.isWorkTimeText() } ?: "09:00",
                endTime = json.optString("end_time", "18:00").takeIf { it.isWorkTimeText() } ?: "18:00",
                weekendMode = json.optString("weekend_mode", HomeWorkSettings.WEEKEND_DOUBLE)
                    .takeIf { it in setOf(HomeWorkSettings.WEEKEND_DOUBLE, HomeWorkSettings.WEEKEND_SINGLE, HomeWorkSettings.WEEKEND_CUSTOM) }
                    ?: HomeWorkSettings.WEEKEND_DOUBLE,
                customRestDays = json.optJSONArray("custom_rest_days")
                    .toIntSet()
                    .filter { it in 1..7 }
                    .toSet()
                    .ifEmpty { setOf(7) },
            )
        }.getOrDefault(HomeWorkSettings())
    }

    fun saveHomeWorkSettings(settings: HomeWorkSettings) {
        prefs.edit()
            .putString(
                KEY_HOME_WORK_SETTINGS,
                JSONObject()
                    .put("start_time", settings.startTime)
                    .put("end_time", settings.endTime)
                    .put("weekend_mode", settings.weekendMode)
                    .put("custom_rest_days", settings.customRestDays.sorted().toIntJsonArray())
                    .toString(),
            )
            .apply()
    }

    fun getHomeActivity(apiKey: String): CachedUserActivity? {
        val token = apiKey.trim()
        if (token.isBlank()) return null
        val raw = prefs.getString(homeActivityKey(token), "").orEmpty()
        if (raw.isBlank()) return null
        return runCatching {
            val json = JSONObject(raw)
            val hasAttemptedAt = json.has("attempted_at")
            val rawLiveness = json.optDouble("liveness", 0.0)
            val legacyUnknownLiveness = !hasAttemptedAt && rawLiveness == 0.0
            CachedUserActivity(
                activity = UserActivityView(
                    liveness = if (legacyUnknownLiveness) UNKNOWN_LIVENESS else rawLiveness,
                    checkedIn = json.optBoolean("checked_in", false),
                    livenessRewarded = json.optBoolean("liveness_rewarded", false),
                ),
                date = json.optString("date"),
                fetchedAt = if (legacyUnknownLiveness) 0L else json.optLong("fetched_at", 0L),
                attemptedAt = if (legacyUnknownLiveness) 0L else json.optLong("attempted_at", json.optLong("fetched_at", 0L)),
            )
        }.getOrNull()
    }

    fun saveHomeActivity(
        apiKey: String,
        activity: UserActivityView,
        fetchedAt: Long = System.currentTimeMillis(),
        attemptedAt: Long = fetchedAt,
    ) {
        val token = apiKey.trim()
        if (token.isBlank()) return
        val cached = getHomeActivity(token)?.takeIf { it.isToday() }
        val nextActivity = if (cached?.activity?.livenessRewarded == true && !activity.livenessRewarded) {
            activity.copy(livenessRewarded = true)
        } else {
            activity
        }
        prefs.edit()
            .putString(
                homeActivityKey(token),
                JSONObject()
                    .put("liveness", nextActivity.liveness)
                    .put("checked_in", nextActivity.checkedIn)
                    .put("liveness_rewarded", nextActivity.livenessRewarded)
                    .put("date", todayKey())
                    .put("fetched_at", fetchedAt)
                    .put("attempted_at", attemptedAt)
                    .toString(),
            )
            .commit()
    }

    fun saveHomeActivityAttempt(apiKey: String, activity: UserActivityView? = null) {
        val token = apiKey.trim()
        if (token.isBlank()) return
        val cached = getHomeActivity(token)
        val nextActivity = activity ?: cached?.activity ?: UserActivityView(
            liveness = UNKNOWN_LIVENESS,
            checkedIn = false,
            livenessRewarded = false,
        )
        prefs.edit()
            .putString(
                homeActivityKey(token),
                JSONObject()
                    .put("liveness", nextActivity.liveness)
                    .put("checked_in", nextActivity.checkedIn)
                    .put("liveness_rewarded", nextActivity.livenessRewarded)
                    .put("date", todayKey())
                    .put("fetched_at", cached?.fetchedAt ?: 0L)
                    .put("attempted_at", System.currentTimeMillis())
                    .toString(),
            )
            .commit()
    }

    private companion object {
        const val KEY_API_KEY = "api_key"
        const val KEY_ACCOUNTS = "accounts"
        const val KEY_CHAT_FILTERS = "chat_filters"
        const val KEY_THEME_PRESET = "theme_preset"
        const val KEY_IMPORTED_THEMES = "imported_themes"
        const val KEY_CHAT_WALLPAPER_URI = "chat_wallpaper_uri"
        const val KEY_HOME_WORK_SETTINGS = "home_work_settings"
    }
}

private const val ACTIVITY_REFRESH_INTERVAL_MS = 10 * 60 * 1000L
const val UNKNOWN_LIVENESS = -1.0

private fun homeActivityKey(apiKey: String): String =
    "home_activity_${apiKey.hashCode()}"

private fun todayKey(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

private fun List<SavedAccount>.toJson(): String {
    val array = JSONArray()
    forEach { account ->
        array.put(
            org.json.JSONObject()
                .put("apiKey", account.apiKey)
                .put("userName", account.userName)
                .put("displayName", account.displayName)
                .put("avatarUrl", account.avatarUrl),
        )
    }
    return array.toString()
}

private fun String.normalizedKey(): String = trim().lowercase()

private fun List<SavedAccount>.dedupAccounts(): List<SavedAccount> {
    if (isEmpty()) return this
    val seenApiKeys = mutableSetOf<String>()
    val seenUsers = mutableSetOf<String>()
    return buildList {
        for (account in this@dedupAccounts) {
            val token = account.apiKey.trim()
            if (token.isBlank() || !seenApiKeys.add(token)) continue

            val userKey = account.userName.normalizedKey()
            if (userKey.isNotBlank() && !seenUsers.add(userKey)) continue

            add(account)
        }
    }
}

private fun JSONArray?.toUniqueStringList(default: List<String>): List<String> {
    if (this == null) {
        return default
    }
    val output = mutableListOf<String>()
    for (index in 0 until length()) {
        val text = optString(index).trim()
        if (text.isNotBlank() && output.none { it.equals(text, ignoreCase = true) }) {
            output += text
        }
    }
    return output
}

private fun List<String>.toJsonArray(): JSONArray {
    val array = JSONArray()
    forEach { item -> array.put(item) }
    return array
}

private fun List<Int>.toIntJsonArray(): JSONArray {
    val array = JSONArray()
    forEach { item -> array.put(item) }
    return array
}

private fun JSONArray?.toIntSet(): Set<Int> {
    if (this == null) return emptySet()
    return buildSet {
        for (index in 0 until length()) {
            add(optInt(index))
        }
    }
}

private fun String.isWorkTimeText(): Boolean =
    Regex("""^([01]\d|2[0-3]):[0-5]\d$""").matches(this)
