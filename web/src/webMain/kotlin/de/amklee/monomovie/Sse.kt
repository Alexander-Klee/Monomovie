package de.amklee.monomovie

import de.amklee.monomovie.components.MovieListItem
import de.amklee.monomovie.components.SelectableMovieListItem
import de.amklee.monomovie.dto.SseEvent
import de.amklee.monomovie.dto.title
import de.amklee.monomovie.util.ful
import io.ktor.client.plugins.sse.*
import kotlinx.browser.document
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.w3c.dom.HTMLElement
import org.w3c.dom.set
import kotlin.time.Duration.Companion.seconds

object Sse {
    fun setup(mode: SseEvent.Mode) {
        val client = HttpClient {
            install(SSE) {
                maxReconnectionAttempts = 4
                reconnectionTime = 4.seconds
            }
        }

        val bookmarkList = document.getElementById("movie-list") as HTMLElement?
        GlobalScope.launch {
            client.sse("/sse-stream") {
                incoming.collect {
                    if (it.data == "heartbeat") return@collect
                    handle(bookmarkList, mode, Json.decodeFromString<SseEvent>(it.data!!))
                }
            }
        }
    }

    private suspend fun handle(bookmarkList: HTMLElement?, mode: SseEvent.Mode, event: SseEvent) {
        val elements = document.querySelectorAll("#${event.kind.name.lowercase()}-${event.id}")
        println("Received SSE event: $event, active elements: ${elements.length}, mode is $mode")

        if (mode == SseEvent.Mode.ROULETTE && event.kind == SseEvent.Kind.BOOKMARK) return

        when (event) {
            is SseEvent.Add -> {
                if (elements.length > 0) {
                    elements.forEachElement {
                        it.dataset["checked"] = "true"
                    }
                } else if (bookmarkList != null) {
                    handleAdd(bookmarkList, mode, event)
                }
            }

            is SseEvent.Remove -> {
                elements.forEachElement { it.dataset["checked"] = "false" }
            }
        }
    }

    private suspend fun handleAdd(bookmarkList: HTMLElement, mode: SseEvent.Mode, event: SseEvent.Add) {
        when (mode) {
            SseEvent.Mode.SEARCH, SseEvent.Mode.ROULETTE, SseEvent.Mode.OFFERS -> return
            SseEvent.Mode.WATCHED -> if (event.kind != SseEvent.Kind.WATCHED) return
            SseEvent.Mode.OVERVIEW -> if (event.kind != SseEvent.Kind.BOOKMARK) return
        }
        println("Prepending new movie to list: ${event.movie.title} (${event.movie.id})")
        context(JellyfinClient.getItems()) {
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
}
