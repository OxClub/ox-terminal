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
import com.nexus.terminal.data.EnvStore
import com.nexus.terminal.ui.ScreenScaffold

@Composable
fun EnvScreen(back: () -> Unit) {
    var vars by remember { mutableStateOf(EnvStore.all()) }
    var showNew by remember { mutableStateOf(false) }
    ScreenScaffold(title = "Environment variables", onBack = back, actions = { IconButton(onClick = { showNew = true }) { Icon(Icons.Filled.Add, null) } }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad)) {
            items(vars) { v ->
                ListItem(
                    headlineContent = { Text(v.name) },
                    supportingContent = { Text(v.value) },
                    trailingContent = { IconButton(onClick = { vars = vars - v; EnvStore.save(vars) }) { Icon(Icons.Filled.Delete, null) } }
                )
            }
        }
    }
    if (showNew) {
        var name by remember { mutableStateOf("") }
        var value by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNew = false },
            title = { Text("New variable") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                    OutlinedTextField(value, { value = it }, label = { Text("Value") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (EnvStore.isValidName(name) && value.isNotEmpty()) {
                        vars = vars + com.nexus.terminal.data.EnvVar(name, value)
                        EnvStore.save(vars); showNew = false
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
