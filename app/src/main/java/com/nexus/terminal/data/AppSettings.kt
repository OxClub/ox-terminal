package com.nexus.terminal.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableIntStateOf
import com.nexus.terminal.BuildConfig
import org.json.JSONObject
import kotlin.reflect.KProperty

/**
 * Preferences backed by SharedPreferences. Every property read subscribes Compose to [version],
 * so any change recomposes the UI that displays it.
 */
object AppSettings {
    lateinit var sp: SharedPreferences
    val version = mutableIntStateOf(0)

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences("nexus_settings", Context.MODE_PRIVATE)
    }

    var onboarded by BoolPref("onboarded", false)

    // Terminal
    var themeId by StrPref("theme", "nexus_dark")
    var customBg by StrPref("custom_bg", "#0D1117")
    var customFg by StrPref("custom_fg", "#E6EDF3")
    var customCursor by StrPref("custom_cursor", "#58A6FF")
    var fontSize by IntPref("font_size", 14)
    var fontFamily by StrPref("font_family", "monospace")
    var customFontPath by StrPref("custom_font_path", "")
    var cursorStyle by IntPref("cursor_style", 0)
    var cursorBlink by BoolPref("cursor_blink", true)
    var scrollback by IntPref("scrollback", 5000)

    // Keyboard
    var extraKeys by StrPref("extra_keys", "esc,tab,ctrl,alt,shift,up,down,left,right,home,end,pgup,pgdn,slash,dash,under,pipe,tilde,dollar,amp,semi,colon,paren,brace")
    var haptic by BoolPref("haptic", true)
    var customShortcuts by StrPref("custom_shortcuts", "")

    // Shell
    var defaultShell by StrPref("default_shell", "")
    var startupEnabled by BoolPref("startup_enabled", false)
    var startupCommands by StrPref("startup_commands", "")
    var envVars by StrPref("env_vars", "")
    var aliases by StrPref("aliases", "")
    var loadAliases by BoolPref("load_aliases", false)

    // Sessions
    var restoreSessions by BoolPref("restore_sessions", true)
    var startupBehavior by StrPref("startup_behavior", "last") // last | new | dashboard
    var savedSessions by StrPref("saved_sessions", "")

    // Files
    var defaultDir by StrPref("default_dir", "")
    var showHidden by BoolPref("show_hidden", false)
    var sortMode by StrPref("sort_mode", "name")

    // Packages
    var repos by StrPref("repos", "")
    var autoCheckUpdates by BoolPref("auto_check_updates", false)

    // History / bookmarks / privacy
    var recordHistory by BoolPref("record_history", true)
    var historySize by IntPref("history_size", 1000)
    var bookmarks by StrPref("bookmarks", "")
    var debugLogging by BoolPref("debug_logging", BuildConfig.DEBUG)

    /** Keys included in config export. Private keys / credentials are never stored here. */
    val exportable = setOf(
        "theme", "custom_bg", "custom_fg", "custom_cursor", "font_size", "font_family", "cursor_style",
        "cursor_blink", "scrollback", "extra_keys", "haptic", "custom_shortcuts", "default_shell",
        "env_vars", "aliases", "load_aliases", "restore_sessions", "startup_behavior", "show_hidden",
        "sort_mode", "repos", "auto_check_updates", "record_history", "history_size", "bookmarks"
    )

    fun exportJson(): String {
        val o = JSONObject()
        for ((k, v) in sp.all) {
            if (k in exportable) when (v) {
                is Boolean -> o.put(k, v)
                is Int -> o.put(k, v)
                is String -> o.put(k, v)
            }
        }
        o.put("_format", "nexus-terminal-config-1")
        return o.toString(2)
    }

    /** @return number of imported keys */
    fun importJson(json: String): Int {
        val o = JSONObject(json)
        var n = 0
        val e = sp.edit()
        for (k in o.keys()) {
            if (k !in exportable) continue
            when (val v = o.get(k)) {
                is Boolean -> e.putBoolean(k, v)
                is Int -> e.putInt(k, v)
                is String -> e.putString(k, v)
                else -> continue
            }
            n++
        }
        e.apply()
        version.intValue++
        return n
    }

    fun reset() {
        sp.edit().clear().apply()
        version.intValue++
    }
}

class BoolPref(private val key: String, private val def: Boolean) {
    operator fun getValue(t: Any?, p: KProperty<*>): Boolean {
        AppSettings.version.intValue
        return AppSettings.sp.getBoolean(key, def)
    }
    operator fun setValue(t: Any?, p: KProperty<*>, v: Boolean) {
        AppSettings.sp.edit().putBoolean(key, v).apply(); AppSettings.version.intValue++
    }
}

class IntPref(private val key: String, private val def: Int) {
    operator fun getValue(t: Any?, p: KProperty<*>): Int {
        AppSettings.version.intValue
        return AppSettings.sp.getInt(key, def)
    }
    operator fun setValue(t: Any?, p: KProperty<*>, v: Int) {
        AppSettings.sp.edit().putInt(key, v).apply(); AppSettings.version.intValue++
    }
}

class StrPref(private val key: String, private val def: String) {
    operator fun getValue(t: Any?, p: KProperty<*>): String {
        AppSettings.version.intValue
        return AppSettings.sp.getString(key, def) ?: def
    }
    operator fun setValue(t: Any?, p: KProperty<*>, v: String) {
        AppSettings.sp.edit().putString(key, v).apply(); AppSettings.version.intValue++
    }
}
