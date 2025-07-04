package de.amklee.monomovie


object CachedMovies {
    data class Movie(val mediaEntry: MediaEntry, var isBookmarked: Boolean, val cacheDate: Long)

    private val cache = mutableMapOf<String, Movie>()
    private val justWatch = JustWatch(country = "DE", language = "en")

    suspend fun get(id: String): Movie? {
        // TODO: invalidate cache entry, if older than ... (remember to keep isBookmarked state)
        return cache[id] ?: run {
            val details = justWatch.details(id)
            if (details != null) {
                val movie = Movie(mediaEntry = details, isBookmarked = id in BookmarksDB, cacheDate = System.currentTimeMillis())
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
            BookmarksDB.addBookmark(id)
        } else {
            BookmarksDB.removeBookmark(id)
        }
        return movie
    }

    suspend fun search(title: String, numResults: Int = 4): List<Movie> {
        val searchResults = justWatch.search(title = title, numResults)
        val searchResults = justWatch.search(title = title, numResults).edges.map { it.node }
        return searchResults.map { mediaEntry ->
            // populate cache
            val isBookmarked = cache[mediaEntry.id]?.isBookmarked ?: false

            val movie = Movie(mediaEntry = mediaEntry, isBookmarked = isBookmarked, cacheDate = System.currentTimeMillis())
            if (mediaEntry.id != null) cache[mediaEntry.id] = movie

            movie
        }
    }

    suspend fun cursorSearch(title: String, cursor: String? = null, numResults: Int = 4): SearchTitles
        = justWatch.search(title = title, cursor = cursor, count = numResults)

    suspend fun getBookmarkedMovies(): List<Movie> = BookmarksDB.getBookmarks().mapNotNull { id -> get(id) }
}