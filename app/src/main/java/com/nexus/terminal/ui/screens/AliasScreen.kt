package com.nexus.terminal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.nexus.terminal.data.AliasStore
import com.nexus.terminal.ui.ScreenScaffold

@Composable
fun AliasScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    var aliases by remember { mutableStateOf(AliasStore.all()) }
    var showNew by remember { mutableStateOf(false) }
    ScreenScaffold(title = "Aliases", onBack = back, actions = { IconButton(onClick = { showNew = true }) { Icon(Icons.Filled.Add, null) } }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad)) {
            items(aliases) { a ->
                ListItem(
                    headlineContent = { Text(a.name) },
                    supportingContent = { Text(a.command) },
                    trailingContent = { IconButton(onClick = { aliases = aliases - a; AliasStore.save(ctx, aliases) }) { Icon(Icons.Filled.Delete, null) } }
                )
            }
        }
    }
    if (showNew) {
        var name by remember { mutableStateOf("") }
        var command by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNew = false },
            title = { Text("New alias") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                    OutlinedTextField(command, { command = it }, label = { Text("Command") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (AliasStore.isValidName(name) && command.isNotEmpty()) {
                        aliases = aliases + com.nexus.terminal.data.AliasDef(name, command)
                        AliasStore.save(ctx, aliases); showNew = false
                    }
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showNew = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun Icon(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?) {
    androidx.compose.material3.Icon(icon, contentDescription)
}
