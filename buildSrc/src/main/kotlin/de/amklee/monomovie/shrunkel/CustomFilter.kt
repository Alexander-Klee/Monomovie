package de.amklee.monomovie.shrunkel

import org.apache.tools.ant.filters.BaseParamFilterReader
import java.io.Reader
import java.io.StringReader

abstract class CustomFilter(`in`: Reader) : BaseParamFilterReader(`in`) {
    private val reader by lazy { StringReader(transform(super.readFully())) }
    override fun read(): Int = this.reader.read()
    abstract fun transform(input: String): String
}
