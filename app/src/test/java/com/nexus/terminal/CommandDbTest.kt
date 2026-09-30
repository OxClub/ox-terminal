package com.nexus.terminal.terminal

import org.junit.Test
import kotlin.test.assertTrue

class CommandDbTest {
    @Test
    fun testSearch() {
        val results = CommandDb.search("ls")
        assertTrue(results.any { it.name == "ls" })
    }

    @Test
    fun testCategories() {
        assertTrue(CommandDb.categories.isNotEmpty())
        assertTrue(CommandDb.categories.contains("Files"))
    }
}
