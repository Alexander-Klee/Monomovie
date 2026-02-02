package de.amklee.monomovie.components

import de.amklee.monomovie.CachedMovies
import de.amklee.monomovie.db.WatchedDB
import de.amklee.monomovie.util.Resources
import kotlinx.html.*
import java.time.format.DateTimeFormatter

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

suspend fun FlowContent.SearchMovieList() = MovieList(
    Resources.imageErrorJs + Resources.bookmarkJs + Resources.watchedJs + Resources.sseJs(Mode.SEARCH)
) {
    id = "infinite-list"
    div {
        id = "infinite-sentinel"
        style = "width: 100%; height: 1px;"
    }
}

private val dateFormatter = DateTimeFormatter.ofPattern("dd. MMMM yyyy")
suspend fun FlowContent.WatchedMovieList(movies: List<WatchedDB.WatchedItem>) = MovieList(
    Resources.imageErrorJs + Resources.bookmarkJs + Resources.watchedJs + Resources.sseJs(Mode.WATCHED)
) {
    for ((date, movies) in movies
        .groupBy { it.watchedAt.toLocalDate() }
        .entries.sortedByDescending { it.key }
    ) {
        li(classes = "watched-date") {
            +date.format(dateFormatter)
        }
        for (movie in movies) {
            MovieListItem(movie.item)
        }
    }
}

suspend fun FlowContent.SelectableMovieList(movies: List<CachedMovies.Movie>) = MovieList(
    Resources.imageErrorJs + Resources.bookmarkJs + Resources.watchedJs + Resources.selectableJs + Resources.sseJs(Mode.OVERVIEW)
) {
    for (movie in movies) {
        SelectableMovieListItem(movie)
    }
}

suspend fun FlowContent.RouletteMovieList(movies: List<CachedMovies.Movie>) = MovieList(
    Resources.imageErrorJs + Resources.watchedJs + Resources.sseJs(Mode.ROULETTE)
) {
    for (movie in movies) {
        RouletteMovieListItem(movie)
    }
}
