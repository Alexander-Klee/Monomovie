package de.amklee.monomovie.pages

import de.amklee.monomovie.CachedMovies
import de.amklee.monomovie.components.RouletteMovieListItem
import kotlinx.html.FlowContent
import kotlinx.html.postForm
import kotlinx.html.submitInput
import kotlinx.html.ul

fun FlowContent.RoulettePage(movies: List<CachedMovies.Movie>) {
    postForm("/roulette/submit") {
        ul(classes = "roulette-list") {
            for (movie in movies) {
                RouletteMovieListItem(movie)
            }
        }
        submitInput(classes = "roulette-button") {
            value = "Start Roulette"
        }
    }
}