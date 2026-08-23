package de.amklee.monomovie.components

import de.amklee.monomovie.R
import de.amklee.monomovie.util.IncludeSvgTemplates
import kotlinx.html.*

@HtmlTagMarker
inline fun HTML.HtmlTemplate(title: String, body: FlowContent.() -> Unit) {
    val title: String = if (title.endsWith("Monomovie")) title else "$title - Monomovie"
    head {
        meta(charset = "utf-8")
        meta(name = "viewport", content = "width=device-width, initial-scale=1")
        meta(name = "color-scheme", content = "dark light")
        title(title)

        meta(content = title) { property = "og:title" }
        meta(content = "website") { property = "og:type" }
        meta(content = "https://mmv.amklee.de/") { property = "og:url" }
        meta(content = "Monomovie") { property = "og:site_name" }
        meta(content = "Discover, bookmark and select movies for playback.") {
            property = "og:description"
        }
        meta(content = "https://mmv.amklee.de/og-image.png") { property = "og:image" }

        link(rel = "manifest", href = "/static/site.webmanifest")
        link(rel = "apple-touch-icon", href = "/static/apple-touch-icon.png")
        link(rel = "icon", type = "image/png", href = "/static/favicon-96x96.png") {
            sizes = "96x96"
        }

        style {
            unsafe { +R.styleCss }
        }

        script {
            unsafe { +R.libJs }
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
                    +R.topbuttonJs
                }
            }
            IncludeSvgTemplates()
            div(classes = "content") {
                body()
            }
        }
    }
}

@HtmlTagMarker
fun FlowContent.NavBar() {
    nav(classes = "navbar") {
        h1 { +"Movies" }

        ul(classes = "nav-list") {
            li { a(href = "/") { +"Home" } }
            li { a(href = "/watched") { +"Watched" } }
            li { a(href = "https://jellyfin.amklee.de", classes = "external") { +"Jellyfin" } }
            li { a(href = "https://recipes.amklee.de", classes = "external") { +"Recipes" } }
        }
    }
}

@HtmlTagMarker
fun FlowContent.MenuButton() {
    checkBoxInput(name = "menu-toggle", classes = "menu-toggle") {
        id = "menu-toggle"
        attributes["aria-label"] = "Open menu"
        style = "display:none"
    }
    label(classes = "menu-button floating-action-button") {
        htmlFor = "menu-toggle"
        unsafe { +R.graphics.svg.menuSvg }
    }
    label(classes = "menu-overlay") {
        htmlFor = "menu-toggle"
    }
}

@HtmlTagMarker
fun FlowContent.TopButton() {
    button(classes = "floating-action-button top-button") {
        id = "top_button"
        onClick = "gotoTop()"
        unsafe { +R.graphics.svg.arrowupSvg }
    }
}
