package de.amklee.monomovie

import de.amklee.monomovie.CachedMovies.getOffers
import io.gitlab.jfronny.commons.logger.SystemLoggerPlus
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
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
import kotlinx.serialization.Serializable
import kotlin.io.path.readText

object Resources {
    private val styleResource: String by lazy {
        Resources::class.java.getResource("/style.css")?.readText()
            ?: throw FileNotFoundException("style.css not found in resources")
    }
    private val styleFile = Path("src/main/resources/style.css").takeIf { it.exists() }
    val style: String get() = styleFile?.readText() ?: styleResource
}

inline fun htmlTemplate(title: String, body: String, Nav: () -> String): String {
    // language=HTML
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <meta name="color-scheme" content="dark light" />
            <title>$title</title>
            
            <meta property="og:title" content="$title" />
            <meta property="og:type" content="website" />
            <meta property="og:url" content="https://mmv.amklee.de/" />
            <meta property="og:site_name" content="Monomovie" />
            <meta property="og:description" content="Discover, bookmark and select movies for playback." />
            <meta property="og:image" content="https://mmv.amklee.de/og-image.png" />
            
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
    // language=HTML
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

fun MovieItem(movie: CachedMovies.Movie, movieItemWrapper: (String) -> String = { it }): String {
    fun getOffers(movie: CachedMovies.Movie): String {
        val offers = movie.getOffers()

        val offerHtml = offers.joinToString("\n") { offer ->
            val iconUrl = "https://images.justwatch.com${offer.`package`?.icon}"
            val altText = offer.`package`?.clearName ?: "Unknown"
            val link = offer.standardWebURL ?: ""

            // language=HTML
            """
            <li class="offer-item">
                <a href="${link.escapeHTML()}">
                    <img src="${iconUrl.escapeHTML()}" alt="${altText.escapeHTML()}" class="offer-icon"/>
                </a>
            </li>
            """.trimIndent()
        }

        // language=HTML
        return """
            <ul class="offer-list">
                $offerHtml
            </ul>
            """.trimIndent()
    }

    fun getRatings(movie: CachedMovies.Movie): String {
        fun formatScore(score: Float): String = String.format("%.1f", score)

        fun linkWrapper(link: String?, content: String?): String {
            if (content.isNullOrBlank()) return ""
            if (link.isNullOrBlank()) return content
            // language=HTML
            return """
                <a href="$link" target="_blank" class="no-link-style" rel="noopener noreferrer">
                    $content
                </a>
                """.trimIndent()
        }

        val tomatoRating = movie.mediaEntry.content?.scoring?.tomatoMeter?.let { score ->
            // language=HTML
            """
            <div class="movie-rating">
                <i class="tomato-icon rating-logo"></i>
                <p>$score%</p>
            </div>
            """.trimIndent() } ?: ""

        val imdbLink = movie.mediaEntry.content?.externalIds?.imdbId?.let { id -> "https://www.imdb.com/title/$id" }
        val imdbRating = movie.mediaEntry.content?.scoring?.imdbScore?.let { score ->
            // language=HTML
            """
            <div class="movie-rating">
                <i class="imdb-icon rating-logo"></i>
                <p>${formatScore(score)}</p>
            </div>
            """.trimIndent() } ?: ""

        val tmdbLinkType = when (movie.mediaEntry.objectType?.lowercase()) {
            "movie" -> "movie"
            "show" -> "tv"
            else -> "movie" // Default
        }
        val tmdbLink = movie.mediaEntry.content?.externalIds?.tmdbId?.let { id -> "https://www.themoviedb.org/$tmdbLinkType/$id" }
        val tmdbRating = movie.mediaEntry.content?.scoring?.tmdbScore?.let { score ->
            // language=HTML
            """
            <div class="movie-rating">
                <i class="tmdb-icon rating-logo"></i>
                <p>${formatScore(score)}</p>
            </div>
            """.trimIndent() } ?: ""

        return tomatoRating + linkWrapper(imdbLink, imdbRating) + linkWrapper(tmdbLink, tmdbRating)
    }

    val cssClass = if (movie.isBookmarked) "bookmarked" else ""
    val movieId = movie.mediaEntry.id?.escapeHTML()
    val posterUrl = movie.mediaEntry.content?.posterUrl?.escapeHTML()
    val movieTitle = movie.mediaEntry.content?.title?.escapeHTML()
    val movieYear = movie.mediaEntry.content?.originalReleaseYear
    val movieDesc = movie.mediaEntry.content?.shortDescription?.escapeHTML()

    // language=HTML
    return """
        <li class="movie-list-item">
           ${movieItemWrapper("""
               <div class="movie-item bookmark-container">
                    <span class="movie-poster $cssClass" onclick="return bookmark('$movieId', this, false)" ondblclick="bookmark('$movieId', this, true)">
                        <span class="bookmark-icon"></span>
                        <img class="movie-poster" src="https://images.justwatch.com$posterUrl" alt="$movieTitle">
                    </span>
                    
                    <div class="movie-details">
                        <div class="movie-title-bar">
                           <p class="movie-title">$movieTitle</p>
                           <div class="movie-rating-container">
                               ${getRatings(movie)}
                           </div>
                        </div>
                        <p class="movie-year">$movieYear</p>
                        <p class="movie-short-description" onclick="this.classList.add('expanded')">$movieDesc</p>
                    </div>
                    
                    <div class="movie-offers">
                        ${getOffers(movie)}
                    </div>
                </div>
           """.trimIndent())}
        </li>
        """.trimIndent()
}

fun MovieListElements(movies: List<CachedMovies.Movie>, selectable: Boolean): String = movies.joinToString("\n") { movie -> MovieItem(
        movie,
        movieItemWrapper = if (!selectable) { it -> it } else { it ->
            // language=HTML
            """
                <label for="${movie.mediaEntry.id?.escapeHTML()}">
                    <input type="checkbox" class="movie-checkbox" name="selected[]"
                        value="${movie.mediaEntry.id?.escapeHTML()}" id="${movie.mediaEntry.id?.escapeHTML()}"
                        onchange="selectedChanged()">
                    $it
                </label>
            """.trimIndent()
        },
    ) }

fun MovieList(movies: List<CachedMovies.Movie>, selectable: Boolean): String {
    @Language("HTML")
    val bookmarkJS = $$"""
        <script>
        function bookmark(movieId, el, isDoubleClick) {
            const isSmallScreen = window.matchMedia("(max-width: 600px)").matches;
            if (isSmallScreen !== isDoubleClick) return;
            document.querySelectorAll("#" + movieId).forEach(checkbox => {
                checkbox.checked = false;
            });
        
            if (el.classList.contains('bookmarked')) {
                deleteBookmark(movieId, el);
            } else {
                setBookmark(movieId, el);
            }
        
            return false; // prevent default action
        }
        
        function setBookmark(movieId, el) {
            fetch(`/bookmark/${movieId}`, {
                method: 'POST',
            })
            .then(() => {
                el.classList.add('bookmarked');
            })
            .catch(error => {
                 console.error("Bookmark error:", error);
            });
        }
        
        function deleteBookmark(movieId, el) {
            fetch(`/bookmark/${movieId}`, {
                method: 'DELETE',
            })
            .then(() => {
                el.classList.remove('bookmarked');
            })
            .catch(error => {
                 console.error("Delete bookmark error:", error);
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
            ${MovieListElements(movies, selectable)}
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
        $"""
            <h4>Search results:</h4>
            ${MovieList(mediaEntries, false)}
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
    return MoreSearchResultsResponse(searchResults.pageInfo.endCursor, MovieListElements(mediaEntries, false), searchResults.pageInfo.hasNextPage)
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
            ${MovieList(movies, !displayHidden)}
        </form>
    """.trimIndent()
}

fun RoulettePage(movies: List<CachedMovies.Movie>): String {
    // language=HTML
    return """
        <form action="/roulette/submit" method="post">
            <ul class="movie-list">
                ${movies.joinToString("\n") { movie -> MovieItem(
                    movie,
                    movieItemWrapper = { it -> """
                        <label for="${movie.mediaEntry.id?.escapeHTML()}">
                            <input type="number" class="roulette-weight"
                                name="${movie.mediaEntry.id?.escapeHTML()}" id="${movie.mediaEntry.id?.escapeHTML()}"
                                min="1" value="1">
                            $it
                        </label>
                        """.trimIndent()
                    }
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
                    "<h1>Watched Movies:</h1>" + MovieList(watchedMovies, false)
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