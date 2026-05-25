package de.amklee.monomovie.components

import de.amklee.monomovie.service.CachedMovies
import de.amklee.monomovie.service.Event
import de.amklee.monomovie.util.buildULHtml
import kotlinx.serialization.Serializable

@Serializable
sealed interface SseEvent {
    val kind: Kind
    val id: String

    @Serializable data class Add(
        override val kind: Kind,
        override val id: String,
        val body: String,
        val insert: Boolean
    ) : SseEvent

    @Serializable data class Remove(
        override val kind: Kind,
        override val id: String
    ) : SseEvent
}

enum class Kind {
    BOOKMARK, WATCHED
}

enum class Mode {
    SEARCH, WATCHED, OVERVIEW, ROULETTE, OFFERS
}

suspend fun convertBookmarkSse(event: Event, mode: Mode, kind: Kind): SseEvent? {
    if (mode == Mode.ROULETTE && kind == Kind.BOOKMARK) return null
    val movie = CachedMovies.get(event.id) ?: return null
    return when (event) {
        is Event.Added -> SseEvent.Add(
            id = event.id,
            kind = kind,
            body = buildULHtml {
                if (mode == Mode.OVERVIEW) SelectableMovieListItem(movie)
                else MovieListItem(movie)
            },
            insert = when (mode) {
                Mode.SEARCH -> false
                Mode.WATCHED -> kind == Kind.WATCHED
                Mode.OVERVIEW -> kind == Kind.BOOKMARK
                Mode.ROULETTE -> false
                Mode.OFFERS -> false
            }
        )
        is Event.Removed -> {
            SseEvent.Remove(
                id = event.id,
                kind = kind
            )
        }
    }
}
