package de.amklee.monomovie.pages

import de.amklee.monomovie.CachedMovies
import kotlinx.html.FlowContent

suspend fun FlowContent.HomePage(bookmarkedMovies: List<CachedMovies.Movie>) {
    SearchBar("")
    BookmarkPage(bookmarkedMovies)
}