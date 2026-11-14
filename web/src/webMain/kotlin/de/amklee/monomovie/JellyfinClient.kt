package de.amklee.monomovie

import de.amklee.monomovie.dto.JellyfinItems
import de.amklee.monomovie.util.NIHCache
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlin.time.Duration.Companion.hours

object JellyfinClient {
    private val client = HttpClient()

    private val items = NIHCache<JellyfinItems>(3.hours) {
        client.get("/jellyfinItems").body<JellyfinItems>()
    }
    suspend fun getItems(): JellyfinItems = items.get()
}
