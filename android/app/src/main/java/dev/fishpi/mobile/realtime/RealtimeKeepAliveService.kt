package dev.fishpi.mobile.realtime

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

// 前台服务：仅在保活开关打开时启动，持有进程存活以维持长连接
class RealtimeKeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = RealtimeNotifier.buildKeepAliveNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                RealtimeNotifier.KEEPALIVE_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(RealtimeNotifier.KEEPALIVE_NOTIFICATION_ID, notification)
        }

        val current = RealtimeConnectionManager.currentSession()
        if (current != null) {
            RealtimeConnectionManager.start(current)
        }
        return START_STICKY
    }

    // Android 15+ dataSync 前台服务限时到点回调：优雅降级，不硬撑
    override fun onTimeout(startId: Int) {
        stopSelf()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, RealtimeKeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RealtimeKeepAliveService::class.java))
        }
    }
}
