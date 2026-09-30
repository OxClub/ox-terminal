package com.nexus.terminal

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import com.nexus.terminal.data.AppSettings
import com.nexus.terminal.terminal.Bootstrap
import com.nexus.terminal.terminal.Sessions
import com.nexus.terminal.ui.NexusApp
import com.nexus.terminal.ui.screens.OnboardingScreen
import com.nexus.terminal.ui.theme.NexusTheme
import com.nexus.terminal.ui.theme.Themes

class MainActivity : ComponentActivity() {
    /** (action, package) requested by `nxpkg install foo` from a shell; always confirmed by the user in-app. */
    private var pendingPkg by mutableStateOf<Pair<String, String>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Bootstrap.ensure(this)
        handleIntent(intent)
        if (savedInstanceState == null && AppSettings.onboarded) startSessions()

        setContent {
            val theme = Themes.current()
            val view = androidx.compose.ui.platform.LocalView.current
            SideEffect {
                window.statusBarColor = theme.bg
                window.navigationBarColor = theme.bg
                val c = WindowCompat.getInsetsController(window, view)
                c.isAppearanceLightStatusBars = !theme.dark
                c.isAppearanceLightNavigationBars = !theme.dark
            }
            NexusTheme(theme) {
                if (!AppSettings.onboarded) {
                    OnboardingScreen(onDone = {
                        AppSettings.onboarded = true
                        Sessions.create(this@MainActivity)
                    })
                } else {
                    NexusApp(
                        startRoute = if (AppSettings.startupBehavior == "dashboard") "home" else "terminal",
                        pendingPkg = pendingPkg,
                        onPendingHandled = { pendingPkg = null }
                    )
                }
            }
        }
    }

    private fun startSessions() {
        if (Sessions.list.isNotEmpty()) return
        val restored = AppSettings.startupBehavior == "last" && AppSettings.restoreSessions && Sessions.restoreSaved(this)
        if (!restored && AppSettings.startupBehavior != "dashboard") Sessions.create(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        val a = i?.getStringExtra("nx_action") ?: return
        pendingPkg = a to (i.getStringExtra("nx_pkg") ?: "")
    }

    override fun onPause() {
        super.onPause()
        if (Sessions.list.isNotEmpty()) Sessions.persist(this)
    }
}
