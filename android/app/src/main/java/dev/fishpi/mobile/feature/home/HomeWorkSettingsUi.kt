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


internal val homeWeekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日")


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeWorkSettingsDialog(
    draft: HomeWorkSettingsDraft,
    onDismiss: () -> Unit,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onWeekendModeChange: (String) -> Unit,
    onToggleCustomRestDay: (Int) -> Unit,
    onSave: () -> Unit,
) {
    var pickingTime by remember { mutableStateOf<HomeTimeTarget?>(null) }
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("首页时间设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeTimePickerItem(
                        value = draft.startTime,
                        label = "上班时间",
                        onClick = { pickingTime = HomeTimeTarget.Start },
                        modifier = Modifier.weight(1f),
                    )
                    HomeTimePickerItem(
                        value = draft.endTime,
                        label = "下班时间",
                        onClick = { pickingTime = HomeTimeTarget.End },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (!draft.isValid) {
                    Text(
                        text = "下班时间需要晚于上班时间。",
                        color = colors.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    text = "周末设置",
                    color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeSettingChoice(
                        text = "双休",
                        selected = draft.weekendMode == HomeWorkSettings.WEEKEND_DOUBLE,
                        onClick = { onWeekendModeChange(HomeWorkSettings.WEEKEND_DOUBLE) },
                    )
                    HomeSettingChoice(
                        text = "单休",
                        selected = draft.weekendMode == HomeWorkSettings.WEEKEND_SINGLE,
                        onClick = { onWeekendModeChange(HomeWorkSettings.WEEKEND_SINGLE) },
                    )
                    HomeSettingChoice(
                        text = "自定义",
                        selected = draft.weekendMode == HomeWorkSettings.WEEKEND_CUSTOM,
                        onClick = { onWeekendModeChange(HomeWorkSettings.WEEKEND_CUSTOM) },
                    )
                }
                if (draft.weekendMode == HomeWorkSettings.WEEKEND_CUSTOM) {
                    Text(
                        text = "选择休息日",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        homeWeekdayLabels.forEachIndexed { index, label ->
                            val day = index + 1
                            HomeSettingChoice(
                                text = label,
                                selected = day in draft.customRestDays,
                                onClick = { onToggleCustomRestDay(day) },
                                compact = true,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = draft.isValid) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )

    pickingTime?.let { target ->
        val current = if (target == HomeTimeTarget.Start) draft.startTime else draft.endTime
        val (hour, minute) = current.toHomeTimeParts()
        val pickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { pickingTime = null },
            title = { Text(if (target == HomeTimeTarget.Start) "选择上班时间" else "选择下班时间") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = pickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val next = "%02d:%02d".format(pickerState.hour, pickerState.minute)
                        if (target == HomeTimeTarget.Start) {
                            onStartTimeChange(next)
                        } else {
                            onEndTimeChange(next)
                        }
                        pickingTime = null
                    },
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingTime = null }) {
                    Text("取消")
                }
            },
        )
    }
}


internal enum class HomeTimeTarget {
    Start,
    End,
}


@Composable
internal fun HomeTimePickerItem(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceContainer)
            .border(1.dp, colors.outline.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            color = colors.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = value,
                color = colors.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}


@Composable
internal fun HomeSettingChoice(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    compact: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = text,
        color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(if (compact) 8.dp else 12.dp))
            .background(if (selected) colors.primaryContainer else colors.surfaceContainerHighest)
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 8.dp else 12.dp, vertical = if (compact) 7.dp else 8.dp),
    )
}

