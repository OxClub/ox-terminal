package com.nexus.terminal.terminal

import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.nexus.terminal.data.CommandHistory
import com.nexus.terminal.util.NxLog
import com.nexus.terminal.util.ShareUtil
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalViewClient

class NexusViewClient(
    private val ctx: Context,
    private val info: SessionInfo,
    private val onLongPressEvent: (MotionEvent) -> Unit,
    private val onFontDelta: (Int) -> Unit
) : TerminalViewClient {

    private val tracker = LineTracker()
    private val passwordPrompt = Regex("(?i)(password|passphrase|passcode|pin)[^\\n]*[:>?]\\s*$")

    override fun onScale(scale: Float): Float {
        if (scale < 0.9f || scale > 1.1f) { onFontDelta(if (scale > 1f) 1 else -1); return 1f }
        return scale
    }

    override fun onSingleTapUp(e: MotionEvent) {
        Sessions.attachedView?.let { showKeyboard(it) }
    }

    override fun shouldBackButtonBeMappedToEscape() = false
    override fun shouldEnforceCharBasedInput() = true
    override fun shouldUseCtrlSpaceWorkaround() = false
    override fun isTerminalViewSelected() = true
    override fun copyModeChanged(copyMode: Boolean) {}

    override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession): Boolean {
        if (e.isCtrlPressed && e.isShiftPressed) {
            when (keyCode) {
                KeyEvent.KEYCODE_V -> { session.emulator?.paste(ShareUtil.paste(ctx)); return true }
                KeyEvent.KEYCODE_T -> { Sessions.create(ctx); return true }
            }
        }
        when (keyCode) {
            KeyEvent.KEYCODE_ENTER -> commit(session)
            KeyEvent.KEYCODE_DEL -> tracker.type(127)
        }
        return false
    }

    override fun onKeyUp(keyCode: Int, e: KeyEvent) = false

    override fun onLongPress(event: MotionEvent): Boolean {
        onLongPressEvent(event)
        return true
    }

    override fun readControlKey() = Modifiers.consumeCtrl()
    override fun readAltKey() = Modifiers.consumeAlt()
    override fun readShiftKey() = Modifiers.consumeShift()
    override fun readFnKey() = false

    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession): Boolean {
        if (ctrlDown) {
            if (codePoint == 'c'.code || codePoint == 'u'.code || codePoint == 'C'.code || codePoint == 'U'.code) tracker.reset()
        } else {
            tracker.type(codePoint)
            if (codePoint == 13 || codePoint == 10) commit(session)
        }
        return false
    }

    override fun onEmulatorSet() { Sessions.onEmulatorReady(info) }

    private fun commit(session: TerminalSession) {
        val line = tracker.take()
        if (line.isBlank()) return
        if (isPasswordPrompt(session)) { NxLog.d("history", "skipped (password prompt)"); return }
        CommandHistory.add(line)
    }

    /** Never record what was typed at a password / passphrase prompt. */
    private fun isPasswordPrompt(session: TerminalSession): Boolean = try {
        val em = session.emulator
        if (em == null) false else {
            val row = em.cursorRow
            val text = em.screen.getSelectedText(0, row, em.mColumns, row) ?: ""
            passwordPrompt.containsMatchIn(text.trimEnd())
        }
    } catch (e: Exception) { false }

    private fun showKeyboard(v: View) {
        v.requestFocus()
        (ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun logError(tag: String?, message: String?) {}
    override fun logWarn(tag: String?, message: String?) {}
    override fun logInfo(tag: String?, message: String?) {}
    override fun logDebug(tag: String?, message: String?) {}
    override fun logVerbose(tag: String?, message: String?) {}
    override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {}
    override fun logStackTrace(tag: String?, e: Exception?) {}
}
