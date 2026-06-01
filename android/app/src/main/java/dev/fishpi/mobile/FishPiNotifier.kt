package dev.fishpi.mobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import coil3.compose.SubcomposeAsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

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
    val id: Long = System.nanoTime(),
)

internal object FishPiNotifier {
    private val _notices = MutableSharedFlow<FishPiNotice>(
        extraBufferCapacity = 8,
    )
    val notices = _notices.asSharedFlow()

    fun show(
        message: String?,
        type: FishPiNoticeType = FishPiNoticeType.Info,
        durationMs: Long = 2_200L,
        avatarUrl: String = "",
    ) {
        val text = message?.trim().orEmpty()
        if (text.isBlank()) return
        _notices.tryEmit(FishPiNotice(text, type, durationMs, avatarUrl.trim()))
    }

    fun success(message: String?) = show(message, FishPiNoticeType.Success)

    fun error(message: String?) = show(message, FishPiNoticeType.Error, durationMs = 2_800L)
}

@Composable
internal fun FishPiNotificationHost() {
    var notice by remember { mutableStateOf<FishPiNotice?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        FishPiNotifier.notices.collect { next ->
            notice = next
            visible = true
            delay(next.durationMs)
            if (notice?.id == next.id) {
                visible = false
                delay(260L)
                if (notice?.id == next.id) {
                    notice = null
                }
            }
        }
    }

    val current = notice
    if (current != null) {
        Popup(alignment = Alignment.TopCenter) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            ) {
                FishPiNotificationPill(current)
            }
        }
    }
}

@Composable
private fun FishPiNotificationPill(notice: FishPiNotice) {
    val colors = MaterialTheme.colorScheme
    val dark = FishPiTheme.background.luminance() < 0.5f
    val accent = notice.type.noticeColor()
    val container = if (dark) {
        FishPiTheme.surfaceElevated
    } else {
        FishPiTheme.surface
    }
    Row(
        modifier = Modifier
            .statusBarsPadding()
            .padding(top = 9.dp, start = 18.dp, end = 18.dp)
            .shadow(18.dp, RoundedCornerShape(999.dp), clip = false)
            .clip(RoundedCornerShape(999.dp))
            .background(container)
            .border(
                width = 1.dp,
                color = FishPiTheme.outline.copy(alpha = if (dark) 0.42f else 0.18f),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 11.dp, vertical = 8.dp)
            .sizeIn(minHeight = 38.dp, maxWidth = 328.dp),
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
            maxLines = 2,
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
