package de.amklee.monomovie.components

import de.amklee.monomovie.util.Resources
import de.amklee.monomovie.util.property
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

            meta(content = this@HtmlTemplate.title) { property = "og:title" }
            meta(content = "website") { property = "og:type" }
            meta(content = "https://mmv.amklee.de/") { property = "og:url" }
            meta(content = "Monomovie") { property = "og:site_name" }
            meta(content = "Discover, bookmark and select movies for playback.") { property = "og:description" }
            meta(content = "https://mmv.amklee.de/og-image.png") { property = "og:image" }

            link(rel = "manifest", href = "/static/site.webmanifest")
            link(rel = "apple-touch-icon", href = "/static/apple-touch-icon.png")
            link(rel = "icon", type = "image/png", href = "/static/favicon-96x96.png") { sizes = "96x96"}

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
