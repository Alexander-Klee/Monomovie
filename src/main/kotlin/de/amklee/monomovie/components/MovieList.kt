package de.amklee.monomovie.components

import de.amklee.monomovie.CachedMovies
import de.amklee.monomovie.util.Resources
import kotlinx.html.FlowContent
import kotlinx.html.script
import kotlinx.html.ul
import kotlinx.html.unsafe

fun FlowContent.BasicMovieList(movies: List<CachedMovies.Movie>) {
    script {
        unsafe { +Resources.bookmarkJs }
    }
    ul(classes = "movie-list") {
        for (movie in movies) {
            MovieListItem(movie)
        }
    }
}

fun FlowContent.SelectableMovieList(movies: List<CachedMovies.Movie>) {
    script {
        unsafe {
            +Resources.bookmarkJs
            +Resources.selectableJs
        }
    }
    ul(classes = "movie-list") {
        for (movie in movies) {
            SelectableMovieListItem(movie)
        }
    }
}
