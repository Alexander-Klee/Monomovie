package de.amklee.monomovie.components

import de.amklee.monomovie.CachedMovies
import de.amklee.monomovie.CachedMovies.getOffers
import kotlinx.html.*

private fun FlowContent.WatchedButton(movie: CachedMovies.Movie) {
    if (movie.mediaEntry.id == null) return

    span(classes = "watched-button${if (movie.isWatched) " watched" else ""}") {
        onClick = "watch('${movie.mediaEntry.id}', this)"
        i(classes = "watched-icon rating-logo")
    }
}

private fun FlowContent.Offers(movie: CachedMovies.Movie) {
    ul(classes = "offer-list") {
        for (offer in movie.getOffers()) {
            li(classes = "offer-item") {
                a(href = offer.standardWebURL ?: "", classes = "offer-link") {
                    img(
                        src = "https://images.justwatch.com${offer.`package`?.icon}",
                        alt = offer.`package`?.clearName ?: "Unknown",
                        classes = "offer-icon"
                    )
                }
            }
        }
    }
}

private fun FlowContent.SimpleLink(target: String?, content: FlowContent.() -> Unit) {
    if (target.isNullOrBlank()) return
    a(href = target, target = "_blank", classes = "no-link-style") {
        rel = "noopener noreferrer"
        content()
    }
}

private fun FlowContent.Ratings(movie: CachedMovies.Movie) {
    fun formatScore(score: Float): String = String.format("%.1f", score)

    movie.mediaEntry.content?.scoring?.tomatoMeter?.let { score ->
        div(classes = "movie-rating") {
            i(classes = "tomato-icon rating-logo")
            p { +"$score%" }
        }
    }

    val imdbLink = movie.mediaEntry.content?.externalIds?.imdbId?.let { id -> "https://www.imdb.com/title/$id" }
    movie.mediaEntry.content?.scoring?.imdbScore?.let { score ->
        SimpleLink(imdbLink) {
            div(classes = "movie-rating") {
                i(classes = "imdb-icon rating-logo")
                p { +formatScore(score) }
            }
        }
    }

    val tmdbLinkType = when (movie.mediaEntry.objectType?.lowercase()) {
        "movie" -> "movie"
        "show" -> "tv"
        else -> "movie" // Default
    }
    val tmdbLink = movie.mediaEntry.content?.externalIds?.tmdbId?.let { id -> "https://www.themoviedb.org/$tmdbLinkType/$id" }
    movie.mediaEntry.content?.scoring?.tmdbScore?.let { score ->
        SimpleLink(tmdbLink) {
            div(classes = "movie-rating") {
                i(classes = "tmdb-icon rating-logo")
                p { +formatScore(score) }
            }
        }
    }
}

private fun FlowContent.MovieItem(movie: CachedMovies.Movie) {
    val movieId = movie.mediaEntry.id
    val movieTitle = movie.mediaEntry.content?.title ?: "Unknown Title"
    val posterUrl = movie.mediaEntry.content?.posterUrl?.let {
        "https://images.justwatch.com$it"
    }

    div(classes = "movie-item bookmark-container") {
        span(classes = "movie-poster" + if (movie.isBookmarked) " bookmarked" else "") {
            onClick = "return bookmark('$movieId', this, false)"
            onDoubleClick = "return bookmark('$movieId', this, true)"
            span(classes = "bookmark-icon")
            img(classes = "movie-poster", src = posterUrl, alt = movieTitle)
        }

        div(classes = "movie-details") {
            div(classes = "movie-title-bar") {
                p(classes = "movie-title") { +movieTitle }
                div(classes = "movie-rating-container") {
                    WatchedButton(movie)
                    Ratings(movie)
                }
            }
            p(classes = "movie-year") {
                +(movie.mediaEntry.content?.originalReleaseYear?.toString() ?: "Unknown Year")
            }
            p(classes = "movie-short-description") {
                onClick = "this.classList.add('expanded')"
                +(movie.mediaEntry.content?.shortDescription ?: "No description available.")
            }
        }

        div(classes = "movie-offers") {
            Offers(movie)
        }
    }
}

fun UL.MovieListItem(movie: CachedMovies.Movie) {
    li(classes = "movie-list-item") {
        MovieItem(movie)
    }
}

fun UL.SelectableMovieListItem(movie: CachedMovies.Movie) {
    li(classes = "movie-list-item") {
        label {
            htmlFor = movie.mediaEntry.id ?: ""
            checkBoxInput(classes = "movie-checkbox", name = "selected[]") {
                value = movie.mediaEntry.id ?: ""
                id = movie.mediaEntry.id ?: ""
                onClick = "selectedChanged()"
            }
            MovieItem(movie)
        }
    }
}

fun UL.RouletteMovieListItem(movie: CachedMovies.Movie) {
    li(classes = "movie-list-item") {
        label {
            htmlFor = movie.mediaEntry.id ?: ""
            numberInput(classes = "roulette-weight", name = movie.mediaEntry.id) {
                id = movie.mediaEntry.id ?: ""
                min = "1"
                value = "1"
            }
            MovieItem(movie)
        }
    }
}
