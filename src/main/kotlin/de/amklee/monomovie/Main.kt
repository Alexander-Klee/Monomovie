package de.amklee.monomovie

import de.amklee.monomovie.components.HtmlTemplate
import de.amklee.monomovie.components.Kind
import de.amklee.monomovie.components.Mode
import de.amklee.monomovie.components.WatchedMovieList
import de.amklee.monomovie.components.convertBookmarkSse
import de.amklee.monomovie.db.BookmarksDB
import de.amklee.monomovie.db.WatchedDB
import de.amklee.monomovie.pages.*
import de.amklee.monomovie.util.error
import de.amklee.monomovie.util.respondHtml
import de.amklee.monomovie.util.setupLogging
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.CIO
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.SSE
import io.ktor.server.sse.heartbeat
import io.ktor.server.sse.send
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import io.ktor.util.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.html.h1
import kotlinx.html.p
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.time.Duration.Companion.seconds


val wheelOfNames = WheelOfNames(
    System.getenv("WHEEL_OF_NAMES_API_KEY")
        ?: throw IllegalStateException("WHEEL_OF_NAMES_API_KEY not set"))

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
        val numResults = call.request.queryParameters["num"]?.toIntOrNull() ?: 4

        call.respondHtml {
            if (title.isNullOrBlank()) HtmlTemplate("Search") {
                EmptySearchPage()
            } else HtmlTemplate("$title Search") {
                val searchResults = CachedMovies.search(title = title, cursor = null, numResults = numResults)
                searchResults?.let {
                    SearchPage(
                        title,
                        it
                    )
                }
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
            period = 15.seconds
            event = ServerSentEvent("heartbeat")
        }
        merge(
            BookmarksDB.eventFlow.map { Kind.BOOKMARK to it },
            WatchedDB.eventFlow.map { Kind.WATCHED to it }
        ).collect { (kind, event) ->
            send(convertBookmarkSse(event, mode, kind) ?: return@collect)
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
        val selectedMovies = call.receiveParameters().getAll("selected[]")?.mapNotNull { CachedMovies.get(it) } ?: emptyList()

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

        call.respondRedirect(
            wheelOfNames.createWheel(
                selectedMovies.map { (movie, weight) ->
                    WheelOfNames.Entry(movie.mediaEntry.content?.title ?: "null", weight)
                }
            ))
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
