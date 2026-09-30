package com.nexus.terminal.terminal

/** Reconstructs the command line the user typed so it can be stored in history. */
class LineTracker {
    private val sb = StringBuilder()

    fun type(cp: Int) {
        when {
            cp == 13 || cp == 10 -> Unit
            cp == 127 || cp == 8 -> if (sb.isNotEmpty()) sb.setLength(sb.length - 1)
            cp >= 32 -> sb.appendCodePoint(cp)
        }
    }

    fun append(text: String) { text.codePoints().forEach { type(it) } }
    fun reset() { sb.setLength(0) }
    fun take(): String { val s = sb.toString(); sb.setLength(0); return s }
    fun peek(): String = sb.toString()
}
