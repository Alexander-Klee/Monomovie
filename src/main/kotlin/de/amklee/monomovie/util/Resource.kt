package de.amklee.monomovie.util

import de.amklee.monomovie.Environment
import java.io.FileNotFoundException
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.reflect.KProperty

class Resource(val path: String) {
    private val resource: String by lazy {
        Resource::class.java.getResource("/$path")?.readText()
            ?: throw FileNotFoundException("$path not found in resources")
    }
    private val resourceFile = Path("src/main/resources/$path").takeIf {
        Environment.isDevelopment &&
            it.exists()
    }

    override fun toString(): String = resourceFile?.readText() ?: resource

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String = toString()
}
