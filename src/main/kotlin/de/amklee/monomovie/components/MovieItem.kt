package de.amklee.monomovie.components

import de.amklee.monomovie.CachedMovies
import de.amklee.monomovie.CachedMovies.getOffers
import de.amklee.monomovie.Offer
import de.amklee.monomovie.util.BookmarkIconSvg
import de.amklee.monomovie.util.BookmarkPlusIconSvg
import de.amklee.monomovie.util.EyeIconSvg
import de.amklee.monomovie.util.EyePlusIconSvg
import de.amklee.monomovie.util.ImdbSvg
import de.amklee.monomovie.util.RottenTomatoesSvg
import de.amklee.monomovie.util.TmdbSvg
import de.amklee.monomovie.fullPosterUrl
import de.amklee.monomovie.imdbLink
import de.amklee.monomovie.title
import de.amklee.monomovie.tmdbLink
import de.amklee.monomovie.util.BookmarkSquareIconSvg
import kotlinx.html.*


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

fun FlowContent.OfferList(offers: List<Offer>, jellyfinLink: String? = null, extraElements: FlowContent.() -> Unit = {}) {
    ul(classes = "offer-list") {
        // Add the jellyfin offer if available
        if (jellyfinLink != null) {
            OfferItem(
                jellyfinLink,
                "https://jellyfin.amklee.de/web/f5bbb798cb2c65908633.png",
                "Jellyfin"
            )
        }

        for (offer in offers) {
            OfferItem(offer)
        }

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

suspend fun FlowContent.MovieItem(movie: CachedMovies.Movie, showOffers: Boolean = true, extraElements: FlowContent.() -> Unit = {}) {
    val movieId = movie.mediaEntry.id
    val movieTitle = movie.mediaEntry.title
    val posterUrl = movie.mediaEntry.fullPosterUrl

    div(classes = "movie-item bookmark-container") {
        val bookmarkedClass = if (movie.isBookmarked) "bookmarked" else ""
        val watchedClass = if (movie.isWatched) "watched" else ""
        BookmarkSquareIconSvg("bookmark-icon $bookmarkedClass") {
            id = "bookmark-$movieId"
        }

        span(classes = "movie-poster") {
            img(classes = "movie-poster", src = posterUrl, alt = movieTitle)
        }
        div(classes = "movie-action-container hidden-movie-action-bar-element") {
            div(classes = "movie-action-bar hidden-movie-action-bar-element") {
                button(type = ButtonType.button, classes = "hidden-movie-action-bar-element eye-button $watchedClass") {
                    id = "watched-${movie.mediaEntry.id}"
                    onClick = "watch('${movie.mediaEntry.id}', this)"

                    EyeIconSvg("in-watched")
                    EyePlusIconSvg("in-not-watched")
                }
                button(type = ButtonType.button, classes = "hidden-movie-action-bar-element eye-button $bookmarkedClass") {
                    id = "bookmark-$movieId"
                    onClick = "bookmark('$movieId', this)"

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
                onClick = "this.classList.add('expanded')"
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

suspend fun UL.RouletteMovieListItem(movie: CachedMovies.Movie) {
    li(classes = "movie-list-item") {
        label {
            htmlFor = movie.mediaEntry.id ?: ""
            MovieItem(movie) {
                div("roulette-weight-container") {
                    button(type = ButtonType.button, classes = "roulette-weight-button") {
                        attributes["mmv_for"] = movie.mediaEntry.id ?: ""
                        onClick = "document.getElementById(attributes.mmv_for.value).stepDown();"
                        +"−"
                    }
                    numberInput(name = movie.mediaEntry.id, classes = "roulette-weight-input") {
                        id = movie.mediaEntry.id ?: ""
                        min = "1"
                        value = "1"
                    }
                    button(type = ButtonType.button, classes = "roulette-weight-button") {
                        attributes["mmv_for"] = movie.mediaEntry.id ?: ""
                        onClick = "document.getElementById(attributes.mmv_for.value).stepUp();"
                        +"+"
                    }
                }
            }
        }
    }
}
