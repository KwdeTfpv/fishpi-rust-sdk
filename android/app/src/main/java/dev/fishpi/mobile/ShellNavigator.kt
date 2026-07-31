package dev.fishpi.mobile

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.Composable
import dev.fishpi.mobile.data.ArticleSummary

enum class FishTab(val title: String) {
    Home("首页"),
    Article("帖子"),
    Chat("聊天"),
    Me("我的"),
}

enum class HomeSubPane {
    Breezemoon,
    Store,
}

sealed interface ShellOverlay {
    /** 聊天室详情。 */
    data object ChatRoom : ShellOverlay

    /** 通知中心。 */
    data object Notice : ShellOverlay

    /** 用户资料浮层。 */
    data class Profile(val username: String) : ShellOverlay

    data class HomePane(val pane: HomeSubPane) : ShellOverlay

    data class Article(val articleId: String, val summary: ArticleSummary? = null) : ShellOverlay
}

@Stable
class ShellNavigator {
    var selectedTab by mutableStateOf(FishTab.Home)
        private set

    val overlays = mutableStateListOf<ShellOverlay>()

    val topOverlay: ShellOverlay? get() = overlays.lastOrNull()

    val onMainLayer: Boolean get() = overlays.isEmpty()

    fun selectTab(tab: FishTab) {
        selectedTab = tab
    }

    fun push(overlay: ShellOverlay) {
        overlays.add(overlay)
    }

    fun replaceTop(overlay: ShellOverlay) {
        if (overlays.isNotEmpty()) {
            overlays[overlays.lastIndex] = overlay
        } else {
            overlays.add(overlay)
        }
    }

    fun back(): Boolean {
        if (overlays.isNotEmpty()) {
            overlays.removeAt(overlays.lastIndex)
            return true
        }
        return false
    }

    fun clearOverlays() {
        overlays.clear()
    }
}

@Composable
fun rememberShellNavigator(): ShellNavigator = remember { ShellNavigator() }
