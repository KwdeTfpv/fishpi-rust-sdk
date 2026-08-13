package dev.fishpi.mobile.realtime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.fishpi.mobile.MainActivity
import dev.fishpi.mobile.R

// 负责创建通知渠道、构建点击 intent 并弹出系统通知
object RealtimeNotifier {
    const val CHANNEL_KEEPALIVE = "fishpi.keepalive"
    const val CHANNEL_MESSAGES = "fishpi.messages"
    const val KEEPALIVE_NOTIFICATION_ID = 1001

    private const val GROUP_MESSAGES = "fishpi.messages.group"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val keepAlive = NotificationChannel(
            CHANNEL_KEEPALIVE,
            "后台保活",
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            description = "维持聊天室长连接的常驻通知"
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
        }
        val messages = NotificationChannel(
            CHANNEL_MESSAGES,
            "消息通知",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "聊天室、私聊与系统通知"
        }
        manager.createNotificationChannel(keepAlive)
        manager.createNotificationChannel(messages)
    }

    fun buildKeepAliveNotification(context: Context): Notification {
        ensureChannels(context)
        return NotificationCompat.Builder(context, CHANNEL_KEEPALIVE)
            .setContentTitle("摸鱼派")
            .setContentText("服务运行中")
            .setSmallIcon(R.drawable.ic_client_chat)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(contentIntent(context, RealtimeNavTarget.Notice, 0))
            .build()
    }

    fun notify(
        context: Context,
        id: Int,
        title: String,
        body: String,
        target: RealtimeNavTarget,
    ) {
        ensureChannels(context)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(R.drawable.ic_client_chat)
            .setLargeIcon(appLogoBitmap(context))
            .setAutoCancel(true)
            .setGroup(GROUP_MESSAGES)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent(context, target, id))
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }

    private fun appLogoBitmap(context: Context): android.graphics.Bitmap? = runCatching {
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_fishpi_logo)
            ?: return null
        val size = 128
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        bitmap
    }.getOrNull()

    private fun contentIntent(
        context: Context,
        target: RealtimeNavTarget,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            target.writeTo(this)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
