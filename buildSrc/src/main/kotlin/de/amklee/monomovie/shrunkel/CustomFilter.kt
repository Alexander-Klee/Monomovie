package de.amklee.monomovie.shrunkel

import java.io.Reader
import java.io.StringReader
import org.apache.tools.ant.filters.BaseParamFilterReader

abstract class CustomFilter(`in`: Reader) : BaseParamFilterReader(`in`) {
    private val reader by lazy { StringReader(transform(super.readFully())) }
    override fun read(): Int = this.reader.read()
    abstract fun transform(input: String): String
}
