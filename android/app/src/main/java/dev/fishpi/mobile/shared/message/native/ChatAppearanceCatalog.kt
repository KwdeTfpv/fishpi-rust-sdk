package dev.fishpi.mobile.shared.message.native

internal data class ChatAppearanceCatalogOption(
    val id: String,
    val label: String,
    val access: String,
) {
    fun chipLabel(): String = if (access == "默认") label else "$label·$access"
}

internal object ChatAppearanceCatalog {
    val bubbles = listOf(
        ChatAppearanceCatalogOption("paipai-orange", "派派橘", "免费"),
        ChatAppearanceCatalogOption("fishpond-blue", "摸鱼蓝", "免费"),
        ChatAppearanceCatalogOption("candy", "糖果", "会员"),
        ChatAppearanceCatalogOption(ChatAppearanceSupport.SakuraLetter, "花瓣折笺", "会员/周租"),
        ChatAppearanceCatalogOption(ChatAppearanceSupport.SakuraMoon, "月弧夜樱", "会员/周租"),
        ChatAppearanceCatalogOption("sea-soda", "海盐汽水", "积分"),
        ChatAppearanceCatalogOption("sunset-letter", "落日邮笺", "积分"),
        ChatAppearanceCatalogOption("star-route", "星夜航线", "积分"),
        ChatAppearanceCatalogOption(ChatBubbleStylePresets.Hexiaotun, "荷叶午后", "积分"),
        ChatAppearanceCatalogOption(ChatBubbleStylePresets.Momota, "河湾手帐", "积分"),
        ChatAppearanceCatalogOption(ChatBubbleStylePresets.Paixiaoyu, "派派浪花", "积分"),
        ChatAppearanceCatalogOption(ChatBubbleStylePresets.Yubingmiao, "鱼饼铃铛", "积分"),
    )

    val frames = listOf(
        ChatAppearanceCatalogOption("none", "无", "默认"),
        ChatAppearanceCatalogOption("paipai-orange", "派派橘", "免费"),
        ChatAppearanceCatalogOption("fishpond-blue", "摸鱼蓝", "免费"),
        ChatAppearanceCatalogOption("gold", "金纹", "会员"),
        ChatAppearanceCatalogOption(ChatAppearanceSupport.SakuraLetter, "花瓣折笺", "会员/周租"),
        ChatAppearanceCatalogOption(ChatAppearanceSupport.SakuraMoon, "月弧夜樱", "会员/周租"),
        ChatAppearanceCatalogOption("paipai-stamp", "派派邮票", "积分"),
        ChatAppearanceCatalogOption("star-ring", "星环", "积分"),
        ChatAppearanceCatalogOption("tide", "潮汐", "积分"),
    )
}
