package de.amklee.monomovie.components

import de.amklee.monomovie.CachedMovies
import de.amklee.monomovie.db.WatchedDB
import de.amklee.monomovie.util.Resources
import kotlinx.html.*
import java.time.format.DateTimeFormatter

fun FlowContent.BasicMovieList(movies: List<CachedMovies.Movie>) {
    script {
        unsafe {
            +Resources.bookmarkJs
            +Resources.watchedJs
        }
    }
    ul(classes = "movie-list") {
        for (movie in movies) {
            MovieListItem(movie)
        }
    }
}

fun FlowContent.SearchMovieList(movies: List<CachedMovies.Movie>) {
    script {
        unsafe {
            +Resources.bookmarkJs
            +Resources.watchedJs
        }
    }
    ul(classes = "movie-list") {
        attributes["id"] = "infinite-list"
        for (movie in movies) {
            MovieListItem(movie)
        }
    }
}

private val dateFormatter = DateTimeFormatter.ofPattern("dd. MMMM yyyy")
fun FlowContent.WatchedMovieList(movies: List<WatchedDB.WatchedItem>) {
    script {
        unsafe {
            +Resources.bookmarkJs
            +Resources.watchedJs
        }
    }
    ul(classes = "movie-list") {
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
}

fun FlowContent.SelectableMovieList(movies: List<CachedMovies.Movie>) {
    script {
        unsafe {
            +Resources.bookmarkJs
            +Resources.watchedJs
            +Resources.selectableJs
        }
    }
    ul(classes = "movie-list") {
        for (movie in movies) {
            SelectableMovieListItem(movie)
        }
    }
}
