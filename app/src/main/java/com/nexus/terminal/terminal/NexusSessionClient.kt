package com.nexus.terminal.terminal

import android.content.Context
import com.nexus.terminal.data.AppSettings
import com.nexus.terminal.util.NxLog
import com.nexus.terminal.util.ShareUtil
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient

class NexusSessionClient(private val app: Context, private val info: SessionInfo) : TerminalSessionClient {

    override fun onTextChanged(changedSession: TerminalSession) {
        if (Sessions.attachedInfo?.id == info.id) Sessions.attachedView?.onScreenUpdated()
    }

    override fun onTitleChanged(changedSession: TerminalSession) {}

    override fun onSessionFinished(finishedSession: TerminalSession) {
        info.exited = true
        info.exitCode = finishedSession.exitStatus
        val msg = "\r\n[Process completed (exit code ${finishedSession.exitStatus}). Restart it from the session menu.]\r\n"
        val bytes = msg.toByteArray()
        finishedSession.emulator?.append(bytes, bytes.size)
        if (Sessions.attachedInfo?.id == info.id) Sessions.attachedView?.onScreenUpdated()
    }

    override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
        ShareUtil.copy(app, text)
    }

    override fun onPasteTextFromClipboard(session: TerminalSession?) {
        val t = ShareUtil.paste(app)
        if (t.isNotEmpty()) session?.emulator?.paste(t)
    }

    override fun onBell(session: TerminalSession) {
        Sessions.attachedView?.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
    }

    override fun onColorsChanged(session: TerminalSession) {}
    override fun onTerminalCursorStateChange(state: Boolean) {}

    override fun logError(tag: String?, message: String?) { NxLog.e(tag ?: "term", message ?: "") }
    override fun logWarn(tag: String?, message: String?) { NxLog.w(tag ?: "term", message ?: "") }
    override fun logInfo(tag: String?, message: String?) { NxLog.d(tag ?: "term", message ?: "") }
    override fun logDebug(tag: String?, message: String?) { NxLog.d(tag ?: "term", message ?: "") }
    override fun logVerbose(tag: String?, message: String?) {}
    override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) { NxLog.e(tag ?: "term", message ?: "", e) }
    override fun logStackTrace(tag: String?, e: Exception?) { NxLog.e(tag ?: "term", "exception", e) }
}
