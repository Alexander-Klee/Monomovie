package de.amklee.monomovie

import io.ktor.resources.Resource
import kotlin.uuid.Uuid

@Resource("/")
class Route {
    @Resource("/")
    class Home(val parent: Route, val hidden: Boolean = false, val watched: Boolean = false)

    @Resource("/sse-stream")
    data class SseStream(val parent: Route = Route())

    @Resource("/search")
    class Search(val parent: Route = Route(), val title: String = "") {
        @Resource("results")
        class Results(val parent: Search, val cursor: String = "")
    }

    @Resource("/bookmarks")
    class Bookmarks(val parent: Route = Route()) {
        @Resource("{movieId}")
        class One(val parent: Bookmarks = Bookmarks(), val movieId: String = "")
    }

    @Resource("/watched")
    class Watched(val parent: Route = Route()) {
        @Resource("{movieId}")
        class One(val parent: Watched = Watched(), val movieId: String = "")
    }

    @Resource("/offers/{movieId}")
    class Offers(val parent: Route = Route(), val movieId: String = "")

    @Resource("/roulette")
    class Roulette(val parent: Route = Route()) {
        @Resource("/roulette")
        class Voting(val parent: Roulette = Roulette(), val shareId: Uuid? = null)

        @Resource("shared")
        class Shared(val parent: Roulette = Roulette()) {
            @Resource("{shareId}")
            class Selection(val parent: Shared = Shared(), val shareId: Uuid, val hidden: Boolean = false, val watched: Boolean = false)

            @Resource("{shareId}/movies/{movieId}")
            class Movie(val parent: Shared = Shared(), val shareId: Uuid, val movieId: String)

            @Resource("{shareId}/sse")
            class Sse(val parent: Shared = Shared(), val shareId: Uuid)
        }

        @Resource("submit")
        class Submit(val parent: Shared = Shared(), val shareId: Uuid? = null)
    }

    @Resource("/CachedMovies.json")
    class CachedMovies(val parent: Route = Route())

    @Resource("/jellyfinItems")
    class JellyfinItems(val parent: Route = Route())

    @Resource("/qr.svg")
    class QrSvg(val parent: Route = Route(), val data: String = "")

    @Resource("/placeholder.svg")
    class PlaceholderSvg(val parent: Route = Route())
}
