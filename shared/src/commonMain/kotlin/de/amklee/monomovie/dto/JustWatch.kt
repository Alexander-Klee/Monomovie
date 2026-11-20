package de.amklee.monomovie.dto

import kotlinx.serialization.Serializable

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
