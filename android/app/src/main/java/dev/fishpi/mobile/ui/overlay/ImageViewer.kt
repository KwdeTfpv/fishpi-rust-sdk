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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import dev.fishpi.mobile.ui.media.cleanupSharedImageCache
import dev.fishpi.mobile.ui.media.imageExtensionOf
import dev.fishpi.mobile.ui.media.openImageStream
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 一次图片查看请求：图片列表 + 起始索引。 */
internal data class ImageViewerRequest(
    val images: List<String>,
    val startIndex: Int,
)

@Composable
internal fun ImageViewer(
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

    LaunchedEffect(saveStatus) {
        val status = saveStatus ?: return@LaunchedEffect
        if (status.startsWith("正在")) return@LaunchedEffect
        delay(3_000)
        saveStatus = null
    }

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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledIconButton(
                onClick = onDismiss,
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.42f),
                    contentColor = Color.White,
                ),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "关闭图片预览")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledIconButton(
                    onClick = { shareImage() },
                    enabled = !isSharing,
                    modifier = Modifier.size(44.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.42f),
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = "分享图片")
                }
                FilledIconButton(
                    onClick = { requestSaveImage() },
                    enabled = !isSaving,
                    modifier = Modifier.size(44.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.42f),
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = "保存图片")
                }
            }
        }

        // 多图页码指示。
        if (images.size > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
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
    var imageWidth by remember(url) { mutableFloatStateOf(0f) }
    var imageHeight by remember(url) { mutableFloatStateOf(0f) }
    var reloadKey by remember(url) { mutableIntStateOf(0) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val viewportWidthPx = constraints.maxWidth
        val viewportHeightPx = constraints.maxHeight
        val request = remember(context, url, viewportWidthPx, viewportHeightPx, reloadKey) {
            ImageRequest.Builder(context)
                .data(url)
                .size(
                    if (viewportWidthPx > 0 && viewportHeightPx > 0) {
                        Size(viewportWidthPx * 2, viewportHeightPx * 2)
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
                .pointerInput(url, imageWidth, imageHeight) {
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
                                    clampOffset(anchored, nextScale, imageWidth, imageHeight, size.width.toFloat(), size.height.toFloat())
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
                                            imageWidth,
                                            imageHeight,
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
                .pointerInput(url, imageWidth, imageHeight) {
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
                                    imageWidth,
                                    imageHeight,
                                    size.width.toFloat(),
                                    size.height.toFloat(),
                                )
                            }
                        },
                    )
                },
        ) {
            val painterState = painter.state.value
            LaunchedEffect(painterState) {
                if (painterState is coil3.compose.AsyncImagePainter.State.Success) {
                    val intrinsic = painterState.painter.intrinsicSize
                    if (intrinsic.width > 0f && intrinsic.height > 0f) {
                        imageWidth = intrinsic.width
                        imageHeight = intrinsic.height
                    }
                }
            }
            when (painterState) {
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
                        TextButton(onClick = { reloadKey++ }) {
                            Text(text = "重试", color = Color.White)
                        }
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

private fun clampOffset(
    offset: Offset,
    scale: Float,
    imageWidth: Float,
    imageHeight: Float,
    viewportWidth: Float,
    viewportHeight: Float,
): Offset {
    val fitScale = if (imageWidth > 0f && imageHeight > 0f) {
        min(viewportWidth / imageWidth, viewportHeight / imageHeight)
    } else {
        1f
    }
    val displayedWidth = if (imageWidth > 0f) imageWidth * fitScale else viewportWidth
    val displayedHeight = if (imageHeight > 0f) imageHeight * fitScale else viewportHeight
    val horizontalBound = max(0f, (displayedWidth * scale - viewportWidth) / 2f)
    val verticalBound = max(0f, (displayedHeight * scale - viewportHeight) / 2f)
    return Offset(
        x = offset.x.coerceIn(-horizontalBound, horizontalBound),
        y = offset.y.coerceIn(-verticalBound, verticalBound),
    )
}

private fun Context.saveImageToPictures(imageUrl: String): Uri {
    val extension = imageExtensionOf(imageUrl)
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
        openImageStream(imageUrl).use { input ->
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
    cleanupSharedImageCache()
    val extension = imageExtensionOf(imageUrl)
    val dir = File(cacheDir, "shared-images").apply { mkdirs() }
    val file = File(dir, "fishpi-${System.currentTimeMillis()}.$extension")
    openImageStream(imageUrl).use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    }
    return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
}
