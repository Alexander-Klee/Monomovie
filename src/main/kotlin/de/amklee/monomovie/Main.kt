package de.amklee.monomovie

import de.amklee.monomovie.components.*
import de.amklee.monomovie.util.Resources
import de.amklee.monomovie.util.buildULHtml
import de.amklee.monomovie.util.respondHtmlTemplate
import io.gitlab.jfronny.commons.logger.SystemLoggerPlus
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.html.*
import kotlinx.serialization.Serializable
import org.intellij.lang.annotations.Language

@Language("HTML")
fun FlowContent.SearchBar(title: String) {
    div(classes = "search-bar") {
        getForm(action = "/search") {
            button(classes = "search-button", type = ButtonType.submit) {
                // language=HTML
                unsafe { +Resources.searchSvg }
            }
            textInput(name = "title", classes = "search-input") {
                placeholder = "Search for a movie…"
                value = title
            }
        }
    }
}

fun FlowContent.SearchPage(title: String, searchResults: SearchTitles?) {
    if (searchResults == null) {
        SearchBar("")
        p { +"Please enter a title to search for." }
        return
    }
    val mediaEntries = searchResults.edges.map {
        CachedMovies.Movie(it.node, isBookmarked = false, isWatched = false, cacheDate = System.currentTimeMillis())
    }

    SearchBar(title)

    script {
        unsafe {
            +Resources.infiniteScrollJs.replace($$"$endCursor$", searchResults.pageInfo.endCursor)
        }
    }

    if (searchResults.edges.isEmpty()) {
        p { +"No Search Results" }
    } else {
        h4 { +"Search Results:" }
        BasicMovieList(mediaEntries)
    }
}

@Serializable
data class MoreSearchResultsResponse(
    val cursor: String,
    val html: String,
    val hasNextPage: Boolean
)

fun MoreSearchResults(searchResults: SearchTitles?): MoreSearchResultsResponse {
    if (searchResults == null || searchResults.edges.isEmpty()) {
        return MoreSearchResultsResponse("", "", false)
    }

    val mediaEntries = searchResults.edges.map {
        CachedMovies.Movie(it.node, isBookmarked = false, isWatched = false, cacheDate = System.currentTimeMillis())
    }
    return MoreSearchResultsResponse(
        searchResults.pageInfo.endCursor,
        buildULHtml {
            for (movie in mediaEntries) {
                MovieListItem(movie)
            }
        },
        searchResults.pageInfo.hasNextPage
    )
}

fun FlowContent.BookmarkPage(movies: List<CachedMovies.Movie>) {
    h1 { +"Bookmarked Movies:" }
    if (movies.isEmpty()) {
        p { +"No bookmarked movies found" }
        return
    }
    postForm(action = "/roulette", classes = "roulette-form") {
        submitInput(classes = "roulette-button") {
            disabled = true
            value = "Roulette"
        }
        SelectableMovieList(movies)
    }
}

fun FlowContent.RoulettePage(movies: List<CachedMovies.Movie>) {
    postForm("/roulette/submit") {
        ul(classes = "roulette-list") {
            for (movie in movies) {
                RouletteMovieListItem(movie)
            }
        }
        submitInput(classes = "roulette-button") {
            value = "Start Roulette"
        }
    }
}

fun FlowContent.HomePage(bookmarkedMovies: List<CachedMovies.Movie>) {
    SearchBar("")
    BookmarkPage(bookmarkedMovies)
}

val wheelOfNames = WheelOfNames(System.getenv("WHEEL_OF_NAMES_API_KEY") ?: throw IllegalStateException("WHEEL_OF_NAMES_API_KEY not set"))
fun Route.miscRoutes() {
    get("/") {
        call.respondHtmlTemplate(HtmlTemplate("Monomovie")) {
            val bookmarkedMovies = CachedMovies.getBookmarkedMovies(displayHidden = false, displayWatched = false)
            body {
                HomePage(bookmarkedMovies)
            }
        }
    }
    get("/search") {
        val title = call.request.queryParameters["title"]
        val numResults = call.request.queryParameters["num"]?.toIntOrNull() ?: 4

        call.respondHtmlTemplate(HtmlTemplate(title?.let { "$it Search" } ?: "Search")) {
            val results = CachedMovies.search(title = title, cursor = null, numResults = numResults)
            body {
                SearchPage(title ?: "", results)
            }
        }
    }
    post("/moreSearchResults") {
        val title = call.request.queryParameters["title"]
        val cursor = call.request.queryParameters["cursor"]

        if (title.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Missing title parameter")
            return@post
        }

        val searchResults = CachedMovies.search(title, cursor)

        call.respond(MoreSearchResults(searchResults))
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

        call.respondHtmlTemplate(HtmlTemplate("Bookmarked Movies")) {
            val movies = CachedMovies.getBookmarkedMovies(displayHidden, displayWatched)
            body {
                BookmarkPage(movies)
            }
        }
    }
    get("/watched") {
        val watchedMovies = CachedMovies.getWatchedMovies()
        call.respondHtmlTemplate(HtmlTemplate("Watched Movies")) {
            body {
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
    post("/roulette") {
        val selectedMovies = call.receiveParameters().getAll("selected[]")?.mapNotNull { CachedMovies.get(it) } ?: emptyList()

        if (selectedMovies.isEmpty() || selectedMovies.size < 2) {
            call.respond(HttpStatusCode.BadRequest, "Not enough movies selected for roulette")
            return@post
        }

        call.respondHtmlTemplate(HtmlTemplate("Roulette")) {
            body {
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
    staticResources("/static", "static")
}

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json()
        }
        routing {
            miscRoutes()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                SystemLoggerPlus.forName(call.request.path()).error("Uncaught exception", cause)
                call.respondText(text = "500: $cause" , status = HttpStatusCode.InternalServerError)
            }
            // TODO: remove, for debugging
//            status(HttpStatusCode.NotFound) {
//                SystemLoggerPlus.forName(call.request.path()).warn("404 Not Found: ${call.request.uri}")
//                call.respondText(text = "404 Not Found", status = HttpStatusCode.NotFound)
//            }
        }
    }.start(wait = true)
}