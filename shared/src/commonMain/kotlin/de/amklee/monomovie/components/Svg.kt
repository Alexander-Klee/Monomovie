package de.amklee.monomovie.components

import de.amklee.monomovie.R
import de.amklee.monomovie.util.Resource
import kotlinx.html.FlowContent
import kotlinx.html.HtmlTagMarker
import kotlinx.html.IMG
import kotlinx.html.img

@HtmlTagMarker
inline fun FlowContent.Svg(resource: Resource, classes: String? = null, block: IMG.() -> Unit = {}) {
    img(src = "/${resource.path}", alt = "", classes = classes, block = block)
}

@HtmlTagMarker
inline fun FlowContent.BookmarkIconSvg(
    classes: String = "bookmark-icon",
    block: IMG.() -> Unit = {
    },
) = Svg(R.graphics.svg.bookmarkSvg, classes = classes, block = block)

@HtmlTagMarker
inline fun FlowContent.BookmarkSquareIconSvg(
    classes: String = "bookmark-icon",
    block: IMG.() -> Unit = {
    },
) = Svg(R.graphics.svg.bookmarkSquareSvg, classes = classes, block = block)

@HtmlTagMarker
inline fun FlowContent.BookmarkPlusIconSvg(
    classes: String = "bookmark-icon",
    block: IMG.() -> Unit = {
    },
) = Svg(R.graphics.svg.bookmarkPlusSvg, classes = classes, block = block)

@HtmlTagMarker
inline fun FlowContent.EyeIconSvg(
    classes: String = "",
    block: IMG.() -> Unit = {
    },
) = Svg(R.graphics.svg.eyeSvg, classes = classes, block = block)

@HtmlTagMarker
inline fun FlowContent.EyePlusIconSvg(
    classes: String = "",
    block: IMG.() -> Unit = {
    },
) = Svg(R.graphics.svg.eyePlusSvg, classes = classes, block = block)

@HtmlTagMarker
inline fun FlowContent.RottenTomatoesSvg(
    classes: String = "rating-logo",
    block: IMG.() -> Unit = {
    },
) = Svg(R.graphics.svg.rottenTomatoesSvg, classes = classes, block = block)

@HtmlTagMarker
inline fun FlowContent.TmdbSvg(
    classes: String = "rating-logo tmdb-icon",
    block: IMG.() -> Unit = {
    },
) = Svg(R.graphics.svg.tmdbSvg, classes = classes, block = block)

@HtmlTagMarker
inline fun FlowContent.ImdbSvg(
    classes: String = "rating-logo",
    block: IMG.() -> Unit = {
    },
) = Svg(R.graphics.svg.imdbSvg, classes = classes, block = block)

@HtmlTagMarker
inline fun FlowContent.ImagePlaceholderSvg(
    classes: String = "image-placeholder",
    block: IMG.() -> Unit = {
    },
) = Svg(R.graphics.svg.imagePlaceholderSvg, classes = classes, block = block)

@HtmlTagMarker
inline fun FlowContent.ArrowUpSvg(
    classes: String = "arrow-up",
    block: IMG.() -> Unit = {
    },
) = Svg(R.graphics.svg.arrowUpSvg, classes = classes, block = block)
