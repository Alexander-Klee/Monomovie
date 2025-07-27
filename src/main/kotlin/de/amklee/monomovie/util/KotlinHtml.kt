package de.amklee.monomovie.util

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import kotlinx.html.stream.createHTML

var META.property: String
    get() = attributes["property"] ?: throw IllegalStateException("Meta tag does not have a property attribute")
    set(value) {
        attributes["property"] = value
    }

// kotlinx.html does not come with helpers for easily building an HTML string, so we have to create our own.
// These are needed for the infinite scroll feature in the search page.
inline fun buildHtml(build: FlowContent.() -> Unit) = createHTML().apply {
    DIV(mapOf(), this).build()
}.finalize()
inline fun FlowContent.ful(content: UL.() -> Unit) = UL(mapOf(), consumer).content()
inline fun buildULHtml(build: UL.() -> Unit): String {
    return buildHtml {
        ful {
            build()
        }
    }
}

suspend inline fun ApplicationCall.respondHtml(status: HttpStatusCode = HttpStatusCode.OK, block: HTML.() -> Unit) {
    val text = buildString {
        append("<!DOCTYPE html>\n")
        appendHTML().html {
            block()
        }
    }
    respond(TextContent(text, ContentType.Text.Html.withCharset(Charsets.UTF_8), status))
}
