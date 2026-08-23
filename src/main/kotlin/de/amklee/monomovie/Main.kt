@file:OptIn(ExperimentalUuidApi::class)

package de.amklee.monomovie

import de.amklee.monomovie.components.*
import de.amklee.monomovie.db.configureDatabases
import de.amklee.monomovie.pages.*
import de.amklee.monomovie.service.BookmarksService
import de.amklee.monomovie.service.CachedMovies
import de.amklee.monomovie.service.WatchedService
import de.amklee.monomovie.util.*
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
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.html.h1
import kotlinx.html.p
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

fun Route.miscRoutes() {
    get("/") {
        val displayHidden = call.request.queryParameters["hidden"]?.toBoolean() ?: false
        val displayWatched = call.request.queryParameters["watched"]?.toBoolean() ?: false

        call.respondHtml {
            HtmlTemplate("Monomovie") {
                HomePage(
                    CachedMovies.getBookmarkedMovies(
                        displayHidden = displayHidden,
                        displayWatched = displayWatched,
                    ),
                )
            }
        }
    }
    get("/search") {
        val title = call.request.queryParameters["title"]

        call.respondHtml {
            val empty = title.isNullOrBlank()
            HtmlTemplate(if (empty) "Search" else "$title Search") {
                if (empty) {
                    EmptySearchPage()
                } else {
                    SearchPage(title)
                }
            }
        }
    }
    post("/search/results") {
        // TODO implement circuit breaker in JS to prevent spamming this
        val title = call.request.queryParameters["title"]
        val cursor = call.request.queryParameters["cursor"]

        if (title.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Missing title parameter")
            return@post
        }

        val searchResults = CachedMovies.search(
            title,
            cursor,
            numResults = if (cursor ==
                null
            ) {
                8
            } else {
                4
            },
        )
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
        call.respondRedirect(permanent = true) {
            // query parameters get included implicitly with this,
            // but not with respondRedirect("/", true)
            path("/")
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
        val eventFlow =
            merge(
                BookmarksService.eventFlow.map { Kind.BOOKMARK to it },
                WatchedService.eventFlow.map { Kind.WATCHED to it },
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
    route("/roulette") {
        rouletteRoutes()
    }
    get("/CachedMovies.json") {
        call.respondText(CachedMovies.statusJson(), ContentType.Application.Json)
    }
    get("/qr.svg") {
        call.request.queryParameters["data"]?.let { data ->
            call.respondText(
                QrCodeRenderer.renderSVG(QrCode.encodeText(data, ecl = QrCode.Ecc.HIGH)),
                contentType = ContentType.Image.SVG,
            )
        } ?: call.respond(HttpStatusCode.BadRequest, "Missing data parameter")
    }
    staticResources("/static", "static")
}

private val LOG = System.getLogger("MMV/Router")

fun main() {
    setupLogging()
    embeddedServer(CIO, configure = {
        connector {
            port = 8080
        }
    }) {
        // including the hostname has the side-benefit of ensuring Environment is initialized
        // and, therefore, that it does not contain errors
        LOG.info {
            "Starting server in ${if (developmentMode) "development" else "production"} mode at ${Environment.hostname}"
        }
        try {
            configureDatabases()
        } catch (e: Exception) {
            LOG.error(e) { "Failed to configure databases" }
            throw e
        }
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
            exception<CancellationException> { _, e ->
                // Client disconnected, no need to log
                if (e.cause is ClosedWriteChannelException || e.cause is ChannelWriteException) {
                    return@exception
                }
                throw e
            }
            exception<Throwable> { call, cause ->
                LOG.error(cause) { "Uncaught exception for path ${call.request.path()}" }
                call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
            }

            if (this@embeddedServer.developmentMode) {
                status(HttpStatusCode.NotFound) {
                    LOG.warn { "404 Not Found: ${call.request.uri}" }
                    call.respondText(text = "404 Not Found", status = HttpStatusCode.NotFound)
                }
            }
        }
    }.start(wait = true)
}
