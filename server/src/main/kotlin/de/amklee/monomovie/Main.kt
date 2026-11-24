package de.amklee.monomovie

import de.amklee.monomovie.components.HtmlTemplate
import de.amklee.monomovie.components.WatchedMovieList
import de.amklee.monomovie.components.convertBookmarkSse
import de.amklee.monomovie.db.configureDatabases
import de.amklee.monomovie.dto.SseEvent
import de.amklee.monomovie.pages.*
import de.amklee.monomovie.service.BookmarksService
import de.amklee.monomovie.service.CachedMovies
import de.amklee.monomovie.service.WatchedService
import de.amklee.monomovie.service.remote.JellyfinClient
import de.amklee.monomovie.util.*
import io.ktor.http.*
import io.ktor.resources.serialization.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.resources.Resources
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.*
import io.ktor.server.routing.Route as KtorRoute
import io.ktor.server.routing.routing
import io.ktor.server.sse.*
import io.ktor.sse.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import java.security.KeyStore
import kotlin.io.path.inputStream
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.html.h1
import kotlinx.html.p

context(resourcesFormat: ResourcesFormat)
fun KtorRoute.miscRoutes() {
    get<Route.Home> { r ->
        call.respondHtml {
            context(JellyfinClient.getItems()) {
                HtmlTemplate("Monomovie") {
                    HomePage(
                        CachedMovies.getBookmarkedMovies(
                            displayHidden = r.hidden,
                            displayWatched = r.watched,
                        ),
                    )
                }
            }
        }
    }
    get<Route.Search> { r ->
        call.respondHtml {
            val empty = r.title.isBlank()
            HtmlTemplate(if (empty) "Search" else "$r.title Search") {
                if (empty) {
                    EmptySearchPage()
                } else {
                    SearchPage(r.title)
                }
            }
        }
    }
    post<Route.Search.Results> { r ->
        // TODO implement circuit breaker in JS to prevent spamming this
        if (r.parent.title.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Missing title parameter")
            return@post
        }

        val searchResults = CachedMovies.search(
            r.parent.title,
            r.cursor,
            numResults = if (r.cursor.isBlank()) {
                8
            } else {
                4
            },
        )
        searchResults?.let {
            call.respond(
                context(JellyfinClient.getItems()) {
                    MoreSearchResults(it)
                },
            )
            return@post
        }
        call.respond(HttpStatusCode.InternalServerError, "Search failed")
    }
    delete<Route.Bookmarks.One> { r ->
        if (r.movieId.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Missing bookmark ID")
            return@delete
        }

        CachedMovies.deleteBookmark(r.movieId)
        call.respond(HttpStatusCode.OK)
    }
    put<Route.Bookmarks.One> { r ->
        if (r.movieId.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Missing bookmark ID")
            return@put
        }

        CachedMovies.setBookmark(r.movieId)
        call.respond(HttpStatusCode.OK)
    }
    get<Route.Bookmarks> { r ->
        call.respondRedirect(permanent = true) {
            // query parameters get included implicitly with this,
            // but not with respondRedirect("/", true)
            path(href(Route.Home))
        }
    }
    sse<Route.SseStream> { r ->
        heartbeat {
            period = 5.seconds
            event = ServerSentEvent("heartbeat")
        }
        val eventFlow =
            merge(
                BookmarksService.eventFlow.map { SseEvent.Kind.BOOKMARK to it },
                WatchedService.eventFlow.map { SseEvent.Kind.WATCHED to it },
            ).mapNotNull { (kind, event) -> convertBookmarkSse(event, kind) }
        eventFlow.collect { event ->
            send(event)
        }
    }
    get<Route.Watched> { r ->
        val watchedMovies = CachedMovies.getWatchedMovies()
        call.respondHtml {
            context(JellyfinClient.getItems()) {
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
    }
    put<Route.Watched.One> { r ->
        if (r.movieId.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Missing movie ID")
            return@put
        }

        CachedMovies.setWatch(r.movieId)
        call.respond(HttpStatusCode.OK)
    }
    delete<Route.Watched.One> { r ->
        if (r.movieId.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Missing movie ID")
            return@delete
        }

        CachedMovies.deleteWatch(r.movieId)
        call.respond(HttpStatusCode.OK)
    }
    get<Route.Offers> { r ->
        if (r.movieId.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Missing movie ID")
            return@get
        }

        val movie = CachedMovies.get(r.movieId)
        if (movie == null) {
            call.respond(HttpStatusCode.NotFound, "Movie not found")
            return@get
        }

        val offers = CachedMovies.getAllOffers(movie)
        call.respondHtml {
            context(JellyfinClient.getItems()) {
                HtmlTemplate("Countries for ${movie.mediaEntry.content?.title ?: "null"}") {
                    OfferPage(movie, offers, "DE")
                }
            }
        }
    }
    rouletteRoutes()
    get<Route.CachedMovies> { r ->
        call.respondText(CachedMovies.statusJson(), ContentType.Application.Json)
    }
    get<Route.JellyfinItems> { r ->
        call.respond(JellyfinClient.getItems())
    }
    get<Route.QrSvg> { r ->
        r.data.ifBlank { null }?.let { data ->
            call.respondText(
                QrCodeRenderer.renderSVG(QrCode.encodeText(data, ecl = QrCode.Ecc.HIGH)),
                contentType = ContentType.Image.SVG,
            )
        } ?: call.respond(HttpStatusCode.BadRequest, "Missing data parameter")
    }
    get<Route.PlaceholderSvg> { r ->
        call.respondText(R.graphics.svg.imagePlaceholderSvg, ContentType.Image.SVG)
    }
    staticResources("/static", "static")
    staticResources("/client", "client")
}

private val LOG = System.getLogger("MMV/Router")

fun main() {
    setupLogging()
    embeddedServer(
        Netty,
        configure = {
            connector {
                port = 8080
            }
            if (Environment.ssl != null) {
                sslConnector(
                    keyStore = try {
                        KeyStore.getInstance("JKS").apply {
                            Environment.ssl.storeFile.inputStream().use {
                                load(it, Environment.ssl.keyStorePassword.toCharArray())
                            }
                        }
                    } catch (e: Exception) {
                        LOG.error(e) { "Failed to load SSL key store" }
                        throw e
                    },
                    keyAlias = Environment.ssl.keyAlias,
                    keyStorePassword = { Environment.ssl.keyStorePassword.toCharArray() },
                    privateKeyPassword = { Environment.ssl.privateKeyPassword.toCharArray() },
                ) {
                    host = Environment.ssl.host
                    port = 8443
                    keyStorePath = Environment.ssl.storeFile.toFile()
                }
            }
        },
    ) {
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
        install(Resources)
        routing {
            context(plugin(Resources).resourcesFormat) {
                miscRoutes()
            }
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
