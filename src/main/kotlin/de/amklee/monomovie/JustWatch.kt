/* JustWatch API client for Kotlin
 * This client allows you to search for media titles, get details, and fetch offers from JustWatch.
 *
 * Usage:
 * val justWatch = JustWatch(country = "DE", language = "de")
 * val results = justWatch.search("Breaking Bad")
 * results.forEach { println(it) }
 *
 * (based on https://github.com/Electronic-Mango/simple-justwatch-python-api)
 *
 * This code is licensed under the GPL-3.0 License.
 */

package de.amklee.monomovie

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import java.net.http.HttpClient.Version as HttpClientVersion
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class JustWatch(private val country: String = "US", private val language: String = "en") {
    private val client =
        HttpClient(Java) {
            engine {
                pipelining = true
                protocolVersion = HttpClientVersion.HTTP_2
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = true
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                    },
                )
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 10000 // Set a timeout for requests
                socketTimeoutMillis = 10000 // Set a timeout for sockets
                connectTimeoutMillis = 10000 // Set a timeout for connections
            }
            defaultRequest {
                url("https://apis.justwatch.com/graphql")
                contentType(ContentType.Application.Json)
            }
        }

    suspend fun search(title: String, count: Int = 4, bestOnly: Boolean = true, cursor: String? = null): SearchTitles? {
        // Be aware that the JustWatch API returns duplicate titles when searching.
        val response =
            client.post {
                setBody(
                    SearchRequestBody(
                        variables =
                            SearchRequestBody.SearchVariables(
                                country = country.uppercase(),
                                first = count,
                                searchAfterCursor = cursor,
                                searchTitlesFilter = mapOf("searchQuery" to title),
                                filter = mapOf("bestOnly" to bestOnly),
                            ),
                        query = SEARCH_QUERY,
                    ),
                )
            }

        if (!response.status.isSuccess()) return null

        val body =
            try {
                response.body<SearchResponse>()
            } catch (_: Exception) {
                return null
            }

        return body.data.searchTitles
    }

    @Serializable
    private data class SearchRequestBody(
        val operationName: String = "GetSearchTitles",
        val variables: SearchVariables,
        val query: String,
    ) {
        @Serializable
        data class SearchVariables(
            val country: String,
            val language: String = "en",
            val first: Int,
            val searchAfterCursor: String? = null,
            val searchTitlesSortBy: String = "POPULAR",
            val searchTitlesFilter: Map<String, String>,
            val formatPoster: String = "JPG",
            val formatOfferIcon: String = "PNG",
            val profile: String = "S718",
            val backdropProfile: String = "S1920",
            val filter: Map<String, Boolean>,
            val location: String = "SearchPage",
        )
    }

    suspend fun details(nodeId: String, bestOnly: Boolean = true): MediaEntry? {
        val response =
            client.post {
                setBody(
                    DetailsRequestBody(
                        operationName = "GetTitleNode",
                        variables =
                            DetailsRequestBody.DetailsVariables(
                                nodeId = nodeId,
                                language = language,
                                country = country.uppercase(),
                                filter = mapOf("bestOnly" to bestOnly),
                            ),
                        query = DETAILS_QUERY,
                    ),
                )
            }

        if (!response.status.isSuccess()) return null

        val body =
            try {
                response.body<DetailsResponse>()
            } catch (_: Exception) {
                return null
            }

        return body.data.node
    }

    @Serializable
    private data class DetailsRequestBody(val operationName: String, val variables: DetailsVariables, val query: String) {
        @Serializable
        data class DetailsVariables(
            val nodeId: String,
            val language: String,
            val country: String,
            val formatPoster: String = "JPG",
            val formatOfferIcon: String = "PNG",
            val profile: String = "S718",
            val backdropProfile: String = "S1920",
            val filter: Map<String, Boolean>,
        )
    }

    private fun prepareOffersByCountryQuery(countries: Set<String>): String {
        val countryEntries =
            countries.joinToString("\n") { code ->
                $$"""
              $${code.uppercase()}: offers(country: $${code.uppercase()}, platform: WEB, filter: $filter) {
                ...TitleOffer
              }
                """.trimIndent()
            }

        // TODO: nodeID is ! so it should never be null? change this maybe in media Entry
        return $$"""
            query GetTitleOffers(
              $nodeId: ID!,
              $language: Language!,
              $formatOfferIcon: ImageFormat,
              $filter: OfferFilter!
            ) {
              node(id: $nodeId) {
                ... on MovieOrShow {
                  $$countryEntries
                }
              }
            }
        """.trimIndent() + OFFER_FRAGMENT
    }

    suspend fun offersForCountries(nodeId: String, countries: Set<String>, bestOnly: Boolean = true): Map<String, List<Offer>> {
        if (countries.isEmpty()) return emptyMap()
        val query = prepareOffersByCountryQuery(countries)

        val response =
            client.post {
                setBody(
                    OffersByCountryRequestBody(
                        variables =
                            OffersByCountryRequestBody.OffersByCountryVariables(
                                nodeId = nodeId,
                                filter = mapOf("bestOnly" to bestOnly),
                            ),
                        query = query,
                    ),
                )
            }

        if (!response.status.isSuccess()) return countries.associateWith { emptyList() }

        val body =
            try {
                response.body<OffersByCountryResponse>()
            } catch (_: Exception) {
                return countries.associateWith { emptyList<Offer>() }
            }

        val offersNode = body.data?.node
        return countries.associateWith { code ->
            offersNode?.get(code.uppercase()) ?: emptyList()
        }
    }

    @Serializable
    private data class OffersByCountryRequestBody(
        val operationName: String = "GetTitleOffers",
        val variables: OffersByCountryVariables,
        val query: String,
    ) {
        @Serializable
        data class OffersByCountryVariables(
            val nodeId: String,
            val language: String = "en",
            val formatPoster: String = "JPG",
            val formatOfferIcon: String = "PNG",
            val profile: String = "S718",
            val backdropProfile: String = "S1920",
            val filter: Map<String, Boolean>,
        )
    }

    companion object {
        private val SEARCH_QUERY =
            $$"""
            query GetSearchTitles(
              $country: Country!,
              $language: Language!,
              $first: Int!,
              $searchAfterCursor: String,
              $searchTitlesFilter: TitleFilter!,
              $searchTitlesSortBy: PopularTitlesSorting!,
              $formatPoster: ImageFormat,
              $formatOfferIcon: ImageFormat,
              $profile: PosterProfile,
              $backdropProfile: BackdropProfile,
              $filter: OfferFilter!,
              $location: String!
            ) {
              searchTitles(
                after: $searchAfterCursor,
                country: $country,
                filter: $searchTitlesFilter,
                first: $first,
                sortBy: $searchTitlesSortBy,
                sortRandomSeed: 0,
                source: $location
              ) {
                edges {
                  cursor
                  node {
                    id
                    objectId
                    objectType
                    content(country: $country, language: $language) {
                      title
                      fullPath
                      originalReleaseYear
                      originalReleaseDate
                      runtime
                      shortDescription
                      genres { shortName }
                      externalIds { imdbId tmdbId }
                      posterUrl(profile: $profile, format: $formatPoster)
                      backdrops(profile: $backdropProfile, format: $formatPoster) { backdropUrl }
                      ageCertification
                      scoring { imdbScore imdbVotes tmdbPopularity tmdbScore tomatoMeter certifiedFresh jwRating }
                      interactions { likelistAdditions dislikelistAdditions }
                    }
                    streamingCharts(country: $country) { edges { streamingChartInfo { rank trend trendDifference daysInTop3 daysInTop10 daysInTop100 daysInTop1000 topRank updatedAt } } }
                    offers(country: $country, platform: WEB, filter: $filter) {
                      id
                      monetizationType
                      presentationType
                      retailPrice(language: $language)
                      retailPriceValue
                      currency
                      lastChangeRetailPriceValue
                      type
                      package { id packageId clearName technicalName icon(profile: S100, format: $formatOfferIcon) }
                      standardWebURL
                      elementCount
                      availableTo
                      deeplinkRoku: deeplinkURL(platform: ROKU_OS)
                      subtitleLanguages
                      videoTechnology
                      audioTechnology
                      audioLanguages
                    }
                  }
                }
                pageInfo {
                  endCursor
                  hasNextPage
                }
              }
            }
            """.trimIndent()
        private val DETAILS_FRAGMENT =
            $$"""
            fragment TitleDetails on MovieOrShow {
              id
              objectId
              objectType
              content(country: $country, language: $language) {
                title
                fullPath
                originalReleaseYear
                originalReleaseDate
                runtime
                shortDescription
                genres { shortName __typename }
                externalIds { imdbId tmdbId __typename }
                posterUrl(profile: $profile, format: $formatPoster)
                backdrops(profile: $backdropProfile, format: $formatPoster) { backdropUrl __typename }
                ageCertification
                scoring { imdbScore imdbVotes tmdbPopularity tmdbScore tomatoMeter certifiedFresh jwRating __typename }
                interactions { likelistAdditions dislikelistAdditions __typename }
                __typename
              }
              streamingCharts(country: $country) {
                edges {
                  streamingChartInfo {
                    rank trend trendDifference daysInTop3 daysInTop10 daysInTop100 daysInTop1000 topRank updatedAt __typename
                  }
                  __typename
                }
                __typename
              }
              offers(country: $country, platform: WEB, filter: $filter) {
                ...TitleOffer
              }
              __typename
            }
            """.trimIndent()
        private val OFFER_FRAGMENT =
            $$"""
            fragment TitleOffer on Offer {
              id
              monetizationType
              presentationType
              retailPrice(language: $language)
              retailPriceValue
              currency
              lastChangeRetailPriceValue
              type
              package { id packageId clearName technicalName icon(profile: S100, format: $formatOfferIcon) __typename }
              standardWebURL
              elementCount
              availableTo
              deeplinkRoku: deeplinkURL(platform: ROKU_OS)
              subtitleLanguages
              videoTechnology
              audioTechnology
              audioLanguages
              __typename
            }
            """.trimIndent()
        private val DETAILS_QUERY =
            $$"""
            query GetTitleNode(
              $nodeId: ID!,
              $language: Language!,
              $country: Country!,
              $formatPoster: ImageFormat,
              $formatOfferIcon: ImageFormat,
              $profile: PosterProfile,
              $backdropProfile: BackdropProfile,
              $filter: OfferFilter!,
            ) {
              node(id: $nodeId) {
                ...TitleDetails
                __typename
              }
              __typename
            }
            """.trimIndent() + DETAILS_FRAGMENT + OFFER_FRAGMENT
    }
}

@Serializable
data class SearchResponse(val data: SearchData)

@Serializable
data class SearchData(val searchTitles: SearchTitles)

@Serializable
data class SearchTitles(val edges: List<Edge>, val pageInfo: PageInfo)

@Serializable
data class PageInfo(val endCursor: String, val hasNextPage: Boolean)

@Serializable
data class Edge(val cursor: String, val node: MediaEntry)

@Serializable
data class MediaEntry(
    val id: String? = null,
    val objectId: Int? = null,
    val objectType: String? = null,
    val content: Content? = null,
    val streamingCharts: StreamingChartsWrapper? = null,
    val offers: List<Offer>? = null,
)

val MediaEntry.title: String
    get() = content?.title ?: "Unknown Title"

val MediaEntry.fullPosterUrl: String?
    get() = content?.posterUrl?.let { "https://images.justwatch.com$it" }

val MediaEntry.imdbLink: String?
    get() = content?.externalIds?.imdbId?.let { "https://www.imdb.com/title/$it" }

val MediaEntry.tmdbLink: String?
    get() {
        val tmdbLinkType =
            when (objectType?.lowercase()) {
                "movie" -> "movie"
                "show" -> "tv"
                else -> "movie" // Default
            }
        return content?.externalIds?.tmdbId?.let { "https://www.themoviedb.org/$tmdbLinkType/$it" }
    }

@Serializable
data class Content(
    val title: String? = null,
    val fullPath: String? = null,
    val originalReleaseYear: Int? = null,
    val originalReleaseDate: String? = null,
    val runtime: Int? = null,
    val shortDescription: String? = null,
    val genres: List<Genre>? = null,
    val externalIds: ExternalIds? = null,
    val posterUrl: String? = null,
    val backdrops: List<Backdrop>? = null,
    val ageCertification: String? = null,
    val scoring: Scoring? = null,
    val interactions: Interactions? = null,
)

@Serializable
data class Genre(val shortName: String? = null)

@Serializable
data class ExternalIds(val imdbId: String? = null, val tmdbId: String? = null)

@Serializable
data class Backdrop(val backdropUrl: String? = null)

@Serializable
data class Scoring(
    val imdbScore: Float? = null,
    val imdbVotes: Float? = null,
    val tmdbPopularity: Float? = null,
    val tmdbScore: Float? = null,
    val tomatoMeter: Int? = null,
    val certifiedFresh: Boolean? = null,
    val jwRating: Float? = null,
)

@Serializable
data class Interactions(val likelistAdditions: Int? = null, val dislikelistAdditions: Int? = null)

@Serializable
data class StreamingChartsWrapper(val edges: List<StreamingChartEdge>? = null)

@Serializable
data class StreamingChartEdge(val streamingChartInfo: StreamingChartInfo? = null)

@Serializable
data class StreamingChartInfo(
    val rank: Int? = null,
    val trend: String? = null,
    val trendDifference: Int? = null,
    val daysInTop3: Int? = null,
    val daysInTop10: Int? = null,
    val daysInTop100: Int? = null,
    val daysInTop1000: Int? = null,
    val topRank: Int? = null,
    val updatedAt: String? = null,
)

@Serializable
data class Offer(
    val id: String? = null,
    val monetizationType: String? = null,
    val presentationType: String? = null,
    val retailPrice: String? = null,
    val retailPriceValue: Float? = null,
    val currency: String? = null,
    val lastChangeRetailPriceValue: Float? = null,
    val type: String? = null,
    val `package`: OfferPackage? = null,
    val standardWebURL: String? = null,
    val elementCount: Int? = null,
    val availableTo: String? = null,
    val deeplinkRoku: String? = null,
    val subtitleLanguages: List<String>? = null,
    val videoTechnology: List<String>? = null,
    val audioTechnology: List<String>? = null,
    val audioLanguages: List<String>? = null,
)

@Serializable
data class OfferPackage(
    val id: String? = null,
    val packageId: Int? = null,
    val clearName: String? = null,
    val technicalName: String? = null,
    val icon: String? = null,
)

@Serializable
data class DetailsResponse(val data: DetailsData)

@Serializable
data class DetailsData(val node: MediaEntry? = null)

@Serializable
data class OffersByCountryResponse(val data: OffersByCountryData? = null)

@Serializable
data class OffersByCountryData(val node: Map<String, List<Offer>> = emptyMap())
