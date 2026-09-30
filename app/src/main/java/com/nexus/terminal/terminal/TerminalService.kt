package com.nexus.terminal.terminal

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.compose.runtime.snapshotFlow
import com.nexus.terminal.MainActivity
import com.nexus.terminal.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the process (and therefore the shells) alive while sessions exist.
 * It shows a persistent notification with the number of running sessions and an Exit action.
 * It does nothing else: no network, no commands.
 */
class TerminalService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Terminal sessions", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shown while terminal sessions are running"
            }
        )
        startForeground(NOTIF_ID, build(Sessions.list.size))
        scope.launch {
            snapshotFlow { Sessions.list.count { !it.exited } to Sessions.list.size }.collect { (running, total) ->
                if (total == 0) {
                    stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
                } else {
                    nm.notify(NOTIF_ID, build(running))
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_EXIT) {
            Sessions.closeAll(this)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun build(running: Int): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val exit = PendingIntent.getService(
            this, 1, Intent(this, TerminalService::class.java).setAction(ACTION_EXIT),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_terminal)
            .setContentTitle("Nexus Terminal")
            .setContentText(if (running == 1) "1 session running" else "$running sessions running")
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Exit all", exit).build())
            .build()
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    companion object {
        const val CHANNEL = "sessions"
        const val NOTIF_ID = 1
        const val ACTION_EXIT = "com.nexus.terminal.EXIT"
    }
}
