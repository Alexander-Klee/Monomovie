package de.amklee.monomovie.service.remote

import de.amklee.monomovie.HttpClient
import de.amklee.monomovie.dto.Movie
import de.amklee.monomovie.dto.tmdbLink
import de.amklee.monomovie.service.BookmarksService
import de.amklee.monomovie.service.CachedMovies
import de.amklee.monomovie.util.error
import de.amklee.monomovie.util.info
import de.amklee.monomovie.util.warn
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

object TmColour {
    private val log = System.getLogger("MMV/TmColour")
    private val client = HttpClient()

    suspend operator fun get(movie: Movie): String? {
        val bookmark = BookmarksService.getBookmarks().firstOrNull { it.id == movie.mediaEntry.id }

        if (bookmark?.colour != null) return bookmark.colour

        log.info { "Fetching colour for movie id ${movie.mediaEntry.id} from TMDB" }

        val tmdbLink = movie.mediaEntry.tmdbLink ?: return null
        val cssPhrase: String
        try {
            cssPhrase =
                client
                    .get(tmdbLink) {
                        contentType(ContentType.Text.Html.withCharset(Charsets.UTF_8))
                    }.bodyAsText()
                    .removeUntil("div.custom_bg {")
                    .removeAfter("}")
                    .trim()
        } catch (e: Exception) {
            log.error(e) { "Unable to fetch TMDB page for colour extraction: $tmdbLink" }
            return null
        }
        val backgroundImage =
            cssPhrase
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
            BookmarksService.setColour(movie.mediaEntry.id!!, colour)
        } else {
            log.warn {
                "Unable to set colour, no bookmark found for movie id ${movie.mediaEntry.id}"
            }
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
