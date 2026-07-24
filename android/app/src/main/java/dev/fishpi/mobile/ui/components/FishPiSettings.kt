package dev.fishpi.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.fishpi.mobile.FishPiErrorRed
import dev.fishpi.mobile.FishPiTheme
import dev.fishpi.mobile.ui.theme.FishPiType

/**
 * 「设置分组卡片」。
 *
 * @param title 分组标题;null 则不渲染标题(纯容器)。
 * @param titlePadding 标题自身的 padding。
 * @param contentPadding 容器内层 padding(默认竖直 4dp,对齐 ProfileActionSection)。
 * @param verticalArrangement 内容行间距(默认 0,行自带 padding)。
 */
@Composable
internal fun FishPiSettingsSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    titlePadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    contentPadding: PaddingValues = PaddingValues(vertical = 4.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FishPiTheme.radiusBox + 4.dp))
            .background(FishPiTheme.surfaceContainer)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
    ) {
        if (title != null) {
            Text(
                text = title,
                style = FishPiType.secondaryStrong,
                color = FishPiTheme.weakText,
                modifier = Modifier.padding(titlePadding),
            )
        }
        content()
    }
}

/**
 * 「设置行 / 列表项行」:圆形图标框 + 标题 + 副标题 + 可选 trailing。
 *
 * @param icon 图标内容槽(调用方自定 imageVector + tint)。
 * @param title 主标题。
 * @param summary 副标题;null 则只渲染标题一行。
 * @param onClick 点击;null 则行不可点(仍渲染)。
 * @param enabled 是否可点 + 是否显示默认右箭头。
 * @param danger 危险态(退出登录/清除等):图标框 error 底色、标题红色。
 * @param iconBoxSize 图标框直径(默认 34dp;主题行用 38dp)。
 * @param iconBoxColor 图标框底色覆盖(默认 surface / danger 时 error@0.12)。
 * @param trailing 尾部内容槽;不为 null 则替换默认右箭头。
 */
@Composable
internal fun FishPiSettingsRow(
    icon: @Composable () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    danger: Boolean = false,
    iconBoxSize: Dp = 34.dp,
    iconBoxColor: Color? = null,
    titleColor: Color? = null,
    summaryColor: Color? = null,
    contentSpacing: Dp = 1.dp,
    rowPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 11.dp),
    trailing: (@Composable () -> Unit)? = null,
) {
    val boxColor = iconBoxColor
        ?: if (danger) FishPiErrorRed.copy(alpha = 0.12f) else FishPiTheme.surface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick)
                else Modifier
            )
            .padding(rowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(iconBoxSize)
                .clip(CircleShape)
                .background(boxColor),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(contentSpacing)) {
            Text(
                text = title,
                style = FishPiType.body,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = titleColor ?: if (danger) FishPiErrorRed else FishPiTheme.onSurface,
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = FishPiType.body,
                    color = summaryColor ?: FishPiTheme.weakText,
                )
            }
        }
        when {
            trailing != null -> trailing()
            enabled && onClick != null -> Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = FishPiTheme.weakText.copy(alpha = 0.5f),
            )
        }
    }
}
