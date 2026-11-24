package de.amklee.monomovie

import de.amklee.monomovie.components.MovieListItem
import de.amklee.monomovie.components.SelectableMovieListItem
import de.amklee.monomovie.dto.SseEvent
import de.amklee.monomovie.util.MmvHttpClient
import de.amklee.monomovie.util.ful
import io.ktor.client.plugins.sse.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.set

class Sse(private val mode: SseEvent.Mode) {
    private val client = MmvHttpClient {
        install(SSE) {
            maxReconnectionAttempts = 4
            reconnectionTime = 4.seconds
        }
    }

    fun setup() {
        val bookmarkList = document.getElementById("movie-list") as HTMLElement?
        client.subscribeSse(Route.SseStream()) { event: SseEvent ->
            val elements = document.querySelectorAll("#${event.kind.name.lowercase()}-${event.id}")
            console.info("Received SSE event, active elements: ${elements.length}, mode is $mode", event)

            if (mode == SseEvent.Mode.ROULETTE && event.kind == SseEvent.Kind.BOOKMARK) return@subscribeSse

            when (event) {
                is SseEvent.Add -> {
                    if (elements.length > 0) {
                        elements.forEachElement {
                            it.dataset["checked"] = "true"
                        }
                    } else if (bookmarkList != null) {
                        handleAdd(bookmarkList, event)
                    }
                }

                is SseEvent.Remove -> {
                    elements.forEachElement { it.dataset["checked"] = "false" }
                }
            }
        }
    }

    private fun handleAdd(bookmarkList: HTMLElement, event: SseEvent.Add) {
        when (mode) {
            SseEvent.Mode.SEARCH, SseEvent.Mode.ROULETTE, SseEvent.Mode.OFFERS -> return
            SseEvent.Mode.WATCHED -> if (event.kind != SseEvent.Kind.WATCHED) return
            SseEvent.Mode.OVERVIEW -> if (event.kind != SseEvent.Kind.BOOKMARK) return
        }
        console.log("Prepending new movie to list: ${event.movie.title} (${event.movie.id})")
        bookmarkList.prepend {
            ful {
                if (mode == SseEvent.Mode.OVERVIEW) {
                    SelectableMovieListItem(event.movie)
                } else {
                    MovieListItem(event.movie)
                }
            }
        }
    }
}
