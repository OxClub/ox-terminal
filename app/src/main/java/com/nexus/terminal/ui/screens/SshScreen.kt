package com.nexus.terminal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexus.terminal.data.SecureStore
import com.nexus.terminal.terminal.Sessions
import com.nexus.terminal.ui.ScreenScaffold

@Composable
fun SshScreen(back: () -> Unit, go: (String) -> Unit) {
    val ctx = LocalContext.current
    var profiles by remember { mutableStateOf(SecureStore.profiles(ctx)) }
    var showNew by remember { mutableStateOf(false) }
    ScreenScaffold(title = "SSH profiles", onBack = back, actions = { IconButton(onClick = { showNew = true }) { Icon(Icons.Filled.Add, null) } }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad)) {
            items(profiles) { p ->
                ListItem(
                    headlineContent = { Text(p.name) },
                    supportingContent = { Text("${p.user}@${p.host}:${p.port}") },
                    trailingContent = { Button(onClick = { Sessions.create(ctx, name = p.name, initialCommand = SecureStore.command(p)) }) { Text("Connect") } }
                )
            }
        }
    }
    if (showNew) {
        var name by remember { mutableStateOf("") }
        var host by remember { mutableStateOf("") }
        var user by remember { mutableStateOf("") }
        var port by remember { mutableStateOf("22") }
        AlertDialog(
            onDismissRequest = { showNew = false },
            title = { Text("New SSH profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                    OutlinedTextField(host, { host = it }, label = { Text("Host") }, singleLine = true)
                    OutlinedTextField(user, { user = it }, label = { Text("User") }, singleLine = true)
                    OutlinedTextField(port, { port = it }, label = { Text("Port") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank() && SecureStore.validHost(host) && SecureStore.validUser(user)) {
                        val p = com.nexus.terminal.data.SshProfile(name, host, port.toIntOrNull() ?: 22, user, "")
                        SecureStore.saveProfiles(ctx, profiles + p)
                        profiles = SecureStore.profiles(ctx)
                        showNew = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showNew = false }) { Text("Cancel") } }
        )
    }
}
