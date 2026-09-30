package com.nexus.terminal.ui.screens

import android.app.ActivityManager
import android.content.Context
import android.os.StatFs
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
import androidx.compose.ui.unit.dp
import com.nexus.terminal.data.BookmarkStore
import com.nexus.terminal.data.CommandHistory
import com.nexus.terminal.files.FileOps
import com.nexus.terminal.terminal.Sessions
import com.nexus.terminal.ui.SectionCard
import com.nexus.terminal.util.NxPaths
import java.io.File

@Composable
fun HomeScreen(go: (String) -> Unit) {
    val ctx = LocalContext.current
    val sessions = Sessions.list.count { !it.exited }
    val historySize = CommandHistory.all().size
    val bookmarks = BookmarkStore.all().size
    var storage by remember { mutableStateOf("") }
    var procCount by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        storage = runCatching {
            val st = StatFs(NxPaths.home(ctx).absolutePath)
            val free = st.availableBlocksLong * st.blockSizeLong
            val total = st.blockCountLong * st.blockSizeLong
            "${FileOps.formatSize(free)} / ${FileOps.formatSize(total)}"
        }.getOrDefault("–")
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        procCount = am.runningAppProcesses?.size ?: 0
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Dashboard", style = MaterialTheme.typography.headlineMedium) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickStat("${sessions}", "Session${if (sessions == 1) "" else "s"}", Icons.Filled.Terminal, Modifier.weight(1f))
                QuickStat("${historySize}", "Commands", Icons.Filled.History, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickStat("${bookmarks}", "Bookmark${if (bookmarks == 1) "" else "s"}", Icons.Filled.Bookmark, Modifier.weight(1f))
                QuickStat("${procCount}", "Process${if (procCount == 1) "" else "es"}", Icons.Filled.Memory, Modifier.weight(1f))
            }
        }
        item {
            SectionCard("Quick Actions") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { Sessions.create(ctx) }, modifier = Modifier.weight(1f)) { Text("New terminal") }
                    OutlinedButton(onClick = { go("files") }, modifier = Modifier.weight(1f)) { Text("Files") }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { go("packages") }, modifier = Modifier.weight(1f)) { Text("Packages") }
                    OutlinedButton(onClick = { go("settings") }, modifier = Modifier.weight(1f)) { Text("Settings") }
                }
            }
        }
        item {
            SectionCard("Storage") {
                Text(storage, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())
            }
        }
        if (historySize > 0) {
            item {
                SectionCard("Recent Commands", trailing = { TextButton(onClick = { go("history") }) { Text("View all") } }) {
                    val recent = CommandHistory.all().take(5)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(recent) {
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                                Text(it.cmd, maxLines = 1, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStat(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = modifier) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
