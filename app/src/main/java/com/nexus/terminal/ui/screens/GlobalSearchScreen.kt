package com.nexus.terminal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexus.terminal.data.CommandHistory
import com.nexus.terminal.terminal.CommandDb
import com.nexus.terminal.ui.ScreenScaffold

@Composable
fun GlobalSearchScreen(back: () -> Unit, go: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val cmds = CommandDb.search(query)
    val hist = CommandHistory.search(query)
    ScreenScaffold(title = "Search", onBack = back) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            OutlinedTextField(query, { query = it }, label = { Text("Search all") }, leadingIcon = { Icon(Icons.Filled.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(8.dp))
            LazyColumn(Modifier.weight(1f)) {
                if (cmds.isNotEmpty()) {
                    item { Text("Commands", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) }
                    items(cmds) { c -> ListItem(headlineContent = { Text(c.name) }, supportingContent = { Text(c.desc) }) }
                }
                if (hist.isNotEmpty()) {
                    item { Text("History", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) }
                    items(hist) { e -> ListItem(headlineContent = { Text(e.cmd, maxLines = 1) }) }
                }
                if (cmds.isEmpty() && hist.isEmpty() && query.isNotEmpty()) {
                    item { Text("No results", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun Icon(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?) {
    androidx.compose.material3.Icon(icon, contentDescription)
}
