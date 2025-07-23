package de.amklee.monomovie.pages

import de.amklee.monomovie.CachedMovies
import de.amklee.monomovie.SearchTitles
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

fun FlowContent.SearchPage(title: String, searchResults: SearchTitles?) {
    if (searchResults == null) {
        SearchBar("")
        p { +"Please enter a title to search for." }
        return
    }
    val mediaEntries = searchResults.edges.map {
        CachedMovies.Movie(it.node, isBookmarked = false, isWatched = false, cacheDate = System.currentTimeMillis())
    }

    SearchBar(title)

    script {
        unsafe {
            +Resources.infiniteScrollJs.replace($$"$endCursor$", searchResults.pageInfo.endCursor)
        }
    }

    if (searchResults.edges.isEmpty()) {
        p { +"No Search Results" }
    } else {
        h4 { +"Search Results:" }
        SearchMovieList(mediaEntries)
    }
}

@Serializable
data class MoreSearchResultsResponse(
    val cursor: String,
    val html: String,
    val hasNextPage: Boolean
)

fun MoreSearchResults(searchResults: SearchTitles?): MoreSearchResultsResponse {
    if (searchResults == null || searchResults.edges.isEmpty()) {
        return MoreSearchResultsResponse("", "", false)
    }

    val mediaEntries = searchResults.edges.map {
        CachedMovies.Movie(it.node, isBookmarked = false, isWatched = false, cacheDate = System.currentTimeMillis())
    }
    return MoreSearchResultsResponse(
        searchResults.pageInfo.endCursor,
        buildULHtml {
            for (movie in mediaEntries) {
                MovieListItem(movie)
            }
        },
        searchResults.pageInfo.hasNextPage
    )
}