package dev.fishpi.mobile

import dev.fishpi.mobile.shared.message.copyToClipboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.fishpi.mobile.ui.components.AppFullScreenWorkspace
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage

private const val AboutRepositoryUrl = "https://github.com/KwdeTfpv/fishpi-rust-sdk"
private const val AboutLicenseName = "MIT License"

private data class AboutAcknowledgement(
    val name: String,
    val description: String,
    val avatarUrl: String = "",
    val link: String = "",
)

private data class AboutProjectLink(
    val title: String,
    val value: String,
)

private val AboutAcknowledgements = listOf(
    AboutAcknowledgement(
        name = "只有午安(Kirito)",
        description = "作者",
        avatarUrl = "https://file.fishpi.cn/2026/03/20220627071838712-e9fa88ba.gif",
        link = "https://fishpi.cn/member/Kirito",
    ),
    AboutAcknowledgement(
        name = "adlered",
        description = "社区创始人 / 赞助",
        avatarUrl = "https://file.fishpi.cn/2025/07/微信图片20250709171949222-dd26ae89.jpg",
        link = "https://fishpi.cn/member/adlered",
    ),
    AboutAcknowledgement(
        name = "csfwff",
        description = "社区创始人 / 赞助",
        avatarUrl = "https://file.fishpi.cn/2026/01/墨墨-f32cc3a3.gif",
        link = "https://fishpi.cn/member/csfwff",
    ),
    AboutAcknowledgement(
        name = "Yui",
        description = "贡献者 / 赞助",
        avatarUrl = "https://file.fishpi.cn/2025/10/blob-8f2a2e25.png",
        link = "https://fishpi.cn/member/Yui",
    ),
    AboutAcknowledgement(
        name = "18",
        description = "贡献者 / 赞助",
        avatarUrl = "https://file.fishpi.cn/2026/05/blob-4aca3b92.png",
        link = "https://fishpi.cn/member/18",
    ),
    AboutAcknowledgement(
        name = "涛之雨(taozhiyu)",
        description = "贡献者 / 赞助",
        avatarUrl = "https://file.fishpi.cn/2021/10/blob-29bbd528.png",
        link = "https://fishpi.cn/member/taozhiyu",
    ),
    AboutAcknowledgement(
        name = "开摆（8888）",
        description = "贡献者 / 赞助",
        avatarUrl = "https://file.fishpi.cn/2022/08/blob-fbe21682.png",
        link = "https://fishpi.cn/member/8888",
    ),
    
)

private val AboutProjectLinks = listOf(
    AboutProjectLink("FishPi / 摸鱼派社区", "https://fishpi.cn"),
    AboutProjectLink("插件开发文档", "https://kwdetfpv.github.io/fishpi-rust-sdk/"),
)

@Composable
internal fun AboutAcknowledgementsWorkspace(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val versionName = BuildConfig.VERSION_NAME.ifBlank { "未知版本" }
    val versionCode = BuildConfig.VERSION_CODE
    AppFullScreenWorkspace(
        title = "关于与鸣谢",
        subtitle = "FishPi $versionName ($versionCode)",
        onDismiss = onDismiss,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                AboutHeroCard(versionName = versionName, versionCode = versionCode)
            }
            item {
                AboutSection(title = "项目") {
                    AboutTextBlock(
                        title = "FishPi Android",
                        text = "一个面向 FishPi / 摸鱼派社区的开源 Android 客户端，包含聊天室、帖子、清风明月、私聊、通知和插件能力。",
                    )
                    AboutInfoRow(
                        title = "开源地址",
                        value = AboutRepositoryUrl,
                        fallback = "待补充",
                        onClick = { openOrCopyAboutValue(context, "开源地址", AboutRepositoryUrl) },
                    )
                    AboutInfoRow(
                        title = "许可证",
                        value = AboutLicenseName,
                        fallback = "待补充",
                        onClick = { openOrCopyAboutValue(context, "许可证", AboutLicenseName) },
                    )
                }
            }
            item {
                AboutSection(title = "鸣谢") {
                    if (AboutAcknowledgements.isEmpty()) {
                        AboutEmptyRow(text = "待补充鸣谢名单")
                    } else {
                        AboutAcknowledgements.forEach { acknowledgement ->
                            AboutAcknowledgementRow(
                                acknowledgement = acknowledgement,
                                onClick = {
                                    openOrCopyAboutValue(
                                        context = context,
                                        label = acknowledgement.name,
                                        value = acknowledgement.link,
                                    )
                                },
                            )
                        }
                    }
                }
            }
            item {
                AboutSection(title = "相关项目") {
                    AboutProjectLinks.forEach { link ->
                        AboutInfoRow(
                            title = link.title,
                            value = link.value,
                            fallback = "待补充",
                            onClick = { openOrCopyAboutValue(context, link.title, link.value) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutHeroCard(
    versionName: String,
    versionCode: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FishPiTheme.surfaceContainer)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(FishPiTheme.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_fishpi_logo),
                contentDescription = "FishPi 图标",
                modifier = Modifier.size(34.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = "FishPi", color = FishPiTheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(text = "版本 $versionName ($versionCode)", color = FishPiTheme.weakText)
        }
    }
}

@Composable
private fun AboutSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FishPiTheme.surfaceContainer)
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = title,
            color = FishPiTheme.weakText,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
        content()
    }
}

@Composable
private fun AboutTextBlock(
    title: String,
    text: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(text = title, color = FishPiTheme.onSurface, fontWeight = FontWeight.SemiBold)
        Text(text = text, color = FishPiTheme.weakText, lineHeight = 18.sp)
    }
}

@Composable
private fun AboutEmptyRow(text: String) {
    Text(
        text = text,
        color = FishPiTheme.weakText.copy(alpha = 0.72f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
    )
}

@Composable
private fun AboutAcknowledgementRow(
    acknowledgement: AboutAcknowledgement,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = acknowledgement.link.isNotBlank(), onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AboutAvatar(
            name = acknowledgement.name,
            avatarUrl = acknowledgement.avatarUrl,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = acknowledgement.name,
                color = FishPiTheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = acknowledgement.description,
                color = FishPiTheme.weakText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp,
            )
        }
        if (acknowledgement.link.isNotBlank()) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = FishPiTheme.weakText.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun AboutAvatar(
    name: String,
    avatarUrl: String,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(FishPiTheme.accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        val fallback: @Composable () -> Unit = {
            Text(
                text = name.trim().take(1).ifBlank { "?" },
                color = FishPiTheme.accent,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (avatarUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model = avatarUrl,
                imageLoader = rememberFishPiImageLoader(),
                contentDescription = "$name 头像",
                contentScale = ContentScale.Crop,
                loading = { fallback() },
                error = { fallback() },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            fallback()
        }
    }
}

@Composable
private fun AboutInfoRow(
    title: String,
    value: String,
    fallback: String,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null && value.isNotBlank()) { onClick?.invoke() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, color = FishPiTheme.onSurface, fontWeight = FontWeight.SemiBold)
            Text(
                text = value.ifBlank { fallback },
                color = if (value.isBlank()) FishPiTheme.weakText.copy(alpha = 0.72f) else FishPiTheme.weakText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onClick != null && value.isNotBlank()) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = FishPiTheme.weakText.copy(alpha = 0.5f),
            )
        }
    }
}

private fun openOrCopyAboutValue(context: Context, label: String, value: String) {
    if (value.isBlank()) return
    val opened = if (value.startsWith("http://") || value.startsWith("https://")) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(value)))
        }.isSuccess
    } else {
        false
    }
    if (!opened) {
        context.copyToClipboard(label, value)
        FishPiNotifier.success("已复制：$label")
    }
}

