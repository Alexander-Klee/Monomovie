package de.amklee.monomovie.service.remote

import de.amklee.monomovie.Environment
import de.amklee.monomovie.pages.RouletteCachedMovie
import de.amklee.monomovie.util.NIHCache
import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.net.http.HttpClient.Version as HttpClientVersion
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * https://providence.jfronny.dev/?page=about
 */
object ProvidenceApi {
    const val ENDPOINT = "https://providence.jfronny.dev/"

    private val client =
        HttpClient(Java) {
            engine {
                pipelining = true
                protocolVersion = HttpClientVersion.HTTP_2
            }
        }

    @Serializable
    data class WheelOption(val id: String?, val label: String, val weight: Int, val color: String?)

    @Serializable
    data class WheelAction(val name: String, val template: String)

    @Serializable
    enum class HashSource {
        @SerialName("Bitcoin")
        Bitcoin,

        @SerialName("Monero")
        Monero,
    }

    @Serializable
    sealed interface HashRef {
        @SerialName("historic")
        @Serializable
        data class Historic(val hash: String, val source: HashSource?) : HashRef

        @SerialName("current")
        @Serializable
        data class Current(val source: HashSource?) : HashRef

        @SerialName("next")
        @Serializable
        data class Next(val source: HashSource?) : HashRef
    }

    @Serializable
    data class WheelConfig(val hash: HashRef, val options: List<WheelOption>, val actions: List<WheelAction>)

    private val latestHash =
        NIHCache(5.minutes) {
            client.get("https://blockstream.info/api/blocks/tip/hash").bodyAsText()
        }

    suspend fun getLatestHash(): String = latestHash.get()

    suspend fun createWheel(entries: List<RouletteCachedMovie>, hash: String?): Url = coroutineScope {
        val config =
            WheelConfig(
                hash =
                    hash?.let { HashRef.Historic(it, HashSource.Bitcoin) }
                        ?: HashRef.Current(source = null),
                options =
                    entries
                        .map { (movie, weight) ->
                            async {
                                WheelOption(
                                    id = movie.mediaEntry.id!!,
                                    label = movie.mediaEntry.content?.title ?: "Unknown Title",
                                    weight = weight,
                                    color = TmColour[movie],
                                )
                            }
                        }.awaitAll(),
                actions =
                    listOf(
                        WheelAction("View Offers", "${Environment.hostname}/offers/{id}"),
                    ),
            )
        return@coroutineScope URLBuilder(ENDPOINT)
            .apply {
                appendPathSegments("wheel")
                parameters.append("config", Json.encodeToString(config))
            }.build()
    }
}
