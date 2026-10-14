package com.nexus.terminal.distro

import android.content.Context

object DistroLaunch {
    fun command(ctx: Context, id: String): String {
        val rootfs = DistroManager.rootfs(ctx, id).absolutePath
        val env = DistroManager.launchEnv(ctx).joinToString(" ") { e ->
            val parts = e.split("=", limit = 2)
            "${parts[0]}='${parts.getOrNull(1) ?: ""}'"
        }
        return "cd '$rootfs' && env $env /bin/sh"
    }
}
