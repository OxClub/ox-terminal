package com.nexus.terminal.util

/** Tiny escaped tab-separated codec used for persisted lists (history, bookmarks, aliases...). */
object Tsv {
    fun esc(s: String) = s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r")

    fun unesc(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (val n = s[i + 1]) {
                    't' -> sb.append('\t')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    '\\' -> sb.append('\\')
                    else -> sb.append(c).append(n)
                }
                i += 2
            } else {
                sb.append(c); i++
            }
        }
        return sb.toString()
    }

    fun encode(vararg fields: String) = fields.joinToString("\t") { esc(it) }
    fun decode(line: String): List<String> = line.split("\t").map { unesc(it) }
    fun decodeAll(text: String): List<List<String>> =
        text.lines().filter { it.isNotBlank() }.map { decode(it) }
    fun encodeAll(rows: List<List<String>>): String =
        rows.joinToString("\n") { encode(*it.toTypedArray()) }
}
