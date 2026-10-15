package com.nexus.terminal.distro

import android.content.Context
import com.nexus.terminal.terminal.LinuxRootfs

object DistroLaunch {
    /** Host-side argv/env for a PRoot process that boots into this distro's own extracted rootfs. */
    fun launchArgs(ctx: Context, id: String): Pair<Array<String>, Array<String>> {
        val root = DistroManager.rootfs(ctx, id)
        return LinuxRootfs.launchArgsFor(ctx, root, "/bin/sh")
    }
}
