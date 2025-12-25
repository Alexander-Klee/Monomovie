package de.amklee.monomovie.pages

import de.amklee.monomovie.CachedMovies
import de.amklee.monomovie.components.SelectableMovieList
import kotlinx.html.*

suspend fun FlowContent.BookmarkPage(movies: List<CachedMovies.Movie>) {
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
