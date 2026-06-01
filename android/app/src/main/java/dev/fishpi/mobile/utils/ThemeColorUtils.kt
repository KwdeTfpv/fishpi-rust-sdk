package dev.fishpi.mobile.utils

import androidx.compose.ui.graphics.Color
import dev.fishpi.mobile.FishPiBorderTokens
import dev.fishpi.mobile.FishPiColorTokens
import dev.fishpi.mobile.FishPiDepthTokens
import dev.fishpi.mobile.FishPiRadiusTokens
import dev.fishpi.mobile.FishPiSpacingTokens
import dev.fishpi.mobile.FishPiThemeColorScheme
import dev.fishpi.mobile.FishPiThemeTokens
import dev.fishpi.mobile.toThemeHex

internal enum class ThemeTokenColorKey {
    Base100,
    Base200,
    Base300,
    BaseContent,
    Primary,
    PrimaryContent,
    Secondary,
    SecondaryContent,
    Accent,
    AccentContent,
    Neutral,
    NeutralContent,
    Info,
    Success,
    Warning,
    Error,
    MessageOutgoing,
}

internal enum class ThemeTokenMetricKey {
    RadiusSelector,
    RadiusField,
    RadiusBox,
    SpacingPage,
    SpacingSection,
    SpacingItem,
    SpacingControl,
    BorderWidth,
    BorderOpacity,
    Depth,
}

internal data class ThemeTokenColorSpec(val key: ThemeTokenColorKey, val label: String)
internal data class ThemeTokenMetricSpec(
    val key: ThemeTokenMetricKey,
    val label: String,
    val range: ClosedFloatingPointRange<Float>,
    val suffix: String,
)

internal data class ThemeTokenColorSection(val label: String, val colors: List<ThemeTokenColorSpec>)
internal data class ThemeTokenMetricSection(val label: String, val metrics: List<ThemeTokenMetricSpec>)

internal val ThemeTokenColorSections = listOf(
    ThemeTokenColorSection(
        "底色",
        listOf(
            ThemeTokenColorSpec(ThemeTokenColorKey.Base100, "base-100 页面底色"),
            ThemeTokenColorSpec(ThemeTokenColorKey.Base200, "base-200 内容表面"),
            ThemeTokenColorSpec(ThemeTokenColorKey.Base300, "base-300 控件表面"),
            ThemeTokenColorSpec(ThemeTokenColorKey.BaseContent, "base-content 正文"),
        ),
    ),
    ThemeTokenColorSection(
        "品牌",
        listOf(
            ThemeTokenColorSpec(ThemeTokenColorKey.Primary, "primary 主操作"),
            ThemeTokenColorSpec(ThemeTokenColorKey.PrimaryContent, "primary-content"),
            ThemeTokenColorSpec(ThemeTokenColorKey.Secondary, "secondary 链接/@"),
            ThemeTokenColorSpec(ThemeTokenColorKey.SecondaryContent, "secondary-content"),
            ThemeTokenColorSpec(ThemeTokenColorKey.Accent, "accent 状态强调"),
            ThemeTokenColorSpec(ThemeTokenColorKey.AccentContent, "accent-content"),
            ThemeTokenColorSpec(ThemeTokenColorKey.Neutral, "neutral 边界/弱色块"),
            ThemeTokenColorSpec(ThemeTokenColorKey.NeutralContent, "neutral-content 弱文字/说明"),
        ),
    ),
    ThemeTokenColorSection(
        "状态",
        listOf(
            ThemeTokenColorSpec(ThemeTokenColorKey.Info, "info 信息/拍照"),
            ThemeTokenColorSpec(ThemeTokenColorKey.Success, "success 成功/相册"),
            ThemeTokenColorSpec(ThemeTokenColorKey.Warning, "warning 警告"),
            ThemeTokenColorSpec(ThemeTokenColorKey.Error, "error 错误/红包"),
        ),
    ),
    ThemeTokenColorSection(
        "聊天",
        listOf(
            ThemeTokenColorSpec(ThemeTokenColorKey.MessageOutgoing, "message-outgoing 自己消息气泡"),
        ),
    ),
)

internal val ThemeTokenMetricSections = listOf(
    ThemeTokenMetricSection(
        "圆角",
        listOf(
            ThemeTokenMetricSpec(ThemeTokenMetricKey.RadiusSelector, "selector 选择器", 0f..40f, "dp"),
            ThemeTokenMetricSpec(ThemeTokenMetricKey.RadiusField, "field 输入/按钮", 0f..40f, "dp"),
            ThemeTokenMetricSpec(ThemeTokenMetricKey.RadiusBox, "box 卡片/气泡", 0f..40f, "dp"),
        ),
    ),
    ThemeTokenMetricSection(
        "间距",
        listOf(
            ThemeTokenMetricSpec(ThemeTokenMetricKey.SpacingPage, "page 页面边距", 8f..28f, "dp"),
            ThemeTokenMetricSpec(ThemeTokenMetricKey.SpacingSection, "section 区块间距", 6f..28f, "dp"),
            ThemeTokenMetricSpec(ThemeTokenMetricKey.SpacingItem, "item 条目间距", 4f..20f, "dp"),
            ThemeTokenMetricSpec(ThemeTokenMetricKey.SpacingControl, "control 控件内距", 4f..20f, "dp"),
        ),
    ),
    ThemeTokenMetricSection(
        "边界",
        listOf(
            ThemeTokenMetricSpec(ThemeTokenMetricKey.BorderWidth, "border 边框粗细", 0f..3f, "dp"),
            ThemeTokenMetricSpec(ThemeTokenMetricKey.BorderOpacity, "border 边框强度", 0f..1f, ""),
            ThemeTokenMetricSpec(ThemeTokenMetricKey.Depth, "depth 层级强度", 0f..1f, ""),
        ),
    ),
)

internal data class EditableThemeTokens(
    val colorScheme: FishPiThemeColorScheme,
    private val colors: Map<ThemeTokenColorKey, String>,
    private val metrics: Map<ThemeTokenMetricKey, Float>,
) {
    operator fun get(key: ThemeTokenColorKey): String = colors[key].orEmpty()
    operator fun get(key: ThemeTokenMetricKey): Float = metrics[key] ?: 0f

    fun with(key: ThemeTokenColorKey, value: String): EditableThemeTokens =
        copy(colors = colors + (key to value))

    fun with(key: ThemeTokenMetricKey, value: Float): EditableThemeTokens =
        copy(metrics = metrics + (key to value))

    fun withScheme(value: FishPiThemeColorScheme): EditableThemeTokens =
        copy(colorScheme = value)

    fun isValid(): Boolean =
        ThemeTokenColorKey.entries.all { get(it).isValidThemeHex() }

    fun toTokens(base: FishPiThemeTokens): FishPiThemeTokens = base.copy(
        colorScheme = colorScheme,
        colors = FishPiColorTokens(
            base100 = get(ThemeTokenColorKey.Base100).toThemeColor(),
            base200 = get(ThemeTokenColorKey.Base200).toThemeColor(),
            base300 = get(ThemeTokenColorKey.Base300).toThemeColor(),
            baseContent = get(ThemeTokenColorKey.BaseContent).toThemeColor(),
            primary = get(ThemeTokenColorKey.Primary).toThemeColor(),
            primaryContent = get(ThemeTokenColorKey.PrimaryContent).toThemeColor(),
            secondary = get(ThemeTokenColorKey.Secondary).toThemeColor(),
            secondaryContent = get(ThemeTokenColorKey.SecondaryContent).toThemeColor(),
            accent = get(ThemeTokenColorKey.Accent).toThemeColor(),
            accentContent = get(ThemeTokenColorKey.AccentContent).toThemeColor(),
            neutral = get(ThemeTokenColorKey.Neutral).toThemeColor(),
            neutralContent = get(ThemeTokenColorKey.NeutralContent).toThemeColor(),
            info = get(ThemeTokenColorKey.Info).toThemeColor(),
            success = get(ThemeTokenColorKey.Success).toThemeColor(),
            warning = get(ThemeTokenColorKey.Warning).toThemeColor(),
            error = get(ThemeTokenColorKey.Error).toThemeColor(),
            messageOutgoing = get(ThemeTokenColorKey.MessageOutgoing).toThemeColor(),
        ),
        radius = FishPiRadiusTokens(
            selector = get(ThemeTokenMetricKey.RadiusSelector),
            field = get(ThemeTokenMetricKey.RadiusField),
            box = get(ThemeTokenMetricKey.RadiusBox),
        ),
        spacing = FishPiSpacingTokens(
            page = get(ThemeTokenMetricKey.SpacingPage),
            section = get(ThemeTokenMetricKey.SpacingSection),
            item = get(ThemeTokenMetricKey.SpacingItem),
            control = get(ThemeTokenMetricKey.SpacingControl),
        ),
        border = FishPiBorderTokens(
            width = get(ThemeTokenMetricKey.BorderWidth),
            opacity = get(ThemeTokenMetricKey.BorderOpacity),
        ),
        depth = FishPiDepthTokens(level = get(ThemeTokenMetricKey.Depth)),
    )

    companion object {
        fun from(tokens: FishPiThemeTokens): EditableThemeTokens = EditableThemeTokens(
            colorScheme = tokens.colorScheme,
            colors = mapOf(
                ThemeTokenColorKey.Base100 to tokens.colors.base100.toThemeHex(),
                ThemeTokenColorKey.Base200 to tokens.colors.base200.toThemeHex(),
                ThemeTokenColorKey.Base300 to tokens.colors.base300.toThemeHex(),
                ThemeTokenColorKey.BaseContent to tokens.colors.baseContent.toThemeHex(),
                ThemeTokenColorKey.Primary to tokens.colors.primary.toThemeHex(),
                ThemeTokenColorKey.PrimaryContent to tokens.colors.primaryContent.toThemeHex(),
                ThemeTokenColorKey.Secondary to tokens.colors.secondary.toThemeHex(),
                ThemeTokenColorKey.SecondaryContent to tokens.colors.secondaryContent.toThemeHex(),
                ThemeTokenColorKey.Accent to tokens.colors.accent.toThemeHex(),
                ThemeTokenColorKey.AccentContent to tokens.colors.accentContent.toThemeHex(),
                ThemeTokenColorKey.Neutral to tokens.colors.neutral.toThemeHex(),
                ThemeTokenColorKey.NeutralContent to tokens.colors.neutralContent.toThemeHex(),
                ThemeTokenColorKey.Info to tokens.colors.info.toThemeHex(),
                ThemeTokenColorKey.Success to tokens.colors.success.toThemeHex(),
                ThemeTokenColorKey.Warning to tokens.colors.warning.toThemeHex(),
                ThemeTokenColorKey.Error to tokens.colors.error.toThemeHex(),
                ThemeTokenColorKey.MessageOutgoing to tokens.colors.messageOutgoing.toThemeHex(),
            ),
            metrics = mapOf(
                ThemeTokenMetricKey.RadiusSelector to tokens.radius.selector,
                ThemeTokenMetricKey.RadiusField to tokens.radius.field,
                ThemeTokenMetricKey.RadiusBox to tokens.radius.box,
                ThemeTokenMetricKey.SpacingPage to tokens.spacing.page,
                ThemeTokenMetricKey.SpacingSection to tokens.spacing.section,
                ThemeTokenMetricKey.SpacingItem to tokens.spacing.item,
                ThemeTokenMetricKey.SpacingControl to tokens.spacing.control,
                ThemeTokenMetricKey.BorderWidth to tokens.border.width,
                ThemeTokenMetricKey.BorderOpacity to tokens.border.opacity,
                ThemeTokenMetricKey.Depth to tokens.depth.level,
            ),
        )
    }
}

internal fun String.isValidThemeHex(): Boolean =
    trim().matches(Regex("^#[0-9a-fA-F]{6}$"))

internal fun String.toThemeColor(): Color =
    Color(android.graphics.Color.parseColor(trim()))

internal fun String.toThemeRgb(): Triple<Int, Int, Int> {
    val color = if (isValidThemeHex()) {
        android.graphics.Color.parseColor(trim())
    } else {
        android.graphics.Color.parseColor("#FFFFFF")
    }
    return Triple(android.graphics.Color.red(color), android.graphics.Color.green(color), android.graphics.Color.blue(color))
}

internal fun themeHexFromRgb(r: Int, g: Int, b: Int): String =
    "#%02X%02X%02X".format(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
