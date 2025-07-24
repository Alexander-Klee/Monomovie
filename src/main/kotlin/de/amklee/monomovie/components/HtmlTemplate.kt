package de.amklee.monomovie.components

import de.amklee.monomovie.util.Resources
import de.amklee.monomovie.util.property
import io.ktor.server.html.*
import kotlinx.html.*

class HtmlTemplate(title: String): Template<HTML> {
    private val title: String = if (title.endsWith("Monomovie")) title else "$title - Monomovie"

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
            MenuButton()
            TopButton()
            NavBar()

            main {
                id = "main"
                script {
                    unsafe {
                        +Resources.topbuttonJs
                    }
                }
                div(classes = "content") {
                    insert(body)
                }
            }
        }
    }
}

fun FlowContent.NavBar() {
    nav(classes = "navbar") {
        h1 { +"Movies" }

        ul(classes = "nav-list") {
            li { a(href = "/") { +"Home" } }
            li { a(href = "/search") { +"Search" } }
            li { a(href = "/bookmarks") { +"Bookmarks" } }
            li { a(href = "/watched") { +"Watched" } }
            li { a(href = "https://jellyfin.amklee.de") { +"Jellyfin" } }
            li { a(href = "https://recipes.amklee.de") { +"Recipes" } }
        }
    }
}

fun FlowContent.MenuButton() {
    checkBoxInput(name = "menu-toggle", classes = "menu-toggle") {
        id = "menu-toggle"
        attributes["aria-label"] = "Open menu"
        style = "display:none"
    }
    label(classes = "menu-button floating-action-button") {
        htmlFor = "menu-toggle"
        unsafe { +Resources.menuSvg }
    }
    label(classes = "menu-overlay") {
        htmlFor = "menu-toggle"
    }
}

fun FlowContent.TopButton() {
    button(classes = "floating-action-button top-button") {
        id = "top_button"
        onClick = "gotoTop()"
        unsafe { +Resources.arrowUpSvg }
    }
}