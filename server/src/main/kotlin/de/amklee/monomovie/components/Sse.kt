package de.amklee.monomovie.components

import de.amklee.monomovie.dto.MovieItemUI
import de.amklee.monomovie.dto.SseEvent
import de.amklee.monomovie.service.CachedMovies
import de.amklee.monomovie.service.Event
import de.amklee.monomovie.service.remote.JellyfinClient

suspend fun convertBookmarkSse(event: Event, kind: SseEvent.Kind): SseEvent? {
    return when (event) {
        is Event.Added -> SseEvent.Add(
            id = event.id,
            kind = kind,
            movie = CachedMovies.get(event.id)?.let {
                context(JellyfinClient.getItems()) { MovieItemUI(it) }
            } ?: return null,
        )

        is Event.Removed -> SseEvent.Remove(
            id = event.id,
            kind = kind,
        )
    }
}
