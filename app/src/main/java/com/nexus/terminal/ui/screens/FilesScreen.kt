package com.nexus.terminal.ui.screens

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Delete
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.terminal.files.Archive
import com.nexus.terminal.files.FileOps
import com.nexus.terminal.files.SortMode
import com.nexus.terminal.terminal.Sessions
import com.nexus.terminal.ui.ScreenScaffold
import com.nexus.terminal.util.NxPaths
import com.nexus.terminal.util.ShareUtil
import java.io.File

@Composable
fun FilesScreen(onOpenEditor: (String) -> Unit, onGoTerminal: () -> Unit) {
    val ctx = LocalContext.current
    var cwd by remember { mutableStateOf(NxPaths.home(ctx)) }
    var files by remember { mutableStateOf(listOf<File>()) }
    var sort by remember { mutableStateOf(SortMode.NAME) }
    var showHidden by remember { mutableStateOf(false) }
    var selectedFiles by remember { mutableStateOf(setOf<File>()) }
    var newName by remember { mutableStateOf<String?>(null) }
    var renaming by remember { mutableStateOf<File?>(null) }
    var showInfo by remember { mutableStateOf<File?>(null) }
    var copyClip by remember { mutableStateOf<List<File>>(emptyList()) }
    var cutMode by remember { mutableStateOf(false) }

    fun refresh() {
        files = try { FileOps.list(cwd, showHidden, sort) } catch (e: Exception) { emptyList() }
    }
    fun goUp() { cwd = cwd.parentFile ?: cwd; refresh() }
    fun goDir(d: File) { cwd = d; selectedFiles = emptySet(); refresh() }

    LaunchedEffect(cwd, sort, showHidden) { refresh() }

    ScreenScaffold(
        title = cwd.name.ifEmpty { "Home" },
        onBack = { if (cwd.parentFile != null) goUp() },
        actions = {
            IconButton(onClick = { showHidden = !showHidden }) { Icon(Icons.Filled.Visibility, "Show hidden") }
            IconButton(onClick = { sort = SortMode.values()[(sort.ordinal + 1) % SortMode.values().size] }) {
                Icon(Icons.Filled.SortByAlpha, "Sort: ${sort.name}")
            }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Text(cwd.absolutePath, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace,
                fontSize = 10.sp, modifier = Modifier.fillMaxWidth().padding(8.dp).horizontalScroll(rememberScrollState()))
            if (files.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Folder is empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(files, key = { it.absolutePath }) { f ->
                        val sel = f in selectedFiles
                        Surface(
                            color = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth().clickable(enabled = !f.isDirectory) { if (f.isFile && FileOps.isText(f)) onOpenEditor(f.absolutePath) }
                                .combinedClickable(onClick = { if (f.isDirectory) goDir(f) }, onLongClick = { selectedFiles = setOf(f) })
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (f.isDirectory) Icons.Filled.Folder else Icons.Filled.Description, null, modifier = Modifier.size(24.dp))
                                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                    Text(f.name, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                                    Text(FileOps.formatSize(if (f.isDirectory) FileOps.folderSize(f) else f.length()),
                                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (sel) Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            if (selectedFiles.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { copyClip = selectedFiles.toList(); cutMode = false }, modifier = Modifier.weight(1f)) { Text("Copy") }
                    OutlinedButton(onClick = { copyClip = selectedFiles.toList(); cutMode = true }, modifier = Modifier.weight(1f)) { Text("Cut") }
                    OutlinedButton(onClick = { showInfo = selectedFiles.first() }, modifier = Modifier.weight(1f)) { Text("Info") }
                    OutlinedButton(onClick = { selectedFiles = emptySet() }, modifier = Modifier.weight(1f)) { Text("Clear") }
                }
            }
            if (copyClip.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        copyClip.forEach { FileOps.copy(it, cwd) }
                        copyClip = emptyList(); refresh()
                    }, modifier = Modifier.weight(1f)) { Text("Paste copy") }
                    if (cutMode) {
                        Button(onClick = {
                            copyClip.forEach { if (it.parentFile == cwd) FileOps.delete(it) else FileOps.move(it, cwd) }
                            copyClip = emptyList(); refresh()
                        }, modifier = Modifier.weight(1f)) { Text("Paste move") }
                    }
                }
            }
        }
    }
    if (renaming != null) {
        TextInputDialog("Rename", renaming!!.name, "New name", onConfirm = { newName ->
            try {
                FileOps.rename(renaming!!, newName); refresh()
            } catch (e: Exception) { }
            renaming = null
        }, onDismiss = { renaming = null })
    }
    showInfo?.let { f ->
        AlertDialog(
            onDismissRequest = { showInfo = null },
            title = { Text(f.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRow("Type", FileOps.typeOf(f))
                    InfoRow("Size", FileOps.formatSize(if (f.isDirectory) FileOps.folderSize(f) else f.length()))
                    InfoRow("Modified", FileOps.formatDate(f.lastModified()))
                    InfoRow("Path", f.absolutePath)
                    InfoRow("Permissions", FileOps.permString(f))
                }
            },
            confirmButton = { TextButton(onClick = { showInfo = null }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showInfo = null; renaming = f }) { Text("Rename") } }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}
