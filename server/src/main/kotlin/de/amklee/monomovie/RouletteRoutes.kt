package de.amklee.monomovie

import de.amklee.monomovie.components.HtmlTemplate
import de.amklee.monomovie.pages.RoulettePage
import de.amklee.monomovie.pages.SharedRouletteSelectionPage
import de.amklee.monomovie.pages.SharedRouletteSession
import de.amklee.monomovie.pages.withVotes
import de.amklee.monomovie.service.CachedMovies
import de.amklee.monomovie.service.remote.JellyfinClient
import de.amklee.monomovie.service.remote.ProvidenceApi
import de.amklee.monomovie.util.SharedSessionContainer
import de.amklee.monomovie.util.href
import de.amklee.monomovie.util.respondHtml
import de.amklee.monomovie.util.sse
import de.amklee.monomovie.util.warn
import io.ktor.http.*
import io.ktor.resources.serialization.ResourcesFormat
import io.ktor.server.request.*
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.Route as KtorRoute
import io.ktor.server.sse.*
import io.ktor.sse.*
import io.ktor.util.*
import kotlin.collections.emptyList
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

private val LOG = System.getLogger("MMV/RouletteRoutes")

context(resourcesFormat: ResourcesFormat)
fun KtorRoute.rouletteRoutes() {
    val sharedRouletteSessions =
        SharedSessionContainer<Uuid, SharedRouletteSession>(10.seconds) { SharedRouletteSession() }
    post<Route.Roulette.Voting> { r ->
        val shareId = r.shareId

        val items = call.receiveParameters()
        val selectedMovies = items
            .getAll("selected[]")
            .orEmpty()
            .mapNotNull { CachedMovies.get(it) }

        val votedMovies = if (shareId != null) {
            sharedRouletteSessions
                .preheat(shareId)
                .addAll(selectedMovies)
        } else {
            selectedMovies.map { it withVotes 1 }
        }

        if (votedMovies.size < if (shareId == null) 2 else 1) {
            call.respond(HttpStatusCode.BadRequest, "Not enough movies selected for roulette")
            return@post
        }

        call.respondHtml {
            context(JellyfinClient.getItems()) {
                HtmlTemplate("Roulette") {
                    RoulettePage(votedMovies, shareId = shareId)
                }
            }
        }
    }
    get<Route.Roulette.Voting> { r ->
        val shareId = r.shareId ?: run {
            call.respond(HttpStatusCode.BadRequest, "Missing shareId parameter")
            return@get
        }

        val selectedMovies = sharedRouletteSessions
            .preheat(shareId, construct = {
                LOG.warn {
                    "Roulette session with ID $shareId was requested but did not exist. Creating new session."
                }
                SharedRouletteSession()
            })
            .addAll(emptyList())

        call.respondHtml {
            context(JellyfinClient.getItems()) {
                HtmlTemplate("Roulette") {
                    RoulettePage(selectedMovies, shareId = shareId)
                }
            }
        }
    }

    get<Route.Roulette.Shared.Selection> { r ->
        sharedRouletteSessions.reheat(r.shareId)

        call.respondHtml {
            context(JellyfinClient.getItems()) {
                HtmlTemplate("Shared Roulette") {
                    // TODO pre-select or hide movies that are already in the session
                    SharedRouletteSelectionPage(
                        CachedMovies.getBookmarkedMovies(displayHidden = r.hidden, displayWatched = r.watched),
                        r.shareId,
                    )
                }
            }
        }
    }
    post<Route.Roulette.Shared.Movie> { r ->
        // update number of votes
        val movie = CachedMovies.get(r.movieId) ?: run {
            call.respond(HttpStatusCode.BadRequest, "Invalid movieId")
            return@post
        }
        val count = call.receiveText().toIntOrNull()

        sharedRouletteSessions.withValueSuspend(r.shareId) {
            it.add(movie)
            if (count != null) {
                it.updateCount(movie, count)
            }
        }

        call.respond(HttpStatusCode.OK)
    }
    delete<Route.Roulette.Shared.Movie> { r ->
        val movie = CachedMovies.get(r.movieId) ?: run {
            call.respond(HttpStatusCode.BadRequest, "Missing movieId")
            return@delete
        }

        sharedRouletteSessions.withValueSuspend(r.shareId) {
            it.remove(movie)
        }

        call.respond(HttpStatusCode.OK)
    }
    sse<Route.Roulette.Shared.Sse> { r ->
        heartbeat {
            period = 5.seconds
            event = ServerSentEvent("heartbeat")
        }

        sharedRouletteSessions.withValueSuspend(r.shareId) { session ->
            session.events().collect { event ->
                send(event)
            }
        }
    }
    post<Route.Roulette.Shared> {
        // create a new shared vote
        val parameters = call.receiveParameters().toMap()
        val selectedMovies =
            (parameters - "selected[]").mapNotNull { (id, count) ->
                CachedMovies.get(id)?.let { it withVotes count.sumOf { it.toIntOrNull() ?: 0 } }
            } + (
                parameters["selected[]"]?.mapNotNull {
                    CachedMovies.get(it)?.let { it withVotes 1 }
                } ?: emptyList()
                )

        val shareId = Uuid.random()

        sharedRouletteSessions.preheat(shareId).let { session ->
            for ((movie, count) in selectedMovies) {
                session.updateCount(movie, count)
            }
        }

        call.respondRedirect(href(Route.Roulette.Voting(shareId = shareId)))
    }
    post<Route.Roulette.Submit> { r ->
        // go to roulette
        val selectedMovies = call.receiveParameters().toMap().mapNotNull { (id, count) ->
            CachedMovies.get(id)?.let { it withVotes count.sumOf { it.toIntOrNull() ?: 0 } }
        }

        // maybe retrieve hash
        val (movies, hash) = r.shareId?.let {
            sharedRouletteSessions.withValueSuspend(it) {
                for ((movie, count) in selectedMovies) {
                    it.updateCount(movie, count)
                }
                it.addAll(listOf()) to it.hash.get()
            }
        } ?: (selectedMovies to null)

        val filteredMovies = movies.filter { it.votes > 0 }

        if (filteredMovies.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest, "No movies selected for roulette")
            return@post
        }

        call.respondRedirect(ProvidenceApi.createWheel(filteredMovies, hash))
    }
}
