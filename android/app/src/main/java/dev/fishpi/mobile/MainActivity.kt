package dev.fishpi.mobile

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import android.content.Intent
import androidx.compose.runtime.DisposableEffect
import dev.fishpi.mobile.data.FishPiNative
import dev.fishpi.mobile.realtime.PendingRealtimeNavigation
import dev.fishpi.mobile.realtime.RealtimeNavTarget

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FishPiNative.setChatRoomClientType("Rust", BuildConfig.VERSION_NAME)
        handleNavIntent(intent)

        setContent {
            val darkMode = isSystemInDarkTheme()

            DisposableEffect(darkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) {
                        darkMode
                    },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) {
                        darkMode
                    },
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
                onDispose {}
            }

            FishPiApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavIntent(intent)
    }

    private fun handleNavIntent(intent: Intent?) {
        if (intent != null && intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) {
            RealtimeNavTarget.clear(intent)
            return
        }
        val target = RealtimeNavTarget.fromIntent(intent) ?: return
        RealtimeNavTarget.clear(intent)
        PendingRealtimeNavigation.submit(target)
    }
}
