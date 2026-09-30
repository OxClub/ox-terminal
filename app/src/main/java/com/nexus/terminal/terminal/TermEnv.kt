package com.nexus.terminal.terminal

import android.content.Context
import com.nexus.terminal.data.AppSettings
import com.nexus.terminal.data.EnvStore
import com.nexus.terminal.util.NxPaths
import java.io.File

object TermEnv {
    private val passthrough = listOf(
        "ANDROID_ROOT", "ANDROID_DATA", "ANDROID_ART_ROOT", "ANDROID_I18N_ROOT", "ANDROID_TZDATA_ROOT",
        "ANDROID_RUNTIME_ROOT", "BOOTCLASSPATH", "DEX2OATBOOTCLASSPATH", "EXTERNAL_STORAGE",
        "ANDROID_STORAGE", "ASEC_MOUNTPOINT", "LOOP_MOUNTPOINT"
    )

    /** Built-in variables (read-only in the env manager). */
    fun builtin(ctx: Context, shell: String): LinkedHashMap<String, String> {
        val prefix = NxPaths.prefix(ctx).absolutePath
        val m = LinkedHashMap<String, String>()
        m["HOME"] = NxPaths.home(ctx).absolutePath
        m["PREFIX"] = prefix
        m["PATH"] = "${NxPaths.bin(ctx).absolutePath}:/system/bin:/system/xbin"
        m["LD_LIBRARY_PATH"] = "$prefix/lib"
        m["TMPDIR"] = NxPaths.tmp(ctx).absolutePath
        m["TERM"] = "xterm-256color"
        m["COLORTERM"] = "truecolor"
        m["LANG"] = "en_US.UTF-8"
        m["SHELL"] = shell
        m["NEXUS_TERMINAL"] = "1"
        passthrough.forEach { k -> System.getenv(k)?.let { m[k] = it } }
        if (AppSettings.loadAliases) {
            // mksh (Android's /system/bin/sh) sources $ENV for interactive shells. Opt-in only.
            m["ENV"] = File(NxPaths.home(ctx), ".nexus_aliases").absolutePath
        }
        return m
    }

    fun map(ctx: Context, shell: String): LinkedHashMap<String, String> {
        val m = builtin(ctx, shell)
        EnvStore.all().forEach { m[it.name] = it.value }
        return m
    }

    fun build(ctx: Context, shell: String): Array<String> =
        map(ctx, shell).map { "${it.key}=${it.value}" }.toTypedArray()
}
