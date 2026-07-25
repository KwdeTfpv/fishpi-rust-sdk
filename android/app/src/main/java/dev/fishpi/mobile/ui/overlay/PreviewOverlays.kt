package dev.fishpi.mobile.ui.overlay

import dev.fishpi.mobile.*
import dev.fishpi.mobile.ui.components.FishPiIconButton
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal fun LinkPreviewOverlay(
    url: String,
    apiKey: String = "",
    onDismiss: () -> Unit,
) {
    val previewUrl = remember(url, apiKey) { authenticatedFishPiUrl(url, apiKey) }
    val requestHeaders = remember(url, apiKey) { fishPiAuthHeaders(url, apiKey) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FishPiTheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FishPiTheme.accent)
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = url,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            FishPiIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = "关闭网页预览",
                onClick = onDismiss,
                tint = Color.White,
                background = Color.White.copy(alpha = 0.16f),
            )
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                configureFishPiCookies(url, apiKey)
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    loadUrl(previewUrl, requestHeaders)
                }
            },
            update = { view ->
                configureFishPiCookies(url, apiKey)
                if (view.url != previewUrl) {
                    view.loadUrl(previewUrl, requestHeaders)
                }
            },
        )
    }
}

private fun authenticatedFishPiUrl(url: String, apiKey: String): String {
    if (apiKey.isBlank() || !isFishPiUrl(url)) {
        return url
    }
    return runCatching {
        val uri = Uri.parse(url)
        if (!uri.getQueryParameter("apiKey").isNullOrBlank()) {
            url
        } else {
            uri.buildUpon().appendQueryParameter("apiKey", apiKey).build().toString()
        }
    }.getOrDefault(url)
}

private fun fishPiAuthHeaders(url: String, apiKey: String): Map<String, String> {
    if (apiKey.isBlank() || !isFishPiUrl(url)) {
        return emptyMap()
    }
    return mapOf(
        "X-FishPi-ApiKey" to apiKey,
        "Authorization" to "Bearer $apiKey",
    )
}

private fun configureFishPiCookies(url: String, apiKey: String) {
    if (apiKey.isBlank() || !isFishPiUrl(url)) {
        return
    }
    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    listOf("apiKey", "fishpi_apiKey", "Key").forEach { name ->
        cookieManager.setCookie("https://fishpi.cn", "$name=$apiKey; Domain=fishpi.cn; Path=/; Secure")
    }
    cookieManager.flush()
}

private fun isFishPiUrl(url: String): Boolean {
    return runCatching {
        val host = Uri.parse(url).host.orEmpty().lowercase()
        host == "fishpi.cn" || host.endsWith(".fishpi.cn")
    }.getOrDefault(false)
}
