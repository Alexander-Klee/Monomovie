package de.amklee.monomovie.components

import de.amklee.monomovie.R
import de.amklee.monomovie.dto.JsCall
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

        link(rel = "stylesheet", href = "/client/bulma.min.css")
        style {
            unsafe { +R.styleCss }
        }

        LoadMmvClient()
    }
    body {
        MenuButton()
        JsCall(JsCall.InjectTopButton)
        NavBar()

        main {
            id = "main"
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
fun HEAD.LoadMmvClient() {
    script {
        type = "module"
        unsafe {
            +"""
            import { entrypoint } from "/client/monomovie-web.mjs";
            window.monomovie = entrypoint;
            """.trimIndent()
        }
    }
}

@HtmlTagMarker
fun FlowContent.JsCall(call: JsCall) {
    script {
        type = "module"
        unsafe {
            +call.toJsString()
        }
    }
}

@HtmlTagMarker
fun FlowContent.IncludeSvgTemplates() {
    consumer.onTagContentUnsafe {
        for (resource in R.graphics.svgTemplates.index.values) {
            val resource by resource
            +resource
        }
    }
}
