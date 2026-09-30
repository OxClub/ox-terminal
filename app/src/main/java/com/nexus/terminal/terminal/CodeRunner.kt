package com.nexus.terminal.terminal

import android.content.Context
import com.nexus.terminal.util.NxPaths
import java.io.File

sealed class RunPlan {
    data class Steps(val steps: List<List<String>>, val label: String) : RunPlan()
    data class Unsupported(val reason: String) : RunPlan()
}

/** Maps a source file to real commands found on PATH. Never claims a language works if its runtime is missing. */
object CodeRunner {
    fun searchDirs(ctx: Context): List<File> =
        listOf(NxPaths.bin(ctx), File("/system/bin"), File("/system/xbin"))

    fun which(ctx: Context, name: String): File? =
        searchDirs(ctx).map { File(it, name) }.firstOrNull { it.isFile && it.canExecute() }

    private fun first(ctx: Context, vararg names: String): File? = names.firstNotNullOfOrNull { which(ctx, it) }

    fun plan(ctx: Context, file: File): RunPlan {
        val path = file.absolutePath
        val out = File(NxPaths.tmp(ctx), file.nameWithoutExtension + ".out").absolutePath
        fun missing(rt: String) = RunPlan.Unsupported("Runtime '$rt' is not installed. Install it from Packages, then try again.")
        return when (file.extension.lowercase()) {
            "sh" -> RunPlan.Steps(listOf(listOf((first(ctx, "bash", "sh") ?: return missing("sh")).path, path)), "shell")
            "py" -> first(ctx, "python3", "python")?.let { RunPlan.Steps(listOf(listOf(it.path, path)), "Python") } ?: missing("python")
            "js" -> which(ctx, "node")?.let { RunPlan.Steps(listOf(listOf(it.path, path)), "Node.js") } ?: missing("node")
            "rb" -> which(ctx, "ruby")?.let { RunPlan.Steps(listOf(listOf(it.path, path)), "Ruby") } ?: missing("ruby")
            "php" -> which(ctx, "php")?.let { RunPlan.Steps(listOf(listOf(it.path, path)), "PHP") } ?: missing("php")
            "pl" -> which(ctx, "perl")?.let { RunPlan.Steps(listOf(listOf(it.path, path)), "Perl") } ?: missing("perl")
            "lua" -> which(ctx, "lua")?.let { RunPlan.Steps(listOf(listOf(it.path, path)), "Lua") } ?: missing("lua")
            "go" -> which(ctx, "go")?.let { RunPlan.Steps(listOf(listOf(it.path, "run", path)), "Go") } ?: missing("go")
            "java" -> which(ctx, "java")?.let { RunPlan.Steps(listOf(listOf(it.path, path)), "Java (source launcher)") } ?: missing("java")
            "kts" -> which(ctx, "kotlin")?.let { RunPlan.Steps(listOf(listOf(it.path, path)), "Kotlin script") } ?: missing("kotlin")
            "c" -> first(ctx, "clang", "gcc", "cc")?.let { RunPlan.Steps(listOf(listOf(it.path, path, "-o", out), listOf(out)), "C") } ?: missing("clang/gcc")
            "cpp", "cc" -> first(ctx, "clang++", "g++", "c++")?.let { RunPlan.Steps(listOf(listOf(it.path, path, "-o", out), listOf(out)), "C++") } ?: missing("clang++/g++")
            "rs" -> which(ctx, "rustc")?.let { RunPlan.Steps(listOf(listOf(it.path, path, "-o", out), listOf(out)), "Rust") } ?: missing("rustc")
            "kt" -> RunPlan.Unsupported("Plain .kt files need kotlinc + a JVM. Only Kotlin scripts (.kts) can run directly.")
            else -> RunPlan.Unsupported("No runner for '.${file.extension}' files.")
        }
    }

    val supportedExtensions = listOf("sh", "py", "js", "rb", "php", "pl", "lua", "go", "java", "kts", "c", "cpp", "rs")
}
