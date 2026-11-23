package dev.fishpi.mobile.shared.message.native

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatBubbleShapeCommandsTest {
    @Test
    fun memoMatchesWebFormulaAtFullSize() {
        val commands = ChatBubbleShapePaths.commands(ChatBubbleShape.MemoV1, 240f, 64f)
        assertEquals(
            listOf(
                ChatShapeCommand.MoveTo(0f, 8f),
                ChatShapeCommand.QuadTo(0f, 0f, 8f, 0f),
                ChatShapeCommand.LineTo(225f, 0f),
                ChatShapeCommand.LineTo(239f, 14f),
                ChatShapeCommand.QuadTo(240f, 15f, 240f, 17f),
                ChatShapeCommand.LineTo(240f, 55f),
                ChatShapeCommand.QuadTo(240f, 64f, 231f, 64f),
                ChatShapeCommand.LineTo(8f, 64f),
                ChatShapeCommand.QuadTo(0f, 64f, 0f, 56f),
                ChatShapeCommand.Close,
            ),
            commands,
        )
    }

    @Test
    fun memoClampsToMinimumBox() {
        val commands = ChatBubbleShapePaths.commands(ChatBubbleShape.MemoV1, 60f, 20f)
        assertEquals(
            listOf(
                ChatShapeCommand.MoveTo(0f, 8f),
                ChatShapeCommand.QuadTo(0f, 0f, 8f, 0f),
                ChatShapeCommand.LineTo(67f, 0f),
                ChatShapeCommand.LineTo(81f, 14f),
                ChatShapeCommand.QuadTo(82f, 15f, 82f, 17f),
                ChatShapeCommand.LineTo(82f, 29f),
                ChatShapeCommand.QuadTo(82f, 38f, 73f, 38f),
                ChatShapeCommand.LineTo(8f, 38f),
                ChatShapeCommand.QuadTo(0f, 38f, 0f, 30f),
                ChatShapeCommand.Close,
            ),
            commands,
        )
    }

    @Test
    fun fishUsesHalfHeightTailNotch() {
        val commands = ChatBubbleShapePaths.commands(ChatBubbleShape.FishV1, 200f, 38f)
        assertEquals(ChatShapeCommand.MoveTo(0f, 19f), commands.first())
        assertEquals(ChatShapeCommand.LineTo(194f, 19f), commands[6])
        assertEquals(ChatShapeCommand.LineTo(160f, 0f), commands[2])
        assertEquals(ChatShapeCommand.Close, commands.last())
    }

    @Test
    fun lotusCarriesProportionalControlPoint() {
        val commands = ChatBubbleShapePaths.commands(ChatBubbleShape.LotusV1, 200f, 64f)
        assertEquals(10, commands.size)
        assertEquals(ChatShapeCommand.LineTo(187f, 0f), commands[2])
        assertEquals(ChatShapeCommand.LineTo(190f, 54f), commands[5])
        val proportional = commands[7] as ChatShapeCommand.CubicTo
        assertEquals(110f, proportional.control1X)
        assertEquals(57f, proportional.control1Y)
    }

    @Test
    fun catKeepsEarPointsAndCornerRadius() {
        val commands = ChatBubbleShapePaths.commands(ChatBubbleShape.CatV1, 200f, 64f)
        assertEquals(16, commands.size)
        assertEquals(ChatShapeCommand.LineTo(33f, 9f), commands[5])
        assertEquals(ChatShapeCommand.LineTo(50f, 9f), commands[8])
        assertEquals(ChatShapeCommand.LineTo(188f, 9f), commands[9])
        assertEquals(ChatShapeCommand.LineTo(200f, 53f), commands[11])
        assertEquals(ChatShapeCommand.LineTo(11f, 64f), commands[13])
    }

    @Test
    fun everyShapeStartsClosedContour() {
        for (shape in ChatBubbleShape.entries) {
            val commands = ChatBubbleShapePaths.commands(shape, 96f, 40f)
            assertTrue("$shape must start with move", commands.first() is ChatShapeCommand.MoveTo)
            assertEquals(shape.name, ChatShapeCommand.Close, commands.last())
            assertTrue("$shape must have at least four commands", commands.size >= 4)
        }
    }
}