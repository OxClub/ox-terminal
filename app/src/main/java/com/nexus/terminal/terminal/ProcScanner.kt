package com.nexus.terminal.terminal

import android.system.Os
import android.system.OsConstants
import java.io.File

data class ProcInfo(val pid: Int, val ppid: Int, val name: String, val state: Char, val ticks: Long, val rssKb: Long)

object ProcScanner {
    /** Parses /proc/<pid>/stat. The command name may contain spaces/parentheses, so split at the last ')'. */
    fun parseStat(pid: Int, stat: String): ProcInfo? {
        val l = stat.indexOf('(')
        val r = stat.lastIndexOf(')')
        if (l < 0 || r < l || r + 2 > stat.length) return null
        val rest = stat.substring(r + 2).trim().split(" ")
        if (rest.size < 13) return null
        val ticks = (rest[11].toLongOrNull() ?: 0L) + (rest[12].toLongOrNull() ?: 0L)
        return ProcInfo(pid, rest[1].toIntOrNull() ?: 0, stat.substring(l + 1, r), rest[0].firstOrNull() ?: '?', ticks, 0L)
    }

    private val pageKb: Long by lazy { runCatching { Os.sysconf(OsConstants._SC_PAGESIZE) / 1024 }.getOrDefault(4L) }
    val clockTicks: Long by lazy { runCatching { Os.sysconf(OsConstants._SC_CLK_TCK) }.getOrDefault(100L) }

    /** Lists processes visible to this app (same-UID processes are readable on Android). */
    fun scan(): List<ProcInfo> {
        val dirs = File("/proc").listFiles { f -> f.isDirectory && f.name.all { it.isDigit() } } ?: return emptyList()
        return dirs.mapNotNull { d ->
            val pid = d.name.toIntOrNull() ?: return@mapNotNull null
            try {
                val p = parseStat(pid, File(d, "stat").readText()) ?: return@mapNotNull null
                val rss = runCatching { File(d, "statm").readText().trim().split(" ")[1].toLong() * pageKb }.getOrDefault(0L)
                p.copy(rssKb = rss)
            } catch (e: Exception) { null }
        }
    }

    fun descendants(all: List<ProcInfo>, root: Int): List<ProcInfo> {
        val byParent = all.groupBy { it.ppid }
        val out = ArrayList<ProcInfo>()
        val queue = ArrayDeque<Int>().apply { add(root) }
        while (queue.isNotEmpty()) {
            val p = queue.removeFirst()
            byParent[p]?.forEach { out.add(it); queue.add(it.pid) }
        }
        return out
    }
}
