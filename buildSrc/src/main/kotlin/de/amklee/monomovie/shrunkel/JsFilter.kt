package de.amklee.monomovie.shrunkel

import com.google.javascript.jscomp.CompilationLevel
import com.google.javascript.jscomp.Compiler
import com.google.javascript.jscomp.CompilerOptions
import com.google.javascript.jscomp.SourceFile
import java.io.Reader


class JsFilter(`in`: Reader) : CustomFilter(`in`) {
    var sourceName: String = ""

    val lf = "\r?\n".toRegex()
    override fun transform(input: String): String {
        val sourceName = parameters?.first { it.type == "sourceName" }?.value ?: sourceName

        val compiler = Compiler(System.err)

        val options = CompilerOptions()
        CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options)

        val extern = SourceFile.fromCode("externs.js", "")
        val input = SourceFile.fromCode(sourceName, input.replace(lf, "\r\n"))
        val res = compiler.compile(extern, input, options)

        if (!res.success) throw IllegalArgumentException(res.errors.joinToString("\n"))

        return compiler.toSource()
    }
}