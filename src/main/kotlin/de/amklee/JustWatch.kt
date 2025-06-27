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

package de.amklee

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.gson.*

class JustWatch(
    private val country: String = "US",
    private val language: String = "en"
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
            }
        }
        defaultRequest {
            url("https://apis.justwatch.com/graphql")
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun search(
        title: String,
        count: Int = 4,
        bestOnly: Boolean = true
    ): List<MediaEntry> {
        val requestBody = mapOf(
            "operationName" to "GetSearchTitles",
            "variables" to mapOf(
                "first" to count,
                "searchTitlesFilter" to mapOf("searchQuery" to title),
                "language" to language,
                "country" to country.uppercase(),
                "formatPoster" to "JPG",
                "formatOfferIcon" to "PNG",
                "profile" to "S718",
                "backdropProfile" to "S1920",
                "filter" to mapOf("bestOnly" to bestOnly)
            ),
            "query" to SEARCH_QUERY
        )
        val response: SearchResponse = client.post {
            setBody(requestBody)
        }.body()
        return response.data.popularTitles.edges.map { it.node }
    }

    suspend fun details(
        nodeId: String,
        bestOnly: Boolean = true
    ): MediaEntry? {
        val requestBody = mapOf(
            "operationName" to "GetTitleNode",
            "variables" to mapOf(
                "nodeId" to nodeId,
                "language" to language,
                "country" to country.uppercase(),
                "formatPoster" to "JPG",
                "formatOfferIcon" to "PNG",
                "profile" to "S718",
                "backdropProfile" to "S1920",
                "filter" to mapOf("bestOnly" to bestOnly)
            ),
            "query" to DETAILS_QUERY
        )
        val response: DetailsResponse = client.post {
            setBody(requestBody)
        }.body()
        return response.data.node
    }

    suspend fun offersForCountries(
        nodeId: String,
        countries: Set<String>,
        bestOnly: Boolean = true
    ): Map<String, List<Offer>> {
        if (countries.isEmpty()) return emptyMap()
        val countryEntries = countries.joinToString("\n") { code ->
            "${'$'}{code.uppercase()}: offers(country: ${'$'}{code.uppercase()}, platform: WEB, filter: \$filter) { ...TitleOffer __typename }"
        }
        val query = OFFERS_BY_COUNTRY_QUERY.replace("{country_entries}", countryEntries) + OFFER_FRAGMENT
        val requestBody = mapOf(
            "operationName" to "GetTitleOffers",
            "variables" to mapOf(
                "nodeId" to nodeId,
                "language" to language,
                "formatPoster" to "JPG",
                "formatOfferIcon" to "PNG",
                "profile" to "S718",
                "backdropProfile" to "S1920",
                "filter" to mapOf("bestOnly" to bestOnly)
            ),
            "query" to query
        )
        val response: OffersByCountryResponse = client.post {
            setBody(requestBody)
        }.body()
        val offersNode = response.data.node
        return countries.associateWith { code ->
            offersNode[code.uppercase()] ?: emptyList()
        }
    }

    companion object {
        private val SEARCH_QUERY = $$"""
            query GetSearchTitles(
              $searchTitlesFilter: TitleFilter!,
                $country: Country!,
              $language: Language!,
              $first: Int!,
              $formatPoster: ImageFormat,
              $formatOfferIcon: ImageFormat,
              $profile: PosterProfile,
              $backdropProfile: BackdropProfile,
              $filter: OfferFilter!,
            ) {
              popularTitles(
                country: $country
                filter: $searchTitlesFilter
                first: $first
                sortBy: POPULAR
                sortRandomSeed: 0
              ) {
                edges {
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
              }
            }
            """.trimIndent()
        private val DETAILS_FRAGMENT = $$"""
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
        private val OFFER_FRAGMENT = $$"""
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
        private val OFFERS_BY_COUNTRY_QUERY = $$"""
            query GetTitleOffers(
              $nodeId: ID!,
              $language: Language!,
              $formatOfferIcon: ImageFormat,
              $filter: OfferFilter!,
            ) {{
              node(id: $nodeId) {{
                ... on MovieOrShow {{
                  {country_entries}
                  __typename
                }}
                __typename
              }}
              __typename
            }}
            """.trimIndent()
        private val DETAILS_QUERY = $$"""
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

data class SearchResponse(val data: SearchData)
data class SearchData(val popularTitles: PopularTitles)
data class PopularTitles(val edges: List<Edge>)
data class Edge(val node: MediaEntry)

data class MediaEntry(
    val id: String?,
    val objectId: Int?,
    val objectType: String?,
    val content: Content?,
    val streamingCharts: StreamingChartsWrapper? = null,
    val offers: List<Offer>? = null
)

data class Content(
    val title: String?,
    val fullPath: String?,
    val originalReleaseYear: Int?,
    val originalReleaseDate: String?,
    val runtime: Int?,
    val shortDescription: String?,
    val genres: List<Genre>?,
    val externalIds: ExternalIds?,
    val posterUrl: String?,
    val backdrops: List<Backdrop>?,
    val ageCertification: String?,
    val scoring: Scoring?,
    val interactions: Interactions?
)

data class Genre(val shortName: String?)
data class ExternalIds(val imdbId: String?, val tmdbId: String?)
data class Backdrop(val backdropUrl: String?)
data class Scoring(
    val imdbScore: Float?,
    val imdbVotes: Int?,
    val tmdbPopularity: Float?,
    val tmdbScore: Float?,
    val tomatoMeter: Int?,
    val certifiedFresh: Boolean?,
    val jwRating: Float?
)
data class Interactions(val likelistAdditions: Int?, val dislikelistAdditions: Int?)
data class StreamingChartsWrapper(val edges: List<StreamingChartEdge>?)
data class StreamingChartEdge(val streamingChartInfo: StreamingChartInfo?)
data class StreamingChartInfo(
    val rank: Int?,
    val trend: String?,
    val trendDifference: Int?,
    val daysInTop3: Int?,
    val daysInTop10: Int?,
    val daysInTop100: Int?,
    val daysInTop1000: Int?,
    val topRank: Int?,
    val updatedAt: String?
)
data class Offer(
    val id: String?,
    val monetizationType: String?,
    val presentationType: String?,
    val retailPrice: String?,
    val retailPriceValue: Float?,
    val currency: String?,
    val lastChangeRetailPriceValue: Float?,
    val type: String?,
    val `package`: OfferPackage?,
    val standardWebURL: String?,
    val elementCount: Int?,
    val availableTo: String?,
    val deeplinkRoku: String?,
    val subtitleLanguages: List<String>?,
    val videoTechnology: List<String>?,
    val audioTechnology: List<String>?,
    val audioLanguages: List<String>?
)
data class OfferPackage(
    val id: String?,
    val packageId: Int?,
    val clearName: String?,
    val technicalName: String?,
    val icon: String?
)

data class DetailsResponse(val data: DetailsData)
data class DetailsData(val node: MediaEntry?)

data class OffersByCountryResponse(val data: OffersByCountryData)
data class OffersByCountryData(val node: Map<String, List<Offer>>)

