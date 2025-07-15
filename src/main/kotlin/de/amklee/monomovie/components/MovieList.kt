package de.amklee.monomovie.components

import de.amklee.monomovie.CachedMovies

class MovieList(private val movies: List<CachedMovies.Movie>) {
    // language=JS
    private val bookmarkJS = $$"""
        function bookmark(movieId, el, isDoubleClick) {
            const isSmallScreen = window.matchMedia("(max-width: 600px)").matches;
            if (isSmallScreen !== isDoubleClick) return;
            document.querySelectorAll("#" + movieId).forEach(checkbox => {
                checkbox.checked = false;
            });
        
            if (el.classList.contains('bookmarked')) {
                deleteBookmark(movieId, el);
            } else {
                setBookmark(movieId, el);
            }
        
            return false; // prevent default action
        }
        
        function setBookmark(movieId, el) {
            fetch(`/bookmark/${movieId}`, {
                method: 'POST'
            })
            .then(() => {
                el.classList.add('bookmarked');
            })
            .catch(error => {
                 console.error("Bookmark error:", error);
            });
        }
        
        function deleteBookmark(movieId, el) {
            fetch(`/bookmark/${movieId}`, {
                method: 'DELETE'
            })
            .then(() => {
                el.classList.remove('bookmarked');
            })
            .catch(error => {
                 console.error("Delete bookmark error:", error);
            });
        }
        """.trimIndent()

    // language=JS
    private val selectableJS = """
        function selectedChanged() {
            const disabled = document.querySelectorAll(".movie-checkbox:checked").length <= 1;
            
            document.querySelectorAll(".roulette-button").forEach(button => {
                button.disabled = disabled;
            });
        }
        
        selectedChanged();
        """.trimIndent()

    private fun selectableListElements() = movies.joinToString(separator = "\n") { MovieItem(it).selectableListItem() }

    fun listElements(): String = movies.joinToString(separator = "\n") { MovieItem(it).listItem() }

    fun basicList(): String {
        // language=HTML
        return """
            <script>
                $bookmarkJS
            </script>
            <ul class="movie-list">
                ${listElements()}
            </ul>
            """.trimIndent()
    }

    fun selectableList(): String {
        // language=HTML
        return """
            <script>
                $bookmarkJS
                $selectableJS
            </script>
            <ul class="movie-list">
                ${selectableListElements()}
            </ul>
            """
    }
}