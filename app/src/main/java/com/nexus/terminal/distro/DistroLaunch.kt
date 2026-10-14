package com.nexus.terminal.distro

import android.content.Context

object DistroLaunch {
    private fun q(s: String) = "'" + s.replace("'", "'\\''") + "'"

    fun command(ctx: Context, id: String): String {
        val exports = DistroManager.launchEnv(ctx).joinToString("; ") { e ->
            "export " + e.substringBefore("=") + "=" + q(e.substringAfter("="))
        }
        val argv = DistroManager.launchArgv(ctx, id).joinToString(" ") { q(it) }
        return "$exports; exec $argv"
    }
}
