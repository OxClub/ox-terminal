package com.nexus.terminal.data

import com.nexus.terminal.util.Tsv
import java.io.File

/** Persistent command history (file backed, newest last on disk, newest first via [all]). */
class HistoryStore(private val file: File, private var max: Int = 1000) {
    data class Entry(val ts: Long, val cmd: String)

    private val items = ArrayList<Entry>()

    init { load() }

    @Synchronized fun load() {
        items.clear()
        if (!file.exists()) return
        file.readLines().forEach { line ->
            val f = Tsv.decode(line)
            if (f.size >= 2) f[0].toLongOrNull()?.let { items.add(Entry(it, f[1])) }
        }
    }

    @Synchronized fun add(cmd: String, ts: Long = System.currentTimeMillis()): Boolean {
        val c = cmd.trim()
        if (c.isEmpty() || isSensitive(c)) return false
        if (items.lastOrNull()?.cmd == c) return false
        items.add(Entry(ts, c))
        trim()
        save()
        return true
    }

    @Synchronized fun all(): List<Entry> = items.toList().asReversed()

    @Synchronized fun search(q: String): List<Entry> =
        if (q.isBlank()) all() else all().filter { it.cmd.contains(q, ignoreCase = true) }

    @Synchronized fun remove(e: Entry) { items.remove(e); save() }

    @Synchronized fun clear() { items.clear(); save() }

    @Synchronized fun setMax(n: Int) { max = n.coerceAtLeast(1); trim(); save() }

    @Synchronized fun size() = items.size

    private fun trim() { while (items.size > max) items.removeAt(0) }

    private fun save() {
        file.parentFile?.mkdirs()
        file.writeText(items.joinToString("\n") { Tsv.encode(it.ts.toString(), it.cmd) })
    }

    companion object {
        private val secret = Regex("(?i)(password|passwd|secret|token|api[_-]?key)\\s*=")
        /** Commands that visibly carry credentials are never persisted. */
        fun isSensitive(cmd: String) = secret.containsMatchIn(cmd)
    }
}
