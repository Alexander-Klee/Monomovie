package de.amklee.monomovie

import de.amklee.monomovie.components.HtmlTemplate
import de.amklee.monomovie.pages.RouletteCachedMovie
import de.amklee.monomovie.pages.RoulettePage
import de.amklee.monomovie.pages.SharedRouletteSelectionPage
import de.amklee.monomovie.pages.SharedRouletteSession
import de.amklee.monomovie.util.SharedSessionContainer
import de.amklee.monomovie.util.respondHtml
import de.amklee.monomovie.util.warn
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.sse.*
import io.ktor.util.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val LOG = System.getLogger("MMV/RouletteRoutes")

@OptIn(ExperimentalUuidApi::class)
fun Route.rouletteRoutes() {
    val sharedRouletteSessions = SharedSessionContainer<Uuid, SharedRouletteSession>(10.seconds) { SharedRouletteSession() }
    post {
        val shareId = call.request.queryParameters["shareId"]?.let {
            Uuid.parseOrNull(it) ?: run {
                call.respond(HttpStatusCode.BadRequest, "Invalid shareId parameter")
                return@post
            }
        }

        val items = call.receiveParameters()
        val selectedMovies = items
            .getAll("selected[]")
            .orEmpty()
            .mapNotNull { CachedMovies.get(it) }

        val votedMovies = if (shareId != null) {
            sharedRouletteSessions.preheat(shareId).let {
                it.addAll(selectedMovies)
            }
        } else selectedMovies.map { RouletteCachedMovie(it, 1) }

        if (votedMovies.isEmpty() || votedMovies.size < 2) {
            call.respond(HttpStatusCode.BadRequest, "Not enough movies selected for roulette")
            return@post
        }

        call.respondHtml {
            HtmlTemplate("Roulette") {
                RoulettePage(votedMovies, shareId = shareId)
            }
        }
    }
    get {
        val shareId = call.request.queryParameters["shareId"]?.let {
            Uuid.parseOrNull(it) ?: run {
                call.respond(HttpStatusCode.BadRequest, "Invalid shareId parameter")
                return@get
            }
        } ?: run {
            call.respond(HttpStatusCode.BadRequest, "Missing shareId parameter")
            return@get
        }

        val selectedMovies = sharedRouletteSessions.preheat(shareId, construct = {
            LOG.warn { "Roulette session with ID $shareId was requested but did not exist. Creating new session." }
            SharedRouletteSession()
        }).addAll(emptyList())

        call.respondHtml {
            HtmlTemplate("Roulette") {
                RoulettePage(selectedMovies, shareId = shareId)
            }
        }
    }
    suspend fun ApplicationCall.getShareId(): Uuid? = parameters["shareId"]?.let {
        Uuid.parseOrNull(it) ?: run {
            respond(HttpStatusCode.BadRequest, "Invalid shareId parameter")
            null
        }
    } ?: run {
        respond(HttpStatusCode.BadRequest, "Missing shareId parameter")
        null
    }
    get("/shared/{shareId}") {
        val shareId = call.getShareId() ?: return@get

        sharedRouletteSessions.reheat(shareId)

        val displayHidden = call.request.queryParameters["hidden"]?.toBoolean() ?: false
        val displayWatched = call.request.queryParameters["watched"]?.toBoolean() ?: false

        call.respondHtml {
            HtmlTemplate("Shared Roulette") {
                //TODO pre-select or hide movies that are already in the session
                SharedRouletteSelectionPage(CachedMovies.getBookmarkedMovies(displayHidden, displayWatched), shareId)
            }
        }
    }
    post("/shared/{shareId}/{movieId}") {
        val shareId = call.getShareId() ?: return@post
        val movieId = call.parameters["movieId"] ?: run {
            call.respond(HttpStatusCode.BadRequest, "Missing movieId")
            return@post
        }
        val movie = CachedMovies.get(movieId) ?: run {
            call.respond(HttpStatusCode.BadRequest, "Invalid movieId")
            return@post
        }
        val count = call.receiveText().toIntOrNull()

        sharedRouletteSessions.withValueSuspend(shareId) {
            it.add(movie)
            if (count != null) {
                it.updateCount(movie, count)
            }
        }

        call.respond(HttpStatusCode.OK)
    }
    delete("/shared/{shareId}/{movieId}") {
        val shareId = call.getShareId() ?: return@delete
        val movieId = call.parameters["movieId"] ?: run {
            call.respond(HttpStatusCode.BadRequest, "Missing movieId")
            return@delete
        }
        val movie = CachedMovies.get(movieId) ?: run {
            call.respond(HttpStatusCode.BadRequest, "Invalid movieId")
            return@delete
        }

        sharedRouletteSessions.withValue(shareId) {
            it.remove(movie)
        }

        call.respond(HttpStatusCode.OK)
    }
    sse("/shared/{shareId}/sse", serialize = { typeInfo, it ->
        val serializer = Json.serializersModule.serializer(typeInfo.kotlinType!!)
        Json.encodeToString(serializer, it)
    }) {
        val shareId = call.getShareId() ?: return@sse
        val isSelection = call.request.queryParameters["isSelection"]?.toBoolean() ?: false

        heartbeat {
            period = 5.seconds
            event = ServerSentEvent("heartbeat")
        }

        sharedRouletteSessions.withValueSuspend(shareId) { session ->
            session.events(isSelection = isSelection).collect { event ->
                send(event)
            }
        }
    }
    post("/share") {
        val selectedMovies = call.receiveParameters().toMap().mapNotNull { (id, count) ->
            CachedMovies.get(id)?.let { it to count.sumOf { it.toIntOrNull() ?: 0 } }
        }

        val shareId = Uuid.random()

        sharedRouletteSessions.preheat(shareId).let { session ->
            for ((movie, count) in selectedMovies) {
                session.updateCount(movie, count)
            }
        }

        call.respondRedirect("/roulette?shareId=$shareId")
    }
    post("/submit") {
        val selectedMovies = call.receiveParameters().toMap().mapNotNull { (id, count) ->
            CachedMovies.get(id)?.let { it to count.sumOf { it.toIntOrNull() ?: 0 } }
        }

        if (selectedMovies.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest, "No movies selected for roulette")
            return@post
        }

        call.respondRedirect(ProvidenceApi.createWheel(selectedMovies))
    }
}