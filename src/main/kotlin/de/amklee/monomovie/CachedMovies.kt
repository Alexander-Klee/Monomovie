package de.amklee.monomovie

import de.amklee.monomovie.db.BookmarksDB
import de.amklee.monomovie.db.WatchedDB

object CachedMovies {
    data class Movie(val mediaEntry: MediaEntry, var isBookmarked: Boolean, var isWatched: Boolean, val cacheDate: Long)
    fun Movie.getOffers() = mediaEntry.offers?.filter { it.monetizationType !in bannedTypes } ?: emptyList()

    private val cache = mutableMapOf<String, Movie>()
    private val justWatch = JustWatch(country = "DE", language = "en")
    private val bannedTypes = setOf("BUY", "RENT")

    suspend fun get(id: String): Movie? {
        // TODO: invalidate cache entry, if older than ... (remember to keep isBookmarked state)
        return cache[id] ?: run {
            val details = justWatch.details(id)
            if (details != null) {
                val movie = Movie(
                    mediaEntry = details,
                    isBookmarked = BookmarksDB.isBookmarked(id),
                    isWatched = id in WatchedDB,
                    cacheDate = System.currentTimeMillis())
                cache[id] = movie
                return@run movie
            }
            null
        }
    }

    suspend fun setBookmark(id: String): Movie? {
        val movie = get(id) ?: return null
        if (!movie.isBookmarked) {
            movie.isBookmarked = true
            BookmarksDB.addBookmark(id)
        }
        return movie
    }

    suspend fun deleteBookmark(id: String): Movie? {
        val movie = get(id) ?: return null
        if (movie.isBookmarked) {
            movie.isBookmarked = false
            BookmarksDB.removeBookmark(id)
        }
        return movie
    }

    suspend fun setWatch(id: String) {
        val movie = get(id) ?: return
        if (movie.isWatched) {
            // TODO maybe increment watch count or unwatch
            return
        }
        movie.isWatched = true
        WatchedDB.setWatch(id)
    }

    suspend fun deleteWatch(id: String) {
        val movie = get(id) ?: return
        if (!movie.isWatched) return
        movie.isWatched = false
        WatchedDB.deleteWatch(id)
    }

    suspend fun search(title: String?, cursor: String? = null, numResults: Int = 4): SearchTitles?
        = if (title.isNullOrBlank()) null else justWatch.search(title = title, cursor = cursor, count = numResults)

    suspend inline fun getWatchedMovies() = WatchedDB.getWatched()
    suspend inline fun getBookmarkedMovies(displayHidden: Boolean) = (
            if (displayHidden) BookmarksDB.getAllBookmarks().map { it.first }
            else BookmarksDB.getBookmarks()
        ).mapNotNull { id -> get(id) }
}
