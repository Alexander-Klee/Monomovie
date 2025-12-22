package de.amklee.monomovie

import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * https://providence.jfronny.dev/?page=about
 */
object ProvidenceApi {
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

    fun createWheel(entries: List<Pair<CachedMovies.Movie, Int>>): Url {
        val config = WheelConfig(
            hash = null,
            options = entries.map { (movie, weight) -> WheelOption(
                id = movie.mediaEntry.id!!,
                label = movie.mediaEntry.content?.title ?: "Unknown Title",
                weight = weight,
                color = null,
            ) },
            actions = listOf(
                WheelAction("View Offers", "$hostname/offers/{id}")
            ),
        )
        return URLBuilder("https://providence.jfronny.dev/").apply {
            parameters.append("config", Json.encodeToString(config))
        }.build()
    }
}
