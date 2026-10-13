package com.nexus.terminal.distro

import android.content.Context

object DistroLaunch {
    private fun q(s: String) = "'" + s.replace("'", "'\\''") + "'"

    /** One shell command line that replaces the current shell with PRoot running the distro. */
    fun command(ctx: Context, id: String): String {
        val env = DistroManager.launchEnv(ctx).joinToString(" ") { e ->
            val k = e.substringBefore("=")
            val v = e.substringAfter("=")
            "$k=${q(v)}"
        }
        val argv = DistroManager.launchArgv(ctx, id).joinToString(" ") { q(it) }
        return "exec env $env $argv"
    }
}
