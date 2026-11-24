package de.amklee.monomovie.pages

import de.amklee.monomovie.R
import de.amklee.monomovie.Route
import de.amklee.monomovie.components.SearchMovieList
import de.amklee.monomovie.components.invoke
import de.amklee.monomovie.dto.JellyfinItems
import de.amklee.monomovie.dto.MoreSearchResultsResponse
import de.amklee.monomovie.dto.MovieItemUI
import de.amklee.monomovie.service.CachedMovies
import de.amklee.monomovie.util.href
import io.ktor.resources.serialization.ResourcesFormat
import kotlinx.html.*

@HtmlTagMarker
context(resourcesFormat: ResourcesFormat)
fun FlowContent.SearchBar(title: String) {
    div(classes = "search-bar") {
        getForm(action = href(Route.Search())) {
            button(classes = "search-button", type = ButtonType.submit) {
                unsafe { +R.graphics.svg.searchSvg }
            }
            textInput(name = "title", classes = "search-input") {
                autoFocus = true
                placeholder = "Search for a movie…"
                value = title
            }
        }
    }
}

@HtmlTagMarker
context(resourcesFormat: ResourcesFormat)
fun FlowContent.EmptySearchPage() {
    SearchBar("")
    p { +"Please enter a title to search for." }
}

@HtmlTagMarker
context(resourcesFormat: ResourcesFormat)
fun FlowContent.SearchPage(title: String) {
    SearchBar(title)
    h4 {
        +"Search Results:"
    }
    SearchMovieList(title)
}

@HtmlTagMarker
context(jellyfinItems: JellyfinItems)
fun MoreSearchResults(searchResults: CachedMovies.SearchResults): MoreSearchResultsResponse {
    if (searchResults.movies.isEmpty()) return MoreSearchResultsResponse("", mapOf(), false)

    return MoreSearchResultsResponse(
        searchResults.pageInfo.endCursor,
        searchResults.movies.associate {
            (it.mediaEntry.id ?: "null") to MovieItemUI(it)
        },
        searchResults.pageInfo.hasNextPage,
    )
}
