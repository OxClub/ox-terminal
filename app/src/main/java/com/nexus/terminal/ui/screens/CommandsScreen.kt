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
import com.nexus.terminal.terminal.CommandDb
import com.nexus.terminal.ui.ScreenScaffold

@Composable
fun CommandsScreen(back: () -> Unit) {
    var search by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf<String?>(null) }
    val results = CommandDb.search(search)
    ScreenScaffold(title = "Commands", onBack = back) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            OutlinedTextField(search, { search = it }, label = { Text("Search") }, leadingIcon = { Icon(Icons.Filled.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(8.dp))
            LazyColumn(Modifier.weight(1f)) {
                items(results) { c ->
                    ElevatedCard(Modifier.fillMaxWidth().padding(8.dp)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text("${c.name} — ${c.desc}", style = MaterialTheme.typography.titleSmall)
                            Text(c.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            if (expanded == c.name) {
                                Spacer(Modifier.height(8.dp))
                                Text(CommandDb.render(c), style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                TextButton(onClick = { expanded = null }) { Text("Collapse") }
                            } else {
                                TextButton(onClick = { expanded = c.name }) { Text("View details") }
                            }
                        }
                    }
                }
            }
        }
    }
}
