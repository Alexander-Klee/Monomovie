package de.amklee.monomovie.components

import de.amklee.monomovie.util.custom
import kotlinx.html.FlowContent
import kotlinx.html.HtmlTagMarker
import kotlinx.html.SVG
import kotlinx.html.svg

inline fun FlowContent.templatedSvg(
    href: String,
    classes: String = "",
    block: SVG.() -> Unit = {
    },
) {
    svg(classes = classes) {
        block()
        custom("use", namespace = "http://www.w3.org/2000/svg") {
            attributes["href"] = href
        }
    }
}

@HtmlTagMarker
inline fun FlowContent.BookmarkIconSvg(
    classes: String = "bookmark-icon",
    block: SVG.() -> Unit = {
    },
) = templatedSvg("#bookmark-icon", classes = classes, block = block)

@HtmlTagMarker
inline fun FlowContent.BookmarkSquareIconSvg(
    classes: String = "bookmark-icon",
    block: SVG.() -> Unit = {
    },
) = templatedSvg("#bookmark-square-icon", classes = classes, block = block)

@HtmlTagMarker
inline fun FlowContent.BookmarkPlusIconSvg(
    classes: String = "bookmark-icon",
    block: SVG.() -> Unit = {
    },
) = templatedSvg("#bookmark-plus-icon", classes = classes, block = block)

@HtmlTagMarker
inline fun FlowContent.EyeIconSvg(
    classes: String = "",
    block: SVG.() -> Unit = {
    },
) = templatedSvg("#eye-icon", classes = classes, block = block)

@HtmlTagMarker
inline fun FlowContent.EyePlusIconSvg(
    classes: String = "",
    block: SVG.() -> Unit = {
    },
) = templatedSvg("#eye-plus-icon", classes = classes, block = block)

@HtmlTagMarker
inline fun FlowContent.RottenTomatoesSvg(
    classes: String = "rating-logo",
    block: SVG.() -> Unit = {
    },
) = templatedSvg("#rotten-tomatoes-icon", classes = classes, block = block)

@HtmlTagMarker
inline fun FlowContent.TmdbSvg(
    classes: String = "rating-logo tmdb-icon",
    block: SVG.() -> Unit = {
    },
) = templatedSvg("#tmdb-icon", classes = classes, block = block)

@HtmlTagMarker
inline fun FlowContent.ImdbSvg(
    classes: String = "rating-logo",
    block: SVG.() -> Unit = {
    },
) = templatedSvg("#imdb-icon", classes = classes, block = block)

@HtmlTagMarker
inline fun FlowContent.ImagePlaceholderSvg(
    classes: String = "image-placeholder",
    block: SVG.() -> Unit = {
    },
) = templatedSvg("#image-placeholder-icon", classes = classes, block = block)

@HtmlTagMarker
inline fun FlowContent.ArrowUpSvg(
    classes: String = "arrow-up",
    block: SVG.() -> Unit = {
    },
) = templatedSvg("#arrow-up-icon", classes = classes, block = block)
