package de.amklee.monomovie.pages

import de.amklee.monomovie.Route
import de.amklee.monomovie.components.*
import de.amklee.monomovie.dto.*
import de.amklee.monomovie.service.remote.JellyfinClient
import de.amklee.monomovie.service.remote.ProvidenceApi
import de.amklee.monomovie.util.LazyValue
import de.amklee.monomovie.util.href
import de.amklee.monomovie.util.hrefFull
import io.ktor.resources.serialization.*
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.html.*

class SharedRouletteSession {
    val hash = LazyValue { ProvidenceApi.getLatestHash() }

    private val movies = LinkedHashMap<String, RouletteCachedMovie>()
    private val mutex = Mutex()

    private val events = MutableSharedFlow<RouletteSseEvent>()

    // Note: this does not contain bookmark events (those are injected in RouletteRoutes)
    fun events(): SharedFlow<RouletteSseEvent> = events.asSharedFlow()

    suspend fun addAll(movies: List<Movie>): Collection<RouletteCachedMovie> {
        for (movie in movies) {
            add(movie)
        }
        return this.movies.values
    }

    suspend fun add(movie: Movie) {
        mutex.withLock {
            if (movies.containsKey(movie.mediaEntry.id!!)) return
            movies[movie.mediaEntry.id!!] = movie withVotes 1
        }
        // not in the critical section, so technically the movie could have been added in the meantime.
        // however, this avoids locking us up which is probably more important in the case of one slow client.
        events.emit(buildAddEvent(movie, 1))
    }

    suspend fun updateCount(movie: Movie, count: Int) {
        val event =
            mutex.withLock {
                val old = movies[movie.mediaEntry.id!!]
                if (old == null) {
                    movies[movie.mediaEntry.id!!] = movie withVotes count
                    buildAddEvent(movie, count)
                } else {
                    if (old.votes != count) {
                        old.votes = count
                        RouletteSseEvent.Update(movie.mediaEntry.id!!, count)
                    } else {
                        null
                    }
                }
            }
        // not in the critical section, so technically the movie could have been added in the meantime.
        // however, this avoids locking us up which is probably more important in the case of one slow client.
        if (event != null) events.emit(event)
    }

    private suspend fun buildAddEvent(movie: Movie, count: Int): RouletteSseEvent.Add = RouletteSseEvent.Add(
        id = movie.mediaEntry.id!!,
        item = context(JellyfinClient.getItems()) { MovieItemUI(movie) },
        count = count,
    )

    suspend fun remove(movie: Movie) {
        mutex.withLock { movies.remove(movie.mediaEntry.id!!) }?.let {
            events.tryEmit(RouletteSseEvent.Remove(movie.mediaEntry.id!!))
        }
    }
}

data class RouletteCachedMovie(val movie: Movie, var votes: Int)

infix fun Movie.withVotes(votes: Int) = RouletteCachedMovie(this, votes)

@HtmlTagMarker
context(jellyfinItems: JellyfinItems, resourcesFormat: ResourcesFormat)
fun FlowContent.RoulettePage(movies: Collection<RouletteCachedMovie>, shareId: Uuid?) {
    if (shareId != null) {
        img(
            classes = "qr-code",
            src = href(Route.QrSvg(data = hrefFull(Route.Roulette.Shared.Selection(shareId = shareId)))),
            alt = "QR Code",
        )
    }
    postForm(href(Route.Roulette.Submit(shareId = shareId))) {
        div(classes = "sticky-action-row") {
            submitInput(classes = "button") {
                value = "Start Roulette"
            }
            if (shareId == null) {
                submitInput(classes = "button") {
                    value = "Share"
                    formAction = href(Route.Roulette.Shared())
                }
            } else {
                a(
                    href = href(Route.Roulette.Shared.Selection(shareId = shareId)),
                    target = "_blank",
                    classes = "button",
                ) {
                    +"Add More Movies"
                }
            }
        }
        RouletteMovieList(movies, shareId)
    }
}

@HtmlTagMarker
context(jellyfinItems: JellyfinItems, resourcesFormat: ResourcesFormat)
fun FlowContent.SharedRouletteSelectionPage(movies: List<Movie>, shareId: Uuid) {
    SearchBar("")
    h1(classes = "title is-3") { +"Shared Roulette:" }
    if (movies.isEmpty()) {
        p(classes = "notification") { +"No bookmarked movies found" }
        return
    }
    postForm(action = href(Route.Roulette.Voting(shareId = shareId)), classes = "roulette-form") {
        div(classes = "sticky-action-row") {
            RouletteSelectionButton(label = "Add to Roulette", minSelection = 1)
        }
        SelectableMovieList(movies)
    }
}
