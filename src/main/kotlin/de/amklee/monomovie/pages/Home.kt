package de.amklee.monomovie.pages

import de.amklee.monomovie.CachedMovies
import de.amklee.monomovie.components.SelectableMovieList
import kotlinx.html.FlowContent
import kotlinx.html.HtmlTagMarker
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.p
import kotlinx.html.postForm
import kotlinx.html.submitInput

@HtmlTagMarker
suspend fun FlowContent.HomePage(movies: List<CachedMovies.Movie>) {
    SearchBar("")
    h1 { +"Bookmarked Movies:" }
    if (movies.isEmpty()) {
        p { +"No bookmarked movies found" }
        return
    }
    postForm(action = "/roulette", classes = "roulette-form") {
        div(classes = "sticky-action-row") {
            submitInput(classes = "roulette-button require-min-selection") {
                disabled = true
                value = "Roulette"
            }
            submitInput(classes = "roulette-button") {
                value = "Shared Roulette"
                formAction = "/roulette/share"
            }
        }
        SelectableMovieList(movies)
    }
}
