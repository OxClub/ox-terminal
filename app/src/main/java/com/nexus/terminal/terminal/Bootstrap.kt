package com.nexus.terminal.terminal

import android.content.Context
import com.nexus.terminal.BuildConfig
import com.nexus.terminal.util.NxPaths
import java.io.File

/** Prepares the app-private terminal filesystem (home, prefix, helper scripts, local help). */
object Bootstrap {
    @Synchronized
    fun ensure(ctx: Context) {
        val app = ctx.applicationContext
        NxPaths.home(app); NxPaths.tmp(app); NxPaths.pkgDb(app); NxPaths.scripts(app)
        File(NxPaths.prefix(app), "etc").mkdirs()
        val helpDir = File(NxPaths.prefix(app), "share/nexus/help").also { it.mkdirs() }
        val stamp = File(NxPaths.pkgDb(app), "bootstrap.stamp")
        val ver = BuildConfig.VERSION_NAME
        if (stamp.exists() && stamp.readText() == ver) return
        for (name in listOf("nxpkg", "nexus-help")) {
            val out = File(NxPaths.bin(app), name)
            app.assets.open("bin/$name").use { i -> out.outputStream().use { o -> i.copyTo(o) } }
            out.setExecutable(true, false)
        }
        CommandDb.all.forEach { File(helpDir, it.name + ".txt").writeText(CommandDb.render(it)) }
        stamp.writeText(ver)
    }
}
