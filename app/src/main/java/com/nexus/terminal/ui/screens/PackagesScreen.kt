package com.nexus.terminal.ui.screens

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
import com.nexus.terminal.pkg.NxPkg
import com.nexus.terminal.pkg.Versions
import com.nexus.terminal.ui.ScreenScaffold
import kotlinx.coroutines.launch

@Composable
fun PackagesScreen(pendingPkg: Pair<String, String>?, onPendingHandled: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(0) }
    var search by remember { mutableStateOf("") }
    var showRepoDialog by remember { mutableStateOf(false) }
    var showPending by remember { mutableStateOf<Pair<String, String>?>(pendingPkg) }

    LaunchedEffect(pendingPkg) { if (pendingPkg != null) { showPending = pendingPkg; tab = 0 } }

    ScreenScaffold(title = "Packages", actions = {
        IconButton(onClick = { scope.launch { NxPkg.refresh(ctx) } }, enabled = !NxPkg.refreshing.value) {
            if (NxPkg.refreshing.value) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            else Icon(Icons.Filled.Refresh, "Refresh")
        }
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            TabRow(tab) {
                Tab(tab == 0, { tab = 0 }) { Text("Available") }
                Tab(tab == 1, { tab = 1 }) { Text("Installed") }
                Tab(tab == 2, { tab = 2 }) { Text("Updates") }
            }
            when (tab) {
                0 -> {
                    OutlinedTextField(search, { search = it }, label = { Text("Search") }, modifier = Modifier.fillMaxWidth().padding(8.dp))
                    val list = NxPkg.available.filter { search.isBlank() || it.name.contains(search, true) || it.description.contains(search, true) }
                    if (list.isEmpty()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(if (NxPkg.available.isEmpty()) "No repositories configured. Refresh to load." else "No matches.")
                        }
                    } else {
                        LazyColumn(Modifier.weight(1f)) {
                            items(list) { p ->
                                PkgCard(p, installed = NxPkg.installed[p.name] != null, onTap = { scope.launch { NxPkg.install(ctx, p.name) } })
                            }
                        }
                    }
                }
                1 -> {
                    if (NxPkg.installed.isEmpty()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No packages installed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(Modifier.weight(1f)) {
                            items(NxPkg.installed.values.sortedBy { it.name }.toList()) { p ->
                                PkgCard(name = p.name, version = p.version, installed = true, onTap = {
                                    scope.launch { NxPkg.remove(ctx, p.name) }
                                }, label = "Remove")
                            }
                        }
                    }
                }
                2 -> {
                    val updates = NxPkg.updates()
                    if (updates.isEmpty()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("All packages are up to date.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(Modifier.weight(1f)) {
                            item {
                                Button(onClick = { scope.launch { NxPkg.upgradeAll(ctx) } }, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                    Text("Upgrade all (${updates.size})")
                                }
                            }
                            items(updates) { p ->
                                val inst = NxPkg.installed[p.name]
                                PkgCard(p, installed = true, onTap = { scope.launch { NxPkg.install(ctx, p.name) } },
                                    label = "Update to ${p.version}" + if (inst != null) " (from ${inst.version})" else "")
                            }
                        }
                    }
                }
            }
            NxPkg.lastError.value?.let { error ->
                Text(error, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(8.dp).background(MaterialTheme.colorScheme.errorContainer))
            }
        }
    }
    if (showPending != null) {
        AlertDialog(
            onDismissRequest = { showPending = null; onPendingHandled() },
            title = { Text("Install package?") },
            text = { Text("Package: ${showPending!!.second}") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        NxPkg.install(ctx, showPending!!.second)
                        showPending = null; onPendingHandled()
                    }
                }) { Text("Install") }
            },
            dismissButton = { TextButton(onClick = { showPending = null; onPendingHandled() }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PkgCard(p: com.nexus.terminal.pkg.Pkg? = null, name: String = "", version: String = "", installed: Boolean = false,
                     onTap: () -> Unit, label: String = if (installed) "Remove" else "Install") {
    val busy = NxPkg.busy[name].isNullOrEmpty().not()
    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p?.name ?: name, style = MaterialTheme.typography.titleSmall)
                    Text(p?.version ?: version, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = onTap, enabled = !busy) { Text(label) }
            }
            if (p != null) { Text(p.description, style = MaterialTheme.typography.bodySmall); Spacer(Modifier.height(4.dp))
                Text(p.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            NxPkg.busy[name]?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary) }
        }
    }
}
