package com.nexus.terminal.terminal

import android.content.Context
import com.nexus.terminal.data.AppSettings
import com.nexus.terminal.util.NxPaths
import java.io.File

object ShellDetector {
    fun detect(ctx: Context): List<String> {
        val bin = NxPaths.bin(ctx)
        val candidates = listOf(
            File(bin, "bash").path, File(bin, "zsh").path, File(bin, "fish").path, File(bin, "sh").path,
            "/system/bin/sh", "/system/bin/bash", "/system/xbin/bash", "/system/bin/zsh", "/system/xbin/zsh"
        )
        return candidates.filter { File(it).let { f -> f.isFile && f.canExecute() } }.distinct()
    }

    fun default(ctx: Context): String {
        val cfg = AppSettings.defaultShell.trim()
        if (cfg.isNotEmpty() && File(cfg).let { it.isFile && it.canExecute() }) return cfg
        val found = detect(ctx)
        return found.firstOrNull { it.endsWith("/bash") } ?: found.firstOrNull { it == "/system/bin/sh" } ?: "/system/bin/sh"
    }
}
