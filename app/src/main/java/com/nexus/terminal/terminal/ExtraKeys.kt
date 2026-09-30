package com.nexus.terminal.terminal

import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import com.nexus.terminal.data.AppSettings

enum class KeyKind { KEYCODE, TEXT, MOD }

data class XKey(val id: String, val label: String, val kind: KeyKind, val desc: String, val keyCode: Int = 0, val text: String = "")

object ExtraKeyCatalog {
    val all: List<XKey> = listOf(
        XKey("esc", "ESC", KeyKind.KEYCODE, "Escape", KeyEvent.KEYCODE_ESCAPE),
        XKey("tab", "TAB", KeyKind.KEYCODE, "Tab", KeyEvent.KEYCODE_TAB),
        XKey("ctrl", "CTRL", KeyKind.MOD, "Control modifier"),
        XKey("alt", "ALT", KeyKind.MOD, "Alt modifier"),
        XKey("shift", "SHIFT", KeyKind.MOD, "Shift modifier"),
        XKey("up", "↑", KeyKind.KEYCODE, "Arrow up", KeyEvent.KEYCODE_DPAD_UP),
        XKey("down", "↓", KeyKind.KEYCODE, "Arrow down", KeyEvent.KEYCODE_DPAD_DOWN),
        XKey("left", "←", KeyKind.KEYCODE, "Arrow left", KeyEvent.KEYCODE_DPAD_LEFT),
        XKey("right", "→", KeyKind.KEYCODE, "Arrow right", KeyEvent.KEYCODE_DPAD_RIGHT),
        XKey("home", "HOME", KeyKind.KEYCODE, "Home", KeyEvent.KEYCODE_MOVE_HOME),
        XKey("end", "END", KeyKind.KEYCODE, "End", KeyEvent.KEYCODE_MOVE_END),
        XKey("pgup", "PGUP", KeyKind.KEYCODE, "Page up", KeyEvent.KEYCODE_PAGE_UP),
        XKey("pgdn", "PGDN", KeyKind.KEYCODE, "Page down", KeyEvent.KEYCODE_PAGE_DOWN),
        XKey("slash", "/", KeyKind.TEXT, "Slash", text = "/"),
        XKey("dash", "-", KeyKind.TEXT, "Dash", text = "-"),
        XKey("under", "_", KeyKind.TEXT, "Underscore", text = "_"),
        XKey("pipe", "|", KeyKind.TEXT, "Pipe", text = "|"),
        XKey("tilde", "~", KeyKind.TEXT, "Tilde", text = "~"),
        XKey("dollar", "$", KeyKind.TEXT, "Dollar", text = "$"),
        XKey("amp", "&", KeyKind.TEXT, "Ampersand", text = "&"),
        XKey("semi", ";", KeyKind.TEXT, "Semicolon", text = ";"),
        XKey("colon", ":", KeyKind.TEXT, "Colon", text = ":"),
        XKey("paren", "( )", KeyKind.TEXT, "Parentheses pair", text = "()\u001b[D"),
        XKey("brace", "{ }", KeyKind.TEXT, "Braces pair", text = "{}\u001b[D"),
        XKey("bracket", "[ ]", KeyKind.TEXT, "Brackets pair", text = "[]\u001b[D"),
        XKey("quote", "\"", KeyKind.TEXT, "Double quote", text = "\""),
        XKey("squote", "'", KeyKind.TEXT, "Single quote", text = "'"),
        XKey("backslash", "\\", KeyKind.TEXT, "Backslash", text = "\\"),
        XKey("star", "*", KeyKind.TEXT, "Asterisk", text = "*"),
        XKey("gt", ">", KeyKind.TEXT, "Greater than", text = ">"),
        XKey("lt", "<", KeyKind.TEXT, "Less than", text = "<"),
        XKey("eq", "=", KeyKind.TEXT, "Equals", text = "="),
        XKey("dot", ".", KeyKind.TEXT, "Dot", text = "."),
        XKey("f1", "F1", KeyKind.KEYCODE, "F1", KeyEvent.KEYCODE_F1), XKey("f2", "F2", KeyKind.KEYCODE, "F2", KeyEvent.KEYCODE_F2),
        XKey("f3", "F3", KeyKind.KEYCODE, "F3", KeyEvent.KEYCODE_F3), XKey("f4", "F4", KeyKind.KEYCODE, "F4", KeyEvent.KEYCODE_F4),
        XKey("f5", "F5", KeyKind.KEYCODE, "F5", KeyEvent.KEYCODE_F5), XKey("f6", "F6", KeyKind.KEYCODE, "F6", KeyEvent.KEYCODE_F6),
        XKey("f7", "F7", KeyKind.KEYCODE, "F7", KeyEvent.KEYCODE_F7), XKey("f8", "F8", KeyKind.KEYCODE, "F8", KeyEvent.KEYCODE_F8),
        XKey("f9", "F9", KeyKind.KEYCODE, "F9", KeyEvent.KEYCODE_F9), XKey("f10", "F10", KeyKind.KEYCODE, "F10", KeyEvent.KEYCODE_F10),
        XKey("f11", "F11", KeyKind.KEYCODE, "F11", KeyEvent.KEYCODE_F11), XKey("f12", "F12", KeyKind.KEYCODE, "F12", KeyEvent.KEYCODE_F12)
    )

    fun parse(layout: String): List<XKey> {
        val map = all.associateBy { it.id }
        return layout.split(",").mapNotNull { map[it.trim()] }
    }

    /** Applies sticky modifiers to a literal text key and returns what to write to the shell. */
    fun transformText(text: String, ctrl: Boolean, alt: Boolean, shift: Boolean): String {
        var t = text
        if (shift && t.length == 1) t = t.uppercase()
        if (ctrl && t.length == 1 && t[0].uppercaseChar() in '@'..'_') t = (t[0].uppercaseChar().code and 0x1f).toChar().toString()
        if (alt) t = "\u001b$t"
        return t
    }

    fun press(key: XKey) {
        val view = Sessions.attachedView
        val info = Sessions.attachedInfo
        if (AppSettings.haptic) view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        when (key.kind) {
            KeyKind.MOD -> when (key.id) {
                "ctrl" -> Modifiers.ctrl = !Modifiers.ctrl
                "alt" -> Modifiers.alt = !Modifiers.alt
                "shift" -> Modifiers.shift = !Modifiers.shift
            }
            KeyKind.KEYCODE -> view?.let {
                it.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key.keyCode))
                it.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key.keyCode))
            }
            KeyKind.TEXT -> info?.send(transformText(key.text, Modifiers.consumeCtrl(), Modifiers.consumeAlt(), Modifiers.consumeShift()))
        }
    }

    fun isActive(key: XKey) = when (key.id) {
        "ctrl" -> Modifiers.ctrl
        "alt" -> Modifiers.alt
        "shift" -> Modifiers.shift
        else -> false
    }
}
