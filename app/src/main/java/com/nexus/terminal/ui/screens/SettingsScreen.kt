package com.nexus.terminal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexus.terminal.data.AppSettings
import com.nexus.terminal.ui.SectionCard
import com.nexus.terminal.ui.ScreenScaffold

@Composable
fun SettingsScreen(go: (String) -> Unit) {
    var showExport by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }

    ScreenScaffold(title = "Settings") { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Terminal", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
            item {
                SectionCard("Theme") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { AppSettings.themeId = "nexus_dark" }) { Text("Dark") }
                        Button(onClick = { AppSettings.themeId = "amoled" }) { Text("AMOLED") }
                        Button(onClick = { AppSettings.themeId = "light" }) { Text("Light") }
                    }
                }
            }
            item {
                SectionCard("Font Size") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("${AppSettings.fontSize}sp", Modifier.weight(1f))
                        IconButton(onClick = { AppSettings.fontSize = (AppSettings.fontSize - 1).coerceIn(8, 40) }) { Icon(Icons.Filled.Remove, null) }
                        IconButton(onClick = { AppSettings.fontSize = (AppSettings.fontSize + 1).coerceIn(8, 40) }) { Icon(Icons.Filled.Add, null) }
                    }
                }
            }
            item { Text("Shell", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
            item {
                SectionCard("Environment") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { go("env") }, Modifier.weight(1f)) { Text("Variables") }
                        Button(onClick = { go("aliases") }, Modifier.weight(1f)) { Text("Aliases") }
                    }
                }
            }
            item { Text("Data", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
            item {
                SectionCard("History & Bookmarks") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { go("history") }, Modifier.weight(1f)) { Text("History") }
                        Button(onClick = { go("bookmarks") }, Modifier.weight(1f)) { Text("Bookmarks") }
                    }
                }
            }
            item {
                SectionCard("Configuration") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showExport = true }, Modifier.weight(1f)) { Text("Export") }
                        Button(onClick = { showReset = true }, Modifier.weight(1f)) { Text("Reset") }
                    }
                }
            }
            item { Text("About", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
            item {
                SectionCard("") {
                    Button(onClick = { go("about") }, Modifier.fillMaxWidth()) { Text("About Nexus Terminal") }
                }
            }
        }
    }
    if (showExport) {
        val json = AppSettings.exportJson()
        AlertDialog(
            onDismissRequest = { showExport = false },
            title = { Text("Configuration JSON") },
            text = { Text(json) },
            confirmButton = { TextButton(onClick = { showExport = false }) { Text("Close") } }
        )
    }
    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Reset all settings?") },
            text = { Text("This cannot be undone.") },
            confirmButton = { Button(onClick = { AppSettings.reset(); showReset = false }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text("Cancel") } }
        )
    }
}
