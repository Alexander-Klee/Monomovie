package de.amklee.monomovie.service

sealed interface Event {
    val id: String

    data class Added(override val id: String) : Event

    data class Removed(override val id: String) : Event
}
