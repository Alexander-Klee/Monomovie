package de.amklee.monomovie.components

import de.amklee.monomovie.CachedMovies
import de.amklee.monomovie.db.WatchedDB
import de.amklee.monomovie.util.Resources
import kotlinx.html.*
import java.time.format.DateTimeFormatter

inline fun FlowContent.MovieList(script: String, crossinline body: UL.() -> Unit) {
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

fun FlowContent.SearchMovieList(movies: List<CachedMovies.Movie>) = MovieList(
    Resources.bookmarkJs + Resources.watchedJs
) {
    id = "infinite-list"
    for (movie in movies) {
        MovieListItem(movie)
    }
}

private val dateFormatter = DateTimeFormatter.ofPattern("dd. MMMM yyyy")
fun FlowContent.WatchedMovieList(movies: List<WatchedDB.WatchedItem>) = MovieList(
    Resources.bookmarkJs + Resources.watchedJs
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

fun FlowContent.SelectableMovieList(movies: List<CachedMovies.Movie>) = MovieList(
    Resources.bookmarkJs + Resources.watchedJs + Resources.selectableJs
) {
    for (movie in movies) {
        SelectableMovieListItem(movie)
    }
}

fun FlowContent.RouletteMovieList(movies: List<CachedMovies.Movie>) = MovieList(
    Resources.watchedJs + Resources.rouletteWeigthJs
) {
    for (movie in movies) {
        RouletteMovieListItem(movie)
    }
}
