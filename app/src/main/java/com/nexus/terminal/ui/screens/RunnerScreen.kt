package com.nexus.terminal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.terminal.terminal.CodeRunner
import com.nexus.terminal.terminal.RunPlan
import com.nexus.terminal.terminal.Sessions
import com.nexus.terminal.ui.ScreenScaffold
import com.nexus.terminal.util.NxPaths
import java.io.File

@Composable
fun RunnerScreen(back: () -> Unit, openEditor: (String) -> Unit) {
    val ctx = LocalContext.current
    var scripts by remember { mutableStateOf(emptyList<File>()) }
    var selected by remember { mutableStateOf<File?>(null) }
    var plan by remember { mutableStateOf<RunPlan?>(null) }

    LaunchedEffect(Unit) { scripts = NxPaths.scripts(ctx).listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: emptyList() }

    ScreenScaffold(title = "Run scripts", onBack = back) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            if (scripts.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No scripts in ~/scripts", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(scripts) { f ->
                        ListItem(
                            headlineContent = { Text(f.name) },
                            supportingContent = { Text(f.extension, fontFamily = FontFamily.Monospace) },
                            trailingContent = {
                                Button(onClick = { selected = f; plan = CodeRunner.plan(ctx, f) }, modifier = Modifier.defaultMinSize(minWidth = 80.dp)) { Text("Run") }
                            },
                            modifier = Modifier.clickable { openEditor(f.absolutePath) }
                        )
                    }
                }
            }
        }
    }

    selected?.let { f ->
        plan?.let { p ->
            when (p) {
                is RunPlan.Steps -> {
                    Sessions.create(ctx, name = "Run: ${f.name}", initialCommand = p.steps.first().joinToString(" ") { "\"$it\"" })
                    selected = null; plan = null
                }
                is RunPlan.Unsupported -> {
                    AlertDialog(
                        onDismissRequest = { selected = null; plan = null },
                        title = { Text("Cannot run") },
                        text = { Text(p.reason) },
                        confirmButton = { TextButton(onClick = { selected = null; plan = null }) { Text("OK") } }
                    )
                }
            }
        }
    }
}
