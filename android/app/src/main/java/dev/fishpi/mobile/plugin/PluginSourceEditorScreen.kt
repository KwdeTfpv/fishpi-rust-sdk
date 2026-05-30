package dev.fishpi.mobile.plugin

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import dev.fishpi.mobile.FishPiNotifier
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.chatui.ChatMarkdownRenderCache
import dev.fishpi.mobile.chatui.MarkwonContentRenderer
import dev.fishpi.mobile.chatui.MarkwonContentStyle
import dev.fishpi.mobile.ui.components.FishPiIconButton
import kotlinx.coroutines.Job
import org.json.JSONObject
import org.json.JSONTokener

@Composable
internal fun PluginSourceEditorScreen(
    plugin: PluginInfo,
    manager: PluginManager,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    var originalSource by remember(plugin.fileName) { mutableStateOf("") }
    var source by remember(plugin.fileName) { mutableStateOf("") }
    var selectedTab by remember(plugin.fileName) { mutableStateOf(0) }
    var loadingError by remember(plugin.fileName) { mutableStateOf<String?>(null) }
    var sourceLoaded by remember(plugin.fileName) { mutableStateOf(false) }
    var closeConfirmOpen by remember(plugin.fileName) { mutableStateOf(false) }
    var readEditorSource by remember(plugin.fileName) {
        mutableStateOf<(((String) -> Unit) -> Unit)?>(null)
    }

    fun readCurrentSource(callback: (String) -> Unit) {
        readEditorSource?.invoke(callback) ?: callback(source)
    }

    fun requestClose() {
        readCurrentSource { currentSource ->
            if (currentSource != originalSource) {
                closeConfirmOpen = true
            } else {
                onDismiss()
            }
        }
    }

    LaunchedEffect(plugin.fileName) {
        runCatching { manager.readPluginSource(plugin.fileName) }
            .onSuccess {
                originalSource = it
                source = it
                sourceLoaded = true
                loadingError = null
            }
            .onFailure {
                sourceLoaded = false
                loadingError = it.message ?: "读取插件源码失败"
            }
    }

    BackHandler(onBack = ::requestClose)

    if (closeConfirmOpen) {
        AlertDialog(
            onDismissRequest = { closeConfirmOpen = false },
            title = { Text("放弃修改？") },
            text = { Text("当前插件源码还没有保存，关闭后会丢失本次修改。") },
            confirmButton = {
                TextButton(onClick = {
                    closeConfirmOpen = false
                    onDismiss()
                }) {
                    Text("放弃", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { closeConfirmOpen = false }) { Text("继续编辑") }
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FishPiTheme.background)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FishPiTheme.surface)
                    .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = plugin.name,
                        color = FishPiTheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = plugin.fileName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Button(
                    onClick = {
                        readCurrentSource { currentSource ->
                            runCatching { manager.savePluginSource(plugin.fileName, currentSource) }
                                .onSuccess {
                                    source = currentSource
                                    originalSource = currentSource
                                    onSaved()
                                    FishPiNotifier.success("已保存并重载插件")
                                }
                                .onFailure { error ->
                                    FishPiNotifier.error("保存失败: ${error.message ?: "未知错误"}")
                                }
                        }
                    },
                    enabled = sourceLoaded && loadingError == null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("保存", fontSize = 13.sp)
                }
                FishPiIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "关闭源码编辑",
                    onClick = ::requestClose,
                    sizeDp = 34,
                    iconSizeDp = 18,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            TabRow(selectedTabIndex = selectedTab, containerColor = FishPiTheme.surface) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("编辑") })
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        readCurrentSource { currentSource ->
                            source = currentSource
                            selectedTab = 1
                        }
                    },
                    text = { Text("预览") },
                )
            }
            loadingError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(14.dp),
                    fontSize = 13.sp,
                )
            } ?: when (selectedTab) {
                0 -> PluginSourceEditor(
                    externalSource = originalSource,
                    onReaderChanged = { reader ->
                        readEditorSource = reader
                    },
                    modifier = Modifier.weight(1f),
                )
                else -> PluginSourcePreview(
                    source = source,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
@SuppressLint("SetJavaScriptEnabled")
private fun PluginSourceEditor(
    externalSource: String,
    onReaderChanged: ((((String) -> Unit) -> Unit)?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestExternalSource = rememberUpdatedState(externalSource)
    var editorWebView by remember { mutableStateOf<WebView?>(null) }
    val editorSource = remember { arrayOfNulls<String>(1) }

    fun decodeJavascriptString(value: String?): String {
        if (value.isNullOrBlank() || value == "null") return ""
        return runCatching { JSONTokener(value).nextValue() as? String }
            .getOrNull()
            ?: ""
    }

    fun readCurrentSource(callback: (String) -> Unit) {
        val view = editorWebView
        if (view == null) {
            callback(editorSource[0] ?: "")
            return
        }
        view.evaluateJavascript(
            "window.fishpiGetSource ? window.fishpiGetSource() : ''",
        ) { encoded ->
            val value = decodeJavascriptString(encoded)
            editorSource[0] = value
            callback(value)
        }
    }

    DisposableEffect(Unit) {
        onReaderChanged(::readCurrentSource)
        onDispose { onReaderChanged(null) }
    }

    fun pushSourceToEditor(view: WebView?, value: String, force: Boolean = false) {
        if (!force && editorSource[0] == value) return
        editorSource[0] = value
        view?.evaluateJavascript(
            "if (window.fishpiSetSource) window.fishpiSetSource(${JSONObject.quote(value)})",
            null,
        )
    }

    fun pushEditorSize(view: WebView?) {
        if (view == null || view.height <= 0) return
        val heightCssPx = view.height / view.resources.displayMetrics.density
        view.evaluateJavascript(
            "if (window.fishpiSetEditorHeight) window.fishpiSetEditorHeight(${heightCssPx.toInt()})",
            null,
        )
    }

    LaunchedEffect(externalSource, editorWebView) {
        pushSourceToEditor(editorWebView, externalSource, force = true)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    editorWebView = this
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    setBackgroundColor(android.graphics.Color.rgb(250, 251, 248))
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            pushEditorSize(view)
                            pushSourceToEditor(view, latestExternalSource.value, force = true)
                        }
                    }
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = false
                    settings.allowFileAccess = true
                    settings.blockNetworkLoads = true
                    settings.builtInZoomControls = false
                    settings.displayZoomControls = false
                    addOnLayoutChangeListener { changedView, _, _, _, _, _, _, _, _ ->
                        pushEditorSize(changedView as? WebView)
                    }
                    loadUrl("file:///android_asset/codemirror-editor/index.html")
                }
            },
            update = { view ->
                pushEditorSize(view)
            },
            onRelease = { view ->
                if (editorWebView === view) {
                    editorWebView = null
                }
                view.stopLoading()
                view.loadUrl("about:blank")
                view.destroy()
            },
        )
    }
}

@Composable
private fun PluginSourcePreview(
    source: String,
    modifier: Modifier = Modifier,
) {
    PluginCodePreview(
        source = source,
        language = "javascript",
        contentKeyPrefix = "plugin-source",
        modifier = modifier,
    )
}

@Composable
internal fun PluginCodePreview(
    source: String,
    language: String,
    contentKeyPrefix: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cache = remember { ChatMarkdownRenderCache(maxEntries = 20, maxChars = 300_000) }
    val markdown = remember(source, language) { source.toCodeFence(language) }
    val style = MarkwonContentStyle(
        textColor = MaterialTheme.colorScheme.onSurface.toArgb(),
        weakTextColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb(),
        accentColor = MaterialTheme.colorScheme.primary.toArgb(),
        codeBackgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh.toArgb(),
        textSizeSp = 13f,
        lineSpacingMultiplier = 1.05f,
    )
    val renderer = remember(context, style, cache, scope) {
        MarkwonContentRenderer(
            context = context,
            style = style,
            cache = cache,
            scope = scope,
            onLinkClick = {},
            onMentionClick = {},
        )
    }
    var renderJob by remember(markdown) { mutableStateOf<Job?>(null) }
    DisposableEffect(markdown) {
        onDispose {
            renderJob?.cancel()
            renderJob = null
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { TextView(it) },
            update = { view ->
                renderJob?.cancel()
                renderJob = renderer.renderInto(
                    textView = view,
                    contentKey = "$contentKeyPrefix-${language}-${source.hashCode()}",
                    markdown = markdown,
                )
            },
            onRelease = { view ->
                renderJob?.cancel()
                renderJob = null
                renderer.clear(view)
            },
        )
    }
}

private fun String.toCodeFence(language: String): String {
    val fence = "````"
    return "$fence $language\n$this\n$fence"
}
