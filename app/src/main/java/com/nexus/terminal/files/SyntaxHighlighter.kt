package com.nexus.terminal.files

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

data class SyntaxColors(val keyword: Color, val string: Color, val comment: Color, val number: Color)

object Syntax {
    private val cLike = setOf("java", "kt", "kts", "c", "h", "cpp", "hpp", "cc", "js", "ts", "php", "css", "go", "rs")
    private val hashLike = setOf("py", "sh", "yaml", "yml", "rb", "toml", "conf", "properties", "bashrc")

    private val keywords: Map<String, Set<String>> = mapOf(
        "kotlin" to "fun val var class object interface if else when for while return import package null true false is in as override private public internal data sealed companion try catch throw".split(" ").toSet(),
        "java" to "class interface public private protected static final void int long double boolean new return if else for while import package null true false extends implements try catch throw this super".split(" ").toSet(),
        "c" to "int long char float double void struct enum typedef return if else for while switch case break continue const static include define sizeof NULL".split(" ").toSet(),
        "js" to "function const let var return if else for while class import export from new this null undefined true false async await try catch throw typeof interface type".split(" ").toSet(),
        "php" to "function class public private protected static return if else foreach for while echo new namespace use null true false try catch throw".split(" ").toSet(),
        "py" to "def class return if elif else for while import from as with try except finally raise None True False lambda pass in not and or is yield async await".split(" ").toSet(),
        "sh" to "if then else elif fi for while do done case esac function in echo export local return exit alias source set unset".split(" ").toSet(),
        "yaml" to "true false null yes no".split(" ").toSet(),
        "css" to "important".split(" ").toSet(),
        "go" to "func var const type struct interface return if else for range import package go defer chan map nil true false".split(" ").toSet(),
        "rs" to "fn let mut struct enum impl trait pub use mod return if else match for while loop self Self true false".split(" ").toSet()
    )

    fun langOf(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "kt", "kts" -> "kotlin"
            "java" -> "java"
            "c", "h", "cpp", "hpp", "cc" -> "c"
            "js", "ts" -> "js"
            "php" -> "php"
            "py" -> "py"
            "sh", "bashrc", "profile" -> "sh"
            "yaml", "yml", "toml", "conf", "properties" -> "yaml"
            "css" -> "css"
            "go" -> "go"
            "rs" -> "rs"
            "xml", "html", "htm" -> "xml"
            "json" -> "json"
            "md" -> "md"
            else -> "text"
        }
    }

    private val cache = HashMap<String, Regex>()

    private fun regexFor(lang: String): Regex? = cache.getOrPut(lang) {
        val string = "\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])*'"
        val number = "\\b\\d+(?:\\.\\d+)?\\b"
        val comment = when (lang) {
            "py", "sh", "yaml" -> "#[^\\n]*"
            "xml" -> "<!--[\\s\\S]*?-->"
            "json", "md", "text" -> "(?!x)x"
            else -> "//[^\\n]*|/\\*[\\s\\S]*?\\*/"
        }
        val kw = when (lang) {
            "xml" -> "</?[A-Za-z][\\w:.-]*"
            else -> keywords[lang]?.joinToString("|", "\\b(?:", ")\\b") ?: "(?!x)x"
        }
        Regex("($comment)|($string)|($number)|($kw)")
    }.takeIf { lang != "text" && lang != "md" }

    fun highlight(text: String, lang: String, c: SyntaxColors): AnnotatedString {
        val re = regexFor(lang) ?: return AnnotatedString(text)
        return buildAnnotatedString {
            append(text)
            re.findAll(text).forEach { m ->
                val color = when {
                    m.groups[1] != null -> c.comment
                    m.groups[2] != null -> c.string
                    m.groups[3] != null -> c.number
                    else -> c.keyword
                }
                addStyle(SpanStyle(color = color), m.range.first, m.range.last + 1)
            }
        }
    }
}

class SyntaxTransformation(private val lang: String, private val colors: SyntaxColors) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val out = if (text.length > 150_000) text else Syntax.highlight(text.text, lang, colors)
        return TransformedText(out, OffsetMapping.Identity)
    }
}
