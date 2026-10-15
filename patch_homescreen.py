import pathlib
p = pathlib.Path("app/src/main/java/com/nexus/terminal/ui/screens/HomeScreen.kt")
text = p.read_text()
old = """                                scope.launch {
                                        busyId = d.id"""
old2_check = "val cmd = DistroLaunch.command(ctx, d.id)"
if old2_check in text:
    text = text.replace(
        """                                    scope.launch(Dispatchers.IO) {
                                        val cmd = DistroLaunch.command(ctx, d.id)
                                        Sessions.create(ctx, d.name, cmd)
                                    }""",
        """                                    scope.launch(Dispatchers.IO) {
                                        Sessions.create(ctx, name = d.name, kind = com.nexus.terminal.terminal.SessionKind.DISTRO, distroId = d.id)
                                    }"""
    )
    p.write_text(text)
    print("OK: HomeScreen Open button fixed")
else:
    print("Pattern not found in HomeScreen.kt — paste the file content for manual fix")
