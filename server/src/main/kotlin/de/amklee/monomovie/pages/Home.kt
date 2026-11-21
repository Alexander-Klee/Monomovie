package de.amklee.monomovie.pages

import de.amklee.monomovie.components.RouletteSelectionButton
import de.amklee.monomovie.components.SelectableMovieList
import de.amklee.monomovie.dto.JellyfinItems
import de.amklee.monomovie.dto.Movie
import kotlinx.html.*

@HtmlTagMarker
context(jellyfinItems: JellyfinItems)
fun FlowContent.HomePage(movies: List<Movie>) {
    SearchBar("")
    h1 { +"Bookmarked Movies:" }
    if (movies.isEmpty()) {
        p { +"No bookmarked movies found" }
        return
    }
    postForm(action = "/roulette", classes = "roulette-form") {
        div(classes = "sticky-action-row") {
            RouletteSelectionButton()
            submitInput(classes = "roulette-button") {
                value = "Shared Roulette"
                formAction = "/roulette/share"
            }
        }
        SelectableMovieList(movies)
    }
}
