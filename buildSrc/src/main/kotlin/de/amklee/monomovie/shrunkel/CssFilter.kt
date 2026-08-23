package de.amklee.monomovie.shrunkel

import java.io.Reader

class CssFilter(`in`: Reader) : CustomFilter(`in`) {
    val cssWhitespaceRegex = "\\s*\n\\s*".toRegex(RegexOption.MULTILINE)
    val cssOpenRegex = "\\s*\\{\\s*".toRegex(RegexOption.MULTILINE)
    val cssColonRegex = "\\s*:\\s*".toRegex(RegexOption.MULTILINE)
    override fun transform(input: String): String = input
        .replace(cssWhitespaceRegex, "")
        .replace(cssOpenRegex, "{")
        .replace(cssColonRegex, ":")
}