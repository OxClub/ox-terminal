package com.nexus.terminal.util

import org.junit.Test
import kotlin.test.assertEquals

class TsvTest {
    @Test
    fun testEncodeDecode() {
        val rows = listOf(
            listOf("a", "b\tc", "d\ne"),
            listOf("x", "y", "z")
        )
        val text = Tsv.encodeAll(rows)
        val decoded = Tsv.decodeAll(text)
        assertEquals(rows, decoded)
    }

    @Test
    fun testEscapeSequences() {
        assertEquals("a\\tb\\nc", Tsv.esc("a\tb\nc"))
        assertEquals("a\tb\nc", Tsv.unesc("a\\tb\\nc"))
    }
}
