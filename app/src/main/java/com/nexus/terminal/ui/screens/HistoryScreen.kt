package com.nexus.terminal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.nexus.terminal.data.CommandHistory
import com.nexus.terminal.terminal.Sessions
import com.nexus.terminal.ui.ScreenScaffold
import com.nexus.terminal.util.ShareUtil

@Composable
fun HistoryScreen(back: () -> Unit, go: (String) -> Unit) {
    var search by remember { mutableStateOf("") }
    val results = CommandHistory.search(search)
    val ctx = LocalContext.current
    ScreenScaffold(title = "History", onBack = back, actions = {
        IconButton(onClick = { CommandHistory.clear() }) { Icon(Icons.Filled.Delete, "Clear all") }
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            OutlinedTextField(search, { search = it }, label = { Text("Search") }, leadingIcon = { Icon(Icons.Filled.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(8.dp))
            LazyColumn(Modifier.weight(1f)) {
                items(results) { e ->
                    ListItem(
                        headlineContent = { Text(e.cmd, fontFamily = FontFamily.Monospace, fontSize = 12.sp, maxLines = 2) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { Sessions.active()?.send(e.cmd + "\n") }) { Icon(Icons.Filled.PlayArrow, null) }
                                IconButton(onClick = { ShareUtil.copy(ctx, e.cmd) }) { Icon(Icons.Filled.ContentCopy, null) }
                                IconButton(onClick = { CommandHistory.remove(e) }) { Icon(Icons.Filled.Close, null) }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Icon(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?) {
    androidx.compose.material3.Icon(icon, contentDescription)
}
