package de.amklee.monomovie.components

import de.amklee.monomovie.CachedMovies
import de.amklee.monomovie.util.Resources
import kotlinx.html.FlowContent
import kotlinx.html.script
import kotlinx.html.ul
import kotlinx.html.unsafe

fun FlowContent.BasicMovieList(moview: List<CachedMovies.Movie>) {
    script {
        unsafe { +Resources.bookmarkJs }
    }
    ul(classes = "movie-list") {
        for (movie in moview) {
            MovieListItem(movie)
        }
    }
}

fun FlowContent.SelectableMovieList(moview: List<CachedMovies.Movie>) {
    script {
        unsafe {
            +Resources.bookmarkJs
            +Resources.selectableJs
        }
    }
    ul(classes = "movie-list") {
        for (movie in moview) {
            SelectableMovieListItem(movie)
        }
    }
}
