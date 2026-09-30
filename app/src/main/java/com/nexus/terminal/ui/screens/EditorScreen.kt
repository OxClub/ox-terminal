package com.nexus.terminal.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.terminal.files.FileOps
import com.nexus.terminal.files.SyntaxColors
import com.nexus.terminal.files.Syntax
import com.nexus.terminal.files.SyntaxTransformation
import com.nexus.terminal.ui.ScreenScaffold
import java.io.File

@Composable
fun EditorScreen(path: String, back: () -> Unit) {
    val ctx = LocalContext.current
    val file = File(path)
    val isReadOnly = file.length() > 2_000_000
    var content by remember { mutableStateOf("") }
    var dirty by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var lineNum by remember { mutableStateOf(0) }

    LaunchedEffect(path) {
        if (file.exists()) {
            content = file.readText().take(if (isReadOnly) 500_000 else 1_000_000)
            lineNum = content.count { it == '\n' } + 1
        }
    }

    ScreenScaffold(
        title = file.name,
        onBack = back,
        actions = {
            if (!isReadOnly) {
                IconButton(onClick = { saving = true; try { file.writeText(content); dirty = false } catch (e: Exception) { }; saving = false }) {
                    Icon(Icons.Filled.Save, "Save")
                }
            }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Text("$lineNum lines${if (isReadOnly) " (read-only: file > 2 MB)" else ""}", style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.fillMaxWidth().padding(8.dp))
            val lang = Syntax.langOf(file.name)
            val colors = SyntaxColors(
                keyword = MaterialTheme.colorScheme.primary,
                string = MaterialTheme.colorScheme.tertiary,
                comment = MaterialTheme.colorScheme.onSurfaceVariant,
                number = MaterialTheme.colorScheme.secondary
            )
            TextField(
                value = content,
                onValueChange = { if (!isReadOnly) { content = it; dirty = true } },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                visualTransformation = SyntaxTransformation(lang, colors),
                readOnly = isReadOnly
            )
        }
    }
}
