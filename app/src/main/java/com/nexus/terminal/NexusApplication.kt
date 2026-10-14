package com.nexus.terminal

import android.app.Application
import android.content.Intent
import android.os.Build
import com.nexus.terminal.data.AppSettings
import com.nexus.terminal.data.CommandHistory
import com.nexus.terminal.pkg.NxPkg
import com.nexus.terminal.util.NxLog

class NexusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 28 && Application.getProcessName().endsWith(":crash")) return
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val trace = android.util.Log.getStackTraceString(e).take(30000)
            val i = Intent(this, CrashActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra("trace", trace)
            startActivity(i)
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(10)
        }
        AppSettings.init(this)
        NxLog.enabled = AppSettings.debugLogging
        CommandHistory.init(this)
        NxPkg.init(this)
    }
}
