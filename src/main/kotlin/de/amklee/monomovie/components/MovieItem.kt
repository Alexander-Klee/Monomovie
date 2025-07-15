package de.amklee.monomovie.components

import de.amklee.monomovie.CachedMovies
import de.amklee.monomovie.CachedMovies.getOffers
import io.ktor.util.escapeHTML

class MovieItem(private val movie: CachedMovies.Movie) {

    private fun renderOffers(movie: CachedMovies.Movie): String {
        val offers = movie.getOffers()

        val offerHtml = offers.joinToString("\n") { offer ->
            val iconUrl = "https://images.justwatch.com${offer.`package`?.icon}"
            val altText = offer.`package`?.clearName ?: "Unknown"
            val link = offer.standardWebURL ?: ""

            // language=HTML
            """
            <li class="offer-item">
                <a href="${link.escapeHTML()}">
                    <img src="${iconUrl.escapeHTML()}" alt="${altText.escapeHTML()}" class="offer-icon"/>
                </a>
            </li>
            """.trimIndent()
        }

        // language=HTML
        return """
            <ul class="offer-list">
                $offerHtml
            </ul>
            """.trimIndent()
    }

    private fun getRatings(movie: CachedMovies.Movie): String {
        fun linkWrapper(link: String?, content: String?): String {
            if (content.isNullOrBlank()) return ""
            if (link.isNullOrBlank()) return content
            // language=HTML
            return """
                <a href="$link" target="_blank" class="no-link-style" rel="noopener noreferrer">
                    $content
                </a>
                """.trimIndent()
        }
        fun formatScore(score: Float): String = String.format("%.1f", score)

        val tomatoRating = movie.mediaEntry.content?.scoring?.tomatoMeter?.let { score ->
            // language=HTML
            """
            <div class="movie-rating">
                <i class="tomato-icon rating-logo"></i>
                <p>$score%</p>
            </div>
            """.trimIndent()
        } ?: ""

        val imdbLink = movie.mediaEntry.content?.externalIds?.imdbId?.let { id -> "https://www.imdb.com/title/$id" }
        val imdbRating = movie.mediaEntry.content?.scoring?.imdbScore?.let { score ->
            // language=HTML
            """
            <div class="movie-rating">
                <i class="imdb-icon rating-logo"></i>
                <p>${formatScore(score)}</p>
            </div>
            """.trimIndent()
        } ?: ""

        val tmdbLinkType = when (movie.mediaEntry.objectType?.lowercase()) {
            "movie" -> "movie"
            "show" -> "tv"
            else -> "movie" // Default
        }
        val tmdbLink = movie.mediaEntry.content?.externalIds?.tmdbId?.let { id -> "https://www.themoviedb.org/$tmdbLinkType/$id" }
        val tmdbRating = movie.mediaEntry.content?.scoring?.tmdbScore?.let { score ->
            // language=HTML
            """
            <div class="movie-rating">
                <i class="tmdb-icon rating-logo"></i>
                <p>${formatScore(score)}</p>
            </div>
            """.trimIndent()
        } ?: ""

        return tomatoRating + linkWrapper(imdbLink, imdbRating) + linkWrapper(tmdbLink, tmdbRating)
    }

    private fun renderMovieItem(movie: CachedMovies.Movie): String {
        val cssClass = if (movie.isBookmarked) "bookmarked" else ""
        val movieId = movie.mediaEntry.id?.escapeHTML()
        val posterUrl = movie.mediaEntry.content?.posterUrl?.escapeHTML()
        val movieTitle = movie.mediaEntry.content?.title?.escapeHTML()
        val movieYear = movie.mediaEntry.content?.originalReleaseYear
        val movieDesc = movie.mediaEntry.content?.shortDescription?.escapeHTML()

        // language=HTML
        return """
            <div class="movie-item bookmark-container">
                <span class="movie-poster $cssClass" onclick="return bookmark('$movieId', this, false)" ondblclick="bookmark('$movieId', this, true)">
                    <span class="bookmark-icon"></span>
                    <img class="movie-poster" src="https://images.justwatch.com$posterUrl" alt="$movieTitle">
                </span>
                
                <div class="movie-details">
                    <div class="movie-title-bar">
                       <p class="movie-title">$movieTitle</p>
                       <div class="movie-rating-container">
                           ${getRatings(movie)}
                       </div>
                    </div>
                    <p class="movie-year">$movieYear</p>
                    <p class="movie-short-description" onclick="this.classList.add('expanded')">$movieDesc</p>
                </div>
                
                <div class="movie-offers">
                    ${renderOffers(movie)}
                </div>
            </div>
            """.trimIndent()
    }

    fun listItem(): String {
        // language=HTML
        return """
            <li class="movie-list-item">
               ${renderMovieItem(movie)}
            </li>
            """.trimIndent()
    }

    fun selectableListItem(): String {
        // language=HTML
        return """
            <li class="movie-list-item">
                <label for="${movie.mediaEntry.id?.escapeHTML()}">
                    <input type="checkbox" class="movie-checkbox" name="selected[]"
                        value="${movie.mediaEntry.id?.escapeHTML()}" id="${movie.mediaEntry.id?.escapeHTML()}"
                        onchange="selectedChanged()">
                    ${renderMovieItem(movie)}
                </label>
            </li>
            """.trimIndent()
    }

    fun rouletteListItem(): String {
        // language=HTML
        return """
            <li class="movie-list">
                <label for="${movie.mediaEntry.id?.escapeHTML()}">
                    <input type="number" class="roulette-weight"
                        name="${movie.mediaEntry.id?.escapeHTML()}" id="${movie.mediaEntry.id?.escapeHTML()}"
                        min="1" value="1">
                    ${renderMovieItem(movie)}
                </label>
            </li>
            """.trimIndent()
    }
}