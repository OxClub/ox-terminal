package com.nexus.terminal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexus.terminal.data.BookmarkStore
import com.nexus.terminal.ui.ScreenScaffold

@Composable
fun BookmarksScreen(back: () -> Unit, go: (String) -> Unit) {
    var bookmarks by remember { mutableStateOf(BookmarkStore.all()) }
    ScreenScaffold(title = "Bookmarks", onBack = back) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad)) {
            items(bookmarks) { b ->
                ListItem(
                    headlineContent = { Text(b.label) },
                    supportingContent = { Text("${b.type.name}: ${b.value}") },
                    trailingContent = {
                        Row {
                            Button(onClick = {
                                when (b.type) {
                                    com.nexus.terminal.data.BookmarkType.DIRECTORY -> go("files") // TODO: navigate to dir
                                    com.nexus.terminal.data.BookmarkType.COMMAND -> {} // TODO: send to terminal
                                    else -> {}
                                }
                            }) { Text("Open") }
                            IconButton(onClick = { bookmarks = bookmarks - b; BookmarkStore.remove(b) }) { Icon(Icons.Filled.Delete, null) }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun Icon(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?) {
    androidx.compose.material3.Icon(icon, contentDescription)
}
