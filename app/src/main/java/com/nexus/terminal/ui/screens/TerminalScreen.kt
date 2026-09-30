@file:OptIn(ExperimentalMaterial3Api::class)

package com.nexus.terminal.ui.screens

import android.view.MotionEvent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.nexus.terminal.data.AppSettings
import com.nexus.terminal.data.ShortcutStore
import com.nexus.terminal.terminal.*
import com.nexus.terminal.ui.*
import com.nexus.terminal.util.NxPaths
import com.nexus.terminal.util.ShareUtil
import com.termux.view.TerminalView
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TerminalScreen(fullscreen: Boolean, onToggleFullscreen: () -> Unit, onNavigate: (String) -> Unit) {
    val ctx = LocalContext.current
    val active = Sessions.active()
    var renameFor by remember { mutableStateOf<SessionInfo?>(null) }
    var showPress by remember { mutableStateOf(false) }
    var pressEvent by remember { mutableStateOf<MotionEvent?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var showShortcuts by remember { mutableStateOf(false) }
    var overflow by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (!fullscreen) {
            SessionTabs(
                onRename = { renameFor = it },
                onFullscreen = onToggleFullscreen,
                onOverflow = { overflow = true },
                overflowMenu = {
                    DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                        DropdownMenuItem(text = { Text("Search output") }, leadingIcon = { Icon(Icons.Filled.Search, null) },
                            onClick = { overflow = false; showSearch = true })
                        DropdownMenuItem(text = { Text("Shortcuts & Git") }, leadingIcon = { Icon(Icons.Filled.Bolt, null) },
                            onClick = { overflow = false; showShortcuts = true })
                        DropdownMenuItem(text = { Text("Processes") }, leadingIcon = { Icon(Icons.Filled.Memory, null) },
                            onClick = { overflow = false; onNavigate("processes") })
                        DropdownMenuItem(text = { Text("Command history") }, leadingIcon = { Icon(Icons.Filled.History, null) },
                            onClick = { overflow = false; onNavigate("history") })
                        DropdownMenuItem(text = { Text("Copy whole terminal") }, leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                            onClick = { overflow = false; active?.let { ShareUtil.copy(ctx, Sessions.transcript(it)); toast(ctx, "Terminal copied") } })
                    }
                }
            )
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onToggleFullscreen) { Icon(Icons.Filled.FullscreenExit, "Exit full screen") }
            }
        }

        if (active == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("No sessions running", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { Sessions.create(ctx) }) { Text("New session") }
                }
            }
        } else {
            key(active.id, active.generation) {
                TerminalHost(active, Modifier.weight(1f).fillMaxWidth()) { e ->
                    pressEvent = MotionEvent.obtain(e); showPress = true
                }
            }
            if (active.exited) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Shell exited (code ${active.exitCode})", Modifier.weight(1f))
                        TextButton(onClick = { Sessions.restart(ctx, active.id) }) { Text("Restart session") }
                    }
                }
            }
            ExtraKeysBar()
        }
    }

    renameFor?.let { s ->
        TextInputDialog("Rename session", s.name, "Name", "Rename", onConfirm = { Sessions.rename(ctx, s.id, it); renameFor = null }, onDismiss = { renameFor = null })
    }
    if (showPress && active != null) {
        ModalBottomSheet(onDismissRequest = { showPress = false }) {
            Column(Modifier.padding(bottom = 24.dp)) {
                @Composable fun item(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: () -> Unit) =
                    ListItem(headlineContent = { Text(text) }, leadingContent = { Icon(icon, null) },
                        modifier = Modifier.clickable { showPress = false; action() })
                val copy = { txt: String -> ShareUtil.copy(ctx, txt); toast(ctx, "Copied") }
                LongPressItems(
                    onSelect = { pressEvent?.let { Sessions.attachedView?.startTextSelectionMode(it) } },
                    onCopyAll = { copy(Sessions.transcript(active)) },
                    onPaste = { active.session.emulator?.paste(ShareUtil.paste(ctx)) },
                    onSearch = { showSearch = true },
                    onShare = { ShareUtil.shareText(ctx, Sessions.transcript(active)) },
                    onClear = { active.send("\u000c") },
                    onSave = { saveOutput(ctx, Sessions.transcript(active)) },
                    item = { t, i, a -> item(t, i, a) }
                )
            }
        }
    }
    if (showSearch && active != null) {
        TerminalSearchDialog(Sessions.transcript(active), onDismiss = { showSearch = false })
    }
    if (showShortcuts && active != null) {
        ShortcutsSheet(active, onDismiss = { showShortcuts = false })
    }
}

@Composable
private fun LongPressItems(
    onSelect: () -> Unit, onCopyAll: () -> Unit, onPaste: () -> Unit, onSearch: () -> Unit,
    onShare: () -> Unit, onClear: () -> Unit, onSave: () -> Unit,
    item: @Composable (String, androidx.compose.ui.graphics.vector.ImageVector, () -> Unit) -> Unit
) {
    // "Copy" starts native text selection (handles + copy action); "Select All" copies the full terminal.
    item("Copy (select text)", Icons.Filled.ContentCopy, onSelect)
    item("Paste", Icons.Filled.ContentPaste, onPaste)
    item("Select All (copy everything)", Icons.Filled.SelectAll, onCopyAll)
    item("Search output", Icons.Filled.Search, onSearch)
    item("Share", Icons.Filled.Share, onShare)
    item("Clear", Icons.Filled.Delete, onClear)
    item("Save output to file", Icons.Filled.Save, onSave)
}

private fun toast(ctx: android.content.Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

private fun saveOutput(ctx: android.content.Context, text: String) {
    try {
        val name = "nexus-output-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".txt"
        val f = File(NxPaths.home(ctx), name)
        f.writeText(text)
        toast(ctx, "Saved to ~/$name")
    } catch (e: Exception) { toast(ctx, "Save failed: ${e.message}") }
}

@Composable
private fun SessionTabs(onRename: (SessionInfo) -> Unit, onFullscreen: () -> Unit, onOverflow: () -> Unit, overflowMenu: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var menuFor by remember { mutableStateOf<Int?>(null) }
    var newMenu by remember { mutableStateOf(false) }
    var linuxBusy by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(Sessions.list.toList(), key = { it.id }) { s ->
                val selected = s.id == Sessions.active()?.id
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.defaultMinSize(minHeight = 40.dp).a11y("Session ${s.name}").clickable {
                        if (selected) menuFor = s.id else Sessions.activeId = s.id
                    }
                ) {
                    Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(if (s.exited) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(s.name, maxLines = 1, style = MaterialTheme.typography.labelLarge)
                        if (selected) {
                            Icon(Icons.Filled.ArrowDropDown, "Session menu")
                            DropdownMenu(expanded = menuFor == s.id, onDismissRequest = { menuFor = null }) {
                                DropdownMenuItem(text = { Text("Rename") }, onClick = { menuFor = null; onRename(s) })
                                DropdownMenuItem(text = { Text("Restart") }, onClick = { menuFor = null; Sessions.restart(ctx, s.id) })
                                DropdownMenuItem(text = { Text("Close") }, onClick = { menuFor = null; Sessions.close(ctx, s.id) })
                            }
                        }
                    }
                }
            }
        }
        Box {
            IconButton(onClick = { newMenu = true }) { Icon(Icons.Filled.Add, "New session") }
            DropdownMenu(expanded = newMenu, onDismissRequest = { newMenu = false }) {
                val shells = remember { ShellDetector.detect(ctx) }
                shells.forEach { sh ->
                    DropdownMenuItem(text = { Text("New " + sh.substringAfterLast('/')) },
                        onClick = { newMenu = false; Sessions.create(ctx, shell = sh) })
                }
                if (LinuxRootfs.isBundled(ctx)) {
                    val label = if (LinuxRootfs.isExtracted(ctx)) "New Linux (Debian)" else "Set up Linux (Debian)…"
                    DropdownMenuItem(text = { Text(label) }, onClick = {
                        newMenu = false
                        scope.launch {
                            linuxBusy = true
                            val ok = LinuxRootfs.ensureExtracted(ctx)
                            linuxBusy = false
                            if (ok) Sessions.createLinux(ctx)
                            else toast(ctx, LinuxRootfs.lastError.value ?: "Could not set up Linux")
                        }
                    })
                }
            }
        }
        IconButton(onClick = onFullscreen) { Icon(Icons.Filled.Fullscreen, "Full screen") }
        Box {
            IconButton(onClick = onOverflow) { Icon(Icons.Filled.MoreVert, "More") }
            overflowMenu()
        }
    }
    if (linuxBusy) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Setting up Linux") },
            text = {
                Column {
                    Text(LinuxRootfs.progress.value.ifBlank { "Extracting the bundled Debian environment…" })
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun TerminalHost(info: SessionInfo, modifier: Modifier, onLongPress: (MotionEvent) -> Unit) {
    // Reading settings here makes AndroidView.update re-run when they change.
    AndroidView(
        modifier = modifier,
        factory = { c ->
            val client = NexusViewClient(c, info, onLongPress) { d ->
                AppSettings.fontSize = (AppSettings.fontSize + d).coerceIn(8, 40)
            }
            TerminalView(c, null).apply {
                setTerminalViewClient(client)
                attachSession(info.session)
                isFocusable = true
                isFocusableInTouchMode = true
                Sessions.attachedView = this
                Sessions.attachedInfo = info
                contentDescription = "Terminal ${info.name}"
                requestFocus()
            }
        },
        update = { tv -> Appearance.apply(tv) }
    )
    DisposableEffect(info.id, info.generation) {
        onDispose {
            if (Sessions.attachedInfo?.id == info.id) { Sessions.attachedView = null; Sessions.attachedInfo = null }
        }
    }
}

@Composable
private fun ExtraKeysBar() {
    val keys = ExtraKeyCatalog.parse(AppSettings.extraKeys)
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).horizontalScroll(rememberScrollState()).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keys.forEach { k ->
            val on = ExtraKeyCatalog.isActive(k)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.defaultMinSize(minWidth = 46.dp, minHeight = 46.dp).a11y(k.desc).clickable { ExtraKeyCatalog.press(k) }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                    Text(k.label, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun TerminalSearchDialog(transcript: String, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    val lines = remember(transcript) { transcript.lines() }
    val results = remember(q, lines) {
        if (q.isBlank()) emptyList() else lines.withIndex().filter { it.value.contains(q, ignoreCase = true) }.take(500)
    }
    val hl = MaterialTheme.colorScheme.primary
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search terminal output") },
        text = {
            Column {
                OutlinedTextField(q, { q = it }, singleLine = true, label = { Text("Search") }, modifier = Modifier.fillMaxWidth())
                Text("${results.size} matching lines", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 6.dp))
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(results) { r ->
                        Text(highlight(r.value.trimEnd(), q, hl), fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth().clickable { ShareUtil.copy(ctx, r.value); toast(ctx, "Line copied") }.padding(vertical = 3.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

fun highlight(text: String, q: String, color: androidx.compose.ui.graphics.Color): AnnotatedString = buildAnnotatedString {
    if (q.isBlank()) { append(text); return@buildAnnotatedString }
    var i = 0
    while (true) {
        val j = text.indexOf(q, i, ignoreCase = true)
        if (j < 0) { append(text.substring(i)); break }
        append(text.substring(i, j))
        withStyle(SpanStyle(background = color.copy(alpha = 0.4f))) { append(text.substring(j, j + q.length)) }
        i = j + q.length
    }
}

@Composable
private fun ShortcutsSheet(info: SessionInfo, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var cloneDialog by remember { mutableStateOf(false) }
    var addDialog by remember { mutableStateOf(false) }
    val custom = ShortcutStore.custom()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Key shortcuts", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShortcutStore.builtin.forEach { s -> AssistChip(onClick = { info.send(s.payload) }, label = { Text(s.label) }) }
                }
            }
            item { Text("Git actions (typed into the terminal)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("status" to "git status", "log" to "git log --oneline -20", "branch" to "git branch -a",
                        "pull" to "git pull", "push" to "git push").forEach { (l, c) ->
                        AssistChip(onClick = { info.send("$c\n"); onDismiss() }, label = { Text(l) })
                    }
                    AssistChip(onClick = { cloneDialog = true }, label = { Text("clone…") })
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("My shortcuts", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    TextButton(onClick = { addDialog = true }) { Text("Add") }
                }
            }
            items(custom) { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(onClick = { info.send(s.payload.replace("\\n", "\n")); onDismiss() }, label = { Text(s.label) })
                    Text(s.payload, Modifier.weight(1f).padding(start = 8.dp), maxLines = 1, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    IconButton(onClick = { ShortcutStore.saveCustom(custom - s) }) { Icon(Icons.Filled.Delete, "Delete shortcut") }
                }
            }
        }
    }
    if (cloneDialog) TextInputDialog("git clone", "", "Repository URL", "Clone", onConfirm = { url ->
        cloneDialog = false
        if (url.isNotBlank()) { info.send("git clone '" + url.replace("'", "'\\''") + "'\n"); onDismiss() }
    }, onDismiss = { cloneDialog = false })
    if (addDialog) {
        var label by remember { mutableStateOf("") }
        var payload by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addDialog = false },
            title = { Text("New shortcut") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(label, { label = it }, label = { Text("Label") }, singleLine = true)
                    OutlinedTextField(payload, { payload = it }, label = { Text("Text to type (use \\n for Enter)") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (label.isNotBlank() && payload.isNotEmpty()) ShortcutStore.saveCustom(custom + com.nexus.terminal.data.ShortcutDef(label, payload))
                    addDialog = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { addDialog = false }) { Text("Cancel") } }
        )
    }
}
