package de.amklee.monomovie.components

import de.amklee.monomovie.R
import de.amklee.monomovie.Route
import de.amklee.monomovie.dto.JsCall
import de.amklee.monomovie.util.href
import io.ktor.resources.serialization.ResourcesFormat
import kotlinx.html.*

@HtmlTagMarker
context(resourcesFormat: ResourcesFormat)
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
        JsCall(JsCall.InjectTopButton)
        NavBar()

        main(classes = "section") {
            id = "main"
            IncludeSvgTemplates()
            div(classes = "container") {
                body()
            }
        }
    }
}

@OptIn(ExperimentalKotlinxHtmlApi::class)
@HtmlTagMarker
context(resourcesFormat: ResourcesFormat)
fun FlowContent.NavBar() {
    nav(classes = "navbar") {
        role = "navigation"

        div(classes = "navbar-brand") {
            a(classes = "navbar-item", href = href(Route.Home())) {
                unsafe { +R.static.MonomovieLogoSvg }
            }
            label(classes = "navbar-burger") {
                role = "button"
                htmlFor = "menu-toggle"
                span { attributes["aria-hidden"] = "true" }
                span { attributes["aria-hidden"] = "true" }
                span { attributes["aria-hidden"] = "true" }
                span { attributes["aria-hidden"] = "true" }
            }

            checkBoxInput {
                id = "menu-toggle"
                attributes["aria-label"] = "Open menu"
                style = "display:none"
            }
        }

        div(classes = "navbar-menu") {
            div(classes = "navbar-start") {
                a(classes = "navbar-item", href = "/") { +"Home" }
                a(classes = "navbar-item", href = "/watched") { +"Watched" }

                div(classes = "navbar-item has-dropdown is-hoverable") {
                    a(classes = "navbar-link") { +"More" }

                    div(classes = "navbar-dropdown") {
                        a(classes = "navbar-item external", href = "https://jellyfin.amklee.de") { +"Jellyfin" }
                        a(classes = "navbar-item external", href = "https://recipes.amklee.de") { +"Recipes" }
                    }
                }
            }
        }
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
