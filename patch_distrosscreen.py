import pathlib
p = pathlib.Path("app/src/main/java/com/nexus/terminal/ui/screens/DistrosScreen.kt")
text = p.read_text()
old = """                                    scope.launch {
                                        val cmd = DistroLaunch.command(ctx, d.id)
                                        Sessions.create(ctx, d.name, cmd)
                                    }"""
new = """                                    scope.launch {
                                        Sessions.create(ctx, name = d.name, kind = com.nexus.terminal.terminal.SessionKind.DISTRO, distroId = d.id)
                                    }"""
if old in text:
    count = text.count(old)
    if count == 1:
        text = text.replace(old, new, 1)
        p.write_text(text)
        print("OK: DistrosScreen Open button fixed")
    else:
        print(f"WARNING: matched {count} times, expected 1 — skipped, paste full file")
else:
    print("Pattern not found — paste full file content for manual fix")
