package de.amklee.monomovie

import de.amklee.monomovie.components.MovieItem
import de.amklee.monomovie.components.MovieList
import de.amklee.monomovie.components.NavBar
import de.amklee.monomovie.components.htmlTemplate
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
import kotlinx.serialization.Serializable
import org.intellij.lang.annotations.Language


@Language("HTML")
fun SearchBar(title: String?): String = """
    <div class="search-bar">
        <form action="/search" method="get">
        <button type="submit" class="search-button">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20">
              <title>search</title>
              <path d="M12.2 13.6a7 7 0 1 1 1.4-1.4l5.4 5.4-1.4 1.4zM3 8a5 5 0 1 0 10 0A5 5 0 0 0 3 8"/>
            </svg>
        </button>
        <input type="text" name="title" placeholder="Search for a movie..." value="${title?.escapeHTML() ?: ""}"/>
        </form>
    </div>"""

suspend inline fun SearchPage(title: String?, numResults: Int = 4): String {
    if (title.isNullOrBlank()) {
        return SearchBar("") + "<p>Please enter a title to search for.</p>"
    }
    val searchResults = CachedMovies.search(title = title, cursor = null, numResults = numResults)
    val mediaEntries = searchResults.edges.map {
        CachedMovies.Movie(it.node, false, System.currentTimeMillis())
    }

    val resultContent = if (searchResults.edges.isEmpty()) {
        return SearchBar(title) + "<p>No Search Results.</p>"
    } else {
        // language=HTML
        """
        <h4>Search results:</h4>
        ${MovieList(mediaEntries).basicList()}
        """.trimIndent()
    }
    // language=HTML
    val showMoreResults = $$"""
          <script>
            let hasNextPage = true;
            let lastCursor = "$${searchResults.pageInfo.endCursor}";
            let isLoading = false;

            function getMoreMovies() {
                if (isLoading) return;
                isLoading = true;
                
                const currentParams = new URLSearchParams(window.location.search);
                let currentTitle = currentParams.get('title');
                
                let formData = new URLSearchParams();
                if (currentTitle) formData.append("title", currentTitle);
                if (lastCursor) formData.append("cursor", lastCursor);
                
                fetch("/moreSearchResults?" + formData.toString(), {
                    method: "POST"
                })
                .then(response => {
                  if (!response.ok) {
                    throw new Error(`Server error: ${response.status}`);
                  }
                  return response.json();
                }).then(data => {
                  document.querySelector(".movie-list").insertAdjacentHTML( 'beforeend', data.html );
                  hasNextPage = data.hasNextPage;
                  lastCursor = data.cursor;
                })
                .catch(error => {
                  console.error("Fetch error:", error);
                })
                .finally(() => {
                    isLoading = false;
                });
            }
            
            window.addEventListener("scroll", () => {
                if (document.documentElement.scrollTop + document.documentElement.clientHeight >= document.documentElement.scrollHeight
                        && hasNextPage) {
                    getMoreMovies();
                }
            });
          </script>
    """.trimIndent()

    return SearchBar(title) + showMoreResults + resultContent
}

@Serializable
data class MoreSearchResultsResponse(
    val cursor: String,
    val html: String,
    val hasNextPage: Boolean
)

suspend inline fun MoreSearchResults(title: String, cursor: String?): MoreSearchResultsResponse {
    val searchResults = CachedMovies.search(title, cursor)

    if (searchResults.edges.isEmpty()) {
        return MoreSearchResultsResponse("", "", false)
    }

    val mediaEntries = searchResults.edges.map {
        CachedMovies.Movie(it.node, false, System.currentTimeMillis())
    }
    return MoreSearchResultsResponse(
        searchResults.pageInfo.endCursor,
        MovieList(mediaEntries).listElements(),
        searchResults.pageInfo.hasNextPage
    )
}

suspend inline fun BookmarkPage(displayHidden: Boolean = false): String {
    val movies = if (displayHidden) CachedMovies.getAllBookmarkedMovies()
                                                    else CachedMovies.getBookmarkedMovies()

    // language=HTML
    return "<h1>Bookmarked Movies:</h1>" +  if (movies.isEmpty()) {
        "<p>No bookmarked movies found</p>"
    } else """
        <form method="post" action="/roulette">
            <button type="submit" class="roulette-button" disabled>Roulette</button>
            ${MovieList(movies).selectableList()}
        </form>
    """.trimIndent()
}

fun RoulettePage(movies: List<CachedMovies.Movie>): String {
    // language=HTML
    return """
        <form action="/roulette/submit" method="post">
            <ul class="movie-list">
                ${movies.joinToString("\n") { MovieItem(it).rouletteListItem() }}
            </ul>
            <button type="submit" class="roulette-button">Start Roulette</button>
        </form>
    """.trimIndent()
}

suspend inline fun HomePage(): String = SearchBar(null) + BookmarkPage()

val wheelOfNames = WheelOfNames(System.getenv("WHEEL_OF_NAMES_API_KEY") ?: throw IllegalStateException("WHEEL_OF_NAMES_API_KEY not set"))
fun Route.miscRoutes() {
    get("/") {
        call.respondText(
            contentType = ContentType.parse("text/html"),
            text = htmlTemplate(
                title = "Welcome",
                body = HomePage(),
                Nav = ::NavBar
            )
        )
    }
    get("/search") {
        val title = call.request.queryParameters["title"]?.escapeHTML()
        val numResults = call.request.queryParameters["num"]?.toIntOrNull() ?: 4

        call.respondText(
            contentType = ContentType.parse("text/html"),
            text = htmlTemplate(
                title = "$title Search",
                body = SearchPage(title, numResults),
                Nav = ::NavBar,
            )
        )
    }
    post("/moreSearchResults") {
        val title = call.request.queryParameters["title"]?.escapeHTML()
        val cursor = call.request.queryParameters["cursor"]?.escapeHTML()

        if (title.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Missing title parameter")
            return@post
        }

        call.respond(MoreSearchResults(title, cursor))
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
        call.respondText(
            contentType = ContentType.parse("text/html"),
            text = htmlTemplate(
                title = "Bookmarked Movies",
                body = BookmarkPage(displayHidden),
                Nav = ::NavBar
            )
        )
    }
    get("/watched") {
        val watchedMovies = CachedMovies.getWatchedMovies()
        call.respondText(
            contentType = ContentType.parse("text/html"),
            text = htmlTemplate(
                title = "Watched Movies",
                body = if (watchedMovies.isEmpty()) {
                    "<p>No watched movies found</p>"
                } else {
                    "<h1>Watched Movies:</h1>" + MovieList(watchedMovies).basicList()
                },
                Nav = ::NavBar
            )
        )
    }
    post("/roulette") {
        val selectedMovies = call.receiveParameters().getAll("selected[]")?.mapNotNull { CachedMovies.get(it) } ?: emptyList()

        if (selectedMovies.isEmpty() || selectedMovies.size < 2) {
            call.respond(HttpStatusCode.BadRequest, "Not enough movies selected for roulette")
            return@post
        }

        call.respondText(
            contentType = ContentType.parse("text/html"),
            text = htmlTemplate(
                title = "Roulette",
                body = RoulettePage(selectedMovies),
                Nav = ::NavBar
            )
        )
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

fun hostServer() {
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
        }
    }.start(wait = true)
}

fun main() {
    hostServer()
}