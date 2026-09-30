package com.nexus.terminal

import android.app.Application
import com.nexus.terminal.data.AppSettings
import com.nexus.terminal.data.CommandHistory
import com.nexus.terminal.pkg.NxPkg
import com.nexus.terminal.util.NxLog

class NexusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppSettings.init(this)
        NxLog.enabled = AppSettings.debugLogging
        CommandHistory.init(this)
        NxPkg.init(this)
    }
}
