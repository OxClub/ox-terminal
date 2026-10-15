package com.nexus.terminal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexus.terminal.distro.Distro
import com.nexus.terminal.distro.DistroLaunch
import com.nexus.terminal.distro.DistroManager
import com.nexus.terminal.terminal.Sessions
import com.nexus.terminal.ui.MessageDialog
import com.nexus.terminal.ui.ScreenScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DistrosScreen(back: () -> Unit, go: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val distros = remember { runCatching { DistroManager.catalog(ctx) }.getOrDefault(emptyList()) }
    var installed by remember { mutableStateOf(distros.filter { DistroManager.isInstalled(ctx, it.id) }.map { it.id }.toSet()) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var progressText by remember { mutableStateOf("") }
    var progressPct by remember { mutableIntStateOf(-1) }
    var error by remember { mutableStateOf<String?>(null) }
    var toDelete by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(title = "Linux Distros", onBack = back) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(distros) { d ->
                val isInstalled = d.id in installed
                val isBusy = busyId == d.id
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(d.name, style = MaterialTheme.typography.titleMedium)
                        Text("Version ${d.version}  •  ~${d.sizeMb} MB", style = MaterialTheme.typography.bodySmall)
                        if (isBusy) {
                            Text(progressText, style = MaterialTheme.typography.bodySmall)
                            if (progressPct in 0..100) LinearProgressIndicator(progress = { progressPct / 100f }, modifier = Modifier.fillMaxWidth())
                            else LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!isInstalled) {
                                Button(enabled = busyId == null, onClick = {
                                    scope.launch {
                                        busyId = d.id
                                        try {
                                            withContext(Dispatchers.IO) {
                                                DistroManager.install(ctx, d) { msg, pct ->
                                                    progressText = msg
                                                    progressPct = pct
                                                }
                                            }
                                            installed = installed + d.id
                                        } catch (e: Exception) {
                                            error = e.message ?: "Install failed"
                                        }
                                        busyId = null
                                    }
                                }) {
                                    Text("Install")
                                }
                            } else {
                                Button(onClick = {
                                    go("terminal")
                                    scope.launch {
                                        Sessions.create(ctx, name = d.name, kind = com.nexus.terminal.terminal.SessionKind.DISTRO, distroId = d.id)
                                    }
                                }) {
                                    Text("Open")
                                }
                                Button(onClick = { toDelete = d.id }) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (error != null) {
        MessageDialog(title = "Error", message = error ?: "", onDismiss = { error = null })
    }

    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete Distro") },
            text = { Text("Remove ${toDelete}? This cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    DistroManager.uninstall(ctx, toDelete!!)
                    installed = installed - toDelete!!
                    toDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { Button(onClick = { toDelete = null }) { Text("Cancel") } }
        )
    }
}
