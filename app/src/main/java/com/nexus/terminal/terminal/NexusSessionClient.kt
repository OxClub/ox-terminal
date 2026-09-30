package com.nexus.terminal.terminal
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.nexus.terminal.util.NxLog

class NexusSessionClient(
    private val sessionInfo: SessionInfo,
    private val onScreenUpdate: () -> Unit,
    private val onSessionFinished: (String) -> Unit
) : TerminalSessionClient {
    override fun onEmulatorSet() { NxLog.d("Emulator ready: ${sessionInfo.name}") }
    override fun onTextChanged(changedNotifier: TerminalSession?) { onScreenUpdate() }
    override fun onSessionFinished(session: TerminalSession?) { onSessionFinished("Session finished: ${sessionInfo.name}") }
    override fun onCopyTextToClipboard(text: String?) { NxLog.d("Copy") }
    override fun onPasteTextFromClipboard(text: String?) { NxLog.d("Paste") }
    override fun onBell(session: TerminalSession?) { NxLog.d("Bell") }
    override fun getTerminalCursorStyle(): Int = 2
}
