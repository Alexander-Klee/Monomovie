package de.amklee.monomovie.util

import java.io.FileNotFoundException
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.reflect.KProperty

object Resources {
    val style by Resource("style.css")
    val menuSvg by Resource("graphics/menu.svg")
    val menuJs by Resource("menu.js")
    val bookmarkJs by Resource("bookmark.js")
    val watchedJs by Resource("watched.js")
    val selectableJs by Resource("selectable.js")
    val searchSvg by Resource("graphics/search.svg")
    val infiniteScrollJs by Resource("infinite-scroll.js")

    private class Resource(private val name: String) {
        private val resource: String by lazy {
            Resources::class.java.getResource("/$name")?.readText()
                ?: throw FileNotFoundException("$name not found in resources")
        }
        private val resourceFile = Path("src/main/resources/$name").takeIf { it.exists() }

        override fun toString(): String = resourceFile?.readText() ?: resource
        operator fun getValue(thisRef: Any?, property: KProperty<*>): String = toString()
    }
}