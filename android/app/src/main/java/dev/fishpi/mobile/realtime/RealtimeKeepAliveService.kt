package dev.fishpi.mobile.realtime

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

class RealtimeKeepAliveService : Service() {

    private var stopReason = KeepAliveStopReason.User

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopReason = KeepAliveStopReason.User
        val notification = RealtimeNotifier.buildKeepAliveNotification(this)
        val started = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    RealtimeNotifier.KEEPALIVE_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            } else {
                startForeground(RealtimeNotifier.KEEPALIVE_NOTIFICATION_ID, notification)
            }
        }.isSuccess
        if (!started) {
            stopReason = KeepAliveStopReason.Error
            RealtimeLifecycleController.onServiceStopped(stopReason)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val current = RealtimeConnectionManager.currentSession()
            ?: RealtimeLifecycleController.restoreSession(this)
        if (current == null) {
            stopReason = KeepAliveStopReason.NoSession
            RealtimeLifecycleController.onServiceStopped(stopReason)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        RealtimeConnectionManager.start(current)
        RealtimeLifecycleController.onServiceStarted()
        return START_STICKY
    }

    override fun onTimeout(startId: Int) {
        stopReason = KeepAliveStopReason.Timeout
        RealtimeLifecycleController.onServiceStopped(stopReason)
        stopSelf()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        stopReason = KeepAliveStopReason.Timeout
        RealtimeLifecycleController.onServiceStopped(stopReason)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        RealtimeLifecycleController.onServiceStopped(stopReason)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    companion object {
        fun start(context: Context): Boolean {
            val intent = Intent(context, RealtimeKeepAliveService::class.java)
            return runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                true
            }.getOrDefault(false)
        }

        fun refresh(context: Context): Boolean = start(context)

        fun stop(context: Context) {
            context.stopService(Intent(context, RealtimeKeepAliveService::class.java))
        }
    }
}
