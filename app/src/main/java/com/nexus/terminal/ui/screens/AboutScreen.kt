package com.nexus.terminal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.terminal.BuildConfig
import com.nexus.terminal.ui.ScreenScaffold

@Composable
fun AboutScreen(back: () -> Unit) {
    ScreenScaffold(title = "About", onBack = back) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Nexus Terminal", style = MaterialTheme.typography.headlineMedium)
            Text("v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Divider()
            Text("A real terminal for Android. Every tab runs a full PTY shell — no simulation.", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Open source • GitHub: github.com/nexus-terminal\n\n" +
                "Built with:\n" +
                "• Kotlin & Jetpack Compose\n" +
                "• Termux terminal-view (JitPack)\n" +
                "• Apache Commons Compress\n" +
                "• Android Security Crypto\n\n" +
                "Known limitations:\n" +
                "• Targets Android API 28 to run installed packages (API 29+ blocks app-local exec)\n" +
                "• No line-spacing customization; renderer default only\n" +
                "• No bundled fonts; uses system monospace or user-provided file\n" +
                "• History never captures commands typed at password prompts\n" +
                "• Sessions cannot survive process death; recreated shells start in previous directories",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }
    }
}
