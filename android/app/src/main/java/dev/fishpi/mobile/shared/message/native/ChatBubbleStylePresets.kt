package dev.fishpi.mobile.shared.message.native

internal object ChatBubbleStylePresets {
    const val Hexiaotun = "bubble-custom-hexiaotun"
    const val Momota = "bubble-custom-momota"
    const val Paixiaoyu = "bubble-custom-paixiaoyu"
    const val Yubingmiao = "bubble-custom-yubingmiao"

    private val presets: Map<String, String> = mapOf(
        Hexiaotun to """
            {"image":"https://file.fishpi.cn/yui/files/2026/11/13/271705.webp","shadow":false,"shape":"lotus-v1","light":{"border":"#829a59","ownBackground":"#e0edbf","background":"#e0edbf","ink":"#829a59","text":"#314525"},"borderWidth":1,"dark":{"border":"#839661","ownBackground":"#2d3927","background":"#2d3927","ink":"#839661","text":"#edf4dd"},"motif":"leaf","radius":14}
        """.trimIndent(),
        Momota to """
            {"image":"https://file.fishpi.cn/yui/files/2026/11/13/271706.webp","shadow":false,"shape":"memo-v1","light":{"border":"#b18366","ownBackground":"#f8e8d1","background":"#f8e8d1","ink":"#b18366","text":"#473126"},"borderWidth":1,"dark":{"border":"#a37657","ownBackground":"#3a2a22","background":"#3a2a22","ink":"#a37657","text":"#fff0db"},"motif":"paw","radius":14}
        """.trimIndent(),
        Paixiaoyu to """
            {"image":"https://file.fishpi.cn/yui/files/2026/11/13/271707.webp","shadow":false,"shape":"fish-v1","light":{"border":"#c5825b","ownBackground":"#ffd6be","background":"#ffd6be","ink":"#c5825b","text":"#542d23"},"borderWidth":1,"dark":{"border":"#b97b52","ownBackground":"#472e25","background":"#472e25","ink":"#b97b52","text":"#ffe9dd"},"motif":"fish","radius":14}
        """.trimIndent(),
        Yubingmiao to """
            {"image":"https://file.fishpi.cn/yui/files/2026/11/13/271708.webp","shadow":false,"shape":"cat-v1","light":{"border":"#ab8eb3","ownBackground":"#eee0f3","background":"#eee0f3","ink":"#ab8eb3","text":"#432e49"},"borderWidth":1,"dark":{"border":"#a38bb1","ownBackground":"#382a40","background":"#382a40","ink":"#a38bb1","text":"#f8eafa"},"motif":"bell","radius":14}
        """.trimIndent(),
    )

    val ids: Set<String> get() = presets.keys

    fun json(id: String?): String? = presets[id?.trim()]
}
