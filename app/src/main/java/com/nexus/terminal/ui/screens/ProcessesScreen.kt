package com.nexus.terminal.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.nexus.terminal.terminal.ProcScanner
import com.nexus.terminal.ui.ScreenScaffold
import androidx.compose.foundation.layout.*

@Composable
fun ProcessesScreen(back: () -> Unit, go: (String) -> Unit) {
    var procs by remember { mutableStateOf(emptyList<com.nexus.terminal.terminal.ProcInfo>()) }
    fun refresh() { procs = ProcScanner.scan() }
    LaunchedEffect(Unit) { refresh() }
    ScreenScaffold(title = "Processes", onBack = back, actions = { IconButton(onClick = { refresh() }) { Icon(Icons.Filled.Refresh, null) } }) { pad ->
        if (procs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No processes found.")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(pad)) {
                items(procs.sortedByDescending { it.rssKb }) { p ->
                    ListItem(
                        headlineContent = { Text(p.name, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        supportingContent = { Text("PID: ${p.pid} | RSS: ${p.rssKb}KB", fontFamily = FontFamily.Monospace, fontSize = 10.sp) }
                    )
                }
            }
        }
    }
}
