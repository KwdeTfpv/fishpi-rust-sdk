package dev.fishpi.mobile.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 统一的语义化文本样式令牌。
 *
 * ```
 * Text(text, style = FishPiType.caption, color = FishPiTheme.weakText)
 * // 原本是 Bold 而档位默认 SemiBold 时，用 fontWeight 覆盖：
 * Text(title, style = FishPiType.title, fontWeight = FontWeight.Bold, color = ...)
 * ```
 */
internal object FishPiType {
    private fun style(sizeSp: Int, weight: FontWeight, family: FontFamily? = null): TextStyle =
        TextStyle(fontSize = sizeSp.sp, fontWeight = weight, fontFamily = family)

    /** 页面级大标题（帖子标题、集市大标题等）。 */
    val displayTitle: TextStyle @Composable get() = style(20, FontWeight.Bold)

    /** 屏/区块主标题、页面 header、TopBar 标题。
     * 默认 SemiBold；原本 Bold 的大标题在调用点用 `fontWeight = FontWeight.Bold` 覆盖。 */
    val title: TextStyle @Composable get() = style(18, FontWeight.SemiBold)

    /** 卡片/小节标题、强调条目。 */
    val heading: TextStyle @Composable get() = style(15, FontWeight.SemiBold)

    /** 正文（可阅读主体）。 */
    val body: TextStyle @Composable get() = style(14, FontWeight.Normal)

    /** 加重正文（同字号需强调）。 */
    val bodyStrong: TextStyle @Composable get() = style(14, FontWeight.SemiBold)

    /** 次级正文、列表项副文本。 */
    val secondary: TextStyle @Composable get() = style(13, FontWeight.Normal)

    /** 次级正文加重。 */
    val secondaryStrong: TextStyle @Composable get() = style(13, FontWeight.SemiBold)

    /** 说明文字、元信息、时间戳（最高频档）。 */
    val caption: TextStyle @Composable get() = style(12, FontWeight.Normal)

    /** 说明文字加重 */
    val captionStrong: TextStyle @Composable get() = style(12, FontWeight.SemiBold)

    /** 说明文字中等字重。 */
    val captionMedium: TextStyle @Composable get() = style(12, FontWeight.Medium)

    /** 小标签/胶囊/角标文字。 */
    val label: TextStyle @Composable get() = style(11, FontWeight.Medium)

    /** 小标签常规字重。 */
    val labelNormal: TextStyle @Composable get() = style(11, FontWeight.Normal)

    /** 小标签 SemiBold。 */
    val labelStrong: TextStyle @Composable get() = style(11, FontWeight.SemiBold)

    /** 极小注记。 */
    val micro: TextStyle @Composable get() = style(10, FontWeight.Normal)

    /** 极小注记加重。 */
    val microStrong: TextStyle @Composable get() = style(10, FontWeight.SemiBold)

    /** 等宽（代码块、语法高亮、哈希等）。 */
    val mono: TextStyle @Composable get() = style(13, FontWeight.Normal, family = FontFamily.Monospace)
}
