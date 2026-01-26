package de.amklee.monomovie

import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * https://providence.jfronny.dev/?page=about
 */
object ProvidenceApi {
    const val endpoint = "https://providence.jfronny.dev/"

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

    suspend fun createWheel(entries: List<Pair<CachedMovies.Movie, Int>>): Url = coroutineScope {
        val config = WheelConfig(
            hash = null,
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
                WheelAction("View Offers", "$hostname/offers/{id}")
            ),
        )
        return@coroutineScope URLBuilder(endpoint).apply {
            parameters.append("config", Json.encodeToString(config))
        }.build()
    }
}
