package dev.fishpi.mobile.utils

import androidx.compose.ui.graphics.Color
import dev.fishpi.mobile.FishPiPalette
import dev.fishpi.mobile.toThemeHex

internal enum class ThemeColorKey {
    Background,
    ChatBackground,
    WallpaperStart,
    WallpaperEnd,
    Surface,
    SurfaceElevated,
    SurfaceContainer,
    Outline,
    QuoteBackground,
    OutgoingBubble,
    IncomingBubble,
    OnSurface,
    WeakText,
    UserName,
    ClientText,
    ClientBackground,
    TimeText,
    Accent,
    LinkText,
    QuoteText,
    QuoteLine,
    ToolDefault,
    ToolGallery,
    ToolCamera,
    ToolRedPacket,
}

internal data class ThemeColorSpec(val key: ThemeColorKey, val label: String)

internal data class ThemeColorSection(val label: String, val colors: List<ThemeColorSpec>)

internal val ThemeColorSections = listOf(
    ThemeColorSection(
        "基础背景",
        listOf(
            ThemeColorSpec(ThemeColorKey.Background, "主背景"),
            ThemeColorSpec(ThemeColorKey.ChatBackground, "聊天背景"),
            ThemeColorSpec(ThemeColorKey.WallpaperStart, "壁纸渐变起始"),
            ThemeColorSpec(ThemeColorKey.WallpaperEnd, "壁纸渐变结束"),
        ),
    ),
    ThemeColorSection(
        "界面表面",
        listOf(
            ThemeColorSpec(ThemeColorKey.Surface, "页面卡片背景"),
            ThemeColorSpec(ThemeColorKey.SurfaceElevated, "浮层 / 高层卡片"),
            ThemeColorSpec(ThemeColorKey.SurfaceContainer, "输入框 / 控件背景"),
            ThemeColorSpec(ThemeColorKey.Outline, "分割线 / 边框"),
        ),
    ),
    ThemeColorSection(
        "文字颜色",
        listOf(
            ThemeColorSpec(ThemeColorKey.OnSurface, "主要字体"),
            ThemeColorSpec(ThemeColorKey.WeakText, "弱文字"),
            ThemeColorSpec(ThemeColorKey.Accent, "全局强调色"),
            ThemeColorSpec(ThemeColorKey.LinkText, "链接 / @ 文字"),
        ),
    ),
    ThemeColorSection(
        "聊天消息",
        listOf(
            ThemeColorSpec(ThemeColorKey.OutgoingBubble, "自己消息气泡"),
            ThemeColorSpec(ThemeColorKey.IncomingBubble, "别人消息气泡"),
            ThemeColorSpec(ThemeColorKey.UserName, "用户名"),
            ThemeColorSpec(ThemeColorKey.TimeText, "消息时间"),
            ThemeColorSpec(ThemeColorKey.ClientText, "ClientType 文字"),
            ThemeColorSpec(ThemeColorKey.ClientBackground, "ClientType 背景"),
        ),
    ),
    ThemeColorSection(
        "引用和代码",
        listOf(
            ThemeColorSpec(ThemeColorKey.QuoteBackground, "引用 / 代码背景"),
            ThemeColorSpec(ThemeColorKey.QuoteText, "引用文字"),
            ThemeColorSpec(ThemeColorKey.QuoteLine, "引用竖线"),
        ),
    ),
    ThemeColorSection(
        "底部工具",
        listOf(
            ThemeColorSpec(ThemeColorKey.ToolDefault, "默认工具图标"),
            ThemeColorSpec(ThemeColorKey.ToolGallery, "相册图标"),
            ThemeColorSpec(ThemeColorKey.ToolCamera, "拍照图标"),
            ThemeColorSpec(ThemeColorKey.ToolRedPacket, "红包图标"),
        ),
    ),
)

internal data class EditableThemePalette(private val values: Map<ThemeColorKey, String>) {
    operator fun get(key: ThemeColorKey): String = values[key].orEmpty()

    fun with(key: ThemeColorKey, value: String): EditableThemePalette =
        copy(values = values + (key to value))

    fun isValid(): Boolean = ThemeColorKey.entries.all { get(it).isValidThemeHex() }

    fun toPalette(base: FishPiPalette): FishPiPalette = base.copy(
        background = get(ThemeColorKey.Background).toThemeColor(),
        chatBackground = get(ThemeColorKey.ChatBackground).toThemeColor(),
        wallpaperColors = listOf(
            get(ThemeColorKey.WallpaperStart).toThemeColor(),
            get(ThemeColorKey.WallpaperEnd).toThemeColor(),
        ),
        surface = get(ThemeColorKey.Surface).toThemeColor(),
        surfaceElevated = get(ThemeColorKey.SurfaceElevated).toThemeColor(),
        surfaceContainer = get(ThemeColorKey.SurfaceContainer).toThemeColor(),
        onSurface = get(ThemeColorKey.OnSurface).toThemeColor(),
        weakText = get(ThemeColorKey.WeakText).toThemeColor(),
        userName = get(ThemeColorKey.UserName).toThemeColor(),
        clientText = get(ThemeColorKey.ClientText).toThemeColor(),
        clientBackground = get(ThemeColorKey.ClientBackground).toThemeColor(),
        timeText = get(ThemeColorKey.TimeText).toThemeColor(),
        outline = get(ThemeColorKey.Outline).toThemeColor(),
        accent = get(ThemeColorKey.Accent).toThemeColor(),
        quoteBackground = get(ThemeColorKey.QuoteBackground).toThemeColor(),
        outgoingBubble = get(ThemeColorKey.OutgoingBubble).toThemeColor(),
        incomingBubble = get(ThemeColorKey.IncomingBubble).toThemeColor(),
        linkText = get(ThemeColorKey.LinkText).toThemeColor(),
        quoteText = get(ThemeColorKey.QuoteText).toThemeColor(),
        quoteLine = get(ThemeColorKey.QuoteLine).toThemeColor(),
        toolDefault = get(ThemeColorKey.ToolDefault).toThemeColor(),
        toolGallery = get(ThemeColorKey.ToolGallery).toThemeColor(),
        toolCamera = get(ThemeColorKey.ToolCamera).toThemeColor(),
        toolRedPacket = get(ThemeColorKey.ToolRedPacket).toThemeColor(),
    )

    companion object {
        fun from(palette: FishPiPalette): EditableThemePalette = EditableThemePalette(
            mapOf(
                ThemeColorKey.Background to palette.background.toThemeHex(),
                ThemeColorKey.ChatBackground to palette.chatBackground.toThemeHex(),
                ThemeColorKey.WallpaperStart to (palette.wallpaperColors.firstOrNull()?.toThemeHex()
                    ?: palette.background.toThemeHex()),
                ThemeColorKey.WallpaperEnd to (palette.wallpaperColors.lastOrNull()?.toThemeHex()
                    ?: palette.chatBackground.toThemeHex()),
                ThemeColorKey.Surface to palette.surface.toThemeHex(),
                ThemeColorKey.SurfaceElevated to palette.surfaceElevated.toThemeHex(),
                ThemeColorKey.SurfaceContainer to palette.surfaceContainer.toThemeHex(),
                ThemeColorKey.Outline to palette.outline.toThemeHex(),
                ThemeColorKey.QuoteBackground to palette.quoteBackground.toThemeHex(),
                ThemeColorKey.OutgoingBubble to palette.outgoingBubble.toThemeHex(),
                ThemeColorKey.IncomingBubble to palette.incomingBubble.toThemeHex(),
                ThemeColorKey.OnSurface to palette.onSurface.toThemeHex(),
                ThemeColorKey.WeakText to palette.weakText.toThemeHex(),
                ThemeColorKey.UserName to palette.userName.toThemeHex(),
                ThemeColorKey.ClientText to palette.clientText.toThemeHex(),
                ThemeColorKey.ClientBackground to palette.clientBackground.toThemeHex(),
                ThemeColorKey.TimeText to palette.timeText.toThemeHex(),
                ThemeColorKey.Accent to palette.accent.toThemeHex(),
                ThemeColorKey.LinkText to palette.linkText.toThemeHex(),
                ThemeColorKey.QuoteText to palette.quoteText.toThemeHex(),
                ThemeColorKey.QuoteLine to palette.quoteLine.toThemeHex(),
                ThemeColorKey.ToolDefault to palette.toolDefault.toThemeHex(),
                ThemeColorKey.ToolGallery to palette.toolGallery.toThemeHex(),
                ThemeColorKey.ToolCamera to palette.toolCamera.toThemeHex(),
                ThemeColorKey.ToolRedPacket to palette.toolRedPacket.toThemeHex(),
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
