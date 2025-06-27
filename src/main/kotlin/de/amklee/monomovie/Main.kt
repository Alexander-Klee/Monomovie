package de.amklee.monomovie

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.escapeHTML
import java.io.File
import java.io.FileNotFoundException

object Resources {
    val style: String by lazy {
        Resources::class.java.getResource("/style.css")?.readText()
            ?: throw FileNotFoundException("style.css not found in resources")
    }
    private val stylePath = "src/main/resources/style.css" // or the full path to your style.css

    val styleAutoUpdate: String
        get() = File(stylePath).takeIf { it.exists() }?.readText()
            ?: throw FileNotFoundException("style.css not found at path: $stylePath")
}

val bannedTypes = setOf("BUY", "RENT")

fun htmlTemplate(title: String, body: String, nav: String): String {
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <title>$title</title>
            <style>
                ${Resources.styleAutoUpdate}
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

suspend fun searchForMovie(title: String?): String {
    println("Title: $title")

    fun getOffers(movie: MediaEntry): String {
        val offers = movie.offers?.filter { it.monetizationType !in bannedTypes } ?: emptyList()

        val offerHtml = offers.joinToString("\n") { offer ->
            val iconUrl = "https://images.justwatch.com${offer.`package`?.icon?.escapeHTML()}"
            val altText = offer.`package`?.clearName ?: "Unknown"

            """
            <li class="offer-item">
                <img src="$iconUrl" alt="$altText" class="offer-icon"/>
            </li>
            """.trimIndent()
        }

        return """
            <ul class="offer-list">
                $offerHtml
            </ul>
            """.trimIndent()
    }

    val justWatch = JustWatch(country = "DE", language = "en")
    val searchResult = if (title.isNullOrBlank()) emptyList() else justWatch.search(title = title)

    val list = searchResult.joinToString("\n") { movie ->
            """
            <li class="movie-item">
                <img src="https://images.justwatch.com${movie.content?.posterUrl?.escapeHTML()}" alt="${movie.content?.title?.escapeHTML()}" class="movie-poster"/>
                <div class="movie-details">
                    <p class="movie-title">${movie.content?.title?.escapeHTML()}</p>
                    <p class="movie-year">${movie.content?.originalReleaseYear}</p>
                    <p class = "movie-short-description" onclick="this.classList.add('expanded')">${movie.content?.shortDescription?.escapeHTML()}</p>
                </div>
                <div class="movie-offers">
                    ${getOffers(movie)}
                </div>
            </li>
            """.trimIndent()
    }

    // TODO; this is a trivial XSS
    return """
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
        </div>
        ${if (list.isNotEmpty()) "<h4>Search results:</h4>" else ""}
        <ul class="movie-list">$list</ul>
    """.trimIndent()
}

fun Route.miscRoutes() {
    get("/") {
        call.respondText(
            contentType = ContentType.parse("text/html"),
            text = htmlTemplate(
                title = "Welcome",
                body = "<p>Not much to see here yet</p>",
                nav = "<p>empty</p>"
            )
        )
    }

    get("/search") {
        call.respondText(
            contentType = ContentType.parse("text/html"),
            text = htmlTemplate(
                title = "Search",
                body = searchForMovie(call.request.queryParameters["title"]),
                nav = "<p>empty</p>",
            )
        )
    }
}

fun hostServer() {
    embeddedServer(Netty, port = 8080) {
        routing {
            miscRoutes()
        }
    }.start(wait = true)
}

fun main() {
////    println(justWatch.details(bestFitMovie.id!!) == bestFitMovie)
    hostServer()
}