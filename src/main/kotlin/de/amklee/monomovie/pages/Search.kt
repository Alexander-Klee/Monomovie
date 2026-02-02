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
                autoFocus = true
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

suspend fun FlowContent.SearchPage(title: String) {
    SearchBar(title)
    SearchMovieList()
}

@Serializable
data class MoreSearchResultsResponse(
    val cursor: String,
    val html: Map<String, String>,
    val hasNextPage: Boolean
)

suspend fun MoreSearchResults(searchResults: CachedMovies.SearchResults): MoreSearchResultsResponse {
    if (searchResults.movies.isEmpty()) return MoreSearchResultsResponse("", mapOf(), false)

    return MoreSearchResultsResponse(
        searchResults.pageInfo.endCursor,
        searchResults.movies.associate { (it.mediaEntry.id ?: "null") to buildULHtml { MovieListItem(it) } },
        searchResults.pageInfo.hasNextPage
    )
}