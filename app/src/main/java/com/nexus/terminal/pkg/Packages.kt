package com.nexus.terminal.pkg

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.nexus.terminal.data.AppSettings
import com.nexus.terminal.files.Archive
import com.nexus.terminal.util.NxLog
import com.nexus.terminal.util.NxPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URL
import java.security.MessageDigest

data class Pkg(
    val name: String, val version: String, val description: String, val category: String,
    val url: String, val sha256: String, val depends: List<String>, val repo: String
)

data class Installed(val name: String, val version: String, val files: List<String>)
data class OpResult(val ok: Boolean, val message: String)

object Versions {
    /** Numeric-aware comparison: 1.10 > 1.9. */
    fun compare(a: String, b: String): Int {
        val pa = a.split(Regex("[^0-9]+")).filter { it.isNotEmpty() }.map { it.toLongOrNull() ?: 0L }
        val pb = b.split(Regex("[^0-9]+")).filter { it.isNotEmpty() }.map { it.toLongOrNull() ?: 0L }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrElse(i) { 0L }; val y = pb.getOrElse(i) { 0L }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }
}

object RepoIndex {
    val categories = listOf("Development", "Networking", "Utilities", "Programming", "Editors", "System",
        "Compression", "Git", "Languages", "Database", "Other")

    /** Index format is documented in docs/REPOSITORY_FORMAT.md. */
    fun parse(json: String, repo: String): List<Pkg> {
        val root = JSONObject(json)
        val arr = root.optJSONArray("packages") ?: JSONArray()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val dep = o.optJSONArray("depends")
            val cat = o.optString("category", "Other").let { c -> categories.firstOrNull { it.equals(c, true) } ?: "Other" }
            Pkg(
                o.getString("name"), o.getString("version"), o.optString("description"), cat,
                o.getString("url"), o.optString("sha256").lowercase(),
                if (dep == null) emptyList() else (0 until dep.length()).map { dep.getString(it) }, repo
            )
        }
    }
}

/** Real package manager: downloads archives from configured repositories, verifies, extracts to $PREFIX. */
object NxPkg {
    val available = mutableStateListOf<Pkg>()
    val installed = mutableStateMapOf<String, Installed>()
    val busy = mutableStateMapOf<String, String>()
    val lastError = mutableStateOf<String?>(null)
    val refreshing = mutableStateOf(false)

    fun repos(): List<String> = AppSettings.repos.lines().map { it.trim() }.filter { it.isNotEmpty() }
    fun setRepos(list: List<String>) { AppSettings.repos = list.joinToString("\n") }

    fun isOnline(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo?.isConnected == true
    }

    fun updates(): List<Pkg> = available.filter { p ->
        installed[p.name]?.let { Versions.compare(p.version, it.version) > 0 } == true
    }

    fun init(ctx: Context) {
        loadInstalled(ctx)
        loadCache(ctx)
    }

    private fun installedFile(ctx: Context) = File(NxPaths.pkgDb(ctx), "installed.json")
    private fun cacheFile(ctx: Context) = File(NxPaths.pkgDb(ctx), "index-cache.json")

    private fun loadInstalled(ctx: Context) {
        installed.clear()
        val f = installedFile(ctx)
        if (!f.exists()) return
        runCatching {
            val a = JSONArray(f.readText())
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val files = o.getJSONArray("files")
                installed[o.getString("name")] = Installed(o.getString("name"), o.getString("version"),
                    (0 until files.length()).map { files.getString(it) })
            }
        }.onFailure { NxLog.e("pkg", "installed.json unreadable", it) }
    }

    private fun saveInstalled(ctx: Context) {
        val a = JSONArray()
        installed.values.forEach {
            a.put(JSONObject().put("name", it.name).put("version", it.version).put("files", JSONArray(it.files)))
        }
        installedFile(ctx).writeText(a.toString())
        // Plain-text views for the `nxpkg` shell helper.
        File(NxPaths.pkgDb(ctx), "installed.txt").writeText(
            installed.values.sortedBy { it.name }.joinToString("\n") { "${it.name} ${it.version}" })
    }

    private fun loadCache(ctx: Context) {
        val f = cacheFile(ctx)
        if (!f.exists()) return
        runCatching {
            val a = JSONArray(f.readText())
            val list = (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                val d = o.getJSONArray("depends")
                Pkg(o.getString("name"), o.getString("version"), o.getString("description"), o.getString("category"),
                    o.getString("url"), o.getString("sha256"), (0 until d.length()).map { d.getString(it) }, o.getString("repo"))
            }
            available.clear(); available.addAll(list)
        }
    }

    private fun saveCache(ctx: Context) {
        val a = JSONArray()
        available.forEach {
            a.put(JSONObject().put("name", it.name).put("version", it.version).put("description", it.description)
                .put("category", it.category).put("url", it.url).put("sha256", it.sha256)
                .put("depends", JSONArray(it.depends)).put("repo", it.repo))
        }
        cacheFile(ctx).writeText(a.toString())
        File(NxPaths.pkgDb(ctx), "available.txt").writeText(
            available.sortedBy { it.name }.joinToString("\n") { "${it.name}\t${it.version}\t${it.category}\t${it.description}" })
    }

    private fun openUrl(url: String): java.io.InputStream {
        val u = URL(url)
        if (u.protocol != "https" && u.protocol != "file") throw IOException("Only https:// or file:// repositories are allowed")
        val c = u.openConnection()
        c.connectTimeout = 15000; c.readTimeout = 30000
        return c.getInputStream()
    }

    suspend fun refresh(ctx: Context): OpResult = withContext(Dispatchers.IO) {
        val urls = repos()
        if (urls.isEmpty()) return@withContext OpResult(false, "No repository configured. Add one in Packages > Repositories.")
        if (urls.any { it.startsWith("https") } && !isOnline(ctx)) return@withContext OpResult(false, "Network unavailable")
        refreshing.value = true
        try {
            val all = ArrayList<Pkg>()
            val errors = ArrayList<String>()
            for (u in urls) {
                try {
                    val text = openUrl(u).bufferedReader().use { it.readText() }
                    all.addAll(RepoIndex.parse(text, u))
                } catch (e: Exception) {
                    errors.add("$u: ${e.message}")
                }
            }
            if (all.isNotEmpty() || errors.isEmpty()) {
                // Highest version wins when several repos provide the same package.
                val merged = all.groupBy { it.name }.map { (_, v) -> v.maxWithOrNull { a, b -> Versions.compare(a.version, b.version) }!! }
                withContext(Dispatchers.Main) { available.clear(); available.addAll(merged.sortedBy { it.name }) }
                saveCache(ctx)
            }
            lastError.value = errors.joinToString("\n").ifEmpty { null }
            if (errors.isNotEmpty() && all.isEmpty()) OpResult(false, "Repository refresh failed: ${errors.first()}")
            else OpResult(true, "Index updated: ${available.size} packages" + if (errors.isNotEmpty()) " (some repositories failed)" else "")
        } finally { refreshing.value = false }
    }

    private fun sha256(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { s -> val b = ByteArray(8192); while (true) { val n = s.read(b); if (n < 0) break; md.update(b, 0, n) } }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun resolveOrder(name: String, seen: MutableSet<String> = mutableSetOf()): List<Pkg> {
        if (!seen.add(name)) return emptyList()
        val p = available.firstOrNull { it.name == name } ?: throw IOException("Package not found in index: $name")
        val out = ArrayList<Pkg>()
        p.depends.filter { installed[it] == null }.forEach { out.addAll(resolveOrder(it, seen)) }
        out.add(p)
        return out
    }

    suspend fun install(ctx: Context, name: String): OpResult = withContext(Dispatchers.IO) {
        try {
            val plan = resolveOrder(name)
            for (p in plan) {
                busy[name] = "Downloading ${p.name} ${p.version}…"
                if (p.url.startsWith("https") && !isOnline(ctx)) return@withContext OpResult(false, "Network unavailable")
                val tmp = File(NxPaths.tmp(ctx), "${p.name}-${p.version}.pkg." + (Archive.kind(p.url.substringBefore('?')) ?: "zip").let { if (it == "tgz") "tar.gz" else it })
                openUrl(p.url).use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
                if (p.sha256.isNotEmpty()) {
                    busy[name] = "Verifying ${p.name}…"
                    val actual = sha256(tmp)
                    if (actual != p.sha256) { tmp.delete(); return@withContext OpResult(false, "Package installation failed: checksum mismatch for ${p.name}") }
                }
                busy[name] = "Installing ${p.name}…"
                installed[p.name]?.let { removeFiles(ctx, it) }
                val files = Archive.extract(tmp, NxPaths.prefix(ctx))
                tmp.delete()
                withContext(Dispatchers.Main) { installed[p.name] = Installed(p.name, p.version, files) }
                saveInstalled(ctx)
            }
            OpResult(true, "Installed $name" + if (plan.size > 1) " (+${plan.size - 1} dependencies)" else "")
        } catch (e: Exception) {
            NxLog.e("pkg", "install failed", e)
            OpResult(false, "Package installation failed: ${e.message ?: e.javaClass.simpleName}")
        } finally { busy.remove(name) }
    }

    private fun removeFiles(ctx: Context, inst: Installed) {
        val root = NxPaths.prefix(ctx)
        inst.files.forEach { rel ->
            val f = File(root, rel)
            if (f.canonicalPath.startsWith(root.canonicalPath + File.separator)) f.delete()
        }
    }

    suspend fun remove(ctx: Context, name: String): OpResult = withContext(Dispatchers.IO) {
        val inst = installed[name] ?: return@withContext OpResult(false, "Not installed: $name")
        val dependents = installed.keys.filter { other -> available.firstOrNull { it.name == other }?.depends?.contains(name) == true }
        if (dependents.isNotEmpty()) return@withContext OpResult(false, "Cannot remove $name: required by ${dependents.joinToString()}")
        try {
            busy[name] = "Removing…"
            removeFiles(ctx, inst)
            withContext(Dispatchers.Main) { installed.remove(name) }
            saveInstalled(ctx)
            OpResult(true, "Removed $name")
        } catch (e: Exception) { OpResult(false, "Removal failed: ${e.message}") } finally { busy.remove(name) }
    }

    suspend fun upgradeAll(ctx: Context): OpResult {
        val list = updates()
        if (list.isEmpty()) return OpResult(true, "Everything is up to date")
        val failures = ArrayList<String>()
        list.forEach { val r = install(ctx, it.name); if (!r.ok) failures.add("${it.name}: ${r.message}") }
        return if (failures.isEmpty()) OpResult(true, "Upgraded ${list.size} package(s)") else OpResult(false, failures.joinToString("\n"))
    }

    /** Installs a local .zip / .tar.gz chosen by the user (works fully offline). */
    suspend fun installLocal(ctx: Context, uri: Uri, displayName: String): OpResult = withContext(Dispatchers.IO) {
        val kind = Archive.kind(displayName) ?: return@withContext OpResult(false, "Unsupported file type (use .zip or .tar.gz): $displayName")
        val name = displayName.substringBefore('.')
        try {
            busy[name] = "Installing $displayName…"
            val tmp = File(NxPaths.tmp(ctx), "local-$displayName")
            ctx.contentResolver.openInputStream(uri)?.use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
                ?: throw IOException("Cannot open file")
            installed[name]?.let { removeFiles(ctx, it) }
            val files = Archive.extract(tmp, NxPaths.prefix(ctx))
            tmp.delete()
            withContext(Dispatchers.Main) { installed[name] = Installed(name, "local", files) }
            saveInstalled(ctx)
            OpResult(true, "Installed local archive '$name' ($kind, ${files.size} files)")
        } catch (e: Exception) { OpResult(false, "Package installation failed: ${e.message}") } finally { busy.remove(name) }
    }
}
