package de.amklee.monomovie

import de.amklee.monomovie.components.MovieListItem
import de.amklee.monomovie.components.SelectableMovieListItem
import de.amklee.monomovie.dto.SseEvent
import de.amklee.monomovie.util.MmvHttpClient
import de.amklee.monomovie.util.ful
import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.browser.document
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.w3c.dom.HTMLElement
import org.w3c.dom.set

object Sse {
    fun setup(mode: SseEvent.Mode) {
        val client = MmvHttpClient {
            install(SSE) {
                maxReconnectionAttempts = 4
                reconnectionTime = 4.seconds
            }
        }

        val bookmarkList = document.getElementById("movie-list") as HTMLElement?
        client.subscribeSse<SseEvent>("/sse-stream") { event ->
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
                        handleAdd(bookmarkList, mode, event)
                    }
                }

                is SseEvent.Remove -> {
                    elements.forEachElement { it.dataset["checked"] = "false" }
                }
            }
        }
    }

    private suspend fun handleAdd(bookmarkList: HTMLElement, mode: SseEvent.Mode, event: SseEvent.Add) {
        when (mode) {
            SseEvent.Mode.SEARCH, SseEvent.Mode.ROULETTE, SseEvent.Mode.OFFERS -> return
            SseEvent.Mode.WATCHED -> if (event.kind != SseEvent.Kind.WATCHED) return
            SseEvent.Mode.OVERVIEW -> if (event.kind != SseEvent.Kind.BOOKMARK) return
        }
        console.log("Prepending new movie to list: ${event.movie.title} (${event.movie.id})")
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
