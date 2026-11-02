package dev.fishpi.mobile.realtime

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network

object RealtimeNetworkMonitor {
    private var registered = false

    fun register(context: Context) {
        if (registered) return
        registered = true
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return
        runCatching {
            manager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    RealtimeLifecycleController.onNetworkAvailable()
                }
            })
        }
    }
}
