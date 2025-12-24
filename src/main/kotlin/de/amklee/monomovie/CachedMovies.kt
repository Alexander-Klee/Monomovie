package de.amklee.monomovie

import de.amklee.monomovie.db.BookmarksDB
import de.amklee.monomovie.db.WatchedDB
import de.amklee.monomovie.pages.MonetizationTypes
import de.amklee.monomovie.util.error
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.math.sqrt

object CachedMovies {
    private val log = System.getLogger("MMV/CachedMovies")

    @Serializable
    data class Movie(
        val mediaEntry: MediaEntry,
        var isBookmarked: Boolean,
        var isWatched: Boolean,
        val cacheDate: Long
    )

    fun Movie.getOffers() = mediaEntry.offers?.filter {
        it.monetizationType.equals(MonetizationTypes.FREE.name, ignoreCase = true)
                || it.monetizationType.equals(MonetizationTypes.FLATRATE.name, ignoreCase = true)
    } ?: emptyList()

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
                log.error(e) { "Unable to parse json for the cached movies." }
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

    private fun populateCache(mediaEntry: MediaEntry) {
        if (mediaEntry.id == null) return

        val movie =  Movie(
            mediaEntry = mediaEntry,
            isBookmarked = BookmarksDB.isBookmarked(mediaEntry.id),
            isWatched = mediaEntry.id in WatchedDB,
            cacheDate = System.currentTimeMillis()
        )

        cache[mediaEntry.id] = movie
    }

    suspend fun get(id: String): Movie? {
        val STALE_MS = 15L * 24 * 60 * 60 * 1000 // 30 days in ms

        val cached = cache[id]

        // update cache if missing or older than 30 days
        if (cached == null || cached.cacheDate < System.currentTimeMillis() - STALE_MS) {
            val details = justWatch.details(id) ?: return null
            populateCache(details)
            saveCache()
        }
        return cache[id]
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
        if (movie.isWatched) return
        movie.isWatched = true
        WatchedDB.setWatch(id)
    }

    suspend fun deleteWatch(id: String) {
        val movie = get(id) ?: return
        if (!movie.isWatched) return
        movie.isWatched = false
        WatchedDB.deleteWatch(id)
    }

    data class SearchResults(
        val movies: List<Movie>,
        val pageInfo: PageInfo
    )

    suspend fun search(title: String, cursor: String? = null, numResults: Int = 4): SearchResults? {
        val response = justWatch.search(title = title, cursor = cursor, count = numResults) ?: return null

        val searchResult = SearchResults(
            response.edges.mapNotNull {
                if (it.node.id == null) return null
                populateCache(it.node)
                cache[it.node.id]
            },
            response.pageInfo
        )
        saveCache()
        return searchResult
    }

    suspend fun getAllOffers(movie: Movie, countries: Set<String> = Locale.getISOCountries().toSet()): Map<String, List<Offer>> {
        if (movie.mediaEntry.id == null) return countries.associateWith { emptyList() }
        return justWatch.offersForCountries(movie.mediaEntry.id, countries)
    }

    suspend fun getJellyfinLink(movie: Movie): String? {
        val tmdbId = movie.mediaEntry.content?.externalIds?.tmdbId
        val imdbId = movie.mediaEntry.content?.externalIds?.imdbId

        val tmdb = tmdbId?.let { JellyfinClient.findTmdbOnJellyfin(it) }
        val imdb = imdbId?.let { JellyfinClient.findImdbOnJellyfin(it) }

        return tmdb ?: imdb
    }

    suspend inline fun getWatchedMovies() = WatchedDB.getWatched()
    suspend inline fun getBookmarkedMovies(
        displayHidden: Boolean,
        displayWatched: Boolean
    ) = BookmarksDB.getBookmarks()
            .filter { displayHidden || it.isBookmarked }
            .mapNotNull { get(it.id) }
            .filter { displayWatched || !it.isWatched }

    @Serializable
    data class Status(
        val cachedMoviesSize: Int,
        val bookmarkedMoviesSize: Int,
        val watchedMoviesSize: Int,
        val averageCacheAgeDays: Double,
        val standardDeviationCacheAgeDays: Double,
        val medianCacheAgeDays: Double,
        val oldestCacheAgeDays: Double
    )

    private val json = Json { prettyPrint = true }
    fun statusJson(): String {
        val status = Status(
            cachedMoviesSize = cache.size,
            bookmarkedMoviesSize = cache.values.count { it.isBookmarked },
            watchedMoviesSize = cache.values.count { it.isWatched },
            averageCacheAgeDays = if (cache.isEmpty()) 0.0 else cache.values.map { System.currentTimeMillis() - it.cacheDate }.average() / (1000 * 60 * 60 * 24),
            standardDeviationCacheAgeDays = if (cache.isEmpty()) 0.0 else {
                val mean = cache.values.map { System.currentTimeMillis() - it.cacheDate }.average()
                val variance = cache.values.map { (System.currentTimeMillis() - it.cacheDate - mean).let { it * it } }.average()
                sqrt(variance) / (1000 * 60 * 60 * 24)
            },
            medianCacheAgeDays = if (cache.isEmpty()) 0.0 else {
                val sortedAges = cache.values.map { System.currentTimeMillis() - it.cacheDate }.sorted()
                val middle = sortedAges.size / 2
                val median = if (sortedAges.size % 2 == 0) {
                    (sortedAges[middle - 1] + sortedAges[middle]) / 2.0
                } else {
                    sortedAges[middle].toDouble()
                }
                median / (1000 * 60 * 60 * 24)
            },
            oldestCacheAgeDays = (if (cache.isEmpty()) 0.0 else cache.values.maxOf { System.currentTimeMillis() - it.cacheDate } / (1000 * 60 * 60 * 24)).toDouble()
        )
        return json.encodeToString(status)
    }
}
