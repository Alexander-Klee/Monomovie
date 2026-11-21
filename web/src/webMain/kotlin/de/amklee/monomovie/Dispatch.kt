package de.amklee.monomovie

import de.amklee.monomovie.dto.JsCall
import de.amklee.monomovie.util.MmvHttpClient
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.browser.document
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.get

@OptIn(DelicateCoroutinesApi::class)
fun JsCall.dispatch() {
    val client = MmvHttpClient()

    try {
        when (this) {
            is JsCall.SetupSse -> document.addEventListener("DOMContentLoaded") {
                Sse.setup(mode)
            }

            is JsCall.SetBookmarked -> GlobalScope.launch {
                client.request("/bookmark/$movieId") {
                    method = if (bookmark) HttpMethod.Put else HttpMethod.Delete
                }
            }

            is JsCall.SetWatched -> GlobalScope.launch {
                client.request("/watch/$movieId") {
                    method = if (watched) HttpMethod.Put else HttpMethod.Delete
                }
            }

            JsCall.InjectTopButton -> document.addEventListener("DOMContentLoaded") {
                TopButton.inject()
            }

            is JsCall.UpdateMovieSelectionCount -> {
                val selected = document.querySelectorAll(".movie-checkbox:checked").length

                document.querySelectorAll(".require-min-selection").forEach {
                    if (it !is HTMLInputElement) return@forEach
                    console.log(it.dataset)
                    val minSelection = it.dataset["minSelection"]?.toIntOrNull()
                    if (minSelection == null) {
                        console.error("Invalid min-selection value")
                        return@forEach
                    }
                    val disabled = selected < minSelection
                    it.disabled = disabled
                }
            }
        }
    } catch (e: Exception) {
        console.error("Error dispatching JsCall: $this", e)
        e.printStackTrace()
        throw e
    }
}
