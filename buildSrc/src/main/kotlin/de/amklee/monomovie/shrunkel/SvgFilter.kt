package de.amklee.monomovie.shrunkel

import java.io.Reader

class SvgFilter(`in`: Reader) : CustomFilter(`in`) {
    val svgClose = "\\s*>\\s*".toRegex(RegexOption.MULTILINE)
    val svgMultispace = "\\s{2,}".toRegex(RegexOption.MULTILINE)
    override fun transform(input: String): String = input
        .replace(svgClose, ">")
        .replace(svgMultispace, " ")
}