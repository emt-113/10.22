package com.aizeek.phonepulse

import com.aizeek.phonepulse.ui.components.TagSearchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagSearchEngineTest {

    private val pool = listOf("微信", "微信读书", "微博", "网易云音乐", "Calendar")

    @Test
    fun `blank query suggests nothing`() {
        assertTrue(TagSearchEngine.suggestions("  ", pool, emptySet()).isEmpty())
        assertTrue(TagSearchEngine.suggestions("", pool, emptySet()).isEmpty())
    }

    @Test
    fun `suggestions match case-insensitively and keep order`() {
        val result = TagSearchEngine.suggestions("cAL", pool, emptySet())
        assertEquals(listOf("Calendar"), result)
        assertEquals(
            listOf("微信", "微信读书", "微博"),
            TagSearchEngine.suggestions("微", pool, emptySet())
        )
    }

    @Test
    fun `already tagged names are excluded`() {
        val result = TagSearchEngine.suggestions("微", pool, setOf("微信"))
        assertEquals(listOf("微信读书", "微博"), result)
    }

    @Test
    fun `suggestions are capped at the limit`() {
        val wide = (1..20).map { "应用$it" }
        val result = TagSearchEngine.suggestions("应用", wide, emptySet())
        assertEquals(6, result.size)
    }

    @Test
    fun `normalizeTag trims whitespace`() {
        assertEquals("微信", TagSearchEngine.normalizeTag("  微信 "))
        assertEquals("", TagSearchEngine.normalizeTag("   "))
    }
}
