package dev.fishpi.mobile.ui.components

import dev.fishpi.mobile.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import dev.fishpi.mobile.data.FishPiUser
import dev.fishpi.mobile.data.MedalView
import dev.fishpi.mobile.utils.toFishPiGeneratedBadgeOrNull

internal enum class FishPiAvatarFallback {
    Text,
    Icon,
}

@Composable
internal fun FishPiAvatar(
    avatarUrl: String,
    displayName: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    fallback: FishPiAvatarFallback = FishPiAvatarFallback.Text,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(FishPiTheme.accent.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        val fallbackContent: @Composable () -> Unit = {
            when (fallback) {
                FishPiAvatarFallback.Text -> Text(
                    text = displayName.fishPiAvatarInitial(),
                    color = FishPiTheme.accent,
                    fontWeight = FontWeight.SemiBold,
                )
                FishPiAvatarFallback.Icon -> Icon(
                    imageVector = Icons.Rounded.AccountCircle,
                    contentDescription = null,
                    tint = FishPiTheme.accent,
                    modifier = Modifier.size(size * 0.66f),
                )
            }
        }
        if (avatarUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model = avatarUrl,
                imageLoader = rememberFishPiImageLoader(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                loading = { fallbackContent() },
                error = { fallbackContent() },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            fallbackContent()
        }
    }
}

@Composable
internal fun FishPiUserSummaryCard(
    user: FishPiUser,
    medals: List<MedalView>,
    apiKey: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FishPiTheme.radiusBox))
            .background(FishPiTheme.surfaceContainer)
            .padding(FishPiTheme.spacingSection),
        verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingSection),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingSection),
        ) {
            FishPiAvatar(
                avatarUrl = user.userAvatarUrl,
                displayName = user.displayName,
                contentDescription = "用户头像",
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem),
                ) {
                    Text(
                        text = user.displayName,
                        color = FishPiTheme.accent,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (user.role.isNotBlank()) {
                        FishPiRoleBadge(role = user.role)
                    }
                }
                Text(text = "@${user.userName}", color = FishPiTheme.onSurface.copy(alpha = 0.62f))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(FishPiTheme.spacingItem)) {
            FishPiProfileStatChip(label = "积分", value = user.points.toString())
            FishPiProfileStatChip(label = "关注", value = user.following.toString())
            FishPiProfileStatChip(label = "粉丝", value = user.follower.toString())
        }

        if (user.intro.isNotBlank()) {
            Text(text = user.intro, color = FishPiTheme.onSurface)
        }
        if (user.city.isNotBlank() || user.url.isNotBlank()) {
            Text(
                text = listOf(user.city, user.url).filter { it.isNotBlank() }.joinToString(" · "),
                color = FishPiTheme.onSurface.copy(alpha = 0.62f),
            )
        }
        if (user.onlineMinutes > 0) {
            Text(
                text = "在线 ${user.onlineMinutes} 分钟",
                color = FishPiTheme.onSurface.copy(alpha = 0.62f),
            )
        }
        FishPiMedalWallContent(
            medals = medals,
            apiKey = apiKey,
            emptyText = user.role.ifBlank { "暂无可展示勋章" },
        )
    }
}

@Composable
internal fun FishPiMedalWall(
    medals: List<MedalView>,
    apiKey: String,
    emptyText: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    titleColor: Color = FishPiTheme.onSurface,
    loadingText: String = "正在加载勋章...",
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FishPiTheme.radiusBox))
            .background(FishPiTheme.surface.copy(alpha = 0.84f))
            .padding(FishPiTheme.spacingSection),
        verticalArrangement = Arrangement.spacedBy(FishPiTheme.spacingSection),
    ) {
        Text(text = "勋章墙", color = titleColor, fontWeight = FontWeight.Bold)
        when {
            isLoading -> Text(text = loadingText, color = FishPiTheme.weakText)
            medals.isEmpty() -> Text(text = emptyText, color = FishPiTheme.weakText)
            else -> FishPiMedalRow(medals = medals, apiKey = apiKey)
        }
    }
}

@Composable
private fun FishPiMedalWallContent(
    medals: List<MedalView>,
    apiKey: String,
    emptyText: String,
) {
    Text(
        text = "勋章墙",
        color = FishPiTheme.onSurface.copy(alpha = 0.72f),
        fontWeight = FontWeight.SemiBold,
    )
    if (medals.isEmpty()) {
        Text(
            text = emptyText,
            color = FishPiTheme.onSurface.copy(alpha = 0.58f),
            fontSize = 12.sp,
        )
    } else {
        FishPiMedalRow(
            medals = medals,
            apiKey = apiKey,
            contentPadding = PaddingValues(end = 4.dp),
        )
    }
}

@Composable
private fun FishPiMedalRow(
    medals: List<MedalView>,
    apiKey: String,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = contentPadding,
    ) {
        itemsIndexed(medals) { index, medal ->
            FishPiMedalBadge(
                medal = medal,
                apiKey = apiKey,
                modifier = if (index == 0) Modifier else Modifier.fishPiOverlapStart(8.dp),
            )
        }
    }
}

@Composable
internal fun FishPiMedalBadge(
    medal: MedalView,
    apiKey: String,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = false,
) {
    val label = medal.name.ifBlank { medal.text }.ifBlank { return }
    val widthDp = remember(label) { fishPiBadgeWidthDp(label) }
    val badge = remember(medal) { medal.toFishPiGeneratedBadgeOrNull() }
    val badgeText = badge?.text ?: label
    val backgroundColor = badge?.backColor?.let(::Color) ?: FishPiTheme.surface
    val textColor = badge?.fontColor?.let(::Color) ?: FishPiTheme.onSurface
    Box(
        modifier = if (fillWidth) {
            modifier
                .fillMaxWidth()
                .height(30.dp)
        } else {
            modifier
                .width(widthDp)
                .height(30.dp)
        },
    ) {
        Box(
            modifier = Modifier
                .padding(start = 12.dp, top = 2.dp)
                .height(21.dp)
                .clip(RoundedCornerShape(FishPiTheme.radiusSelector))
                .background(backgroundColor)
                .border(FishPiTheme.borderWidth, FishPiTheme.outline.copy(alpha = 0.38f), RoundedCornerShape(FishPiTheme.radiusSelector))
                .padding(start = 15.dp, end = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = badgeText,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Bold,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                ),
            )
        }
        MedalIcon(
            imageUrl = badge?.imageUrl.orEmpty(),
            label = badgeText,
            textColor = textColor,
            modifier = Modifier
                .size(25.dp)
                .clip(RoundedCornerShape(FishPiTheme.radiusSelector))
                .border(FishPiTheme.borderWidth, FishPiTheme.outline.copy(alpha = 0.38f), RoundedCornerShape(FishPiTheme.radiusSelector)),
        )
    }
}

@Composable
private fun MedalIcon(
    imageUrl: String,
    label: String,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model = imageUrl,
                imageLoader = rememberFishPiImageLoader(),
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = label.take(1),
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
internal fun FishPiRoleBadge(role: String) {
    val imageUrl = fishPiRoleImageUrl(role)
    if (imageUrl != null) {
        SubcomposeAsyncImage(
            model = imageUrl,
            imageLoader = rememberFishPiImageLoader(),
            contentDescription = role,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(20.dp)
                .widthIn(min = 30.dp, max = 68.dp),
        )
    } else {
        Text(
            text = role,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(FishPiTheme.radiusField * 0.35f))
                .background(FishPiErrorRed)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
internal fun FishPiProfileStatChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(FishPiTheme.radiusBox))
            .background(FishPiTheme.surface)
            .padding(
                horizontal = FishPiTheme.spacingControl,
                vertical = FishPiTheme.spacingControl * 0.75f,
            ),
    ) {
        Text(text = value, color = FishPiTheme.accent, fontWeight = FontWeight.SemiBold)
        Text(text = label, color = FishPiTheme.onSurface.copy(alpha = 0.58f))
    }
}

private fun String.fishPiAvatarInitial(): String =
    trim().firstOrNull()?.toString().orEmpty().ifBlank { "鱼" }

private fun fishPiBadgeWidthDp(text: String): Dp {
    val chars = text.length.toFloat()
    val wide = text.count { it.code > 127 }.toFloat()
    val effective = chars + wide * 0.84f
    val px = (effective * 8.6f + 52f).toInt().coerceIn(84, 220)
    return px.dp
}

private fun fishPiRoleImageUrl(role: String): String? {
    val normalized = role.trim().lowercase()
    return when {
        normalized.isBlank() -> null
        normalized == "op" || role.contains("OP", ignoreCase = true) -> "https://file.fishpi.cn/opRole.png"
        normalized.contains("admin") || role.contains("管理员") -> "https://file.fishpi.cn/adminRole.png"
        role.contains("纪律委员") || normalized.contains("police") -> "https://file.fishpi.cn/policeRole.png"
        role.contains("超级会员") || normalized.contains("svip") -> "https://file.fishpi.cn/svipRole.png"
        role.contains("成员") || normalized.contains("member") || role.contains("会员") || normalized.contains("vip") -> "https://file.fishpi.cn/vipRole.png"
        role.contains("新手") || normalized.contains("new") -> "https://file.fishpi.cn/newRole.png"
        else -> null
    }
}

private fun Modifier.fishPiOverlapStart(overlap: Dp): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val overlapPx = overlap.roundToPx().coerceAtLeast(0)
        val width = (placeable.width - overlapPx).coerceAtLeast(0)
        layout(width, placeable.height) {
            placeable.placeRelative(-overlapPx, 0)
        }
    },
)


