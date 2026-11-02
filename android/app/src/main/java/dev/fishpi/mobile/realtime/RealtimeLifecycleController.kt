package dev.fishpi.mobile.realtime

import android.content.Context
import dev.fishpi.mobile.data.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class KeepAliveStatus {
    Stopped,
    Starting,
    Running,
    StoppedBySystem,
    NoSession,
    Error,
}

enum class KeepAliveStopReason {
    User,
    Timeout,
    NoSession,
    Error,
    System,
}

object RealtimeLifecycleController {
    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var foreground = false

    @Volatile
    private var session: RealtimeSession? = null

    @Volatile
    private var serviceRunning = false

    @Volatile
    private var serviceStarting = false

    @Volatile
    private var lastNetworkRestartAt = 0L

    private val _status = MutableStateFlow(KeepAliveStatus.Stopped)
    val status: StateFlow<KeepAliveStatus> = _status.asStateFlow()

    fun attach(context: Context) {
        appContext = context.applicationContext
        RealtimeNetworkMonitor.register(context.applicationContext)
    }

    fun restoreSession(context: Context): RealtimeSession? {
        val store = SessionStore(context)
        val apiKey = store.getApiKey().trim()
        if (apiKey.isBlank()) return null
        val account = store.getAccounts().firstOrNull { it.apiKey == apiKey }
        return RealtimeSession(apiKey, account?.userName.orEmpty())
    }

    fun onSession(next: RealtimeSession?) {
        session = next
        if (next == null) {
            stopEverything()
        } else {
            RealtimeConnectionManager.start(next)
            reconcileService()
        }
    }

    fun onForeground(isForeground: Boolean) {
        foreground = isForeground
        val current = session
        if (isForeground) {
            if (current != null && !RealtimeConnectionManager.hasActiveChatHandle()) {
                RealtimeConnectionManager.start(current)
            }
        } else if (!keepAliveEnabled()) {
            RealtimeConnectionManager.stop()
        } else if (current != null && !RealtimeConnectionManager.hasActiveChatHandle()) {
            RealtimeConnectionManager.start(current)
        }
        reconcileService()
    }

    fun onKeepAlivePrefChanged() {
        reconcileService()
        val current = session
        if (!foreground && !keepAliveEnabled()) {
            RealtimeConnectionManager.stop()
            return
        }
        if (current != null &&
            (foreground || keepAliveEnabled()) &&
            !RealtimeConnectionManager.hasActiveChatHandle()
        ) {
            RealtimeConnectionManager.start(current)
        }
    }

    fun onServiceStarted() {
        serviceStarting = false
        serviceRunning = true
        _status.value = KeepAliveStatus.Running
        val current = session ?: restoreSession(appContext ?: return)
        if (current != null) {
            session = current
            if (!RealtimeConnectionManager.hasActiveChatHandle()) {
                RealtimeConnectionManager.start(current)
            }
        }
    }

    fun onServiceStopped(reason: KeepAliveStopReason) {
        serviceStarting = false
        serviceRunning = false
        _status.value = when (reason) {
            KeepAliveStopReason.Timeout -> KeepAliveStatus.StoppedBySystem
            KeepAliveStopReason.NoSession -> KeepAliveStatus.NoSession
            KeepAliveStopReason.Error -> KeepAliveStatus.Error
            KeepAliveStopReason.User,
            KeepAliveStopReason.System,
            -> KeepAliveStatus.Stopped
        }
    }

    fun onNotificationChannelsReset() {
        val context = appContext ?: return
        if (keepAliveEnabled()) {
            RealtimeKeepAliveService.refresh(context)
        }
    }

    fun onNetworkAvailable() {
        val current = session ?: return
        if (!foreground && !keepAliveEnabled()) return
        val now = System.currentTimeMillis()
        if (now - lastNetworkRestartAt < NETWORK_RESTART_INTERVAL_MS) return
        lastNetworkRestartAt = now
        RealtimeConnectionManager.restart()
        reconcileService()
        if (!RealtimeConnectionManager.hasActiveChatHandle()) {
            RealtimeConnectionManager.start(current)
        }
    }

    private fun reconcileService() {
        val context = appContext ?: return
        val shouldRun = keepAliveEnabled() && (session != null || restoreSession(context) != null)
        if (!shouldRun) {
            if (serviceRunning) {
                RealtimeKeepAliveService.stop(context)
                serviceRunning = false
            }
            serviceStarting = false
            _status.value = KeepAliveStatus.Stopped
            return
        }
        if (serviceRunning || serviceStarting) return
        serviceStarting = true
        _status.value = KeepAliveStatus.Starting
        if (!RealtimeKeepAliveService.start(context)) {
            serviceStarting = false
            _status.value = KeepAliveStatus.Error
        }
    }

    private fun stopEverything() {
        RealtimeConnectionManager.stop()
        appContext?.let { RealtimeKeepAliveService.stop(it) }
        serviceRunning = false
        serviceStarting = false
        _status.value = KeepAliveStatus.Stopped
    }

    private fun keepAliveEnabled(): Boolean {
        val context = appContext ?: return false
        return SessionStore(context).getNotificationPrefs().keepAlive
    }

    private const val NETWORK_RESTART_INTERVAL_MS = 5_000L
}
