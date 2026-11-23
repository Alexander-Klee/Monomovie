package de.amklee.monomovie

import de.amklee.monomovie.components.RouletteMovieListItem
import de.amklee.monomovie.dto.RouletteSseEvent
import de.amklee.monomovie.util.MmvHttpClient
import de.amklee.monomovie.util.ful
import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid
import kotlinx.browser.document
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement

class RouletteVoting(private val shareId: Uuid) {
    private val client = MmvHttpClient {
        install(SSE) {
            maxReconnectionAttempts = 4
            reconnectionTime = 4.seconds
        }
    }

    fun setup() {
        injectUpdateCount(document.body!!)

        val bookmarkList = document.getElementById("movie-list") as HTMLElement?
        client.subscribeSse<RouletteSseEvent>("/roulette/shared/$shareId/sse") { event ->
            val elements = document.querySelectorAll("#${event.id}")
            console.info("Received SSE event, active elements: ${elements.length}", event)

            when (event) {
                is RouletteSseEvent.Add -> {
                    if (elements.length == 0 && bookmarkList != null) {
                        injectUpdateCount(
                            bookmarkList.append {
                                ful {
                                    RouletteMovieListItem(movie = event.item, count = event.count)
                                }
                            },
                        )
                    }
                }

                is RouletteSseEvent.Update -> elements.forEach {
                    (it as? HTMLInputElement)?.value = event.count.toString()
                }

                is RouletteSseEvent.Remove -> document.querySelectorAll("#roulette-${event.id}")
                    .forEachElement {
                        it.remove()
                    }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun injectUpdateCount(parent: HTMLElement) {
        parent.querySelectorAll(".roulette-weight-input").forEach { element ->
            (element as? HTMLInputElement)?.onchange = {
                val count = element.value.toIntOrNull() ?: 0
                GlobalScope.launch {
                    try {
                        client.post("/roulette/shared/$shareId/${element.id}") {
                            setBody(count.toString())
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) ensureActive()
                        console.error("Failed to update count", e)
                    }
                }
            }
        }
    }
}
