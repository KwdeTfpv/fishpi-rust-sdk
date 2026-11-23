package dev.fishpi.mobile.shared.message.native

internal object ChatAppearanceSupport {
    const val NightSakura = "night-sakura"
    const val SakuraLetter = "sakura-letter"
    const val SakuraMoon = "sakura-moon"
    const val RoomAvatarInnerDp = 36
    const val RoomAvatarSlotRingDp = 4

    private val sakuraIds = setOf(NightSakura, SakuraLetter, SakuraMoon)

    val bubbles: Set<String> = setOf(
        "classic",
        "paipai-orange",
        "fishpond-blue",
        "candy",
        "sea-soda",
        "sunset-letter",
        "star-route",
        SakuraLetter,
        SakuraMoon,
        NightSakura,
    ) + ChatBubbleStylePresets.ids

    val frames: Set<String> = setOf(
        "none",
        "paipai-orange",
        "fishpond-blue",
        "gold",
        "paipai-stamp",
        "star-ring",
        "tide",
        SakuraLetter,
        SakuraMoon,
        NightSakura,
    )

    fun supportsBubble(id: String?): Boolean = id != null && id in bubbles

    fun supportsFrame(id: String?): Boolean = id != null && id in frames

    fun isSakuraBubble(id: String?): Boolean = id != null && id in sakuraIds

    fun isSakuraFrame(id: String?): Boolean = id != null && id in sakuraIds

    fun canonicalBubbleId(id: String): String = if (id == NightSakura) SakuraLetter else id

    fun canonicalFrameId(id: String): String = if (id == NightSakura) SakuraLetter else id
}
