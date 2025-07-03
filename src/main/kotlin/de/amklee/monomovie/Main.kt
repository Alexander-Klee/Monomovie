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

inline fun htmlTemplate(title: String, body: String, Nav: () -> String): String {
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <meta name="color-scheme" content="dark light" />
            <title>$title</title>
            <style>
                ${Resources.style}
            </style>
        </head>
        <body>
            <button class="menu-button" onclick="showMenu()">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512"><!--!Font Awesome Free 6.7.2 by @fontawesome - https://fontawesome.com License - https://fontawesome.com/license/free Copyright 2025 Fonticons, Inc.--><path d="M0 96C0 78.3 14.3 64 32 64l384 0c17.7 0 32 14.3 32 32s-14.3 32-32 32L32 128C14.3 128 0 113.7 0 96zM0 256c0-17.7 14.3-32 32-32l384 0c17.7 0 32 14.3 32 32s-14.3 32-32 32L32 288c-17.7 0-32-14.3-32-32zM448 416c0 17.7-14.3 32-32 32L32 448c-17.7 0-32-14.3-32-32s14.3-32 32-32l384 0c17.7 0 32 14.3 32 32z"/></svg>
            </button>
            <script>
                function showMenu() {
                    const nav = document.querySelector('nav');
                    nav.classList.toggle('nav-visible');
                    document.body.classList.toggle('menu-open', nav.classList.contains('nav-visible'));
                }
                function closeMenu() {
                    const nav = document.querySelector('nav');
                    if (nav.classList.contains('nav-visible')) {
                        nav.classList.remove('nav-visible');
                        document.body.classList.remove('menu-open');
                    }
                }
            </script>
            ${Nav()}
            <main onclick="closeMenu()">
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

fun MovieItem(movie: CachedMovies.Movie, prefix: String = ""): String {
    fun getOffers(movie: CachedMovies.Movie): String {
        val offers = movie.mediaEntry.offers?.filter { it.monetizationType !in bannedTypes } ?: emptyList()

        val offerHtml = offers.joinToString("\n") { offer ->
            val iconUrl = "https://images.justwatch.com${offer.`package`?.icon}"
            val altText = offer.`package`?.clearName ?: "Unknown"
            val link = offer.standardWebURL ?: ""

            """
            <li class="offer-item">
                <a href="${link.escapeHTML()}">
                    <img src="${iconUrl.escapeHTML()}" alt="${altText.escapeHTML()}" class="offer-icon"/>
                </a>
            </li>
            """.trimIndent()
        }

        return """
            <ul class="offer-list">
                $offerHtml
            </ul>
            """.trimIndent()
    }

    val cssClass = if (movie.isBookmarked) "bookmarked" else ""
    val id = movie.mediaEntry.id?.escapeHTML()
    val posterUrl = movie.mediaEntry.content?.posterUrl?.escapeHTML()
    val movieTitle = movie.mediaEntry.content?.title?.escapeHTML()
    val movieYear = movie.mediaEntry.content?.originalReleaseYear
    val movieDesc = movie.mediaEntry.content?.shortDescription?.escapeHTML()

    // language=HTML
    return """
        <li class="movie-list-item">
            $prefix
            <div class="movie-item bookmark-container">
                <span class="movie-poster $cssClass" onclick="setBookmark('$id', this)">
                    <span class="bookmark-icon"></span>
                    <img class="movie-poster" src="https://images.justwatch.com$posterUrl" alt="$movieTitle">
                </span>
                
                <div class="movie-details">
                    <p class="movie-title">$movieTitle</p>
                    <p class="movie-year">$movieYear</p>
                    <p class="movie-short-description" onclick="this.classList.add('expanded')">$movieDesc</p>
                </div>
                
                <div class="movie-offers">
                    ${getOffers(movie)}
                </div>
            </div>
        </li>
        """.trimIndent()
}

fun MovieList(movies: List<CachedMovies.Movie>, selectable: Boolean): String {
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
        
        $${if (selectable) """
            function selectedChanged() {
                const disabled = document.querySelectorAll(".movie-checkbox:checked").length <= 1;
                
                document.querySelectorAll(".roulette-button").forEach(button => {
                    button.disabled = disabled;
                });
            }
            
            selectedChanged();
        """.trimIndent() else ""}
        </script>
    """.trimIndent()

    val movieList = """
        <ul class="movie-list">
            ${movies.joinToString("\n") { movie -> MovieItem(
                movie,
                prefix = if (selectable) """<input type="checkbox" class="movie-checkbox" name="selected[]" value="${movie.mediaEntry.id?.escapeHTML()}" onchange="selectedChanged()">""" else ""
            ) }}
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

suspend inline fun SearchPage(title: String?, numResults: Int = 4): String {
    val searchResult = if (title.isNullOrBlank()) emptyList() else CachedMovies.search(title, numResults)

    return SearchBar(title) + if (searchResult.isEmpty()) {
        "<p>No Search Results</p>"
    } else {
        $$"""
            <h4>Search results:</h4>
            $${MovieList(searchResult, false)}
        """.trimIndent()
    }
}

suspend inline fun BookmarkPage(): String {
    val bookmarkedMovies = CachedMovies.getBookmarkedMovies()

    return "<h1>Bookmarked Movies:</h1>" +  if (bookmarkedMovies.isEmpty()) {
        "<p>No bookmarked movies found</p>"
    } else """
        <form method="post" action="/roulette">
            <button type="submit" class="roulette-button" disabled>Roulette</button>
            ${MovieList(bookmarkedMovies, true)}
        </form>
    """.trimIndent()
}

fun RoulettePage(movies: List<CachedMovies.Movie>): String {
    return """
        <form action="/roulette/submit" method="post">
            <ul class="movie-list">
                ${movies.joinToString("\n") { movie -> MovieItem(
                    movie,
                    prefix = """<input type="number" class="roulette-weight" name="${movie.mediaEntry.id?.escapeHTML()}" min="1" value = "1">"""
                ) }}
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