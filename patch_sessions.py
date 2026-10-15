import pathlib

p = pathlib.Path("app/src/main/java/com/nexus/terminal/terminal/Sessions.kt")
text = p.read_text()

def patch(old, new, label):
    global text
    if old not in text:
        print(f"SKIP (not found): {label}")
        return
    if text.count(old) != 1:
        print(f"WARNING: {label} matched {text.count(old)} times, expected 1 — skipped")
        return
    text = text.replace(old, new, 1)
    print(f"OK: {label}")

patch(
    "enum class SessionKind { SHELL, LINUX }",
    "enum class SessionKind { SHELL, LINUX, DISTRO }",
    "SessionKind add DISTRO"
)

patch(
    "class SessionInfo(\n    val id: Int, name: String, val kind: SessionKind, val shell: String, val cwd: String, val initialCommand: String?\n) {",
    "class SessionInfo(\n    val id: Int, name: String, val kind: SessionKind, val shell: String, val cwd: String, val initialCommand: String?,\n    val distroId: String? = null\n) {",
    "SessionInfo add distroId field"
)

patch(
    """    fun create(
        ctx: Context, name: String? = null, shell: String? = null, cwd: String? = null,
        initialCommand: String? = null, makeActive: Boolean = true, kind: SessionKind = SessionKind.SHELL
    ): SessionInfo {
        val app = ctx.applicationContext
        Bootstrap.ensure(app)
        val id = ++counter
        val sh = if (kind == SessionKind.LINUX) "proot" else (shell ?: ShellDetector.default(app))
        val dir = cwd?.takeIf { File(it).isDirectory } ?: startDir(app)
        val label = name ?: if (kind == SessionKind.LINUX) "Linux" else "Session $id"
        val info = SessionInfo(id, label, kind, sh, dir, initialCommand)""",
    """    fun create(
        ctx: Context, name: String? = null, shell: String? = null, cwd: String? = null,
        initialCommand: String? = null, makeActive: Boolean = true, kind: SessionKind = SessionKind.SHELL,
        distroId: String? = null
    ): SessionInfo {
        val app = ctx.applicationContext
        Bootstrap.ensure(app)
        val id = ++counter
        val sh = if (kind == SessionKind.LINUX || kind == SessionKind.DISTRO) "proot" else (shell ?: ShellDetector.default(app))
        val dir = cwd?.takeIf { File(it).isDirectory } ?: startDir(app)
        val label = name ?: if (kind == SessionKind.LINUX || kind == SessionKind.DISTRO) "Linux" else "Session $id"
        val info = SessionInfo(id, label, kind, sh, dir, initialCommand, distroId)""",
    "create() accepts + wires distroId"
)

patch(
    """    private fun newSession(app: Context, info: SessionInfo): TerminalSession {
        val client = NexusSessionClient(app, info)
        val rows = AppSettings.scrollback.coerceIn(100, 50000)
        return if (info.kind == SessionKind.LINUX) {
            val (argv, env) = LinuxRootfs.launchArgs(app)
            TerminalSession(argv[0], info.cwd, argv, env, rows, client)
        } else {
            TerminalSession(info.shell, info.cwd, arrayOf(info.shell), TermEnv.build(app, info.shell), rows, client)
        }
    }""",
    """    private fun newSession(app: Context, info: SessionInfo): TerminalSession {
        val client = NexusSessionClient(app, info)
        val rows = AppSettings.scrollback.coerceIn(100, 50000)
        return when (info.kind) {
            SessionKind.LINUX -> {
                val (argv, env) = LinuxRootfs.launchArgs(app)
                TerminalSession(argv[0], info.cwd, argv, env, rows, client)
            }
            SessionKind.DISTRO -> {
                val (argv, env) = com.nexus.terminal.distro.DistroLaunch.launchArgs(app, info.distroId!!)
                TerminalSession(argv[0], info.cwd, argv, env, rows, client)
            }
            else -> TerminalSession(info.shell, info.cwd, arrayOf(info.shell), TermEnv.build(app, info.shell), rows, client)
        }
    }""",
    "newSession() routes DISTRO through PRoot"
)

patch(
    """                                    scope.launch(Dispatchers.IO) {
                                        val cmd = DistroLaunch.command(ctx, d.id)
                                        Sessions.create(ctx, d.name, cmd)
                                    }""",
    """                                    scope.launch(Dispatchers.IO) {
                                        Sessions.create(ctx, name = d.name, kind = SessionKind.DISTRO, distroId = d.id)
                                    }""",
    "HomeScreen Open button (only runs if Sessions.kt happened to contain it — safe no-op otherwise)"
)

p.write_text(text)
print("Done patching Sessions.kt")
