package dev.fishpi.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.ui.theme.FishPiType

/**
 * 统一页面 / 区块 header。
 *
 * @param title 页面/区块标题。
 * @param subtitle 可选副标题(在线数、描述等)。
 * @param titleStyle 标题样式(默认 [FishPiType.title];大标题传 displayTitle)。
 * @param titleFontWeight 标题字重覆盖(原本 Bold 的传 FontWeight.Bold)。
 * @param titleColor 标题颜色(默认 onSurface)。
 * @param subtitleColor 副标题颜色(默认 weakText)。
 * @param leading 左侧自定义槽(放图标框等);与 [onNavigationClick] 二选一。
 * @param onNavigationClick 提供则显示左侧返回键;null 且无 leading 则不占位。
 * @param navigationIcon 返回键图标,默认返回箭头。
 * @param horizontalArrangement 行内元素间距(默认 spacingItem=8dp)。
 * @param actions 右侧动作槽(放 [IconActionButton]/[ActionChipButton] 等)。
 */
@Composable
internal fun FishPiScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleStyle: androidx.compose.ui.text.TextStyle? = null,
    titleFontWeight: FontWeight? = null,
    titleColor: Color? = null,
    subtitleColor: Color? = null,
    leading: (@Composable () -> Unit)? = null,
    onNavigationClick: (() -> Unit)? = null,
    navigationIcon: ImageVector = Icons.AutoMirrored.Rounded.ArrowBack,
    navigationContentDescription: String? = "返回",
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(FishPiTheme.spacingItem),
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement,
    ) {
        when {
            onNavigationClick != null -> IconActionButton(
                icon = navigationIcon,
                contentDescription = navigationContentDescription,
                onClick = onNavigationClick,
            )
            leading != null -> leading()
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = titleStyle ?: FishPiType.title,
                fontWeight = titleFontWeight ?: (titleStyle ?: FishPiType.title).fontWeight,
                color = titleColor ?: FishPiTheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = FishPiType.caption,
                    color = subtitleColor ?: FishPiTheme.weakText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        actions()
    }
}
