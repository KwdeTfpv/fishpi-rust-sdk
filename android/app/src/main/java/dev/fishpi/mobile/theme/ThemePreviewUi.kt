package dev.fishpi.mobile.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import dev.fishpi.mobile.FishPiPalette
import dev.fishpi.mobile.FishPiThemeOption
import dev.fishpi.mobile.FishPiThemeTokens
import dev.fishpi.mobile.FishPiUiStyle
import dev.fishpi.mobile.FishPiM3BridgedTheme
import dev.fishpi.mobile.rememberFishPiImageLoader
import dev.fishpi.mobile.toPalette

private enum class ThemePreviewPage(val label: String) {
    Chat("聊天"),
    Home("首页"),
    Article("帖子"),
    Profile("我的"),
}

@Composable
internal fun ThemeOptionPreview(
    option: FishPiThemeOption,
    modifier: Modifier = Modifier,
) {
    ThemeMiniPreview(option.tokens, modifier)
}

@Composable
internal fun ThemePreviewDeck(
    tokens: FishPiThemeTokens,
    title: String,
    modifier: Modifier = Modifier,
) {
    var page by remember(tokens) { mutableStateOf(ThemePreviewPage.Chat) }
    val palette = tokens.toPalette()
    FishPiM3BridgedTheme(palette = palette, tokens = tokens, uiStyle = FishPiUiStyle.Classic) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(tokens.radius.box.dp))
                .background(Brush.linearGradient(palette.wallpaperColors))
                .padding(tokens.spacing.section.dp),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.item.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ThemePreviewPage.entries.forEach { item ->
                    ThemePreviewTab(
                        label = item.label,
                        selected = item == page,
                        palette = palette,
                        tokens = tokens,
                        modifier = Modifier.weight(1f),
                        onClick = { page = item },
                    )
                }
            }
            when (page) {
                ThemePreviewPage.Chat -> ChatPreview(palette, tokens, title)
                ThemePreviewPage.Home -> HomePreview(palette, tokens)
                ThemePreviewPage.Article -> ArticlePreview(palette, tokens)
                ThemePreviewPage.Profile -> ProfilePreview(palette, tokens)
            }
        }
    }
}

@Composable
private fun ThemeMiniPreview(tokens: FishPiThemeTokens, modifier: Modifier = Modifier) {
    val palette = tokens.toPalette()
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Brush.linearGradient(palette.wallpaperColors)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(palette.accent),
        )
    }
}

@Composable
private fun ThemePreviewTab(
    label: String,
    selected: Boolean,
    palette: FishPiPalette,
    tokens: FishPiThemeTokens,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(tokens.radius.selector.dp))
            .background(if (selected) palette.primarySoft() else palette.surface.copy(alpha = 0.58f))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) palette.accent else palette.weakText,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun ChatPreview(palette: FishPiPalette, tokens: FishPiThemeTokens, title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.item.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = title, color = palette.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(Modifier.size(7.dp).clip(CircleShape).background(tokens.colors.success))
        }
        PreviewBubble("今天的主题包看起来很舒服", false, palette, tokens)
        PreviewBubble("边距、圆角和气泡都会跟着变", true, palette, tokens)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(tokens.radius.field.dp))
                .background(palette.surfaceContainer.copy(alpha = 0.86f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "biu~", color = palette.weakText, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, tint = palette.accent, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun HomePreview(palette: FishPiPalette, tokens: FishPiThemeTokens) {
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.item.dp)) {
        Text(text = "今日状态", color = palette.onSurface, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.item.dp)) {
            PreviewStat("活跃", "64", palette, tokens, Modifier.weight(1f))
            PreviewStat("奖励", "+5", palette, tokens, Modifier.weight(1f))
        }
        PreviewCard(palette, tokens) {
            Text(text = "推荐帖子", color = palette.onSurface, fontWeight = FontWeight.SemiBold)
            Text(text = "主题会影响列表、按钮和状态提示", color = palette.weakText, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ArticlePreview(palette: FishPiPalette, tokens: FishPiThemeTokens) {
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.item.dp)) {
        PreviewCard(palette, tokens) {
            Text(text = "帖子标题的阅读层级", color = palette.onSurface, fontWeight = FontWeight.Bold)
            Text(text = "正文、引用、优选标记和评论区域使用同一套 token。", color = palette.weakText, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(tokens.radius.field.dp))
                .background(palette.quoteBackground)
                .padding(10.dp),
        ) {
            Text(text = "回复 @fishpi：楼中楼关系", color = palette.quoteText, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ProfilePreview(palette: FishPiPalette, tokens: FishPiThemeTokens) {
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.item.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(palette.accent.copy(alpha = 0.22f)))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(text = "FishPi", color = palette.onSurface, fontWeight = FontWeight.Bold)
                Text(text = "徽章墙 / 统计 / 动态", color = palette.weakText, fontSize = 12.sp)
            }
            Icon(Icons.Rounded.Notifications, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.item.dp)) {
            PreviewStat("帖子", "128", palette, tokens, Modifier.weight(1f))
            PreviewStat("积分", "2048", palette, tokens, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PreviewBubble(text: String, mine: Boolean, palette: FishPiPalette, tokens: FishPiThemeTokens) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .clip(RoundedCornerShape(tokens.radius.box.dp))
                .background(if (mine) palette.outgoingBubble else palette.incomingBubble)
                .padding(horizontal = tokens.spacing.control.dp + 2.dp, vertical = tokens.spacing.control.dp),
        ) {
            Text(text = text, color = palette.onSurface, fontSize = 13.sp)
        }
        Text(text = if (mine) "12:35 · Android" else "Android · 12:34", color = palette.timeText, fontSize = 10.sp)
    }
}

@Composable
private fun PreviewCard(
    palette: FishPiPalette,
    tokens: FishPiThemeTokens,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(tokens.radius.box.dp))
            .background(palette.surface.copy(alpha = 0.86f))
            .padding(tokens.spacing.control.dp + 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

@Composable
private fun PreviewStat(
    label: String,
    value: String,
    palette: FishPiPalette,
    tokens: FishPiThemeTokens,
    modifier: Modifier = Modifier,
) {
    PreviewCard(palette, tokens, modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(palette.accent))
            Text(text = label, color = palette.weakText, fontSize = 11.sp)
        }
        Text(text = value, color = palette.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

private fun FishPiPalette.primarySoft(): Color =
    accent.copy(alpha = 0.16f)
