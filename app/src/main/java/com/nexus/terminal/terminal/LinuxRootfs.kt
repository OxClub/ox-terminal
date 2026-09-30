package com.nexus.terminal.terminal

import android.content.Context
import android.system.ErrnoException
import android.system.Os
import androidx.compose.runtime.mutableStateOf
import com.nexus.terminal.util.NxLog
import com.nexus.terminal.util.NxPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File
import java.io.IOException

/**
 * Boots into the Debian rootfs bundled by `scripts/fetch-linux.sh` (assets/linux/rootfs.tar.gz +
 * jniLibs/arm64-v8a/libproot*.so), run under PRoot. Nothing here downloads anything at runtime —
 * it only unpacks what's already inside the APK, once, on first use.
 */
object LinuxRootfs {
    val busy = mutableStateOf(false)
    val progress = mutableStateOf("")
    val lastError = mutableStateOf<String?>(null)

    private const val ASSET_PATH = "linux/rootfs.tar.gz"
    private const val STAMP_NAME = "rootfs.stamp"
    // Bump this if fetch-linux.sh ever produces an incompatible rootfs layout, to force re-extraction.
    private const val ROOTFS_VERSION = "1"

    private fun rootDir(ctx: Context) = File(ctx.filesDir, "linux").also { it.mkdirs() }
    fun rootfsDir(ctx: Context): File = File(rootDir(ctx), "root")
    private fun stampFile(ctx: Context) = File(rootDir(ctx), STAMP_NAME)

    private fun nativeLibDir(ctx: Context) = File(ctx.applicationInfo.nativeLibraryDir)
    private fun prootBin(ctx: Context) = File(nativeLibDir(ctx), "libproot.so")
    private fun prootLoader(ctx: Context) = File(nativeLibDir(ctx), "libproot-loader.so")

    /** True only when both the rootfs asset and the PRoot native libraries were bundled at build time. */
    fun isBundled(ctx: Context): Boolean = try {
        ctx.assets.open(ASSET_PATH).close()
        prootBin(ctx).canExecute() && prootLoader(ctx).isFile
    } catch (e: IOException) {
        false
    }

    fun isExtracted(ctx: Context): Boolean =
        stampFile(ctx).let { it.exists() && it.readText() == ROOTFS_VERSION } && File(rootfsDir(ctx), "bin").isDirectory

    /**
     * Extracts the bundled rootfs the first time it's needed. Safe to call every time a Linux
     * session is requested; it's a no-op once the stamp file matches [ROOTFS_VERSION].
     */
    suspend fun ensureExtracted(ctx: Context): Boolean = withContext(Dispatchers.IO) {
        if (isExtracted(ctx)) return@withContext true
        val app = ctx.applicationContext
        if (!isBundled(app)) {
            lastError.value = "No Linux rootfs was bundled in this build (run scripts/fetch-linux.sh before building)."
            return@withContext false
        }
        val dest = rootfsDir(app)
        busy.value = true
        lastError.value = null
        progress.value = "Extracting Linux environment…"
        try {
            dest.deleteRecursively()
            dest.mkdirs()
            var count = 0
            TarArchiveInputStream(GzipCompressorInputStream(app.assets.open(ASSET_PATH).buffered(1 shl 16))).use { tin ->
                var e = tin.nextTarEntry
                while (e != null) {
                    extractEntry(dest, e, tin)
                    count++
                    if (count % 200 == 0) progress.value = "Extracting Linux environment… ($count files)"
                    e = tin.nextTarEntry
                }
            }
            stampFile(app).writeText(ROOTFS_VERSION)
            NxLog.d("linux", "rootfs extracted: $count entries")
            true
        } catch (e: Exception) {
            NxLog.e("linux", "rootfs extraction failed", e)
            lastError.value = "Extraction failed: ${e.message ?: e.javaClass.simpleName}"
            dest.deleteRecursively()
            false
        } finally {
            busy.value = false
            progress.value = ""
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

    private fun extractEntry(dest: File, e: TarArchiveEntry, tin: TarArchiveInputStream) {
        // A Debian rootfs from `docker export` also contains device nodes that a plain tar reader
        // can't (and shouldn't) recreate without root; PRoot binds /dev, /proc and /sys itself at
        // launch, so anything that isn't a directory, file or link is safely skipped here.
        val out = safeTarget(dest, e.name)
        when {
            e.isDirectory -> out.mkdirs()
            e.isSymbolicLink -> {
                out.parentFile?.mkdirs()
                runCatching { out.delete(); Os.symlink(e.linkName, out.path) }
            }
            e.isLink -> {
                out.parentFile?.mkdirs()
                runCatching { linkOrCopy(dest, e, out) }
            }
            e.isFile -> {
                out.parentFile?.mkdirs()
                out.outputStream().use { tin.copyTo(it) }
                if ((e.mode and 0b001001001) != 0) out.setExecutable(true, false)
            }
            else -> NxLog.d("linux", "skipped non-regular entry: ${e.name}")
        }
    }

    private fun linkOrCopy(dest: File, e: TarArchiveEntry, out: File) {
        val target = safeTarget(dest, e.linkName)
        try {
            Os.link(target.path, out.path)
        } catch (ex: ErrnoException) {
            if (target.isFile) target.copyTo(out, overwrite = true)
        }
    }

    /** Host-side argv/env for the PRoot process that boots into the extracted rootfs. */
    fun launchArgs(ctx: Context): Pair<Array<String>, Array<String>> {
        val app = ctx.applicationContext
        val proot = prootBin(app).path
        val root = rootfsDir(app).path
        val argv = arrayOf(
            proot, "-r", root, "-0", "-w", "/root",
            "-b", "/dev", "-b", "/proc", "-b", "/sys",
            "/usr/bin/env", "-i",
            "HOME=/root", "TERM=xterm-256color", "LANG=C.UTF-8",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "/bin/bash", "--login"
        )
        val env = arrayOf(
            "LD_LIBRARY_PATH=" + nativeLibDir(app).path,
            "PROOT_LOADER=" + prootLoader(app).path,
            "PROOT_TMP_DIR=" + NxPaths.tmp(app).path
        )
        return argv to env
    }
}
