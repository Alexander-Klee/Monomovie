package de.amklee.monomovie.service

import de.amklee.monomovie.db.Bookmarks
import de.amklee.monomovie.db.Movies
import de.amklee.monomovie.db.Watched
import de.amklee.monomovie.db.get
import de.amklee.monomovie.db.monomovieDb
import de.amklee.monomovie.pages.MonetizationTypes
import de.amklee.monomovie.service.remote.JellyfinClient
import de.amklee.monomovie.service.remote.JustWatch
import de.amklee.monomovie.service.remote.MediaEntry
import de.amklee.monomovie.service.remote.Offer
import de.amklee.monomovie.service.remote.PageInfo
import de.amklee.monomovie.util.error
import java.util.Locale
import kotlin.math.sqrt
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert

object CachedMovies {
    private val log = System.getLogger("MMV/CachedMovies")

    data class Movie(val mediaEntry: MediaEntry, var isBookmarked: Boolean, var isWatched: Boolean, val cacheDate: Long)

    fun Movie.getOffers() = mediaEntry.offers?.filter {
        it.monetizationType.equals(MonetizationTypes.FREE.name, ignoreCase = true) ||
            it.monetizationType.equals(MonetizationTypes.FLATRATE.name, ignoreCase = true)
    } ?: emptyList()

    const val STALE_MS = 15L * 24 * 60 * 60 * 1000 // 15 days in ms

    private val justWatch = JustWatch(country = "DE", language = "en")

    suspend fun get(id: String): Movie? {
        val cached = Movies[id]
        val isStale = cached == null ||
            cached[Movies.cacheDate] < System.currentTimeMillis() - STALE_MS ||
            cached[Movies.mediaEntry] == null

        // try to update cache if missing or older than 15 days
        if (isStale) {
            val details = try {
                justWatch.details(id)
            } catch (e: Exception) {
                log.error(e) { "Unable to update cache for $id." }
                null
            }
            if (details?.id == null) {
                // keep serving the stale or placeholder entry if JustWatch doesn't resolve it
                return cached?.let { createMovie(it) }
            }
            populateCache(details)
        }
        return Movies[id]?.let { createMovie(it) }
    }

    private suspend fun createMovie(row: ResultRow): Movie? {
        val mediaEntry = row[Movies.mediaEntry] ?: return null
        val id = row[Movies.id].value
        return Movie(
            mediaEntry = mediaEntry,
            isBookmarked = BookmarksService.isBookmarked(id),
            isWatched = WatchedService.contains(id),
            cacheDate = row[Movies.cacheDate],
        )
    }

    private suspend fun populateCache(mediaEntry: MediaEntry) {
        val id = mediaEntry.id ?: return

        suspendTransaction(monomovieDb) {
            Movies.upsert {
                it[Movies.id] = id
                it[Movies.mediaEntry] = mediaEntry
                it[Movies.cacheDate] = System.currentTimeMillis()
            }
        }
    }

    suspend fun setBookmark(id: String): Movie? {
        val movie = get(id) ?: return null
        if (!movie.isBookmarked) {
            BookmarksService.addBookmark(id)
            movie.isBookmarked = true
        }
        return movie
    }

    suspend fun deleteBookmark(id: String): Movie? {
        val movie = get(id) ?: return null
        if (movie.isBookmarked) {
            BookmarksService.removeBookmark(id)
            movie.isBookmarked = false
        }
        return movie
    }

    suspend fun setWatch(id: String) {
        val movie = get(id) ?: return
        if (movie.isWatched) return
        WatchedService.setWatch(id)
        movie.isWatched = true
    }

    suspend fun deleteWatch(id: String) {
        val movie = get(id) ?: return
        if (!movie.isWatched) return
        WatchedService.deleteWatch(id)
        movie.isWatched = false
    }

    data class SearchResults(val movies: List<Movie>, val pageInfo: PageInfo)

    suspend fun search(title: String, cursor: String? = null, numResults: Int = 4): SearchResults? {
        val response = try {
            justWatch.search(title = title, cursor = cursor, count = numResults) ?: return null
        } catch (e: Exception) {
            log.error(e) { "Unable to search for $title" }
            return null
        }

        val searchResult = SearchResults(
            response.edges.mapNotNull {
                if (it.node.id == null) return null
                populateCache(it.node)
                get(it.node.id!!)
            },
            response.pageInfo,
        )
        return searchResult
    }

    suspend fun getAllOffers(movie: Movie, countries: Set<String> = Locale.getISOCountries().toSet()): Map<String, List<Offer>> {
        if (movie.mediaEntry.id == null) return countries.associateWith { emptyList() }
        try {
            return justWatch.offersForCountries(movie.mediaEntry.id, countries)
        } catch (e: Exception) {
            log.error(e) { "Unable to get offers for $movie." }
            return countries.associateWith { emptyList() }
        }
    }

    suspend fun getJellyfinLink(movie: Movie): String? {
        val tmdbId = movie.mediaEntry.content
            ?.externalIds
            ?.tmdbId
        val imdbId = movie.mediaEntry.content
            ?.externalIds
            ?.imdbId

        val tmdb = tmdbId?.let { JellyfinClient.findTmdbOnJellyfin(it) }
        val imdb = imdbId?.let { JellyfinClient.findImdbOnJellyfin(it) }

        return tmdb ?: imdb
    }

    suspend inline fun getWatchedMovies() = WatchedService.getWatched()
    suspend inline fun getBookmarkedMovies(displayHidden: Boolean, displayWatched: Boolean) = BookmarksService.getBookmarks()
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
        val oldestCacheAgeDays: Double,
    )

    private val json = Json { prettyPrint = true }

    suspend fun statusJson(): String {
        val now = System.currentTimeMillis()
        val (bookmarkedCount, watchedCount, cacheDates) = suspendTransaction(monomovieDb) {
            Triple(
                Bookmarks.selectAll().where { Bookmarks.isBookmarked eq true }.toList().size,
                Watched.selectAll().toList().size,
                Movies.selectAll().map { it[Movies.cacheDate] }.toList(),
            )
        }

        fun List<Long>.ages() = map { now - it }

        val status = Status(
            cachedMoviesSize = cacheDates.size,
            bookmarkedMoviesSize = bookmarkedCount,
            watchedMoviesSize = watchedCount,
            averageCacheAgeDays = if (cacheDates.isEmpty()) {
                0.0
            } else {
                cacheDates.ages().average() / (1000 * 60 * 60 * 24)
            },
            standardDeviationCacheAgeDays = if (cacheDates.isEmpty()) {
                0.0
            } else {
                val ages = cacheDates.ages()
                val mean = ages.average()
                val variance = ages.map { (it - mean).let { it * it } }.average()
                sqrt(variance) / (1000 * 60 * 60 * 24)
            },
            medianCacheAgeDays = if (cacheDates.isEmpty()) {
                0.0
            } else {
                val sortedAges = cacheDates.ages().sorted()
                val middle = sortedAges.size / 2
                val median = if (sortedAges.size % 2 == 0) {
                    (sortedAges[middle - 1] + sortedAges[middle]) / 2.0
                } else {
                    sortedAges[middle].toDouble()
                }
                median / (1000 * 60 * 60 * 24)
            },
            oldestCacheAgeDays = (
                if (cacheDates.isEmpty()) {
                    0.0
                } else {
                    cacheDates.ages().max() / (1000 * 60 * 60 * 24)
                }
                ).toDouble(),
        )
        return json.encodeToString(status)
    }
}
