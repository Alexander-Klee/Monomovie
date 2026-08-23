package de.amklee.monomovie.components

import de.amklee.monomovie.*
import de.amklee.monomovie.CachedMovies.getOffers
import de.amklee.monomovie.util.*
import kotlinx.html.*
import kotlinx.html.impl.dataset


private fun UL.OfferItem(offer: Offer) = OfferItem(
    offer.standardWebURL ?: "",
    "https://images.justwatch.com${offer.`package`?.icon}",
    offer.`package`?.clearName ?: "Unknown Title"
)

private fun UL.OfferItem(offerUrl: String, iconUrl: String, offerName: String) {
    li(classes = "offer-item") {
        a(href = offerUrl, classes = "offer-link") {
            title = offerName

            img(
                src = iconUrl,
                alt = offerName,
                classes = "offer-icon"
            )
        }
    }
}

fun UL.JellyfinOfferItem(jellyfinLink: String) =
    OfferItem(
        jellyfinLink,
        JellyfinClient.getLogoLink(),
        "Jellyfin"
    )

fun FlowContent.OfferList(offers: List<Offer>, jellyfinLink: String? = null, extraElements: FlowContent.() -> Unit = {}) {
    ul(classes = "offer-list") {
        // Offers
        jellyfinLink?.let { JellyfinOfferItem(it) }
        for (offer in offers) OfferItem(offer)

        extraElements()
    }
}

private fun FlowContent.MoreOffersButton(movie: CachedMovies.Movie) {
    val movieId = movie.mediaEntry.id ?: return
    val offersLink = "/offers/$movieId"

    SimpleLink(offersLink, classes = "more-offers-link") {
        +"More Offers"
    }
}

private fun FlowContent.SimpleLink(target: String?, classes: String = "no-link-style", content: FlowContent.() -> Unit) {
    if (target.isNullOrBlank()) return
    a(href = target, classes = classes) { content() }
}

private fun FlowContent.SimpleLinkNewTab(target: String?, classes: String = "no-link-style", content: FlowContent.() -> Unit) {
    if (target.isNullOrBlank()) return
    a(href = target, target = "_blank", classes = classes) {
        rel = "noopener noreferrer"
        content()
    }
}

private fun FlowContent.Ratings(movie: CachedMovies.Movie) {
    fun formatScore(score: Float): String = String.format("%.1f", score)

    movie.mediaEntry.content?.scoring?.tomatoMeter?.let { score ->
        div(classes = "movie-rating") {
            RottenTomatoesSvg()
            p { +"$score%" }
        }
    }

    val imdbLink = movie.mediaEntry.imdbLink
    movie.mediaEntry.content?.scoring?.imdbScore?.let { score ->
        SimpleLinkNewTab(imdbLink) {
            div(classes = "movie-rating") {
                ImdbSvg()
                p { +formatScore(score) }
            }
        }
    }

    val tmdbLink = movie.mediaEntry.tmdbLink
    movie.mediaEntry.content?.scoring?.tmdbScore?.let { score ->
        SimpleLinkNewTab(tmdbLink) {
            div(classes = "movie-rating") {
                TmdbSvg()
                p { +formatScore(score) }
            }
        }
    }
}

fun formatTime(minutes: Int): String {
    val m = minutes % 60
    val h = minutes / 60

    return if (h > 0) {String.format("%dh %dm", h, m)} else String.format("%dm", m)
}

fun FlowContent.YearDurationInfo(movie: CachedMovies.Movie) {
    val releaseYear = movie.mediaEntry.content?.originalReleaseYear
    val runtime = movie.mediaEntry.content?.runtime?.takeIf { it != 0 }

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
suspend fun FlowContent.MovieItem(movie: CachedMovies.Movie, showOffers: Boolean = true, extraElements: FlowContent.() -> Unit = {}) {
    val movieId = movie.mediaEntry.id
    val movieTitle = movie.mediaEntry.title
    val posterUrl = movie.mediaEntry.fullPosterUrl

    div(classes = "movie-item bookmark-container") {
        id = "movie-item-$movieId"
        BookmarkSquareIconSvg("bookmark-icon") {
            id = "bookmark-$movieId"
            dataset["checked"] = movie.isBookmarked.toString()
        }

        span(classes = "movie-poster") {
            if (posterUrl.isNullOrBlank()) dataset["error"] = true.toString()
            div(classes = "movie-poster-placeholder") {
                ImagePlaceholderSvg()
            }
            if (posterUrl.isNullOrBlank()) return@span
            img(classes = "movie-poster-img", src = posterUrl, alt = movieTitle) {
                onError = "handleImageError(this)"
            }
        }
        div(classes = "movie-action-container hidden-movie-action-bar-element") {
            div(classes = "movie-action-bar hidden-movie-action-bar-element") {
                button(type = ButtonType.button, classes = "hidden-movie-action-bar-element eye-button watched-scope") {
                    id = "watched-$movieId"
                    onClick = "dataset.checked == 'true' ? deleteWatch('$movieId') : setWatch('$movieId')"

                    dataset["checked"] = movie.isWatched.toString()

                    EyeIconSvg("in-watched")
                    EyePlusIconSvg("in-not-watched")
                }
                button(type = ButtonType.button, classes = "hidden-movie-action-bar-element eye-button bookmark-scope") {
                    id = "bookmark-$movieId"
                    onClick = "dataset.checked == 'true' ? deleteBookmark('$movieId') : setBookmark('$movieId')"

                    dataset["checked"] = movie.isBookmarked.toString()

                    BookmarkIconSvg("in-bookmarked")
                    BookmarkPlusIconSvg("in-not-bookmarked")
                }
            }
        }

        div(classes = "movie-details") {
            div(classes = "movie-title-bar") {
                p(classes = "movie-title") { +movieTitle }
                div(classes = "movie-rating-container") {
                    Ratings(movie)
                }
            }

            YearDurationInfo(movie)

            p(classes = "movie-short-description") {
                onClick = "dataset.expanded = true"
                +(movie.mediaEntry.content?.shortDescription ?: "No description available.")
            }
        }

        val jellyfinLink = CachedMovies.getJellyfinLink(movie)

        if (showOffers) {
            div(classes = "movie-offers") {
                OfferList(movie.getOffers(), jellyfinLink) {
                    div(classes = "more-offers-box") {
                        MoreOffersButton(movie)
                    }
                }
            }
        }
        extraElements()
    }
}

suspend fun UL.MovieListItem(movie: CachedMovies.Movie) {
    li(classes = "movie-list-item") {
        MovieItem(movie)
    }
}

@OptIn(ExperimentalKotlinxHtmlApi::class)
suspend fun UL.MovieListSentinel() {
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

suspend fun UL.SelectableMovieListItem(movie: CachedMovies.Movie) {
    li(classes = "movie-list-item") {
        val name = movie.mediaEntry.id ?: ""
        label {
            htmlFor = name
            checkBoxInput(classes = "movie-checkbox", name = "selected[]") {
                value = name
                id = name
                onClick = "selectedChanged()"
            }
            MovieItem(movie)
        }
    }
}

@OptIn(ExperimentalKotlinxHtmlApi::class)
suspend fun UL.RouletteMovieListItem(movie: CachedMovies.Movie, count: Int = 1) {
    li(classes = "movie-list-item") {
        id = "roulette-${movie.mediaEntry.id}"
        label {
            htmlFor = movie.mediaEntry.id ?: ""
            MovieItem(movie) {
                div("roulette-weight-container") {
                    button(type = ButtonType.button, classes = "roulette-weight-button roulette-weight-decrease") {
                        dataset["movie"] = movie.mediaEntry.id ?: ""
                        onClick = "const el = document.getElementById(dataset.movie); el.stepDown(); el.dispatchEvent(new Event('change'));"
                        +"−"
                    }
                    numberInput(name = movie.mediaEntry.id, classes = "roulette-weight-input") {
                        id = movie.mediaEntry.id ?: ""
                        min = "0"
                        value = count.toString()
                    }
                    button(type = ButtonType.button, classes = "roulette-weight-button roulette-weight-increase") {
                        dataset["movie"] = movie.mediaEntry.id ?: ""
                        onClick = "const el = document.getElementById(dataset.movie); el.stepUp(); el.dispatchEvent(new Event('change'));"
                        +"+"
                    }
                }
            }
        }
    }
}
