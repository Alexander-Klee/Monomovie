package de.amklee.monomovie.util

import kotlinx.html.FlowContent
import kotlinx.html.svg
import kotlinx.html.unsafe


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

fun FlowContent.templatedSvg(href: String, classes: String = "") {
    svg(classes = classes) {
        unsafe {
            +"<use href=\"$href\"></use>"
        }
    }
}

fun FlowContent.BookmarkIconSvg(classes: String = "bookmark-icon") = templatedSvg("#bookmark-icon", classes = classes)
fun FlowContent.BookmarkPlusIconSvg(classes: String = "bookmark-icon") = templatedSvg("#bookmark-plus-icon", classes = classes)
fun FlowContent.EyeIconSvg(classes: String = "") = templatedSvg("#eye-icon", classes = classes)
fun FlowContent.EyePlusIconSvg(classes: String = "") = templatedSvg("#eye-plus-icon", classes = classes)
fun FlowContent.RottenTomatoesSvg(classes: String = "rating-logo") = templatedSvg("#rotten-tomatoes-icon", classes = classes)
fun FlowContent.TmdbSvg(classes: String = "rating-logo tmdb-icon") = templatedSvg("#tmdb-icon", classes = classes)
fun FlowContent.ImdbSvg(classes: String = "rating-logo") = templatedSvg("#imdb-icon", classes = classes)
