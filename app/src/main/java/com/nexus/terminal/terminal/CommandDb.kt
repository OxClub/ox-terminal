package com.nexus.terminal.terminal

data class Cmd(
    val name: String, val category: String, val source: String, val desc: String,
    val usage: String, val options: List<String>, val examples: List<String>
)

/** Local, offline command reference. Also exported to $PREFIX/share/nexus/help for `nexus-help`. */
object CommandDb {
    private fun c(name: String, cat: String, src: String, desc: String, usage: String, opts: String, ex: String) =
        Cmd(name, cat, src, desc, usage, opts.split("|").filter { it.isNotBlank() }, ex.split("|").filter { it.isNotBlank() })

    val all: List<Cmd> = listOf(
        c("ls", "Files", "system", "List directory contents", "ls [OPTION]... [FILE]...", "-l long format|-a include hidden files|-h human-readable sizes|-R recurse|-t sort by time", "ls -lah ~|ls -R src"),
        c("cd", "Files", "shell", "Change the working directory", "cd [DIR]", "- previous directory|~ home", "cd ~/projects|cd .."),
        c("pwd", "Files", "system", "Print working directory", "pwd", "", "pwd"),
        c("cat", "Files", "system", "Print files to stdout", "cat [FILE]...", "-n number lines", "cat notes.txt|cat a b > c"),
        c("cp", "Files", "system", "Copy files and directories", "cp [OPTION]... SRC DEST", "-r recursive|-p preserve attributes|-n no overwrite", "cp -r src backup"),
        c("mv", "Files", "system", "Move or rename files", "mv SRC DEST", "-n no overwrite|-f force", "mv old.txt new.txt"),
        c("rm", "Files", "system", "Remove files or directories", "rm [OPTION]... FILE...", "-r recursive|-f force|-i prompt", "rm -r build"),
        c("mkdir", "Files", "system", "Create directories", "mkdir [OPTION]... DIR...", "-p create parents", "mkdir -p a/b/c"),
        c("rmdir", "Files", "system", "Remove empty directories", "rmdir DIR...", "", "rmdir empty"),
        c("touch", "Files", "system", "Create a file or update its timestamp", "touch FILE...", "", "touch a.txt"),
        c("chmod", "Files", "system", "Change file permissions", "chmod MODE FILE...", "-R recursive", "chmod +x script.sh|chmod 644 file"),
        c("ln", "Files", "system", "Create links", "ln [-s] TARGET LINK", "-s symbolic", "ln -s /sdcard sd"),
        c("find", "Files", "system", "Search for files", "find [PATH] [EXPRESSION]", "-name PATTERN|-type f/d|-size N|-mtime N", "find . -name '*.txt'|find ~ -type d"),
        c("du", "Files", "system", "Estimate file space usage", "du [OPTION]... [FILE]...", "-s summary|-h human-readable", "du -sh ~"),
        c("df", "System", "system", "Report filesystem space usage", "df [OPTION]... [FILE]...", "-h human-readable", "df -h"),
        c("grep", "Text", "system", "Search text with patterns", "grep [OPTION]... PATTERN [FILE]...", "-i ignore case|-r recursive|-n line numbers|-v invert|-E extended regex", "grep -rn TODO src|ps | grep sh"),
        c("sed", "Text", "system", "Stream editor", "sed [OPTION]... SCRIPT [FILE]...", "-i edit in place|-n quiet|-e script", "sed 's/foo/bar/g' file"),
        c("head", "Text", "system", "Output the first lines", "head [-n N] [FILE]", "-n number of lines", "head -n 20 log.txt"),
        c("tail", "Text", "system", "Output the last lines", "tail [-n N] [FILE]", "-n lines|-f follow", "tail -f app.log"),
        c("wc", "Text", "system", "Count lines, words and bytes", "wc [OPTION]... [FILE]...", "-l lines|-w words|-c bytes", "wc -l file.txt"),
        c("sort", "Text", "system", "Sort lines", "sort [OPTION]... [FILE]...", "-r reverse|-n numeric|-u unique", "sort -n numbers.txt"),
        c("uniq", "Text", "system", "Filter adjacent duplicate lines", "uniq [OPTION]... [FILE]", "-c count|-d only duplicates", "sort a | uniq -c"),
        c("cut", "Text", "system", "Select fields or columns", "cut -d DELIM -f FIELDS [FILE]", "-d delimiter|-f fields|-c characters", "cut -d: -f1 /etc/hosts"),
        c("tr", "Text", "system", "Translate or delete characters", "tr SET1 [SET2]", "-d delete", "echo hi | tr a-z A-Z"),
        c("tee", "Text", "system", "Write to stdout and files", "tee [-a] FILE...", "-a append", "ls | tee out.txt"),
        c("xargs", "Text", "system", "Build commands from stdin", "xargs [COMMAND]", "-n max args|-I replace", "find . -name '*.tmp' | xargs rm"),
        c("echo", "Shell", "shell", "Print text", "echo [-n] [STRING]...", "-n no newline", "echo \"hello\"|echo \$HOME"),
        c("printf", "Shell", "system", "Formatted output", "printf FORMAT [ARG]...", "", "printf '%s\\n' hello"),
        c("env", "Shell", "system", "Show or set environment", "env [NAME=VALUE]... [COMMAND]", "", "env | sort"),
        c("export", "Shell", "shell", "Set an environment variable", "export NAME=VALUE", "", "export EDITOR=vi"),
        c("alias", "Shell", "shell", "Define command shortcuts", "alias NAME='COMMAND'", "", "alias ll='ls -lah'"),
        c("history", "Shell", "shell", "Show command history", "history", "", "history | grep git"),
        c("which", "Shell", "system", "Locate a command", "which COMMAND", "", "which python3"),
        c("date", "System", "system", "Print or set the date", "date [+FORMAT]", "", "date '+%Y-%m-%d'"),
        c("sleep", "System", "system", "Delay for a time", "sleep SECONDS", "", "sleep 5"),
        c("uname", "System", "system", "Print system information", "uname [OPTION]", "-a all|-m machine|-r kernel release", "uname -a"),
        c("id", "System", "system", "Print user and group IDs", "id", "", "id"),
        c("whoami", "System", "system", "Print the current user", "whoami", "", "whoami"),
        c("free", "System", "system", "Show memory usage", "free [-h]", "-h human-readable", "free -h"),
        c("ps", "Processes", "system", "List processes", "ps [OPTION]", "-A all processes|-o fields", "ps -A | grep sh"),
        c("top", "Processes", "system", "Interactive process viewer", "top [-n N]", "-n iterations|-m max lines", "top -n 1"),
        c("kill", "Processes", "shell", "Send a signal to a process", "kill [-SIGNAL] PID", "-9 force kill|-15 terminate", "kill 1234|kill -9 1234"),
        c("tar", "Compression", "system", "Archive files", "tar [OPTION]... [FILE]...", "-c create|-x extract|-z gzip|-f archive file|-t list", "tar czf a.tgz dir|tar xzf a.tgz"),
        c("gzip", "Compression", "system", "Compress files", "gzip [OPTION] FILE", "-d decompress|-k keep original", "gzip -k big.log"),
        c("ping", "Networking", "system", "Test network reachability", "ping [-c COUNT] HOST", "-c count|-W timeout", "ping -c 4 1.1.1.1"),
        c("ifconfig", "Networking", "system", "Show network interfaces", "ifconfig [INTERFACE]", "", "ifconfig wlan0"),
        c("nc", "Networking", "system", "Netcat: raw TCP/UDP connections", "nc [OPTION] HOST PORT", "-l listen|-u UDP", "nc -l 8080"),
        c("curl", "Networking", "package", "Transfer data with URLs", "curl [OPTION]... URL", "-o file|-L follow redirects|-I headers only", "curl -L https://example.com"),
        c("wget", "Networking", "package", "Download files", "wget [OPTION]... URL", "-O file|-c continue", "wget https://example.com/a.zip"),
        c("ssh", "Networking", "package", "Secure shell client", "ssh [-p PORT] [-i KEY] USER@HOST", "-p port|-i identity file|-L local forward", "ssh -p 22 me@server"),
        c("scp", "Networking", "package", "Secure copy over SSH", "scp [OPTION] SRC DEST", "-P port|-r recursive", "scp file me@host:~/"),
        c("git", "Git", "package", "Distributed version control", "git COMMAND [ARGS]", "clone|status|log|branch|pull|push|commit|diff", "git clone URL|git status|git log --oneline -20"),
        c("python", "Languages", "package", "Python interpreter", "python [FILE|-c CODE]", "-m module|-c code", "python script.py"),
        c("node", "Languages", "package", "Node.js runtime", "node [FILE]", "-e code", "node app.js"),
        c("vi", "Editors", "package", "Text editor (if provided by a package)", "vi FILE", "", "vi notes.txt"),
        c("nano", "Editors", "package", "Simple text editor (package)", "nano FILE", "", "nano notes.txt"),
        c("nxpkg", "Packages", "nexus", "Nexus package helper", "nxpkg list|search|info|install|remove|update|upgrade", "", "nxpkg list|nxpkg search python"),
        c("nexus-help", "Packages", "nexus", "Local command documentation", "nexus-help [COMMAND]", "", "nexus-help grep")
    )

    val categories: List<String> get() = all.map { it.category }.distinct().sorted()

    fun search(q: String): List<Cmd> =
        if (q.isBlank()) all else all.filter {
            it.name.contains(q, true) || it.desc.contains(q, true) || it.category.contains(q, true)
        }

    fun render(c: Cmd): String = buildString {
        appendLine(c.name + " - " + c.desc)
        appendLine("Category: ${c.category}   Source: ${c.source}")
        appendLine()
        appendLine("Usage:")
        appendLine("  " + c.usage)
        if (c.options.isNotEmpty()) { appendLine(); appendLine("Common options:"); c.options.forEach { appendLine("  $it") } }
        if (c.examples.isNotEmpty()) { appendLine(); appendLine("Examples:"); c.examples.forEach { appendLine("  $it") } }
        if (c.source == "package") { appendLine(); appendLine("Not preinstalled: provided by a package (Packages screen).") }
    }
}
