@file:OptIn(ExperimentalUuidApi::class)

package de.amklee.monomovie.pages

import de.amklee.monomovie.CachedMovies
import de.amklee.monomovie.components.RouletteMovieList
import de.amklee.monomovie.components.RouletteMovieListItem
import de.amklee.monomovie.components.SelectableMovieList
import de.amklee.monomovie.components.SelectableMovieListItem
import de.amklee.monomovie.hostname
import de.amklee.monomovie.util.QrCode
import de.amklee.monomovie.util.Resources
import de.amklee.monomovie.util.buildULHtml
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
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

    data class AddEventWithModes(override val id: String, val selectionBody: String, val votingBody: String) : RouletteSseEvent
}

class SharedRouletteSession {
    private val movies = LinkedHashMap<String, RouletteCachedMovie>()
    private val mutex = Mutex()

    private val _events = MutableSharedFlow<RouletteSseEvent>()

    suspend fun events(isSelection: Boolean): Flow<RouletteSseEvent> = _events.map {
        when (it) {
            is RouletteSseEvent.AddEventWithModes -> if (isSelection) RouletteSseEvent.Add(it.id, it.selectionBody)
                                                     else             RouletteSseEvent.Add(it.id, it.votingBody)
            else -> it
        }
    }

    constructor()

    suspend fun addAll(movies: List<CachedMovies.Movie>): Collection<RouletteCachedMovie> {
        for (movie in movies) { add(movie) }
        return this.movies.values
    }

    suspend fun add(movie: CachedMovies.Movie) {
        mutex.withLock {
            if (movies.containsKey(movie.mediaEntry.id!!)) return
            movies[movie.mediaEntry.id] = RouletteCachedMovie(movie, 1)
        }
        // not in the critical section, so technically the movie could have been added in the meantime.
        // however, this avoids locking us up which is probably more important in the case of one slow client.
        _events.emit(buildAddEvent(movie, 1))
    }
    suspend fun updateCount(movie: CachedMovies.Movie, count: Int) {
        val event = mutex.withLock {
            val old = movies[movie.mediaEntry.id!!]
            if (old == null) {
                movies[movie.mediaEntry.id] = RouletteCachedMovie(movie, count)
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

    private suspend fun buildAddEvent(movie: CachedMovies.Movie, count: Int): RouletteSseEvent.AddEventWithModes {
        return RouletteSseEvent.AddEventWithModes(
            id = movie.mediaEntry.id!!,
            selectionBody = buildULHtml { SelectableMovieListItem(movie) },
            votingBody = buildULHtml { RouletteMovieListItem(movie, count) }
        )
    }

    fun remove(movie: CachedMovies.Movie) {
        movies.remove(movie.mediaEntry.id!!)?.let {
            _events.tryEmit(RouletteSseEvent.Remove(movie.mediaEntry.id))
        }
    }
}

data class RouletteCachedMovie(val movie: CachedMovies.Movie, var votes: Int)

suspend fun FlowContent.RoulettePage(movies: Collection<RouletteCachedMovie>, shareId: Uuid?) {
    if (shareId != null) {
        script {
            unsafe {
                +Resources.rouletteSharedJs(false, shareId)
            }
        }
        div(classes = "qr-code") {
            QrCode(QrCode.encodeText("$hostname/roulette/shared/$shareId", QrCode.Ecc.QUARTILE))
        }
    }
    postForm("/roulette/submit") {
        div(classes = "roulette-action-row") {
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
        submitInput(classes = "roulette-button") {
            value = "Start Roulette"
        }
    }
}

suspend fun FlowContent.SharedRouletteSelectionPage(movies: List<CachedMovies.Movie>, shareId: Uuid) {
    script {
        unsafe {
            +Resources.rouletteSharedJs(true, shareId)
        }
    }
    h1 { +"Shared Roulette:" }
    div(classes = "qr-code") {
        QrCode(QrCode.encodeText("$hostname/roulette/shared/$shareId", QrCode.Ecc.QUARTILE))
    }
    if (movies.isEmpty()) {
        p { +"No bookmarked movies found" }
        return
    }
    postForm(action = "/roulette?shareId=$shareId", classes = "roulette-form") {
        submitInput(classes = "roulette-button") {
            disabled = true
            value = "Add to Roulette"
        }
        SelectableMovieList(movies, minSelection = 1)
    }
}
