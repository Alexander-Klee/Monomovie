package de.amklee.monomovie.service

import de.amklee.monomovie.db.*
import de.amklee.monomovie.dto.MediaEntry
import de.amklee.monomovie.dto.Movie
import de.amklee.monomovie.dto.Offer
import de.amklee.monomovie.dto.PageInfo
import de.amklee.monomovie.service.remote.JustWatch
import de.amklee.monomovie.util.error
import de.amklee.monomovie.util.info
import de.amklee.monomovie.util.warn
import java.util.*
import kotlin.math.sqrt
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.upsert

object CachedMovies {
    private val log = System.getLogger("MMV/CachedMovies")

    private val STALE = 15.days

    private val justWatch = JustWatch(country = "DE", language = "en")

    suspend fun get(id: String): Movie? {
        val cached = Movies[id]
        val isStale = cached == null ||
            cached[Movies.cacheDate] < Clock.System.now() - STALE ||
            cached[Movies.mediaEntry] == null

        // try to update cache if missing or older than 15 days
        if (isStale) {
            log.info { "Refetching movie $id (missing or stale)" }
            val details = try {
                justWatch.details(id)
            } catch (e: Exception) {
                log.error(e) { "Unable to update cache for $id." }
                null
            }
            if (details?.id == null) {
                // keep serving the stale or placeholder entry if JustWatch doesn't resolve it
                log.warn { "Using stale cache for $id" }
                return cached?.let { createMovie(it) }
            }
            populateCache(details)
        }
        val joinedMovieRow = suspendTransaction(monomovieDb) {
            Movies.join(Bookmarks, JoinType.LEFT, additionalConstraint = { Bookmarks.id eq Movies.id })
                .join(Watched, JoinType.LEFT, additionalConstraint = { Watched.id eq Movies.id })
                .selectAll().where { Movies.id eq id }
                .singleOrNull()
        }
        return joinedMovieRow?.let { createMovie(it) }
    }

    private suspend fun createMovie(row: ResultRow): Movie? {
        val mediaEntry = row[Movies.mediaEntry] ?: return null
        return suspendTransaction(monomovieDb) {
            Movie(
                mediaEntry = mediaEntry,
                isBookmarked = row.getOrNull(Bookmarks.isBookmarked) ?: false,
                isWatched = row.getOrNull(Watched.watchedAt) != null,
                cacheDate = row[Movies.cacheDate],
            )
        }
    }

    private suspend fun populateCache(mediaEntry: MediaEntry) {
        val id = mediaEntry.id ?: return

        suspendTransaction(monomovieDb) {
            Movies.upsert {
                it[Movies.id] = id
                it[Movies.mediaEntry] = mediaEntry
                it[Movies.cacheDate] = Clock.System.now()
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
            justWatch.search(title = title, cursor = cursor?.ifBlank { null }, count = numResults) ?: return null
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
        val id = movie.mediaEntry.id ?: return countries.associateWith { emptyList() }
        try {
            return justWatch.offersForCountries(id, countries)
        } catch (e: Exception) {
            log.error(e) { "Unable to get offers for $movie." }
            return countries.associateWith { emptyList() }
        }
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
        val now = Clock.System.now()
        val (bookmarkedCount, watchedCount, cacheDates) = suspendTransaction(monomovieDb) {
            Triple(
                Bookmarks.selectAll().where { Bookmarks.isBookmarked eq true }.toList().size,
                Watched.selectAll().toList().size,
                Movies.selectAll().map { it[Movies.cacheDate] }.toList(),
            )
        }

        fun List<Instant>.agesInDays() = map { (now - it).toDouble(DurationUnit.DAYS) }

        val status = Status(
            cachedMoviesSize = cacheDates.size,
            bookmarkedMoviesSize = bookmarkedCount,
            watchedMoviesSize = watchedCount,
            averageCacheAgeDays = if (cacheDates.isEmpty()) {
                0.0
            } else {
                cacheDates.agesInDays().average()
            },
            standardDeviationCacheAgeDays = if (cacheDates.isEmpty()) {
                0.0
            } else {
                val ages = cacheDates.agesInDays()
                val mean = ages.average()
                val variance = ages.map { (it - mean).let { it * it } }.average()
                sqrt(variance)
            },
            medianCacheAgeDays = if (cacheDates.isEmpty()) {
                0.0
            } else {
                val sortedAges = cacheDates.agesInDays().sorted()
                val middle = sortedAges.size / 2
                val median = if (sortedAges.size % 2 == 0) {
                    (sortedAges[middle - 1] + sortedAges[middle]) / 2.0
                } else {
                    sortedAges[middle]
                }
                median
            },
            oldestCacheAgeDays = if (cacheDates.isEmpty()) {
                0.0
            } else {
                cacheDates.agesInDays().max()
            },
        )
        return json.encodeToString(status)
    }
}
