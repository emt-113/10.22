package com.aizeek.phonepulse

import com.aizeek.phonepulse.intervention.ReturnGiftEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReturnGiftEngineTest {

    @Test
    fun `short absence is not a rest`() {
        assertNull(ReturnGiftEngine.compute(0, 0))
        assertNull(ReturnGiftEngine.compute(2, 0))
    }

    @Test
    fun `minimum rest earns one stardust`() {
        val gift = ReturnGiftEngine.compute(3, 0)!!
        assertEquals(3, gift.gapMinutes)
        assertEquals(1, gift.stardustEarned)
        assertEquals(1, gift.totalStardust)
    }

    @Test
    fun `stardust scales with the gap`() {
        assertEquals(2, ReturnGiftEngine.compute(5, 0)!!.stardustEarned)
        assertEquals(2, ReturnGiftEngine.compute(9, 0)!!.stardustEarned)
        assertEquals(3, ReturnGiftEngine.compute(10, 0)!!.stardustEarned)
        assertEquals(3, ReturnGiftEngine.compute(14, 0)!!.stardustEarned)
    }

    @Test
    fun `long rest is capped`() {
        assertEquals(ReturnGiftEngine.MAX_STARDUST_PER_GAP, ReturnGiftEngine.compute(60, 0)!!.stardustEarned)
    }

    @Test
    fun `total accumulates across rests`() {
        val first = ReturnGiftEngine.compute(5, 0)!!
        val second = ReturnGiftEngine.compute(10, first.totalStardust)!!
        assertEquals(first.stardustEarned + second.stardustEarned, second.totalStardust)
    }
}
