package com.nexus.terminal.terminal

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.*
import com.nexus.terminal.data.AppSettings
import com.nexus.terminal.util.NxLog
import com.nexus.terminal.util.NxPaths
import com.nexus.terminal.util.Tsv
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import java.io.File

class SessionInfo(val id: Int, name: String, val shell: String, val cwd: String, val initialCommand: String?) {
    var name by mutableStateOf(name)
    private val sessionState = mutableStateOf<TerminalSession?>(null)
    var session: TerminalSession
        get() = sessionState.value!!
        set(v) { sessionState.value = v }
    var pid by mutableIntStateOf(-1)
    var exited by mutableStateOf(false)
    var exitCode by mutableIntStateOf(0)
    var generation by mutableIntStateOf(0)
    var initialSent = false
    val pending = ArrayList<String>()

    /** Sends text to the shell; queued until the emulator exists (session starts when first shown). */
    fun send(text: String) {
        if (session.emulator != null && !exited) session.write(text) else pending.add(text)
    }
}

/** Process-wide registry of terminal sessions. Sessions outlive the Activity (see TerminalService). */
object Sessions {
    val list = mutableStateListOf<SessionInfo>()
    var activeId by mutableIntStateOf(-1)
    var attachedView: TerminalView? = null
    var attachedInfo: SessionInfo? = null
    private var counter = 0

    fun active(): SessionInfo? = list.firstOrNull { it.id == activeId } ?: list.firstOrNull()

    fun create(
        ctx: Context, name: String? = null, shell: String? = null, cwd: String? = null,
        initialCommand: String? = null, makeActive: Boolean = true
    ): SessionInfo {
        val app = ctx.applicationContext
        Bootstrap.ensure(app)
        val id = ++counter
        val sh = shell ?: ShellDetector.default(app)
        val dir = cwd?.takeIf { File(it).isDirectory } ?: startDir(app)
        val label = name ?: "Session $id"
        val info = SessionInfo(id, label, sh, dir, initialCommand)
        info.session = newSession(app, info)
        list.add(info)
        if (makeActive) activeId = id
        startService(app)
        persist(app)
        NxLog.d("Sessions", "created $label shell=$sh")
        return info
    }

    private fun startDir(ctx: Context): String {
        val d = AppSettings.defaultDir
        return if (d.isNotBlank() && File(d).isDirectory) d else NxPaths.home(ctx).absolutePath
    }

    private fun newSession(app: Context, info: SessionInfo): TerminalSession {
        val client = NexusSessionClient(app, info)
        return TerminalSession(info.shell, info.cwd, arrayOf(info.shell), TermEnv.build(app, info.shell),
            AppSettings.scrollback.coerceIn(100, 50000), client)
    }

    /** Called by the view client when the emulator for the attached session exists. */
    fun onEmulatorReady(info: SessionInfo) {
        if (!info.initialSent) {
            info.initialSent = true
            val cmds = ArrayList<String>()
            info.initialCommand?.let { cmds.add(it) }
            if (AppSettings.startupEnabled && info.initialCommand == null) {
                AppSettings.startupCommands.lines().filter { it.isNotBlank() }.forEach { cmds.add(it) }
            }
            cmds.forEach { info.session.write(it + "\n") }
        }
        if (info.pending.isNotEmpty()) {
            info.pending.toList().forEach { info.session.write(it) }
            info.pending.clear()
        }
    }

    fun close(ctx: Context, id: Int) {
        val i = list.indexOfFirst { it.id == id }
        if (i < 0) return
        list[i].session.finishIfRunning()
        list.removeAt(i)
        if (activeId == id) activeId = list.getOrNull((i - 1).coerceAtLeast(0))?.id ?: -1
        persist(ctx.applicationContext)
    }

    fun closeAll(ctx: Context) {
        list.toList().forEach { it.session.finishIfRunning() }
        list.clear(); activeId = -1
        persist(ctx.applicationContext)
    }

    fun restart(ctx: Context, id: Int) {
        val info = list.firstOrNull { it.id == id } ?: return
        info.session.finishIfRunning()
        val app = ctx.applicationContext
        info.exited = false
        info.pid = -1
        info.initialSent = false
        info.session = newSession(app, info)
        info.generation++
    }

    fun rename(ctx: Context, id: Int, name: String) {
        list.firstOrNull { it.id == id }?.name = name.ifBlank { "Session $id" }
        persist(ctx.applicationContext)
    }

    fun sendToActive(text: String): Boolean {
        val a = active() ?: return false
        a.send(text)
        return true
    }

    fun transcript(info: SessionInfo): String =
        info.session.emulator?.screen?.transcriptText.orEmpty()

    /** Current working directory of a session's shell, if readable. */
    fun cwdOf(info: SessionInfo): String =
        if (info.pid > 0) runCatching { File("/proc/${info.pid}/cwd").canonicalPath }.getOrDefault(info.cwd) else info.cwd

    fun persist(ctx: Context) {
        AppSettings.savedSessions = Tsv.encodeAll(list.map { listOf(it.name, it.shell, cwdOf(it)) })
    }

    /** Recreates the sessions from last run (fresh shells in the previous directories). */
    fun restoreSaved(ctx: Context): Boolean {
        val rows = Tsv.decodeAll(AppSettings.savedSessions).filter { it.size >= 3 }
        if (rows.isEmpty()) return false
        rows.forEach { create(ctx, name = it[0], shell = it[1].takeIf { s -> File(s).canExecute() }, cwd = it[2]) }
        activeId = list.firstOrNull()?.id ?: -1
        return true
    }

    private fun startService(app: Context) {
        val i = Intent(app, TerminalService::class.java)
        runCatching { app.startForegroundService(i) }.onFailure { NxLog.e("Sessions", "service start failed", it) }
    }
}
