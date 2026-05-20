@file:OptIn(ExperimentalUuidApi::class)

package de.amklee.monomovie.pages

import de.amklee.monomovie.CachedMovies
import de.amklee.monomovie.Environment
import de.amklee.monomovie.ProvidenceApi
import de.amklee.monomovie.components.RouletteMovieList
import de.amklee.monomovie.components.RouletteMovieListItem
import de.amklee.monomovie.components.SelectableMovieList
import de.amklee.monomovie.util.LazyValue
import de.amklee.monomovie.util.Resources
import de.amklee.monomovie.util.buildULHtml
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.html.*
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
sealed interface RouletteSseEvent {
    val id: String

    @Serializable data class Add(override val id: String, val body: String) : RouletteSseEvent
    @Serializable data class Remove(override val id: String) : RouletteSseEvent
    @Serializable data class Update(override val id: String, val count: Int) : RouletteSseEvent
}

class SharedRouletteSession {
    val hash = LazyValue { ProvidenceApi.getLatestHash() }

    private val movies = LinkedHashMap<String, RouletteCachedMovie>()
    private val mutex = Mutex()

    private val _events = MutableSharedFlow<RouletteSseEvent>()

    fun events(): SharedFlow<RouletteSseEvent> = _events.asSharedFlow()

    suspend fun addAll(movies: List<CachedMovies.Movie>): Collection<RouletteCachedMovie> {
        for (movie in movies) { add(movie) }
        return this.movies.values
    }

    suspend fun add(movie: CachedMovies.Movie) {
        mutex.withLock {
            if (movies.containsKey(movie.mediaEntry.id!!)) return
            movies[movie.mediaEntry.id] = movie withVotes 1
        }
        // not in the critical section, so technically the movie could have been added in the meantime.
        // however, this avoids locking us up which is probably more important in the case of one slow client.
        _events.emit(buildAddEvent(movie, 1))
    }
    suspend fun updateCount(movie: CachedMovies.Movie, count: Int) {
        val event = mutex.withLock {
            val old = movies[movie.mediaEntry.id!!]
            if (old == null) {
                movies[movie.mediaEntry.id] = movie withVotes count
                buildAddEvent(movie, count)
            } else {
                if (old.votes != count) {
                    old.votes = count
                    RouletteSseEvent.Update(movie.mediaEntry.id, count)
                } else null
            }
        }
        // not in the critical section, so technically the movie could have been added in the meantime.
        // however, this avoids locking us up which is probably more important in the case of one slow client.
        if (event != null) _events.emit(event)
    }

    private suspend fun buildAddEvent(movie: CachedMovies.Movie, count: Int): RouletteSseEvent.Add {
        return RouletteSseEvent.Add(
            id = movie.mediaEntry.id!!,
            body = buildULHtml { RouletteMovieListItem(movie, count) }
        )
    }

    suspend fun remove(movie: CachedMovies.Movie) {
        mutex.withLock { movies.remove(movie.mediaEntry.id!!) }?.let {
            _events.tryEmit(RouletteSseEvent.Remove(movie.mediaEntry.id!!))
        }
    }
}

data class RouletteCachedMovie(val movie: CachedMovies.Movie, var votes: Int)
infix fun CachedMovies.Movie.withVotes(votes: Int) = RouletteCachedMovie(this, votes)

suspend fun FlowContent.RoulettePage(movies: Collection<RouletteCachedMovie>, shareId: Uuid?) {
    if (shareId != null) {
        script {
            unsafe {
                +Resources.rouletteSharedJs(shareId)
            }
        }
        val target = "${Environment.hostname}/roulette?shareId=$shareId"
        img(classes = "qr-code", src = "/qr.svg?data=${target.encodeURLParameter()}", alt = "QR Code")
    }
    postForm("/roulette/submit" + if (shareId == null) "" else "?shareId=$shareId") {
        div(classes = "sticky-action-row") {
            submitInput(classes = "roulette-button") {
                value = "Start Roulette"
            }
            if (shareId == null) {
                submitInput(classes = "roulette-button") {
                    value = "Share"
                    formAction = "/roulette/share"
                }
            } else {
                a(href = "/roulette/shared/$shareId", target = "_blank", classes = "roulette-button") {
                    +"Add More Movies"
                }
            }
        }
        RouletteMovieList(movies)
    }
}

suspend fun FlowContent.SharedRouletteSelectionPage(movies: List<CachedMovies.Movie>, shareId: Uuid) {
    SearchBar("")
    h1 { +"Shared Roulette:" }
    if (movies.isEmpty()) {
        p { +"No bookmarked movies found" }
        return
    }
    postForm(action = "/roulette?shareId=$shareId", classes = "roulette-form") {
        div(classes = "sticky-action-row") {
            submitInput(classes = "roulette-button require-min-selection") {
                disabled = true
                value = "Add to Roulette"
            }
        }
        SelectableMovieList(movies, minSelection = 1)
    }
}
