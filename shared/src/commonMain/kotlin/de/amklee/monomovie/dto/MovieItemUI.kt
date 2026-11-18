package de.amklee.monomovie.dto

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.serialization.Serializable

@Serializable
data class MovieItemUI(
    val id: String?,

    val isBookmarked: Boolean,
    val isWatched: Boolean,

    val title: String,
    val shortDescription: String?,
    val posterUrl: String?,
    val scoring: Scoring?,
    val originalReleaseYear: Int? = null,
    val runtime: Duration? = null,

    val imdbLink: String?,
    val tmdbLink: String?,

    val offers: List<OfferItemUI> = emptyList(),
) {
    constructor(movie: Movie, jellyfinLink: String?, jellyfinLogoLink: String?) : this(
        id = movie.mediaEntry.id?.ifBlank { null },
        isBookmarked = movie.isBookmarked,
        isWatched = movie.isWatched,
        title = movie.mediaEntry.title,
        shortDescription = movie.mediaEntry.content?.shortDescription,
        posterUrl = movie.mediaEntry.fullPosterUrl,
        scoring = movie.mediaEntry.content?.scoring,
        originalReleaseYear = movie.mediaEntry.content?.originalReleaseYear,
        runtime = movie.mediaEntry.content?.runtime?.let { if (it <= 0) null else it }?.minutes,
        imdbLink = movie.mediaEntry.imdbLink,
        tmdbLink = movie.mediaEntry.tmdbLink,
        offers = movie.getOffers().map { OfferItemUI(it) } + listOfNotNull(
            OfferItemUI.fromJellyfinLink(jellyfinLink, jellyfinLogoLink),
        ),
    )
}
