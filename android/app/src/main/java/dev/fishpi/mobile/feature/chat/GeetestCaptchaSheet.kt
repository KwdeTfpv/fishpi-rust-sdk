package dev.fishpi.mobile.feature.chat

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import dev.fishpi.mobile.data.FishPiApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

private class CaptchaBridge(
    private val onResult: (String) -> Unit,
    private val onFailed: (String) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onResult(result: String) {
        mainHandler.post { onResult(result) }
    }

    @JavascriptInterface
    fun onFailed(message: String) {
        mainHandler.post { onFailed(message) }
    }
}

@Composable
internal fun GeetestCaptchaSheet(
    onResult: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var captchaPage by remember { mutableStateOf<Pair<String, String>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var componentError by remember { mutableStateOf<String?>(null) }
    val pendingCaptchaId = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val prepared = withContext(Dispatchers.IO) {
            runCatching {
                val options = FishPiApiClient.shared.v3CaptchaOptions()
                val html = context.assets.open("captcha_gateway.html").bufferedReader().use { it.readText() }
                options.captchaId to html
            }.getOrNull()
        }
        if (prepared == null || prepared.first.isBlank()) {
            loadError = "无法加载人机验证组件，请稍后重试"
        } else {
            captchaPage = prepared
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("人机验证", style = MaterialTheme.typography.titleMedium)
                when {
                    loadError != null -> {
                        Text(loadError.orEmpty(), color = MaterialTheme.colorScheme.error)
                    }
                    captchaPage == null -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("正在加载验证组件…")
                        }
                    }
                    else -> {
                        componentError?.let { message ->
                            Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        AndroidView(
                            factory = { viewContext ->
                                WebView(viewContext).apply {
                                    settings.javaScriptEnabled = true
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            val captchaId = pendingCaptchaId.value ?: return
                                            if (!CAPTCHA_ID_FORMAT.matcher(captchaId).matches()) {
                                                return
                                            }
                                            view?.evaluateJavascript("startGeetest('$captchaId')", null)
                                        }
                                    }
                                }
                            },
                            update = { view ->
                                val (captchaId, html) = captchaPage ?: return@AndroidView
                                if (view.tag != captchaId) {
                                    view.tag = captchaId
                                    pendingCaptchaId.value = captchaId
                                    view.addJavascriptInterface(
                                        CaptchaBridge(
                                            onResult = { componentError = null; onResult(it) },
                                            onFailed = { message -> componentError = message },
                                        ),
                                        "AndroidCaptcha",
                                    )
                                    view.loadDataWithBaseURL(
                                        "https://fishpi.cn/",
                                        html,
                                        "text/html",
                                        "utf-8",
                                        null,
                                    )
                                }
                            },
                            onRelease = { view ->
                                view.stopLoading()
                                view.destroy()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                }
            }
        }
    }
}

private val CAPTCHA_ID_FORMAT: Pattern = Pattern.compile("[A-Za-z0-9_-]{4,128}")
