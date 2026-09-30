package com.nexus.terminal.terminal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Sticky CTRL / ALT / SHIFT state driven by the extra-keys toolbar (one-shot). */
object Modifiers {
    var ctrl by mutableStateOf(false)
    var alt by mutableStateOf(false)
    var shift by mutableStateOf(false)

    fun consumeCtrl(): Boolean { val v = ctrl; if (v) ctrl = false; return v }
    fun consumeAlt(): Boolean { val v = alt; if (v) alt = false; return v }
    fun consumeShift(): Boolean { val v = shift; if (v) shift = false; return v }
}
