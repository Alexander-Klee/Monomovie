package de.amklee.monomovie.components

import de.amklee.monomovie.dto.*
import de.amklee.monomovie.util.*
import kotlin.time.Duration
import kotlinx.html.*
import kotlinx.html.a
import kotlinx.html.img
import kotlinx.html.impl.dataset
import kotlinx.html.li
import kotlinx.html.title

@HtmlTagMarker
fun UL.OfferItem(offer: OfferItemUI) {
    val offerName = offer.name ?: "Unknown Title"
    li(classes = "offer-item") {
        a(href = offer.webUrl ?: "", classes = "offer-link") {
            title = offerName

            img(
                src = offer.iconUrl ?: "",
                alt = offerName,
                classes = "offer-icon",
            )
        }
    }
}

@HtmlTagMarker
fun FlowContent.OfferList(offers: List<OfferItemUI>, extraElements: FlowContent.() -> Unit = {}) {
    ul(classes = "offer-list") {
        for (offer in offers) OfferItem(offer)

        extraElements()
    }
}

@HtmlTagMarker
private fun FlowContent.MoreOffersButton(movie: MovieItemUI) {
    val movieId = movie.id ?: return
    val offersLink = "/offers/$movieId"

    SimpleLink(offersLink, classes = "more-offers-link") {
        +"More Offers"
    }
}

@HtmlTagMarker
private fun FlowContent.SimpleLink(target: String?, classes: String = "no-link-style", content: FlowContent.() -> Unit) {
    if (target.isNullOrBlank()) return
    a(href = target, classes = classes) { content() }
}

@HtmlTagMarker
private fun FlowContent.SimpleLinkNewTab(target: String?, classes: String = "no-link-style", content: FlowContent.() -> Unit) {
    if (target.isNullOrBlank()) return
    a(href = target, target = "_blank", classes = classes) {
        rel = "noopener noreferrer"
        content()
    }
}

@HtmlTagMarker
private fun FlowContent.Ratings(movie: MovieItemUI) {
    fun formatScore(score: Float): String = score.toString(precision = 1)

    movie.scoring?.tomatoMeter?.let { score ->
        div(classes = "movie-rating") {
            RottenTomatoesSvg()
            p { +"$score%" }
        }
    }

    val imdbLink = movie.imdbLink
    movie.scoring?.imdbScore?.let { score ->
        SimpleLinkNewTab(imdbLink) {
            div(classes = "movie-rating") {
                ImdbSvg()
                p { +formatScore(score) }
            }
        }
    }

    val tmdbLink = movie.tmdbLink
    movie.scoring?.tmdbScore?.let { score ->
        SimpleLinkNewTab(tmdbLink) {
            div(classes = "movie-rating") {
                TmdbSvg()
                p { +formatScore(score) }
            }
        }
    }
}

fun formatTime(duration: Duration): String {
    val minutes = duration.inWholeMinutes
    val m = minutes % 60
    val h = minutes / 60

    return if (h > 0) {
        "${h}h ${m}m"
    } else {
        "${m}m"
    }
}

@HtmlTagMarker
fun FlowContent.YearDurationInfo(movie: MovieItemUI) {
    val releaseYear = movie.originalReleaseYear
    val runtime = movie.runtime

    if (releaseYear != null && runtime != null) {
        p(classes = "movie-info-line") {
            +releaseYear.toString()
            +" • "
            +formatTime(runtime)
        }
    } else if (releaseYear != null) {
        p(classes = "movie-info-line") {
            +releaseYear.toString()
        }
    } else if (runtime != null) {
        p(classes = "movie-info-line") {
            +runtime.toString()
        }
    }
}

@OptIn(ExperimentalKotlinxHtmlApi::class)
@HtmlTagMarker
fun FlowContent.MovieItem(movie: MovieItemUI, showOffers: Boolean = true, extraElements: FlowContent.() -> Unit = {}) {
    div(classes = "movie-item bookmark-container card") {
        id = "movie-item-${movie.id}"
        BookmarkSquareIconSvg("bookmark-icon") {
            id = "bookmark-${movie.id}"
            dataset["checked"] = movie.isBookmarked.toString()
        }

        span(classes = "movie-poster") {
            if (movie.posterUrl.isNullOrBlank()) dataset["error"] = true.toString()
            div(classes = "movie-poster-placeholder") {
                ImagePlaceholderSvg()
            }
            if (movie.posterUrl.isNullOrBlank()) return@span
            img(classes = "movie-poster-img img-fallback-placeholder", src = movie.posterUrl, alt = "") {
            }
        }
        div(classes = "movie-action-container hidden-movie-action-bar-element") {
            div(classes = "movie-action-bar hidden-movie-action-bar-element") {
                CustomToggleButton(
                    classes = "hidden-movie-action-bar-element eye-button",
                    initialState = movie.isWatched,
                    checkedBody = { EyeIconSvg() },
                    checkAction = JsCall.SetWatched(movie.id!!, false),
                    uncheckedBody = { EyePlusIconSvg() },
                    uncheckAction = JsCall.SetWatched(movie.id, true),
                ) {
                    id = "watched-${movie.id}"
                }
                CustomToggleButton(
                    classes = "hidden-movie-action-bar-element eye-button",
                    initialState = movie.isBookmarked,
                    checkedBody = { BookmarkIconSvg(classes = "") },
                    checkAction = JsCall.SetBookmarked(movie.id, false),
                    uncheckedBody = { BookmarkPlusIconSvg(classes = "") },
                    uncheckAction = JsCall.SetBookmarked(movie.id, true),
                ) {
                    id = "bookmark-${movie.id}"
                }
            }
        }

        div(classes = "movie-details") {
            div(classes = "movie-title-bar") {
                p(classes = "movie-title") { +movie.title }
                div(classes = "movie-rating-container") {
                    Ratings(movie)
                }
            }

            YearDurationInfo(movie)

            p(classes = "movie-short-description") {
                onClick = "dataset.expanded = true"
                +(movie.shortDescription ?: "No description available.")
            }
        }

        if (showOffers) {
            div(classes = "movie-offers") {
                OfferList(movie.offers) {
                    div(classes = "more-offers-box") {
                        MoreOffersButton(movie)
                    }
                }
            }
        }
        extraElements()
    }
}

@HtmlTagMarker
fun UL.MovieListItem(movie: MovieItemUI) {
    li(classes = "movie-list-item") {
        MovieItem(movie)
    }
}

@OptIn(ExperimentalKotlinxHtmlApi::class)
@HtmlTagMarker
fun UL.MovieListSentinel() {
    li(classes = "movie-list-item") {
        id = "infinite-sentinel"
        div(classes = "movie-item bookmark-container") {
            span(classes = "movie-poster") {
                dataset["error"] = true.toString()
                div(classes = "movie-poster-placeholder") {
                    ImagePlaceholderSvg()
                }
            }

            div(classes = "movie-details") {
                div(classes = "movie-title-bar") {
                    p(classes = "movie-title") { +"Loading" }
                }

                p(classes = "movie-short-description") {
                    onClick = "dataset.expanded = true"
                    +"Loading..."
                }
            }
        }
    }
}

@HtmlTagMarker
fun UL.SelectableMovieListItem(movie: MovieItemUI) {
    li(classes = "movie-list-item") {
        val name = movie.id ?: ""
        label {
            htmlFor = name
            checkBoxInput(classes = "movie-checkbox", name = "selected[]") {
                value = name
                id = name
                onClick = JsCall.UpdateMovieSelectionCount.toJsString()
            }
            MovieItem(movie)
        }
    }
}

@OptIn(ExperimentalKotlinxHtmlApi::class)
@HtmlTagMarker
fun UL.RouletteMovieListItem(movie: MovieItemUI, count: Int = 1) {
    li(classes = "movie-list-item") {
        id = "roulette-${movie.id}"
        label {
            htmlFor = movie.id ?: ""
            MovieItem(movie) {
                div("roulette-weight-container") {
                    button(
                        type = ButtonType.button,
                        classes = "roulette-weight-button roulette-weight-decrease",
                    ) {
                        dataset["movie"] = movie.id ?: ""
                        onClick = "const el = document.getElementById(dataset.movie); el.stepDown(); el.dispatchEvent(new Event('change'));"
                        +"−"
                    }
                    numberInput(name = movie.id, classes = "roulette-weight-input") {
                        id = movie.id ?: ""
                        min = "0"
                        value = count.toString()
                    }
                    button(
                        type = ButtonType.button,
                        classes = "roulette-weight-button roulette-weight-increase",
                    ) {
                        dataset["movie"] = movie.id ?: ""
                        onClick = "const el = document.getElementById(dataset.movie); el.stepUp(); el.dispatchEvent(new Event('change'));"
                        +"+"
                    }
                }
            }
        }
    }
}
