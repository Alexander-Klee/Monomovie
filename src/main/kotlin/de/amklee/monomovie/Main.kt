package de.amklee.monomovie

import io.gitlab.jfronny.commons.logger.SystemLoggerPlus
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import org.intellij.lang.annotations.Language
import java.io.FileNotFoundException
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

object Resources {
    private val styleResource: String by lazy {
        Resources::class.java.getResource("/style.css")?.readText()
            ?: throw FileNotFoundException("style.css not found in resources")
    }
    private val styleFile = Path("src/main/resources/style.css").takeIf { it.exists() }
    val style: String get() = styleFile?.readText() ?: styleResource
}

val bannedTypes = setOf("BUY", "RENT")

fun htmlTemplate(title: String, body: String, nav: String): String {
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>$title</title>
            <style>
                ${Resources.style}
            </style>
        </head>
        <body>
            <nav>
                $nav
            </nav>
            <main>
                $body
            </main>
        </body>
        </html>
        """.trimIndent()
}

fun NavBar(): String {
    return """
        <nav>
            <h1>Movies</h1>
            <ul class="nav-list">
                <li><a href="/">Home</a></li>
                <li><a href="/search">Search</a></li>
                <li><a href="/bookmarks">Bookmarks</a></li>
                <li><a href="https://jellyfin.amklee.de">Jellyfin</a></li>
                <li><a href="https://amklee.de/recipe">Recipes</a></li>
            </ul>
        </nav>
    """.trimIndent()
}

fun MovieList(movies: List<CachedMovies.Movie>): String {
    fun getOffers(movie: CachedMovies.Movie): String {
        val offers = movie.mediaEntry.offers?.filter { it.monetizationType !in bannedTypes } ?: emptyList()

        val offerHtml = offers.joinToString("\n") { offer ->
            val iconUrl = "https://images.justwatch.com${offer.`package`?.icon}"
            val altText = offer.`package`?.clearName ?: "Unknown"

            """
            <li class="offer-item">
                <img src="${iconUrl.escapeHTML()}" alt="${altText.escapeHTML()}" class="offer-icon"/>
            </li>
            """.trimIndent()
        }

        return """
            <ul class="offer-list">
                $offerHtml
            </ul>
            """.trimIndent()
    }

    fun renderMovieItem(movie: CachedMovies.Movie): String {
        return """
            <li class="movie-item bookmark-container">
                <span class="movie-poster">
                    <span class="bookmark-icon ${if (movie.isBookmarked) "bookmarked" else ""}" onclick="setBookmark('${movie.mediaEntry.id?.escapeHTML()}', this)"></span>
                    <img class="movie-poster" src="https://images.justwatch.com${movie.mediaEntry.content?.posterUrl?.escapeHTML()}" alt="${movie.mediaEntry.content?.title?.escapeHTML()}">
                </span>
                
                <div class="movie-details">
                    <p class="movie-title">${movie.mediaEntry.content?.title?.escapeHTML()}</p>
                    <p class="movie-year">${movie.mediaEntry.content?.originalReleaseYear}</p>
                    <p class = "movie-short-description" onclick="this.classList.add('expanded')">${movie.mediaEntry.content?.shortDescription?.escapeHTML()}</p>
                </div>
                
                <div class="movie-offers">
                    ${getOffers(movie)}
                </div>
            </li>
            """.trimIndent()
    }

    @Language("HTML")
    val bookmarkJS = $$"""
        <script>
        function setBookmark(movieId, el) {
            fetch(`/bookmark/${movieId}`, {
                method: 'POST',
            })
            .then(() => {
                console.log("Bookmarking movie with ID: " + movieId);
                el.classList.toggle('bookmarked');
            })
            .catch(error => {
                 console.error("Bookmark error:", error);
            });
        }
        </script>
    """.trimIndent()

    val movieList = """
        <ul class="movie-list">
            ${movies.joinToString("\n") { movie -> renderMovieItem(movie) }}
        </ul>
    """.trimIndent()

    return bookmarkJS + movieList
}

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

suspend fun SearchPage(title: String?, numResults: Int = 4): String {
    val searchResult = if (title.isNullOrBlank()) emptyList() else CachedMovies.search(title, numResults)

    return SearchBar(title) + if (searchResult.isEmpty()) {
        "<p>No Search Results</p>"
    } else {
        $$"""
            <h4>Search results:</h4>
            $${MovieList(searchResult)}
        """.trimIndent()
    }
}

suspend fun BookmarkPage(): String {
    val bookmarkedMovies = CachedMovies.getBookmarkedMovies()
    val list = MovieList(bookmarkedMovies)

    return "<h1>Bookmarked Movies:</h1>" +  if (bookmarkedMovies.isEmpty()) {
        "<p>No bookmarked movies found</p>"
    } else {
        list
    }
}

suspend fun HomePage(): String = SearchBar(null) + BookmarkPage()

fun Route.miscRoutes() {
    get("/") {
        call.respondText(
            contentType = ContentType.parse("text/html"),
            text = htmlTemplate(
                title = "Welcome",
                body = HomePage(),
                nav = NavBar()
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
                nav = NavBar(),
            )
        )
    }
    post("/bookmark/{movieId}") {
        val movieId = call.parameters["movieId"]

        if (movieId == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing bookmark ID")
            return@post
        }

        CachedMovies.toggleBookmark(movieId)

        call.respond(HttpStatusCode.OK)
    }
    get("/bookmarks") {
        call.respondText(
            contentType = ContentType.parse("text/html"),
            text = htmlTemplate(
                title = "Bookmarked Movies",
                body = BookmarkPage(),
                nav = NavBar()
            )
        )
    }
}

fun hostServer() {
    embeddedServer(Netty, port = 8080) {
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