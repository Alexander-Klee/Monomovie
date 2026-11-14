package de.amklee.monomovie.dto

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Movie(val mediaEntry: MediaEntry, var isBookmarked: Boolean, var isWatched: Boolean, val cacheDate: Instant) {
    fun getOffers() = mediaEntry.offers?.filter {
        it.monetizationType.equals(MonetizationTypes.FREE.name, ignoreCase = true) ||
            it.monetizationType.equals(MonetizationTypes.FLATRATE.name, ignoreCase = true)
    } ?: emptyList()
}
