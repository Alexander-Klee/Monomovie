package de.amklee.monomovie.components

import java.io.FileNotFoundException
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

object Resources {
    private val styleResource: String by lazy {
        Resources::class.java.getResource("/style.css")?.readText()
            ?: throw FileNotFoundException("style.css not found in resources")
    }
    private val styleFile = Path("src/main/resources/style.css").takeIf { it.exists() }
    val style: String get() = styleFile?.readText() ?: styleResource
}

inline fun htmlTemplate(title: String, body: String, Nav: () -> String): String {
    // language=HTML
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <meta name="color-scheme" content="dark light" />
            <title>$title</title>
            
            <meta property="og:title" content="$title" />
            <meta property="og:type" content="website" />
            <meta property="og:url" content="https://mmv.amklee.de/" />
            <meta property="og:site_name" content="Monomovie" />
            <meta property="og:description" content="Discover, bookmark and select movies for playback." />
            <meta property="og:image" content="https://mmv.amklee.de/og-image.png" />
            
            <style>
                ${Resources.style}
            </style>
        </head>
        <body>
            <button class="menu-button" onclick="showMenu()">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512"><!--!Font Awesome Free 6.7.2 by @fontawesome - https://fontawesome.com License - https://fontawesome.com/license/free Copyright 2025 Fonticons, Inc.--><path d="M0 96C0 78.3 14.3 64 32 64l384 0c17.7 0 32 14.3 32 32s-14.3 32-32 32L32 128C14.3 128 0 113.7 0 96zM0 256c0-17.7 14.3-32 32-32l384 0c17.7 0 32 14.3 32 32s-14.3 32-32 32L32 288c-17.7 0-32-14.3-32-32zM448 416c0 17.7-14.3 32-32 32L32 448c-17.7 0-32-14.3-32-32s14.3-32 32-32l384 0c17.7 0 32 14.3 32 32z"/></svg>
            </button>
            <script>
                function showMenu() {
                    const nav = document.querySelector('nav');
                    nav.classList.toggle('nav-visible');
                    document.body.classList.toggle('menu-open', nav.classList.contains('nav-visible'));
                }
                function closeMenu() {
                    const nav = document.querySelector('nav');
                    if (nav.classList.contains('nav-visible')) {
                        nav.classList.remove('nav-visible');
                        document.body.classList.remove('menu-open');
                    }
                }
            </script>
            ${Nav()}
            <main onclick="closeMenu()">
                $body
            </main>
        </body>
        </html>
        """.trimIndent()
}

fun NavBar(): String {
    // language=HTML
    return """
        <nav>
            <h1>Movies</h1>
            <ul class="nav-list">
                <li><a href="/">Home</a></li>
                <li><a href="/search">Search</a></li>
                <li><a href="/bookmarks">Bookmarks</a></li>
                <li><a href="/watched">Watched</a></li>
                <li><a href="https://jellyfin.amklee.de">Jellyfin</a></li>
                <li><a href="https://amklee.de/recipe">Recipes</a></li>
            </ul>
        </nav>
    """.trimIndent()
}