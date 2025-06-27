package de.amklee.monomovie

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

val bannedTypes = setOf("BUY", "RENT")

fun htmlTemplate(title: String, body: String, nav: String): String {
//                ${Resources.style}
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <title>$title</title>
            <style>
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

val justWatch = JustWatch(country = "DE", language = "en")

suspend fun searchForMovie(title: String?): String {
    if (title == null) return "<p>No results found!</p>"

    println("Title: $title")

    var list = ""
    val searchResult = justWatch.search(title)

    for (result in searchResult) {
        list += "<li>${result.content?.title} (${result.content?.originalReleaseYear})</li>"
    }

    return "<ul>$list</ul>"
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

suspend fun main() {
//    val justWatch = JustWatch(country = "DE", language = "en")
//
//    val searchResult = justWatch.search("12 angry men")
//
//
//    val bestFitMovie = searchResult[0]
//    println("Best fit movie: ${bestFitMovie.content?.title}")
//    val offers = bestFitMovie.offers?.filter { it.monetizationType !in bannedTypes } ?: emptyList()
//
////    println(justWatch.details(bestFitMovie.id!!) == bestFitMovie)
//
//    for (offer in offers) {
//        println("Found offer: ${offer.`package`?.clearName} at ${offer.standardWebURL}, presentationType: ${offer.presentationType}, monetizationType: ${offer.monetizationType}")
//    }
    hostServer()

}