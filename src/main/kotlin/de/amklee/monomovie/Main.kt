package de.amklee.monomovie

import de.amklee.monomovie.components.*
import de.amklee.monomovie.db.BookmarksDB
import de.amklee.monomovie.db.WatchedDB
import de.amklee.monomovie.pages.*
import de.amklee.monomovie.util.error
import de.amklee.monomovie.util.respondHtml
import de.amklee.monomovie.util.setupLogging
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.sse.*
import io.ktor.util.*
import io.ktor.util.cio.ChannelWriteException
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.html.h1
import kotlinx.html.p
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.time.Duration.Companion.seconds

val hostname = System.getenv("MMV_HOSTNAME") ?: "http://localhost:8080"

fun Route.miscRoutes() {
    get("/") {
        call.respondHtml {
            HtmlTemplate("Monomovie") {
                HomePage(CachedMovies.getBookmarkedMovies(displayHidden = false, displayWatched = false))
            }
        }
    }
    get("/search") {
        val title = call.request.queryParameters["title"]

        call.respondHtml {
            if (title.isNullOrBlank()) HtmlTemplate("Search") {
                EmptySearchPage()
            } else HtmlTemplate("$title Search") {
                SearchPage(title)
            }
        }
    }
    post("/moreSearchResults") {
        //TODO implement circuit breaker in JS to prevent spamming this
        val title = call.request.queryParameters["title"]
        val cursor = call.request.queryParameters["cursor"]

        if (title.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Missing title parameter")
            return@post
        }

        val searchResults = CachedMovies.search(title, cursor)
        searchResults?.let {
            call.respond(MoreSearchResults(it))
            return@post
        }
        call.respond(HttpStatusCode.InternalServerError, "Search failed")
    }
    post("/bookmark/{movieId}") {
        val movieId = call.parameters["movieId"]

        if (movieId == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing bookmark ID")
            return@post
        }

        CachedMovies.setBookmark(movieId)
        call.respond(HttpStatusCode.OK)
    }
    delete("/bookmark/{movieId}") {
        val movieId = call.parameters["movieId"]

        if (movieId == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing bookmark ID")
            return@delete
        }

        CachedMovies.deleteBookmark(movieId)
        call.respond(HttpStatusCode.OK)
    }
    get("/bookmarks") {
        val displayHidden = call.request.queryParameters["hidden"]?.toBoolean() ?: false
        val displayWatched = call.request.queryParameters["watched"]?.toBoolean() ?: false

        call.respondHtml {
            HtmlTemplate("Bookmarked Movies") {
                BookmarkPage(CachedMovies.getBookmarkedMovies(displayHidden, displayWatched))
            }
        }
    }
    sse("/sse-stream", serialize = { typeInfo, it ->
        val serializer = Json.serializersModule.serializer(typeInfo.kotlinType!!)
        Json.encodeToString(serializer, it)
    }) {
        val mode = call.request.queryParameters["mode"]?.let { Mode.valueOf(it) } ?: Mode.OVERVIEW
        heartbeat {
            period = 5.seconds
            event = ServerSentEvent("heartbeat")
        }
        val eventFlow = merge(
            BookmarksDB.eventFlow.map { Kind.BOOKMARK to it },
            WatchedDB.eventFlow.map { Kind.WATCHED to it }
        ).mapNotNull { (kind, event) -> convertBookmarkSse(event, mode, kind) }
        eventFlow.collect { event ->
            send(event)
        }
    }
    get("/watched") {
        val watchedMovies = CachedMovies.getWatchedMovies()
        call.respondHtml {
            HtmlTemplate("Watched Movies") {
                if (watchedMovies.isEmpty()) {
                    p { +"No watched movies found" }
                } else {
                    h1 { +"Watched Movies:" }
                    WatchedMovieList(watchedMovies)
                }
            }
        }
    }
    post("/watch/{movieId}") {
        val movieId = call.parameters["movieId"]

        if (movieId == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing movie ID")
            return@post
        }

        CachedMovies.setWatch(movieId)
        call.respond(HttpStatusCode.OK)
    }
    delete("/watch/{movieId}") {
        val movieId = call.parameters["movieId"]

        if (movieId == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing bookmark ID")
            return@delete
        }

        CachedMovies.deleteWatch(movieId)
        call.respond(HttpStatusCode.OK)
    }
    get("/offers/{movieId}") {
        val movieId = call.parameters["movieId"]

        if (movieId == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing movie ID")
            return@get
        }

        val movie = CachedMovies.get(movieId)
        if (movie == null) {
            call.respond(HttpStatusCode.NotFound, "Movie not found")
            return@get
        }

        val offers = CachedMovies.getAllOffers(movie)
        call.respondHtml {
            HtmlTemplate("Countries for ${movie.mediaEntry.content?.title ?: "null"}") {
                OfferPage(movie, offers, "DE")
            }
        }
    }
    post("/roulette") {
        val items = call.receiveParameters()
        val selectedMovies = items
            .getAll("selected[]")
            .orEmpty()
            .mapNotNull { CachedMovies.get(it) }

        if (selectedMovies.isEmpty() || selectedMovies.size < 2) {
            call.respond(HttpStatusCode.BadRequest, "Not enough movies selected for roulette")
            return@post
        }

        call.respondHtml {
            HtmlTemplate("Roulette") {
                RoulettePage(selectedMovies)
            }
        }
    }
    post("/roulette/submit") {
        val selectedMovies = call.receiveParameters().toMap().mapNotNull { (id, count) ->
            CachedMovies.get(id)?.let { it to count.sumOf { it.toIntOrNull() ?: 0 } }
        }

        if (selectedMovies.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest, "No movies selected for roulette")
            return@post
        }

        call.respondRedirect(ProvidenceApi.createWheel(selectedMovies))
    }
    get("/CachedMovies.json") {
        call.respondText(CachedMovies.statusJson(), ContentType.Application.Json)
    }
    staticResources("/static", "static")
}

private val LOG = System.getLogger("MMV/Router")

fun main() {
    setupLogging()
    embeddedServer(CIO, port = 8080) {
        install(ContentNegotiation) {
            json()
        }
        install(SSE)
        routing {
            miscRoutes()
        }
        install(StatusPages) {
            exception<ClosedWriteChannelException> { _, _ ->
                // Client disconnected, no need to log
            }
            exception<ChannelWriteException> { _, _ ->
                // Client disconnected, no need to log
            }
            exception<Throwable> { call, cause ->
                LOG.error(cause) { "Uncaught exception for path ${call.request.path()}" }
                call.respondText(text = "500: $cause" , status = HttpStatusCode.InternalServerError)
            }
//             TODO: remove, for debugging
//            status(HttpStatusCode.NotFound) {
//                LOG.warn("404 Not Found: ${call.request.uri}")
//                call.respondText(text = "404 Not Found", status = HttpStatusCode.NotFound)
//            }
        }
    }.start(wait = true)
}
