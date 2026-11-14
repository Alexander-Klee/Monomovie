package de.amklee.monomovie

import de.amklee.monomovie.dto.SseEvent
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.url
import kotlinx.browser.document
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.w3c.dom.*
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
            client.sse("/sse-stream?mode=${mode.name}") {
                incoming.collect {
                    if (it.data == "heartbeat") return@collect
                    handle(bookmarkList, mode, Json.decodeFromString<SseEvent>(it.data!!))
                }
            }
        }
    }

    private fun handle(bookmarkList: HTMLElement?, mode: SseEvent.Mode, event: SseEvent) {
        val elements = document.querySelectorAll("#${event.kind.name.lowercase()}-${event.id}")
        println("Received SSE event: $event, active elements: ${elements.length}")
        when (event) {
            is SseEvent.Add -> {
                if (elements.length > 0) {
                    elements.forEachElement {
                        it.dataset["checked"] = "true"
                    }
                } else if (bookmarkList != null && event.insert) {
                    bookmarkList.prepend(parseHtmlElement(event.body)!!)
                }
            }

            is SseEvent.Remove -> {
                elements.forEachElement { it.dataset["checked"] = "false" }
            }
        }
    }
}
