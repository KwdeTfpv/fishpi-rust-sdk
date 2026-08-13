package dev.fishpi.mobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.SubcomposeAsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class FishPiNoticeType {
    Info,
    Success,
    Warning,
    Error,
}

internal data class FishPiNotice(
    val message: String,
    val type: FishPiNoticeType = FishPiNoticeType.Info,
    val durationMs: Long = 2_200L,
    val avatarUrl: String = "",
    val id: Long,
)

internal const val FISHPI_NOTICE_MAX_VISIBLE = 3

internal object FishPiNoticeReducer {
    fun add(current: List<FishPiNotice>, notice: FishPiNotice): List<FishPiNotice> {
        val next = current + notice
        return if (next.size > FISHPI_NOTICE_MAX_VISIBLE) {
            next.takeLast(FISHPI_NOTICE_MAX_VISIBLE)
        } else {
            next
        }
    }

    fun remove(current: List<FishPiNotice>, id: Long): List<FishPiNotice> =
        current.filterNot { it.id == id }
}

internal object FishPiNotifier {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _notices = MutableStateFlow<List<FishPiNotice>>(emptyList())
    val notices: StateFlow<List<FishPiNotice>> = _notices.asStateFlow()
    private val timers = mutableMapOf<Long, Job>()

    fun show(
        message: String?,
        type: FishPiNoticeType = FishPiNoticeType.Info,
        durationMs: Long? = null,
        avatarUrl: String = "",
    ) {
        val text = message?.trim().orEmpty()
        if (text.isBlank()) return
        val duration = durationMs ?: readingDurationMs(text, type)
        val notice = FishPiNotice(
            message = text,
            type = type,
            durationMs = duration,
            avatarUrl = avatarUrl.trim(),
            id = FishPiOverlayRegistry.nextId(),
        )
        scope.launch {
            _notices.update { FishPiNoticeReducer.add(it, notice) }
            timers[notice.id] = scope.launch {
                delay(duration)
                removeNow(notice.id)
            }
        }
    }

    fun dismiss(id: Long) {
        scope.launch { removeNow(id) }
    }

    private fun removeNow(id: Long) {
        timers.remove(id)?.cancel()
        _notices.update { FishPiNoticeReducer.remove(it, id) }
    }

    fun success(message: String?) = show(message, FishPiNoticeType.Success)

    fun error(message: String?) = show(message, FishPiNoticeType.Error)

    private fun readingDurationMs(text: String, type: FishPiNoticeType): Long {
        val floor = if (type == FishPiNoticeType.Error) 4_500L else 2_200L
        val estimated = 1_400L + text.length * 90L
        return estimated.coerceIn(floor, 9_000L)
    }
}

@Composable
internal fun FishPiNotificationHost() {
    val hostId = remember { FishPiOverlayRegistry.nextId() }
    DisposableEffect(hostId) {
        FishPiOverlayRegistry.push(hostId)
        onDispose { FishPiOverlayRegistry.pop(hostId) }
    }

    val activeHostId by FishPiOverlayRegistry.activeHostId.collectAsState()
    if (activeHostId != hostId) return

    val notices by FishPiNotifier.notices.collectAsState()
    if (notices.isEmpty()) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(Float.MAX_VALUE),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            notices.forEach { notice ->
                key(notice.id) {
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(notice.id) { visible = true }
                    AnimatedVisibility(
                        visible = visible,
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    ) {
                        FishPiNotificationPill(
                            notice = notice,
                            onDismiss = { FishPiNotifier.dismiss(notice.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FishPiNotificationPill(
    notice: FishPiNotice,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val dark = FishPiTheme.background.luminance() < 0.5f
    val accent = notice.type.noticeColor()
    val clipboard = LocalClipboardManager.current
    val container = if (dark) {
        FishPiTheme.surfaceElevated
    } else {
        FishPiTheme.surface
    }
    Row(
        modifier = Modifier
            .padding(top = 9.dp, start = 18.dp, end = 18.dp)
            .shadow(18.dp, RoundedCornerShape(999.dp), clip = false)
            .clip(RoundedCornerShape(999.dp))
            .background(container)
            .border(
                width = 1.dp,
                color = FishPiTheme.outline.copy(alpha = if (dark) 0.42f else 0.18f),
                shape = RoundedCornerShape(999.dp),
            )
            .pointerInput(notice.id) {
                detectTapGestures(
                    onLongPress = {
                        clipboard.setText(AnnotatedString(notice.message))
                        onDismiss()
                        FishPiNotifier.success("已复制通知内容")
                    },
                )
            }
            .pointerInput(notice.id) {
                var dragUp = 0f
                detectVerticalDragGestures(
                    onDragEnd = { dragUp = 0f },
                    onDragCancel = { dragUp = 0f },
                    onVerticalDrag = { change, delta ->
                        change.consume()
                        if (delta < 0f) dragUp += -delta
                        if (dragUp > 40f) {
                            dragUp = 0f
                            onDismiss()
                        }
                    },
                )
            }
            .padding(horizontal = 11.dp, vertical = 8.dp)
            .sizeIn(maxWidth = 328.dp, maxHeight = 220.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = if (dark) 0.20f else 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            if (notice.avatarUrl.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = notice.avatarUrl,
                    imageLoader = rememberFishPiImageLoader(),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    error = {
                        NoticeTypeIcon(notice.type, accent)
                    },
                    loading = {
                        NoticeTypeIcon(notice.type, accent)
                    },
                )
            } else {
                NoticeTypeIcon(notice.type, accent)
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = notice.message,
            color = if (dark) Color.White.copy(alpha = 0.94f) else colors.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 8,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NoticeTypeIcon(type: FishPiNoticeType, tint: Color) {
    Icon(
        imageVector = type.noticeIcon(),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(15.dp),
    )
}

private fun FishPiNoticeType.noticeIcon(): ImageVector = when (this) {
    FishPiNoticeType.Success -> Icons.Rounded.CheckCircle
    FishPiNoticeType.Warning -> Icons.Rounded.WarningAmber
    FishPiNoticeType.Error -> Icons.Rounded.ErrorOutline
    FishPiNoticeType.Info -> Icons.Rounded.Info
}

@Composable
private fun FishPiNoticeType.noticeColor(): Color = when (this) {
    FishPiNoticeType.Success -> FishPiTheme.success
    FishPiNoticeType.Warning -> FishPiTheme.warning
    FishPiNoticeType.Error -> FishPiTheme.error
    FishPiNoticeType.Info -> FishPiTheme.accent
}
