package dev.fishpi.mobile.realtime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dev.fishpi.mobile.data.SessionStore

class RealtimeBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> Unit

            else -> return
        }
        val store = SessionStore(context)
        if (!store.getNotificationPrefs().keepAlive) return
        if (store.getApiKey().isBlank()) return
        if (Build.VERSION.SDK_INT >= 35 && intent.action == Intent.ACTION_BOOT_COMPLETED) {
            RealtimeNotifier.notify(
                context = context,
                id = 7101,
                title = "后台保活需要恢复",
                body = "点击打开摸鱼派，恢复后台在线",
                target = RealtimeNavTarget.Notice,
                category = RealtimeNotifier.MessageCategory.System,
            )
            return
        }
        RealtimeLifecycleController.attach(context)
        val session = RealtimeLifecycleController.restoreSession(context) ?: return
        RealtimeLifecycleController.onSession(session)
        RealtimeKeepAliveService.start(context)
    }
}
