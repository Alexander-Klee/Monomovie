package de.amklee.monomovie

import de.amklee.monomovie.util.NIHCache
import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.minutes
import java.net.http.HttpClient.Version as HttpClientVersion

/**
 * https://providence.jfronny.dev/?page=about
 */
object ProvidenceApi {
    const val endpoint = "https://providence.jfronny.dev/"

    private val client = HttpClient(Java) {
        engine {
            pipelining = true
            protocolVersion = HttpClientVersion.HTTP_2
        }
    }

    @Serializable
    data class WheelOption(
        val id: String?,
        val label: String,
        val weight: Int,
        val color: String?,
    )

    @Serializable
    data class WheelAction(
        val name: String,
        val template: String,
    )

    @Serializable
    data class WheelConfig(
        val hash: String?,
        val options: List<WheelOption>,
        val actions: List<WheelAction>,
    )

    private val latestHash = NIHCache(5.minutes) {
        client.get("https://blockstream.info/api/blocks/tip/hash").bodyAsText()
    }
    suspend fun getLatestHash(): String = latestHash.get()

    suspend fun createWheel(entries: List<Pair<CachedMovies.Movie, Int>>, hash: String?): Url = coroutineScope {
        val config = WheelConfig(
            hash = hash,
            options = entries.map { (movie, weight) ->
                async {
                    WheelOption(
                        id = movie.mediaEntry.id!!,
                        label = movie.mediaEntry.content?.title ?: "Unknown Title",
                        weight = weight,
                        color = TmColour[movie],
                    )
                }
            }.awaitAll(),
            actions = listOf(
                WheelAction("View Offers", "${Environment.hostname}/offers/{id}")
            ),
        )
        return@coroutineScope URLBuilder(endpoint).apply {
            parameters.append("config", Json.encodeToString(config))
        }.build()
    }
}
