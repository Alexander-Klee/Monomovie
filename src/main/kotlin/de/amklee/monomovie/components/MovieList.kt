package de.amklee.monomovie.components

import de.amklee.monomovie.CachedMovies
import de.amklee.monomovie.R
import de.amklee.monomovie.db.WatchedDB
import de.amklee.monomovie.pages.RouletteCachedMovie
import de.amklee.monomovie.util.selectableJs
import de.amklee.monomovie.util.sseJs
import java.time.format.DateTimeFormatter
import kotlinx.html.*

@HtmlTagMarker
inline fun FlowContent.MovieList(script: String, body: UL.() -> Unit) {
    script {
        unsafe {
            +script
        }
    }
    ul(classes = "movie-list") {
        id = "movie-list"
        body()
    }
}

@HtmlTagMarker
suspend fun FlowContent.SearchMovieList() = MovieList(
    R.imageErrorJs + R.bookmarkJs + R.watchedJs + R.sseJs(Mode.SEARCH) + R.infiniteScrollJs,
) {
    id = "infinite-list"
    MovieListSentinel()
}

private val dateFormatter = DateTimeFormatter.ofPattern("dd. MMMM yyyy")

@HtmlTagMarker
suspend fun FlowContent.WatchedMovieList(movies: List<WatchedDB.WatchedItem>) = MovieList(
    R.imageErrorJs + R.bookmarkJs + R.watchedJs + R.sseJs(Mode.WATCHED),
) {
    for (
    (date, movies) in movies
        .groupBy { it.watchedAt.toLocalDate() }
        .entries
        .sortedByDescending { it.key }
    ) {
        li(classes = "watched-date") {
            +date.format(dateFormatter)
        }
        for (movie in movies) {
            MovieListItem(movie.item)
        }
    }
}

@HtmlTagMarker
suspend fun FlowContent.SelectableMovieList(movies: List<CachedMovies.Movie>, minSelection: Int = 2) = MovieList(
    R.imageErrorJs + R.bookmarkJs + R.watchedJs + R.selectableJs(minSelection) +
        R.sseJs(Mode.OVERVIEW),
) {
    for (movie in movies) {
        SelectableMovieListItem(movie)
    }
}

@HtmlTagMarker
suspend fun FlowContent.RouletteMovieList(movies: Collection<RouletteCachedMovie>) = MovieList(
    R.imageErrorJs + R.watchedJs + R.sseJs(Mode.ROULETTE),
) {
    for (movie in movies) {
        RouletteMovieListItem(movie.movie, movie.votes)
    }
}
