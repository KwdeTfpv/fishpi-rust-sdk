package dev.fishpi.mobile.realtime

import android.content.Context
import dev.fishpi.mobile.data.SessionStore

// 统一驱动连接与前台服务：登录态 + 前后台 + 保活开关 → 连/断/起服务/停服务
object RealtimeLifecycleController {
    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var foreground = false

    @Volatile
    private var session: RealtimeSession? = null

    @Volatile
    private var serviceRunning = false

    fun attach(context: Context) {
        appContext = context.applicationContext
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
        val current = session ?: return
        if (isForeground) {
            // 回到前台：若被限时/降级断开则重连
            if (!RealtimeConnectionManager.hasActiveChatHandle()) {
                RealtimeConnectionManager.start(current)
            }
        } else if (!keepAliveEnabled()) {
            // 省电模式：进后台即断
            RealtimeConnectionManager.stop()
        }
        reconcileService()
    }

    fun onKeepAlivePrefChanged() {
        reconcileService()
        val current = session
        if (current != null && !RealtimeConnectionManager.hasActiveChatHandle()) {
            RealtimeConnectionManager.start(current)
        }
    }

    private fun reconcileService() {
        val context = appContext ?: return
        val shouldRun = session != null && keepAliveEnabled()
        if (shouldRun == serviceRunning) return
        if (shouldRun) {
            RealtimeKeepAliveService.start(context)
        } else {
            RealtimeKeepAliveService.stop(context)
        }
        serviceRunning = shouldRun
    }

    private fun stopEverything() {
        RealtimeConnectionManager.stop()
        appContext?.let { RealtimeKeepAliveService.stop(it) }
        serviceRunning = false
    }

    private fun keepAliveEnabled(): Boolean {
        val context = appContext ?: return false
        return SessionStore(context).getNotificationPrefs().keepAlive
    }
}
