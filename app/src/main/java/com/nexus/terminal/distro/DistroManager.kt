package com.nexus.terminal.distro

import android.content.Context
import com.nexus.terminal.util.NxPaths
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.json.JSONObject
import java.io.File
import java.net.URL

data class Distro(
    val id: String,
    val name: String,
    val version: String,
    val sizeMb: Int,
    val url: String,
    val sha256url: String,
    val initCmds: List<String>
)

object DistroManager {
    fun catalog(ctx: Context): List<Distro> {
        val text = ctx.assets.open("distros/catalog.json").bufferedReader().use { it.readText() }
        val arr = JSONObject(text).getJSONArray("distros")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val cmds = o.optJSONArray("initCmds")
            Distro(
                id = o.getString("id"),
                name = o.getString("name"),
                version = o.optString("version"),
                sizeMb = o.optInt("sizeMb"),
                url = o.getString("url"),
                sha256url = o.optString("sha256url"),
                initCmds = if (cmds == null) emptyList() else (0 until cmds.length()).map { cmds.getString(it) }
            )
        }
    }

    private fun base(ctx: Context, id: String) = File(NxPaths.data(ctx), "distros/$id")
    fun rootfs(ctx: Context, id: String) = File(base(ctx, id), "rootfs")
    private fun marker(ctx: Context, id: String) = File(base(ctx, id), ".installed")

    fun isInstalled(ctx: Context, id: String) = marker(ctx, id).exists()

    fun install(ctx: Context, distro: Distro, progress: (String, Int) -> Unit) {
        val baseDir = base(ctx, distro.id)
        baseDir.mkdirs()
        val tarFile = File(baseDir, "root.tar.gz")
        try {
            progress("Downloading ${distro.name}...", 0)
            downloadFile(distro.url, tarFile)
            progress("Extracting...", 50)
            extractRootfs(tarFile, rootfs(ctx, distro.id))
            progress("Finalizing...", 90)
            marker(ctx, distro.id).createNewFile()
            progress("Done!", 100)
        } catch (e: Exception) {
            baseDir.deleteRecursively()
            throw e
        }
    }

    private fun downloadFile(urlStr: String, file: File) {
        val url = URL(urlStr)
        val conn = url.openConnection().apply {
            connectTimeout = 10000
            readTimeout = 10000
        }
        conn.inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun extractRootfs(tarFile: File, outputDir: File) {
        outputDir.mkdirs()
        val stream = tarFile.inputStream().buffered()
        val gzip = GzipCompressorInputStream(stream)
        val tar = TarArchiveInputStream(gzip)
        var entry = tar.nextEntry
        while (entry != null) {
            val file = File(outputDir, entry.name)
            if (entry.isDirectory) {
                file.mkdirs()
            } else {
                file.parentFile?.mkdirs()
                file.outputStream().use { tar.copyTo(it) }
            }
            entry = tar.nextEntry
        }
    }

    fun uninstall(ctx: Context, id: String) {
        base(ctx, id).deleteRecursively()
    }

    fun launchEnv(ctx: Context): List<String> {
        return listOf(
            "TERM=xterm-256color",
            "LANG=C.UTF-8",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
        )
    }

    fun launchArgv(ctx: Context, id: String): List<String> {
        return listOf("sh", "-c", "cd / && exec /bin/sh")
    }
}
