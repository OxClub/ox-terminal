package com.nexus.terminal.util

import android.util.Log

/** Developer logging. Disabled by default in release builds; secrets are redacted. */
object NxLog {
    @Volatile var enabled: Boolean = false
    private val redact = Regex("(?i)(password|passwd|token|secret|key)=\\S+")

    private fun clean(msg: String) = redact.replace(msg, "\$1=***")

    fun d(tag: String, msg: String) { if (enabled) Log.d("Nexus/$tag", clean(msg)) }
    fun w(tag: String, msg: String) { if (enabled) Log.w("Nexus/$tag", clean(msg)) }
    fun e(tag: String, msg: String, t: Throwable? = null) {
        if (enabled) Log.e("Nexus/$tag", clean(msg), t)
    }
}
