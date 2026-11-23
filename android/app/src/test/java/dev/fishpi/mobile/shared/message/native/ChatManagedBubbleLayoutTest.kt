package dev.fishpi.mobile.shared.message.native

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatManagedBubbleLayoutTest {
    @Test
    fun memoPadsLikeWebShape() {
        val layout = managedBubbleLayout(ChatBubbleShape.MemoV1, own = false, hasMotif = false, hasImage = false)
        assertEquals(11, layout.paddingTopDp)
        assertEquals(46, layout.paddingRightDp)
        assertEquals(10, layout.paddingBottomDp)
        assertEquals(30, layout.paddingLeftDp)
        assertEquals(34, layout.stickerSizeDp)
        assertEquals(9, layout.stickerMarginDp)
        assertEquals(1, layout.stickerBottomDp)
        assertEquals(82, layout.minWidthDp)
    }

    @Test
    fun ownSwapsHorizontalPaddingForShapes() {
        val layout = managedBubbleLayout(ChatBubbleShape.FishV1, own = true, hasMotif = false, hasImage = false)
        assertEquals(35, layout.paddingLeftDp)
        assertEquals(44, layout.paddingRightDp)
        assertFalse(layout.stickerOnStart)
        assertEquals(7, layout.stickerMarginDp)
    }

    @Test
    fun lotusStickerSitsTwelveDeep() {
        val layout = managedBubbleLayout(ChatBubbleShape.LotusV1, own = false, hasMotif = false, hasImage = true)
        assertEquals(12, layout.stickerMarginDp)
        assertTrue(layout.stickerOnStart.not())
    }

    @Test
    fun plainBubbleUsesCompactPadding() {
        val layout = managedBubbleLayout(null, own = false, hasMotif = false, hasImage = false)
        assertEquals(8, layout.paddingTopDp)
        assertEquals(12, layout.paddingRightDp)
        assertEquals(8, layout.paddingBottomDp)
        assertEquals(12, layout.paddingLeftDp)
        assertEquals(15, layout.stickerSizeDp)
        assertEquals(6, layout.stickerBottomDp)
        assertEquals(0, layout.minWidthDp)
    }

    @Test
    fun decorationReservesThirtyOnTrailingSide() {
        val other = managedBubbleLayout(null, own = false, hasMotif = true, hasImage = false)
        assertEquals(30, other.paddingRightDp)
        assertEquals(12, other.paddingLeftDp)
        val mine = managedBubbleLayout(null, own = true, hasMotif = false, hasImage = true)
        assertEquals(30, mine.paddingLeftDp)
        assertEquals(12, mine.paddingRightDp)
        assertEquals(15, mine.stickerSizeDp)
    }

    @Test
    fun stickerFlipsOnlyForOwnFishShape() {
        assertTrue(managedBubbleLayout(ChatBubbleShape.MemoV1, own = true, hasMotif = false, hasImage = true).stickerOnStart)
        assertTrue(managedBubbleLayout(null, own = false, hasMotif = false, hasImage = true).stickerOnStart.not())
        assertTrue(managedBubbleLayout(ChatBubbleShape.FishV1, own = false, hasMotif = false, hasImage = true).stickerOnStart)
    }
}