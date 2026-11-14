package de.amklee.monomovie.dto

import kotlinx.serialization.Serializable

@Serializable
data class JellyfinItems(val logoLink: String?, val tmdb: Map<String, String>, val imdb: Map<String, String>) {
    companion object {
        val EMPTY = JellyfinItems(null, emptyMap(), emptyMap())
    }

    operator fun get(movie: Movie): String? {
        val tmdbId = movie.mediaEntry.content
            ?.externalIds
            ?.tmdbId
        val imdbId = movie.mediaEntry.content
            ?.externalIds
            ?.imdbId

        val tmdb = tmdbId?.let { tmdb[it] }
        val imdb = imdbId?.let { imdb[it] }

        return tmdb ?: imdb
    }
}
