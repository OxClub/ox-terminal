package com.nexus.terminal.data

import org.junit.Test
import org.junit.Before
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class HistoryStoreTest {
    private lateinit var tmpFile: File

    @Before
    fun setup() {
        tmpFile = Files.createTempFile("history", ".tsv").toFile()
    }

    @Test
    fun testAdd() {
        val store = HistoryStore(tmpFile)
        store.add("ls -la")
        store.add("cd ~")
        val all = store.all()
        assertEquals(2, all.size)
        assertEquals("cd ~", all[0].cmd)
        assertEquals("ls -la", all[1].cmd)
    }

    @Test
    fun testDuplicates() {
        val store = HistoryStore(tmpFile)
        store.add("echo hi")
        val added = store.add("echo hi")
        assertFalse(added)
        assertEquals(1, store.size())
    }

    @Test
    fun testSensitive() {
        val store = HistoryStore(tmpFile)
        store.add("export PASSWORD=secret")
        assertEquals(0, store.size())
    }

    @Test
    fun testPersist() {
        run { val store = HistoryStore(tmpFile); store.add("persisted"); store.add("cmd") }
        val store2 = HistoryStore(tmpFile)
        assertEquals(2, store2.size())
        assertEquals("cmd", store2.all()[0].cmd)
    }
}
