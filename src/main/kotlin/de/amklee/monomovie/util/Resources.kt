package de.amklee.monomovie.util

import de.amklee.monomovie.components.Mode
import java.io.FileNotFoundException
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.reflect.KProperty
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object Resources {
    val style by Resource("style.css")
    val menuSvg by Resource("graphics/menu.svg")
    val arrowUpSvg by Resource("graphics/arrowup.svg")
    val topbuttonJs by Resource("topbutton.js")
    val bookmarkJs by Resource("bookmark.js")
    val watchedJs by Resource("watched.js")
    private val selectableJs by Resource("selectable.js")
    val searchSvg by Resource("graphics/search.svg")
    private val infiniteScrollJs by Resource("infinite-scroll.js")
    private val sseJs by Resource("sse.js")
    private val rouletteSharedJs by Resource("roulette-shared.js")

    fun infiniteScrollJs(endCursor: String) = infiniteScrollJs.replace($$"$endCursor$", endCursor)
    fun sseJs(mode: Mode) = sseJs.replace($$"$mode$", mode.toString())
    fun selectableJs(minSelection: Int) = selectableJs.replace($$"$minSelection$", minSelection.toString())
    @OptIn(ExperimentalUuidApi::class)
    fun rouletteSharedJs(isSelection: Boolean, shareId: Uuid) = rouletteSharedJs
        .replace($$"$isSelection$", isSelection.toString())
        .replace($$"$shareId$", shareId.toString())

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
