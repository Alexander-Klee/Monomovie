package de.amklee.monomovie.pages

import de.amklee.monomovie.CachedMovies
import de.amklee.monomovie.components.SelectableMovieList
import kotlinx.html.FlowContent
import kotlinx.html.h1
import kotlinx.html.p
import kotlinx.html.postForm
import kotlinx.html.submitInput

suspend fun FlowContent.HomePage(movies: List<CachedMovies.Movie>) {
    SearchBar("")
    h1 { +"Bookmarked Movies:" }
    if (movies.isEmpty()) {
        p { +"No bookmarked movies found" }
        return
    }
    postForm(action = "/roulette", classes = "roulette-form") {
        submitInput(classes = "roulette-button") {
            disabled = true
            value = "Roulette"
        }
        SelectableMovieList(movies)
    }
}
