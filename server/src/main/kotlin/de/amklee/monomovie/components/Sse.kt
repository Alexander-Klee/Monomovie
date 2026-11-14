package de.amklee.monomovie.components

import de.amklee.monomovie.dto.SseEvent
import de.amklee.monomovie.service.CachedMovies
import de.amklee.monomovie.service.Event

suspend fun convertBookmarkSse(event: Event, kind: SseEvent.Kind): SseEvent? {
    return when (event) {
        is Event.Added -> SseEvent.Add(
            id = event.id,
            kind = kind,
            movie = CachedMovies.get(event.id) ?: return null,
        )

        is Event.Removed -> SseEvent.Remove(
            id = event.id,
            kind = kind,
        )
    }
}
