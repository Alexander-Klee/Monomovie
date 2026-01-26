package de.amklee.monomovie.pages

import de.amklee.monomovie.CachedMovies
import de.amklee.monomovie.components.MovieListItem
import de.amklee.monomovie.components.SearchMovieList
import de.amklee.monomovie.util.Resources
import de.amklee.monomovie.util.buildULHtml
import kotlinx.html.*
import kotlinx.serialization.Serializable

fun FlowContent.SearchBar(title: String) {
    div(classes = "search-bar") {
        getForm(action = "/search") {
            button(classes = "search-button", type = ButtonType.submit) {
                unsafe { +Resources.searchSvg }
            }
            textInput(name = "title", classes = "search-input") {
                placeholder = "Search for a movie…"
                value = title
            }
        }
    }
}

fun FlowContent.EmptySearchPage() {
    SearchBar("")
    p { +"Please enter a title to search for." }
}

suspend fun FlowContent.SearchPage(title: String, searchResults: CachedMovies.SearchResults) {
    SearchBar(title)

    script {
        unsafe {
            +Resources.infiniteScrollJs(searchResults.pageInfo.endCursor)
        }
    }

    if (searchResults.movies.isEmpty()) {
        p { +"No Search Results" }
    } else {
        h4 { +"Search Results:" }
        SearchMovieList(searchResults.movies)
    }
}

@Serializable
data class MoreSearchResultsResponse(
    val cursor: String,
    val html: String,
    val hasNextPage: Boolean
)

suspend fun MoreSearchResults(searchResults: CachedMovies.SearchResults): MoreSearchResultsResponse {
    if (searchResults.movies.isEmpty()) return MoreSearchResultsResponse("", "", false)

    return MoreSearchResultsResponse(
        searchResults.pageInfo.endCursor,
        buildULHtml {
            for (movie in searchResults.movies) {
                MovieListItem(movie)
            }
        },
        searchResults.pageInfo.hasNextPage
    )
}