package de.amklee.monomovie

import de.amklee.monomovie.db.BookmarksDB
import de.amklee.monomovie.db.WatchedDB
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object CachedMovies {

    @Serializable
    data class Movie(
        val mediaEntry: MediaEntry,
        var isBookmarked: Boolean,
        var isWatched: Boolean,
        val cacheDate: Long
    )

    fun Movie.getOffers() = mediaEntry.offers?.filter { it.monetizationType !in bannedTypes } ?: emptyList()

    private var _cache: MutableMap<String, Movie>? = null
    private val cacheFile = Path("cached_movies.json")

    val cache: MutableMap<String, Movie>
        get() {
            if (_cache == null) {
                _cache = loadCache()
            }
            return _cache!!
        }

    private fun loadCache(): MutableMap<String, Movie> {
        return if (cacheFile.exists()) {
            try {
                val jsonString = cacheFile.readText()
                Json.decodeFromString<Map<String, Movie>>(jsonString).toMutableMap()
            } catch (e: Exception) {
                // TODO: log error
                mutableMapOf()
            }
        } else {
            mutableMapOf()
        }
    }

    private fun saveCache() {
        if (_cache != null) {
            val jsonString = Json.encodeToString(_cache!!)
            cacheFile.writeText(jsonString)
        }
    }

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
                saveCache()
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
    suspend inline fun getBookmarkedMovies(
        displayHidden: Boolean,
        displayWatched: Boolean
    ) = BookmarksDB.getBookmarks()
            .filter { displayHidden || it.isBookmarked }
            .mapNotNull { get(it.id) }
            .filter { displayWatched || !it.isWatched }
}
