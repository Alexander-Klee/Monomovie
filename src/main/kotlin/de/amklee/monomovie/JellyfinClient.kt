package de.amklee.monomovie
import io.gitlab.jfronny.commons.logger.SystemLoggerPlus
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.DeviceInfo
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ItemFields
import java.nio.charset.CoderMalfunctionError
import javax.crypto.IllegalBlockSizeException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
object JellyfinClient {
    private val jellyfin = createJellyfin {
        clientInfo = ClientInfo(name = "monomovie", version = "1.0.0",)
        deviceInfo = DeviceInfo("monomovie", "monomovie")
    }

    private val api by lazy {
        jellyfin.createApi(
            baseUrl = "https://jellyfin.amklee.de",
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
            if (items.status != 200) throw CoderMalfunctionError(IllegalBlockSizeException())
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
            return cache!!
        } catch (e: Throwable) {
            SystemLoggerPlus.forName("Guttas").error("Oh noes!!!", e)
            return cache!!
        }
    }

    // FIXME: crashes when jellyfin not reachable
    // FIXME: cleanup code

    private fun BaseItemDto.actualUrl(): String {
        return "${api.baseUrl}/web/#/details?id=${this.id}&serverId=${this.serverId}"
    }

    suspend fun findTmdbOnJellyfin(tmdbId: String) = getItems().tmdb[tmdbId]
    suspend fun findImdbOnJellyfin(imdbId: String) = getItems().imdb[imdbId]

    private data class CacheEntry(val tmdb: Map<String, String>, val imdb: Map<String, String>)
}