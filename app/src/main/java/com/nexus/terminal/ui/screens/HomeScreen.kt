package com.nexus.terminal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexus.terminal.distro.Distro
import com.nexus.terminal.distro.DistroLaunch
import com.nexus.terminal.distro.DistroManager
import com.nexus.terminal.terminal.Sessions
import com.nexus.terminal.ui.ScreenScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(back: () -> Unit, go: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val distros = remember { runCatching { DistroManager.catalog(ctx) }.getOrDefault(emptyList()) }
    var installed by remember { mutableStateOf(distros.filter { DistroManager.isInstalled(ctx, it.id) }.map { it.id }.toSet()) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var progressText by remember { mutableStateOf("") }
    var progressPct by remember { mutableIntStateOf(-1) }
    var error by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(title = "Nexus Terminal", onBack = back) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("📥 Linux Distributions", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 8.dp))
            }
            
            items(distros) { d ->
                val isInstalled = d.id in installed
                val isBusy = busyId == d.id
                
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(d.name, style = MaterialTheme.typography.titleMedium)
                                Text("v${d.version}  •  ${d.sizeMb} MB", style = MaterialTheme.typography.bodySmall)
                            }
                            if (isInstalled) {
                                Surface(color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(4.dp)) {
                                    Text("✓", modifier = Modifier.padding(4.dp), color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
                        
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
                                            error = "Install failed: ${e.message}"
                                        }
                                        busyId = null
                                    }
                                }, modifier = Modifier.weight(1f)) {
                                    Text("Install")
                                }
                            } else {
                                Button(onClick = {
                                    go("terminal")
                                    scope.launch(Dispatchers.IO) {
                                        val cmd = DistroLaunch.command(ctx, d.id)
                                        Sessions.create(ctx, d.name, cmd)
                                    }
                                }, modifier = Modifier.weight(1f)) {
                                    Text("Open")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (error != null) {
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text("Error") },
            text = { Text(error ?: "") },
            confirmButton = { Button(onClick = { error = null }) { Text("OK") } }
        )
    }
}
