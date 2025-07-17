package de.amklee.monomovie.components

import de.amklee.monomovie.util.Resources
import io.ktor.server.html.*
import kotlinx.html.*

class HtmlTemplate(private val title: String): Template<HTML> {
    val body = Placeholder<FlowContent>()

    override fun HTML.apply() {
        head {
            meta(charset = "utf-8")
            meta(name = "viewport", content = "width=device-width, initial-scale=1")
            meta(name = "color-scheme", content = "dark light")
            title(this@HtmlTemplate.title)

            meta(content = this@HtmlTemplate.title) { name = "og:title" }
            meta(content = "website") { name = "og:type" }
            meta(content = "https://mmv.amklee.de/") { name = "og:url" }
            meta(content = "Monomovie") { name = "og:site_name" }
            meta(content = "Discover, bookmark and select movies for playback.") { name = "og:description" }
            meta(content = "https://mmv.amklee.de/og-image.png") { name = "og:image" }

            style {
                unsafe { +Resources.style }
            }
        }
        body {
            //TODO this is awful!
            //  A menu should never need JavaScript for basic functionality.
            button(classes = "menu-button") {
                onClick = "showMenu()"
                unsafe { +Resources.menuSvg }
            }
            script {
                unsafe { +Resources.menuJs }
            }

            NavBar()

            main {
                onClick = "closeMenu()"
                insert(body)
            }
        }
    }
}

fun FlowContent.NavBar() {
    nav {
        h1 { +"Movies" }
        ul(classes = "nav-list") {
            li { a(href = "/") { +"Home" } }
            li { a(href = "/search") { +"Search" } }
            li { a(href = "/bookmarks") { +"Bookmarks" } }
            li { a(href = "/watched") { +"Watched" } }
            li { a(href = "https://jellyfin.amklee.de") { +"Jellyfin" } }
            li { a(href = "https://amklee.de/recipe") { +"Recipes" } }
        }
    }
}
