package com.aizeek.phonepulse

import com.aizeek.phonepulse.ui.components.UsageBand
import com.aizeek.phonepulse.ui.components.UsageFilterEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageFilterEngineTest {

    // --- band classification ------------------------------------------------

    @Test
    fun `exactly 30 minutes is high frequency`() {
        assertEquals(UsageBand.HIGH, UsageFilterEngine.bandOf(30 * 60_000L))
    }

    @Test
    fun `29 minutes and 59 seconds is mid frequency`() {
        assertEquals(UsageBand.MID, UsageFilterEngine.bandOf(30 * 60_000L - 1))
    }

    @Test
    fun `exactly 5 minutes is mid frequency`() {
        assertEquals(UsageBand.MID, UsageFilterEngine.bandOf(5 * 60_000L))
    }

    @Test
    fun `just under 5 minutes is low frequency`() {
        assertEquals(UsageBand.LOW, UsageFilterEngine.bandOf(5 * 60_000L - 1))
    }

    @Test
    fun `zero usage is low frequency`() {
        assertEquals(UsageBand.LOW, UsageFilterEngine.bandOf(0L))
    }

    // --- band matching -------------------------------------------------------

    @Test
    fun `empty band selection matches everything`() {
        assertTrue(UsageFilterEngine.matchesBands(emptySet(), 0L))
        assertTrue(UsageFilterEngine.matchesBands(emptySet(), 60 * 60_000L))
    }

    @Test
    fun `several selected bands are an OR`() {
        val bands = setOf(UsageBand.LOW, UsageBand.HIGH)
        assertTrue(UsageFilterEngine.matchesBands(bands, 10_000L))
        assertTrue(UsageFilterEngine.matchesBands(bands, 45 * 60_000L))
        assertFalse(UsageFilterEngine.matchesBands(bands, 10 * 60_000L))
    }

    // --- tag matching --------------------------------------------------------

    @Test
    fun `empty tag list matches everything`() {
        assertTrue(UsageFilterEngine.matchesTags(emptyList(), "微信", "com.tencent.mm"))
    }

    @Test
    fun `any tag hit matches case-insensitively across name and package`() {
        assertTrue(UsageFilterEngine.matchesTags(listOf("微信"), "微信", "com.tencent.mm"))
        assertTrue(UsageFilterEngine.matchesTags(listOf("tencent"), "微信", "com.tencent.mm"))
        assertTrue(UsageFilterEngine.matchesTags(listOf("WECHAT"), "WeChat", "com.tencent.mm"))
        assertFalse(UsageFilterEngine.matchesTags(listOf("支付宝"), "微信", "com.tencent.mm"))
    }

    // --- combined semantics (bands AND tags) ---------------------------------

    @Test
    fun `band and tag filters combine with AND`() {
        val bands = setOf(UsageBand.HIGH)
        val tags = listOf("music")
        val app = Triple("网易云音乐", "com.netease.cloudmusic", 40 * 60_000L)
        val other = Triple("网易云音乐", "com.netease.cloudmusic", 3 * 60_000L)

        val passes = { t: Triple<String, String, Long> ->
            UsageFilterEngine.matchesBands(bands, t.third) &&
                UsageFilterEngine.matchesTags(tags, t.first, t.second)
        }
        assertTrue(passes(app))
        assertFalse(passes(other))
    }
}
