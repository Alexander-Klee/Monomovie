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

    fun setBookmark(id: String): Movie? {
        val movie = cache[id] ?: return null
        if (!movie.isBookmarked) {
            movie.isBookmarked = true
            BookmarksDB.addBookmark(id)
        }
        return movie
    }

    fun deleteBookmark(id: String): Movie? {
        val movie = cache[id] ?: return null
        if (movie.isBookmarked) {
            movie.isBookmarked = false
            BookmarksDB.removeBookmark(id)
        }
        return movie
    }

    suspend fun search(title: String, cursor: String? = null, numResults: Int = 4): SearchTitles
        = justWatch.search(title = title, cursor = cursor, count = numResults)

    suspend fun getBookmarkedMovies(): List<Movie> = BookmarksDB.getBookmarks().mapNotNull { id -> get(id) }
}