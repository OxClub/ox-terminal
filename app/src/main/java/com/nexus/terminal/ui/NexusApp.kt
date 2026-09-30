package com.nexus.terminal.ui

import android.net.Uri
import android.view.ViewTreeObserver
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navArgument
import com.nexus.terminal.ui.screens.*

private data class Dest(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun rememberKeyboardVisible(): State<Boolean> {
    val view = LocalView.current
    val s = remember { mutableStateOf(false) }
    DisposableEffect(view) {
        val l = ViewTreeObserver.OnGlobalLayoutListener {
            s.value = ViewCompat.getRootWindowInsets(view)?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(l)
        onDispose { view.viewTreeObserver.removeOnGlobalLayoutListener(l) }
    }
    return s
}

@Composable
fun NexusApp(startRoute: String, pendingPkg: Pair<String, String>?, onPendingHandled: () -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val fullscreen = remember { mutableStateOf(false) }
    val kbd by rememberKeyboardVisible()
    val wide = LocalConfiguration.current.screenWidthDp >= 600

    val tabs = listOf(
        Dest("home", "Home", Icons.Filled.Home),
        Dest("terminal", "Terminal", Icons.Filled.Terminal),
        Dest("files", "Files", Icons.Filled.Folder),
        Dest("packages", "Packages", Icons.Filled.Inventory2),
        Dest("settings", "Settings", Icons.Filled.Settings)
    )
    val isTab = tabs.any { it.route == route }
    val showNav = isTab && !(fullscreen.value && route == "terminal") && !(kbd && route == "terminal")

    fun goTab(r: String) = nav.navigate(r) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    val go: (String) -> Unit = { r -> if (tabs.any { it.route == r }) goTab(r) else nav.navigate(r) }
    val back: () -> Unit = { nav.popBackStack() }

    LaunchedEffect(pendingPkg) { if (pendingPkg != null) goTab("packages") }
    BackHandler(enabled = fullscreen.value && route == "terminal") { fullscreen.value = false }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showNav && !wide) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEach { d ->
                    NavigationBarItem(selected = route == d.route, onClick = { goTab(d.route) },
                        icon = { Icon(d.icon, contentDescription = d.label) }, label = { Text(d.label) })
                }
            }
        }
    ) { pad ->
        Row(Modifier.padding(pad).fillMaxSize()) {
            if (showNav && wide) NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                Spacer(Modifier.weight(1f))
                tabs.forEach { d ->
                    NavigationRailItem(selected = route == d.route, onClick = { goTab(d.route) },
                        icon = { Icon(d.icon, contentDescription = d.label) }, label = { Text(d.label) })
                }
                Spacer(Modifier.weight(1f))
            }
            NavHost(nav, startDestination = startRoute, modifier = Modifier.weight(1f)) {
                composable("home") { HomeScreen(go) }
                composable("terminal") {
                    TerminalScreen(fullscreen.value, { fullscreen.value = !fullscreen.value }, go)
                }
                composable("files") { FilesScreen(onOpenEditor = { go("editor?path=" + Uri.encode(it)) }, onGoTerminal = { go("terminal") }) }
                composable("packages") { PackagesScreen(pendingPkg, onPendingHandled) }
                composable("settings") { SettingsScreen(go) }
                composable("editor?path={path}", arguments = listOf(navArgument("path") { type = NavType.StringType; defaultValue = "" })) {
                    EditorScreen(it.arguments?.getString("path").orEmpty(), back)
                }
                composable("processes") { ProcessesScreen(back, go) }
                composable("commands") { CommandsScreen(back) }
                composable("sysinfo") { SysInfoScreen(back) }
                composable("runner") { RunnerScreen(back) { p -> go("editor?path=" + Uri.encode(p)) } }
                composable("ssh") { SshScreen(back, go) }
                composable("env") { EnvScreen(back) }
                composable("aliases") { AliasScreen(back) }
                composable("bookmarks") { BookmarksScreen(back, go) }
                composable("history") { HistoryScreen(back, go) }
                composable("search") { GlobalSearchScreen(back, go) }
                composable("about") { AboutScreen(back) }
            }
        }
    }
}
