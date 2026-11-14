package de.amklee.monomovie.components

import de.amklee.monomovie.dto.SseEvent
import de.amklee.monomovie.service.CachedMovies
import de.amklee.monomovie.service.Event
import de.amklee.monomovie.util.buildULHtml

suspend fun convertBookmarkSse(event: Event, mode: SseEvent.Mode, kind: SseEvent.Kind): SseEvent? {
    if (mode == SseEvent.Mode.ROULETTE && kind == SseEvent.Kind.BOOKMARK) return null
    val movie = CachedMovies.get(event.id) ?: return null
    return when (event) {
        is Event.Added -> {
            SseEvent.Add(
                id = event.id,
                kind = kind,
                body =
                    buildULHtml {
                        if (mode == SseEvent.Mode.OVERVIEW) {
                            SelectableMovieListItem(movie)
                        } else {
                            MovieListItem(movie)
                        }
                    },
                insert =
                    when (mode) {
                        SseEvent.Mode.SEARCH -> false
                        SseEvent.Mode.WATCHED -> kind == SseEvent.Kind.WATCHED
                        SseEvent.Mode.OVERVIEW -> kind == SseEvent.Kind.BOOKMARK
                        SseEvent.Mode.ROULETTE -> false
                        SseEvent.Mode.OFFERS -> false
                    },
            )
        }

        is Event.Removed -> {
            SseEvent.Remove(
                id = event.id,
                kind = kind,
            )
        }
    }
}
