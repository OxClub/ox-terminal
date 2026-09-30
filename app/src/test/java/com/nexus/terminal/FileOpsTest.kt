package com.nexus.terminal.files

import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertTrue

class FileOpsTest {
    @Test
    fun testTextDetection() {
        val tmp = Files.createTempFile("test", ".txt").toFile()
        tmp.writeText("hello world")
        assertTrue(FileOps.isText(tmp))
    }

    @Test
    fun testFormatSize() {
        assertEquals("1.0 KB", FileOps.formatSize(1024))
        assertEquals("1.0 MB", FileOps.formatSize(1024 * 1024))
    }

    private fun assertEquals(expected: String, actual: String) = kotlin.test.assertEquals(expected, actual)
}
