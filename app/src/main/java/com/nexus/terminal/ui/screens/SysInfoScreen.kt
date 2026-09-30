package com.nexus.terminal.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.nexus.terminal.ui.ScreenScaffold
import com.nexus.terminal.ui.InfoRow

@Composable
fun SysInfoScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    ScreenScaffold(title = "System Info", onBack = back) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            InfoRow("Device", Build.DEVICE)
            InfoRow("Model", Build.MODEL)
            InfoRow("Brand", Build.BRAND)
            InfoRow("Manufacturer", Build.MANUFACTURER)
            InfoRow("Android", Build.VERSION.RELEASE + " (API ${Build.VERSION.SDK_INT})")
            InfoRow("Kernel", System.getProperty("os.version") ?: "–")
            InfoRow("Build ID", Build.ID)
            InfoRow("Host", Build.HOST)
            InfoRow("User", Build.USER)
            val rt = Runtime.getRuntime()
            val totalMem = rt.totalMemory() / 1024 / 1024
            val freeMem = rt.freeMemory() / 1024 / 1024
            InfoRow("Memory", "${freeMem}MB / ${totalMem}MB")
            InfoRow("CPU count", Runtime.getRuntime().availableProcessors().toString())
        }
    }
}
