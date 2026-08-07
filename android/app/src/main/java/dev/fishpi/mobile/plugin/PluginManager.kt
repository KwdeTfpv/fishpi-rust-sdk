package dev.fishpi.mobile.plugin

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.fishpi.mobile.FishPiNoticeType
import dev.fishpi.mobile.FishPiNotifier
import dev.fishpi.mobile.data.FishPiNative
import dev.fishpi.mobile.feature.pluginui.PluginUiController
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.lang.reflect.Modifier
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

data class PluginInfo(
    val name: String, val version: String, val author: String,
    val scenes: List<String>, val fileName: String, val enabled: Boolean,
    val source: PluginSource = PluginSource.Unknown,
    val readable: Boolean = true,
)

enum class PluginSource { Store, Local, Unknown }

data class PluginToolbarAction(
    val pluginId: String,
    val entryId: String,
    val id: String,
    val label: String,
    val subtitle: String = "",
    val enabled: Boolean = true,
)

data class PluginToolbarEntry(
    val pluginId: String,
    val id: String,
    val title: String,
    val actions: List<PluginToolbarAction>,
)

data class PluginMenuAction(
    val pluginId: String,
    val id: String,
    val scene: String,
    val label: String,
    val enabled: Boolean = true,
)

private data class PluginChatClientConfig(
    val client: String,
    val version: String,
)

enum class PluginStatus { Disabled, Loading, Running, Error, Stopped }

data class CallRecord(
    val requestId: String,
    val method: String,
    val durationMs: Long,
    val ok: Boolean,
    val error: String? = null,
)

data class PluginRuntimeState(
    val fileName: String,
    var status: PluginStatus = PluginStatus.Disabled,
    var lastError: String? = null,
    var lastEventAt: Long = 0,
    val errors: MutableList<String> = mutableListOf(),
    val recentCalls: MutableList<CallRecord> = mutableListOf(),
) {
    fun recordError(msg: String) {
        lastError = msg
        errors.add(msg)
        if (errors.size > 20) errors.removeAt(0)
    }

    fun recordCall(requestId: String, method: String, durationMs: Long, ok: Boolean, error: String?) {
        recentCalls.add(CallRecord(requestId, method, durationMs, ok, error))
        if (recentCalls.size > 10) recentCalls.removeAt(0)
    }
}

data class PluginRuntimeSnapshot(
    val totalPlugins: Int,
    val runningSandboxes: Int,
    val toolbarEntryCount: Int,
    val toolbarActionCount: Int,
    val errorPluginCount: Int,
    val recentErrors: List<String>,
)

class PluginManager private constructor(context: Context) {
    companion object {
        @Volatile private var INSTANCE: PluginManager? = null

        fun init(ctx: Context): PluginManager = INSTANCE ?: synchronized(this) {
            INSTANCE ?: PluginManager(ctx.applicationContext).also { INSTANCE = it }
        }

        fun get(): PluginManager = INSTANCE
            ?: throw IllegalStateException("PluginManager not initialized")

        fun storageFor(pluginId: String): SharedPreferences =
            get().appContext.getSharedPreferences("fishpi_plugin_$pluginId", Context.MODE_PRIVATE)
    }

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val sandboxes = CopyOnWriteArrayList<PluginSandbox>()
    private val pluginDir = File(appContext.getExternalFilesDir(null) ?: appContext.filesDir, "plugins")
    private val runtimeStates = ConcurrentHashMap<String, PluginRuntimeState>()
    private val toolbarLock = Any()
    private val toolbarEntriesByPlugin = mutableMapOf<String, MutableMap<String, PluginToolbarEntry>>()
    private val menuLock = Any()
    private val menuActionsByPlugin = mutableMapOf<String, MutableMap<String, PluginMenuAction>>()
    private val chatClientConfigs = ConcurrentHashMap<String, PluginChatClientConfig>()
    @Volatile private var loaded = false
    var apiKey: String = ""
    var userName: String = ""
    var onSystemMessage: ((String) -> Unit)? = null
    var toolbarEntries by mutableStateOf<List<PluginToolbarEntry>>(emptyList())
        private set
    var menuActions by mutableStateOf<List<PluginMenuAction>>(emptyList())
        private set

    private val nativeMethods = mutableMapOf<String, java.lang.reflect.Method>()
    private val nativeClass = FishPiNative::class.java

    init {
        PluginUiController.get().setActionHandler { pluginId, actionId, values ->
            emitPluginUiAction(pluginId, actionId, values)
        }
    }

    private val methodParams = mapOf(
        "openRedPacket" to listOf("apiKey", "messageId", "gesture"),
        "sendChatRoomMessage" to listOf("apiKey", "content"),
        "revokeChatRoomMessage" to listOf("apiKey", "id"),
        "reactChatRoomMessage" to listOf("apiKey", "id", "value"),
        "getChatRoomHistory" to listOf("apiKey", "page", "selfUsername"),
        "sendRedPacket" to listOf("apiKey", "type", "money", "count", "message", "receivers", "gesture"),
        "uploadChatFile" to listOf("apiKey", "filePath"),
        "searchAtUsers" to listOf("query"),
        "getUser" to listOf("apiKey"),
        "getUserProfile" to listOf("apiKey", "userName"),
        "getUserActivity" to listOf("apiKey"),
        "rewardLiveness" to listOf("apiKey"),
        "getUserMedals" to listOf("apiKey", "userName"),
        "sendPrivateChatMessage" to listOf("apiKey", "peer", "content"),
        "getPrivateChatSessions" to listOf("apiKey", "selfUsername"),
        "getPrivateChatHistory" to listOf("apiKey", "peer", "page", "selfUsername"),
        "revokePrivateChatMessage" to listOf("apiKey", "id"),
        "markPrivateChatRead" to listOf("apiKey", "peer"),
        "getArticles" to listOf("apiKey", "filter", "tag", "page"),
        "getUserArticles" to listOf("apiKey", "userName", "page"),
        "getArticleDetail" to listOf("apiKey", "articleId", "page"),
        "sendArticleComment" to listOf("apiKey", "articleId", "content", "replyId"),
        "voteArticle" to listOf("apiKey", "articleId", "like"),
        "thankArticle" to listOf("apiKey", "articleId"),
        "followArticle" to listOf("apiKey", "articleId"),
        "unfollowArticle" to listOf("apiKey", "articleId"),
        "watchArticle" to listOf("apiKey", "articleId"),
        "getEmojiGroups" to listOf("apiKey"),
        "getEmojiGroupItems" to listOf("apiKey", "groupId"),
        "sendBreezemoon" to listOf("apiKey", "content"),
        "getBreezemoons" to listOf("apiKey", "page", "size"),
        "getUserBreezemoons" to listOf("apiKey", "userName", "page", "size"),
        "getNoticeUnreadCount" to listOf("apiKey"),
        "getNotices" to listOf("apiKey"),
        "markAllNoticesRead" to listOf("apiKey"),
    )

    private val sceneLock = Any()
    private val sceneStack = LinkedHashMap<SceneToken, String>()

    @Volatile
    var currentScene: String = ""
        private set

    class SceneToken

    fun acquireSceneToken(): SceneToken = SceneToken()

    fun setSceneFor(token: SceneToken, scene: String) {
        synchronized(sceneLock) {
            val trimmed = scene.trim()
            if (trimmed.isEmpty()) {
                sceneStack.remove(token)
            } else {
                sceneStack.remove(token)
                sceneStack[token] = trimmed
            }
        }
        onSceneChanged()
    }

    fun releaseSceneToken(token: SceneToken) {
        val changed = synchronized(sceneLock) { sceneStack.remove(token) != null }
        if (changed) onSceneChanged()
    }

    private fun onSceneChanged() {
        val top = synchronized(sceneLock) { sceneStack.values.lastOrNull() ?: "" }
        if (top == currentScene) return
        currentScene = top
        if (!loaded) loadInternal()
        refreshToolbarEntries()
        refreshMenuActions()
    }

    fun getState(fileName: String): PluginRuntimeState =
        runtimeStates.getOrPut(fileName) { PluginRuntimeState(fileName) }

    fun getPluginStorage(fileName: String): Map<String, String> {
        val prefs = storageFor(fileName)
        return prefs.all.entries.associate { (key, value) -> key to (value?.toString() ?: "") }
    }

    fun setPluginStorage(fileName: String, key: String, value: String) {
        storageFor(fileName).edit().putString(key, value).apply()
    }

    fun removePluginStorage(fileName: String, key: String) {
        storageFor(fileName).edit().remove(key).apply()
    }

    fun dispatch(pluginId: String, action: String, args: JSONObject): JSONObject {
        if (action == "systemMessage") {
            val text = args.optString("text", "")
            mainHandler.post { onSystemMessage?.invoke(text) }
            return JSONObject("{}")
        }
        if (action == "app.notify") {
            val text = args.optString("text", "").ifBlank { args.optString("message", "") }
            val title = args.optString("title", "")
            val message = listOf(title, text).filter(String::isNotBlank).joinToString("：")
            val type = args.optString("type", "info").toFishPiNoticeType()
            val durationMs = args.optLong("durationMs", 2_200L).coerceIn(800L, 10_000L)
            val avatarUrl = args.optString("avatarUrl", "")
            mainHandler.post {
                FishPiNotifier.show(
                    message = message,
                    type = type,
                    durationMs = durationMs,
                    avatarUrl = avatarUrl,
                )
            }
            return JSONObject().put("ok", true)
        }
        when (action) {
            "toolbar.register" -> return registerToolbarEntry(pluginId, args)
            "toolbar.unregister" -> return unregisterToolbarEntry(pluginId, args.optString("id", ""))
            "toolbar.clear" -> return clearToolbarEntries(pluginId)
            "menu.register" -> return registerMenuAction(pluginId, args)
            "menu.unregister" -> return unregisterMenuAction(pluginId, args.optString("id", ""))
            "menu.clear" -> return clearMenuActions(pluginId)
            "ui.open" -> return PluginUiController.get().open(pluginId, args)
            "ui.update" -> return PluginUiController.get().update(pluginId, args)
            "ui.close" -> return PluginUiController.get().close(pluginId)
            "ui.clear" -> return PluginUiController.get().clear(pluginId)
            "ui.streamPush" -> return PluginUiController.get().streamPush(pluginId, args)
            "ui.streamEnd" -> return PluginUiController.get().streamEnd(pluginId, args)
            "ui.copy" -> return copyToClipboard(args)
            "ui.exportCard" -> return exportSummaryCard(args)
            "chat.setClientType" -> return setPluginChatClientType(pluginId, args)
            "chat.clearClientType" -> return clearPluginChatClientType(pluginId)
            "http.request" -> return performHttpRequest(pluginId, args)
            "http.stream.start" -> return performHttpStreamStart(pluginId, args)
            "http.stream.abort" -> return performHttpStreamAbort(args)
        }
        val state = getState(pluginId)
        val requestId = "${pluginId}:${action}:${System.currentTimeMillis()}"
        val start = System.currentTimeMillis()
        state.lastEventAt = start
        return try {
            val result = invokeNative(pluginId, action, args)
            state.recordCall(requestId, action, System.currentTimeMillis() - start, ok = true, error = null)
            state.status = PluginStatus.Running
            result
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - start
            state.recordCall(requestId, action, elapsed, ok = false, error = e.message)
            state.recordError("$action: ${e.message}")
            state.status = PluginStatus.Error
            JSONObject().put("ok", false).put("error", e.message ?: "native call failed")
        }
    }

    private fun invokeNative(pluginId: String, methodName: String, args: JSONObject): JSONObject {
        if (methodName == "sendChatRoomMessage") {
            chatClientConfigs[pluginId]?.let { config ->
                return invokeSendChatRoomMessageWithClientType(args, config)
            }
        }
        val m = nativeMethods.getOrPut(methodName) {
            nativeClass.declaredMethods.first { it.name == methodName }
        }
        val paramNames = methodParams[methodName]
            ?: throw RuntimeException("unknown method: $methodName")
        val values = arrayOfNulls<Any>(paramNames.size)
        for (i in paramNames.indices) {
            val name = paramNames[i]
            values[i] = when (name) {
                "apiKey" -> apiKey
                else -> jsonArg(args, name, m.parameters[i].type)
            }
        }
        val latch = CountDownLatch(1)
        var result: JSONObject? = null
        var error: Throwable? = null
        Thread {
            runCatching {
                val raw = m.invoke(
                    if (Modifier.isStatic(m.modifiers)) null else FishPiNative, *values,
                ) as String
                raw.unwrap()
            }.onSuccess {
                result = it
            }.onFailure {
                error = it
            }
            latch.countDown()
        }.start()
        latch.await(10, TimeUnit.SECONDS)
        result?.let { return it }
        throw error ?: RuntimeException("timeout")
    }

    private fun invokeSendChatRoomMessageWithClientType(
        args: JSONObject,
        config: PluginChatClientConfig,
    ): JSONObject {
        val content = args.optString("content", "")
        val latch = CountDownLatch(1)
        var result: JSONObject? = null
        var error: Throwable? = null
        Thread {
            runCatching {
                FishPiNative
                    .sendChatRoomMessageWithClientType(apiKey, content, config.client, config.version)
                    .unwrap()
            }.onSuccess {
                result = it
            }.onFailure {
                error = it
            }
            latch.countDown()
        }.start()
        latch.await(10, TimeUnit.SECONDS)
        result?.let { return it }
        throw error ?: RuntimeException("timeout")
    }

    private fun jsonArg(args: JSONObject, name: String, type: Class<*>): Any = when (type) {
        Int::class.javaPrimitiveType, Int::class.java -> if (args.has(name)) args.getInt(name) else -1
        Long::class.javaPrimitiveType, Long::class.java -> if (args.has(name)) args.getLong(name) else 0L
        Boolean::class.javaPrimitiveType, Boolean::class.java -> if (args.has(name)) args.getBoolean(name) else false
        Double::class.javaPrimitiveType, Double::class.java -> if (args.has(name)) args.getDouble(name) else 0.0
        String::class.java -> if (name == "receivers") receiversArg(args) else args.optString(name, "")
        else -> args.optString(name, "")
    }

    private fun receiversArg(args: JSONObject): String {
        if (!args.has("receivers")) return ""
        val raw = args.opt("receivers")
        if (raw is org.json.JSONArray) {
            return (0 until raw.length())
                .mapNotNull { raw.optString(it).trim().takeIf(String::isNotEmpty) }
                .joinToString(",")
        }
        return args.optString("receivers", "")
    }

    private fun String.unwrap(): JSONObject {
        val json = JSONObject(this)
        if (!json.optBoolean("ok", false))
            throw RuntimeException(json.optString("error", "native call failed"))
        return json.optJSONObject("data") ?: json
    }

    fun notify(event: String, dataJson: String) {
        for (sb in sandboxes) {
            val scenes = sb.header.scenes.map(String::trim).filter(String::isNotBlank)
            if (scenes.isNotEmpty() && !scenes.contains(currentScene)) continue
            try { sb.bridge.emit(event, dataJson) } catch (e: Exception) {
                getState(sb.fileName).recordError("emit $event: ${e.message}")
            }
        }
    }


    fun emitToolbarAction(pluginId: String, entryId: String, actionId: String, context: JSONObject? = null) {
        val sb = sandboxes.firstOrNull { it.fileName == pluginId } ?: return
        val payload = JSONObject()
            .put("entryId", entryId)
            .put("actionId", actionId)
        if (context != null) payload.put("context", context)
        try {
            sb.bridge.emit("toolbarAction", payload.toString())
        } catch (e: Exception) {
            getState(pluginId).recordError("emit toolbarAction: ${e.message}")
        }
    }

    fun emitMenuAction(pluginId: String, actionId: String, scene: String, message: JSONObject) {
        val sb = sandboxes.firstOrNull { it.fileName == pluginId } ?: return
        val payload = JSONObject()
            .put("actionId", actionId)
            .put("scene", scene)
            .put("message", message)
        try {
            sb.bridge.emit("menuAction", payload.toString())
        } catch (e: Exception) {
            getState(pluginId).recordError("emit menuAction: ${e.message}")
        }
    }

    private fun emitPluginUiAction(pluginId: String, actionId: String, values: JSONObject) {
        val sb = sandboxes.firstOrNull { it.fileName == pluginId } ?: return
        val payload = JSONObject()
            .put("actionId", actionId)
            .put("values", values.optJSONObject("values") ?: JSONObject())
            .put("nodeId", values.optString("nodeId"))
        try {
            sb.bridge.emit("uiAction", payload.toString())
        } catch (e: Exception) {
            getState(pluginId).recordError("emit uiAction: ${e.message}")
        }
    }

    fun pluginInfos(): List<PluginInfo> {
        val disabled = disabledFileNames()
        return pluginDir.listFiles { f -> f.extension == "js" }?.mapNotNull { f ->
            runCatching {
                val h = PluginHeaderParser.parse(f.readText()) ?: return@runCatching null
                val needsConfirmation = requiresSafetyConfirmation(f.name)
                PluginInfo(
                    h.name,
                    h.version,
                    h.author,
                    h.scenes,
                    f.name,
                    !disabled.contains(f.name) && !needsConfirmation,
                    pluginSource(f.name),
                    readable = true,
                )
            }.getOrElse {
                PluginInfo(
                    name = f.nameWithoutExtension,
                    version = "",
                    author = "",
                    scenes = emptyList(),
                    fileName = f.name,
                    enabled = false,
                    source = PluginSource.Unknown,
                    readable = false,
                )
            }
        }?.toList() ?: emptyList()
    }

    fun runtimeSnapshot(): PluginRuntimeSnapshot {
        val totalPlugins = pluginDir.listFiles { f -> f.extension == "js" }?.size ?: 0
        val entries = synchronized(toolbarLock) {
            toolbarEntriesByPlugin.values.flatMap { it.values }
        }
        val recentErrors = runtimeStates.values
            .filter { it.lastError != null }
            .sortedByDescending { it.lastEventAt }
            .take(5)
            .map { state -> "${state.fileName}: ${state.lastError}" }
        return PluginRuntimeSnapshot(
            totalPlugins = totalPlugins,
            runningSandboxes = sandboxes.size,
            toolbarEntryCount = entries.size,
            toolbarActionCount = entries.sumOf { it.actions.size },
            errorPluginCount = runtimeStates.values.count { it.lastError != null },
            recentErrors = recentErrors,
        )
    }

    fun togglePlugin(fileName: String, enable: Boolean) {
        val prefs = storageFor("__manager")
        val disabled = disabledFileNames().toMutableSet()
        if (enable) disabled.remove(fileName) else disabled.add(fileName)
        prefs.edit().putStringSet("disabled", disabled).apply()
        if (enable) {
            reloadPlugin(fileName)
        } else {
            destroyPlugin(fileName)
            getState(fileName).status = PluginStatus.Disabled
        }
    }

    fun pluginDirectoryPath(): String = pluginDir.absolutePath

    fun uninstallPlugin(fileName: String) {
        destroyPlugin(fileName)
        File(pluginDir, fileName).delete()
        runtimeStates.remove(fileName)
        storageFor("__manager").edit()
            .remove("source:$fileName")
            .remove("approved_hash:$fileName")
            .remove("store_hash:$fileName")
            .apply()
    }

    fun requiresSafetyConfirmation(fileName: String): Boolean {
        val file = File(pluginDir, fileName)
        if (!file.exists()) return false
        if (isStoreVerified(fileName, file)) return false
        val approvedHash = storageFor("__manager").getString("approved_hash:$fileName", "")
        val currentHash = runCatching { file.sha256() }.getOrNull() ?: return true
        return approvedHash != currentHash
    }

    fun approvePluginForCurrentContent(fileName: String) {
        val file = File(pluginDir, fileName)
        if (!file.exists()) return
        storageFor("__manager").edit()
            .putString("approved_hash:$fileName", file.sha256())
            .apply()
    }

    fun installPluginFromUri(uri: Uri): String {
        pluginDir.mkdirs()
        val rawName = runCatching {
            appContext.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
                }
        }.getOrNull()
        val safeBase = (rawName ?: "plugin-${System.currentTimeMillis()}.js")
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
        val finalName = if (safeBase.endsWith(".js", ignoreCase = true)) safeBase else "$safeBase.js"
        val target = File(pluginDir, finalName)
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IOException("无法读取所选文件")
        markPluginSource(finalName, PluginSource.Local)
        val disabled = disabledFileNames().toMutableSet()
        disabled.add(finalName)
        storageFor("__manager").edit()
            .putStringSet("disabled", disabled)
            .remove("approved_hash:$finalName")
            .remove("store_hash:$finalName")
            .apply()
        destroyPlugin(finalName)
        getState(finalName).status = PluginStatus.Disabled
        return finalName
    }

    fun reimportPluginFromUri(uri: Uri, targetFileName: String): String {
        pluginDir.mkdirs()
        val finalName = targetFileName.takeIf {
            it.endsWith(".js", ignoreCase = true) && !it.contains('/') && !it.contains('\\')
        } ?: throw IOException("非法目标文件名")
        val target = File(pluginDir, finalName)
        runCatching { if (target.exists()) target.delete() }
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IOException("无法读取所选文件")
        markPluginSource(finalName, PluginSource.Local)
        val disabled = disabledFileNames().toMutableSet()
        disabled.add(finalName)
        storageFor("__manager").edit()
            .putStringSet("disabled", disabled)
            .remove("approved_hash:$finalName")
            .remove("store_hash:$finalName")
            .apply()
        destroyPlugin(finalName)
        getState(finalName).status = PluginStatus.Disabled
        return finalName
    }

    private fun String.toFishPiNoticeType(): FishPiNoticeType =
        when (trim().lowercase()) {
            "success", "ok" -> FishPiNoticeType.Success
            "warning", "warn" -> FishPiNoticeType.Warning
            "error", "danger" -> FishPiNoticeType.Error
            else -> FishPiNoticeType.Info
        }

    fun installPluginFromSource(
        source: String,
        preferredName: String,
        enable: Boolean = true,
        pluginSource: PluginSource = PluginSource.Local,
    ): String {
        pluginDir.mkdirs()
        val header = PluginHeaderParser.parse(source)
            ?: throw IOException("插件缺少 FishPiPlugin 元信息")
        val finalName = storePluginFileName(preferredName.ifBlank { header.name })
        val target = File(pluginDir, finalName)
        target.writeText(source, Charsets.UTF_8)
        markPluginSource(finalName, pluginSource)
        val managerPrefs = storageFor("__manager").edit().remove("approved_hash:$finalName")
        if (pluginSource == PluginSource.Store) {
            managerPrefs.putString("store_hash:$finalName", target.sha256())
        } else {
            managerPrefs.remove("store_hash:$finalName")
        }
        managerPrefs.apply()
        if (enable) {
            togglePlugin(finalName, enable = true)
        } else {
            val prefs = storageFor("__manager")
            val disabled = disabledFileNames().toMutableSet()
            disabled.add(finalName)
            prefs.edit().putStringSet("disabled", disabled).apply()
            destroyPlugin(finalName)
            getState(finalName).status = PluginStatus.Disabled
        }
        return finalName
    }

    fun storePluginInfo(preferredName: String): PluginInfo? {
        val finalName = storePluginFileName(preferredName)
        return pluginInfos().firstOrNull { it.fileName == finalName }
    }

    fun storePluginMatchesSource(preferredName: String, source: String): Boolean {
        val finalName = storePluginFileName(preferredName)
        val target = File(pluginDir, finalName)
        return runCatching {
            target.exists() && target.readText(Charsets.UTF_8) == source
        }.getOrDefault(false)
    }

    fun storePluginFileName(preferredName: String): String {
        val safeBase = preferredName
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('_', '.', '-')
            .ifBlank { "store-plugin" }
        return if (safeBase.endsWith(".js", ignoreCase = true)) safeBase else "$safeBase.js"
    }

    fun readPluginSource(fileName: String): String {
        return pluginFile(fileName).readText()
    }

    fun savePluginSource(fileName: String, source: String) {
        pluginFile(fileName).writeText(source)
        markPluginSource(fileName, PluginSource.Local)
        approvePluginForCurrentContent(fileName)
        storageFor("__manager").edit().remove("store_hash:$fileName").apply()
        reloadPlugin(fileName)
    }

    fun reloadPlugin(fileName: String) {
        destroyPlugin(fileName)
        chatClientConfigs.remove(fileName)
        val file = File(pluginDir, fileName)
        if (!file.exists()) return
        getState(fileName).status = PluginStatus.Loading
        runCatching {
            val script = file.readText()
            val header = PluginHeaderParser.parse(script) ?: return
            val sandbox = PluginSandbox(appContext, fileName, header, script)
            sandbox.start()
            sandboxes.add(sandbox)
            getState(fileName).status = PluginStatus.Running
            refreshMenuActions()
        }.onFailure { e ->
            getState(fileName).recordError("load: ${e.message}")
            getState(fileName).status = PluginStatus.Error
        }
    }

    fun applySendHook(text: String): String {
        var result = text
        val active = sandboxes.filter { it.header.scenes.contains(currentScene) }
        if (active.isEmpty()) return result
        val latch = CountDownLatch(active.size)
        for (sb in active) {
            mainHandler.post { sb.applySendHook(result) { modified -> result = modified; latch.countDown() } }
        }
        latch.await(300, TimeUnit.MILLISECONDS)
        return result
    }

    fun destroyAll() {
        sandboxes.forEach { it.destroy() }
        sandboxes.clear()
        chatClientConfigs.clear()
        synchronized(toolbarLock) {
            toolbarEntriesByPlugin.clear()
        }
        synchronized(menuLock) {
            menuActionsByPlugin.clear()
        }
        refreshToolbarEntries()
        refreshMenuActions()
    }

    private fun destroyPlugin(fileName: String) {
        chatClientConfigs.remove(fileName)
        synchronized(toolbarLock) {
            toolbarEntriesByPlugin.remove(fileName)
        }
        synchronized(menuLock) {
            menuActionsByPlugin.remove(fileName)
        }
        val sb = sandboxes.find { it.fileName == fileName }
        if (sb != null) {
            sandboxes.remove(sb)
            sb.destroy()
        }
        refreshToolbarEntries()
        refreshMenuActions()
    }

    private fun registerToolbarEntry(pluginId: String, args: JSONObject): JSONObject {
        val entryId = args.optString("id").trim()
        val title = args.optString("title").trim()
        if (entryId.isBlank()) return JSONObject().put("ok", false).put("error", "toolbar entry id is required")
        if (title.isBlank()) return JSONObject().put("ok", false).put("error", "toolbar entry title is required")
        val actionArray = args.optJSONArray("actions") ?: JSONArray()
        val actions = mutableListOf<PluginToolbarAction>()
        for (i in 0 until actionArray.length()) {
            val raw = actionArray.optJSONObject(i) ?: continue
            val actionId = raw.optString("id").trim()
            val label = raw.optString("label").trim()
            if (actionId.isBlank() || label.isBlank()) continue
            actions += PluginToolbarAction(
                pluginId = pluginId,
                entryId = entryId,
                id = actionId,
                label = label,
                subtitle = raw.optString("subtitle").trim(),
                enabled = if (raw.has("enabled")) raw.optBoolean("enabled", true) else true,
            )
        }
        synchronized(toolbarLock) {
            val entries = toolbarEntriesByPlugin.getOrPut(pluginId) { mutableMapOf() }
            entries[entryId] = PluginToolbarEntry(
                pluginId = pluginId,
                id = entryId,
                title = title,
                actions = actions,
            )
        }
        refreshToolbarEntries()
        return JSONObject().put("ok", true)
    }

    private fun unregisterToolbarEntry(pluginId: String, entryId: String): JSONObject {
        if (entryId.isBlank()) return JSONObject().put("ok", false).put("error", "toolbar entry id is required")
        synchronized(toolbarLock) {
            toolbarEntriesByPlugin[pluginId]?.remove(entryId)
            if (toolbarEntriesByPlugin[pluginId]?.isEmpty() == true) {
                toolbarEntriesByPlugin.remove(pluginId)
            }
        }
        refreshToolbarEntries()
        return JSONObject().put("ok", true)
    }

    private fun clearToolbarEntries(pluginId: String): JSONObject {
        synchronized(toolbarLock) {
            toolbarEntriesByPlugin.remove(pluginId)
        }
        refreshToolbarEntries()
        return JSONObject().put("ok", true)
    }

    private fun registerMenuAction(pluginId: String, args: JSONObject): JSONObject {
        val actionId = args.optString("id").trim()
        val scene = args.optString("scene", currentScene).trim()
        val label = args.optString("label").trim()
        if (actionId.isBlank()) return JSONObject().put("ok", false).put("error", "menu action id is required")
        if (scene.isBlank()) return JSONObject().put("ok", false).put("error", "menu action scene is required")
        if (label.isBlank()) return JSONObject().put("ok", false).put("error", "menu action label is required")
        synchronized(menuLock) {
            val actions = menuActionsByPlugin.getOrPut(pluginId) { mutableMapOf() }
            actions[actionId] = PluginMenuAction(
                pluginId = pluginId,
                id = actionId,
                scene = scene,
                label = label,
                enabled = if (args.has("enabled")) args.optBoolean("enabled", true) else true,
            )
        }
        refreshMenuActions()
        return JSONObject().put("ok", true)
    }

    private fun unregisterMenuAction(pluginId: String, actionId: String): JSONObject {
        if (actionId.isBlank()) return JSONObject().put("ok", false).put("error", "menu action id is required")
        synchronized(menuLock) {
            menuActionsByPlugin[pluginId]?.remove(actionId)
            if (menuActionsByPlugin[pluginId]?.isEmpty() == true) {
                menuActionsByPlugin.remove(pluginId)
            }
        }
        refreshMenuActions()
        return JSONObject().put("ok", true)
    }

    private fun clearMenuActions(pluginId: String): JSONObject {
        synchronized(menuLock) {
            menuActionsByPlugin.remove(pluginId)
        }
        refreshMenuActions()
        return JSONObject().put("ok", true)
    }

    private fun setPluginChatClientType(pluginId: String, args: JSONObject): JSONObject {
        val client = args.optString("client").trim()
        val version = args.optString("version").trim()
        if (client.isBlank()) {
            return JSONObject().put("ok", false).put("error", "client is required")
        }
        if (version.isBlank()) {
            return JSONObject().put("ok", false).put("error", "version is required")
        }
        chatClientConfigs[pluginId] = PluginChatClientConfig(client = client, version = version)
        return JSONObject().put("ok", true)
    }

    private fun clearPluginChatClientType(pluginId: String): JSONObject {
        chatClientConfigs.remove(pluginId)
        return JSONObject().put("ok", true)
    }

    private fun performHttpRequest(pluginId: String, args: JSONObject): JSONObject {
        val url = args.optString("url").trim()
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            return JSONObject().put("ok", false).put("error", "invalid url")
        }
        val method = args.optString("method", "GET").trim().uppercase().ifBlank { "GET" }
        val headers = args.optJSONObject("headers") ?: JSONObject()
        val body = if (args.isNull("body")) null else args.optString("body", "")
        val timeoutMs = args.optInt("timeoutMs", 30_000).coerceIn(1_000, 120_000)
        val latch = CountDownLatch(1)
        var result: JSONObject? = null
        Thread {
            result = runCatching { httpExchange(url, method, headers, body, timeoutMs) }
                .getOrElse { JSONObject().put("ok", false).put("error", it.message ?: "request failed") }
            latch.countDown()
        }.start()
        latch.await((timeoutMs + 5_000).toLong(), TimeUnit.MILLISECONDS)
        getState(pluginId).lastEventAt = System.currentTimeMillis()
        return result ?: JSONObject().put("ok", false).put("error", "timeout")
    }

    private fun httpExchange(
        url: String,
        method: String,
        headers: JSONObject,
        body: String?,
        timeoutMs: Int,
    ): JSONObject {
        val conn = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("Accept", "application/json, text/plain, */*")
            setRequestProperty("User-Agent", "FishPi-Mobile-Android")
            headers.keys().forEach { key -> setRequestProperty(key, headers.optString(key)) }
            if (body != null && method != "GET" && method != "HEAD") {
                doOutput = true
                outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
        }
        return try {
            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            JSONObject().put("ok", status in 200..299).put("status", status).put("body", text)
        } finally {
            conn.disconnect()
        }
    }

    private val streamClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .build()
    }
    private val activeStreams = ConcurrentHashMap<String, Call>()

    private fun emitStreamEvent(pluginId: String, payload: JSONObject) {
        val sb = sandboxes.firstOrNull { it.fileName == pluginId } ?: return
        try {
            sb.bridge.emit("httpStream", payload.toString())
        } catch (e: Exception) {
            getState(pluginId).recordError("emit httpStream: ${e.message}")
        }
    }

    private fun performHttpStreamStart(pluginId: String, args: JSONObject): JSONObject {
        val streamId = args.optString("streamId").trim()
        if (streamId.isBlank()) return JSONObject().put("ok", false).put("error", "streamId required")
        val url = args.optString("url").trim()
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            return JSONObject().put("ok", false).put("error", "invalid url")
        }
        val method = args.optString("method", "POST").trim().uppercase().ifBlank { "POST" }
        val headers = args.optJSONObject("headers") ?: JSONObject()
        val bodyStr = if (args.isNull("body")) null else args.optString("body", "")
        val timeoutMs = args.optInt("timeoutMs", 120_000).coerceIn(1_000, 600_000)

        val builder = Request.Builder().url(url)
            .header("Accept", "text/event-stream")
            .header("User-Agent", "FishPi-Mobile-Android")
        headers.keys().forEach { key -> builder.header(key, headers.optString(key)) }
        val mediaType = headers.optString("Content-Type", "application/json").toMediaTypeOrNull()
        val reqBody = if (bodyStr != null && method != "GET" && method != "HEAD") {
            bodyStr.toRequestBody(mediaType)
        } else null
        builder.method(method, reqBody)

        val call = streamClient.newBuilder()
            .callTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
            .build()
            .newCall(builder.build())
        activeStreams[streamId] = call
        getState(pluginId).lastEventAt = System.currentTimeMillis()
        call.enqueue(streamCallback(pluginId, streamId))
        return JSONObject().put("ok", true)
    }

    private fun streamCallback(pluginId: String, streamId: String) = object : Callback {
        override fun onFailure(call: Call, e: java.io.IOException) {
            activeStreams.remove(streamId)
            if (call.isCanceled()) return  // abort 主动取消,不当作错误
            emitStreamEvent(pluginId, JSONObject().put("streamId", streamId)
                .put("type", "error").put("error", e.message ?: "network error"))
        }

        override fun onResponse(call: Call, response: Response) {
            response.use { resp ->
                val code = resp.code
                if (!resp.isSuccessful) {
                    val detail = try { resp.body?.string()?.take(500) ?: "" } catch (_: Exception) { "" }
                    activeStreams.remove(streamId)
                    emitStreamEvent(pluginId, JSONObject().put("streamId", streamId)
                        .put("type", "error").put("error", "HTTP $code").put("detail", detail))
                    return
                }
                val source = resp.body?.source()
                if (source == null) {
                    activeStreams.remove(streamId)
                    emitStreamEvent(pluginId, JSONObject().put("streamId", streamId)
                        .put("type", "error").put("error", "empty body"))
                    return
                }
                try {
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (line.startsWith("data:")) {
                            val data = line.substring(5).trim()
                            if (data == "[DONE]") break
                            if (data.isNotEmpty()) {
                                emitStreamEvent(pluginId, JSONObject().put("streamId", streamId)
                                    .put("type", "chunk").put("data", data))
                            }
                        }
                        getState(pluginId).lastEventAt = System.currentTimeMillis()
                    }
                    activeStreams.remove(streamId)
                    emitStreamEvent(pluginId, JSONObject().put("streamId", streamId).put("type", "done"))
                } catch (e: Exception) {
                    activeStreams.remove(streamId)
                    if (!call.isCanceled()) {
                        emitStreamEvent(pluginId, JSONObject().put("streamId", streamId)
                            .put("type", "error").put("error", e.message ?: "read error"))
                    }
                }
            }
        }
    }

    private fun performHttpStreamAbort(args: JSONObject): JSONObject {
        val streamId = args.optString("streamId").trim()
        activeStreams.remove(streamId)?.cancel()
        return JSONObject().put("ok", true)
    }

    private fun copyToClipboard(args: JSONObject): JSONObject {
        val text = args.optString("text")
        if (text.isBlank()) return JSONObject().put("ok", false).put("error", "empty text")
        val run = Runnable {
            runCatching {
                val cm = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("FishPi", text))
                FishPiNotifier.success("已复制")
            }.onFailure { FishPiNotifier.error("复制失败：${it.message}") }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) run.run() else mainHandler.post(run)
        return JSONObject().put("ok", true)
    }

    private fun exportSummaryCard(args: JSONObject): JSONObject {
        val markdown = args.optString("markdown")
        if (markdown.isBlank()) return JSONObject().put("ok", false).put("error", "empty markdown")
        PluginCardExporter.export(
            appContext = appContext,
            title = args.optString("title"),
            author = args.optString("author"),
            avatarUrl = args.optString("avatar"),
            source = args.optString("source").ifBlank { "摸鱼派 · AI 总结" },
            markdown = markdown,
            footer = args.optString("footer"),
        )
        return JSONObject().put("ok", true)
    }

    private fun refreshToolbarEntries() {
        val visible = synchronized(toolbarLock) {
            toolbarEntriesByPlugin.values
                .flatMap { it.values }
                .filter { entry ->
                    val sb = sandboxes.firstOrNull { it.fileName == entry.pluginId } ?: return@filter false
                    val scenes = sb.header.scenes.map(String::trim).filter(String::isNotBlank)
                    scenes.isEmpty() || scenes.contains(currentScene)
                }
                .sortedWith(compareBy<PluginToolbarEntry> { it.pluginId }.thenBy { it.id })
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            toolbarEntries = visible
        } else {
            mainHandler.post { toolbarEntries = visible }
        }
    }

    private fun refreshMenuActions() {
        val visible = synchronized(menuLock) {
            menuActionsByPlugin.values
                .flatMap { it.values }
                .filter { action ->
                    val sb = sandboxes.firstOrNull { it.fileName == action.pluginId } ?: return@filter false
                    val scenes = sb.header.scenes.map(String::trim).filter(String::isNotBlank)
                    val pluginVisible = scenes.isEmpty() || scenes.contains(currentScene)
                    pluginVisible && action.scene == currentScene
                }
                .sortedWith(compareBy<PluginMenuAction> { it.pluginId }.thenBy { it.id })
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            menuActions = visible
        } else {
            mainHandler.post { menuActions = visible }
        }
    }

    private fun disabledFileNames(): Set<String> =
        storageFor("__manager").getStringSet("disabled", emptySet()) ?: emptySet()

    private fun pluginSource(fileName: String): PluginSource {
        val value = storageFor("__manager").getString("source:$fileName", "") ?: ""
        return when (value) {
            "local" -> PluginSource.Local
            "store" -> {
                val file = File(pluginDir, fileName)
                if (file.exists() && isStoreVerified(fileName, file)) PluginSource.Store else PluginSource.Unknown
            }
            else -> PluginSource.Unknown
        }
    }

    private fun isStoreVerified(fileName: String, file: File = File(pluginDir, fileName)): Boolean {
        val storeHash = storageFor("__manager").getString("store_hash:$fileName", "") ?: ""
        if (storeHash.isBlank() || !file.exists()) return false
        val hash = runCatching { file.sha256() }.getOrNull() ?: return false
        return storeHash == hash
    }

    private fun markPluginSource(fileName: String, source: PluginSource) {
        val value = when (source) {
            PluginSource.Store -> "store"
            PluginSource.Local -> "local"
            PluginSource.Unknown -> "unknown"
        }
        storageFor("__manager").edit().putString("source:$fileName", value).apply()
    }

    private fun pluginFile(fileName: String): File {
        require(fileName.endsWith(".js", ignoreCase = true)) { "仅支持编辑 .js 插件" }
        require(!fileName.contains('/') && !fileName.contains('\\')) { "非法插件文件名" }
        return File(pluginDir, fileName)
    }

    private fun loadInternal(): Int {
        pluginDir.mkdirs()
        val disabled = disabledFileNames()
        pluginDir.listFiles { f -> f.extension == "js" }?.forEach { file ->
            if (file.name in disabled) return@forEach
            runCatching {
                if (requiresSafetyConfirmation(file.name)) {
                    getState(file.name).status = PluginStatus.Disabled
                    return@runCatching
                }
                val script = file.readText()
                val header = PluginHeaderParser.parse(script) ?: return@runCatching
                val sandbox = PluginSandbox(appContext, file.name, header, script)
                sandbox.start()
                sandboxes.add(sandbox)
                getState(file.name).status = PluginStatus.Running
            }.onFailure { e ->
                getState(file.name).recordError("load: ${e.message}")
                getState(file.name).status = PluginStatus.Error
            }
        }
        loaded = true
        return sandboxes.size
    }
}

private fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
