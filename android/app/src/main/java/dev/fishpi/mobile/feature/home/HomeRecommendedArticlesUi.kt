package dev.fishpi.mobile.feature.home

import dev.fishpi.mobile.ui.components.FishPiAsyncImage
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
import dev.fishpi.mobile.data.HomeWorkSettings
import dev.fishpi.mobile.data.UserActivityView
import dev.fishpi.mobile.feature.home.model.HomeArticleUiModel
import dev.fishpi.mobile.feature.home.model.HomeWorkSettingsDraft
import dev.fishpi.mobile.feature.home.model.homeTimeMinutes
import dev.fishpi.mobile.feature.home.model.isHomeTimeText
import dev.fishpi.mobile.feature.home.model.toHomeTimeParts
import java.util.Calendar
import java.util.Locale
import dev.fishpi.mobile.ui.components.FishPiAvatar


@Composable
internal fun HomeArticleAvatar(article: HomeArticleUiModel) {
    FishPiAvatar(
        avatarUrl = article.avatar,
        displayName = article.author.ifBlank { "帖" },
        contentDescription = "${article.author.ifBlank { "作者" }}头像",
        size = 40.dp,
        fallbackBackground = MaterialTheme.colorScheme.surfaceContainerHigh,
        fallbackContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
@Composable
internal fun HomeArticleMetric(icon: ImageVector, value: Long) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.padding(start = 8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f), modifier = Modifier.size(15.dp))
        Text(text = value.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}

internal fun homeArticleTags(tags: String): List<String> =
    tags.split(",", "，", " ", "#")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()



