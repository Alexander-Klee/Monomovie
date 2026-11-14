package de.amklee.monomovie.dto

import kotlinx.serialization.Serializable

@Serializable
sealed interface SseEvent {
    val kind: Kind
    val id: String

    @Serializable data class Add(override val kind: Kind, override val id: String, val body: String, val insert: Boolean) : SseEvent

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
