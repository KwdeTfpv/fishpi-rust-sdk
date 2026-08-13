package dev.fishpi.mobile

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dev.fishpi.mobile.realtime.RealtimeLifecycleController
import dev.fishpi.mobile.realtime.RealtimeNotificationDispatcher
import dev.fishpi.mobile.realtime.RealtimeNotifier

class FishPiApplication : Application() {

    lateinit var notificationDispatcher: RealtimeNotificationDispatcher
        private set

    override fun onCreate() {
        super.onCreate()
        RealtimeNotifier.ensureChannels(this)
        RealtimeLifecycleController.attach(this)

        notificationDispatcher = RealtimeNotificationDispatcher(this).also { it.start() }

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                notificationDispatcher.appInForeground = true
                RealtimeLifecycleController.onForeground(true)
            }

            override fun onStop(owner: LifecycleOwner) {
                notificationDispatcher.appInForeground = false
                RealtimeLifecycleController.onForeground(false)
            }
        })
    }
}
