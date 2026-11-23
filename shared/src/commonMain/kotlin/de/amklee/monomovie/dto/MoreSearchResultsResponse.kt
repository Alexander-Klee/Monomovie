package de.amklee.monomovie.dto

import kotlinx.serialization.Serializable

@Serializable
data class MoreSearchResultsResponse(val cursor: String, val movies: Map<String, MovieItemUI>, val hasNextPage: Boolean)
