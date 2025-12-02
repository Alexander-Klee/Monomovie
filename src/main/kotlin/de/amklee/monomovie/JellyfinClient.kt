package de.amklee.monomovie
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.DeviceInfo
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ItemFields
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
object JellyfinClient {
    private val log = LoggerFactory.getLogger("MMV/Jellyfin")

    private val jellyfin = createJellyfin {
        clientInfo = ClientInfo(name = "monomovie", version = "1.0.0",)
        deviceInfo = DeviceInfo("monomovie", "monomovie")
    }

    private val api by lazy {
        jellyfin.createApi(
            baseUrl = System.getenv("MMV_JELLYFIN_HOST"),
            accessToken = System.getenv("MMV_JELLYFIN_TOKEN")
        )
    }

    private var cache: CacheEntry? = null
    private var lastAccessed = Instant.fromEpochSeconds(0)
    private suspend fun getItems(): CacheEntry {
        if (cache != null && (Clock.System.now() - lastAccessed) < 3.hours) {
            return cache!!
        }
        try {
            val items = api.itemsApi.getItems(recursive = true, fields = setOf(ItemFields.PROVIDER_IDS))
            if (items.status != 200) throw IOException("Unexpected return code: ${items.status}")
            cache = CacheEntry(
                tmdb = items.content.items.mapNotNull {
                    val id = it.providerIds?.get("Tmdb") ?: return@mapNotNull null
                    id to it.actualUrl()
                }.toMap(),
                imdb = items.content.items.mapNotNull {
                    val id = it.providerIds?.get("Imdb") ?: return@mapNotNull null
                    id to it.actualUrl()
                }.toMap()
            )
            lastAccessed = Clock.System.now()
            return cache!!
        } catch (e: Throwable) {
            log.error("Could not fetch items", e)
            return cache ?: CacheEntry(emptyMap(), emptyMap())
        }
    }

    private fun BaseItemDto.actualUrl(): String {
        return "${api.baseUrl}/web/#/details?id=${this.id.toString().urlEncode()}&serverId=${this.serverId?.urlEncode()}"
    }

    private fun String.urlEncode() = URLEncoder.encode(this, StandardCharsets.UTF_8)

    suspend fun findTmdbOnJellyfin(tmdbId: String) = getItems().tmdb[tmdbId]
    suspend fun findImdbOnJellyfin(imdbId: String) = getItems().imdb[imdbId]

    private data class CacheEntry(val tmdb: Map<String, String>, val imdb: Map<String, String>)
}