package de.amklee.monomovie

import de.amklee.monomovie.dto.JsCall
import de.amklee.monomovie.util.MmvHttpClient
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.browser.document
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
fun JsCall.dispatch() {
    val client = MmvHttpClient()

    try {
        when (this) {
            is JsCall.SetupSse -> document.addEventListener("DOMContentLoaded") {
                Sse.setup(mode)
            }

            is JsCall.SetBookmarked -> {
                GlobalScope.launch {
                    client.request("/bookmark/$movieId") {
                        method = if (bookmark) HttpMethod.Put else HttpMethod.Delete
                    }
                }
            }
            is JsCall.SetWatched -> {
                GlobalScope.launch {
                    client.request("/watch/$movieId") {
                        method = if (watched) HttpMethod.Put else HttpMethod.Delete
                    }
                }
            }
        }
    } catch (e: Exception) {
        println("Error dispatching JsCall: $this")
        e.printStackTrace()
        throw e
    }
}
