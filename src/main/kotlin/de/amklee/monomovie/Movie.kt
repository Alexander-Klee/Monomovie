package de.amklee.monomovie

import kotlinx.serialization.json.Json
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText


object CachedMovies {
    data class Movie(val mediaEntry: MediaEntry, var isBookmarked: Boolean, val cacheDate: Long)

    private val cache = mutableMapOf<String, Movie>()

    private val bookmarksFile = Path("bookmarks.json")
    private var bookmarks: MutableSet<String> = loadBookmarks()


    // Load bookmarks from file at startup
    private fun loadBookmarks(): MutableSet<String> {
        return if (bookmarksFile.exists()) {
            try {
                Json.decodeFromString<Set<String>>(bookmarksFile.readText()).toMutableSet()
            } catch (_: Exception) {
                mutableSetOf()
            }
        } else {
            mutableSetOf()
        }
    }

    private fun saveBookmarks() {
        bookmarksFile.writeText(Json.encodeToString(bookmarks))
    }

    suspend fun get(id: String): Movie? {
        // TODO: invalidate cache entry, if older than ... (remember to keep isBookmarked state)
        return cache[id] ?: run {
            val jw = JustWatch(country = "DE", language = "en")
            val details = jw.details(id)
            if (details != null) {
                val movie = Movie(mediaEntry = details, isBookmarked = id in bookmarks, cacheDate = System.currentTimeMillis())
                cache[id] = movie
                return@run movie
            }
            null
        }
    }

    fun toggleBookmark(id: String): Movie? {
        val movie = cache[id] ?: return null
        movie.isBookmarked = !movie.isBookmarked
        if (movie.isBookmarked) {
            bookmarks.add(id)
        } else {
            bookmarks.remove(id)
        }
        saveBookmarks()
        return movie
    }

    suspend fun search(title: String, numResults: Int = 4): List<Movie> {
        val jw = JustWatch(country = "DE", language = "en")
        val searchResults = jw.search(title = title, numResults)
        return searchResults.map { mediaEntry ->
            val isBookmarked = cache[mediaEntry.id]?.isBookmarked ?: false

            val movie = Movie(mediaEntry = mediaEntry, isBookmarked = isBookmarked, cacheDate = System.currentTimeMillis())
            if (mediaEntry.id != null) cache[mediaEntry.id] = movie

            movie
        }
    }

    suspend fun getBookmarkedMovies(): List<Movie> = bookmarks.mapNotNull { id -> get(id) }
}