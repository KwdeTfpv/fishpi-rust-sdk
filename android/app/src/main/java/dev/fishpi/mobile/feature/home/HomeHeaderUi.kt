package dev.fishpi.mobile.feature.home

import dev.fishpi.mobile.ui.components.FishPiPillButton
import dev.fishpi.mobile.FishPiTheme

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
internal fun HomeQuickEntry(
    title: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(FishPiTheme.radiusBox + 4.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.66f))
                .border(FishPiTheme.borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.34f), RoundedCornerShape(FishPiTheme.radiusBox + 4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
    }
}


@Composable
internal fun HomeGreetingCard(
    displayName: String,
    settings: HomeWorkSettings,
    quote: String,
    onSettingsClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val onAccent = colors.onPrimary
    val greetingGradient = if (dark) {
        Brush.linearGradient(
            listOf(
                colors.primary.copy(alpha = 0.92f),
                FishPiTheme.accent.copy(alpha = 0.74f),
                colors.primary.copy(alpha = 0.78f),
            ),
        )
    } else {
        Brush.linearGradient(
            listOf(
                FishPiTheme.accent.copy(alpha = 0.12f),
                colors.primary.copy(alpha = 0.10f),
                FishPiTheme.surface.copy(alpha = 0.98f),
            ),
        )
    }
    val cardShape = RoundedCornerShape(FishPiTheme.radiusBox + 12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(greetingGradient)
            .border(FishPiTheme.borderWidth, onAccent.copy(alpha = 0.24f), cardShape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Hi，${displayName.ifBlank { "鱼油" }}，${homeDayGreeting()}！",
                color = onAccent,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = onSettingsClick, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.Settings, contentDescription = "设置", tint = onAccent.copy(alpha = 0.9f))
            }
        }
        Text(
            text = homeDateLabel(),
            color = onAccent.copy(alpha = 0.92f),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(CircleShape)
                .background(onAccent.copy(alpha = 0.18f))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
        val hoursLeft = homeHoursUntilOffWork(settings)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                hoursLeft == null -> Text(text = "今天是休息日", color = onAccent.copy(alpha = 0.9f), fontWeight = FontWeight.SemiBold)
                hoursLeft <= 0f -> Text(
                    text = "下班啦！今天辛苦了，好好享受休息时光吧~",
                    color = onAccent.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 22.sp,
                )
                else -> {
                    Text(text = "距离您下班还有", color = onAccent.copy(alpha = 0.86f), fontWeight = FontWeight.SemiBold)
                    Text(text = String.format(Locale.US, "%.1f", hoursLeft), color = onAccent, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(text = "小时", color = onAccent.copy(alpha = 0.86f), fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(FishPiTheme.radiusBox + 4.dp))
                .background(onAccent.copy(alpha = 0.14f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = onAccent.copy(alpha = 0.82f), modifier = Modifier.size(18.dp))
            Text(text = "距离端午节还有${homeDaysUntilDragonBoat()}天", color = onAccent.copy(alpha = 0.9f), fontWeight = FontWeight.SemiBold)
        }
        Text(
            text = quote.ifBlank { HomeQuoteFallback },
            color = onAccent.copy(alpha = 0.88f),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(FishPiTheme.radiusBox + 4.dp))
                .background(onAccent.copy(alpha = 0.12f))
                .padding(14.dp),
        )
    }
}



