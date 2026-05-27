package dev.fishpi.mobile.feature.home

import dev.fishpi.mobile.ui.components.FishPiPillButton

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.ModeComment
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import dev.fishpi.mobile.data.HomeWorkSettings
import dev.fishpi.mobile.data.UserActivityView
import dev.fishpi.mobile.feature.home.model.HomeArticleUiModel
import dev.fishpi.mobile.feature.home.model.HomeWorkSettingsDraft
import dev.fishpi.mobile.feature.home.model.homeTimeMinutes
import dev.fishpi.mobile.feature.home.model.isHomeTimeText
import dev.fishpi.mobile.feature.home.model.toHomeTimeParts
import dev.fishpi.mobile.rememberFishPiImageLoader
import java.util.Calendar
import java.util.Locale


@Composable
internal fun homeIslandSurfaceColor(): Color {
    val colors = MaterialTheme.colorScheme
    return if (colors.background.luminance() < 0.5f) {
        colors.surfaceContainerHigh.copy(alpha = 0.92f)
    } else {
        colors.surface.copy(alpha = 0.92f)
    }
}

@Composable
internal fun homeArticleCardColor(): Color {
    val colors = MaterialTheme.colorScheme
    return if (colors.background.luminance() < 0.5f) {
        colors.surface.copy(alpha = 0.96f)
    } else {
        colors.surface.copy(alpha = 0.94f)
    }
}

@Composable
internal fun homeIslandBorderColor(): Color {
    val colors = MaterialTheme.colorScheme
    return if (colors.background.luminance() < 0.5f) {
        Color.White.copy(alpha = 0.10f)
    } else {
        colors.outline.copy(alpha = 0.42f)
    }
}

@Composable
internal fun homeRewardGradient(): Brush {
    val colors = MaterialTheme.colorScheme
    return if (colors.background.luminance() < 0.5f) {
        Brush.linearGradient(listOf(Color(0xFF173629), Color(0xFF214B39)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFF3FAF1), Color(0xFFE7F5EA), Color(0xFFF9FCF6)))
    }
}

internal fun homeDayGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..10 -> "早上好"
        in 11..13 -> "中午好"
        in 14..17 -> "下午好"
        else -> "晚上好"
    }
}

internal fun homeDateLabel(): String {
    val calendar = Calendar.getInstance()
    val week = listOf("星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六")
        .getOrElse(calendar.get(Calendar.DAY_OF_WEEK) - 1) { "" }
    return "今天是${calendar.get(Calendar.MONTH) + 1}月${calendar.get(Calendar.DAY_OF_MONTH)}日 $week"
}

internal fun homeHoursUntilOffWork(settings: HomeWorkSettings): Float? {
    val now = Calendar.getInstance()
    val appWeekday = when (now.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> 7
        else -> now.get(Calendar.DAY_OF_WEEK) - 1
    }
    if (appWeekday in settings.restDays()) {
        return null
    }
    val endMinutes = homeTimeMinutes(settings.endTime)
    val offWork = now.clone() as Calendar
    offWork.set(Calendar.HOUR_OF_DAY, endMinutes / 60)
    offWork.set(Calendar.MINUTE, endMinutes % 60)
    offWork.set(Calendar.SECOND, 0)
    offWork.set(Calendar.MILLISECOND, 0)
    val diffMs = offWork.timeInMillis - now.timeInMillis
    return diffMs / 3_600_000f
}

internal fun homeDaysUntilDragonBoat(): Long {
    val today = Calendar.getInstance()
    today.set(Calendar.HOUR_OF_DAY, 0)
    today.set(Calendar.MINUTE, 0)
    today.set(Calendar.SECOND, 0)
    today.set(Calendar.MILLISECOND, 0)
    val festival = Calendar.getInstance().apply {
        set(Calendar.YEAR, 2026)
        set(Calendar.MONTH, Calendar.JUNE)
        set(Calendar.DAY_OF_MONTH, 19)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val diffMs = festival.timeInMillis - today.timeInMillis
    return (diffMs / 86_400_000L).coerceAtLeast(0L)
}

