package de.amklee.monomovie

import de.amklee.monomovie.browser.IntersectionObserver
import de.amklee.monomovie.browser.IntersectionObserverInit
import de.amklee.monomovie.components.MovieListItem
import de.amklee.monomovie.dto.MoreSearchResultsResponse
import de.amklee.monomovie.util.MmvHttpClient
import de.amklee.monomovie.util.ful
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.browser.document
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.html.h3
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLUListElement

class SearchInfiniteScroll(private val title: String) {
    private val client = MmvHttpClient()
    private var state: State = State.Initial

    fun setup() {
        console.log("Setting up search infinite scroll for title: $title")

        val infiniteList = document.getElementById("infinite-list") as HTMLUListElement

        val observerRoot = document.getElementById("main") as HTMLElement
        val sentinel = document.getElementById("infinite-sentinel") as HTMLElement?

        if (sentinel == null) {
            console.error("Sentinel element not found")
            return
        }

        fun isElementVisibleInRoot(el: HTMLElement, root: HTMLElement, extra: Int = 400): Boolean {
            val rect = el.getBoundingClientRect()
            val rootRect = root.getBoundingClientRect()
            return rect.bottom >= rootRect.top - extra && rect.top <= rootRect.bottom + extra
        }

        var observer: IntersectionObserver? = null

        suspend fun handleEndReached() {
            console.log("Requesting more movies")
            getMoreMovies(infiniteList, sentinel)
            if (state is State.Finished) {
                console.log("No more movies to load")
                observer?.disconnect()
                sentinel.remove()
            } else {
                // move sentinel to the end so newly added items are before it
                infiniteList.appendChild(sentinel)
                console.log("more movies added")
            }
        }

        val mutex = Mutex()
        observer = IntersectionObserver(
            { entries, _ ->
                if (state is State.Finished || state is State.Loading) return@IntersectionObserver
                var hasIntersectingEntry = false
                for (entry in entries) {
                    if (entry.isIntersecting) hasIntersectingEntry = true
                }
                if (!hasIntersectingEntry) return@IntersectionObserver

                @OptIn(DelicateCoroutinesApi::class)
                GlobalScope.launch {
                    mutex.withLock {
                        do {
                            handleEndReached()
                        } while (state !is State.Finished && state !is State.Loading && isElementVisibleInRoot(sentinel, observerRoot))
                    }
                }
            },
            IntersectionObserverInit(
                root = observerRoot,
                rootMargin = "300px",
                scrollMargin = "0px",
                threshold = 0.01,
            ),
        )
        observer.observe(sentinel)
    }

    private suspend fun getMoreMovies(infiniteList: HTMLUListElement, sentinel: HTMLElement) {
        try {
            if (state !is State.Initial && state !is State.Idle) {
                return
            }
            state = State.Loading(state.lastCursor)

            val response = client.post {
                url {
                    path("search", "results")
                    parameters["title"] = this@SearchInfiniteScroll.title
                    state.lastCursor?.let { parameters["cursor"] = it }
                }
            }

            if (response.status.value !in 200..299) {
                throw IllegalStateException("Failed to fetch more movies: ${response.status}")
            }

            val movies = response.body<MoreSearchResultsResponse>()

            val filteredMovies = movies.movies.filterKeys { document.getElementById("movie-item-$it") == null }.values
            val moviesToUse = filteredMovies.ifEmpty { movies.movies.values }.toList()

            moviesToUse.asReversed().forEach {
                sentinel.insertAdjacentElement(
                    "beforebegin",
                    document.create {
                        ful {
                            MovieListItem(it)
                        }
                    },
                )
            }

            state = if (movies.hasNextPage) {
                State.Idle(movies.cursor)
            } else {
                infiniteList.append {
                    h3 {
                        if (state.lastCursor != null || moviesToUse.isNotEmpty()) {
                            +"No more movies found"
                        } else {
                            +"No movies found"
                        }
                    }
                }
                State.Finished
            }
        } catch (e: Exception) {
            if (e is CancellationException) {
                currentCoroutineContext().ensureActive()
            }
            console.error("Fetch error:", e)
            state = State.Idle(state.lastCursor)
        }
    }

    private sealed interface State {
        val lastCursor: String? get() = null
        object Initial : State
        class Idle(override val lastCursor: String?) : State
        class Loading(override val lastCursor: String?) : State
        object Finished : State
    }
}
