package dev.fishpi.mobile.shared.message

import dev.fishpi.mobile.data.ChatAppearanceSnapshot
import dev.fishpi.mobile.data.ChatRoomMessage
import dev.fishpi.mobile.data.ReactionSummaryItem
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageUpdatePlannerTest {

    private fun item(
        content: String = "hello",
        oId: String = "1",
        appearance: ChatAppearanceSnapshot? = null,
        reactions: List<ReactionSummaryItem> = emptyList(),
        echoKey: String = "",
        avatarUrl: String = "https://file.fishpi.cn/avatar/a.png",
    ): ChatListItem {
        val message = ChatRoomMessage(
            oId = oId,
            userName = "tester",
            content = content,
            time = "2026-11-20 16:00",
            reactionSummary = reactions,
            appearance = appearance,
            userAvatarURL = avatarUrl,
        )
        return ChatListItem(
            message = message,
            separator = null,
            renderHints = message.toRenderHints(),
            echoKey = echoKey,
        )
    }

    @Test
    fun firstBindIsFull() {
        assertEquals(MessageUpdateKind.Full, MessageUpdatePlanner.plan(null, item(), sameContext = true))
    }

    @Test
    fun contextChangeIsFull() {
        assertEquals(
            MessageUpdateKind.Full,
            MessageUpdatePlanner.plan(item(), item(), sameContext = false),
        )
    }

    @Test
    fun keyChangeIsFull() {
        assertEquals(
            MessageUpdateKind.Full,
            MessageUpdatePlanner.plan(item(oId = "1"), item(oId = "2"), sameContext = true),
        )
    }

    @Test
    fun appearanceOnlyChangeKeepsBody() {
        val before = item(appearance = ChatAppearanceSnapshot("classic", "none"))
        val after = item(appearance = ChatAppearanceSnapshot("paipai-orange", "paipai-orange"))
        assertEquals(
            MessageUpdateKind.AppearanceOnly,
            MessageUpdatePlanner.plan(before, after, sameContext = true),
        )
    }

    @Test
    fun appearanceWithContentChangeIsFull() {
        val before = item(content = "hello", appearance = ChatAppearanceSnapshot("classic", "none"))
        val after = item(content = "hello world", appearance = ChatAppearanceSnapshot("candy", "gold"))
        assertEquals(
            MessageUpdateKind.Full,
            MessageUpdatePlanner.plan(before, after, sameContext = true),
        )
    }

    @Test
    fun reactionOnlyChangeIsReactionUpdate() {
        val before = item(reactions = listOf(ReactionSummaryItem(value = "like", emoji = "👍", count = 1, selected = false)))
        val after = item(reactions = listOf(ReactionSummaryItem(value = "like", emoji = "👍", count = 2, selected = true)))
        assertEquals(
            MessageUpdateKind.ReactionsOnly,
            MessageUpdatePlanner.plan(before, after, sameContext = true),
        )
    }

    @Test
    fun reactionWithContentChangeIsFull() {
        val before = item(content = "a", reactions = listOf(ReactionSummaryItem(value = "like", emoji = "👍", count = 1, selected = false)))
        val after = item(content = "b", reactions = listOf(ReactionSummaryItem(value = "like", emoji = "👍", count = 2, selected = false)))
        assertEquals(
            MessageUpdateKind.Full,
            MessageUpdatePlanner.plan(before, after, sameContext = true),
        )
    }

    @Test
    fun avatarOnlyChangeKeepsBubble() {
        val before = item(avatarUrl = "https://file.fishpi.cn/avatar/a.png")
        val after = item(avatarUrl = "https://file.fishpi.cn/avatar/b.png")
        assertEquals(
            MessageUpdateKind.AvatarOnly,
            MessageUpdatePlanner.plan(before, after, sameContext = true),
        )
    }

    @Test
    fun avatarWithAppearanceChangeIsFull() {
        val before = item(avatarUrl = "https://file.fishpi.cn/avatar/a.png", appearance = ChatAppearanceSnapshot("classic", "none"))
        val after = item(avatarUrl = "https://file.fishpi.cn/avatar/b.png", appearance = ChatAppearanceSnapshot("candy", "gold"))
        assertEquals(
            MessageUpdateKind.Full,
            MessageUpdatePlanner.plan(before, after, sameContext = true),
        )
    }

    @Test
    fun avatarWithContentChangeIsFull() {
        val before = item(content = "a", avatarUrl = "https://file.fishpi.cn/avatar/a.png")
        val after = item(content = "b", avatarUrl = "https://file.fishpi.cn/avatar/b.png")
        assertEquals(
            MessageUpdateKind.Full,
            MessageUpdatePlanner.plan(before, after, sameContext = true),
        )
    }

    @Test
    fun identicalItemIsFull() {
        assertEquals(
            MessageUpdateKind.Full,
            MessageUpdatePlanner.plan(item(), item(), sameContext = true),
        )
    }
}
