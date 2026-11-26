package de.amklee.monomovie.components

import de.amklee.monomovie.Environment
import de.amklee.monomovie.R
import de.amklee.monomovie.db.Watched
import de.amklee.monomovie.dto.JellyfinItems
import de.amklee.monomovie.dto.JsCall
import de.amklee.monomovie.dto.Movie
import de.amklee.monomovie.dto.MovieItemUI
import de.amklee.monomovie.dto.SseEvent
import de.amklee.monomovie.pages.RouletteCachedMovie
import java.time.format.DateTimeFormatter
import kotlin.uuid.Uuid
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import kotlinx.html.*

context(jellyfinItems: JellyfinItems)
operator fun MovieItemUI.Companion.invoke(movie: Movie) = MovieItemUI(
    movie = movie,
    jellyfinLink = jellyfinItems[movie],
    jellyfinLogoLink = jellyfinItems.logoLink,
)

@HtmlTagMarker
inline fun FlowContent.MovieList(vararg calls: JsCall, classes: String = "movie-list", body: UL.() -> Unit) {
    script {
        type = "module"
        unsafe {
            for (call in calls) +call.toJsString()
        }
    }
    ul(classes = classes) {
        id = "movie-list"
        body()
    }
}

@HtmlTagMarker
fun FlowContent.SearchMovieList(title: String) = MovieList(
    JsCall.SetupSse(SseEvent.Mode.SEARCH),
    JsCall.SetupSearchInfiniteScroll(title),
) {
    id = "infinite-list"
    MovieListSentinel()
}

private val dateFormatter = DateTimeFormatter.ofPattern("dd. MMMM yyyy")

@HtmlTagMarker
context(jellyfinItems: JellyfinItems)
fun FlowContent.WatchedMovieList(movies: List<Watched.Item>) = MovieList(
    JsCall.SetupSse(SseEvent.Mode.WATCHED),
    classes = "movie-list movie-grid grid is-auto-fill",
) {
    val items = movies
        .groupBy { it.watchedAt.toLocalDateTime(Environment.timezone) }
        .entries
        .sortedByDescending { it.key }
    for ((date, movies) in items) {
        li(classes = "watched-date") {
            +date.toJavaLocalDateTime().format(dateFormatter)
        }
        for (movie in movies) {
            MovieListItem(MovieItemUI(movie.item))
        }
    }
}

@HtmlTagMarker
context(jellyfinItems: JellyfinItems)
fun FlowContent.SelectableMovieList(movies: List<Movie>) = MovieList(
    JsCall.SetupSse(SseEvent.Mode.OVERVIEW),
    JsCall.UpdateMovieSelectionCount,
    classes = "movie-list movie-grid grid is-auto-fill",
) {
    for (movie in movies) {
        SelectableMovieListItem(MovieItemUI(movie))
    }
}

@HtmlTagMarker
context(jellyfinItems: JellyfinItems)
fun FlowContent.RouletteMovieList(movies: Collection<RouletteCachedMovie>, shareId: Uuid?) = MovieList(
    if (shareId == null) {
        JsCall.SetupSse(SseEvent.Mode.ROULETTE)
    } else {
        JsCall.SetupRouletteVoting(shareId)
    },
) {
    for (movie in movies) {
        RouletteMovieListItem(MovieItemUI(movie.movie), movie.votes)
    }
}
