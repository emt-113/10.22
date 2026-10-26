package com.aizeek.phonepulse

import com.aizeek.phonepulse.ui.theme.ReadingFontStore
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingFontSnapTest {

    @Test
    fun `snap picks the nearest tick`() {
        assertEquals(14, ReadingFontStore.snap(13.6f))
        assertEquals(14, ReadingFontStore.snap(14.4f))
    }

    @Test
    fun `midpoint rounds to the upper tick`() {
        assertEquals(15, ReadingFontStore.snap(14.5f))
    }

    @Test
    fun `out-of-range values clamp to the ruler ends`() {
        assertEquals(ReadingFontStore.MIN_SP, ReadingFontStore.snap(3f))
        assertEquals(ReadingFontStore.MAX_SP, ReadingFontStore.snap(99f))
    }
}
