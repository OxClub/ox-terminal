package com.nexus.terminal.distro

import android.content.Context
import android.system.Os
import com.nexus.terminal.util.NxPaths
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.json.JSONObject
import java.io.File
import java.io.IOException
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
            tarFile.delete()
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

    /** Guards against tar-slip: every extracted path must stay inside [dest]. */
    private fun safeTarget(dest: File, entryName: String): File {
        val out = File(dest, entryName)
        val root = dest.canonicalPath + File.separator
        if (!(out.canonicalPath + File.separator).startsWith(root) && out.canonicalPath != dest.canonicalPath)
            throw IOException("Blocked unsafe path in rootfs: $entryName")
        return out
    }

    private fun extractRootfs(tarFile: File, outputDir: File) {
        outputDir.mkdirs()
        TarArchiveInputStream(GzipCompressorInputStream(tarFile.inputStream().buffered(1 shl 16))).use { tar ->
            var entry = tar.nextEntry
            while (entry != null) {
                val out = safeTarget(outputDir, entry.name)
                when {
                    entry.isDirectory -> out.mkdirs()
                    entry.isSymbolicLink -> {
                        out.parentFile?.mkdirs()
                        runCatching { out.delete(); Os.symlink(entry.linkName, out.path) }
                    }
                    entry.isLink -> {
                        out.parentFile?.mkdirs()
                        val target = safeTarget(outputDir, entry.linkName)
                        val linked = runCatching { Os.link(target.path, out.path) }.isSuccess
                        if (!linked && target.isFile) target.copyTo(out, overwrite = true)
                    }
                    else -> {
                        out.parentFile?.mkdirs()
                        out.outputStream().use { tar.copyTo(it) }
                        if ((entry.mode and 0b001001001) != 0) out.setExecutable(true, false)
                    }
                }
                entry = tar.nextEntry
            }
        }
    }

    fun uninstall(ctx: Context, id: String) {
        base(ctx, id).deleteRecursively()
    }
}
