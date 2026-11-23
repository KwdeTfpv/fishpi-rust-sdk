package dev.fishpi.mobile.shared.message.native

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatSakuraTimingTest {

    @Test
    fun easingHitsEndpointsAndStaysMonotonic() {
        assertEquals(0f, cubicBezierEase(0.18f, 0.51f, 0.54f, 1f, 0f), 0.0001f)
        assertEquals(1f, cubicBezierEase(0.18f, 0.51f, 0.54f, 1f, 1f), 0.0001f)
        var previous = -1f
        for (step in 0..20) {
            val value = cubicBezierEase(0.18f, 0.51f, 0.54f, 1f, step / 20f)
            assertTrue("monotonic at $step", value >= previous)
            previous = value
        }
        val mid = cubicBezierEase(0.18f, 0.51f, 0.54f, 1f, 0.5f)
        assertTrue("ease-out should pass halfway early: $mid", mid > 0.5f)
    }

    @Test
    fun petalAppearsEarlyAndFadesAfterFourteenPercent() {
        assertEquals(0f, SakuraDuo.driftState(0f, 0).alpha, 0.0001f)
        val appearing = SakuraDuo.driftState(0.10f, 0)
        assertTrue("petal should be visible while rising: ${appearing.alpha}", appearing.alpha > 0.8f)
        val drifting = SakuraDuo.driftState(0.20f, 0)
        assertTrue("petal should fade while drifting: ${drifting.alpha}", drifting.alpha in 0.1f..0.85f)
        assertEquals(0f, SakuraDuo.driftState(0.5f, 0).alpha, 0.0001f)
    }

    @Test
    fun petalScaleGrowsThenShrinksToDriftSize() {
        assertTrue(SakuraDuo.driftState(0.02f, 0).scale < 0.5f)
        assertEquals(1f, SakuraDuo.driftState(0.14f, 0).scale, 0.02f)
        assertEquals(0.63f, SakuraDuo.driftState(0.30f, 0).scale, 0.001f)
    }

    @Test
    fun sealBloomsAtSixPercentAndSettles() {
        assertEquals(1f, SakuraDuo.sealBloom(0f), 0.001f)
        assertEquals(1.34f, SakuraDuo.sealBloom(0.06f), 0.001f)
        assertEquals(1f, SakuraDuo.sealBloom(0.16f), 0.001f)
        assertEquals(16f, SakuraDuo.sealRotation(0.06f), 0.001f)
        assertEquals(0f, SakuraDuo.sealRotation(0.5f), 0.001f)
    }

    @Test
    fun moonArcRisesAndDisappearsWithinTwentyTwoPercent() {
        assertEquals(0f, SakuraDuo.arcAlpha(0f), 0.001f)
        assertTrue(SakuraDuo.arcAlpha(0.08f) > 0.7f)
        assertEquals(0f, SakuraDuo.arcAlpha(0.22f), 0.001f)
        assertEquals(0f, SakuraDuo.arcAlpha(0.6f), 0.001f)
    }

    @Test
    fun moonArcTransformFollowsWebKeyframes() {
        val start = SakuraDuo.arcState(0f, 2f)
        assertEquals(0.25f, start.scaleX, 0.001f)
        assertEquals(0.65f, start.scaleY, 0.001f)
        assertEquals(-8f, start.rotation, 0.001f)
        assertEquals(0f, start.translateYPx, 0.001f)
        val end = SakuraDuo.arcState(0.22f, 2f)
        assertEquals(1.08f, end.scaleX, 0.001f)
        assertEquals(1.18f, end.scaleY, 0.001f)
        assertEquals(3f, end.rotation, 0.001f)
        assertEquals(12f, end.translateYPx, 0.001f)
        val held = SakuraDuo.arcState(0.6f, 2f)
        assertEquals(1.08f, held.scaleX, 0.001f)
        assertEquals(0f, held.alpha, 0.001f)
    }

    @Test
    fun petalCycleDurationMatchesWebAnimation() {
        assertEquals(5000L, SakuraDuo.durationMs)
    }
}
