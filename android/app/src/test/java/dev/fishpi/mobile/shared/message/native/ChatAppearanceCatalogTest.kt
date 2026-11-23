package dev.fishpi.mobile.shared.message.native

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatAppearanceCatalogTest {
    @Test
    fun `every offered bubble is drawable`() {
        val missing = ChatAppearanceCatalog.bubbles.map { it.id }.filterNot { ChatAppearanceSupport.supportsBubble(it) }
        assertTrue("catalog bubbles without renderer: $missing", missing.isEmpty())
    }

    @Test
    fun `every offered frame is drawable`() {
        val missing = ChatAppearanceCatalog.frames.map { it.id }.filterNot { ChatAppearanceSupport.supportsFrame(it) }
        assertTrue("catalog frames without renderer: $missing", missing.isEmpty())
    }

    @Test
    fun `drawable styles are offered except defaults and legacy aliases`() {
        val offeredBubbles = ChatAppearanceCatalog.bubbles.map { it.id }.toSet()
        val offeredFrames = ChatAppearanceCatalog.frames.map { it.id }.toSet()
        assertEquals(
            setOf("classic", ChatAppearanceSupport.NightSakura),
            ChatAppearanceSupport.bubbles - offeredBubbles,
        )
        assertEquals(
            setOf(ChatAppearanceSupport.NightSakura),
            ChatAppearanceSupport.frames - offeredFrames,
        )
    }

    @Test
    fun `classic is the implicit default and is not offered as a bubble option`() {
        assertFalse(ChatAppearanceCatalog.bubbles.any { it.id == "classic" })
        assertTrue(ChatAppearanceSupport.supportsBubble("classic"))
    }

    @Test
    fun `defaults are supported`() {
        assertTrue(ChatAppearanceSupport.supportsBubble("classic"))
        assertTrue(ChatAppearanceSupport.supportsFrame("none"))
    }

    @Test
    fun `sakura letter is the offered id and night sakura stays a legacy alias`() {
        assertTrue(ChatAppearanceSupport.supportsBubble(ChatAppearanceSupport.SakuraLetter))
        assertTrue(ChatAppearanceSupport.supportsFrame(ChatAppearanceSupport.SakuraLetter))
        assertTrue(ChatAppearanceSupport.supportsBubble(ChatAppearanceSupport.NightSakura))
        assertTrue(ChatAppearanceSupport.supportsFrame(ChatAppearanceSupport.NightSakura))
        assertTrue(ChatAppearanceSupport.isSakuraBubble(ChatAppearanceSupport.NightSakura))
        assertTrue(ChatAppearanceSupport.isSakuraFrame(ChatAppearanceSupport.SakuraLetter))
        assertEquals(
            ChatAppearanceSupport.SakuraLetter,
            ChatAppearanceSupport.canonicalBubbleId(ChatAppearanceSupport.NightSakura),
        )
        assertEquals(
            ChatAppearanceSupport.SakuraLetter,
            ChatAppearanceSupport.canonicalFrameId(ChatAppearanceSupport.NightSakura),
        )
        assertEquals("paipai-orange", ChatAppearanceSupport.canonicalBubbleId("paipai-orange"))
    }

    @Test
    fun `unknown and blank ids fall back`() {
        assertFalse(ChatAppearanceSupport.supportsBubble(null))
        assertFalse(ChatAppearanceSupport.supportsFrame(""))
        assertFalse(ChatAppearanceSupport.supportsBubble("bubble-unknown"))
        assertFalse(ChatAppearanceSupport.supportsFrame("frame-unknown"))
    }

    @Test
    fun `sakura moon is offered and drawable`() {
        assertTrue(ChatAppearanceSupport.supportsBubble(ChatAppearanceSupport.SakuraMoon))
        assertTrue(ChatAppearanceSupport.supportsFrame(ChatAppearanceSupport.SakuraMoon))
        assertTrue(ChatAppearanceCatalog.bubbles.any { it.id == ChatAppearanceSupport.SakuraMoon })
        assertTrue(ChatAppearanceCatalog.frames.any { it.id == ChatAppearanceSupport.SakuraMoon })
        assertEquals("sakura-moon", ChatAppearanceSupport.canonicalBubbleId(ChatAppearanceSupport.SakuraMoon))
    }

    @Test
    fun `dynamic bubble presets are offered and carry a style`() {
        val offered = ChatAppearanceCatalog.bubbles.map { it.id }.toSet()
        ChatBubbleStylePresets.ids.forEach { id ->
            assertTrue("$id missing from catalog", id in offered)
            assertTrue("$id not drawable", ChatAppearanceSupport.supportsBubble(id))
            val json = ChatBubbleStylePresets.json(id)
            assertTrue("$id has no preset", json != null && json.isNotBlank())
        }
        assertTrue(ChatBubbleStylePresets.json(null) == null)
        assertTrue(ChatBubbleStylePresets.json("bubble-unknown") == null)
    }
}
