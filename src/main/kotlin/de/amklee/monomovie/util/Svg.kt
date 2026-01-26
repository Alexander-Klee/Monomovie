package de.amklee.monomovie.util

import kotlinx.html.FlowContent
import kotlinx.html.SVG
import kotlinx.html.svg

fun FlowContent.IncludeSvgTemplates() {
    consumer.onTagContentUnsafe {
        +Resources.eyeTemplateSvg
        +Resources.eyePlusTemplateSvg
        +Resources.bookmarkTemplateSvg
        +Resources.bookmarkPlusTemplateSvg
        +Resources.rottenTomatoesSvg
        +Resources.tmdbSvg
        +Resources.imdbSvg
    }
}

inline fun FlowContent.templatedSvg(href: String, classes: String = "", block: SVG.() -> Unit = {}) {
    svg(classes = classes) {
        block()
        custom("use") {
            attributes["href"] = href
        }
    }
}

inline fun FlowContent.BookmarkIconSvg(classes: String = "bookmark-icon", block: SVG.() -> Unit = {}) = templatedSvg("#bookmark-icon", classes = classes, block = block)
inline fun FlowContent.BookmarkPlusIconSvg(classes: String = "bookmark-icon", block: SVG.() -> Unit = {}) = templatedSvg("#bookmark-plus-icon", classes = classes, block = block)
inline fun FlowContent.EyeIconSvg(classes: String = "", block: SVG.() -> Unit = {}) = templatedSvg("#eye-icon", classes = classes, block = block)
inline fun FlowContent.EyePlusIconSvg(classes: String = "", block: SVG.() -> Unit = {}) = templatedSvg("#eye-plus-icon", classes = classes, block = block)
inline fun FlowContent.RottenTomatoesSvg(classes: String = "rating-logo", block: SVG.() -> Unit = {}) = templatedSvg("#rotten-tomatoes-icon", classes = classes, block = block)
inline fun FlowContent.TmdbSvg(classes: String = "rating-logo tmdb-icon", block: SVG.() -> Unit = {}) = templatedSvg("#tmdb-icon", classes = classes, block = block)
inline fun FlowContent.ImdbSvg(classes: String = "rating-logo", block: SVG.() -> Unit = {}) = templatedSvg("#imdb-icon", classes = classes, block = block)
