package dev.fishpi.mobile

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import dev.fishpi.mobile.data.FishPiNative

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FishPiNative.setChatRoomClientType("Rust", BuildConfig.VERSION_NAME)
        preferHighestRefreshRate()

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

    override fun onResume() {
        super.onResume()
        preferHighestRefreshRate()
    }

    private fun preferHighestRefreshRate() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        val modes = currentDisplayModes()
        val currentMode = currentDisplayMode()
        val sameSizeModes = modes.filter {
            currentMode == null || (it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight)
        }
        val bestMode = (sameSizeModes.ifEmpty { modes })
            .filter { it.modeId != 0 }
            .maxByOrNull { it.refreshRate }
            ?: return

        val attrs = window.attributes
        if (attrs.preferredDisplayModeId != bestMode.modeId) {
            attrs.preferredDisplayModeId = bestMode.modeId
            window.attributes = attrs
        }

    }

    private fun currentDisplayMode(): Display.Mode? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.mode
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.mode
        }
    }

    private fun currentDisplayModes(): List<Display.Mode> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.supportedModes
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.supportedModes
        }?.toList().orEmpty()
    }
}
