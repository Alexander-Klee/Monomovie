package de.amklee.monomovie.pages

import de.amklee.monomovie.CachedMovies
import de.amklee.monomovie.components.RouletteMovieList
import kotlinx.html.FlowContent
import kotlinx.html.postForm
import kotlinx.html.submitInput

suspend fun FlowContent.RoulettePage(movies: List<CachedMovies.Movie>) {
    postForm("/roulette/submit") {
        RouletteMovieList(movies)
        submitInput(classes = "roulette-button") {
            value = "Start Roulette"
        }
    }
}