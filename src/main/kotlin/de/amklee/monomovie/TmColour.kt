package de.amklee.monomovie

import de.amklee.monomovie.db.BookmarksDB
import de.amklee.monomovie.util.error
import de.amklee.monomovie.util.warn
import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.net.http.HttpClient.Version as HttpClientVersion

object TmColour {
    private val log = System.getLogger("MMV/TmColour")

    private val client = HttpClient(Java) {
        engine {
            pipelining = true
            protocolVersion = HttpClientVersion.HTTP_2
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
    }

    suspend operator fun get(movie: CachedMovies.Movie): String? {
        val bookmark = BookmarksDB.getBookmarks().firstOrNull { it.id == movie.mediaEntry.id }

        if (bookmark != null && bookmark.colour != null) {
            return bookmark.colour
        }

        val tmdbLink = movie.mediaEntry.tmdbLink ?: return null
        val cssPhrase: String
        try {
            cssPhrase = client.get(tmdbLink) {
                contentType(ContentType.Text.Html.withCharset(Charsets.UTF_8))
            }.bodyAsText()
                .removeUntil("div.custom_bg {")
                .removeAfter("}")
                .trim()
        } catch (e: Exception) {
            log.error(e) { "Unable to fetch TMDB page for colour extraction: $tmdbLink" }
            return null
        }
        val backgroundImage = cssPhrase
            .split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("background-image:") }
            ?.removePrefix("background-image:")
            ?.trim() ?: return null
        if (!backgroundImage.startsWith("linear-gradient")) {
            log.warn { "Unable to find gradient background in: $cssPhrase" }
            return null
        }

        val rgba = backgroundImage.removeUntil("rgba(").removeAfter(")")
        if (!rgba.endsWith(", 1")) {
            log.warn { "Found non-opaque colour in: $cssPhrase" }
            return null
        }
        if (rgba.length > 7 * 4) { // "255.0, " * 4
            log.warn { "Found unexpected long rgba string: $rgba" }
            return null
        }

        val colour = "rgb(${rgba.removeSuffix(", 1").trim()})"

        if (bookmark != null) {
            BookmarksDB.setColour(movie.mediaEntry.id!!, colour)
        } else {
            log.warn { "Unable to set colour, no bookmark found for movie id ${movie.mediaEntry.id}" }
        }

        return colour
    }

    private fun String.removeUntil(divider: String): String {
        val index = this.indexOf(divider)
        return if (index >= 0) this.substring(index + divider.length) else this
    }

    private fun String.removeAfter(divider: String): String {
        val index = this.indexOf(divider)
        return if (index >= 0) this.substring(0, index) else this
    }
}
