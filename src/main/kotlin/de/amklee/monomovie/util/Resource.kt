package de.amklee.monomovie.util

import de.amklee.monomovie.Environment
import java.io.FileNotFoundException
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.reflect.KProperty

class Resource(private val name: String) {
    private val resource: String by lazy {
        Resource::class.java.getResource("/$name")?.readText()
            ?: throw FileNotFoundException("$name not found in resources")
    }
    private val resourceFile = Path("src/main/resources/$name").takeIf {
        Environment.isDevelopment &&
            it.exists()
    }

    override fun toString(): String = resourceFile?.readText() ?: resource

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String = toString()
}
