package com.nexus.terminal.util

import android.content.Context
import java.io.File

/** App-private terminal filesystem layout. */
object NxPaths {
    const val SDCARD = "/storage/emulated/0"
    fun home(ctx: Context) = File(ctx.filesDir, "home").also { it.mkdirs() }
    fun prefix(ctx: Context) = File(ctx.filesDir, "usr").also { it.mkdirs() }
    fun bin(ctx: Context) = File(prefix(ctx), "bin").also { it.mkdirs() }
    fun tmp(ctx: Context) = File(ctx.cacheDir, "tmp").also { it.mkdirs() }
    fun data(ctx: Context) = File(ctx.filesDir, "nexus").also { it.mkdirs() }
    fun pkgDb(ctx: Context) = File(prefix(ctx), "var/nx").also { it.mkdirs() }
    fun scripts(ctx: Context) = File(home(ctx), "scripts").also { it.mkdirs() }
}
