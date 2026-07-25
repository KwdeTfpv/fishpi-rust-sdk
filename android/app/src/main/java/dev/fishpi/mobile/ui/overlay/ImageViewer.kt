package dev.fishpi.mobile.ui.overlay

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import dev.fishpi.mobile.rememberFishPiImageLoader
import dev.fishpi.mobile.ui.components.FishPiIconButton
import java.io.File
import java.net.URL
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 一次图片查看请求：图片列表 + 起始索引。 */
internal data class ImageViewerRequest(
    val images: List<String>,
    val startIndex: Int,
)

/**
 * 全局图片查看器控制器。挂在 app 根部（见 [ImageViewerHost]），
 * 三处入口（聊天室 / 帖子 / 私聊）通过 [LocalImageViewer] 调用 [open]。
 */
internal class ImageViewerController {
    var request by mutableStateOf<ImageViewerRequest?>(null)
        private set

    fun open(images: List<String>, index: Int) {
        val cleaned = images.filter { it.isNotBlank() }
        if (cleaned.isEmpty()) return
        request = ImageViewerRequest(
            images = cleaned,
            startIndex = index.coerceIn(0, cleaned.lastIndex),
        )
    }

    fun open(url: String) {
        if (url.isBlank()) return
        request = ImageViewerRequest(listOf(url), 0)
    }

    fun dismiss() {
        request = null
    }
}

internal val LocalImageViewer = staticCompositionLocalOf<ImageViewerController> {
    error("ImageViewerController 未提供，请在上层包裹 ImageViewerHost")
}

/**
 * 在 app 根部包裹一次：提供 [LocalImageViewer]，并在内容之上渲染全屏查看器。
 */
@Composable
internal fun ImageViewerHost(content: @Composable () -> Unit) {
    val controller = remember { ImageViewerController() }
    CompositionLocalProvider(LocalImageViewer provides controller) {
        content()
        val request = controller.request
        AnimatedVisibility(
            visible = request != null,
            enter = fadeIn(FishPiMotionSpec),
            exit = fadeOut(FishPiMotionSpec),
        ) {
            // 记住最后一次非空请求，让退场动画期间仍有内容可渲染。
            val lastRequest = remember { mutableStateOf(request) }
            if (request != null) lastRequest.value = request
            lastRequest.value?.let {
                ImageViewer(request = it, onDismiss = { controller.dismiss() })
            }
        }
    }
}

private val FishPiMotionSpec = androidx.compose.animation.core.tween<Float>(durationMillis = 180)

@Composable
private fun ImageViewer(
    request: ImageViewerRequest,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imageLoader = rememberFishPiImageLoader()
    val images = request.images
    val pagerState = rememberPagerState(initialPage = request.startIndex) { images.size }

    var currentPage by remember { mutableIntStateOf(request.startIndex) }
    LaunchedEffectPage(pagerState) { currentPage = it }
    val currentUrl = images.getOrElse(currentPage) { images.first() }

    // 下滑关闭时整体背景透明度随位移变化。
    var dismissProgress by remember { mutableFloatStateOf(0f) }
    val bgAlpha = (1f - dismissProgress).coerceIn(0.3f, 1f)

    var isSaving by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf<String?>(null) }
    var isSharing by remember { mutableStateOf(false) }

    BackHandler(enabled = true) { onDismiss() }

    fun saveImage() {
        if (isSaving) return
        isSaving = true
        saveStatus = "正在保存..."
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { context.saveImageToPictures(currentUrl) }
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
        if (granted) saveImage() else saveStatus = "需要存储权限才能保存图片"
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

    fun shareImage() {
        if (isSharing) return
        isSharing = true
        saveStatus = "正在准备分享..."
        scope.launch {
            runCatching {
                val uri = withContext(Dispatchers.IO) { context.cacheImageForShare(currentUrl) }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = context.contentResolver.getType(uri) ?: "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(intent, "分享图片").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            }.onSuccess {
                saveStatus = null
            }.onFailure {
                saveStatus = it.message ?: "分享失败"
            }
            isSharing = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f * bgAlpha)),
        contentAlignment = Alignment.Center,
    ) {
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            ZoomableImage(
                url = images[page],
                imageLoader = imageLoader,
                onDismiss = onDismiss,
                onDismissProgress = { progress ->
                    // 仅当前页驱动背景淡出。
                    if (page == currentPage) dismissProgress = progress
                },
            )
        }

        // 顶部操作栏。
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FishPiIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = "关闭图片预览",
                onClick = onDismiss,
                tint = Color.White,
                background = Color.Black.copy(alpha = 0.42f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FishPiIconButton(
                    icon = Icons.Rounded.Share,
                    contentDescription = "分享图片",
                    onClick = { shareImage() },
                    enabled = !isSharing,
                    tint = Color.White,
                    background = Color.Black.copy(alpha = 0.42f),
                )
                FishPiIconButton(
                    icon = Icons.Rounded.Download,
                    contentDescription = "保存图片",
                    onClick = { requestSaveImage() },
                    enabled = !isSaving,
                    tint = Color.White,
                    background = Color.Black.copy(alpha = 0.42f),
                )
            }
        }

        // 多图页码指示。
        if (images.size > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(bottom = 28.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(text = "${currentPage + 1} / ${images.size}", color = Color.White)
            }
        }

        saveStatus?.let { status ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(text = status, color = Color.White)
            }
        }
    }
}

/** 单页：缩放 / 平移 / 双击缩放到点 / 下滑关闭。 */
@Composable
private fun ZoomableImage(
    url: String,
    imageLoader: coil3.ImageLoader,
    onDismiss: () -> Unit,
    onDismissProgress: (Float) -> Unit,
) {
    val context = LocalContext.current
    var scale by remember(url) { mutableFloatStateOf(1f) }
    var offset by remember(url) { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val viewportWidthPx = constraints.maxWidth
        val viewportHeightPx = constraints.maxHeight
        val request = remember(context, url, viewportWidthPx, viewportHeightPx) {
            ImageRequest.Builder(context)
                .data(url)
                .size(
                    if (viewportWidthPx > 0 && viewportHeightPx > 0) {
                        Size(viewportWidthPx, viewportHeightPx)
                    } else {
                        Size.ORIGINAL
                    },
                )
                .scale(Scale.FIT)
                .precision(Precision.INEXACT)
                .build()
        }

        SubcomposeAsyncImage(
            model = request,
            imageLoader = imageLoader,
            contentDescription = "图片预览",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
                .pointerInput(url) {
                    // 统一手势循环：双指缩放/平移 + 放大后单指平移 + 1x 时单指竖直下滑关闭。
                    // 关键：1x 且横向为主的单指拖动不消费，交给 HorizontalPager 翻页。
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var totalDx = 0f
                        var totalDy = 0f
                        var mode = DragMode.Undecided
                        do {
                            val event = awaitPointerEvent()
                            val pressedCount = event.changes.count { it.pressed }
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)

                            if (pressedCount >= 2) {
                                // 多指：缩放 + 平移，始终消费。
                                mode = DragMode.Transform
                                val oldScale = scale
                                val nextScale = (scale * zoomChange).coerceIn(1f, 5f)
                                scale = nextScale
                                offset = if (nextScale == 1f) {
                                    Offset.Zero
                                } else {
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val scaleChange = nextScale / oldScale
                                    val anchored = (offset + center - centroid) * scaleChange - center + centroid + panChange
                                    clampOffset(anchored, nextScale, size.width.toFloat(), size.height.toFloat())
                                }
                                event.changes.forEach { it.consume() }
                            } else if (pressedCount == 1) {
                                totalDx += panChange.x
                                totalDy += panChange.y
                                if (mode == DragMode.Undecided && (abs(totalDx) > 8f || abs(totalDy) > 8f)) {
                                    mode = when {
                                        scale > 1f -> DragMode.Pan
                                        abs(totalDy) > abs(totalDx) -> DragMode.Dismiss
                                        else -> DragMode.YieldToPager
                                    }
                                }
                                when (mode) {
                                    DragMode.Pan -> {
                                        offset = clampOffset(
                                            offset + panChange,
                                            scale,
                                            size.width.toFloat(),
                                            size.height.toFloat(),
                                        )
                                        event.changes.forEach { it.consume() }
                                    }
                                    DragMode.Dismiss -> {
                                        offset = Offset(0f, totalDy)
                                        onDismissProgress((abs(totalDy) / (size.height * 0.5f)).coerceIn(0f, 1f))
                                        event.changes.forEach { it.consume() }
                                    }
                                    else -> Unit // YieldToPager / Undecided：不消费，翻页生效。
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        // 抬手结算。
                        if (mode == DragMode.Dismiss) {
                            if (abs(totalDy) > size.height * 0.18f) {
                                onDismiss()
                            } else {
                                offset = Offset.Zero
                                onDismissProgress(0f)
                            }
                        }
                    }
                }
                .pointerInput(url) {
                    detectTapGestures(
                        onDoubleTap = { tap ->
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                val target = 2.5f
                                scale = target
                                val center = Offset(size.width / 2f, size.height / 2f)
                                offset = clampOffset(
                                    (center - tap) * (target - 1f),
                                    target,
                                    size.width.toFloat(),
                                    size.height.toFloat(),
                                )
                            }
                        },
                    )
                },
        ) {
            when (painter.state.value) {
                is coil3.compose.AsyncImagePainter.State.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
                is coil3.compose.AsyncImagePainter.State.Error -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Rounded.BrokenImage,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                        )
                        Text(text = "图片加载失败", color = Color.White.copy(alpha = 0.7f))
                    }
                }
                else -> SubcomposeAsyncImageContent()
            }
        }
    }
}

@Composable
private fun LaunchedEffectPage(
    pagerState: androidx.compose.foundation.pager.PagerState,
    onPage: (Int) -> Unit,
) {
    androidx.compose.runtime.LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { onPage(it) }
    }
}

private enum class DragMode { Undecided, Transform, Pan, Dismiss, YieldToPager }

private fun clampOffset(offset: Offset, scale: Float, width: Float, height: Float): Offset {
    val horizontalBound = max(0f, (width * scale - width) / 2f)
    val verticalBound = max(0f, (height * scale - height) / 2f)
    return Offset(
        x = offset.x.coerceIn(-horizontalBound, horizontalBound),
        y = offset.y.coerceIn(-verticalBound, verticalBound),
    )
}

private fun Context.saveImageToPictures(imageUrl: String): Uri {
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

/** 下载到 cacheDir 并通过 FileProvider 生成可分享的 content:// URI。 */
private fun Context.cacheImageForShare(imageUrl: String): Uri {
    val extension = imageUrl.substringBefore('?')
        .substringAfterLast('.', "jpg")
        .lowercase()
        .takeIf { it in setOf("jpg", "jpeg", "png", "webp", "gif") }
        ?: "jpg"
    val dir = File(cacheDir, "shared-images").apply { mkdirs() }
    val file = File(dir, "fishpi-${System.currentTimeMillis()}.$extension")
    URL(imageUrl).openStream().use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    }
    return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
}
