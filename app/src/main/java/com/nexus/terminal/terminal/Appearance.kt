package com.nexus.terminal.terminal

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import com.nexus.terminal.data.AppSettings
import com.nexus.terminal.ui.theme.Themes
import com.termux.terminal.TerminalColors
import com.termux.view.TerminalView
import java.io.File
import java.util.Properties

object Fonts {
    val options = listOf("monospace" to "Default monospace", "serif_monospace" to "Serif monospace", "custom" to "Custom font file")

    fun resolve(ctx: Context): Typeface = when (AppSettings.fontFamily) {
        "serif_monospace" -> Typeface.create("serif-monospace", Typeface.NORMAL)
        "custom" -> AppSettings.customFontPath.takeIf { File(it).isFile }
            ?.let { runCatching { Typeface.createFromFile(it) }.getOrNull() } ?: Typeface.MONOSPACE
        else -> Typeface.MONOSPACE
    }
}

object Appearance {
    private var appliedColors = ""

    fun apply(tv: TerminalView) {
        val ctx = tv.context
        val theme = Themes.current()
        val px = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, AppSettings.fontSize.coerceIn(8, 40).toFloat(), ctx.resources.displayMetrics
        ).toInt()
        val key = "$px|${AppSettings.fontFamily}|${AppSettings.customFontPath}"
        if (tv.tag != key) {
            tv.tag = key
            tv.setTextSize(px)
            tv.setTypeface(Fonts.resolve(ctx))
        }
        tv.setBackgroundColor(theme.bg)
        applyColors(theme.id + Themes.toProperties(theme).values.joinToString(","), theme)
        if (AppSettings.cursorBlink) {
            tv.setTerminalCursorBlinkerRate(600)
            tv.setTerminalCursorBlinkerState(true, true)
        } else {
            tv.setTerminalCursorBlinkerState(false, false)
        }
        tv.onScreenUpdated()
    }

    private fun applyColors(key: String, theme: com.nexus.terminal.ui.theme.TermTheme) {
        if (key == appliedColors) return
        appliedColors = key
        val p = Properties()
        Themes.toProperties(theme).forEach { (k, v) -> p.setProperty(k, v) }
        TerminalColors.COLOR_SCHEME.updateWith(p)
        Sessions.list.forEach { it.session.emulator?.mColors?.reset() }
    }
}
