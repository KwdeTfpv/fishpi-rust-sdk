package dev.fishpi.mobile.shared.message.native

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatBubbleStyleParseTest {

    private val image = "https://file.fishpi.cn/yui/files/2026/11/13/271706.webp"

    private fun palette(
        background: Any? = "#f8e8d1",
        ownBackground: Any? = "#f8e8d1",
        border: Any? = "#b18366",
        ink: Any? = "#b18366",
        text: Any? = "#473126",
    ): Map<String, Any?> = mapOf(
        "background" to background,
        "ownBackground" to ownBackground,
        "border" to border,
        "ink" to ink,
        "text" to text,
    )

    private fun fields(vararg overrides: Pair<String, Any?>): Map<String, Any?> {
        val base = mutableMapOf<String, Any?>(
            "light" to palette(),
            "dark" to palette(
                background = "#3a2a22",
                ownBackground = "#3a2a22",
                border = "#a37657",
                ink = "#a37657",
                text = "#fff0db",
            ),
            "radius" to 14,
            "borderWidth" to 1,
            "shadow" to false,
            "motif" to "paw",
            "image" to image,
        )
        overrides.forEach { (key, value) ->
            if (value === Remove) base.remove(key) else base[key] = value
        }
        return base
    }

    @Test
    fun decodesCollectedPayload() {
        val style = ChatBubbleStyle.parseFields(fields())!!
        assertEquals(ChatBubbleMotif.Paw, style.motif)
        assertNull(style.shape)
        assertEquals(14f, style.radius)
        assertEquals(1f, style.borderWidth)
        assertEquals(false, style.shadow)
        assertEquals(image, style.imageUrl)
        assertEquals(0xFFF8E8D1.toInt(), style.light.background)
        assertEquals(0xFFFFF0DB.toInt(), style.dark.text)
    }

    @Test
    fun acceptsEmptyImageAndMissingShape() {
        val style = ChatBubbleStyle.parseFields(fields("image" to "", "shape" to Remove))!!
        assertEquals("", style.imageUrl)
        assertNull(style.shape)
    }

    @Test
    fun rejectsMissingPalette() {
        assertNull(ChatBubbleStyle.parseFields(fields("light" to Remove)))
        assertNull(ChatBubbleStyle.parseFields(fields("dark" to null)))
    }

    @Test
    fun rejectsInvalidColors() {
        assertNull(ChatBubbleStyle.parseFields(fields("light" to palette(border = "#b1836"))))
        assertNull(ChatBubbleStyle.parseFields(fields("light" to palette(text = "473126"))))
        assertNull(ChatBubbleStyle.parseFields(fields("dark" to palette(ink = 0x473126))))
    }

    @Test
    fun rejectsOutOfRangeNumbers() {
        assertNull(ChatBubbleStyle.parseFields(fields("radius" to 5)))
        assertNull(ChatBubbleStyle.parseFields(fields("radius" to 25)))
        assertNull(ChatBubbleStyle.parseFields(fields("radius" to 14.5)))
        assertNull(ChatBubbleStyle.parseFields(fields("borderWidth" to 3)))
        assertNull(ChatBubbleStyle.parseFields(fields("borderWidth" to -1)))
        assertNull(ChatBubbleStyle.parseFields(fields("radius" to "14")))
    }

    @Test
    fun rejectsInvalidFlagsAndEnums() {
        assertNull(ChatBubbleStyle.parseFields(fields("shadow" to "false")))
        assertNull(ChatBubbleStyle.parseFields(fields("motif" to "star")))
        assertNull(ChatBubbleStyle.parseFields(fields("motif" to Remove)))
        assertNull(ChatBubbleStyle.parseFields(fields("shape" to "star-v1")))
    }

    @Test
    fun acceptsKnownShapes() {
        val shapes = mapOf(
            "memo-v1" to ChatBubbleShape.MemoV1,
            "lotus-v1" to ChatBubbleShape.LotusV1,
            "cat-v1" to ChatBubbleShape.CatV1,
            "fish-v1" to ChatBubbleShape.FishV1,
        )
        shapes.forEach { (raw, expected) ->
            assertEquals(expected, ChatBubbleStyle.parseFields(fields("shape" to raw))?.shape)
        }
    }

    @Test
    fun rejectsUnsafeImageSources() {
        assertNull(ChatBubbleStyle.parseFields(fields("image" to "http://file.fishpi.cn/a.png")))
        assertNull(ChatBubbleStyle.parseFields(fields("image" to "https://evil.example/a.png")))
        assertNull(ChatBubbleStyle.parseFields(fields("image" to "https://file.fishpi.cn/../a.png")))
        assertNull(ChatBubbleStyle.parseFields(fields("image" to "https://file.fishpi.cn/a.svg")))
        assertNull(ChatBubbleStyle.parseFields(fields("image" to Remove)))
        assertNull(ChatBubbleStyle.parseFields(fields("image" to ("https://file.fishpi.cn/" + "a".repeat(512) + ".png"))))
        assertTrue(ChatBubbleStyle.parseFields(fields("image" to "https://file.fishpi.cn/a/b_c-d.webp")) != null)
    }

    private object Remove
}
