package com.nexus.terminal.distro

import android.content.Context
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

internal fun shimDir(ctx: Context): String {
    val lib = ctx.applicationInfo.nativeLibraryDir
    val dir = File(ctx.filesDir, "nexus/shim")
    dir.mkdirs()
    val link = File(dir, "libtalloc.so.2")
    val target = File(lib, "libtalloc.so")
    try {
        if (link.exists() || Files.isSymbolicLink(link.toPath())) link.delete()
        Files.createSymbolicLink(link.toPath(), Paths.get(target.absolutePath))
    } catch (_: Exception) {
    }
    return dir.absolutePath
}
