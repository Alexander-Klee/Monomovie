package de.amklee.monomovie.dto

import kotlinx.serialization.Serializable

@Serializable
sealed interface SseEvent {
    val kind: Kind
    val id: String

    // TODO movie objects can get quite big (several kilobytes), we might want to be more selective
    @Serializable data class Add(override val kind: Kind, override val id: String, val movie: Movie) : SseEvent

    @Serializable data class Remove(override val kind: Kind, override val id: String) : SseEvent

    enum class Kind {
        BOOKMARK,
        WATCHED,
    }

    enum class Mode {
        SEARCH,
        WATCHED,
        OVERVIEW,
        ROULETTE,
        OFFERS,
    }
}
