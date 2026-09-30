package com.nexus.terminal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class Page(val title: String, val body: String)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pages = listOf(
        Page("Welcome to Nexus Terminal", "A real terminal: every tab is a shell process running on your device through a pseudo-terminal (PTY). Commands you type are executed for real; nothing is simulated."),
        Page("Where your files live", "The terminal home is app-private storage (Files > Terminal home). Other apps cannot read it. Shared storage is separate and only available if you allow it, or via Import / Export in the Files screen."),
        Page("Permissions", "Nexus asks for as little as possible. Storage permission is requested only when you tap \"Enable shared storage\". There is no analytics, and the network is used only for features you start (like refreshing a package repository)."),
        Page("Sessions", "Open several sessions and switch with the tabs above the terminal. Sessions keep running in the background while a notification is visible; use the tab menu to rename, restart or close them."),
        Page("Extra keyboard", "The key row under the terminal has ESC, TAB, CTRL, ALT, arrows and symbols. Tap CTRL then a letter for shortcuts like Ctrl+C. Customize the row in Settings > Keyboard.")
    )
    var i by remember { mutableIntStateOf(0) }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
            Text("${i + 1} / ${pages.size}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(12.dp))
            Text(pages[i].title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            Text(pages[i].body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pages.indices.forEach { k ->
                    Box(Modifier.size(8.dp).background(if (k == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape))
                }
            }
            if (i > 0) TextButton(onClick = { i-- }) { Text("Back") }
            Button(onClick = { if (i < pages.lastIndex) i++ else onDone() }, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                Text(if (i < pages.lastIndex) "Next" else "Start terminal")
            }
        }
    }
}
