package com.nexus.terminal.files

import android.system.Os
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File
import java.io.IOException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

object Archive {
    fun isArchive(f: File): Boolean = kind(f.name) != null
    fun kind(name: String): String? {
        val n = name.lowercase()
        return when {
            n.endsWith(".zip") -> "zip"
            n.endsWith(".tar.gz") || n.endsWith(".tgz") -> "tgz"
            else -> null
        }
    }

    private fun safeTarget(dest: File, entryName: String): File {
        val out = File(dest, entryName)
        val root = dest.canonicalPath + File.separator
        if (!(out.canonicalPath + File.separator).startsWith(root) && out.canonicalPath != dest.canonicalPath)
            throw IOException("Blocked unsafe path in archive: $entryName")
        return out
    }

    /** Extracts into [dest]; returns extracted file paths relative to [dest]. */
    fun extract(archive: File, dest: File): List<String> {
        dest.mkdirs()
        return when (kind(archive.name)) {
            "zip" -> unzip(archive, dest)
            "tgz" -> untar(archive, dest)
            else -> throw IOException("Unsupported archive type: ${archive.name}")
        }
    }

    private fun unzip(zip: File, dest: File): List<String> {
        val files = ArrayList<String>()
        ZipInputStream(zip.inputStream().buffered()).use { zin ->
            var e = zin.nextEntry
            while (e != null) {
                val out = safeTarget(dest, e.name)
                if (e.isDirectory) out.mkdirs() else {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { zin.copyTo(it) }
                    if (e.name.startsWith("bin/") || e.name.contains("/bin/")) out.setExecutable(true, false)
                    files.add(e.name)
                }
                e = zin.nextEntry
            }
        }
        return files
    }

    private fun untar(tgz: File, dest: File): List<String> {
        val files = ArrayList<String>()
        TarArchiveInputStream(GzipCompressorInputStream(tgz.inputStream().buffered())).use { tin ->
            var e = tin.nextTarEntry
            while (e != null) {
                val out = safeTarget(dest, e.name)
                when {
                    e.isDirectory -> out.mkdirs()
                    e.isSymbolicLink -> {
                        out.parentFile?.mkdirs()
                        runCatching { out.delete(); Os.symlink(e.linkName, out.path) }
                        files.add(e.name)
                    }
                    e.isFile -> {
                        out.parentFile?.mkdirs()
                        out.outputStream().use { tin.copyTo(it) }
                        if ((e.mode and 0b001001001) != 0) out.setExecutable(true, false)
                        files.add(e.name)
                    }
                }
                e = tin.nextTarEntry
            }
        }
        return files
    }

    fun zip(sources: List<File>, out: File) {
        ZipOutputStream(out.outputStream().buffered()).use { zos ->
            fun add(f: File, base: String) {
                if (f.isDirectory) {
                    f.listFiles()?.forEach { add(it, "$base${f.name}/") }
                    if (f.listFiles().isNullOrEmpty()) zos.putNextEntry(ZipEntry("$base${f.name}/")).also { zos.closeEntry() }
                } else {
                    zos.putNextEntry(ZipEntry(base + f.name))
                    f.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
            sources.forEach { add(it, "") }
        }
    }
}
