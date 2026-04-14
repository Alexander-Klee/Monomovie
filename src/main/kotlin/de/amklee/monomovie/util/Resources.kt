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
    // styles
    val style by Resource("style.css")

    // scripts
    val libJs by Resource("lib.js")
    val topbuttonJs by Resource("topbutton.js")
    val bookmarkJs by Resource("bookmark.js")
    val watchedJs by Resource("watched.js")
    private val selectableJs by Resource("selectable.js")
    val imageErrorJs by Resource("image-error.js")
    val infiniteScrollJs by Resource("infinite-scroll.js")
    private val sseJs by Resource("sse.js")
    private val rouletteSharedJs by Resource("roulette-shared.js")

    fun sseJs(mode: Mode) = sseJs.replace($$"$mode$", mode.toString())
    fun selectableJs(minSelection: Int) = selectableJs.replace($$"$minSelection$", minSelection.toString())
    @OptIn(ExperimentalUuidApi::class)
    fun rouletteSharedJs(isSelection: Boolean, shareId: Uuid) = rouletteSharedJs
        .replace($$"$isSelection$", isSelection.toString())
        .replace($$"$shareId$", shareId.toString())

    // svgs
    val menuSvg by Resource("graphics/svg/menu.svg")
    val arrowUpSvg by Resource("graphics/svg/arrowup.svg")
    val searchSvg by Resource("graphics/svg/search.svg")
    val eyeTemplateSvg by Resource("graphics/svgTemplates/eye-template.svg")
    val eyePlusTemplateSvg by Resource("graphics/svgTemplates/eye-plus-template.svg")
    val bookmarkTemplateSvg by Resource("graphics/svgTemplates/bookmark-template.svg")
    val bookmarkSquareTemplateSvg by Resource("graphics/svgTemplates/bookmark-square-template.svg")
    val bookmarkPlusTemplateSvg by Resource("graphics/svgTemplates/bookmark-plus-template.svg")
    val rottenTomatoesSvg by Resource("graphics/svgTemplates/rotten-tomatoes-template.svg")
    val tmdbSvg by Resource("graphics/svgTemplates/tmdb-template.svg")
    val imdbSvg by Resource("graphics/svgTemplates/imdb-template.svg")
    val imagePlaceholderSvg by Resource("graphics/svgTemplates/image-placeholder-template.svg")

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
