package dev.fishpi.mobile.ui.overlay

import dev.fishpi.mobile.*
import dev.fishpi.mobile.ui.components.FishPiIconButton
import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import java.net.URL
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.Text

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

@Composable
internal fun ImagePreviewOverlay(
    imageUrl: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imageLoader = rememberFishPiImageLoader()
    var scale by remember(imageUrl) { mutableFloatStateOf(1f) }
    var offset by remember(imageUrl) { mutableStateOf(Offset.Zero) }
    var isSaving by remember(imageUrl) { mutableStateOf(false) }
    var saveStatus by remember(imageUrl) { mutableStateOf<String?>(null) }
    val fullSizeImageRequest = remember(context, imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .size(Size.ORIGINAL)
            .scale(Scale.FIT)
            .precision(Precision.EXACT)
            .build()
    }

    fun saveImage() {
        if (isSaving) {
            return
        }
        isSaving = true
        saveStatus = "正在保存..."
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.saveRemoteImageToPictures(imageUrl)
                }
            }.onSuccess {
                saveStatus = "已保存到相册"
            }.onFailure {
                saveStatus = it.message ?: "保存失败"
            }
            isSaving = false
        }
    }

    val writePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            saveImage()
        } else {
            saveStatus = "需要存储权限才能保存图片"
        }
    }

    fun requestSaveImage() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            writePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            saveImage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .pointerInput(imageUrl) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        event.changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = fullSizeImageRequest,
            imageLoader = imageLoader,
            contentDescription = "图片预览",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .pointerInput(imageUrl) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = scale
                        val nextScale = (scale * zoom).coerceIn(1f, 5f)
                        scale = nextScale
                        offset = if (nextScale == 1f) {
                            Offset.Zero
                        } else {
                            val scaleChange = nextScale / oldScale
                            val anchoredOffset = centroid - (centroid - offset) * scaleChange + pan
                            clampImageOffset(
                                offset = anchoredOffset,
                                scale = nextScale,
                                width = size.width.toFloat(),
                                height = size.height.toFloat(),
                            )
                        }
                    }
                }
                .pointerInput(imageUrl) {
                    detectTapGestures(
                        onDoubleTap = {
                            scale = 1f
                            offset = Offset.Zero
                        },
                    )
                },
        )
        saveStatus?.let { status ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(18.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(text = status, color = Color.White)
            }
        }
        FishPiIconButton(
            icon = Icons.Rounded.Download,
            contentDescription = "保存图片",
            onClick = { requestSaveImage() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            tint = Color.White,
            background = Color.Black.copy(alpha = 0.42f),
        )
        FishPiIconButton(
            icon = Icons.Rounded.Close,
            contentDescription = "关闭图片预览",
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            tint = Color.White,
            background = Color.Black.copy(alpha = 0.42f),
        )
    }
}

private fun Context.saveRemoteImageToPictures(imageUrl: String): Uri {
    val extension = imageUrl.substringBefore('?')
        .substringAfterLast('.', "jpg")
        .lowercase()
        .takeIf { it in setOf("jpg", "jpeg", "png", "webp", "gif") }
        ?: "jpg"
    val mimeType = when (extension) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        else -> "image/jpeg"
    }
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "fishpi-${System.currentTimeMillis()}.$extension")
        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/FishPi")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val resolver = contentResolver
    val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)) {
        "无法创建图片文件"
    }
    try {
        URL(imageUrl).openStream().use { input ->
            resolver.openOutputStream(uri).use { output ->
                requireNotNull(output) { "无法写入图片文件" }
                input.copyTo(output)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return uri
    } catch (error: Throwable) {
        resolver.delete(uri, null, null)
        throw error
    }
}

private fun clampImageOffset(
    offset: Offset,
    scale: Float,
    width: Float,
    height: Float,
): Offset {
    val horizontalBound = max(0f, (width * scale - width) / 2f + width * 0.25f)
    val verticalBound = max(0f, (height * scale - height) / 2f + height * 0.25f)
    return Offset(
        x = offset.x.coerceIn(-horizontalBound, horizontalBound),
        y = offset.y.coerceIn(-verticalBound, verticalBound),
    )
}

