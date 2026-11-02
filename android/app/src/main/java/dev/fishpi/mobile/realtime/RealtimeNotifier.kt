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

object RealtimeNotifier {
    const val CHANNEL_KEEPALIVE = "fishpi.keepalive"
    const val CHANNEL_CHAT = "fishpi.chat"
    const val CHANNEL_CHAT_MENTIONS = "fishpi.chat.mentions"
    const val CHANNEL_PRIVATE = "fishpi.private"
    const val CHANNEL_SYSTEM = "fishpi.system"
    const val KEEPALIVE_NOTIFICATION_ID = 1001

    private const val LEGACY_CHANNEL_MESSAGES = "fishpi.messages"

    enum class MessageCategory(
        val channelId: String,
        val channelName: String,
        val importance: Int,
        val description: String,
    ) {
        Chat(
            channelId = CHANNEL_CHAT,
            channelName = "聊天室消息",
            importance = NotificationManager.IMPORTANCE_HIGH,
            description = "聊天室普通消息",
        ),
        ChatMention(
            channelId = CHANNEL_CHAT_MENTIONS,
            channelName = "聊天室提及与关键词",
            importance = NotificationManager.IMPORTANCE_HIGH,
            description = "有人提到你、引用你或命中关键词",
        ),
        Private(
            channelId = CHANNEL_PRIVATE,
            channelName = "私聊消息",
            importance = NotificationManager.IMPORTANCE_HIGH,
            description = "收到私聊消息",
        ),
        System(
            channelId = CHANNEL_SYSTEM,
            channelName = "系统通知",
            importance = NotificationManager.IMPORTANCE_DEFAULT,
            description = "回复、关注、积分等系统通知",
        ),
    }

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.deleteNotificationChannel(LEGACY_CHANNEL_MESSAGES)

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
        manager.createNotificationChannel(keepAlive)

        MessageCategory.values().forEach { category ->
            val channel = NotificationChannel(
                category.channelId,
                category.channelName,
                category.importance,
            ).apply {
                description = category.description
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun resetChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.deleteNotificationChannel(CHANNEL_KEEPALIVE)
        MessageCategory.values().forEach { manager.deleteNotificationChannel(it.channelId) }
        manager.deleteNotificationChannel(LEGACY_CHANNEL_MESSAGES)
        ensureChannels(context)
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
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(contentIntent(context, RealtimeNavTarget.Notice, 0))
            .build()
    }

    fun notify(
        context: Context,
        id: Int,
        title: String,
        body: String,
        target: RealtimeNavTarget,
        category: MessageCategory,
    ): Boolean {
        ensureChannels(context)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return false
        }
        val notification = NotificationCompat.Builder(context, category.channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(R.drawable.ic_client_chat)
            .setLargeIcon(appLogoBitmap(context))
            .setAutoCancel(true)
            .setCategory(
                if (category == MessageCategory.System) {
                    NotificationCompat.CATEGORY_STATUS
                } else {
                    NotificationCompat.CATEGORY_MESSAGE
                },
            )
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(contentIntent(context, target, id))
            .build()
        return runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
            true
        }.getOrElse {
            false
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
