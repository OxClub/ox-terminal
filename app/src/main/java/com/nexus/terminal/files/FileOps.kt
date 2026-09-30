package com.nexus.terminal.files

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SortMode { NAME, SIZE, DATE, TYPE;
    companion object { fun from(s: String) = values().firstOrNull { it.name.equals(s, true) } ?: NAME }
}

object FileOps {
    val textExtensions = setOf(
        "txt", "md", "json", "xml", "html", "htm", "css", "js", "ts", "py", "sh", "java", "kt", "kts",
        "c", "h", "cpp", "hpp", "cc", "php", "yaml", "yml", "toml", "ini", "cfg", "conf", "log", "csv",
        "gradle", "properties", "rb", "go", "rs", "lua", "pl", "sql", "bashrc", "profile"
    )

    fun list(dir: File, showHidden: Boolean, sort: SortMode, filter: String = ""): List<File> {
        val all = dir.listFiles() ?: throw IOException("Permission denied or not a directory: ${dir.path}")
        val cmp: Comparator<File> = when (sort) {
            SortMode.NAME -> compareBy { it.name.lowercase() }
            SortMode.SIZE -> compareByDescending<File> { if (it.isDirectory) -1L else it.length() }.thenBy { it.name.lowercase() }
            SortMode.DATE -> compareByDescending<File> { it.lastModified() }
            SortMode.TYPE -> compareBy<File> { it.extension.lowercase() }.thenBy { it.name.lowercase() }
        }
        return all.asSequence()
            .filter { showHidden || !it.name.startsWith(".") }
            .filter { filter.isBlank() || it.name.contains(filter, ignoreCase = true) }
            .sortedWith(compareByDescending<File> { it.isDirectory }.then(cmp))
            .toList()
    }

    fun uniqueName(dir: File, name: String): File {
        var f = File(dir, name)
        if (!f.exists()) return f
        val base = name.substringBeforeLast('.', name)
        val ext = if (name.contains('.')) "." + name.substringAfterLast('.') else ""
        var i = 1
        while (f.exists()) { f = File(dir, "$base ($i)$ext"); i++ }
        return f
    }

    fun createFile(dir: File, name: String): File {
        validateName(name)
        val f = File(dir, name)
        if (f.exists()) throw IOException("Already exists: $name")
        if (!f.createNewFile()) throw IOException("Could not create file: $name")
        return f
    }

    fun createDir(dir: File, name: String): File {
        validateName(name)
        val f = File(dir, name)
        if (f.exists()) throw IOException("Already exists: $name")
        if (!f.mkdirs()) throw IOException("Could not create folder: $name")
        return f
    }

    fun rename(f: File, newName: String): File {
        validateName(newName)
        val t = File(f.parentFile, newName)
        if (t.exists()) throw IOException("Already exists: $newName")
        if (!f.renameTo(t)) throw IOException("Rename failed: ${f.name}")
        return t
    }

    fun copy(src: File, destDir: File): File {
        if (!src.exists()) throw IOException("File not found: ${src.path}")
        if (src.isDirectory && destDir.canonicalPath.startsWith(src.canonicalPath + File.separator))
            throw IOException("Cannot copy a folder into itself")
        val target = uniqueName(destDir, src.name)
        if (src.isDirectory) src.copyRecursively(target, overwrite = false) else src.copyTo(target)
        return target
    }

    fun move(src: File, destDir: File): File {
        val target = uniqueName(destDir, src.name)
        if (src.renameTo(target)) return target
        copy(src, destDir).let { if (!delete(src)) throw IOException("Copied but could not remove source"); return it }
    }

    fun duplicate(src: File): File = copy(src, src.parentFile ?: throw IOException("Invalid path"))

    fun delete(f: File): Boolean = if (f.isDirectory) f.deleteRecursively() else f.delete()

    fun search(root: File, query: String, limit: Int = 200, showHidden: Boolean = true): List<File> {
        if (query.isBlank()) return emptyList()
        val out = ArrayList<File>()
        root.walkTopDown()
            .onEnter { showHidden || !it.name.startsWith(".") || it == root }
            .forEach { if (it != root && it.name.contains(query, true)) { out.add(it); if (out.size >= limit) return out } }
        return out
    }

    fun validateName(name: String) {
        if (name.isBlank() || name.contains('/') || name == "." || name == "..")
            throw IOException("Invalid name: \"$name\"")
    }

    fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var v = bytes.toDouble(); var i = -1
        while (v >= 1024 && i < units.size - 1) { v /= 1024; i++ }
        return String.format(Locale.US, "%.1f %s", v, units[i])
    }

    fun formatDate(ms: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ms))

    fun permString(f: File): String = try {
        (if (f.isDirectory) "d" else "-") + PosixFilePermissions.toString(Files.getPosixFilePermissions(f.toPath()))
    } catch (e: Exception) {
        (if (f.isDirectory) "d" else "-") + (if (f.canRead()) "r" else "-") + (if (f.canWrite()) "w" else "-") + (if (f.canExecute()) "x" else "-")
    }

    fun typeOf(f: File): String = when {
        f.isDirectory -> "Folder"
        Archive.isArchive(f) -> "Archive"
        f.extension.isBlank() -> "File"
        else -> f.extension.uppercase()
    }

    fun isText(f: File): Boolean {
        if (!f.isFile) return false
        if (f.extension.lowercase() in textExtensions || f.name.lowercase() in textExtensions) return true
        if (f.length() == 0L) return true
        return try {
            f.inputStream().use { s ->
                val buf = ByteArray(1024); val n = s.read(buf)
                (0 until n).none { buf[it].toInt() == 0 }
            }
        } catch (e: Exception) { false }
    }

    fun folderSize(f: File): Long = if (f.isFile) f.length() else f.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
