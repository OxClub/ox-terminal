package com.nexus.terminal.distro

import android.content.Context
import com.nexus.terminal.util.NxPaths
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.security.MessageDigest

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

    fun delete(ctx: Context, id: String) {
        base(ctx, id).deleteRecursively()
    }

    /** Download + verify + extract. Call from a background thread. onProgress(message, percent or -1). */
    fun install(ctx: Context, d: Distro, onProgress: (String, Int) -> Unit) {
        val dir = base(ctx, d.id)
        dir.deleteRecursively()
        dir.mkdirs()
        val archive = File(ctx.cacheDir, "${d.id}-download.tmp")
        try {
            onProgress("Downloading ${d.name}", 0)
            download(d.url, archive) { pct -> onProgress("Downloading ${d.name}", pct) }

            if (d.sha256url.isNotBlank()) {
                onProgress("Verifying", -1)
                val expected = fetchText(d.sha256url).trim().split(Regex("\\s+")).first().lowercase()
                val actual = sha256(archive)
                if (expected != actual) error("SHA-256 mismatch (file corrupted)")
            }

            onProgress("Extracting (this takes a while)", -1)
            val root = rootfs(ctx, d.id)
            root.mkdirs()
            extract(archive, root, d.url)

            // DNS inside the distro
            val etc = File(root, "etc")
            etc.mkdirs()
            File(etc, "resolv.conf").delete()
            File(etc, "resolv.conf").writeText("nameserver 8.8.8.8\nnameserver 1.1.1.1\n")

            marker(ctx, d.id).writeText(System.currentTimeMillis().toString())
            onProgress("Done", 100)
        } catch (e: Exception) {
            dir.deleteRecursively()
            throw e
        } finally {
            archive.delete()
        }
    }

    private fun download(url: String, out: File, onPct: (Int) -> Unit) {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 20000
        c.readTimeout = 30000
        c.instanceFollowRedirects = true
        if (c.responseCode != 200) error("Download failed: HTTP ${c.responseCode}")
        val total = c.contentLengthLong
        var done = 0L
        var last = -1
        c.inputStream.use { ins ->
            out.outputStream().use { os ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = ins.read(buf)
                    if (n < 0) break
                    os.write(buf, 0, n)
                    done += n
                    if (total > 0) {
                        val pct = (done * 100 / total).toInt()
                        if (pct != last) { last = pct; onPct(pct) }
                    }
                }
            }
        }
    }

    private fun fetchText(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 20000
        c.readTimeout = 30000
        if (c.responseCode != 200) error("HTTP ${c.responseCode} for checksum")
        return c.inputStream.bufferedReader().use { it.readText() }
    }

    private fun sha256(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { ins ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = ins.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun extract(archive: File, root: File, url: String) {
        val raw: InputStream = BufferedInputStream(archive.inputStream())
        val decompressed: InputStream =
            if (url.endsWith(".xz")) XZCompressorInputStream(raw) else GzipCompressorInputStream(raw)
        val rootCanon = root.canonicalPath
        TarArchiveInputStream(decompressed).use { tar ->
            var e: TarArchiveEntry? = tar.nextTarEntry
            while (e != null) {
                val name = e.name.removePrefix("./")
                if (name.isNotEmpty()) {
                    val target = File(root, name)
                    // block path traversal (../)
                    if (!target.absoluteFile.normalize().path.startsWith(rootCanon)) {
                        e = tar.nextTarEntry; continue
                    }
                    when {
                        e.isDirectory -> {
                            target.mkdirs()
                            target.setReadable(true); target.setWritable(true); target.setExecutable(true)
                        }
                        e.isSymbolicLink -> {
                            target.parentFile?.mkdirs()
                            target.delete()
                            Files.createSymbolicLink(target.toPath(), java.nio.file.Paths.get(e.linkName))
                        }
                        e.isLink -> {
                            target.parentFile?.mkdirs()
                            target.delete()
                            val src = File(root, e.linkName.removePrefix("./"))
                            if (src.exists()) src.copyTo(target, overwrite = true)
                        }
                        e.isFile -> {
                            target.parentFile?.mkdirs()
                            target.delete()
                            target.outputStream().use { tar.copyTo(it) }
                            target.setReadable(true)
                            target.setWritable(true)
                            if (e.mode and 0b001001001 != 0) target.setExecutable(true)
                        }
                        else -> { /* skip device nodes, fifos */ }
                    }
                }
                e = tar.nextTarEntry
            }
        }
    }

    /** argv for starting a shell inside the distro through PRoot. UNTESTED. */
    fun launchArgv(ctx: Context, id: String): Array<String> {
        val lib = ctx.applicationInfo.nativeLibraryDir
        val root = rootfs(ctx, id).absolutePath
        val shell = if (File(root, "bin/bash").exists()) "/bin/bash" else "/bin/sh"
        return arrayOf(
            "$lib/libproot.so",
            "--kill-on-exit", "--link2symlink",
            "-0", "-r", root,
            "-b", "/dev", "-b", "/proc", "-b", "/sys",
            "-b", "${NxPaths.SDCARD}:/sdcard",
            "-w", "/root",
            "/usr/bin/env", "-i",
            "HOME=/root", "TERM=xterm-256color", "LANG=C.UTF-8",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            shell, "-l"
        )
    }

    fun launchEnv(ctx: Context): Array<String> {
        val lib = ctx.applicationInfo.nativeLibraryDir
        return arrayOf(
            "PROOT_LOADER=$lib/libproot-loader.so",
            "PROOT_LOADER_32=$lib/libproot-loader32.so",
            "PROOT_TMP_DIR=${NxPaths.tmp(ctx).absolutePath}",
            "LD_LIBRARY_PATH=$lib"
        )
    }
}
