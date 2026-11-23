package de.amklee.monomovie.dto

import kotlinx.serialization.Serializable

@Serializable
sealed interface RouletteSseEvent {
    val id: String

    @Serializable data class Add(override val id: String, val item: MovieItemUI, val count: Int) : RouletteSseEvent

    @Serializable data class Remove(override val id: String) : RouletteSseEvent

    @Serializable data class Update(override val id: String, val count: Int) : RouletteSseEvent
}
