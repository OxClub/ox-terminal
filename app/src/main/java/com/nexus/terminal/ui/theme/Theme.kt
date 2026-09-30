package com.nexus.terminal.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.nexus.terminal.data.AppSettings

data class TermTheme(
    val id: String, val name: String,
    val bg: Int, val fg: Int, val cursor: Int, val accent: Int,
    val dark: Boolean, val palette: List<Int>
)

object Themes {
    private fun c(v: Int) = v or 0xFF000000.toInt()

    private val darkPalette = listOf(
        0x21262D, 0xF85149, 0x3FB950, 0xD29922, 0x58A6FF, 0xBC8CFF, 0x39C5CF, 0xB1BAC4,
        0x6E7681, 0xFF7B72, 0x56D364, 0xE3B341, 0x79C0FF, 0xD2A8FF, 0x56D4DD, 0xF0F6FC
    ).map(::c)
    private val lightPalette = listOf(
        0x24292F, 0xCF222E, 0x116329, 0x9A6700, 0x0969DA, 0x8250DF, 0x1B7C83, 0x6E7781,
        0x57606A, 0xA40E26, 0x1A7F37, 0x7D4E00, 0x218BFF, 0xA475F9, 0x3192AA, 0x8C959F
    ).map(::c)
    private val neonPalette = listOf(
        0x1A0033, 0xFF2A6D, 0x05FFA1, 0xF9F871, 0x01CDFE, 0xB967FF, 0x00FFD5, 0xD1F7FF,
        0x5A3F8A, 0xFF6B9A, 0x7BFFC8, 0xFFFFA5, 0x7FE3FF, 0xD9A6FF, 0x7FFFE9, 0xFFFFFF
    ).map(::c)

    val builtin = listOf(
        TermTheme("nexus_dark", "Nexus Dark", c(0x0D1117), c(0xE6EDF3), c(0x58A6FF), c(0x58A6FF), true, darkPalette),
        TermTheme("light", "Light", c(0xFAFAFA), c(0x24292F), c(0x0969DA), c(0x0969DA), false, lightPalette),
        TermTheme("amoled", "AMOLED Black", c(0x000000), c(0xF0F0F0), c(0x00E5FF), c(0x00E5FF), true, darkPalette),
        TermTheme("classic", "Classic Green", c(0x000000), c(0x33FF33), c(0x33FF33), c(0x33FF33), true, darkPalette),
        TermTheme("cyberpunk", "Cyberpunk", c(0x120024), c(0x00FFD5), c(0xFF2A6D), c(0xFF2A6D), true, neonPalette),
        TermTheme("blue", "Blue Terminal", c(0x0A1F3D), c(0xD6E6FF), c(0x4FC3F7), c(0x4FC3F7), true, darkPalette)
    )

    fun parseHex(s: String): Int? {
        val t = s.trim().removePrefix("#")
        if (t.length != 6) return null
        return t.toIntOrNull(16)?.let { c(it) }
    }

    fun hex(color: Int) = String.format("#%06X", color and 0xFFFFFF)

    fun custom(bg: String, fg: String, cursor: String): TermTheme {
        val b = parseHex(bg) ?: c(0x0D1117)
        val f = parseHex(fg) ?: c(0xE6EDF3)
        val cu = parseHex(cursor) ?: c(0x58A6FF)
        val luminance = (0.299 * ((b shr 16) and 255) + 0.587 * ((b shr 8) and 255) + 0.114 * (b and 255)) / 255
        val dark = luminance < 0.5
        return TermTheme("custom", "Custom", b, f, cu, cu, dark, if (dark) darkPalette else lightPalette)
    }

    fun byId(id: String): TermTheme =
        if (id == "custom") custom(AppSettings.customBg, AppSettings.customFg, AppSettings.customCursor)
        else builtin.firstOrNull { it.id == id } ?: builtin.first()

    fun current(): TermTheme = byId(AppSettings.themeId)

    /** Keys understood by Termux's TerminalColorScheme.updateWith(). */
    fun toProperties(t: TermTheme): Map<String, String> {
        val m = LinkedHashMap<String, String>()
        m["background"] = hex(t.bg)
        m["foreground"] = hex(t.fg)
        m["cursor"] = hex(t.cursor)
        t.palette.forEachIndexed { i, col -> m["color$i"] = hex(col) }
        return m
    }
}

@Composable
fun NexusTheme(theme: TermTheme, content: @Composable () -> Unit) {
    val bg = Color(theme.bg)
    val fg = Color(theme.fg)
    val acc = Color(theme.accent)
    val onAcc = if (theme.dark) bg else Color.White
    val surface = lerp(bg, fg, 0.06f)
    val surfaceVar = lerp(bg, fg, 0.12f)
    val scheme = if (theme.dark) darkColorScheme(
        primary = acc, onPrimary = onAcc, primaryContainer = lerp(bg, acc, 0.3f), onPrimaryContainer = fg,
        secondaryContainer = lerp(bg, acc, 0.22f), onSecondaryContainer = fg,
        background = bg, onBackground = fg, surface = surface, onSurface = fg,
        surfaceVariant = surfaceVar, onSurfaceVariant = lerp(fg, bg, 0.3f),
        outline = lerp(bg, fg, 0.3f), error = Color(0xFFFF6B6B)
    ) else lightColorScheme(
        primary = acc, onPrimary = onAcc, primaryContainer = lerp(bg, acc, 0.2f), onPrimaryContainer = fg,
        secondaryContainer = lerp(bg, acc, 0.15f), onSecondaryContainer = fg,
        background = bg, onBackground = fg, surface = surface, onSurface = fg,
        surfaceVariant = surfaceVar, onSurfaceVariant = lerp(fg, bg, 0.3f),
        outline = lerp(bg, fg, 0.3f), error = Color(0xFFB3261E)
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(),
        shapes = Shapes(
            small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(20.dp), extraLarge = RoundedCornerShape(28.dp)
        ),
        content = content
    )
}
