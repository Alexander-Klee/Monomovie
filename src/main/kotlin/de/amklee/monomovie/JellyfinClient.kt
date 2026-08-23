package de.amklee.monomovie
import de.amklee.monomovie.util.NIHCache
import de.amklee.monomovie.util.error
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.DeviceInfo
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ItemFields

@OptIn(ExperimentalTime::class)
object JellyfinClient {
	private val log = System.getLogger("MMV/Jellyfin")

	private val jellyfin =
		createJellyfin {
			clientInfo = ClientInfo(name = "monomovie", version = "1.0.0")
			deviceInfo = DeviceInfo("monomovie", "monomovie")
		}

	private val api by lazy {
		jellyfin.createApi(
			baseUrl = Environment.jellyfinHost ?: return@lazy null,
			accessToken = Environment.jellyfinToken ?: return@lazy null,
		)
	}

	private val items =
		NIHCache<CacheEntry>(3.hours) {
			val api = api ?: return@NIHCache CacheEntry()
			try {
				val items = api.itemsApi.getItems(
					recursive = true,
					fields = setOf(ItemFields.PROVIDER_IDS),
				)
				if (items.status !=
					200
				) {
					throw IOException("Unexpected return code: ${items.status}")
				}
				CacheEntry(
					tmdb =
						items.content.items
							.mapNotNull {
								val id = it.providerIds?.get("Tmdb") ?: return@mapNotNull null
								id to api.actualUrl(it)
							}.toMap(),
					imdb =
						items.content.items
							.mapNotNull {
								val id = it.providerIds?.get("Imdb") ?: return@mapNotNull null
								id to api.actualUrl(it)
							}.toMap(),
				)
			} catch (e: Throwable) {
				log.error(e) { "Could not fetch items" }
				CacheEntry()
			}
		}

	private fun ApiClient.actualUrl(item: BaseItemDto): String =
		"$baseUrl/web/#/details?id=${item.id.toString().urlEncode()}&serverId=${item.serverId?.urlEncode()}"

	private fun String.urlEncode() = URLEncoder.encode(this, StandardCharsets.UTF_8)

	suspend fun findTmdbOnJellyfin(tmdbId: String) = items.get().tmdb[tmdbId]

	suspend fun findImdbOnJellyfin(imdbId: String) = items.get().imdb[imdbId]

	fun getLogoLink(): String {
		val api = api ?: return ""
		return "${api.baseUrl}/web/icon-transparent.baba78f2a106d9baee83.png"
	}

	private data class CacheEntry(val tmdb: Map<String, String>, val imdb: Map<String, String>) {
		constructor() : this(emptyMap(), emptyMap())
	}
}
